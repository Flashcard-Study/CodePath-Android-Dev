package com.example.flashcardstudy.ui.addcard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.gemini.GeminiFlashcardGenerator
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.util.NetworkUtils
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch
import java.util.UUID

class AddCardFragment : Fragment(R.layout.fragment_add_card) {

    private var deckId: String? = null
    private var deckName: String? = null
    private var deckColor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckId = arguments?.getString(ARG_DECK_ID)
        deckName = arguments?.getString(ARG_DECK_NAME)
        deckColor = arguments?.getString(ARG_DECK_COLOR)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val frontInput = view.findViewById<EditText>(R.id.editTextFront)
        val backInput = view.findViewById<EditText>(R.id.editTextBack)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val generateAiButton = view.findViewById<Button>(R.id.buttonGenerateAi)
        val addAnotherButton = view.findViewById<Button>(R.id.buttonAddAnother)
        val deleteButton = view.findViewById<Button>(R.id.buttonDelete)
        val deckBanner = view.findViewById<CardView>(R.id.deckBanner)
        val tvDeckName = view.findViewById<TextView>(R.id.tvDeckName)
        val colorDot = view.findViewById<View>(R.id.deckColorDot)

        val currentDeckId = deckId
        val currentDeckName = deckName

        if (currentDeckId != null && currentDeckName != null) {
            deckBanner.visibility = View.VISIBLE
            tvDeckName.text = currentDeckName
            val color = deckColor ?: "#6C63FF"
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(color))
            }
            colorDot.background = dot
        }

        saveButton.setOnClickListener {
            val front = frontInput.text.toString().trim()
            val back = backInput.text.toString().trim()

            if (front.isEmpty() || back.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Enter both sides of the card.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (currentDeckId == null) {
                Toast.makeText(
                    requireContext(),
                    "No deck selected. Open a deck first.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val flashcard = Flashcard(
                id = UUID.randomUUID().toString(),
                deckId = currentDeckId,
                question = front,
                answer = back
            )

            lifecycleScope.launch {
                val success = RepositoryProvider.flashcardRepository.addFlashcard(flashcard)
                if (success) {
                    frontInput.text.clear()
                    backInput.text.clear()
                    Toast.makeText(
                        requireContext(),
                        "Card saved to \"$currentDeckName\"!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to save card.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        generateAiButton.setOnClickListener {
            showGenerateWithAiDialog(currentDeckId, currentDeckName, generateAiButton)
        }

        addAnotherButton.setOnClickListener {
            frontInput.text.clear()
            backInput.text.clear()
            frontInput.requestFocus()
        }

        deleteButton.setOnClickListener {
            frontInput.text.clear()
            backInput.text.clear()
            Toast.makeText(requireContext(), "Cleared.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGenerateWithAiDialog(
        deckId: String?,
        deckName: String?,
        actionButton: Button
    ) {
        if (deckId == null) {
            Toast.makeText(
                requireContext(),
                "No deck selected. Open a deck first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val topicInput = EditText(requireContext()).apply {
            hint = "e.g. Cellular respiration"
            minHeight = (48 * resources.displayMetrics.density).toInt()
        }
        val countInput = EditText(requireContext()).apply {
            hint = "Number of cards"
            setText("5")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            minHeight = (48 * resources.displayMetrics.density).toInt()
        }
        val useDeckContextSwitch = MaterialSwitch(requireContext()).apply {
            isChecked = true
        }
        val useDeckContextLabel = TextView(requireContext()).apply {
            text = "Use deck context"
            textSize = 16f
        }
        val contextRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(useDeckContextLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(useDeckContextSwitch)
        }

        val dialogContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(topicInput)
            addView(countInput)
            addView(contextRow)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Generate with AI")
            .setMessage("Generate flashcards for a topic and save them into this deck.")
            .setView(dialogContent)
            .setPositiveButton("Generate") { dialog, _ ->
                val topic = topicInput.text?.toString()?.trim().orEmpty()
                val count = countInput.text?.toString()?.trim()?.toIntOrNull() ?: 5

                if (topic.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Enter a topic to generate cards.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    Toast.makeText(
                        requireContext(),
                        "No network connection available.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                dialog.dismiss()

                viewLifecycleOwner.lifecycleScope.launch {
                    actionButton.isEnabled = false
                    val originalText = actionButton.text
                    actionButton.text = "Generating..."
                    try {
                        val deckContext = if (useDeckContextSwitch.isChecked) {
                            buildDeckContext(deckId, deckName)
                        } else {
                            null
                        }
                        val generation = GeminiFlashcardGenerator.generateDeckGeneration(
                            topic = topic,
                            deckId = deckId,
                            count = count.coerceIn(1, 25),
                            deckContext = deckContext
                        )

                        var savedCount = 0
                        generation.flashcards.forEach { card ->
                            if (RepositoryProvider.flashcardRepository.addFlashcard(card)) {
                                savedCount++
                            }
                        }

                        if (savedCount > 0) {
                            Toast.makeText(
                                requireContext(),
                                "Generated and saved $savedCount cards.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Generated cards could not be saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (error: Exception) {
                        Toast.makeText(
                            requireContext(),
                            error.message ?: "Failed to generate cards.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        actionButton.isEnabled = true
                        actionButton.text = originalText
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
}
