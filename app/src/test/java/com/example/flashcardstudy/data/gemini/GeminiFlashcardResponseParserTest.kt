package com.example.flashcardstudy.data.gemini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiFlashcardResponseParserTest {

    @Test
    fun parse_validGeminiResponse_parsesFlashcards() {
        val responseJson = readFixture("data/gemini/gemini_flashcards_response.json")

        val cards = GeminiFlashcardResponseParser.parse(responseJson, "deck-123")

        assertEquals(5, cards.size)
        assertEquals("What is photosynthesis?", cards[0].question)
        assertEquals("Process plants use to convert light into energy", cards[0].answer)
        assertEquals("deck-123", cards[0].deckId)
    }

    @Test
    fun parseDeckGeneration_validGeminiResponse_parsesDeckTitleAndFlashcards() {
        val responseJson = readFixture("data/gemini/gemini_flashcards_with_title_response.json")

        val result = GeminiFlashcardResponseParser.parseDeckGeneration(responseJson, "deck-123")

        assertEquals("Cell Biology Basics", result.deckName)
        assertEquals(5, result.flashcards.size)
        assertEquals("What is photosynthesis?", result.flashcards[0].question)
    }

    @Test
    fun parse_responseWithoutCards_throws() {
        val responseJson = readFixture("data/gemini/gemini_flashcards_empty_response.json")

        try {
            GeminiFlashcardResponseParser.parse(responseJson, "deck-123")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message?.contains("flashcards") == true)
            return
        }

        throw AssertionError("Expected IllegalArgumentException")
    }

    private fun readFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw AssertionError("Missing fixture: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
