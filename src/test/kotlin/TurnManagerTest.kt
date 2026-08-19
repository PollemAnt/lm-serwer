import com.example.state.managers.DeckManager
import com.example.state.managers.PlayerManager
import com.example.state.managers.TurnManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TurnManagerTest {

    private lateinit var playerManager: PlayerManager
    private lateinit var deckManager: DeckManager
    private lateinit var turnManager: TurnManager

    @BeforeEach
    fun setup() {
        playerManager = PlayerManager()
        deckManager = DeckManager()
        turnManager = TurnManager(playerManager, deckManager)
        
        playerManager.addPlayer("Alice") // ID 1
        playerManager.addPlayer("Bob")   // ID 2
        playerManager.addPlayer("Charlie") // ID 3
        
        deckManager.resetAndShuffle()
    }

    @Test
    fun `should skip dead players when advancing turn`() {
        // Given: Bob is dead
        playerManager.updatePlayer(2) { it.copy(isAlive = false) }
        
        // When: Alice finishes turn
        turnManager.setFirstPlayer(1)
        turnManager.advanceTurn()
        
        // Then: Next player should be Charlie (ID 3), skipping Bob (ID 2)
        assertEquals(3, turnManager.getActivePlayerId())
    }

    @Test
    fun `should wrap around and skip dead players`() {
        // Given: Alice (1) and Charlie (3) are dead
        playerManager.updatePlayer(1) { it.copy(isAlive = false) }
        playerManager.updatePlayer(3) { it.copy(isAlive = false) }
        
        // When: Bob finishes turn
        turnManager.setFirstPlayer(2)
        turnManager.advanceTurn()
        
        // Then: Next player should be Bob again (since others are dead)
        assertEquals(2, turnManager.getActivePlayerId())
    }
}
