package com.example

import com.example.models.CardPlayedEvent
import com.example.models.CardPlayedFeedback
import com.example.models.GameSnapshotEvent
import com.example.models.MoveResult
import com.example.models.PlayerJoinEvent
import com.example.models.PriestRevealEvent
import com.example.models.RoundEndedEvent
import com.example.models.ServerEvent
import com.example.models.TurnChangedEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object JsonConfig {
    val eventModule = SerializersModule {
        polymorphic(CardPlayedFeedback::class) {
            subclass(CardPlayedFeedback.Standard::class)
            subclass(CardPlayedFeedback.SpyPlayed::class)
            subclass(CardPlayedFeedback.GuardPlayed::class)
            subclass(CardPlayedFeedback.PriestPlayed::class)
            subclass(CardPlayedFeedback.BaronPlayed::class)
            subclass(CardPlayedFeedback.HandmaidPlayed::class)
            subclass(CardPlayedFeedback.PrincePlayed::class)
            subclass(CardPlayedFeedback.ChancellorPlayed::class)
            subclass(CardPlayedFeedback.KingPlayed::class)
        }
        polymorphic(ServerEvent::class) {
            subclass(PlayerJoinEvent::class)
            subclass(CardPlayedEvent::class)
            subclass(TurnChangedEvent::class)
            subclass(RoundEndedEvent::class)
            subclass(GameSnapshotEvent::class)
            subclass(PriestRevealEvent::class)
        }
        polymorphic(MoveResult::class) {
            subclass(MoveResult.Success::class)
            subclass(MoveResult.Error::class)
        }
    }

    val json = Json {
        serializersModule = eventModule
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
    }
}
