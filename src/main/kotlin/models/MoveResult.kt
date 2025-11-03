package com.example.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface MoveResult {
    data class Success(
        val feedback: MoveFeedback,
        val messageToAll: String,
        val nextPlayerId: Int,
    ) : MoveResult

    data class Error(val message: String) : MoveResult

    data class ChancellorChoice(
        val message: String,
        val availableCards: List<Card>,
        val nextPlayerId: Int
    ) : MoveResult
}

@Serializable
data class MoveResponse(val ok: String, val nextPlayerId: Int?)

sealed class MoveFeedback {
    data class Standard(val message: String) : MoveFeedback()

    data class SpyPlayed(
        val spyPlayerId: Int
    ) : MoveFeedback()

    data class GuardPlayed(
        val guessingPlayerId: Int,
        val targetPlayerId: Int,
        val guessedCardNumber: Int,
        val wasCorrect: Boolean
    ) : MoveFeedback()

    data class PriestPlayed(
        val viewingPlayerId: Int,
        val targetPlayerId: Int,
        val revealedCardNumber: Int
    ) : MoveFeedback()

    data class BaronPlayed(
        val player1Id: Int,
        val player2Id: Int,
        val winnerId: Int?,
        val loserId: Int?
    ) : MoveFeedback()

    data class HandmaidPlayed(val protectedPlayerId: Int) : MoveFeedback()

    data class PrincePlayed(val playerId: Int, val targetId: Int) : MoveFeedback()

    data class ChancellorPlayed(val playerId: Int, val availableCards: List<Card>) :
        MoveFeedback()

    data class KingPlayed(val player1Id: Int, val player2Id: Int) : MoveFeedback()
}

@Serializable
data class CompleteChancellorRequest(
    val playerId: Int,
    val cardToKeep: Card
)

@Serializable
data class ChancellorChoiceResponse(
    val message: String,
    val availableCards: List<Card>,
    val nextPlayer: Int
)