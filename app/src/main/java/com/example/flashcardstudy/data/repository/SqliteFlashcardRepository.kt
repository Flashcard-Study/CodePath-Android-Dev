package com.example.flashcardstudy.data.repository

import android.content.Context
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.data.database.FlashcardDatabaseHelper
import com.example.flashcardstudy.data.database.StudyProgress
import com.example.flashcardstudy.data.database.StudyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteFlashcardRepository(context: Context) : FlashcardRepository {
    private val dbHelper = FlashcardDatabaseHelper(context)

    override suspend fun getDecks(): List<Deck> = withContext(Dispatchers.IO) {
        dbHelper.getAllDecks()
    }

    override suspend fun getFlashcardsForDeck(deckId: String): List<Flashcard> = withContext(Dispatchers.IO) {
        dbHelper.getFlashcardsForDeck(deckId)
    }

    override suspend fun recordStudyProgress(cardId: String, deckId: String, status: String) = withContext(Dispatchers.IO) {
        dbHelper.recordStudyProgress(cardId, deckId, status)
    }

    override suspend fun getStudyProgressForDeck(deckId: String): List<StudyProgress> = withContext(Dispatchers.IO) {
        dbHelper.getStudyProgressForDeck(deckId)
    }

    override suspend fun getStudyStatsForDeck(deckId: String): StudyStats = withContext(Dispatchers.IO) {
        dbHelper.getStudyStatsForDeck(deckId)
    }

    override suspend fun updateDeckLastStudied(deckId: String) = withContext(Dispatchers.IO) {
        dbHelper.updateDeckLastStudied(deckId)
    }

    override suspend fun getMostRecentlyStudiedDeck(): Deck? = withContext(Dispatchers.IO) {
        dbHelper.getMostRecentlyStudiedDeck()
    }
}
