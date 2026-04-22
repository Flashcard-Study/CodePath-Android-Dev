package com.example.flashcardstudy.data.repository

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.data.database.StudyProgress
import com.example.flashcardstudy.data.database.StudyStats
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

    override suspend fun createDeck(deck: Deck): Boolean {
        decks.add(deck)
        return true
    }

    override suspend fun addFlashcard(flashcard: Flashcard): Boolean {
        flashcards.add(flashcard)
        return true
    }

    override suspend fun importDeckWithCards(deck: Deck, cards: List<Flashcard>): Boolean {
        decks.add(deck)
        flashcards.addAll(cards)
        return true
    }

    override suspend fun getFlashcardsForDeck(deckId: String): List<Flashcard> {
        delay(CARDS_DELAY_MS)
        return flashcards.filter { it.deckId == deckId }
    }

    override suspend fun recordStudyProgress(cardId: String, deckId: String, status: String) {
    }

    override suspend fun getStudyProgressForDeck(deckId: String): List<StudyProgress> {
        return emptyList()
    }

    override suspend fun getStudyStatsForDeck(deckId: String): StudyStats {
        return StudyStats()
    }

    override suspend fun updateDeckLastStudied(deckId: String) {
    }

    override suspend fun getMostRecentlyStudiedDeck(): Deck? {
        return decks.firstOrNull()
    }

    companion object {
        private const val DECKS_DELAY_MS = 450L
        private const val CARDS_DELAY_MS = 350L
    }
}
