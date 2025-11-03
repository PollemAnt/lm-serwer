package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val hand: List<Card> = emptyList(),
    var points: Int = 0,
    val playedCards: List<Card> = emptyList(),
    var isSpy: Boolean = false,
    var isProtected: Boolean = false,
    var isAlive: Boolean = true
)

val players = mutableListOf<Player>()

@Serializable
data class PlayerJoinRequest(val name: String)

val playersJoin = mutableListOf<PlayerJoinRequest>()