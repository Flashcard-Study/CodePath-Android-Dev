package com.example.flashcardstudy.ui.newdeck

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.data.transfer.DeckTransferException
import com.example.flashcardstudy.data.transfer.DeckTransferParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.core.graphics.toColorInt
import androidx.core.content.edit

class NewDeckFragment : Fragment(R.layout.fragment_new_deck) {

    private val colors =
        listOf("#6C63FF", "#FF6B6B", "#FFD166", "#4CAF50", "#118AB2", "#EF476F", "#F77F00")
    private val icons = listOf("📚", "🔬", "🧮", "🌍", "💡", "⚡", "🎯", "🏆", "📝", "🧠")
    private var selectedColor = "#6C63FF"
    private var selectedIcon = "📚"
    private val colorViews = mutableListOf<View>()
    private val iconViews = mutableListOf<MaterialCardView>()

    private val prefs by lazy {
        requireActivity().getSharedPreferences("new_deck_prefs", Context.MODE_PRIVATE)
    }
    private val importDeckLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importDeckFromUri(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restorePrefs()

        setupColorPicker(view)
        setupIconPicker(view)

        val switchPublic = view.findViewById<MaterialSwitch>(R.id.switchPublic)
        val tvLabel = view.findViewById<TextView>(R.id.tvVisibilityLabel)
        val tvSub = view.findViewById<TextView>(R.id.tvVisibilitySub)
        val tilDeckName = view.findViewById<TextInputLayout>(R.id.tilDeckName)
        val etDeckName = view.findViewById<TextInputEditText>(R.id.etDeckName)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreateDeck)
        val btnImport = view.findViewById<MaterialButton>(R.id.btnImportDeck)

        switchPublic.isChecked = prefs.getBoolean("is_public", false)
        tvLabel.text = if (switchPublic.isChecked) "Public deck" else "Private deck"
        tvSub.text =
            if (switchPublic.isChecked) "Anyone can find this deck" else "Only you can see this deck"

        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            tvLabel.text = if (isChecked) "Public deck" else "Private deck"
            tvSub.text =
                if (isChecked) "Anyone can find this deck" else "Only you can see this deck"
            prefs.edit { putBoolean("is_public", isChecked) }
        }

        btnCreate.setOnClickListener {
            val name = etDeckName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilDeckName.error = "Please enter a deck name"
                return@setOnClickListener
            }
            tilDeckName.error = null

            val deck = Deck(
                id = UUID.randomUUID().toString(),
                name = name,
                cardCount = 0,
                color = selectedColor,
                icon = selectedIcon,
                isPublic = switchPublic.isChecked
            )

            Log.d("NewDeck", "Creating deck: $deck")

            lifecycleScope.launch {
                val success = RepositoryProvider.flashcardRepository.createDeck(deck)
                if (success) {
                    Toast.makeText(requireContext(), "\"$name\" deck created!", Toast.LENGTH_SHORT)
                        .show()
                    (activity as? com.example.flashcardstudy.MainActivity)?.navigateToHome()
                } else {
                    Snackbar.make(view, "Failed to create deck", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        btnImport.setOnClickListener {
            importDeckLauncher.launch(arrayOf("application/json", "text/plain"))
        }
    }

    private fun restorePrefs() {
        selectedColor = prefs.getString("selected_color", "#6C63FF") ?: "#6C63FF"
        selectedIcon = prefs.getString("selected_icon", "📚") ?: "📚"
    }

    private fun setupColorPicker(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.colorPickerContainer)
        val size = (40 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()

        colors.forEachIndexed { index, hex ->
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

        icons.forEachIndexed { index, emoji ->
            val card = MaterialCardView(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = margin
            card.layoutParams = params
            card.radius = 10 * resources.displayMetrics.density
            card.cardElevation = 0f
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            val isSelected = emoji == selectedIcon
            card.strokeColor =
                if (isSelected) "#6C63FF".toColorInt() else "#DDDDDD".toColorInt()
            card.setCardBackgroundColor(
                if (isSelected) "#EEF0FF".toColorInt() else "#F2F2F7".toColorInt()
            )

            val label = TextView(requireContext())
            label.text = emoji
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
        selectedColor = colors[index]
        prefs.edit { putString("selected_color", selectedColor) }
        colorViews.forEachIndexed { i, v -> v.background = makeCircle(colors[i], i == index) }
    }

    private fun selectIcon(index: Int) {
        selectedIcon = icons[index]
        prefs.edit { putString("selected_icon", selectedIcon) }
        iconViews.forEachIndexed { i, card ->
            card.strokeColor =
                if (i == index) "#6C63FF".toColorInt() else "#DDDDDD".toColorInt()
            card.setCardBackgroundColor(
                if (i == index) "#EEF0FF".toColorInt() else "#F2F2F7".toColorInt()
            )
        }
    }

    private fun makeCircle(hex: String, selected: Boolean): android.graphics.drawable.Drawable {
        val color = hex.toColorInt()
        return if (selected) {
            val filled = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            val ring = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setStroke(
                4,
                color
            ); setColor(Color.TRANSPARENT)
            }
            LayerDrawable(arrayOf(ring, filled)).apply { setLayerInset(1, 6, 6, 6, 6) }
        } else {
            GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        }
    }

    private fun importDeckFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = readTextFromUri(uri)
                val payload = DeckTransferParser.parse(json)
                val importedDeckId = UUID.randomUUID().toString()

                val deckToImport = payload.deck.copy(
                    id = importedDeckId,
                    cardCount = payload.cards.size
                )
                val cardsToImport = payload.cards.map { card ->
                    Flashcard(
                        id = UUID.randomUUID().toString(),
                        deckId = importedDeckId,
                        question = card.question,
                        answer = card.answer
                    )
                }

                val imported = RepositoryProvider.flashcardRepository.importDeckWithCards(
                    deck = deckToImport,
                    cards = cardsToImport
                )
                if (!imported) {
                    throw DeckTransferException("Failed to import this deck.")
                }

                Toast.makeText(
                    requireContext(),
                    "Imported \"${deckToImport.name}\" with ${cardsToImport.size} cards.",
                    Toast.LENGTH_SHORT
                ).show()
                (activity as? com.example.flashcardstudy.MainActivity)?.navigateToHome()
            } catch (error: DeckTransferException) {
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Invalid import file.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Could not import file. Please select a valid JSON deck export.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
            ?: throw DeckTransferException("Could not open the selected file.")
        return inputStream.bufferedReader().use { it.readText() }
    }
}
