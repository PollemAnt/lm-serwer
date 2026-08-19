package com.example.state.managers

import com.example.logger.Logger
import org.example.shared.domain.Player
import org.example.shared.domain.RoundSummary

class RoundManager(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager,
    private val turnManager: TurnManager
) {
    private var currentRound = 0
    private var roundWinnerIds = mutableListOf<Int>()
    private var roundSpyId : Int? = null

    fun prepareNewRoundIfNeeded(): Boolean {
        if (isRoundIsEnded()) {
            assignPoints()
            prepareRound()
            return true
        }
        return false
    }

    fun isRoundIsEnded(): Boolean {
        return playerManager.onlyOnePlayerIsAlive() || deckManager.remaining() == 0
    }

    private fun assignPoints() {
        roundWinnerIds.clear()
        addPointsForWinners()
        addPointsForSpy()
    }

    private fun addPointsForWinners() {
        val alivePlayers = playerManager.getAlivePlayers()
        if (alivePlayers.isEmpty()) return

        val winners = if (alivePlayers.size == 1) {
            alivePlayers
        } else {
            comparePlayersCard(alivePlayers)
        }

        winners.forEach { player ->
            playerManager.addPoints(player.id)
            roundWinnerIds.add(player.id)
        }
    }

    private fun addPointsForSpy() {
        val spies = playerManager.getSpyPlayers()

        if (spies.isNotEmpty() && spies.size == 1) {
            playerManager.addPoints(spies[0].id)
            roundSpyId = spies[0].id
        }
    }

    private fun comparePlayersCard(players: List<Player>): List<Player> {
        val maxCardValue = players
            .flatMap { it.hand }
            .maxOfOrNull { it.number } ?: return emptyList()

        val withMaxCard = players.filter { player ->
            player.hand.any { card -> card.number == maxCardValue }
        }

        if (withMaxCard.size == 1) return withMaxCard

        // Tie-breaker: sum of values in playedCards
        val maxSumPlayed = withMaxCard.maxOfOrNull { it.playedCards.sumOf { card -> card.number } } ?: 0
        return withMaxCard.filter { it.playedCards.sumOf { card -> card.number } == maxSumPlayed }
    }

    fun prepareRound() {
        if (currentRound != 0) {
            playerManager.restartForNewRound()
        }
        Logger.debug(" prepareRound ")
        deckManager.resetAndShuffle()
        deckManager.drawSecretCard()
        
        // Start with the last winner (or the first one if multiple)
        turnManager.setFirstPlayer(roundWinnerIds.firstOrNull() ?: 0)

        playerManager.getAll().forEach { player ->
            val card = deckManager.draw()
            if (card != null) playerManager.addCardToHand(player.id, card)
        }
        Logger.debug(" currentRound:$currentRound currentPlayer: ${turnManager.currentPlayer} ")
        turnManager.forceDrawForActivePlayer()
        currentRound++
    }

    fun resetRound() {
        currentRound = 0
        roundWinnerIds.clear()
        roundSpyId = null
    }

    fun getRoundSummary(): RoundSummary {
        return RoundSummary(
            winnerName = roundWinnerIds.joinToString(", ") { playerManager.getPlayerName(it) },
            spyName = roundSpyId?.let { playerManager.getPlayerName(it) },
            scoreTable = playerManager.getAll().associate { it.name to it.points }
        )
    }
}
