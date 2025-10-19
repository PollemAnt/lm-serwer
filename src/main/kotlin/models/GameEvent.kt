package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEventBase

/*@Serializable
data class GameEvent(
    val type: String,
    val player: Player? = null,
    val activePlayerId: Int? = null,
    val card: Card? = null
)*/

@Serializable
@SerialName("card_played")
data class CardPlayedEvent(
    val playerId: Int,
    val card: Card,
    val targetPlayerId: Int
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
