package state.managers.network

import com.example.logger.Logger
import com.example.models.ServerEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

interface ConnectionManager {
    suspend fun broadcastEvent(event: ServerEvent)
    suspend fun sendToPlayer(playerId: Int, event: ServerEvent)
}

class WebSocketConnectionManager : ConnectionManager {

    private val connections = mutableMapOf<Int, WebSocketSession>()
    private val mutex = Mutex()

    suspend fun addConnection(playerId : Int, session: WebSocketSession) {
        mutex.withLock {
            connections[playerId] = (session)
        }
        Logger.info("🔌 WebSocket połączony - PlayerId: $playerId, Aktywne połączenia: ${connections.size}")
    }

    suspend fun removeConnection(playerId : Int) {
        mutex.withLock {
            connections.remove(playerId)
        }
        Logger.info("🔌 WebSocket rozłączony - PlayerId: $playerId, Aktywne połączenia: ${connections.size}")
    }

    override suspend fun broadcastEvent(event: ServerEvent) {
        val json = Json.encodeToString(event)

        val snapshot = mutex.withLock {
            connections.values.toList()
        }

        Logger.debug("📡 Broadcast event: ${event::class.simpleName} do ${snapshot.size} graczy")
        Logger.debug("📦 Dane eventu: ${json.take(200)}${if (json.length > 200) "..." else ""}")

        var successCount = 0
        var errorCount = 0

        snapshot.forEach { session ->
            try {
                session.send(Frame.Text(json))
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
                val json = Json.encodeToString(event)
                session.send(Frame.Text(Json.encodeToString(event)))
                Logger.debug("📨 Prywatna wiadomość do gracza $playerId: ${event::class.simpleName}")
                Logger.debug("📦 Dane: ${json.take(200)}${if (json.length > 200) "..." else ""}")

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