package com.example.flashcardstudy.ui.newdeck

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.gemini.GeminiFlashcardGenerator
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.util.NetworkUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.UUID

class NewDeckFragment : Fragment(R.layout.fragment_new_deck) {
    private val colorAccents = listOf(
        "#4A7C3A",
        "#C66B1F",
        "#B53D2E",
        "#0E8A5F",
        "#C84B7C",
        "#2E5BFF",
        "#6B4FE8",
        "#D18A00",
        "#0F7A8A"
    )
    private val icons = listOf("📚", "🧪", "🌍", "📈", "🪶", "🧭", "💻", "🧠", "🎨", "🎵", "✨", "🎯")

    private var selectedColor = "#4A7C3A"
    private var selectedIcon = "📚"
    private var seedAiEnabled = false
    private var seedCardCount = 8

    private val colorViews = mutableListOf<View>()
    private val iconViews = mutableListOf<MaterialCardView>()
    private var createButton: MaterialButton? = null

    private val prefs by lazy {
        requireActivity().getSharedPreferences("new_deck_prefs", Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restorePrefs()

        val tilDeckName = view.findViewById<TextInputLayout>(R.id.tilDeckName)
        val etDeckName = view.findViewById<TextInputEditText>(R.id.etDeckName)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreateDeck)
        val closeButton = view.findViewById<ImageButton>(R.id.newDeckClose)
        val switchSeedAi = view.findViewById<MaterialSwitch>(R.id.switchSeedAi)
        val seedCountRow = view.findViewById<LinearLayout>(R.id.seedCountRow)
        val seedCountMinus = view.findViewById<ImageButton>(R.id.seedCountMinus)
        val seedCountPlus = view.findViewById<ImageButton>(R.id.seedCountPlus)
        val seedCountValue = view.findViewById<TextView>(R.id.seedCountValue)
        val seedPromptLayout = view.findViewById<TextInputLayout>(R.id.seedPromptLayout)
        val seedPromptInput = view.findViewById<TextInputEditText>(R.id.seedPromptInput)

        createButton = btnCreate
        setupColorPicker(view)
        setupIconPicker(view)

        etDeckName.setText(prefs.getString("draft_deck_name", ""))
        seedPromptInput.setText(prefs.getString("seed_prompt", ""))
        seedPromptInput.setOnFocusChangeListener { _, _ ->
            prefs.edit {
                putString("seed_prompt", seedPromptInput.text?.toString().orEmpty())
            }
        }

        switchSeedAi.isChecked = seedAiEnabled
        seedCountRow.visibility = if (seedAiEnabled) View.VISIBLE else View.GONE
        seedPromptLayout.visibility = if (seedAiEnabled) View.VISIBLE else View.GONE
        seedCountValue.text = seedCardCount.toString()
        btnCreate.backgroundTintList = ColorStateList.valueOf(selectedColor.toColorInt())

        closeButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToHome()
        }

        switchSeedAi.setOnCheckedChangeListener { _, isChecked ->
            seedAiEnabled = isChecked
            seedCountRow.visibility = if (isChecked) View.VISIBLE else View.GONE
            seedPromptLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit { putBoolean("seed_ai", seedAiEnabled) }
        }

        seedCountMinus.setOnClickListener {
            seedCardCount = (seedCardCount - 1).coerceAtLeast(1)
            seedCountValue.text = seedCardCount.toString()
            prefs.edit { putInt("seed_count", seedCardCount) }
        }

        seedCountPlus.setOnClickListener {
            seedCardCount = (seedCardCount + 1).coerceAtMost(25)
            seedCountValue.text = seedCardCount.toString()
            prefs.edit { putInt("seed_count", seedCardCount) }
        }

