package com.example.state.managers

import com.example.models.Card
import com.example.models.Player
import java.util.concurrent.atomic.AtomicInteger

class PlayerManager {
    private var players: MutableList<Player> = mutableListOf()
    private var idGenerator = AtomicInteger(0)

    val count: Int
        get() = players.size

    fun getAll(): MutableList<Player> = players

    fun findById(id: Int): Player? =
        players.find { it.id == id }

    fun addPlayer(name: String): Player? {
        val player = Player(id = idGenerator.getAndIncrement(), name = name)
        players.add(player)
        return player
    }

    fun reset() {
        players.clear()
        idGenerator.set(0)
    }

    fun updatePlayer(playerId: Int, transform: (Player) -> Player) {
        players = players.map { p ->
            if (p.id == playerId) transform(p) else p
        }.toMutableList()
    }

    fun addCardToHand(playerId: Int, card: Card) {
        updatePlayer(playerId) { p ->
            p.copy(hand = p.hand + card)
        }
    }

    fun removeCardFromHand(playerId: Int, card: Card) {
        updatePlayer(playerId) { p ->
            p.copy(hand = p.hand.filterNot { it.id == card.id })
        }
    }

    fun addPlayedCard(playerId: Int, card: Card) {
        updatePlayer(playerId) { p ->
            p.copy(playedCards = p.playedCards + card)
        }
    }

    fun onlyOnePlayerIsAlive(): Boolean {
        return players.count { it.isAlive } == 1
    }

    fun getAlivePlayers(): List<Player> {
        return players.filter { it.isAlive }
    }

    fun getSpyPlayers(): List<Player> {
        return getAlivePlayers().filter { it.isSpy }
    }

    fun addPoints(playerId: Int) {
        updatePlayer(playerId) { p ->
            p.copy(points = p.points + 1)
        }
    }

    fun restartForNewRound() {
        players.forEach { player ->
            updatePlayer(player.id) { p ->
                p.copy(
                    hand = emptyList(),
                    playedCards = emptyList(),
                    isSpy = false,
                    isProtected = false,
                    isAlive = true
                )
            }
        }
    }

    fun getPlayerName(id: Int): String {
        return players.find { it.id == id }?.name ?: ""
    }

    fun getPlayerHand(playerId: Int): List<Card>? {
        return players.find { it.id == playerId }?.hand
    }
}