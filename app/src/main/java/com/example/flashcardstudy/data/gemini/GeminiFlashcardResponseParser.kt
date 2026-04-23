package com.example.flashcardstudy.data.gemini

import com.example.flashcardstudy.Flashcard
import org.json.JSONObject
import java.util.UUID

object GeminiFlashcardResponseParser {
    fun parse(responseJson: String, deckId: String): List<Flashcard> {
        return parseDeckGeneration(responseJson, deckId).flashcards
    }

    fun parseDeckGeneration(responseJson: String, deckId: String): GeminiDeckGenerationResult {
        val generatedJson = extractGeneratedJson(responseJson)
        val root = JSONObject(generatedJson)
        val deckName = root.optString("deckName")
            .ifBlank { root.optString("title") }
            .ifBlank { root.optString("deckTitle") }
            .trim()
        val cardsArray = root.optJSONArray("flashcards")
            ?: root.optJSONArray("cards")
            ?: throw IllegalArgumentException("Gemini response did not include flashcards.")

        val cards = mutableListOf<Flashcard>()
        for (index in 0 until cardsArray.length()) {
            val cardObject = cardsArray.optJSONObject(index) ?: continue
            val question = cardObject.optString("question")
                .ifBlank { cardObject.optString("front") }
                .ifBlank { cardObject.optString("prompt") }
            val answer = cardObject.optString("answer")
                .ifBlank { cardObject.optString("back") }
                .ifBlank { cardObject.optString("response") }

            if (question.isNotBlank() && answer.isNotBlank()) {
                cards.add(
                    Flashcard(
                        id = UUID.randomUUID().toString(),
                        deckId = deckId,
                        question = question.trim(),
                        answer = answer.trim()
                    )
                )
            }
        }

        if (cards.isEmpty()) {
            throw IllegalArgumentException("Gemini response did not include any valid flashcards.")
        }

        return GeminiDeckGenerationResult(
            deckName = deckName,
            flashcards = cards
        )
    }

    private fun extractGeneratedJson(responseJson: String): String {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalArgumentException("Gemini response did not include candidates.")
        val candidate = candidates.optJSONObject(0)
            ?: throw IllegalArgumentException("Gemini response did not include a usable candidate.")
        val content = candidate.optJSONObject("content")
            ?: throw IllegalArgumentException("Gemini response did not include content.")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalArgumentException("Gemini response did not include parts.")

        val builder = StringBuilder()
        for (index in 0 until parts.length()) {
            val part = parts.optJSONObject(index) ?: continue
            builder.append(part.optString("text"))
        }

        val rawText = builder.toString().trim()
        if (rawText.isBlank()) {
            throw IllegalArgumentException("Gemini response did not include any text.")
        }

        return unwrapJson(rawText)
    }

    private fun unwrapJson(text: String): String {
        val cleaned = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val start = cleaned.indexOfFirst { it == '{' || it == '[' }
        val end = cleaned.lastIndexOfFirst { it == '}' || it == ']' }

        if (start == -1 || end == -1 || end <= start) {
            throw IllegalArgumentException("Gemini response was not valid JSON.")
        }

        return cleaned.substring(start, end + 1)
    }

    private fun String.lastIndexOfFirst(predicate: (Char) -> Boolean): Int {
        for (index in lastIndex downTo 0) {
            if (predicate(this[index])) {
                return index
            }
        }
        return -1
    }
}
