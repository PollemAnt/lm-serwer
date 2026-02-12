package com.example.state.handler

import com.example.Strings
import com.example.models.Card
import com.example.models.CardPlayedFeedback
import com.example.models.MoveResult
import com.example.state.ChancellorState
import com.example.state.managers.DeckManager
import com.example.state.managers.PlayerManager

class ChancellorHandler(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager
) {
    private var chancellorState: ChancellorState? = null

    fun getState(): ChancellorState? = chancellorState

    fun drawCardForChancellor(playerId: Int, chancellorCard: Card) {
        playerManager.removeCardFromHand(playerId, chancellorCard)
        playerManager.addPlayedCard(playerId, chancellorCard)

        val drawn = deckManager.drawTwoWithSecretFallback()
        if (drawn.isEmpty()) return

        chancellorState = ChancellorState(
            playerId = playerId,
            drawnCards = drawn
        )

        drawn.forEach { card ->
            playerManager.addCardToHand(playerId, card)
        }
    }

    fun completeChancellorMove(playerId: Int, cardToKeep: Card): MoveResult {
        val state = chancellorState
            ?: return MoveResult.Error(Strings.get("chancellor.no_pending_move"))

        if (state.playerId != playerId) return MoveResult.Error(Strings.get("chancellor.invalid_player_for_move"))

        val player =
            playerManager.findById(playerId)
                ?: return MoveResult.Error(Strings.get("error.player_not_found"))

        if (!player.hand.contains(cardToKeep)) {
            return MoveResult.Error(Strings.get("error.card_not_in_hand"))
        }


        val cardsToDiscard = player.hand - cardToKeep
        deckManager.putOnBottom(cardsToDiscard)

        playerManager.updatePlayer(playerId) { p ->
            p.copy( isProtected = false,hand = listOf(cardToKeep))
        }

        chancellorState = null

        return MoveResult.Success(
            feedback = CardPlayedFeedback.ChancellorPlayed(playerId),
            messageToAll = Strings.format("chancellor.move_finished", playerId),
            nextPlayerId = playerId
        )
    }
}