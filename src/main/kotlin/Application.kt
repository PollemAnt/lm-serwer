package com.example

import com.example.models.CardPlayedFeedback
import com.example.models.MoveResult
import com.example.state.managers.network.GameException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.EngineMain
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*

@Serializable
data class ErrorResponse(
    val message: String
)

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
                polymorphic(CardPlayedFeedback::class) {
                    subclass(CardPlayedFeedback.Standard::class)
                    subclass(CardPlayedFeedback.GuardPlayed::class)
                    subclass(CardPlayedFeedback.BaronPlayed::class)
                    subclass(CardPlayedFeedback.HandmaidPlayed::class)
                    subclass(CardPlayedFeedback.KingPlayed::class)
                    subclass(CardPlayedFeedback.PrincePlayed::class)
                    subclass(CardPlayedFeedback.PriestPlayed::class)
                    subclass(CardPlayedFeedback.SpyPlayed::class)
                    subclass(CardPlayedFeedback.ChancellorPlayed::class)
                }
                polymorphic(MoveResult::class) {
                    subclass(MoveResult.Success::class)
                    subclass(MoveResult.Error::class)
                }
            }
        })
    }

    install(StatusPages) {

        exception<GameException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(cause.message ?: "Błąd gry")
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Wewnętrzny błąd serwera")
            )
        }
    }

    install(WebSockets) {
        pingPeriodMillis = 15000
        timeoutMillis = 15000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    configureRouting()
}
