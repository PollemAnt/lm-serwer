package com.example.state.handler

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

    fun complete(playerId: Int, cardToKeep: Card): MoveResult {
        val state = chancellorState ?: return MoveResult.Error("Brak oczekującego ruchu kanclerza")
        if (state.playerId != playerId) return MoveResult.Error("Nieprawidłowy gracz dla ruchu kanclerza")

        val player =
            playerManager.findById(playerId) ?: return MoveResult.Error("Gracz nie znaleziony")
        if (!player.hand.contains(cardToKeep)) {
            return MoveResult.Error("Wybrana karta nie znajduje się w ręce")
        }

        val cardsToDiscard = player.hand - cardToKeep
        deckManager.putOnBottom(cardsToDiscard)

        playerManager.updatePlayer(playerId) { p ->
            p.copy(hand = listOf(cardToKeep))
        }

        chancellorState = null

        return MoveResult.Success(
            feedback = CardPlayedFeedback.ChancellorPlayed(playerId),
            messageToAll = "Gracz ${player.name} zakończył ruch kanclerza",
            nextPlayerId = playerId
        )
    }
}