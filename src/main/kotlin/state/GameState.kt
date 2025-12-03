package com.example.state

import com.example.MoveRequest
import com.example.models.Card
import com.example.models.GameSnapshot
import com.example.models.MoveResult
import com.example.models.Player
import com.example.state.effects.CardEffectResolver
import com.example.state.handler.ChancellorHandler
import com.example.state.managers.DeckManager
import com.example.state.managers.PlayerManager
import com.example.state.managers.TurnManager
import kotlinx.serialization.Serializable

class GameState {
    private val playerManager = PlayerManager()
    private val deckManager = DeckManager()
    private var turnManager: TurnManager = TurnManager(playerManager, deckManager)
    private val chancellorHandler = ChancellorHandler(playerManager, deckManager)
    private val maxPlayers = 2
    private var moveValidator: MoveValidator = MoveValidator(
        playerManager,
        turnManager
    ) { chancellorHandler.getState() }

    fun addPlayer(name: String): Player? {
        val player = playerManager.addPlayer(name) ?: return null
        if (playerManager.count == maxPlayers) startGame()
        return player
    }

    private fun startGame() {
        deckManager.resetAndShuffle()
        deckManager.drawSecretCard()
        turnManager.setFirstPlayer()

        playerManager.getAll().forEach { player ->
            val card = deckManager.draw()
            if (card != null) playerManager.addCardToHand(player.id, card)
        }

        turnManager.forceDrawForActivePlayer()
    }

    fun getState(): GameSnapshot {
        return GameSnapshot(
            players = playerManager.getAll(),
            cards = deckManager.getDeckSnapshot(),
            activePlayerId = turnManager.getActivePlayerId() ?: 0
        )
    }

    fun makeMove(request: MoveRequest): MoveResult {
        moveValidator.validate(request.playerId)?.let { return it }

        val player = playerManager.findById(request.playerId)
            ?: return MoveResult.Error("Gracz nie istnieje")

        playerManager.removeCardFromHand(player.id, request.card)
        playerManager.addPlayedCard(player.id, request.card)

        val feedback = CardEffectResolver(
            playerManager.getAll(),
            deckManager
        ).resolve(request)

        turnManager.advanceTurn()

        return MoveResult.Success(
            feedback,
            "Gracz ${player.name} wykonał ruch",
            turnManager.getActivePlayerId() ?: 0
        )
    }

    fun completeChancellorMove(playerId: Int, cardToKeep: Card): MoveResult {
        val result = chancellorHandler.complete(playerId, cardToKeep)

        if (result is MoveResult.Success) {
            turnManager.advanceTurn()
        }

        return result
    }

    fun resetGame() {
        playerManager.reset()
        deckManager.resetAndShuffle()
        deckManager.drawSecretCard()
        turnManager.setFirstPlayer()
    }

    fun getPlayers(): List<Player> {
       return playerManager.getAll()
    }

    fun drawCardForChancellor(playerId: Int, card: Card) {
        chancellorHandler.drawCardForChancellor(playerId, card)
    }
}


@Serializable
data class ChancellorState(
    val playerId: Int,
    val drawnCards: List<Card>
)
