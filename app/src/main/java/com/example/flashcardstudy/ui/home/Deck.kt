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
            val color = when (dbDeck.name.lowercase()) {
                "biology" -> "#009628"
                "spanish" -> "#E86C00"
                "mathematics" -> "#4A90D9"
                "world history" -> "#9B59B6"
                else -> "#666666"
            }
            return Deck(
                id = dbDeck.id,
                name = dbDeck.name,
                color = color,
                cardCount = dbDeck.cardCount,
                subtitle = dbDeck.subtitle
            )
        }
    }
}
