package com.example

import com.example.models.MoveFeedback
import com.example.models.MoveResult
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.EngineMain
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*

fun main(args: Array<String>) {
    EngineMain.main(args)
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true
    }

    install(ContentNegotiation) {
        json(Json {
            classDiscriminator = "type"
            prettyPrint = true
            isLenient = true

            serializersModule = SerializersModule {
                polymorphic(MoveFeedback::class) {
                    subclass(MoveFeedback.Standard::class)
                    subclass(MoveFeedback.GuardPlayed::class)
                    subclass(MoveFeedback.BaronPlayed::class)
                    subclass(MoveFeedback.HandmaidPlayed::class)
                    subclass(MoveFeedback.KingPlayed::class)
                    subclass(MoveFeedback.PrincePlayed::class)
                    subclass(MoveFeedback.PriestPlayed::class)
                    subclass(MoveFeedback.SpyPlayed::class)
                    subclass(MoveFeedback.ChancellorPlayed::class)
                }
                polymorphic(MoveResult::class) {
                    subclass(MoveResult.Success::class)
                    subclass(MoveResult.Error::class)
                    subclass(MoveResult.ChancellorChoice::class)
                }
            }
        })
    }

    install(WebSockets) {
        pingPeriodMillis = 15000
        timeoutMillis = 15000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    configureRouting()
}
