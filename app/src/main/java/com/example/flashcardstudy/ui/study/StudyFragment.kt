package com.example.flashcardstudy.ui.study

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

class StudyFragment : Fragment() {
    private val viewModel: StudyViewModel by viewModels()
    private var flashcards = listOf<Flashcard>()
    private var currentIndex = 0
    private var showAnswer = false

    private lateinit var card: MaterialCardView
    private lateinit var cardTV: TextView
    private lateinit var gotItButton: Button
    private lateinit var stillLearningButton: Button
    
    private lateinit var totalCardsCount: TextView
    private lateinit var masteredCount: TextView
    private lateinit var learningCount: TextView

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
            flashcards = cards
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

    fun loadDeck(deckId: String) {
        viewModel.loadDeckFlashcards(deckId)
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"

        fun newInstance(deckId: String): StudyFragment {
            return StudyFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DECK_ID, deckId)
                }
            }
        }
    }
}
