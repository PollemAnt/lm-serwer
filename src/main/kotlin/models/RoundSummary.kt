package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class RoundSummary(
    val winnerName: String,
    val spyName: String?,
    val scoreTable: Map<String, Int>)