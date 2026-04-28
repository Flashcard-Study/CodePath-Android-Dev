package com.example.flashcardstudy.data.gemini

import com.example.flashcardstudy.BuildConfig
import com.example.flashcardstudy.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.IOException

object GeminiFlashcardGenerator {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val MODEL = "gemini-2.5-flash"
    private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateFlashcards(
        topic: String,
        deckId: String,
        count: Int = 5
    ): List<Flashcard> = withContext(Dispatchers.IO) {
        generateDeckGeneration(topic, deckId, count).flashcards
    }

    suspend fun generateDeckGeneration(
        topic: String,
        deckId: String,
        count: Int = 5,
        deckContext: String? = null
    ): GeminiDeckGenerationResult = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim()
        val cleanDeckId = deckId.trim()
        val cleanCount = count.coerceAtLeast(1)

        if (cleanTopic.isEmpty()) {
            throw IllegalArgumentException("Topic cannot be empty.")
        }
        if (cleanDeckId.isEmpty()) {
            throw IllegalArgumentException("Deck is required to save generated cards.")
        }

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "Gemini API key is not configured. Add GEMINI_API_KEY to local.properties."
            )
        }

        val requestBody =
            buildRequestBody(cleanTopic, cleanCount, deckContext?.trim()).toRequestBody(
                JSON_MEDIA_TYPE.toMediaType()
            )
        val response = service.generateContent(MODEL, apiKey, requestBody)

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string().orEmpty()
            throw IOException(buildErrorMessage(response.code(), errorBody))
        }

        val responseBody = response.body()?.string().orEmpty()
        if (responseBody.isBlank()) {
            throw IOException("Gemini returned an empty response.")
        }

        GeminiFlashcardResponseParser.parseDeckGeneration(responseBody, cleanDeckId)
    }

    private fun buildRequestBody(topic: String, count: Int, deckContext: String?): String {
        val prompt = buildString {
            appendLine("Generate $count flashcards about $topic.")
            appendLine("Also choose a short, specific deck title in 2 to 5 words.")
            if (!deckContext.isNullOrBlank()) {
                appendLine("Use the following deck context to make the new cards related and non-duplicate:")
                appendLine(deckContext)
            }
            appendLine("Return concise question and answer pairs.")
            appendLine("Focus on important facts a student would study.")
        }.trim()

        val flashcardSchema = JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject()
                    .put("question", JSONObject().put("type", "string"))
                    .put("answer", JSONObject().put("type", "string"))
            )
            put("required", JSONArray().put("question").put("answer"))
        }

        val responseSchema = JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject().put(
                    "deckName",
                    JSONObject()
                        .put("type", "string")
                        .put("minLength", 1)
                        .put("description", "A short title for the generated deck.")
                ).put(
                    "flashcards",
                    JSONObject().apply {
                        put("type", "array")
                        put("minItems", count)
                        put("maxItems", count)
                        put("items", flashcardSchema)
                    }
                )
            )
            put("required", JSONArray().put("deckName").put("flashcards"))
        }

        val contents = JSONArray().put(
            JSONObject().apply {
                put("role", "user")
                put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", prompt)
                    )
                )
            }
        )

        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("responseSchema", responseSchema)
        }

        return JSONObject().apply {
            put("contents", contents)
            put("generationConfig", generationConfig)
        }.toString()
    }

    private fun buildErrorMessage(code: Int, errorBody: String): String {
        if (errorBody.isBlank()) {
            return "Gemini request failed ($code)."
        }

        return try {
            val root = JSONObject(errorBody)
            val error = root.optJSONObject("error")
            val message = error?.optString("message").orEmpty()
            if (message.isBlank()) {
                "Gemini request failed ($code)."
            } else {
                "Gemini request failed ($code): $message"
            }
        } catch (_: Exception) {
            "Gemini request failed ($code)."
        }
    }
}
