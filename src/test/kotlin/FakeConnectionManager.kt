import org.example.shared.protocol.ServerEvent
import com.example.state.managers.network.ConnectionManager

class FakeConnectionManager: ConnectionManager {

    val events = mutableListOf<ServerEvent>()

    override suspend fun broadcastEvent(event: ServerEvent) {
        events.add(event)
    }

    override suspend fun sendToPlayer(playerId: Int, event: ServerEvent) {
        events.add(event)
    }
}
