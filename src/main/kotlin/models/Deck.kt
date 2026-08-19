package com.example.models

import com.example.Strings

object DeckFactory {

    fun createDeck(composition: List<CardCount>): List<Card> {
        val deck = mutableListOf<Card>()
        var idCounter = 1

        composition.forEach { card ->
            repeat(card.count) {
                deck.add(
                    Card(
                        id = idCounter++,
                        number = card.cardNumber,
                        cardType = getCardType(card.cardNumber),
                        name = card.cardName,
                        description = getCardDescription(card.cardNumber)
                    )
                )
            }
        }

        return deck
    }

    fun getCardType(number: Int): CardType {
        return when (number) {
            0 -> CardType.SPY
            1 -> CardType.GUARD
            2 -> CardType.PRIEST
            3 -> CardType.BARON
            4 -> CardType.HANDMAID
            5 -> CardType.PRICE
            6 -> CardType.CHANCELLOR
            7 -> CardType.KING
            8 -> CardType.COUNTESS
            9 -> CardType.PRINCESS
            else -> CardType.SPY
        }
    }

    private fun getCardDescription(number: Int): String {
        return when (number) {
            0 -> Strings.get("card.action.spy")
            1 -> Strings.get("card.action.guard")
            2 -> Strings.get("card.action.priest")
            3 -> Strings.get("card.action.baron")
            4 -> Strings.get("card.action.handmaid")
            5 -> Strings.get("card.action.prince")
            6 -> Strings.get("card.action.chancellor")
            7 -> Strings.get("card.action.king")
            8 -> Strings.get("card.action.countess_rule")
            9 -> Strings.get("card.action.princess_rule")
            else -> ""
        }
    }

    fun createDefaultDeckComposition(): List<CardCount> {
        return listOf(
            CardCount(Strings.get("card.spy"), 0, 2),
            CardCount(Strings.get("card.guard"), 1, 6),
            CardCount(Strings.get("card.priest"), 2, 2),
            CardCount(Strings.get("card.baron"), 3, 2),
            CardCount(Strings.get("card.handmaid"), 4, 2),
            CardCount(Strings.get("card.prince"), 5, 2),
            CardCount(Strings.get("card.chancellor"), 6, 2),
            CardCount(Strings.get("card.king"), 7, 1),
            CardCount(Strings.get("card.countess"), 8, 1),
            CardCount(Strings.get("card.princess"), 9, 1)
        )
    }
}
