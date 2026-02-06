package com.example.models

import kotlinx.serialization.Serializable


@Serializable
data class DrawRequest(val playerId: Int, val card: Card)

