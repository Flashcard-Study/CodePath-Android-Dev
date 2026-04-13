package com.example.flashcardstudy.ui.study

import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
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
    var index = 0
    var showAnswer = false
    lateinit var listener: OrientationEventListener

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
        outState.putBoolean("showAnswer", showAnswer)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)
        savedInstanceState?.let {
            index = it.getInt("index", 0)
            showAnswer = it.getBoolean("showAnswer", false)
        }
        val card = view.findViewById<MaterialCardView>(R.id.studyCard)
        val cardTV = card.findViewById<TextView>(R.id.cardContent)
        listener = object : OrientationEventListener(requireContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                when {
                    orientation <= 45 || orientation >= 315 -> {
                        showAnswer = false
                        cardTV.text = flashcards[index].question
                        Log.d("ORIENTATION", "vertical")
                    }
                    orientation in 46..134 -> {
                        showAnswer = true
                        cardTV.text = flashcards[index].answer
                        Log.d("ORIENTATION", "horizontal")
                    }
                    orientation in 135..224 -> {
                        showAnswer = false
                        cardTV.text = flashcards[index].question
                        Log.d("ORIENTATION", "vertical")
                    }
                    orientation in 225..314 -> {
                        showAnswer = true
                        cardTV.text = flashcards[index].answer
                        Log.d("ORIENTATION", "horizontal")
                    }
                }
            }
        }
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
            cardTV.text = if (showAnswer) flashcards[index].answer else flashcards[index].question
            showAnswer = false
        }
        val stillLearning = view.findViewById<Button>(R.id.stillLearning)
        stillLearning.setOnClickListener {
            index = (index + 1) % flashcards.size
            cardTV.text = if (showAnswer) flashcards[index].answer else flashcards[index].question
            showAnswer = false
        }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
    }

    override fun onPause() {
        super.onPause()
        listener.disable()
    }
}
