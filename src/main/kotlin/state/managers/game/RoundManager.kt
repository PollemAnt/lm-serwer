package com.example.state.managers

import com.example.models.Player
import com.example.models.RoundSummary

class RoundManager(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager,
    private val turnManager: TurnManager
) {
    private var currentRound = 0
    private var roundWinnerId = 0
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
        addPointsForHighestCard()
        addPointsForSpy()
    }

    private fun addPointsForHighestCard() {
        val alivePlayers = playerManager.getAlivePlayers()
        val playersWithTheHighestCard = comparePlayersCard(alivePlayers)

        playersWithTheHighestCard.forEach { player ->
            playerManager.addPoints(player.id)
            roundWinnerId = player.id
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

        return players.filter { player ->
            player.hand.any { card -> card.number == maxCardValue }
        }
    }

    fun prepareRound() {
        //checkIsGameEnded()
        if (currentRound != 0) {
            playerManager.restartForNewRound()
        }
        deckManager.resetAndShuffle()
        deckManager.drawSecretCard()
        turnManager.setFirstPlayer(roundWinnerId)

        playerManager.getAll().forEach { player ->
            val card = deckManager.draw()
            if (card != null) playerManager.addCardToHand(player.id, card)
        }

        turnManager.forceDrawForActivePlayer()
        currentRound++
    }

    fun resetRound() {
        currentRound = 0
        roundWinnerId = 0
        roundSpyId = null
    }

    fun getRoundSummary(): RoundSummary {
        return RoundSummary(
            winnerName = playerManager.getPlayerName(roundWinnerId),
            spyName = roundSpyId?.let { playerManager.getPlayerName(it) },
            scoreTable = playerManager.getAll().associate { it.name to it.points }
        )
    }
}