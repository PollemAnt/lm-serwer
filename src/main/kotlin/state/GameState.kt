package com.example.state

import com.example.Strings
import com.example.logger.Logger
import com.example.models.Card
import com.example.models.CardPlayedFeedback
import com.example.models.GameConfig
import com.example.models.GameSnapshot
import com.example.models.MoveRequest
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

    private var gameConfig = GameConfig(2, deckManager.deckComposition)

    private var moveValidator: MoveValidator = MoveValidator(
        playerManager,
        turnManager
    ) { chancellorHandler.getState() }

    fun getPlayerById(id: Int): Player? = playerManager.findById(id)
    fun playerExists(id: Int): Boolean = playerManager.exists(id)
    fun getPlayerName(id: Int): String = playerManager.getPlayerName(id)

    fun updateGameConfig(config: GameConfig) {
        gameConfig = config
        deckManager.updateDeckComposition(gameConfig.deckComposition)
    }

    fun getGameConfig(): GameConfig = gameConfig

    fun addPlayer(name: String): Player? {
        var player: Player? = null
        if (playerManager.count < gameConfig.maxPlayers) {
            player = playerManager.addPlayer(name)
        }
        Logger.debug("👤 Dodawanie gracza $name count: ${playerManager.count} maxPlayers:${gameConfig.maxPlayers}")
        if (playerManager.count == gameConfig.maxPlayers) {
            startGame()

            return player?.id?.let { playerManager.findById(it) }
        }
        return player
    }

    //For tests
    fun startGameForTest() = startGame()

    private fun startGame() {
        Logger.debug(" startGame ${playerManager.count}  maxPlayers:${gameConfig.maxPlayers}")
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

        val player = getPlayerById(request.playerId)
            ?: return MoveResult.Error(Strings.get("error.player_not_found"))

        if (request.card !in player.hand)
            return MoveResult.Error(Strings.get("error.card_not_in_hand"))

        playerManager.removeCardFromHand(player.id, request.card)
        playerManager.addPlayedCard(player.id, request.card)

        val feedback = CardEffectResolver(
            playerManager,
            deckManager
        ).resolve(request)

        if (feedback is CardPlayedFeedback.Standard && feedback.message == Strings.get("error.cannot_target_self")) {
            // Revert changes if validation failed inside resolver (this is a bit hacky, but avoids duplicate validation logic)
            // Actually, better to just return Error here so GameService doesn't broadcast anything.
            playerManager.addCardToHand(player.id, request.card)
            playerManager.removePlayedCard(player.id, request.card)
            return MoveResult.Error(feedback.message)
        }

        val isRoundEnded = roundManager.prepareNewRoundIfNeeded()

        if (isRoundEnded) {
            // Po rozdaniu nowej rundy potrzebujemy wysłać snapshot, żeby gracze widzieli nowe karty
            return MoveResult.Success(
                feedback = feedback,
                messageToAll = Strings.format("game.player_move", player.name),
                nextPlayerId = turnManager.getActivePlayerId() ?: 0,
                isRoundEnded = isRoundEnded,
                roundSummary = roundManager.getRoundSummary()
            )
        }

        turnManager.advanceTurn()

        return MoveResult.Success(
            feedback = feedback,
            messageToAll = Strings.format("game.player_move", player.name),
            nextPlayerId = turnManager.getActivePlayerId() ?: 0,
            isRoundEnded = isRoundEnded,
            roundSummary = roundManager.getRoundSummary()
        )
    }

    fun completeChancellorMove(playerId: Int, cardToKeep: Card): MoveResult {
        val result = chancellorHandler.completeChancellorMove(playerId, cardToKeep)

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

    fun drawCardForChancellor(playerId: Int, card: Card) {
        if (playerId != turnManager.getActivePlayerId()) return
        chancellorHandler.drawCardForChancellor(playerId, card)
    }

    fun getPlayerHandForPriest(playerId: Int, targetId: Int): List<Card>? {
        if (playerId != turnManager.getActivePlayerId()) return null
        return playerManager.getPlayerHand(targetId)
    }

    // For tests
    fun setPlayerProtected(playerId: Int, isProtected: Boolean) {
        playerManager.updatePlayer(playerId) { it.copy(isProtected = isProtected) }
    }

    fun setPlayerHand(playerId: Int, hand: List<Card>) {
        playerManager.updatePlayer(playerId) { it.copy(hand = hand) }
    }
}

@Serializable
data class ChancellorState(
    val playerId: Int,
    val drawnCards: List<Card>
)
