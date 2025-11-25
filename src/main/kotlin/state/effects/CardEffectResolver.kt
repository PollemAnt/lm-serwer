import com.example.MoveRequest
import com.example.models.Card
import com.example.models.MoveFeedback
import com.example.models.Player


class CardEffectResolver(
    private val players: MutableList<Player>,
    private val deck: MutableList<Card>,
    private val secretCard: Card?
) {

    fun resolve(request: MoveRequest): MoveFeedback {
        println("[ACTION] Rozwiązywanie efektu karty przez CardEffectResolver")
        val currentPlayer = players.find { it.id == request.playerId }
            ?: return MoveFeedback.Standard("Nie znaleziono gracza")

        val targetPlayer = players.firstOrNull { it.id == request.targetPlayerId } ?: currentPlayer

        return when (request.card.number) {
            0 -> resolveSpy(currentPlayer)
            1 -> resolveGuard(request, currentPlayer, targetPlayer)
            2 -> resolvePriest(currentPlayer, targetPlayer)
            3 -> resolveBaron(currentPlayer, targetPlayer)
            4 -> resolveHandmaid(currentPlayer)
            5 -> resolvePrince(request, currentPlayer, targetPlayer)
            6 -> resolveChancellor(currentPlayer)
            7 -> resolveKing(currentPlayer, targetPlayer)
            8 -> resolveCountess(currentPlayer)
            else -> {
                println("Zagrano kartę o nieznanym numerze: ${request.card.number}")
                MoveFeedback.Standard("Zagrano nieznaną kartę.")
            }
        }
    }

    private fun updatePlayerState(playerId: Int, transform: (Player) -> Player) {
        val index = players.indexOfFirst { it.id == playerId }
        if (index != -1) {
            players[index] = transform(players[index])
        }
    }

    private fun removeCardFromHand(playerId: Int, card: Card) {
        updatePlayerState(playerId) {
            it.copy(hand = it.hand.filterNot { handCard -> handCard.id == card.id })
        }
    }

    private fun addCardToHand(playerId: Int, card: Card) {
        updatePlayerState(playerId) {
            it.copy(hand = it.hand + card)
        }
    }

    private fun addCardToPlayed(playerId: Int, card: Card) {
        updatePlayerState(playerId) {
            it.copy(playedCards = it.playedCards + card)
        }
    }

    private fun checkDiscardedCardEffect(discardedCard: Card, targetPlayer: Player) {
        when (discardedCard.number) {
            9 -> {
                println("[EFFECT] Gracz ${targetPlayer.name} odrzucił Księżniczkę i przegrywa!")
                updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
            }
            0 -> {
                println("[EFFECT] Gracz ${targetPlayer.name} odrzucił Szpiega. Efekt Szpiega aktywowany.")
                updatePlayerState(targetPlayer.id) { it.copy(isSpy = true) }
            }
        }
    }


    private fun resolveSpy(currentPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 0 (Szpieg)")
        updatePlayerState(currentPlayer.id) { it.copy(isSpy = true) }
        return MoveFeedback.SpyPlayed(currentPlayer.id)
    }

    private fun resolveGuard(request: MoveRequest, currentPlayer: Player, targetPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 1 (Strażnik)")
        val guessedCardNumber = request.guessCardNumber
        if (guessedCardNumber != null) {
            val opponentCard = targetPlayer.hand.firstOrNull()
            val wasCorrect = opponentCard?.number == guessedCardNumber
            if (wasCorrect) {
                updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
            }
            return MoveFeedback.GuardPlayed(
                currentPlayer.id,
                targetPlayer.id,
                guessedCardNumber,
                wasCorrect
            )
        }
        return MoveFeedback.Standard("Zagrano strażnika, ale wystąpił błąd")
    }

    private fun resolvePriest(currentPlayer: Player, targetPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 2 (Kapłan)")
        val opponentCard = targetPlayer.hand.firstOrNull()
        return if (opponentCard != null) {
            MoveFeedback.PriestPlayed(
                currentPlayer.id,
                targetPlayer.id,
                opponentCard.number
            )
        } else {
            MoveFeedback.Standard("Zagrano Kapłana, ale wystąpił błąd")
        }
    }

    private fun resolveBaron(currentPlayer: Player, targetPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 3 (Baron)")
        val playerCard = currentPlayer.hand.firstOrNull()
        val opponentCard = targetPlayer.hand.firstOrNull()

        if (playerCard != null && opponentCard != null) {
            return when {
                playerCard.number < opponentCard.number -> {
                    updatePlayerState(currentPlayer.id) { it.copy(isAlive = false) }
                    MoveFeedback.BaronPlayed(currentPlayer.id, targetPlayer.id, targetPlayer.id, currentPlayer.id)
                }
                playerCard.number > opponentCard.number -> {
                    updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
                    MoveFeedback.BaronPlayed(currentPlayer.id, targetPlayer.id, currentPlayer.id, targetPlayer.id)
                }
                else -> MoveFeedback.BaronPlayed(currentPlayer.id, targetPlayer.id, null, null)
            }
        }
        return MoveFeedback.Standard("Nie udało się porównać kart")
    }

    private fun resolveHandmaid(currentPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 4 (Służaca) i jest chroniony do następnej tury.")
        updatePlayerState(currentPlayer.id) { it.copy(isProtected = true) }
        return MoveFeedback.HandmaidPlayed(currentPlayer.id)
    }

    private fun resolvePrince(request: MoveRequest, currentPlayer: Player, targetPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył 5 (Księcia)")
        val discardedCard = targetPlayer.hand.firstOrNull()

        if (discardedCard != null) {
            removeCardFromHand(targetPlayer.id, discardedCard)
            addCardToPlayed(targetPlayer.id, discardedCard)
            checkDiscardedCardEffect(discardedCard, targetPlayer)

            val newCard = deck.removeFirstOrNull()
            if (newCard != null) {
                addCardToHand(targetPlayer.id, newCard)
            } else {
                secretCard?.let { addCardToHand(targetPlayer.id, it) }
            }
        }
        return MoveFeedback.PrincePlayed(currentPlayer.id, targetPlayer.id)
    }

    private fun resolveChancellor(currentPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} zagrał kanclerza.")
        // Logika Kanclerza jest teraz w `handleChancellorMove` w GameState.
        // `resolveCardEffect` nie powinno być za nią odpowiedzialne.
        // Zwracamy pusty feedback, bo główna akcja dzieje się gdzie indziej.
        return MoveFeedback.ChancellorPlayed(
            playerId = currentPlayer.id,
            availableCards = emptyList()
        )
    }

    private fun resolveKing(currentPlayer: Player, targetPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} użył karty 7 (Król)")
        val playerHand = currentPlayer.hand
        val targetHand = targetPlayer.hand

        updatePlayerState(currentPlayer.id) { it.copy(hand = targetHand) }
        updatePlayerState(targetPlayer.id) { it.copy(hand = playerHand) }

        println("Gracze ${currentPlayer.name} i ${targetPlayer.name} zamienili się kartami.")
        return MoveFeedback.KingPlayed(currentPlayer.id, targetPlayer.id)
    }

    private fun resolveCountess(currentPlayer: Player): MoveFeedback {
        println("Gracz ${currentPlayer.name} zagrał kartę nr 8 (Hrabina).")
        return MoveFeedback.Standard("Zagrano Hrabinę")
    }
}
