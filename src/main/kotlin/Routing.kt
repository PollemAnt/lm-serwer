package com.example

import com.example.models.Player
import com.example.models.PlayerJoinRequest
import com.example.state.GameState
import com.example.state.GameState.MoveResult
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Serwer List Miłosny działa!")
        }

        post("/join") {
            val request = call.receive<PlayerJoinRequest>()
            val playerAdded = GameState.addPlayer(request.name)
            if (!playerAdded) {
                call.respond(HttpStatusCode.BadRequest, "Maksymalna liczba graczy została osiągnięta")
                return@post
            }
            call.respond(HttpStatusCode.OK, "Dodano gracza o nazwie ${request.name}")
        }

        get("/state") {
            val json = Json.encodeToString(GameState.getState())
            call.respondText(json, ContentType.Application.Json)
        }

        post("/move") {
            val request = call.receive<MoveRequest>()
            //call.respond(GameState.makeMove(request.playerId))
            //call.respondText(Json.encodeToString(GameState.makeMove(request.playerId)), ContentType.Application.Json)


            val result = GameState.makeMove(request.playerId)
            when (result) {
                is MoveResult.Success -> call.respond(MoveResponse(result.message, result.nextPlayer))
                is MoveResult.Error -> call.respond(HttpStatusCode.BadRequest, result.message)
            }
        }
    }
}

@Serializable
data class MoveRequest(val playerId: Int)

@Serializable
data class MoveResponse(val ok: String, val nextPlayerId: Int?)

@Serializable
data class ErrorResponse(val error: String)