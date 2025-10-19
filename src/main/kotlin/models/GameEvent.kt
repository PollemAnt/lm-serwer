package com.example.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEventBase {
    val type: String
}

/*@Serializable
data class GameEvent(
    val type: String,
    val player: Player? = null,
    val activePlayerId: Int? = null,
    val card: Card? = null
)*/

@Serializable
data class CardPlayedEvent(
    override val type: String ="card_played",
    val playerId: Int,
    val card: Card,
    val targetPlayerId: Int
) : GameEventBase


@Serializable
data class PlayerJoinEvent(
    override val type: String = "player_joined",
    val player: Player
) : GameEventBase


@Serializable
data class TurnChangedEvent(
    override val type: String = "turn_changed",
    val activePlayerId: Int
) : GameEventBase
