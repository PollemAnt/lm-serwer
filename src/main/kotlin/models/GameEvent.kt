package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEventBase

@Serializable
@SerialName("card_played")
data class CardPlayedEvent(
    val playerId: Int,
    val card: Card,
    val targetPlayerId: Int? = null,
    val feedback: CardPlayedFeedback
) : GameEventBase


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
