package com.example.state

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
            return MoveResult.Error("Brak graczy")
        }

        val chState = chancellorStateProvider()
        if (chState != null) {
            return MoveResult.Error("Trwa oczekiwanie na wybór karty przez kanclerza")
        }

        val activeId = turnManager.getActivePlayerId()
        if (activeId != playerId) {
            return MoveResult.Error("Nie twoja tura")
        }

        return null
    }
}