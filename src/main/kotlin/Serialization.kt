package com.example

import org.example.shared.protocol.CardPlayedEvent
import org.example.shared.protocol.CardPlayedFeedback
import org.example.shared.protocol.GameSnapshotEvent
import org.example.shared.protocol.MoveResult
import org.example.shared.protocol.PlayerJoinEvent
import org.example.shared.protocol.PriestRevealEvent
import org.example.shared.protocol.RoundEndedEvent
import org.example.shared.protocol.ServerEvent
import org.example.shared.protocol.TurnChangedEvent
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
