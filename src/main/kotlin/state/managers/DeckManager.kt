package com.example.state.managers

import com.example.models.Card
import com.example.models.DeckFactory

class DeckManager {

    private var deck: MutableList<Card> = mutableListOf()
    private var secretCard: Card? = null

    fun resetAndShuffle() {
        deck = DeckFactory.createDeck().shuffled().toMutableList()
        secretCard = null
    }

    fun drawSecretCard() {
        secretCard = deck.removeFirstOrNull()
    }

    fun getSecretCard(): Card? = secretCard

    fun draw(): Card? = deck.removeFirstOrNull()

    fun drawTwoWithSecretFallback(): List<Card> {
        val c1 = deck.removeFirstOrNull()
        val c2 = deck.removeFirstOrNull()

        val card1 = c1 ?: secretCard
        val card2 = when {
            c1 == null -> null
            else -> c2 ?: secretCard
        }

        return listOfNotNull(card1, card2)
    }

    fun putOnBottom(cards: List<Card>) {
        deck.addAll(cards)
    }

    fun remaining(): Int = deck.size

    fun getDeckSnapshot(): List<Card> = deck.toList()
}
