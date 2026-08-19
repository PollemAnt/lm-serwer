package com.example.state.managers.network

import com.example.Strings
import com.example.logger.Logger
import com.example.state.GameState
import kotlinx.coroutines.delay
import org.example.shared.domain.Card
import org.example.shared.domain.CardType
import org.example.shared.domain.GameConfig
import org.example.shared.domain.GameSnapshot
import org.example.shared.domain.Player
import org.example.shared.protocol.CardPlayedEvent
import org.example.shared.protocol.CardPlayedFeedback
import org.example.shared.protocol.DrawRequest
import org.example.shared.protocol.GameSnapshotEvent
import org.example.shared.protocol.MoveRequest
import org.example.shared.protocol.MoveResult
import org.example.shared.protocol.PlayerJoinEvent
import org.example.shared.protocol.PriestRevealEvent
import org.example.shared.protocol.RoundEndedEvent
import org.example.shared.protocol.TurnChangedEvent
import kotlin.time.Duration.Companion.milliseconds


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

            broadcastSnapshot()

        } else {
            throw GameException(Strings.get("lobby.max_players_reached"))
        }

        return playerAdded
    }

    private suspend fun sendPrivateEvent(
        playerId: Int,
        event: PriestRevealEvent
    ) {
        connectionManager.sendToPlayer(playerId, event)
    }

    suspend fun handleMove(request: MoveRequest) {
        when (val result = gameState.makeMove(request)) {

            is MoveResult.Success -> {

                when (val feedback = result.feedback) {

                    is CardPlayedFeedback.PriestPlayed -> {

                        sendPrivateEvent(
                            playerId = feedback.viewingPlayerId,
                            event = PriestRevealEvent(
                                targetPlayerId = feedback.targetPlayerId,
                                revealedCardNumber = feedback.revealedCardNumber
                            )
                        )


                        broadcastCardPlayed(request, result)
                    }

                    else -> {
                        broadcastCardPlayed(request, result)
                    }
                }
                broadcastSnapshot()
                broadcastTurnChanged(result.nextPlayerId)

                if (result.isRoundEnded) {
                    broadcastSnapshot() // Wyślij nowe karty po zakończeniu rundy
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
        delay(1000.milliseconds)

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
                    cardType = CardType.CHANCELLOR,
                    name = Strings.get("card.chancellor"),
                    description = Strings.get("card.action.chancellor")
                )

                broadcastCardPlayed(request, result, chancellorCard)
                broadcastSnapshot()
                broadcastTurnChanged(result.nextPlayerId)
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
        val event = CardPlayedEvent(
            playerId = request.playerId,
            card = card ?: request.card,
            targetPlayerId = request.targetPlayerId ?: -1,
            feedback = result.feedback
        )
        Logger.info("📢 Rozsyłanie CardPlayedEvent: Gracz ${request.playerId} zagrał ${event.card.name}")
        connectionManager.broadcastEvent(event)
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

    private suspend fun broadcastTurnChanged(nextPlayerId: Int) {
        connectionManager.broadcastEvent(
            TurnChangedEvent(nextPlayerId)
        )
    }
}

