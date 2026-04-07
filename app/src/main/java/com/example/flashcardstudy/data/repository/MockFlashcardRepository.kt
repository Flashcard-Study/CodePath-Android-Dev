package com.example.flashcardstudy.data.repository

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import kotlinx.coroutines.delay

class MockFlashcardRepository : FlashcardRepository {

    private val decks = mutableListOf(
        Deck(
            id = "deck_biology",
            name = "Biology",
            cardCount = 5,
            subtitle = "Ch. 1"
        ),
        Deck(
            id = "deck_spanish",
            name = "Spanish",
            cardCount = 3,
            subtitle = "Basics"
        )
    )

    private val flashcards = mutableListOf(
        Flashcard(
            question = "What is the powerhouse of the cell?",
            answer = "Mitochondria",
            id = "fc_bio_1",
            deckId = "deck_biology"
        ),
        Flashcard(
            question = "Which molecule carries genetic information?",
            answer = "DNA",
            id = "fc_bio_2",
            deckId = "deck_biology"
        ),
        Flashcard(
            question = "What is the main function of red blood cells?",
            answer = "Carrying oxygen",
            id = "fc_bio_3",
            deckId = "deck_biology"
        ),
        Flashcard(
            question = "What is the term for keeping internal conditions stable?",
            answer = "Homeostasis",
            id = "fc_bio_4",
            deckId = "deck_biology"
        ),
        Flashcard(
            question = "What is the largest organ in the human body?",
            answer = "Skin",
            id = "fc_bio_5",
            deckId = "deck_biology"
        ),
        Flashcard(
            question = "How do you say \"Hello\"?",
            answer = "Hola",
            id = "fc_es_1",
            deckId = "deck_spanish"
        ),
        Flashcard(
            question = "How do you say \"Thank you\"?",
            answer = "Gracias",
            id = "fc_es_2",
            deckId = "deck_spanish"
        ),
        Flashcard(
            question = "How do you say \"Goodbye\"?",
            answer = "Adiós",
            id = "fc_es_3",
            deckId = "deck_spanish"
        )
    )

    override suspend fun getDecks(): List<Deck> {
        delay(DECKS_DELAY_MS)
        return decks.toList()
    }

    override suspend fun getFlashcardsForDeck(deckId: String): List<Flashcard> {
        delay(CARDS_DELAY_MS)
        return flashcards.filter { it.deckId == deckId }
    }

    companion object {
        private const val DECKS_DELAY_MS = 450L
        private const val CARDS_DELAY_MS = 350L
    }
}
