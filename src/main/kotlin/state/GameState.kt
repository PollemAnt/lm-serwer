package com.example.state

import com.example.MoveRequest
import com.example.models.Card
import com.example.models.DeckFactory
import com.example.models.GameSnapshot
import com.example.models.MoveFeedback
import com.example.models.MoveResult
import com.example.models.Player
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

object GameState {
    private val players = mutableListOf<Player>()
    private var activeIndex = 0
    private val maxPlayers = 2
    private val idGen = AtomicInteger(0)

    private var deck: MutableList<Card> = mutableListOf()
    private var secretCard: Card? = null

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
        secretCard = deck.removeFirstOrNull()
        activeIndex = 0
        println("[GAME] Ustawiono aktywnego gracza na pozycję 0.")

        players.forEach { player ->
            println("[ACTION] Dobieranie karty startowej dla gracza: ${player.name}")
            drawCardForPlayer(player.id)
        }
        if (players.isNotEmpty()) {
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
            addCardToHand(player, card)
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
        println("[ACTION] Próba wykonania ruchu: Gracz ID ${request.playerId} zagrywa kartę ${request.card.name}")

        // Sprawdź czy trwa wybór karty dla kanclerza
        if (chancellorState != null) {
            return MoveResult.Error("Trwa oczekiwanie na wybór karty przez kanclerza")
        }

        if (players.isEmpty()) {
            return MoveResult.Error("Brak graczy")
        }

        val activePlayer = players[activeIndex]
        if (activePlayer.id != request.playerId) {
            return MoveResult.Error("Nie twoja tura")
        }

        println("[SUCCESS] Tura gracza ${activePlayer.name} potwierdzona.")

        return if (request.card.number == 6) {
            handleChancellorMove(request, activePlayer)
        } else {
            handleNormalMove(activePlayer, request)
        }
    }

    private fun handleNormalMove(
        activePlayer: Player,
        request: MoveRequest
    ): MoveResult {
        val feedback = playTheCard(request)
        val msg = "Gracz ${activePlayer.name} wykonał ruch kartą ${request.card.name}"

        activeIndex = (activeIndex + 1) % players.size
        val nextActivePlayer = players[activeIndex]

        drawCardForPlayer(nextActivePlayer.id)

        return MoveResult.Success(feedback, msg, nextActivePlayer.id)
    }

    private fun playTheCard(request: MoveRequest): MoveFeedback {
        val player = players.find { it.id == request.playerId }
            ?: return MoveFeedback.Standard("Błąd krytyczny: Gracz zniknął w trakcie tury.")

        val playerAfterRemove = removeCardFromHand(player, request.card)

        addCardToPlayed(playerAfterRemove, request.card)

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
    ): Player {
        val playerIndex = players.indexOfFirst { it.id == player.id }

        if (playerIndex != -1) {
            val newHand = player.hand.toMutableList().apply {
                removeAll { it.id == card.id }
            }
            players[playerIndex] = player.copy(hand = newHand)
        }
        return players[playerIndex]
    }

