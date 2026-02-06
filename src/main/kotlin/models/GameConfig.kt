package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val maxPlayers: Int,
    val deckComposition: List<CardCount>
)

@Serializable
data class CardCount(
    val cardName: String,
    val cardNumber: Int,
    val count: Int
)