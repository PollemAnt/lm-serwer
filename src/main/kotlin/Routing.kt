package com.example

import com.example.com.example.ConnectionManager
import com.example.models.Card
import com.example.models.CardPlayedEvent
import com.example.models.DrawRequest
import com.example.models.GameConfig
import com.example.models.MoveRequest
import com.example.models.PlayerJoinEvent
import com.example.models.PlayerJoinRequest
import com.example.state.GameState
import com.example.models.MoveResult
import com.example.models.PriestRequest
import com.example.models.RoundEndedEvent
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
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json


fun Application.configureRouting() {

    val gameState = GameState()

    routing {
        get("/") {
            call.respondText(Strings.get("hello.message"), ContentType.Text.Plain)
        }

        webSocket("/updates") {
            ConnectionManager.addConnection(this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Close) break
                }
            } finally {
                ConnectionManager.removeConnection(this)
            }
        }


        post("/join") {
            val request = call.receive<PlayerJoinRequest>()
            val playerAdded = gameState.addPlayer(request.name)

            if (playerAdded != null) {
                ConnectionManager.broadcastEvent(PlayerJoinEvent(player = playerAdded))
                call.respond(playerAdded)
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to Strings.get("lobby.max_players_reached"))
                )
            }

        }

        get("/state") {
            val json = Json.encodeToString(gameState.getState())
            call.respondText(json, ContentType.Application.Json)
        }


        post("/move") {
            val request = call.receive<MoveRequest>()

            val result = gameState.makeMove(request)

            when (result) {
                is MoveResult.Success -> {
                    ConnectionManager.broadcastEvent(
                        CardPlayedEvent(
                            playerId = request.playerId,
                            card = request.card,
                            targetPlayerId = request.targetPlayerId,
                            feedback = result.feedback
                        )
                    )

                    if (result.isRoundEnded) {
                        delay(1000)
                        ConnectionManager.broadcastEvent(
                            RoundEndedEvent(
                                roundSummary = result.roundSummary!!
                            )
                        )
                    }
                }

                is MoveResult.Error -> call.respond(HttpStatusCode.BadRequest, result.message)
                else -> call.respond(HttpStatusCode.BadRequest, Strings.get("error.unexpected_result"))
            }
        }

        post("/complete_chancellor") {
            val request = call.receive<MoveRequest>()

            val result = gameState.completeChancellorMove(request.playerId, request.card)
            when (result) {
                is MoveResult.Success -> {

                    ConnectionManager.broadcastEvent(
                        CardPlayedEvent(
                            playerId = request.playerId,
                            card = Card(
                                id = -1,
                                number = 6,
                                name = Strings.get("card.chancellor"),
                                description = Strings.get("card.action.chancellor")
                            ),
                            targetPlayerId = request.targetPlayerId,
                            feedback = result.feedback
                        )
                    )
                }

                is MoveResult.Error -> call.respond(HttpStatusCode.BadRequest, result.message)
                else -> call.respond(HttpStatusCode.BadRequest, Strings.get("error.unexpected_result"))
            }
        }

        post("/drawCardForChancellor") {
            val request = call.receive<DrawRequest>()
            gameState.drawCardForChancellor(request.playerId, request.card)

            val player = gameState.getPlayers().find { it.id == request.playerId }
            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to Strings.get("error.player_not_found")))
                return@post
            }

            call.respond(player.hand)
        }

        post("/getPlayerHandForPriest") {
            val request = call.receive<PriestRequest>()

            val target = gameState.getPlayers().find { it.id == request.targetId }
            if (target == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to Strings.get("error.player_not_found")))
                return@post
            }

            val targetHand = gameState.getPlayerHandForPriest(request.playerId, request.targetId)

            if (targetHand == null) {

                call.respond(HttpStatusCode.Forbidden, mapOf("error" to Strings.get("error.not_your_turn")))

                return@post
            }

            call.respond(HttpStatusCode.OK, targetHand)
        }

        get("/hand/{playerId}") {
            val playerId = call.parameters["playerId"]?.toIntOrNull()
            if (playerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to Strings.get("error.invalid_player_id")))
                return@get
            }

            val player = gameState.getPlayers().find { it.id == playerId }
            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to Strings.get("error.player_not_found")))
                return@get
            }

            call.respond(player.hand)
        }

        get("/reset") {
            gameState.resetGame()
            call.respond(HttpStatusCode.OK)
        }

        get("/player/{playerId}") {
            val playerId = call.parameters["playerId"]?.toIntOrNull()
            if (playerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to Strings.get("error.invalid_player_id")))
                return@get
            }

            val player = gameState.getPlayers().find { it.id == playerId }
            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to Strings.get("error.player_not_found")))
                return@get
            }

            call.respond(player)
        }

        get("/getGameConfig"){
            call.respond(gameState.getGameConfig())
        }

        post("/updateGameConfig"){
            val request = call.receive<GameConfig>()
            gameState.updateGameConfig(request)
            call.respond(HttpStatusCode.OK)
        }
    }
}