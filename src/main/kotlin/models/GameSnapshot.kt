package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class GameSnapshot(
    val players: List<Player>,
    val activePlayerId: Int?
)

val gameSnapshots = mutableListOf<GameSnapshot>()