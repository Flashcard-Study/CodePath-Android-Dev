package com.example.flashcardstudy.data.repository

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard

interface FlashcardRepository {
    suspend fun getDecks(): List<Deck>
    suspend fun getFlashcardsForDeck(deckId: String): List<Flashcard>
}
