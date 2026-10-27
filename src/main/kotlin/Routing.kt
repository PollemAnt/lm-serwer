package com.example

import com.example.logger.Logger
import com.example.state.GameState
import com.example.state.managers.network.GameService
import com.example.state.managers.network.WebSocketConnectionManager
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import org.example.shared.domain.GameConfig
import org.example.shared.protocol.DrawRequest
import org.example.shared.protocol.MoveRequest
import org.example.shared.protocol.PlayerJoinRequest
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.sessions.get
import org.koin.ktor.ext.inject
import io.ktor.server.routing.route
import com.example.state.managers.network.ConnectionManager

fun Application.configureRouting() {

    val connectionManager by inject<ConnectionManager>()
    val gameService by inject<GameService>()

    routing {
        route("/api/v1") {
            get("/") {
                Logger.info("🌐 Żądanie GET /api/v1/ od ${call.request.origin.remoteHost}")
                call.respondText(Strings.get("hello.message"), ContentType.Text.Plain)
            }
    
            webSocket("/updates") {
                val session = call.sessions.get<UserSession>()
                val playerId = session?.playerId ?: call.request.queryParameters["playerId"]?.toIntOrNull()
                val clientIp = call.request.origin.remoteHost
    
                Logger.info("🔌 Nowe połączenie WebSocket - IP: $clientIp, PlayerId: $playerId")
    
                if (playerId == null) {
                    Logger.warn("❌ Odrzucono połączenie WebSocket - brak playerId, IP: $clientIp")
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing playerId"))
                    return@webSocket
                }
    
                val wsManager = connectionManager as? WebSocketConnectionManager
                wsManager?.addConnection(playerId, this)
    
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                Logger.debug("📨 Otrzymano wiadomość WebSocket od gracza $playerId: ${frame.readText()}")
                            }
                            is Frame.Close -> {
                                Logger.info("🔌 WebSocket zamknięty przez gracza $playerId")
                                break
                            }
                            else -> {
                                Logger.debug("📨 Otrzymano frame typu ${frame.frameType} od gracza $playerId")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.error("❌ Błąd podczas obsługi WebSocket dla gracza $playerId", e)
                } finally {
                    wsManager?.removeConnection(playerId, this)
                    Logger.info("🔌 Połączenie WebSocket zamknięte dla gracza $playerId")
                }
            }
    
    
            post("/join") {
                val request = call.receive<PlayerJoinRequest>()
                val clientIp = call.request.origin.remoteHost
    
                Logger.info("👤 Próba dołączenia gracza - IP: $clientIp, Nazwa: ${request.name}")
    
                val playerAdded = gameService.addPlayer(request.name)
                call.sessions.set(UserSession(playerAdded.id))
                Logger.info("✅ Gracz dołączył - ID: ${playerAdded.id}, Nazwa: ${playerAdded.name}")
                call.respond(HttpStatusCode.OK, playerAdded)
            }
    
            get("/state") {
                val playerId = call.request.queryParameters["playerId"]?.toIntOrNull()
                Logger.debug("📊 Żądanie stanu gry od ${call.request.origin.remoteHost}, playerId: $playerId")
                val json = Json.encodeToString(gameService.getSanitizedState(playerId))
                call.respondText(json, ContentType.Application.Json)
            }
    
    
            post("/move") {
                val session = call.sessions.get<UserSession>()
                val request = call.receive<MoveRequest>()
                
                val activePlayerId = session?.playerId ?: request.playerId
                val securedRequest = request.copy(playerId = activePlayerId)
                
                Logger.info("🎮 Ruch gracza ${securedRequest.playerId}: $securedRequest")
    
                gameService.handleMove(securedRequest)
                Logger.info("✅ Ruch gracza ${securedRequest.playerId} wykonany pomyślnie")
                call.respond(HttpStatusCode.OK)
            }
    
            post("/complete_chancellor") {
                val session = call.sessions.get<UserSession>()
                val request = call.receive<MoveRequest>()
                
                val activePlayerId = session?.playerId ?: request.playerId
                val securedRequest = request.copy(playerId = activePlayerId)
                
                Logger.info("🎮 Kanclerz ${securedRequest.playerId} kończy turę")
    
                gameService.completeChancellorMove(securedRequest)
                Logger.info("✅ Tura kanclerza ${securedRequest.playerId} zakończona")
                call.respond(HttpStatusCode.OK)
            }
    
            post("/drawCardForChancellor") {
                val session = call.sessions.get<UserSession>()
                val request = call.receive<DrawRequest>()
                
                val activePlayerId = session?.playerId ?: request.playerId
                val securedRequest = request.copy(playerId = activePlayerId)
                
                Logger.info("🎴 Kanclerz ${securedRequest.playerId} dobiera karty")
    
                val hand = gameService.drawCardsForChancellor(securedRequest)
                Logger.info("✅ Kanclerz ${securedRequest.playerId} dobrał ${hand.size} kart")
                call.respond(HttpStatusCode.OK, hand)
            }
    
            get("/player/{playerId}") {
                val playerId = call.parameters["playerId"]?.toIntOrNull()
                Logger.debug("👤 Pobieranie danych gracza $playerId")
    
                val player = gameService.getPlayer(playerId)
                call.respond(player)
            }
    
            get("/getGameConfig") {
                Logger.debug("⚙️ Pobieranie konfiguracji gry")
                call.respond(gameService.getGameConfig())
            }
    
            post("/updateGameConfig") {
                val request = call.receive<GameConfig>()
                Logger.info("⚙️ Aktualizacja konfiguracji gry: $request")
                gameService.updateGameConfig(request)
                call.respond(HttpStatusCode.OK)
    
            }
    
            //Temporary
            get("/reset") {
                Logger.warn("🔄 RESET GRY - Wykonano reset stanu gry")
                gameService.resetGame()
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
