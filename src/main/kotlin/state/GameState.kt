package com.example.state

import CardEffectResolver
import com.example.MoveRequest
import com.example.models.Card
import com.example.models.DeckFactory
import com.example.models.GameSnapshot
import com.example.models.CardPlayedFeedback
import com.example.models.MoveResult
import com.example.models.Player
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

object GameState {
    private var players = mutableListOf<Player>()
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
            addCardToHand(player.id, card)
        } else {
            if (player == null) println("[WARNING] Nie znaleziono gracza o ID: $playerId")
            if (card == null) println("[WARNING] Talia jest pusta. Nie można dobrać karty.")
        }
    }

    fun getState(): GameSnapshot {
        val activeId = if (players.isNotEmpty()) players[activeIndex].id else null
        return GameSnapshot(players.toList(), deck, activeId)
    }

    private fun validateMove(request: MoveRequest): MoveResult.Error? {
        println("[VALIDATE] START validation for player ${request.playerId}")
        val activePlayer = players[activeIndex]
        println("[VALIDATE] Active player: ${activePlayer.id}")
        println("[ACTION] Próba walidacji ruchu: Gracz ID ${request.playerId} aktywny gracz : ${activePlayer.name} gracze: ${players.size}")

        if (chancellorState != null) {
            println("[VALIDATE] FAIL: Chancellor state not null")
            return MoveResult.Error("Trwa oczekiwanie na wybór karty przez kanclerza")
        }
        println("[VALIDATE] PASS: No chancellor state")

        if (players.isEmpty()) {
            println("[VALIDATE] FAIL: No players")
            return MoveResult.Error("Brak graczy")
        }

        println("[VALIDATE] PASS: Players exist")

        if (activePlayer.id != request.playerId) {
            println("[VALIDATE] FAIL: Wrong turn")
            return MoveResult.Error("Nie twoja tura")
        }
        println("[VALIDATE] PASS: Correct turn")

        println("[VALIDATE] END validation - SUCCESS")
        println("[ACTION] Próba walidacji ruchu: Gracz ID ${request.playerId} moze wykonac ruch")
        return null
    }

    fun makeMove(request: MoveRequest): MoveResult {
        println("[ACTION] Próba wykonania ruchu: Gracz ID ${request.playerId} zagrywa kartę ${request.card.name}")

        validateMove(request)?.let { return it }

        val activePlayer = players[activeIndex]
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
        println("[ACTION] Gracz ${activePlayer.name} wykonuje ruch")
        val feedback = playTheCard(request)
        val msg = "Gracz ${activePlayer.name} wykonał ruch kartą ${request.card.name}"

        activeIndex = (activeIndex + 1) % players.size
        val nextActivePlayer = players[activeIndex].also{
            drawCardForPlayer(it.id)
        }
        println("[ACTION] Feedback po ruchu gracza $feedback")
        return MoveResult.Success(feedback, msg, nextActivePlayer.id)
    }

    private fun playTheCard(request: MoveRequest): CardPlayedFeedback {
        val player = players.find { it.id == request.playerId }
            ?: return CardPlayedFeedback.Standard("Błąd krytyczny: Gracz zniknął w trakcie tury.")

        println("[ACTION] Gracz ${player.name} wykonuje ruch")
        removeCardFromHand(player.id, request.card)
        addCardToPlayed(player.id, request.card)

        return resolveCardEffect(request)
    }

    private fun updatePlayerState(playerId: Int, transform: (Player) -> Player) {
        players = players.map { if (it.id == playerId) transform(it) else it }.toMutableList()
    }

    private fun addCardToPlayed(playerId: Int, card: Card) {
        println("[ACTION] Gracz ${players.find { it.id == playerId }?.name} dodaje kartę ${card.name} do zagranych")
        updatePlayerState(playerId) {
            it.copy(playedCards = it.playedCards + card)
        }
    }

    private fun removeCardFromHand(playerId: Int, card: Card) {
        println("[ACTION] Gracz ${players.find { it.id == playerId }?.name} gra kartę ${card.name} z ręki")
        updatePlayerState(playerId) {
            it.copy(hand = it.hand.filterNot { handCard -> handCard.id == card.id })
        }
    }

    private fun addCardToHand(playerId: Int, card: Card) {
        updatePlayerState(playerId) {
            it.copy(hand = it.hand + card)
        }
    }

    private fun resolveCardEffect(request: MoveRequest): CardPlayedFeedback {
        val resolver = CardEffectResolver(players, deck, secretCard)
        val feedback = resolver.resolve(request)

        return feedback
    }

    private fun handleChancellorMove(request: MoveRequest, player: Player): MoveResult {
        println("[CHANCELLOR] Rozpoczynanie specjalnego ruchu kanclerza")


        val feedback = playTheCard(request)

        val drawnCards = listOfNotNull(
            deck.removeFirstOrNull(),
            deck.removeFirstOrNull()
        )

        chancellorState = ChancellorState(
            playerId = player.id,
            drawnCards = drawnCards,
            originalHand = player.hand
        )

        drawnCards.forEach { card ->
            addCardToHand(player.id, card)
        }

        return MoveResult.ChancellorChoice(
            feedback = feedback,
            nextPlayerId = player.id
        )
    }


    fun resetGame() {
        players.clear()
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        activeIndex = 0
        chancellorState = null
        idGen.set(0)
    }

    fun completeChancellorMove(playerId: Int, cardToKeep: Card): MoveResult {
        val state = chancellorState ?: return MoveResult.Error("Brak oczekującego ruchu kanclerza")

        if (state.playerId != playerId) {
            return MoveResult.Error("Nieprawidłowy gracz dla ruchu kanclerza")
        }


        val playerIndex = players.indexOfFirst { it.id == playerId }
        if (playerIndex == -1) return MoveResult.Error("Gracz nie znaleziony")

        val player = players[playerIndex]

        if (!player.hand.contains(cardToKeep)) {
            return MoveResult.Error("Wybrana karta nie znajduje się w ręce")
        }


        val newHand = listOf(cardToKeep)


        val cardsToDiscard = player.hand - cardToKeep
        deck.addAll(cardsToDiscard)


        players[playerIndex] = player.copy(hand = newHand)


        chancellorState = null


        val previousActivePlayerName = players[activeIndex].name
        activeIndex = (activeIndex + 1) % players.size
        val nextActivePlayer = players[activeIndex]

        println("[CHANCELLOR] Ruch gracza $previousActivePlayerName zakończony. Następny gracz: ${nextActivePlayer.name}")
        drawCardForPlayer(nextActivePlayer.id)

        return MoveResult.Success(
            feedback = CardPlayedFeedback.Standard("Kanclerz zakończył ruch"),
            messageToAll = "Gracz ${player.name} zakończył ruch kanclerza",
            nextPlayerId = nextActivePlayer.id
        )
    }

    private var chancellorState: ChancellorState? = null

}

@Serializable
data class ChancellorState(
    val playerId: Int,
    val drawnCards: List<Card>,
    val originalHand: List<Card>
)
