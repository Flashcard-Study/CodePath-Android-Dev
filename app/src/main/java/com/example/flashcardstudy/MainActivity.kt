package com.example.flashcardstudy

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val cards = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val frontInput = findViewById<EditText>(R.id.editTextFront)
        val backInput = findViewById<EditText>(R.id.editTextBack)
        val saveButton = findViewById<Button>(R.id.buttonSave)
        val deleteButton = findViewById<Button>(R.id.buttonDelete)

        saveButton.setOnClickListener {
            val frontText = frontInput.text.toString().trim()
            val backText = backInput.text.toString().trim()

            if (frontText.isEmpty() || backText.isEmpty()) {
                Toast.makeText(this, "Enter both sides of the card.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cards.add(Pair(frontText, backText))

            frontInput.text.clear()
            backInput.text.clear()

            Toast.makeText(this, "Card saved. Total cards: ${cards.size}", Toast.LENGTH_SHORT).show()
        }

        deleteButton.setOnClickListener {
            if (cards.isNotEmpty()) {
                cards.removeAt(cards.lastIndex)
                frontInput.text.clear()
                backInput.text.clear()
                Toast.makeText(this, "Last card deleted. Total cards: ${cards.size}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No cards to delete.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}