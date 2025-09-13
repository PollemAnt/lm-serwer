package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val number: Int,
    val name: String,
    val description: String //co robi
)