package com.example

import com.example.models.DrawRequest
import com.example.models.GameConfig
import com.example.models.MoveRequest
import com.example.models.PlayerJoinRequest
import com.example.state.GameState
import com.example.models.PriestRequest
import com.example.state.managers.network.GameException
import com.example.state.managers.network.GameService
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
import kotlinx.serialization.json.Json
import state.managers.network.ConnectionManager


fun Application.configureRouting() {

    val gameState = GameState()
    val gameService = GameService(gameState, ConnectionManager)

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

            try {
                val playerAdded = gameService.addPlayer(request.name)
                call.respond(HttpStatusCode.OK, playerAdded)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message)
                )
            }
        }

        get("/state") {
            val json = Json.encodeToString(gameService.getState())
            call.respondText(json, ContentType.Application.Json)
        }


        post("/move") {
            val request = call.receive<MoveRequest>()

            try {
                gameService.handleMove(request)
                call.respond(HttpStatusCode.OK)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message)
                )
            }
        }

        post("/complete_chancellor") {
            val request = call.receive<MoveRequest>()

            try {
                gameService.completeChancellorMove(request)
                call.respond(HttpStatusCode.OK)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message)
                )
            }
        }

        post("/drawCardForChancellor") {
            val request = call.receive<DrawRequest>()

            try {
                val hand = gameService.drawCardsForChancellor(request)
                call.respond(HttpStatusCode.OK, hand)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message)
                )
            }
        }

        post("/getPlayerHandForPriest") {
            val request = call.receive<PriestRequest>()

            try {
                val hand = gameService.getPlayerHand(request)
                call.respond(HttpStatusCode.OK, hand)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to e.message)
                )
            }
        }

        get("/player/{playerId}") {
            val playerId = call.parameters["playerId"]?.toIntOrNull()

            try {
                val player = gameService.getPlayer(playerId)
                call.respond(player)
            } catch (e: GameException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to e.message)
                )
            }

        }

        get("/getGameConfig") {
            call.respond(gameService.getGameConfig())
        }

        post("/updateGameConfig") {
            val request = call.receive<GameConfig>()
            gameService.updateGameConfig(request)
            call.respond(HttpStatusCode.OK)

        }

        //Temporary
        get("/reset") {
            gameState.resetGame()
            call.respond(HttpStatusCode.OK)
        }
    }
}