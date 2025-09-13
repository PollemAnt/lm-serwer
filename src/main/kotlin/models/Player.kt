package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(val id: Int, val name: String,val hand: MutableList<Card> = mutableListOf())

val players = mutableListOf<Player>()

@Serializable
data class PlayerJoinRequest(val name: String)

val playersJoin = mutableListOf<PlayerJoinRequest>()