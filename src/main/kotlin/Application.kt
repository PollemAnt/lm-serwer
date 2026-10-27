package com.example

import com.example.state.managers.network.GameException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable
import org.slf4j.event.Level as SLF4JLevel

import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

@Serializable
data class ErrorResponse(
    val message: String
)

@Serializable
data class UserSession(val playerId: Int)

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 // 1 godzina
        }
    }

    install(CallLogging) {
        level = SLF4JLevel.INFO
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    install(ContentNegotiation) {
        json(JsonConfig.json)
    }

    install(StatusPages) {

        exception<GameException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
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
