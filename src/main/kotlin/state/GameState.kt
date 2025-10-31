package com.example.state

import com.example.MoveRequest
import com.example.models.Card
import com.example.models.DeckFactory
import com.example.models.GameSnapshot
import com.example.models.Player
import java.util.concurrent.atomic.AtomicInteger

object GameState {
    private val players = mutableListOf<Player>()
    private var deck: MutableList<Card> = mutableListOf()
    private var activeIndex = 0
    private val maxPlayers = 2
    private val idGen = AtomicInteger(0)

    fun addPlayer(name: String): Player? {
        println("---")
        println("[ACTION] Próba dodania gracza: $name")

        if (players.size >= maxPlayers) {
            println("[ERROR] Nie można dodać gracza. Osiągnięto maksymalną liczbę graczy ($maxPlayers).")
            return null
        }

        val player = Player(idGen.getAndIncrement(), name)
        players.add(player)
        println("[STATE] Dodano gracza: ${player.name} (ID: ${player.id}). Liczba graczy: ${players.size}")

        if (players.size == 1) {
            activeIndex = 0
            println("[STATE] Ustawiono aktywnego gracza na pozycję 0 (ID: ${players[activeIndex].id}).")
        }


        if (players.size == maxPlayers) {
            println("[ACTION] Osiągnięto maksymalną liczbę graczy. Rozpoczynanie gry...")
            startGame()
        }

        return player
    }

    fun startGame() {
        println("---")
        println("[GAME] Rozpoczęcie gry...")
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        println("[GAME] Stworzono i potasowano talię. Liczba kart: ${deck.size}")
        activeIndex = 0
        println("[GAME] Ustawiono aktywnego gracza na pozycję 0.")

        players.forEach { player ->
            println("[ACTION] Dobieranie karty startowej dla gracza: ${player.name}")
            drawCardForPlayer(player.id)
        }
        if(players.isNotEmpty()){
            println("[ACTION] Dobieranie drugiej karty dla aktywnego gracza: ${players[activeIndex].name}")
            drawCardForPlayer(players[activeIndex].id)
        }
        println("[GAME] Gra rozpoczęta. Aktywny gracz: ${players[activeIndex].name}, karty na ręce: ${players[activeIndex].hand.joinToString { it.name }}")
    }

    fun getPlayers(): List<Player> = players

    fun drawCardForPlayer(playerId: Int) {
        val player = players.find { it.id == playerId }
        val card = deck.removeFirstOrNull()

        if (card != null && player != null) {
            println("[ACTION] Gracz ${player.name} dobiera kartę: ${card.name}. Pozostało kart w talii: ${deck.size}")
            addCardToHand(player,card)
        } else {
            if (player == null) println("[WARNING] Nie znaleziono gracza o ID: $playerId")
            if (card == null) println("[WARNING] Talia jest pusta. Nie można dobrać karty.")
        }
    }

    fun getState(): GameSnapshot {
        val activeId = if (players.isNotEmpty()) players[activeIndex].id else null
        return GameSnapshot(players.toList(), deck, activeId)
    }

    fun makeMove(request: MoveRequest): MoveResult {
        println("---")
        println("[ACTION] Próba wykonania ruchu: Gracz ID ${request.playerId} zagrywa kartę ${request.card.name}")

        if (players.isEmpty()) {
            println("[ERROR] Nie można wykonać ruchu, ponieważ nie ma graczy w grze.")
            return MoveResult.Error("Brak graczy")
        }

        val activePlayer = players[activeIndex]
        println("[CHECK] Oczekiwany ruch gracza: ${activePlayer.name} (ID: ${activePlayer.id})")

        return if (activePlayer.id != request.playerId) {
            println("[ERROR] Ruch odrzucony. Nie jest to tura gracza o ID ${request.playerId}.")
            MoveResult.Error("Nie twoja tura")
        } else {
            println("[SUCCESS] Tura gracza ${activePlayer.name} potwierdzona.")

            val moveCompleted = playTheCard(request)
            println("[STATE] Efekt karty przetworzony. Czy ruch został zakończony?: $moveCompleted")

            val msg = "Gracz ${activePlayer.name} wykonał ruch kartą ${request.card.name}"
            if (moveCompleted) {
                val previousActivePlayerName = players[activeIndex].name
                activeIndex = (activeIndex + 1) % players.size
                val nextActivePlayer = players[activeIndex]
                println("[STATE] Ruch zakończony. Poprzedni gracz: $previousActivePlayerName. Następny gracz: ${nextActivePlayer.name} (ID: ${nextActivePlayer.id})")
                println("[ACTION] Dobieranie karty dla następnego gracza...")
                drawCardForPlayer(nextActivePlayer.id)
            } else {
                println("[STATE] Ruch nie został zakończony. Gracz ${activePlayer.name} musi wykonać dodatkową akcję. Tura nie zostaje zmieniona.")
            }

            MoveResult.Success(msg, activeIndex)
        }
    }

    private fun playTheCard(request: MoveRequest): Boolean {
        println("[ACTION] Zagrywanie karty: ${request.card.name}")
        val player = players.find { it.id == request.playerId }
            ?: return false // Zabezpieczenie, gdyby gracz zniknął

        removeCardFromHand(player, request.card)
        addCardToPlayed(player, request.card)

        println("[ACTION] Przetwarzanie efektu karty...")
        return resolveCardEffect(request)
    }

