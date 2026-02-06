package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class PriestRequest(val playerId: Int, val targetId: Int)