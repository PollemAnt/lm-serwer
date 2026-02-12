package com.example.state.managers

import com.example.models.Player

class TurnManager(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager
) {

    private var activeIndex: Int = 0

    val currentPlayer: Player?
        get() = playerManager.getByIndex(activeIndex)

    fun setFirstPlayer(playerId: Int = 0) {
        activeIndex = playerManager.getIndexById(playerId) ?: 0
    }

    fun advanceTurn() {
        val playersCount = playerManager.count
        if (playersCount == 0) return

        activeIndex = (activeIndex + 1) % playersCount

        val card = deckManager.draw()
        if (card != null) {
            currentPlayer?.id?.let { playerId ->
                playerManager.addCardToHand(playerId, card)
            }
        }
    }

    fun forceDrawForActivePlayer() {
        val card = deckManager.draw()
        if (card != null) {
            currentPlayer?.id?.let { playerId ->
                playerManager.addCardToHand(playerId, card)
            }
        }
    }

    fun getActivePlayerId(): Int? = currentPlayer?.id

    fun getNextPlayerId(): Int? {
        val nextIndex = (activeIndex + 1) % playerManager.count
        return playerManager.getByIndex(nextIndex)?.id
    }
}