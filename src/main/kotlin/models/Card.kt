package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val number: Int,
    val cardType: CardType,
    val name: String,
    val description: String
) {

    override fun toString(): String {
        return "Card(id=$id, number=$number, name=$name)"
    }
}

enum class CardType {
    SPY,
    GUARD,
    PRIEST,
    BARON,
    HANDMAID,
    PRICE,
    CHANCELLOR,
    KING,
    COUNTESS,
    PRINCESS
}
