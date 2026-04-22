package com.example.flashcardstudy.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckTransferParserTest {

    @Test
    fun parse_validSchemaFromFixture_parsesDeckAndCards() {
        val json = readFixture("data/transfer/deck_valid_v1.json")

        val payload = DeckTransferParser.parse(json)

        assertEquals("Biology", payload.deck.name)
        assertEquals("Chapter 1", payload.deck.subtitle)
        assertEquals("#118AB2", payload.deck.color)
        assertEquals("🔬", payload.deck.icon)
        assertTrue(payload.deck.isPublic)
        assertEquals(2, payload.cards.size)
        assertEquals("What is ATP?", payload.cards[0].question)
        assertEquals("Cell energy currency", payload.cards[0].answer)
    }

    @Test
    fun parse_geminiStyleFieldsFromFixture_parsesCards() {
        val json = readFixture("data/transfer/deck_gemini_style.json")

        val payload = DeckTransferParser.parse(json)

        assertEquals("Gemini Import", payload.deck.name)
        assertEquals(2, payload.cards.size)
        assertEquals("Term 1", payload.cards[0].question)
        assertEquals("Definition 1", payload.cards[0].answer)
        assertEquals("Term 2", payload.cards[1].question)
        assertEquals("Definition 2", payload.cards[1].answer)
    }

    @Test
    fun parse_unsupportedSchemaFromFixture_throws() {
        val json = readFixture("data/transfer/deck_unsupported_schema.json")

        try {
            DeckTransferParser.parse(json)
        } catch (error: DeckTransferException) {
            assertTrue(error.message?.contains("Unsupported schema version") == true)
            return
        }
        throw AssertionError("Expected DeckTransferException")
    }

    @Test
    fun parse_corruptJsonFromFixture_throws() {
        val invalidJson = readFixture("data/transfer/deck_corrupt.json")

        try {
            DeckTransferParser.parse(invalidJson)
        } catch (error: DeckTransferException) {
            assertTrue(error.message?.contains("Invalid JSON format") == true)
            return
        }
        throw AssertionError("Expected DeckTransferException")
    }

    private fun readFixture(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: throw AssertionError("Missing fixture: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
