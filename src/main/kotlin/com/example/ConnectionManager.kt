package com.example.com.example

import com.example.models.GameEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

object ConnectionManager {

    private val connections = mutableListOf<WebSocketSession>()
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

    suspend fun broadcastEvent(event: GameEvent) {
        val json = Json.encodeToString(event)
        mutex.withLock {
            connections.forEach { session ->
                try {
                    session.send(Frame.Text(json))
                } catch (e: Exception) {
                    println("Błąd przy wysyłaniu: $e")
                }
            }
        }
    }
}


