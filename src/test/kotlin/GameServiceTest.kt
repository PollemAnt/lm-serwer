import com.example.Strings
import com.example.models.Card
import com.example.models.CardPlayedEvent
import com.example.models.GameSnapshotEvent
import com.example.models.MoveRequest
import com.example.models.PlayerJoinEvent
import com.example.state.GameState
import com.example.state.managers.network.GameException
import com.example.state.managers.network.GameService
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameServiceTest {

    private lateinit var gameState: GameState
    private lateinit var connectionManager: FakeConnectionManager
    private lateinit var gameService: GameService

    @BeforeEach
    fun setup() {
        gameState = GameState()
        connectionManager = FakeConnectionManager()
        gameService = GameService(gameState, connectionManager)
    }

    @Test
    fun `should add player and broadcast PlayerJoinEvent`() = runTest  {
        val player = gameService.addPlayer("Alice")

        assertEquals("Alice", player.name)
        assertEquals(2, connectionManager.events.size)
        assertTrue(connectionManager.events.any { it is PlayerJoinEvent })
        assertTrue(connectionManager.events.any { it is GameSnapshotEvent })
    }

    @Test
    fun `should throw exception when lobby is full`() = runTest  {
        repeat(gameState.getGameConfig().maxPlayers) {
            gameService.addPlayer("Player$it")
        }

        val exception = assertThrows<GameException> {
                gameService.addPlayer("TooMuch")
        }

        assertEquals(
            Strings.get("lobby.max_players_reached"),
            exception.message
        )
    }


    @Test
    fun `should broadcast CardPlayedEvent on valid move`() = runTest {
        val p1 = gameService.addPlayer("Alice")
        val p2 = gameService.addPlayer("Bob")

        gameState.startGameForTest()

        val actualP1 = gameState.getPlayerById(p1.id)
        val actualP2 = gameState.getPlayerById(p2.id)

        val move = MoveRequest(
            playerId = actualP1!!.id,
            card = actualP1.hand.first(),
            targetPlayerId = actualP2!!.id,
            guessCardNumber = null
        )

        gameService.handleMove(move)
        advanceUntilIdle()

        assertTrue(
            connectionManager.events.any { it is CardPlayedEvent }
        )
    }

    @Test
    fun `should reject move when not players turn`() = runTest  {

        val p1 = gameService.addPlayer("Alice")
        val p2 = gameService.addPlayer("Bob")

        gameState.startGameForTest()

        val actualP1 = gameState.getPlayerById(p1.id)
        val actualP2 = gameState.getPlayerById(p2.id)

        val invalidMove = MoveRequest(
            playerId = actualP2!!.id,
            card = actualP2.hand.first(),
            targetPlayerId = actualP1!!.id,
            guessCardNumber = null
        )

        val exception = assertThrows<GameException> {
            gameService.handleMove(invalidMove)
        }

        assertEquals(
            Strings.get("error.not_your_turn"),
            exception.message
        )

        assertTrue(connectionManager.events.none { it is CardPlayedEvent })
    }

    @Test
    fun `should prevent Guard from targeting self when other targets exist`() = runTest {
        val p1 = gameService.addPlayer("Alice")
        val p2 = gameService.addPlayer("Bob")
        gameState.startGameForTest()

        val actualP1 = gameState.getPlayerById(p1.id)!!
        
        // Ensure Alice has a Guard
        val guardCard = Card(99, 1, com.example.models.CardType.GUARD, "Guard", "")
        gameState.setPlayerHand(p1.id, listOf(guardCard))

        val move = MoveRequest(
            playerId = actualP1.id,
            card = guardCard,
            targetPlayerId = actualP1.id, // Targeting self
            guessCardNumber = 2
        )

        val exception = assertThrows<com.example.state.managers.network.GameException> {
            gameService.handleMove(move)
        }
        assertEquals(com.example.Strings.get("error.cannot_target_self"), exception.message)
    }

    @Test
    fun `should allow Guard to target self when all others are protected`() = runTest {
        val p1 = gameService.addPlayer("Alice")
        val p2 = gameService.addPlayer("Bob")
        gameState.startGameForTest()

        // Bob is protected
        gameState.setPlayerProtected(p2.id, true)
        
        // Ensure Alice has a Guard
        val guardCard = Card(99, 1, com.example.models.CardType.GUARD, "Guard", "")
        gameState.setPlayerHand(p1.id, listOf(guardCard))

        val move = MoveRequest(
            playerId = p1.id,
            card = guardCard,
            targetPlayerId = p1.id, // Targeting self forced
            guessCardNumber = 2
        )

        gameService.handleMove(move)
        advanceUntilIdle()

        val lastEvent = connectionManager.events.filterIsInstance<com.example.models.CardPlayedEvent>().last()
        // Should NOT be an error message, but the actual Guard effect (even if it does nothing to self)
        assertTrue(lastEvent.feedback is com.example.models.CardPlayedFeedback.GuardPlayed)
    }
}
