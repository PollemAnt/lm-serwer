package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServerEvent

@Serializable
sealed interface GameEventBase :ServerEvent

@Serializable
@SerialName("priest_reveal")
data class PriestRevealEvent(
    val targetPlayerId: Int,
    val revealedCardNumber: Int
) : ServerEvent

@Serializable
@SerialName("snapshot")
data class GameSnapshotEvent(
    val snapshot: GameSnapshot
) : ServerEvent

@Serializable
@SerialName("card_played")
data class CardPlayedEvent(
    val playerId: Int,
    val card: Card,
    val targetPlayerId: Int,
    val feedback: CardPlayedFeedback
) : GameEventBase

@Serializable
@SerialName("round_ended")
data class RoundEndedEvent(
    val roundSummary: RoundSummary
): GameEventBase


@Serializable
@SerialName("player_joined")
data class PlayerJoinEvent(
    val player: Player
) : GameEventBase


@Serializable
@SerialName("turn_changed")
data class TurnChangedEvent(
    val activePlayerId: Int
) : GameEventBase

