package com.example.flashcardstudy.data.transfer

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckTransferSerializerTest {

    @Test
    fun toJson_roundTripsWithParser() {
        val deck = Deck(
            id = "deck_1",
            name = "History",
            cardCount = 2,
            subtitle = "Unit 4",
            color = "#FF6B6B",
            icon = "📝",
            isPublic = false
        )
        val cards = listOf(
            Flashcard(question = "When was Rome founded?", answer = "753 BC"),
            Flashcard(question = "Who was Cleopatra?", answer = "Queen of Egypt")
        )

        val json = DeckTransferSerializer.toJson(deck, cards)
        val payload = DeckTransferParser.parse(json)

        assertEquals("History", payload.deck.name)
        assertEquals("Unit 4", payload.deck.subtitle)
        assertEquals(2, payload.cards.size)
        assertEquals("When was Rome founded?", payload.cards[0].question)
        assertEquals("753 BC", payload.cards[0].answer)
    }
}
