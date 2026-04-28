package com.example.flashcardstudy.data.transfer

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import org.json.JSONException
import org.json.JSONObject

object DeckTransferParser {
    fun parse(json: String): DeckTransferPayload {
        val root = try {
            JSONObject(json)
        } catch (error: JSONException) {
            throw DeckTransferException("Invalid JSON format.", error)
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != DeckTransferSchema.CURRENT_SCHEMA_VERSION) {
            throw DeckTransferException(
                "Unsupported schema version: $schemaVersion. Expected ${DeckTransferSchema.CURRENT_SCHEMA_VERSION}."
            )
        }

        val deckObject = root.optJSONObject("deck")
        val deckName = firstNonBlank(
            deckObject?.optString("name"),
            root.optString("deckName")
        ) ?: throw DeckTransferException("Deck name is missing.")

        val subtitle = deckObject?.optString("subtitle").orEmpty()
        val color = firstNonBlank(deckObject?.optString("color")) ?: "#6C63FF"
        val icon = deckObject?.optString("icon").orEmpty()
        val isPublic = when {
            deckObject?.has("isPublic") == true -> deckObject.optBoolean("isPublic", false)
            else -> root.optBoolean("isPublic", false)
        }

        val cardArray = root.optJSONArray("cards") ?: root.optJSONArray("flashcards")
        ?: throw DeckTransferException("No cards were found in the file.")

        val cards = mutableListOf<Flashcard>()
        for (index in 0 until cardArray.length()) {
            val item = cardArray.optJSONObject(index)
                ?: throw DeckTransferException("Card ${index + 1} is not a valid object.")

            val question = firstNonBlank(
                item.optString("question"),
                item.optString("front"),
                item.optString("prompt")
            ) ?: throw DeckTransferException("Card ${index + 1} is missing a question.")

            val answer = firstNonBlank(
                item.optString("answer"),
                item.optString("back"),
                item.optString("response")
            ) ?: throw DeckTransferException("Card ${index + 1} is missing an answer.")

            cards.add(
                Flashcard(
                    question = question,
                    answer = answer
                )
            )
        }

        val deck = Deck(
            id = "",
            name = deckName,
            cardCount = cards.size,
            subtitle = subtitle,
            color = color,
            icon = icon,
            isPublic = isPublic
        )

        return DeckTransferPayload(deck = deck, cards = cards)
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }
}
