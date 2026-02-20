package com.example.models

import com.example.Strings

object DeckFactory {

    fun createDeck(composition: List<CardCount>): List<Card> {
        val deck = mutableListOf<Card>()
        var idCounter = 1

        repeat(composition[0].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 0,
                    cardType = CardType.SPY,
                    name = Strings.get("card.spy"),
                    description = Strings.get("card.action.spy")
                )
            )
        }
        repeat(composition[1].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 1,
                    cardType = CardType.GUARD,
                    name = Strings.get("card.guard"),
                    description = Strings.get("card.action.guard")
                )
            )
        }
        repeat(composition[2].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 2,
                    cardType = CardType.PRIEST,
                    name = Strings.get("card.priest"),
                    description = Strings.get("card.action.priest")
                )
            )
        }
        repeat(composition[3].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 3,
                    cardType = CardType.BARON,
                    name = Strings.get("card.baron"),
                    description = Strings.get("card.action.baron")
                )
            )
        }
        repeat(composition[4].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 4,
                    cardType = CardType.HANDMAID,
                    name = Strings.get("card.handmaid"),
                    description = Strings.get("card.action.handmaid")
                )
            )
        }
        repeat(composition[5].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 5,
                    cardType = CardType.PRICE,
                    name = Strings.get("card.prince"),
                    description = Strings.get("card.action.prince")
                )
            )
        }
        repeat(composition[6].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 6,
                    cardType = CardType.CHANCELLOR,
                    name = Strings.get("card.chancellor"),
                    description = Strings.get("card.action.chancellor")
                )
            )
        }
        repeat(composition[7].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 7,
                    cardType = CardType.KING,
                    name = Strings.get("card.king"),
                    description = Strings.get("card.action.king")
                )
            )
        }
        repeat(composition[8].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 8,
                    cardType = CardType.COUNTESS,
                    name = Strings.get("card.countess"),
                    description = Strings.get("card.action.countess_rule")
                )
            )
        }
        repeat(composition[9].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 9,
                    cardType = CardType.PRINCESS,
                    name = Strings.get("card.princess"),
                    description = Strings.get("card.action.princess_rule")
                )
            )
        }

        return deck
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