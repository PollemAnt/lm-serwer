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
import com.example.state.managers.RoundManager
import com.example.state.managers.TurnManager
import kotlinx.serialization.Serializable

class GameState {
    private val playerManager = PlayerManager()
    private val deckManager = DeckManager()
    private val turnManager = TurnManager(playerManager, deckManager)
    private val chancellorHandler = ChancellorHandler(playerManager, deckManager)
    private val roundManager = RoundManager(playerManager, deckManager, turnManager)
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
        roundManager.prepareRound()
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

        val isRoundEnded = roundManager.prepareNewRoundIfNeeded()

        if (!isRoundEnded) {
            turnManager.advanceTurn()
        }

        return MoveResult.Success(
            feedback = feedback,
            messageToAll = "Gracz ${player.name} wykonał ruch",
            nextPlayerId = turnManager.getActivePlayerId() ?: 0,
            isRoundEnded = isRoundEnded,
            roundSummary = roundManager.getRoundSummary()
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
        roundManager.resetRound()
    }

    fun getPlayers(): List<Player> {
        return playerManager.getAll()
    }

    fun drawCardForChancellor(playerId: Int, card: Card) {
        if (playerId != turnManager.getActivePlayerId()) return
        chancellorHandler.drawCardForChancellor(playerId, card)
    }

    fun getPlayerHandForPriest(playerId: Int, targetId  : Int):List<Card>? {
        if (playerId != turnManager.getActivePlayerId()) return null

        return playerManager.getPlayerHand(targetId)
    }
}


@Serializable
data class ChancellorState(
    val playerId: Int,
    val drawnCards: List<Card>
)
