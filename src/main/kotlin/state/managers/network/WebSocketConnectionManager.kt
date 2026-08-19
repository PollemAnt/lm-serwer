package com.example.state.managers.network

import com.example.JsonConfig
import com.example.logger.Logger
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.shared.protocol.ServerEvent

interface ConnectionManager {
    suspend fun broadcastEvent(event: ServerEvent)
    suspend fun sendToPlayer(playerId: Int, event: ServerEvent)
}

class WebSocketConnectionManager : ConnectionManager {

    private val json = JsonConfig.json
    private val connections = mutableMapOf<Int, WebSocketSession>()
    private val mutex = Mutex()

    suspend fun addConnection(playerId : Int, session: WebSocketSession) {
        mutex.withLock {
            connections[playerId] = (session)
        }
        Logger.info("🔌 WebSocket połączony - PlayerId: $playerId, Aktywne połączenia: ${connections.size}")
    }

    suspend fun removeConnection(playerId : Int, session: WebSocketSession) {
        mutex.withLock {
            if (connections[playerId] === session) {
                connections.remove(playerId)
                Logger.info("🔌 WebSocket rozłączony - PlayerId: $playerId, Aktywne połączenia: ${connections.size}")
            } else {
                Logger.debug("🔌 Pominięto usuwanie WebSocket dla PlayerId: $playerId - sesja została już zastąpiona")
            }
        }
    }

    override suspend fun broadcastEvent(event: ServerEvent) {
        val jsonString = try {
            json.encodeToString(event)
        } catch (e: Exception) {
            Logger.error("❌ Błąd serializacji eventu ${event::class.simpleName}: ${e.message}", e)
            return
        }

        val snapshot = mutex.withLock {
            connections.values.toList()
        }

        Logger.debug("📡 Broadcast event: ${event::class.simpleName} do ${snapshot.size} graczy. JSON: $jsonString")

        var successCount = 0
        var errorCount = 0

        snapshot.forEach { session ->
            try {
                session.send(Frame.Text(jsonString))
                successCount++
            } catch (e: Exception) {
                println("WS send error,  $e")
                errorCount++
                Logger.error("❌ Błąd wysyłki WebSocket broadcast: ${e.message}", e)
            }
        }

        if (errorCount > 0) {
            Logger.warn("⚠️ Broadcast: $successCount sukcesów, $errorCount błędów")
        } else if (successCount > 0) {
            Logger.debug("✅ Broadcast: $successCount wiadomości wysłanych")
        }
    }

    override suspend fun sendToPlayer(playerId: Int, event: ServerEvent) {
        val session = mutex.withLock { connections[playerId] }

        if (session != null) {
            try {
                val jsonString = json.encodeToString(event)
                session.send(Frame.Text(jsonString))
                Logger.debug("📨 Prywatna wiadomość do gracza $playerId: ${event::class.simpleName}")
                Logger.debug("📦 Dane: ${jsonString.take(200)}${if (jsonString.length > 200) "..." else ""}")

            } catch (e: Exception) {
                println("WS private send error: $e")
                Logger.error("❌ Błąd wysyłki prywatnej do gracza $playerId: ${e.message}", e)
            }
        }
    }

    suspend fun getConnectionStats(): Map<Int, Boolean> {
        return mutex.withLock {
            connections.mapValues { true }.toMap()
        }
    }
}
