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
        if (players.size >= maxPlayers) return null

        val player = Player(idGen.getAndIncrement(), name)
        players.add(player)

        if (players.size == 1)
            activeIndex = 0

        if (players.size == maxPlayers) {
            startGame()
        }

        return player
    }

    fun startGame() {
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        activeIndex = 0
       players.forEach { player ->
           drawCardForPlayer(player.id)
       }
        if(players.isNotEmpty()){
            drawCardForPlayer(players[activeIndex].id)
        }
    }

    fun getPlayers(): List<Player> = players

    fun drawCardForPlayer(playerId: Int) {
        val player = players.find { it.id == playerId }
        val card = deck.removeFirstOrNull()

        if (card != null && player != null) {
            addCardToHand(player,card)
        }
    }

    fun getState(): GameSnapshot {
        val activeId = if (players.isNotEmpty()) players[activeIndex].id else null
        return GameSnapshot(players.toList(), deck, activeId)
    }

    fun makeMove(request: MoveRequest): MoveResult {
        if (players.isEmpty()) {
            return MoveResult.Error("Brak graczy")
        }

        val activePlayer = players[activeIndex]

        return if (activePlayer.id != request.playerId) {
            MoveResult.Error("Nie twoja tura")
        } else {

            val moveCompleted = playTheCard(request)

            val msg = "Gracz ${activePlayer.name} wykonał ruch"
            if (moveCompleted) {
                activeIndex = (activeIndex + 1) % players.size
                drawCardForPlayer(players[activeIndex].id)
            }

            MoveResult.Success(msg, activeIndex)
        }
    }

    private fun playTheCard(request: MoveRequest): Boolean {
        val player = players[request.playerId]

        removeCardFromHand(player, request.card)
        addCardToPlayed(player, request.card)

        return resolveCardEffect(request)
    }

    private fun addCardToPlayed(player: Player, card: Card) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            players[playerIndex] = player.copy(playedCards = player.playedCards + card)
        }
    }

    private fun addCardToHand(player: Player, card: Card) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            players[playerIndex] = player.copy(hand = player.hand + card)
        }
    }

    private fun removeCardFromHand(
        player: Player,
        card: Card
    ) {
        val playerIndex = players.indexOfFirst { it.id == player.id }
        if (playerIndex != -1) {
            players[playerIndex] = player.copy(hand = player.hand - card)
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