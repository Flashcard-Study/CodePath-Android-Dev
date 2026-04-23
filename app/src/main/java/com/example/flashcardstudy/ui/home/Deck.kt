package com.example.flashcardstudy.ui.home

data class Deck(
    val id: String = "",
    val name: String,
    val color: String,
    val cardCount: Int,
    val subtitle: String = ""
) {
    companion object {
        fun fromDatabaseDeck(dbDeck: com.example.flashcardstudy.Deck): Deck {
            return Deck(
                id = dbDeck.id,
                name = dbDeck.name,
                color = dbDeck.color.ifBlank { "#6C63FF" },
                cardCount = dbDeck.cardCount,
                subtitle = dbDeck.subtitle
            )
        }
    }
}
