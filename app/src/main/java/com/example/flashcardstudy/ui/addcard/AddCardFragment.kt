package com.example.flashcardstudy.ui.addcard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.R

class AddCardFragment : Fragment(R.layout.fragment_add_card) {

    private val cards = mutableListOf<Pair<String, String>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val frontInput = view.findViewById<EditText>(R.id.editTextFront)
        val backInput = view.findViewById<EditText>(R.id.editTextBack)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val addAnotherButton = view.findViewById<Button>(R.id.buttonAddAnother)
        val deleteButton = view.findViewById<Button>(R.id.buttonDelete)

        saveButton.setOnClickListener {
            val frontText = frontInput.text.toString().trim()
            val backText = backInput.text.toString().trim()

            if (frontText.isEmpty() || backText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter both sides of the card.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            cards.add(Pair(frontText, backText))
            frontInput.text.clear()
            backInput.text.clear()

            Toast.makeText(
                requireContext(),
                "Card saved. Total cards: ${cards.size}",
                Toast.LENGTH_SHORT
            ).show()
        }

        addAnotherButton.setOnClickListener {
            frontInput.text.clear()
            backInput.text.clear()
            Toast.makeText(
                requireContext(),
                "Add another card. Total saved: ${cards.size}",
                Toast.LENGTH_SHORT
            ).show()
        }

        deleteButton.setOnClickListener {
            if (cards.isNotEmpty()) {
                cards.removeAt(cards.lastIndex)
                frontInput.text.clear()
                backInput.text.clear()
                Toast.makeText(
                    requireContext(),
                    "Last card deleted. Total cards: ${cards.size}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "No cards to delete.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
