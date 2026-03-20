package state.managers.network

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
    }

    suspend fun removeConnection(playerId : Int) {
        mutex.withLock {
            connections.remove(playerId)
        }
    }

    override suspend fun broadcastEvent(event: ServerEvent) {
        val json = Json.encodeToString(event)

        val snapshot = mutex.withLock {
            connections.values.toList()
        }

        snapshot.forEach { session ->
            try {
                session.send(Frame.Text(json))
            } catch (e: Exception) {
                println("WS send error,  $e")
            }
        }
    }

    override suspend fun sendToPlayer(playerId: Int, event: ServerEvent) {
        val session = mutex.withLock { connections[playerId] }

        if (session != null) {
            try {
                session.send(Frame.Text(Json.encodeToString(event)))
            } catch (e: Exception) {
                println("WS private send error: $e")
            }
        }
    }
}