package com.example.state

import com.example.models.GameSnapshot
import com.example.models.Player
import java.util.concurrent.atomic.AtomicInteger

object GameState {
    private val players = mutableListOf<Player>()
    private var activeIndex = 0
    private val maxPlayers = 4
    private val idGen = AtomicInteger(1)

    fun addPlayer(name: String): Player? {
        if (players.size >= maxPlayers) return null

        val player = Player(idGen.getAndIncrement(), name)
        players.add(player)

        if (players.size == 1)
            activeIndex = 0

        return player
    }

    fun getState(): GameSnapshot {
        val activeId = if (players.isNotEmpty()) players[activeIndex].id else null
        return GameSnapshot(players.toList(), activeId)
    }

    fun makeMove(playerId: Int): MoveResult {
        if (players.isEmpty()) {
            return MoveResult.Error("Brak graczy")
        }

        val activePlayer = players[activeIndex]

        return if (activePlayer.id != playerId) {
            MoveResult.Error("Nie twoja tura")
        } else {
            val msg = "Gracz ${activePlayer.name} wykonał ruch"
            activeIndex = (activeIndex + 1) % players.size

            MoveResult.Success(msg, activeIndex)
        }
    }

    sealed interface MoveResult {
        data class Success(val message: String,val nextPlayer: Int ) : MoveResult
        data class Error(val message: String) : MoveResult
    }
}