        btnCreate.setOnClickListener {
            val name = etDeckName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                tilDeckName.error = "Please enter a deck name"
                return@setOnClickListener
            }
            tilDeckName.error = null
            prefs.edit {
                putString("draft_deck_name", name)
                putString("seed_prompt", seedPromptInput.text?.toString().orEmpty())
            }
            createDeckAndOptionallySeed(
                deckName = name,
                seedPrompt = seedPromptInput.text?.toString()?.trim().orEmpty(),
                createButton = btnCreate
            )
        }
    }

    private fun createDeckAndOptionallySeed(
        deckName: String,
        seedPrompt: String,
        createButton: MaterialButton
    ) {
        val deckId = UUID.randomUUID().toString()
        val deck = Deck(
            id = deckId,
            name = deckName,
            cardCount = 0,
            color = selectedColor,
            icon = selectedIcon,
            isPublic = false
        )

        createButton.isEnabled = false
        createButton.text = "Creating..."

        lifecycleScope.launch {
            val createSuccess = RepositoryProvider.flashcardRepository.createDeck(deck)
            if (!createSuccess) {
                createButton.isEnabled = true
                createButton.text = "Create Deck"
                Snackbar.make(requireView(), "Failed to create deck", Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            if (seedAiEnabled) {
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    Toast.makeText(
                        requireContext(),
                        "\"$deckName\" created. Network unavailable, skipped AI seeding.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    createButton.text = "Generating starter cards..."
                    try {
                        val promptTopic = seedPrompt.ifBlank {
                            deckName.ifBlank { "General review" }
                        }
                        val generatedCards = GeminiFlashcardGenerator.generateFlashcards(
                            topic = promptTopic,
                            deckId = deckId,
                            count = seedCardCount.coerceIn(1, 25)
                        )
                        var savedCount = 0
                        generatedCards.forEach { card ->
                            if (RepositoryProvider.flashcardRepository.addFlashcard(card)) {
                                savedCount++
                            }
                        }
                        Toast.makeText(
                            requireContext(),
                            "\"$deckName\" created with $savedCount starter cards.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (error: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "\"$deckName\" created. AI seeding failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "\"$deckName\" deck created!", Toast.LENGTH_SHORT)
                    .show()
            }

            createButton.isEnabled = true
            createButton.text = "Create Deck"
            prefs.edit { remove("draft_deck_name") }
            (activity as? MainActivity)?.navigateToHome()
        }
    }

    private fun restorePrefs() {
        selectedColor = prefs.getString("selected_color", "#4A7C3A") ?: "#4A7C3A"
        if (!colorAccents.contains(selectedColor)) {
            selectedColor = colorAccents.first()
        }
        val storedIcon = prefs.getString("selected_icon", "📚") ?: "📚"
        selectedIcon = if (icons.contains(storedIcon)) storedIcon else icons.first()
        seedAiEnabled = prefs.getBoolean("seed_ai", false)
        seedCardCount = prefs.getInt("seed_count", 8).coerceIn(1, 25)
    }

    private fun setupColorPicker(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.colorPickerContainer)
        val size = (44 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()
        container.removeAllViews()
        colorViews.clear()

        colorAccents.forEachIndexed { index, hex ->
            val dot = View(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = margin
            dot.layoutParams = params
            dot.background = makeCircle(hex, hex == selectedColor)
            dot.setOnClickListener { selectColor(index) }
            colorViews.add(dot)
            container.addView(dot)
        }
    }

    private fun setupIconPicker(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.iconPickerContainer)
        val size = (48 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()
        container.removeAllViews()
        iconViews.clear()

        icons.forEachIndexed { index, icon ->
            val card = MaterialCardView(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = margin
            card.layoutParams = params
            card.radius = 10 * resources.displayMetrics.density
            card.cardElevation = 0f
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            applyIconSelectionStyle(card, icon == selectedIcon)

            val label = TextView(requireContext())
            label.text = icon
            label.textSize = 20f
            label.gravity = Gravity.CENTER
            label.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            card.addView(label)
            card.setOnClickListener { selectIcon(index) }
            iconViews.add(card)
            container.addView(card)
        }
    }

    private fun selectColor(index: Int) {
        selectedColor = colorAccents[index]
        prefs.edit { putString("selected_color", selectedColor) }
        colorViews.forEachIndexed { i, v -> v.background = makeCircle(colorAccents[i], i == index) }
        createButton?.backgroundTintList = ColorStateList.valueOf(selectedColor.toColorInt())
        iconViews.forEachIndexed { i, card ->
            applyIconSelectionStyle(card, icons[i] == selectedIcon)
        }
    }

    private fun selectIcon(index: Int) {
        selectedIcon = icons[index]
        prefs.edit { putString("selected_icon", selectedIcon) }
        iconViews.forEachIndexed { i, card ->
            applyIconSelectionStyle(card, i == index)
        }
    }

    private fun applyIconSelectionStyle(card: MaterialCardView, selected: Boolean) {
        card.strokeColor = if (selected) selectedColor.toColorInt() else "#DCD4C2".toColorInt()
        card.setCardBackgroundColor(if (selected) "#EADBF5".toColorInt() else "#FBF7EE".toColorInt())
    }

    private fun makeCircle(hex: String, selected: Boolean): android.graphics.drawable.Drawable {
        val color = hex.toColorInt()
        return if (selected) {
            val filled = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            val ring = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), "#1A1D24".toColorInt())
                setColor(Color.TRANSPARENT)
            }
            LayerDrawable(arrayOf(ring, filled)).apply {
                setLayerInset(1, dp(5), dp(5), dp(5), dp(5))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