    private fun addCardToPlayed(player: Player, card: Card) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            println("[STATE] Dodawanie karty ${card.name} do zagranych przez gracza ${player.name}")
            players[playerIndex] = player.copy(playedCards = player.playedCards + card)
        }
    }

    private fun addCardToHand(player: Player, card: Card) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            println("[STATE] Dodawanie karty ${card.name} do ręki gracza ${player.name}")
            players[playerIndex] = player.copy(hand = player.hand + card)
        }
    }

    private fun removeCardFromHand(
        player: Player,
        card: Card
    ) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            println("[STATE] Usuwanie karty ${card.name} z ręki gracza ${player.name}")

            val newHand = player.hand - card
            println("[STATE] Removing card: $card")
            println("[STATE] Old hand: ${player.hand}")
            println("[STATE] New hand: $newHand")

            players[playerIndex] = player.copy(hand = newHand)
        }
    }

    private fun resolveCardEffect(request: MoveRequest): Boolean {
        // Pobranie gracza wykonującego ruch
        val currentPlayer = players.find { it.id == request.playerId } ?: return false

        // Pobranie przeciwnika (działa dla 2 graczy)
        val opponent = players.firstOrNull { it.id == request.targetPlayerId }

        when (request.card.number) {
            1 -> {
                // Logika dla karty nr 1: Gracz odgaduje kartę przeciwnika.
                // Tutaj potrzebna byłaby dodatkowa informacja z `request`, np. `request.guessedCard`.
                // Na razie zostawiamy jako przykład.
                println("Gracz ${currentPlayer.name} użył karty 1 (Strażniczka)")
            }

            2 -> {
                // Logika dla karty nr 2: Gracz patrzy na rękę przeciwnika.
                opponent?.let {
                    println("Gracz ${currentPlayer.name} użył karty 2 (Kapłan) i podejrzał rękę ${it.name}")
                    // W prawdziwej aplikacji wysłalibyśmy informację o karcie przeciwnika
                    // tylko do gracza, który zagrał kartę.
                }
            }

            3 -> {
                // Logika dla karty nr 3: Gracz porównuje swoją kartę z kartą przeciwnika.
                // Gracz z niższą kartą odpada.
                opponent?.let {
                    val playerCard = currentPlayer.hand.firstOrNull()
                    val opponentCard = it.hand.firstOrNull()

                    if (playerCard != null && opponentCard != null) {
                        when {
                            playerCard.number < opponentCard.number -> println("Gracz ${currentPlayer.name} przegrał starcie i odpada z gry.")
                            playerCard.number > opponentCard.number -> println("Gracz ${it.name} przegrał starcie i odpada z gry.")
                            else -> println("Starcie zakończone remisem.")
                        }
                    }
                }
            }

            4 -> {
                // sluzaca
                println("Gracz ${currentPlayer.name} jest chroniony do następnej tury.")
                // currentPlayer.isProtected = true
            }

            5 -> {
                // ksiaze
                /*opponent?.let {
                    val discardedCard = it.hand.removeFirstOrNull()
                    if (discardedCard != null) {
                        println("Gracz ${it.name} odrzucił kartę ${discardedCard.name} i dobrał nową.")
                        drawCardForPlayer(it.id)
                    }
                }*/
            }

            6 -> {
                println("Gracz ${currentPlayer.name} zagrał kanclerza.")

                /* // Krok 1: Gracz dobiera dwie dodatkowe karty z talii.
                 val firstExtraCard = deck.removeFirstOrNull()
                 val secondExtraCard = deck.removeFirstOrNull()

                 // Krok 2: Dodaj nowe karty do ręki gracza, jeśli zostały dobrane.
                 if (firstExtraCard != null) {
                     addCardToHand(currentPlayer, firstExtraCard)
                 }
                 if (secondExtraCard != null) {
                     addCardToHand(currentPlayer, secondExtraCard)
                 }

                 // Krok 3: W tym momencie gracz ma w ręku więcej niż jedną kartę.
                 // Gra musi poczekać, aż gracz wybierze jedną z nich, a resztę odrzuci.
                 // To jest kluczowy moment - stan gry staje się "oczekujący na decyzję gracza".



                 // Tutaj powinniśmy wysłać do klienta informację, że musi dokonać wyboru.
                 // Poniższy println symuluje tę akcję.
                 println("Gracz ${currentPlayer.name} musi teraz wybrać jedną kartę do pozostawienia w ręce z: ${currentPlayer.hand.joinToString { it.name }}")

                 // WAŻNE: W tym miejscu nie zmieniamy tury gracza!
                 // Aktywnym graczem pozostaje currentPlayer, dopóki nie dokończy swojego ruchu (wybierając kartę).
                 // Logika zmiany tury w `makeMove` musi zostać dostosowana, aby to obsłużyć.

                 //return false*/
            }

            7 -> {
                opponent?.let {
                    val playerHand = currentPlayer.hand
                    val tempHand = playerHand.toMutableList()

                    currentPlayer.copy(hand = it.hand)
                    it.copy(hand = tempHand)

                    println("Gracze ${currentPlayer.name} i ${it.name} zamienili się kartami.")
                }
            }

            8 -> {
                // hrabina
                println("Gracz ${currentPlayer.name} zagrał kartę  nr 8.")

            }

            9 -> {

                println("Gracz ${currentPlayer.name} odrzucił Księżniczkę i odpada z gry!")
            }

            else -> {
                // Dobra praktyka: obsłuż nieoczekiwane wartości, chociaż nie powinny wystąpić.
                println("Zagrano kartę o nieznanym numerze: ${request.card.number}")
            }
        }
        return true
    }


    fun resetGame() {
        players.clear()
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        activeIndex = 0
        idGen.set(0)
    }

    sealed interface MoveResult {
        data class Success(val message: String, val nextPlayer: Int) : MoveResult
        data class Error(val message: String) : MoveResult
    }
}