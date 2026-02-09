package state.managers.network

import com.example.models.GameEventBase
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class ConnectionManager {

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

    suspend fun broadcastEvent(event: GameEventBase) {
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