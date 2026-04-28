package com.example.flashcardstudy.ui.addcard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.gemini.GeminiFlashcardGenerator
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.util.NetworkUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.UUID

class AddCardFragment : Fragment(R.layout.fragment_add_card) {

    private var deckId: String? = null
    private var deckName: String? = null
    private var deckColor: String? = null
    private val aiSuggestions = mutableListOf<AiSuggestion>()
    private val acceptedSuggestionIds = mutableSetOf<Int>()
    private var generateCount = 8
    private var isAiPanelExpanded = false
    private val prefs by lazy {
        requireActivity().getSharedPreferences("add_card_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckId = arguments?.getString(ARG_DECK_ID)
        deckName = arguments?.getString(ARG_DECK_NAME)
        deckColor = arguments?.getString(ARG_DECK_COLOR)
        isAiPanelExpanded = savedInstanceState?.getBoolean(KEY_AI_PANEL_EXPANDED, false) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AI_PANEL_EXPANDED, isAiPanelExpanded)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val frontInput = view.findViewById<EditText>(R.id.editTextFront)
        val backInput = view.findViewById<EditText>(R.id.editTextBack)
        val saveButton = view.findViewById<MaterialButton>(R.id.buttonSave)
        val addAnotherButton = view.findViewById<MaterialButton>(R.id.buttonAddAnother)
        val closeButton = view.findViewById<ImageButton>(R.id.addCardClose)
        val deckBanner = view.findViewById<CardView>(R.id.deckBanner)
        val tvDeckName = view.findViewById<TextView>(R.id.tvDeckName)
        val colorDot = view.findViewById<View>(R.id.deckColorDot)
        val aiPanelHeader = view.findViewById<View>(R.id.aiPanelHeader)
        val aiPanelContent = view.findViewById<LinearLayout>(R.id.aiPanelContent)
        val aiTopicInput = view.findViewById<EditText>(R.id.aiTopicInput)
        val generateAiButton = view.findViewById<MaterialButton>(R.id.buttonGenerateAi)
        val generateCountMinus = view.findViewById<ImageButton>(R.id.generateCountMinus)
        val generateCountPlus = view.findViewById<ImageButton>(R.id.generateCountPlus)
        val generateCountValue = view.findViewById<TextView>(R.id.generateCountValue)
        val aiAcceptedCount = view.findViewById<TextView>(R.id.aiAcceptedCount)
        val aiSuggestionsContainer = view.findViewById<LinearLayout>(R.id.aiSuggestionsContainer)
        generateCount = prefs.getInt("seed_count", 8).coerceIn(1, 25)
        aiTopicInput.setText(prefs.getString("seed_prompt", ""))
        aiPanelContent.visibility = if (isAiPanelExpanded) View.VISIBLE else View.GONE

        val currentDeckId = deckId
        val currentDeckName = deckName

        if (currentDeckId != null && currentDeckName != null) {
            deckBanner.visibility = View.VISIBLE
            tvDeckName.text = currentDeckName
            val color = deckColor ?: "#6C63FF"
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color.toColorInt())
            }
            colorDot.background = dot
        }

        closeButton.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                (activity as? MainActivity)?.navigateToHome()
            }
        }

        aiPanelHeader.setOnClickListener {
            isAiPanelExpanded = !isAiPanelExpanded
            aiPanelContent.visibility = if (isAiPanelExpanded) View.VISIBLE else View.GONE
        }

        generateCountMinus.setOnClickListener {
            generateCount = (generateCount - 1).coerceAtLeast(1)
            prefs.edit { putInt("seed_count", generateCount) }
            updateGenerateCountUi(generateAiButton, generateCountValue)
        }

        generateCountPlus.setOnClickListener {
            generateCount = (generateCount + 1).coerceAtMost(25)
            prefs.edit { putInt("seed_count", generateCount) }
            updateGenerateCountUi(generateAiButton, generateCountValue)
        }
        updateGenerateCountUi(generateAiButton, generateCountValue)

        generateAiButton.setOnClickListener {
            if (currentDeckId == null) {
                Toast.makeText(
                    requireContext(),
                    "No deck selected. Open a deck first.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    "No network connection available.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val targetDeckId = currentDeckId

            val topic = aiTopicInput.text.toString().trim()
            prefs.edit { putString("seed_prompt", topic) }
            val seed = if (topic.isEmpty()) {
                currentDeckName?.trim().orEmpty().ifBlank { "General review" }
            } else {
                topic
            }
            generateAiButton.isEnabled = false
            generateAiButton.text = "Generating..."

            lifecycleScope.launch {
                try {
                    val deckContext =
                        buildDeckContext(targetDeckId, currentDeckName).takeIf { it.isNotBlank() }
                    val generation = GeminiFlashcardGenerator.generateDeckGeneration(
                        topic = seed,
                        deckId = targetDeckId,
                        count = generateCount.coerceIn(1, 25),
                        deckContext = deckContext
                    )
                    val generatedCards = generation.flashcards
                    aiSuggestions.clear()
                    acceptedSuggestionIds.clear()
                    aiSuggestions.addAll(generatedCards.toSuggestions())
                    renderSuggestions(aiSuggestionsContainer, aiAcceptedCount, saveButton)
                    Toast.makeText(
                        requireContext(),
                        "Generated ${generatedCards.size} flashcards.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (error: Exception) {
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to generate with Gemini.",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    generateAiButton.isEnabled = true
                    updateGenerateCountUi(generateAiButton, generateCountValue)
                }
            }
        }

        fun saveEntries(keepOpenForAnother: Boolean) {
            val front = frontInput.text.toString().trim()
            val back = backInput.text.toString().trim()
            val accepted = aiSuggestions.filter { acceptedSuggestionIds.contains(it.id) }
            val entries = mutableListOf<Pair<String, String>>()

            if (front.isNotEmpty() && back.isNotEmpty()) {
                entries += front to back
            }

            accepted.forEach { suggestion ->
                entries += suggestion.question to suggestion.answer
            }

            if (entries.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Add card content or accept AI suggestions.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (currentDeckId == null) {
                Toast.makeText(
                    requireContext(),
                    "No deck selected. Open a deck first.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val targetDeckId = currentDeckId

            lifecycleScope.launch {
                var savedCount = 0
                entries.forEach { (question, answer) ->
                    val flashcard = Flashcard(
                        id = UUID.randomUUID().toString(),
                        deckId = targetDeckId,
                        question = question,
                        answer = answer
                    )
                    if (RepositoryProvider.flashcardRepository.addFlashcard(flashcard)) {
                        savedCount++
                    }
                }

                if (savedCount > 0) {
                    frontInput.text.clear()
                    backInput.text.clear()
                    aiSuggestions.clear()
                    acceptedSuggestionIds.clear()
                    renderSuggestions(aiSuggestionsContainer, aiAcceptedCount, saveButton)
                    Toast.makeText(
                        requireContext(),
                        "$savedCount card(s) saved to \"${currentDeckName ?: "deck"}\".",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (keepOpenForAnother) {
                        frontInput.requestFocus()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to save card.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        saveButton.setOnClickListener {
            saveEntries(keepOpenForAnother = false)
        }

        addAnotherButton.setOnClickListener {
            saveEntries(keepOpenForAnother = true)
        }

        updateSaveButtonLabel(saveButton)
    }

    private fun renderSuggestions(
        container: LinearLayout,
        counter: TextView,
        saveButton: MaterialButton
    ) {
        container.removeAllViews()
        aiSuggestions.forEach { suggestion ->
            container.addView(buildSuggestionView(suggestion, saveButton, counter))
        }
        counter.text = "AI suggestions - ${acceptedSuggestionIds.size}/${aiSuggestions.size}"
        updateSaveButtonLabel(saveButton)
    }

    private fun buildSuggestionView(
        suggestion: AiSuggestion,
        saveButton: MaterialButton,
        counter: TextView
    ): View {
        val card = CardView(requireContext()).apply {
            radius = 14f * resources.displayMetrics.density
            cardElevation = 0f
            useCompatPadding = false
            setCardBackgroundColor("#F9F4ED".toColorInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        card.addView(inner)

        val questionText = TextView(requireContext()).apply {
            text = suggestion.question
            textSize = 15f
            setTextColor("#1A1D24".toColorInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        val answerText = TextView(requireContext()).apply {
            text = suggestion.answer
            textSize = 12f
            setTextColor("#6E6A5F".toColorInt())
            setPadding(0, dp(6), 0, 0)
        }

        inner.addView(questionText)
        inner.addView(answerText)

        card.setOnClickListener {
            if (acceptedSuggestionIds.contains(suggestion.id)) {
                acceptedSuggestionIds.remove(suggestion.id)
                card.setCardBackgroundColor("#F9F4ED".toColorInt())
                questionText.setTextColor("#1A1D24".toColorInt())
                answerText.setTextColor("#6E6A5F".toColorInt())
            } else {
                acceptedSuggestionIds.add(suggestion.id)
                card.setCardBackgroundColor("#1A1D24".toColorInt())
                questionText.setTextColor(Color.WHITE)
                answerText.setTextColor("#EADCCF".toColorInt())
            }

            counter.text = "AI suggestions - ${acceptedSuggestionIds.size}/${aiSuggestions.size}"
            updateSaveButtonLabel(saveButton)
        }

        return card
    }

    private fun updateSaveButtonLabel(saveButton: MaterialButton) {
        val acceptedCount = acceptedSuggestionIds.size
        saveButton.text = if (acceptedCount > 0) {
            "Save card + $acceptedCount from AI"
        } else {
            "Save card"
        }
    }

    private fun updateGenerateCountUi(generateButton: MaterialButton, countView: TextView) {
        countView.text = generateCount.toString()
        generateButton.text = "Generate $generateCount cards"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun List<Flashcard>.toSuggestions(): List<AiSuggestion> {
        return mapIndexed { index, card ->
            AiSuggestion(
                id = index,
                question = card.question,
                answer = card.answer
            )
        }
    }

    private suspend fun buildDeckContext(deckId: String, deckName: String?): String {
        val cards = RepositoryProvider.flashcardRepository.getFlashcardsForDeck(deckId)
        val sampleCards = cards.take(8)
        val contextBuilder = StringBuilder()

        if (!deckName.isNullOrBlank()) {
            contextBuilder.appendLine("Deck name: $deckName")
        }

        if (sampleCards.isNotEmpty()) {
            contextBuilder.appendLine("Existing cards:")
            sampleCards.forEachIndexed { index, card ->
                contextBuilder.appendLine("${index + 1}. ${card.question} -> ${card.answer}")
            }
        }

        return contextBuilder.toString().trim()
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val ARG_DECK_NAME = "deck_name"
        private const val ARG_DECK_COLOR = "deck_color"
        private const val KEY_AI_PANEL_EXPANDED = "key_ai_panel_expanded"

        fun newInstance(
            deckId: String,
            deckName: String,
            deckColor: String = "#6C63FF"
        ): AddCardFragment {
            return AddCardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DECK_ID, deckId)
                    putString(ARG_DECK_NAME, deckName)
                    putString(ARG_DECK_COLOR, deckColor)
                }
            }
        }
    }

    private data class AiSuggestion(
        val id: Int,
        val question: String,
        val answer: String
    )
}
