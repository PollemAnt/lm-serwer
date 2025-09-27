package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class GameSnapshot(
    val players: List<Player>,
    val cards: List<Card>,
    val activePlayerId: Int?
)

val gameSnapshots = mutableListOf<GameSnapshot>()