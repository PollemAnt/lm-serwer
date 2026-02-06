package com.example.state

import com.example.Strings
import com.example.models.MoveResult
import com.example.state.managers.PlayerManager
import com.example.state.managers.TurnManager

class MoveValidator(
    private val playerManager: PlayerManager,
    private val turnManager: TurnManager,
    private val chancellorStateProvider: () -> ChancellorState?
) {

    fun validate(playerId: Int): MoveResult.Error? {
        val players = playerManager.getAll()
        if (players.isEmpty()) {
            return MoveResult.Error(Strings.get("error.no_players"))
        }

        val chState = chancellorStateProvider()
        if (chState != null) {
            return MoveResult.Error(Strings.get("chancellor.waiting_for_card_selection"))
        }

        val activeId = turnManager.getActivePlayerId()
        if (activeId != playerId) {
            return MoveResult.Error(Strings.get("error.not_your_turn"))
        }

        return null
    }
}