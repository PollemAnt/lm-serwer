package com.example.models

object DeckFactory {

    fun createDeck(composition: List<CardCount>): List<Card> {
        val deck = mutableListOf<Card>()
        var idCounter = 1

        repeat(composition[0].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 0,
                    name = "Szpieg",
                    description = "Szpieg blabll"
                )
            )
        }
        repeat(composition[1].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 1,
                    name = "Strażnik",
                    description = "Wybierz innego gracza i spróbuj odgadnąć jego kartę."
                )
            )
        }
        repeat(composition[2].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 2,
                    name = "Kapłan",
                    description = "Zobacz karte innego gracza"
                )
            )
        }
        repeat(composition[3].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 3,
                    name = "Baron",
                    description = "Porónaj numer kart z innym graczem"
                )
            )
        }
        repeat(composition[4].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 4,
                    name = "Służąca",
                    description = "Do końca tej tury jesteś nietykalny"
                )
            )
        }
        repeat(composition[5].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 5,
                    name = "Książe",
                    description = "Wyrzuć karte z ręki gracza"
                )
            )
        }
        repeat(composition[6].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 6,
                    name = "Kanclerz",
                    description = "Zobacz karty z talibla balbla"
                )
            )
        }
        repeat(composition[7].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 7,
                    name = "Król",
                    description = "Zamien karty z innym garczem"
                )
            )
        }
        repeat(composition[8].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 8,
                    name = "Hrabina",
                    description = "Musisz zagrać hrabine jak dobierzesz ksiecia albo króla"
                )
            )
        }
        repeat(composition[9].count) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 9,
                    name = "Księżniczka",
                    description = "Jak odrzucisz lub zagrasz te karte to przegrasz"
                )
            )
        }

        return deck
    }

    fun createDefaultDeckComposition(): List<CardCount> {
        return listOf(
            CardCount("Szpieg", 0, 2),
            CardCount("Strażnik", 1, 6),
            CardCount("Kapłan", 2, 2),
            CardCount("Baron", 3, 2),
            CardCount("Służaca", 4, 2),
            CardCount("Książe", 5, 2),
            CardCount("Kanclerz", 6, 2),
            CardCount("Król", 7, 1),
            CardCount("Hrabina", 8, 1),
            CardCount("Księżniczka", 9, 1)
        )
    }
}