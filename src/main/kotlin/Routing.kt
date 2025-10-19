package com.example

import com.example.com.example.ConnectionManager
import com.example.models.Card
import com.example.models.CardPlayedEvent
import com.example.models.PlayerJoinEvent
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
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Serwer List Miłosny działa!")
        }

        webSocket("/updates") {
            println("Nowe połączenie ${this.hashCode()}")

            ConnectionManager.addConnection(this)

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("Od klienta: $text")
                    }
                }
            } finally {
                println("Rozloczono ${this.hashCode()}")
                ConnectionManager.removeConnection(this)
            }
        }


        post("/join") {
            val request = call.receive<PlayerJoinRequest>()
            val playerAdded = GameState.addPlayer(request.name)

            if (playerAdded != null) {
                ConnectionManager.broadcastEvent(PlayerJoinEvent(player = playerAdded))
                call.respond(playerAdded)
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Maksymalna liczba graczy została osiągnięta")
                )
            }

        }

        get("/state") {
            val json = Json.encodeToString(GameState.getState())
            call.respondText(json, ContentType.Application.Json)
        }


        post("/move") {
            val request = call.receive<MoveRequest>()

            val result = GameState.makeMove(request.playerId)
            when (result) {
                is MoveResult.Success -> {
                    call.respond(
                        MoveResponse(
                            result.message,
                            result.nextPlayer
                        )
                    )
                    ConnectionManager.broadcastEvent(
                        CardPlayedEvent(playerId = request.playerId, card = request.card, targetPlayerId = request.targetPlayerId)
                    )
                }

                is MoveResult.Error -> call.respond(HttpStatusCode.BadRequest, result.message)
            }

        }

        post("/draw") {
            val request = call.receive<DrawRequest>()
            val card = GameState.drawCardForPlayer(request.playerId)
            if (card == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Brak kart w talii albo gracza")
                )
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

        get("/reset") {
            GameState.resetGame()
            call.respond(HttpStatusCode.OK)
        }

        get("/player/{playerId}") {
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

            call.respond(player)
        }
    }
}


@Serializable
data class DrawRequest(val playerId: Int)

@Serializable
data class MoveRequest(val playerId: Int, val card: Card, val targetPlayerId: Int)

@Serializable
data class MoveResponse(val ok: String, val nextPlayerId: Int?)

@Serializable
data class ErrorResponse(val error: String)