    private fun resolveCardEffect(request: MoveRequest): MoveFeedback {

        val currentPlayer = players.find { it.id == request.playerId }
            ?: return MoveFeedback.Standard("Nie znaleziono gracza")

        val targetPlayer = players.firstOrNull { it.id == request.targetPlayerId } ?: currentPlayer

        val playerIndex = players.indexOfFirst { it.id == currentPlayer.id }
        val targetIndex = players.indexOfFirst { it.id == targetPlayer.id }

        return when (request.card.number) {
            0 -> {
                println("Gracz ${currentPlayer.name} użył karty 0 (Szpieg)")

                players[playerIndex] = currentPlayer.copy(isSpy = true)
                MoveFeedback.SpyPlayed(currentPlayer.id)
            }

            1 -> {
                println("Gracz ${currentPlayer.name} użył karty 1 (Strażnik)")

                val guessedCardNumber = request.guessCardNumber
                if (guessedCardNumber != null) {
                    val opponentCard = targetPlayer.hand.firstOrNull()
                    val wasCorrect = opponentCard?.number == guessedCardNumber
                    if (wasCorrect) {

                        if (targetIndex != -1) {
                            players[targetIndex] = targetPlayer.copy(isAlive = false)
                        }
                    }
                    MoveFeedback.GuardPlayed(
                        currentPlayer.id,
                        targetPlayer.id,
                        guessedCardNumber,
                        wasCorrect
                    )
                } else {
                    MoveFeedback.Standard("Zagrano stażnika, ale wytąpił błąd")
                }
            }

            2 -> {
                println("Gracz ${currentPlayer.name} użył karty 2 (Kapłan)")
                val opponentCard = targetPlayer.hand.firstOrNull()
                if (opponentCard != null) {
                    MoveFeedback.PriestPlayed(
                        currentPlayer.id,
                        targetPlayer.id,
                        opponentCard.number
                    )
                }
                MoveFeedback.Standard("Zagrano Kapłana, ale wystapił błąd")
            }

            3 -> {

                val playerCard = currentPlayer.hand.firstOrNull()
                val opponentCard = targetPlayer.hand.firstOrNull()

                if (playerCard != null && opponentCard != null) {
                    when {
                        playerCard.number < opponentCard.number -> MoveFeedback.BaronPlayed(
                            currentPlayer.id,
                            targetPlayer.id,
                            targetPlayer.id,
                            currentPlayer.id
                        )

                        playerCard.number > opponentCard.number -> MoveFeedback.BaronPlayed(
                            currentPlayer.id,
                            targetPlayer.id,
                            currentPlayer.id,
                            targetPlayer.id
                        )

                        else -> MoveFeedback.BaronPlayed(
                            currentPlayer.id,
                            targetPlayer.id,
                            null,
                            null
                        )
                    }
                } else {
                    MoveFeedback.Standard("Nie udalo się porównać kart")
                }
            }

            4 -> {
                println("Gracz ${currentPlayer.name} jest chroniony do następnej tury.")

                players[playerIndex] = currentPlayer.copy(isProtected = true)
                MoveFeedback.HandmaidPlayed(currentPlayer.id)
            }

            5 -> {
                println("Gracz ${currentPlayer.name} użył Księcia")
                val discardedCard = targetPlayer.hand.firstOrNull()

                if (discardedCard != null) {
                    val playerAfterDiscard = removeCardFromHand(targetPlayer, discardedCard)
                    addCardToPlayed(targetPlayer, discardedCard)

                    checkDiscardedCardEffect(discardedCard, targetPlayer)

                    val newCard = deck.removeFirstOrNull()

                    if (newCard != null) {
                        addCardToHand(playerAfterDiscard, newCard)
                    } else secretCard?.let { addCardToHand(playerAfterDiscard, it) }
                }
                MoveFeedback.PrincePlayed(currentPlayer.id, targetPlayer.id)
            }

            6 -> {
                println("Gracz ${currentPlayer.name} zagrał kanclerza.")

               /* val firstExtraCard = deck.removeFirstOrNull()
                val secondExtraCard = deck.removeFirstOrNull()

                if (firstExtraCard != null) {
                    addCardToHand(currentPlayer, firstExtraCard)
                }
                if (secondExtraCard != null) {
                    addCardToHand(currentPlayer, secondExtraCard)
                }
                // WAŻNE: Ruch nie jest zakończony. Klient musi teraz otrzymać polecenie wyboru karty.
                // Zwracamy specjalny MoveFeedback, który to sygnalizuje.
                MoveFeedback.ChancellorPlayed(
                    playerId = currentPlayer.id,
                    availableCards = players.find { it.id == currentPlayer.id }?.hand
                        ?: emptyList()
                )*/
                MoveFeedback.ChancellorPlayed(
                    playerId = currentPlayer.id,
                    availableCards = emptyList()
                )
            }

            7 -> {
                targetPlayer.let {
                    val playerHand = currentPlayer.hand
                    val tempHand = playerHand

                    if (playerIndex != -1 && targetIndex != -1) {
                        players[playerIndex] = players[playerIndex].copy(hand = it.hand)
                        players[targetIndex] = players[targetIndex].copy(hand = tempHand)
                        println("Gracze ${currentPlayer.name} i ${targetPlayer.name} zamienili się kartami.")
                        MoveFeedback.KingPlayed(currentPlayer.id, targetPlayer.id)
                    } else {
                        MoveFeedback.Standard("Błąd podczas zamiany kart.")
                    }
                }
                MoveFeedback.Standard("Nieprawidłowy cel dla Króla.")

            }

            8 -> {
                println("Gracz ${currentPlayer.name} zagrał kartę  nr 8.")
                MoveFeedback.Standard("Zagrano Hrabinę")
            }

            else -> {
                println("Zagrano kartę o nieznanym numerze: ${request.card.number}")
                MoveFeedback.Standard("Zagrano nieznaną kartę.")
            }
        }
    }

