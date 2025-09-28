package com.example.com.example

import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    suspend fun broadcast(message: String) {
        mutex.withLock {
            connections.forEach { session ->
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    println("Błąd wysyłania wiadomości: $e")
                }
            }
        }
    }
}

