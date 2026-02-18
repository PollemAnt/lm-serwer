package com.example.state.managers.network

import com.example.Strings
import com.example.models.Card
import com.example.models.CardPlayedEvent
import com.example.models.DrawRequest
import com.example.models.GameConfig
import com.example.models.GameSnapshot
import com.example.models.GameSnapshotEvent
import com.example.models.MoveRequest
import com.example.models.MoveResult
import com.example.models.Player
import com.example.models.PlayerJoinEvent
import com.example.models.PriestRequest
import com.example.models.RoundEndedEvent
import com.example.state.GameState
import kotlinx.coroutines.delay
import state.managers.network.ConnectionManager


class GameException(message: String) : RuntimeException(message)

class GameService(
    private val gameState: GameState,
    private val connectionManager: ConnectionManager
) {

    suspend fun addPlayer(playerName: String): Player {

        require(playerName.isNotBlank()) { "Player name cannot be blank" }
        require(playerName.length <= 20) { "Player name too long" }

        val playerAdded = gameState.addPlayer(playerName)

        if (playerAdded != null) {
            broadcastPlayerJoin(playerAdded)

        } else {
            throw GameException(Strings.get("lobby.max_players_reached"))
        }

        return playerAdded
    }

    suspend fun handleMove(request: MoveRequest) {
        when (val result = gameState.makeMove(request)) {

            is MoveResult.Success -> {
                broadcastCardPlayed(request, result)
                broadcastSnapshot()
                if (result.isRoundEnded) {
                    handleRoundEnd(result)
                }
            }

            is MoveResult.Error -> {
                throw GameException(result.message)
            }
        }
    }

    private suspend fun broadcastSnapshot() {
        connectionManager.broadcastEvent(
            GameSnapshotEvent(gameState.getState())
        )
    }

    private suspend fun handleRoundEnd(result: MoveResult.Success) {
        delay(1000)

        val roundSummary = result.roundSummary
            ?: run {
                throw GameException("Round summary missing")
            }

        connectionManager.broadcastEvent(RoundEndedEvent(roundSummary))
    }

    suspend fun completeChancellorMove(request: MoveRequest) {
        when (val result = gameState.completeChancellorMove(request.playerId, request.card)) {

            is MoveResult.Success -> {

                val chancellorCard = Card(
                    id = -1,
                    number = 6,
                    name = Strings.get("card.chancellor"),
                    description = Strings.get("card.action.chancellor")
                )

                broadcastCardPlayed(request, result, chancellorCard)

            }

            is MoveResult.Error -> {
                throw GameException(result.message)
            }
        }

    }

    fun getPlayer(playerId: Int?): Player {
        requireNotNull(playerId) { Strings.get("error.invalid_player_id") }

        return gameState.getPlayerById(playerId)
            ?: throw GameException(Strings.get("error.player_not_found"))
    }

    fun drawCardsForChancellor(request: DrawRequest): List<Card> {
        gameState.drawCardForChancellor(request.playerId, request.card)

        val player = getPlayer(request.playerId)

        return player.hand
    }

    fun getPlayerHand(
        request: PriestRequest
    ): List<Card> {

        if (!gameState.playerExists(request.targetId)) {
            throw GameException(Strings.get("error.player_not_found"))
        }

        val targetHand = gameState.getPlayerHandForPriest(
            request.playerId,
            request.targetId
        ) ?: throw GameException(Strings.get("error.not_your_turn"))

        return targetHand
    }

    fun updateGameConfig(config: GameConfig) {
        gameState.updateGameConfig(config)
    }

    fun getGameConfig(): GameConfig {
        return gameState.getGameConfig()
    }

    fun getState(): GameSnapshot {
        return gameState.getState()
    }

    private suspend fun broadcastCardPlayed(
        request: MoveRequest,
        result: MoveResult.Success,
        card: Card? = null,
    ) {
        connectionManager.broadcastEvent(
            CardPlayedEvent(
                playerId = request.playerId,
                card = card ?: request.card,
                targetPlayerId = request.targetPlayerId ?: -1,
                feedback = result.feedback
            )
        )
    }

    private suspend fun broadcastPlayerJoin(
        playerAdded: Player
    ) {
        connectionManager.broadcastEvent(
            PlayerJoinEvent(
                player = playerAdded
            )
        )
    }
}

