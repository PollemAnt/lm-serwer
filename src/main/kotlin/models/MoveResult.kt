package com.example.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface MoveResult {

    @Serializable
    data class Success(
        val feedback: CardPlayedFeedback,
        val messageToAll: String,
        val nextPlayerId: Int,
    ) : MoveResult

    @Serializable
    data class Error(val message: String) : MoveResult
}

@Serializable
sealed class CardPlayedFeedback {

    @Serializable
    data class Standard(val message: String) : CardPlayedFeedback()

    @Serializable
    data class SpyPlayed(
        val spyPlayerId: Int
    ) : CardPlayedFeedback()

    @Serializable
    data class GuardPlayed(
        val guessingPlayerId: Int,
        val targetPlayerId: Int,
        val guessedCardNumber: Int,
        val wasCorrect: Boolean
    ) : CardPlayedFeedback()

    @Serializable
    data class PriestPlayed(
        val viewingPlayerId: Int,
        val targetPlayerId: Int,
        val revealedCardNumber: Int
    ) : CardPlayedFeedback()

    @Serializable
    data class BaronPlayed(
        val player1Id: Int,
        val player2Id: Int,
        val winnerId: Int?,
        val loserId: Int?
    ) : CardPlayedFeedback()

    @Serializable
    data class HandmaidPlayed(val protectedPlayerId: Int) : CardPlayedFeedback()

    @Serializable
    data class PrincePlayed(val playerId: Int, val targetId: Int) : CardPlayedFeedback()

    @Serializable
    data class ChancellorPlayed(val playerId: Int) : CardPlayedFeedback()

    @Serializable
    data class KingPlayed(val player1Id: Int, val player2Id: Int) : CardPlayedFeedback()
}