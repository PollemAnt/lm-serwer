package com.example.state.effects

import com.example.Strings
import com.example.models.MoveRequest
import com.example.models.Card
import com.example.models.CardPlayedFeedback
import com.example.models.Player
import com.example.state.managers.DeckManager
import com.example.state.managers.PlayerManager


class CardEffectResolver(
    private val playerManager: PlayerManager,
    private val deckManager: DeckManager
) {

    fun resolve(request: MoveRequest): CardPlayedFeedback {
        val currentPlayer = playerManager.getAll().find { it.id == request.playerId }
            ?: return CardPlayedFeedback.Standard(Strings.get("error.player_not_found"))

        val targetPlayer = playerManager.getAll().firstOrNull { it.id == request.targetPlayerId } ?: currentPlayer
        updatePlayerState(currentPlayer.id) { it.copy(isProtected = false) }
        return when (request.card.number) {
            0 -> resolveSpy(currentPlayer)
            1 -> resolveGuard(request, currentPlayer, targetPlayer)
            2 -> resolvePriest(currentPlayer, targetPlayer)
            3 -> resolveBaron(currentPlayer, targetPlayer)
            4 -> resolveHandmaid(currentPlayer)
            5 -> resolvePrince(currentPlayer, targetPlayer)
            6 -> resolveChancellor(currentPlayer)
            7 -> resolveKing(currentPlayer, targetPlayer)
            8 -> resolveCountess()
            else -> {
                CardPlayedFeedback.Standard(Strings.get("error.unknown_card_played"))
            }
        }
    }

    private fun updatePlayerState(playerId: Int, transform: (Player) -> Player) {
        val index = playerManager.getAll().indexOfFirst { it.id == playerId }
        if (index != -1) {
            playerManager.getAll()[index] = transform(playerManager.getAll()[index])
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
                updatePlayerState(targetPlayer.id) { it.copy(isAlive = false) }
            }

            0 -> {
                updatePlayerState(targetPlayer.id) { it.copy(isSpy = true) }
            }
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
            CardPlayedFeedback.Standard(Strings.get("game.guard_played"))
        }
        return CardPlayedFeedback.Standard(Strings.get("error.guard_play_error"))
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

            val newCard = deckManager.draw()
            if (newCard != null) {
                addCardToHand(targetPlayer.id, newCard)
            } else {
                deckManager.getSecretCard()?.let { addCardToHand(targetPlayer.id, it) }
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
