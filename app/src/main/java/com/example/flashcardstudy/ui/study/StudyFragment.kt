package com.example.flashcardstudy.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.google.android.material.card.MaterialCardView

class StudyFragment : Fragment() {
    val flashcards = mutableListOf<Flashcard>(
        Flashcard("What is the powerhouse of the cell?", "Mitochondria"),
        Flashcard("Which molecule carries genetic information?", "DNA"),
        Flashcard("What is the main function of red blood cells?", "Carrying oxygen"),
        Flashcard("What is the term for keeping internal conditions stable?", "Homeostasis"),
        Flashcard("What is the largest organ in the human body?", "Skin")
    )
    var showAnswer = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)
        val card = view.findViewById<MaterialCardView>(R.id.studyCard)
        val cardTV = card.findViewById<TextView>(R.id.cardContent)
        var index = 0
        cardTV.text = flashcards[index].question
        card.setOnClickListener {
            if (showAnswer) {
                cardTV.text = flashcards[index].question
            } else {
                cardTV.text = flashcards[index].answer
            }
            showAnswer = !showAnswer
        }
        val gotIt = view.findViewById<Button>(R.id.gotIt)
        gotIt.setOnClickListener {
            index = (index + 1) % flashcards.size
            cardTV.text = flashcards[index].question
            showAnswer = false
        }
        val stillLearning = view.findViewById<Button>(R.id.stillLearning)
        stillLearning.setOnClickListener {
            index = (index + 1) % flashcards.size
            cardTV.text = flashcards[index].question
            showAnswer = false
        }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

