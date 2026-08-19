package com.example.state.effects

import com.example.Strings
import org.example.shared.protocol.MoveRequest
import org.example.shared.domain.Card
import org.example.shared.protocol.CardPlayedFeedback
import org.example.shared.domain.CardType
import org.example.shared.domain.CardType.*
import org.example.shared.domain.Player
import com.example.state.managers.DeckManager
import com.example.state.managers.PlayerManager


class CardEffectResolver(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager
) {

    fun resolve(request: MoveRequest): CardPlayedFeedback {
        val currentPlayer = playerManager.findById(request.playerId)
            ?: return CardPlayedFeedback.Standard(Strings.get("error.player_not_found"))

        val targetPlayer = request.targetPlayerId?.let { playerManager.findById(it) } ?: currentPlayer

        // Reset protection at the start of player's turn (actually it should be reset when their turn starts,
        // but here it's reset when they play a card, which is usually correct for Love Letter unless we want to be very strict).
        // Actually, the rules say protection lasts until your next turn.
        updatePlayerState(currentPlayer.id) { it.copy(isProtected = false) }

        if (targetPlayer.isProtected && targetPlayer.id != currentPlayer.id && isTargetedEffect(request.card.number)) {
             return CardPlayedFeedback.Standard(Strings.format("game.player_protected", targetPlayer.name))
        }

        if (targetPlayer.id == currentPlayer.id && isMustTargetOther(request.card.cardType)) {
            val hasOtherValidTargets = playerManager.getAll()
                .any { it.id != currentPlayer.id && it.isAlive && !it.isProtected }

            if (hasOtherValidTargets) {
                return CardPlayedFeedback.Standard(Strings.get("error.cannot_target_self"))
            }
        }

        return when (request.card.cardType) {
            SPY -> resolveSpy(currentPlayer)
            GUARD -> resolveGuard(request, currentPlayer, targetPlayer)
            PRIEST -> resolvePriest(currentPlayer, targetPlayer)
            BARON -> resolveBaron(currentPlayer, targetPlayer)
            HANDMAID -> resolveHandmaid(currentPlayer)
            PRINCE -> resolvePrince(currentPlayer, targetPlayer)
            CHANCELLOR -> resolveChancellor(currentPlayer)
            KING -> resolveKing(currentPlayer, targetPlayer)
            COUNTESS -> resolveCountess()
            else -> {
                CardPlayedFeedback.Standard(Strings.get("error.unknown_card_played"))
            }
        }
    }

    private fun isTargetedEffect(cardNumber: Int): Boolean {
        return cardNumber in listOf(1, 2, 3, 5, 7)
    }

    private fun isMustTargetOther(cardType: CardType): Boolean {
        return cardType in listOf(GUARD, PRIEST, BARON, KING)
    }

    private fun updatePlayerState(playerId: Int, transform: (Player) -> Player) {
        playerManager.updatePlayer(playerId, transform)
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
        when (discardedCard.cardType) {
            PRINCESS -> {
                updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
            }

            SPY -> {
                updatePlayerState(targetPlayer.id) { it.copy(isSpy = true) }
            }

            else -> return

        }
    }


    private fun resolveSpy(currentPlayer: Player): CardPlayedFeedback {
        updatePlayerState(currentPlayer.id) { it.copy(isSpy = true) }
        return CardPlayedFeedback.SpyPlayed(currentPlayer.id)
    }

    private fun resolveGuard(
        request: MoveRequest,
        currentPlayer: Player,
        targetPlayer: Player
    ): CardPlayedFeedback {
        val guessedCardNumber = request.guessCardNumber
        if (guessedCardNumber != null) {
            val opponentCard = targetPlayer.hand.firstOrNull()
            val wasCorrect = opponentCard?.number == guessedCardNumber
            if (wasCorrect) {
                updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
            }
            return CardPlayedFeedback.GuardPlayed(
                currentPlayer.id,
                targetPlayer.id,
                guessedCardNumber,
                wasCorrect
            )
        } else {
            return CardPlayedFeedback.Standard(Strings.get("game.guard_played"))
        }
    }

    private fun resolvePriest(currentPlayer: Player, targetPlayer: Player): CardPlayedFeedback {
        val opponentCard = targetPlayer.hand.firstOrNull()
        return if (opponentCard != null) {
            CardPlayedFeedback.PriestPlayed(
                currentPlayer.id,
                targetPlayer.id,
                opponentCard.number
            )
        } else {
            CardPlayedFeedback.Standard(Strings.get("error.priest_play_error"))
        }
    }

    private fun resolveBaron(currentPlayer: Player, targetPlayer: Player): CardPlayedFeedback {
        val playerCard = currentPlayer.hand.firstOrNull()
        val opponentCard = targetPlayer.hand.firstOrNull()

        if (playerCard != null && opponentCard != null) {
            return when {
                playerCard.number < opponentCard.number -> {
                    updatePlayerState(currentPlayer.id) { it.copy(isAlive = false) }
                    CardPlayedFeedback.BaronPlayed(
                        currentPlayer.id,
                        targetPlayer.id,
                        targetPlayer.id,
                        currentPlayer.id
                    )
                }

                playerCard.number > opponentCard.number -> {
                    updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
                    CardPlayedFeedback.BaronPlayed(
                        currentPlayer.id,
                        targetPlayer.id,
                        currentPlayer.id,
                        targetPlayer.id
                    )
                }

                else -> CardPlayedFeedback.BaronPlayed(
                    currentPlayer.id,
                    targetPlayer.id,
                    null,
                    null
                )
            }
        }
        return CardPlayedFeedback.Standard(Strings.get("error.card_compare_failed"))
    }

    private fun resolveHandmaid(currentPlayer: Player): CardPlayedFeedback {
        updatePlayerState(currentPlayer.id) { it.copy(isProtected = true) }
        return CardPlayedFeedback.HandmaidPlayed(currentPlayer.id)
    }

    private fun resolvePrince(
        currentPlayer: Player,
        targetPlayer: Player
    ): CardPlayedFeedback {
        val discardedCard = targetPlayer.hand.firstOrNull()

        if (discardedCard != null) {
            removeCardFromHand(targetPlayer.id, discardedCard)
            addCardToPlayed(targetPlayer.id, discardedCard)
            checkDiscardedCardEffect(discardedCard, targetPlayer)

            if (targetPlayer.isAlive) {
                val newCard = deckManager.draw()
                if (newCard != null) {
                    addCardToHand(targetPlayer.id, newCard)
                } else {
                    deckManager.getSecretCard()?.let { addCardToHand(targetPlayer.id, it) }
                }
            }
        }
        return CardPlayedFeedback.PrincePlayed(currentPlayer.id, targetPlayer.id)
    }

    private fun resolveChancellor(currentPlayer: Player): CardPlayedFeedback {

        return CardPlayedFeedback.ChancellorPlayed(
            playerId = currentPlayer.id
        )
    }

    private fun resolveKing(currentPlayer: Player, targetPlayer: Player): CardPlayedFeedback {
        val playerHand = currentPlayer.hand
        val targetHand = targetPlayer.hand

        updatePlayerState(currentPlayer.id) { it.copy(hand = targetHand) }
        updatePlayerState(targetPlayer.id) { it.copy(hand = playerHand) }

        return CardPlayedFeedback.KingPlayed(currentPlayer.id, targetPlayer.id)
    }

    private fun resolveCountess(): CardPlayedFeedback {
        return CardPlayedFeedback.Standard(Strings.get("game.countess_played"))
    }
}
