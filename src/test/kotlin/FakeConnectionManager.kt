import com.example.models.GameEventBase
import state.managers.network.ConnectionManager

class FakeConnectionManager: ConnectionManager {

    val events = mutableListOf<GameEventBase>()

    override suspend fun broadcastEvent(event: GameEventBase) {
        events.add(event)
    }
}