package com.example.flashcardstudy.ui.newdeck

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
        val etAiTopic = view.findViewById<TextInputEditText>(R.id.etAiTopic)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            tvLabel.text = if (isChecked) "Public deck" else "Private deck"
            tvSub.text = if (isChecked) "Anyone can find this deck" else "Only you can see this deck"
        }

        btnGenerate.setOnClickListener {
            val topic = etAiTopic.text?.toString()?.trim()
            if (topic.isNullOrEmpty()) {
                Snackbar.make(view, "Please enter a topic to generate", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnGenerate.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true

                val success = (0..1).random() == 1
                if (success) {
                    etDeckName.setText(topic)
                    Snackbar.make(view, "Deck generated for \"$topic\"!", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(view, "Failed to generate. Please try again.", Snackbar.LENGTH_SHORT).show()
                }
            }, 2000)
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
