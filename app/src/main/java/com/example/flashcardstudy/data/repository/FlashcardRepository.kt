package com.example.flashcardstudy.data.repository

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.data.database.StudyProgress
import com.example.flashcardstudy.data.database.StudyStats

interface FlashcardRepository {
    suspend fun getDecks(): List<Deck>
    suspend fun createDeck(deck: Deck): Boolean
    suspend fun addFlashcard(flashcard: Flashcard): Boolean
    suspend fun getFlashcardsForDeck(deckId: String): List<Flashcard>
    suspend fun recordStudyProgress(cardId: String, deckId: String, status: String)
    suspend fun getStudyProgressForDeck(deckId: String): List<StudyProgress>
    suspend fun getStudyStatsForDeck(deckId: String): StudyStats
    suspend fun updateDeckLastStudied(deckId: String)
    suspend fun getMostRecentlyStudiedDeck(): Deck?
}
