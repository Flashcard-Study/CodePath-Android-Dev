package com.example.flashcardstudy.data.transfer

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import org.json.JSONArray
import org.json.JSONObject

object DeckTransferSerializer {
    fun toJson(deck: Deck, cards: List<Flashcard>): String {
        val root = JSONObject()
            .put("schemaVersion", DeckTransferSchema.CURRENT_SCHEMA_VERSION)
            .put(
                "deck",
                JSONObject()
                    .put("name", deck.name)
                    .put("subtitle", deck.subtitle)
                    .put("color", deck.color)
                    .put("icon", deck.icon)
                    .put("isPublic", deck.isPublic)
            )
            .put(
                "cards",
                JSONArray().apply {
                    cards.forEach { card ->
                        put(
                            JSONObject()
                                .put("question", card.question)
                                .put("answer", card.answer)
                        )
                    }
                }
            )
            .put(
                "metadata",
                JSONObject()
                    .put("source", "flashcard_study_android")
                    .put("exportedAtEpochMs", System.currentTimeMillis())
            )

        return root.toString(2)
    }
}
