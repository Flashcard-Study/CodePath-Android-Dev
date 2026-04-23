package com.example.flashcardstudy.ui.newdeck

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.gemini.GeminiFlashcardGenerator
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

    private val colors = listOf("#6C63FF","#FF6B6B","#FFD166","#4CAF50","#118AB2","#EF476F","#F77F00")
    private val icons = listOf("📚","🔬","🧮","🌍","💡","⚡","🎯","🏆","📝","🧠")
    private var selectedColor = "#6C63FF"
    private var selectedIcon = "📚"
    private val colorViews = mutableListOf<View>()
    private val iconViews = mutableListOf<MaterialCardView>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorPicker(view)
        setupIconPicker(view)

        val switchPublic = view.findViewById<MaterialSwitch>(R.id.switchPublic)
        val tvLabel = view.findViewById<TextView>(R.id.tvVisibilityLabel)
        val tvSub = view.findViewById<TextView>(R.id.tvVisibilitySub)
        val tilDeckName = view.findViewById<TextInputLayout>(R.id.tilDeckName)
        val etDeckName = view.findViewById<TextInputEditText>(R.id.etDeckName)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreateDeck)
        val btnGenerate = view.findViewById<MaterialButton>(R.id.btnGenerate)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            tvLabel.text = if (isChecked) "Public deck" else "Private deck"
            tvSub.text = if (isChecked) "Anyone can find this deck" else "Only you can see this deck"
        }

        btnGenerate.setOnClickListener {
            showGenerateWithAiDialog(etDeckName, btnGenerate, progressBar, view)
        }

        btnCreate.setOnClickListener {
            val name = etDeckName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilDeckName.error = "Please enter a deck name"
            } else {
                tilDeckName.error = null
                Snackbar.make(view, "\"$name\" deck created!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGenerateWithAiDialog(
        etDeckName: TextInputEditText,
        btnGenerate: MaterialButton,
        progressBar: ProgressBar,
        view: View
    ) {
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
            isChecked = false
        }
        val useDeckContextLabel = TextView(requireContext()).apply {
            text = "Use deck context"
            textSize = 16f
        }
        val contextRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
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
            .setMessage("Generate a deck name and flashcards for a topic.")
            .setView(dialogContent)
            .setPositiveButton("Generate") { dialog, _ ->
                val topic = topicInput.text?.toString()?.trim().orEmpty()
                val count = countInput.text?.toString()?.trim()?.toIntOrNull() ?: 5

                if (topic.isBlank()) {
                    Toast.makeText(requireContext(), "Enter a topic to generate cards.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    Toast.makeText(requireContext(), "No network connection available.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                dialog.dismiss()
                btnGenerate.isEnabled = false
                progressBar.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val deckId = UUID.randomUUID().toString()
                        val result = GeminiFlashcardGenerator.generateDeckGeneration(
                            topic = topic,
                            deckId = deckId,
                            count = count.coerceIn(1, 25)
                        )
                        progressBar.visibility = View.GONE
                        btnGenerate.isEnabled = true
                        etDeckName.setText(result.deckName)
                        Snackbar.make(view, "Generated ${result.flashcards.size} flashcards for \"${result.deckName}\"!", Snackbar.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        progressBar.visibility = View.GONE
                        btnGenerate.isEnabled = true
                        Snackbar.make(view, e.message ?: "Failed to generate.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
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
            dot.background = makeCircle(hex, index == 0)
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
            card.strokeColor = if (index == 0) Color.parseColor("#6C63FF") else Color.parseColor("#DDDDDD")
            card.setCardBackgroundColor(if (index == 0) Color.parseColor("#EEF0FF") else Color.parseColor("#F2F2F7"))

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
        colorViews.forEachIndexed { i, v -> v.background = makeCircle(colors[i], i == index) }
    }

    private fun selectIcon(index: Int) {
        selectedIcon = icons[index]
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
