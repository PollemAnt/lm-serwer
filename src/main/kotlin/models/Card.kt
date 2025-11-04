package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val number: Int,
    val name: String,
    val description: String
) {

    override fun toString(): String {
        return "Card(id=$id, number=$number, name=$name)"
    }
}