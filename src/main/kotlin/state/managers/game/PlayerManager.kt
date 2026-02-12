package com.example.state.managers

import com.example.models.Card
import com.example.models.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class PlayerManager {

    private val playersById = ConcurrentHashMap<Int, Player>()
    private val idGenerator = AtomicInteger(0)

    private val playerOrder = mutableListOf<Int>()

    val count: Int
        get() = playersById.size

    fun findById(id: Int): Player? = playersById[id]

    fun exists(id: Int): Boolean = playersById.containsKey(id)


    fun getByIndex(index: Int): Player? {
        if (index !in playerOrder.indices) return null
        val id = playerOrder[index]
        return playersById[id]
    }


    fun getIndexById(id: Int): Int? {
        if (!playersById.containsKey(id)) return null
        return playerOrder.indexOf(id).takeIf { it != -1 }
    }

    fun addPlayer(name: String): Player {
        val player = Player(id = idGenerator.getAndIncrement(), name = name)
        playersById[player.id] = player
        playerOrder.add(player.id)
        return player
    }

    fun getAll(): List<Player> {
        return playerOrder.mapNotNull { id -> playersById[id] }
    }

    fun updatePlayer(playerId: Int, transform: (Player) -> Player) {
        playersById.computeIfPresent(playerId) { _, player ->
            transform(player)
        }
    }

    fun removePlayer(playerId: Int): Player? {
        playerOrder.remove(playerId)
        return playersById.remove(playerId)
    }

    fun reset() {
        playersById.clear()
        playerOrder.clear()
        idGenerator.set(0)
    }

    fun addCardToHand(playerId: Int, card: Card) {
        updatePlayer(playerId) { p ->
            p.copy(hand = p.hand + card)
        }
    }

    fun getPlayerName(id: Int): String =
        playersById[id]?.name ?: ""

    fun getPlayerHand(playerId: Int): List<Card>? =
        playersById[playerId]?.hand

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

    fun onlyOnePlayerIsAlive(): Boolean =
         playersById.values.count { it.isAlive } == 1


    fun getAlivePlayers(): List<Player> =
        playersById.values.filter { it.isAlive }

    fun getSpyPlayers(): List<Player> =
        getAlivePlayers().filter { it.isSpy }

    fun addPoints(playerId: Int) {
        updatePlayer(playerId) { p ->
            p.copy(points = p.points + 1)
        }
    }

    fun restartForNewRound() {
        playersById.replaceAll { _, player ->
            player.copy(
                hand = emptyList(),
                playedCards = emptyList(),
                isSpy = false,
                isProtected = false,
                isAlive = true
            )
        }
    }
}