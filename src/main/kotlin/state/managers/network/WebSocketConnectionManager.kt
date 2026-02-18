package state.managers.network

import com.example.models.ServerEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

interface ConnectionManager {
    suspend fun broadcastEvent(event: ServerEvent)
}

class WebSocketConnectionManager : ConnectionManager {

    private val connections = mutableSetOf<WebSocketSession>()
    private val mutex = Mutex()

    suspend fun addConnection(session: WebSocketSession) {
        mutex.withLock {
            connections.add(session)
        }
    }

    suspend fun removeConnection(session: WebSocketSession) {
        mutex.withLock {
            connections.remove(session)
        }
    }

    override suspend fun broadcastEvent(event: ServerEvent) {
        val json = Json.encodeToString(event)

        val snapshot = mutex.withLock {
            connections.toList()
        }

        snapshot.forEach { session ->
            try {
                session.send(Frame.Text(json))
            } catch (e: Exception) {
                println("WS send error, removing session: $e")
                removeConnection(session)
            }
        }
    }
}