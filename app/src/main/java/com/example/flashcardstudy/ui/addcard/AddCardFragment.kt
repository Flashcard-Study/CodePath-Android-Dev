package com.example.flashcardstudy.ui.addcard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
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
                Toast.makeText(requireContext(), "Enter both sides of the card.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentDeckId == null) {
                Toast.makeText(requireContext(), "No deck selected. Open a deck first.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Card saved to \"$currentDeckName\"!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to save card.", Toast.LENGTH_SHORT).show()
                }
            }
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

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val ARG_DECK_NAME = "deck_name"
        private const val ARG_DECK_COLOR = "deck_color"

        fun newInstance(deckId: String, deckName: String, deckColor: String = "#6C63FF"): AddCardFragment {
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
