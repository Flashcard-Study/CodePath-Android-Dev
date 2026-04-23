package com.example.flashcardstudy.data.gemini

import com.example.flashcardstudy.Flashcard

data class GeminiDeckGenerationResult(
    val deckName: String,
    val flashcards: List<Flashcard>
)
