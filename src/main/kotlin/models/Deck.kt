package com.example.models

object DeckFactory {
    fun createDeck(): MutableList<Card> {
        val deck = mutableListOf<Card>()
        var idCounter = 1

        repeat(6) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 1,
                    name = "Strażnik",
                    description = "Wybierz innego gracza i spróbuj odgadnąć jego kartę."
                )
            )
        }
        repeat(2) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 2,
                    name = "Kapłan",
                    description = "Zobacz karte innego gracza"
                )
            )
        }
        repeat(2) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 3,
                    name = "Baron",
                    description = "Porónaj numer kart z innym graczem"
                )
            )
        }
        repeat(2) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 4,
                    name = "Służąca",
                    description = "Do końca tej tury jesteś nietykalny"
                )
            )
        }
        repeat(2) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 5,
                    name = "Książe",
                    description = "Wyrzuć karte z ręki gracza"
                )
            )
        }
        repeat(2) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 6,
                    name = "Kanclerz",
                    description = "Zobacz karty z talibla balbla"
                )
            )
        }
        repeat(1) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 7,
                    name = "Król",
                    description = "Zamien karty z innym garczem"
                )
            )
        }
        repeat(1) {
            deck.add(
                Card(
                    id = idCounter++,
                    number = 8,
                    name = "Hrabina",
                    description = "Musisz zagrać hrabine jak dobierzesz ksiecia albo króla"
                )
            )
        }
        repeat(1) {
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
}