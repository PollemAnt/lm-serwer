package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MoveResult {

    @Serializable
    data class Success(
        val feedback: CardPlayedFeedback,
        val messageToAll: String,
        val nextPlayerId: Int,
        val isRoundEnded: Boolean = false,
        val roundSummary : RoundSummary? = null
    ) : MoveResult

    @Serializable
    data class Error(val message: String) : MoveResult
}

@Serializable
sealed class CardPlayedFeedback {

    @Serializable
    @SerialName("feedback_standard")
    data class Standard(val message: String) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_spy")
    data class SpyPlayed(
        val spyPlayerId: Int
    ) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_guard")
    data class GuardPlayed(
        val guessingPlayerId: Int,
        val targetPlayerId: Int,
        val guessedCardNumber: Int,
        val wasCorrect: Boolean
    ) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_priest")
    data class PriestPlayed(
        val viewingPlayerId: Int,
        val targetPlayerId: Int,
        val revealedCardNumber: Int
    ) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_baron")
    data class BaronPlayed(
        val player1Id: Int,
        val player2Id: Int,
        val winnerId: Int?,
        val loserId: Int?
    ) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_handmaid")
    data class HandmaidPlayed(val protectedPlayerId: Int) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_prince")
    data class PrincePlayed(val playerId: Int, val targetId: Int) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_chancellor")
    data class ChancellorPlayed(val playerId: Int) : CardPlayedFeedback()

    @Serializable
    @SerialName("feedback_king")
    data class KingPlayed(val player1Id: Int, val player2Id: Int) : CardPlayedFeedback()
}
