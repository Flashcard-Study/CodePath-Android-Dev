package com.example.flashcardstudy.ui.newdeck

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.UUID

class NewDeckFragment : Fragment(R.layout.fragment_new_deck) {

    private val colors = listOf("#6C63FF","#FF6B6B","#FFD166","#4CAF50","#118AB2","#EF476F","#F77F00")
    private val icons = listOf("📚","🔬","🧮","🌍","💡","⚡","🎯","🏆","📝","🧠")
    private var selectedColor = "#6C63FF"
    private var selectedIcon = "📚"
    private val colorViews = mutableListOf<View>()
    private val iconViews = mutableListOf<MaterialCardView>()

    private val prefs by lazy {
        requireActivity().getSharedPreferences("new_deck_prefs", Context.MODE_PRIVATE)
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

        switchPublic.isChecked = prefs.getBoolean("is_public", false)
        tvLabel.text = if (switchPublic.isChecked) "Public deck" else "Private deck"
        tvSub.text = if (switchPublic.isChecked) "Anyone can find this deck" else "Only you can see this deck"

        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            tvLabel.text = if (isChecked) "Public deck" else "Private deck"
            tvSub.text = if (isChecked) "Anyone can find this deck" else "Only you can see this deck"
            prefs.edit().putBoolean("is_public", isChecked).apply()
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
                    Toast.makeText(requireContext(), "\"$name\" deck created!", Toast.LENGTH_SHORT).show()
                    (activity as? com.example.flashcardstudy.MainActivity)?.navigateToHome()
                } else {
                    Snackbar.make(view, "Failed to create deck", Snackbar.LENGTH_SHORT).show()
                }
            }
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
            card.strokeColor = if (isSelected) Color.parseColor("#6C63FF") else Color.parseColor("#DDDDDD")
            card.setCardBackgroundColor(if (isSelected) Color.parseColor("#EEF0FF") else Color.parseColor("#F2F2F7"))

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
        prefs.edit().putString("selected_color", selectedColor).apply()
        colorViews.forEachIndexed { i, v -> v.background = makeCircle(colors[i], i == index) }
    }

    private fun selectIcon(index: Int) {
        selectedIcon = icons[index]
        prefs.edit().putString("selected_icon", selectedIcon).apply()
        iconViews.forEachIndexed { i, card ->
            card.strokeColor = if (i == index) Color.parseColor("#6C63FF") else Color.parseColor("#DDDDDD")
            card.setCardBackgroundColor(if (i == index) Color.parseColor("#EEF0FF") else Color.parseColor("#F2F2F7"))
        }
    }

    private fun makeCircle(hex: String, selected: Boolean): android.graphics.drawable.Drawable {
        val color = Color.parseColor(hex)
        return if (selected) {
            val filled = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            val ring = GradientDrawable().apply { shape = GradientDrawable.OVAL; setStroke(4, color); setColor(Color.TRANSPARENT) }
            LayerDrawable(arrayOf(ring, filled)).apply { setLayerInset(1, 6, 6, 6, 6) }
        } else {
            GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        }
    }
}
