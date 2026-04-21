package com.example.flashcardstudy.ui.study

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.database.DatabaseVerifier
import com.google.android.material.card.MaterialCardView
import kotlin.math.sqrt

class StudyFragment : Fragment(), SensorEventListener {
    private val viewModel: StudyViewModel by viewModels()
    private var flashcards = mutableListOf<Flashcard>()
    private var currentIndex = 0
    private var showAnswer = false

    private lateinit var card: MaterialCardView
    private lateinit var cardTV: TextView
    private lateinit var gotItButton: Button
    private lateinit var stillLearningButton: Button
    
    private lateinit var totalCardsCount: TextView
    private lateinit var masteredCount: TextView
    private lateinit var learningCount: TextView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)
        
        card = view.findViewById(R.id.studyCard)
        cardTV = card.findViewById(R.id.cardContent)
        gotItButton = view.findViewById(R.id.gotIt)
        stillLearningButton = view.findViewById(R.id.stillLearning)
        
        totalCardsCount = view.findViewById(R.id.totalCardsCount)
        masteredCount = view.findViewById(R.id.masteredCount)
        learningCount = view.findViewById(R.id.learningCount)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setupObservers()
        setupClickListeners()
        
        arguments?.getString(ARG_DECK_ID)?.let { deckId ->
            Log.d("StudyFragment", "Loading deck from arguments: $deckId")
            viewModel.loadDeckFlashcards(deckId)
        } ?: run {
            Log.d("StudyFragment", "No deck ID in arguments")
        }

        return view
    }

    private fun setupObservers() {
        viewModel.flashcards.observe(viewLifecycleOwner) { cards ->
            flashcards = cards.toMutableList()
            if (cards.isNotEmpty()) {
                currentIndex = 0
                displayCard()
            } else {
                cardTV.text = "No cards available"
            }
        }
        
        viewModel.studyStats.observe(viewLifecycleOwner) { stats ->
            totalCardsCount.text = stats.totalCards.toString()
            masteredCount.text = stats.masteredCards.toString()
            learningCount.text = stats.learningCards.toString()
        }
    }

    private fun setupClickListeners() {
        card.setOnClickListener {
            if (flashcards.isEmpty()) return@setOnClickListener
            
            if (showAnswer) {
                cardTV.text = flashcards[currentIndex].question
            } else {
                cardTV.text = flashcards[currentIndex].answer
            }
            showAnswer = !showAnswer
        }

        gotItButton.setOnClickListener {
            if (flashcards.isEmpty()) return@setOnClickListener
            
            val currentCard = flashcards[currentIndex]
            Log.d("StudyFragment", "Got It clicked for card: ${currentCard.id}")
            
            viewModel.recordGrade(currentCard.id, "got_it")
            DatabaseVerifier.verifyStudyProgress(requireContext())
            
            currentIndex = (currentIndex + 1) % flashcards.size
            showAnswer = false
            displayCard()
        }

        stillLearningButton.setOnClickListener {
            if (flashcards.isEmpty()) return@setOnClickListener
            
            val currentCard = flashcards[currentIndex]
            Log.d("StudyFragment", "Still Learning clicked for card: ${currentCard.id}")
            
            viewModel.recordGrade(currentCard.id, "still_learning")
            DatabaseVerifier.verifyStudyProgress(requireContext())
            
            currentIndex = (currentIndex + 1) % flashcards.size
            showAnswer = false
            displayCard()
        }
    }

    private fun displayCard() {
        if (flashcards.isNotEmpty()) {
            cardTV.text = flashcards[currentIndex].question
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER || flashcards.isEmpty()) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = acceleration - SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()

        if (delta > SHAKE_THRESHOLD && now - lastShakeTime > SHAKE_COOLDOWN_MS) {
            lastShakeTime = now
            flashcards.shuffle()
            currentIndex = 0
            showAnswer = false
            displayCard()
            Log.d("StudyFragment", "Deck shuffled by shake")
        }
    }

    fun loadDeck(deckId: String) {
        viewModel.loadDeckFlashcards(deckId)
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val SHAKE_THRESHOLD = 8f
        private const val SHAKE_COOLDOWN_MS = 1000L

        fun newInstance(deckId: String): StudyFragment {
            return StudyFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DECK_ID, deckId)
                }
            }
        }
    }
}