    private fun checkDiscardedCardEffect(
        discardedCard: Card,
        targetPlayer: Player
    ) {
        when (discardedCard.number) {
            9 -> {
                println("[EFFECT] Gracz ${targetPlayer.name} odrzucił Księżniczkę i przegrywa!")
                val targetPlayerIndex =
                    players.indexOfFirst { it.id == targetPlayer.id }
                if (targetPlayerIndex != -1) {
                    players[targetPlayerIndex] =
                        players[targetPlayerIndex].copy(isAlive = false)
                }
            }

            0 -> {
                println("[EFFECT] Gracz ${targetPlayer.name} odrzucił Szpiega. Efekt Szpiega aktywowany.")
                val targetPlayerIndex =
                    players.indexOfFirst { it.id == targetPlayer.id }
                if (targetPlayerIndex != -1) {
                    players[targetPlayerIndex] =
                        players[targetPlayerIndex].copy(isSpy = true)
                }
            }
        }
    }

    private fun handleChancellorMove(request: MoveRequest, player: Player): MoveResult {
        println("[CHANCELLOR] Rozpoczynanie specjalnego ruchu kanclerza")

        // 1. Zagraj kartę kanclerza
        val feedback = playTheCard(request)

        // 2. Dobierz 2 karty
        val drawnCards = listOfNotNull(
            deck.removeFirstOrNull(),
            deck.removeFirstOrNull()
        )

        // 3. Zapisz stan kanclerza
        chancellorState = ChancellorState(
            playerId = player.id,
            drawnCards = drawnCards,
            originalHand = player.hand
        )

        // 4. Dodaj karty do ręki gracza (tymczasowo)
        drawnCards.forEach { card ->
            addCardToHand(player, card)
        }

        // 5. Zwróć informację, że gracz musi wybrać kartę
        val currentPlayerState = players.find { it.id == player.id }
        return MoveResult.ChancellorChoice(
            message = "Wybierz kartę do odrzucenia",
            availableCards = currentPlayerState?.hand ?: emptyList(),
            nextPlayerId = player.id // TURA NIE ZMIENIA SIĘ - ten sam gracz kontynuuje
        )
    }


    fun resetGame() {
        players.clear()
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        activeIndex = 0
        idGen.set(0)
    }

    fun completeChancellorMove(playerId: Int, cardToKeep: Card): MoveResult {
        val state = chancellorState ?: return MoveResult.Error("Brak oczekującego ruchu kanclerza")

        if (state.playerId != playerId) {
            return MoveResult.Error("Nieprawidłowy gracz dla ruchu kanclerza")
        }

        // 1. Znajdź gracza
        val playerIndex = players.indexOfFirst { it.id == playerId }
        if (playerIndex == -1) return MoveResult.Error("Gracz nie znaleziony")

        val player = players[playerIndex]

        // 2. Sprawdź czy wybrana karta jest w ręce
        if (!player.hand.contains(cardToKeep)) {
            return MoveResult.Error("Wybrana karta nie znajduje się w ręce")
        }

        // 3. Zbuduj nową rękę: tylko wybrana karta
        val newHand = listOf(cardToKeep)

        // 4. Odłóż pozostałe karty na spód talii
        val cardsToDiscard = player.hand - cardToKeep
        deck.addAll(cardsToDiscard)

        // 5. Zaktualizuj rękę gracza
        players[playerIndex] = player.copy(hand = newHand)

        // 6. Wyczyść stan kanclerza
        chancellorState = null

        // 7. Zakończ turę i przejdź do następnego gracza
        val previousActivePlayerName = players[activeIndex].name
        activeIndex = (activeIndex + 1) % players.size
        val nextActivePlayer = players[activeIndex]

        println("[CHANCELLOR] Ruch kanclerza zakończony. Następny gracz: ${nextActivePlayer.name}")
        drawCardForPlayer(nextActivePlayer.id)

        return MoveResult.Success(
            feedback = MoveFeedback.Standard("Kanclerz zakończył ruch"),
            messageToAll = "Gracz ${player.name} zakończył ruch kanclerza",
            nextPlayerId = nextActivePlayer.id
        )
    }


    private var chancellorState: ChancellorState? = null

    @Serializable
    data class ChancellorState(
        val playerId: Int,
        val drawnCards: List<Card>, // 2 nowe karty
        val originalHand: List<Card> // ręka przed dobraniem
    )
}


