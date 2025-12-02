package com.example.state.managers

import com.example.models.Player

class TurnManager(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager
) {

    private var activeIndex: Int = 0

    val currentPlayer: Player?
        get() = playerManager.getAll().getOrNull(activeIndex)

    fun setFirstPlayer() {
        activeIndex = 0
    }

    fun advanceTurn() {
        val players = playerManager.getAll()
        if (players.isEmpty()) return

        activeIndex = (activeIndex + 1) % players.size

        val card = deckManager.draw()
        if (card != null) {
            playerManager.addCardToHand(players[activeIndex].id, card)
        }
    }

    fun forceDrawForActivePlayer() {
        val card = deckManager.draw()
        if (card != null && currentPlayer != null) {
            playerManager.addCardToHand(currentPlayer!!.id, card)
        }
    }

    fun getActivePlayerId(): Int? = currentPlayer?.id
}