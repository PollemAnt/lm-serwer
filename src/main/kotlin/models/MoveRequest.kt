package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class MoveRequest(
    val playerId: Int,
    val card: Card,
    val targetPlayerId: Int?,
    val guessCardNumber: Int?
)