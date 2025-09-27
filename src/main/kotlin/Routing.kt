package com.example

import com.example.models.PlayerJoinRequest
import com.example.state.GameState
import com.example.state.GameState.MoveResult
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Serwer List Miłosny działa!")
        }

        post("/join") {
            val request = call.receive<PlayerJoinRequest>()
            val playerAdded = GameState.addPlayer(request.name)
            if (playerAdded == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Maksymalna liczba graczy została osiągnięta"))
                return@post
            }
            call.respond(playerAdded)

        }

        get("/state") {
            val json = Json.encodeToString(GameState.getState())
            call.respondText(json, ContentType.Application.Json)
        }

        post("/move") {
            val request = call.receive<MoveRequest>()

            val result = GameState.makeMove(request.playerId)
            when (result) {
                is MoveResult.Success -> call.respond(MoveResponse(result.message, result.nextPlayer))
                is MoveResult.Error -> call.respond(HttpStatusCode.BadRequest, result.message)
            }
        }

        post("/draw") {
            val request = call.receive<DrawRequest>()
            val card = GameState.drawCardForPlayer(request.playerId)
            if (card == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Brak kart w talii albo gracza"))
            } else {
                call.respond(card)
            }
        }

        get("/hand/{playerId}") {
            val playerId = call.parameters["playerId"]?.toIntOrNull()
            if (playerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Niepoprawne ID gracza"))
                return@get
            }

            val player = GameState.getPlayers().find { it.id == playerId }
            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Gracz nie istnieje"))
                return@get
            }

            call.respond(player.hand)
        }
    }
}

@Serializable
data class DrawRequest(val playerId: Int)

@Serializable
data class MoveRequest(val playerId: Int)

@Serializable
data class MoveResponse(val ok: String, val nextPlayerId: Int?)

@Serializable
data class ErrorResponse(val error: String)