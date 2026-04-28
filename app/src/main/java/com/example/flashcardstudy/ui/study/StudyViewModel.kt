package com.example.flashcardstudy.ui.study

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.data.database.FlashcardDatabaseHelper
import com.example.flashcardstudy.data.database.StudyStats
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

class StudyViewModel : ViewModel() {
    private val repository = RepositoryProvider.flashcardRepository

    private val _flashcards = MutableLiveData<List<Flashcard>>(emptyList())
    val flashcards: LiveData<List<Flashcard>> = _flashcards

    private val _studyStats = MutableLiveData<StudyStats>(StudyStats())
    val studyStats: LiveData<StudyStats> = _studyStats

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentDeckId: String? = null

    fun loadDeckFlashcards(deckId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                currentDeckId = deckId
                Log.d("StudyViewModel", "Loading cards for deck: $deckId")
                val cards = repository.getFlashcardsForDeck(deckId)
                Log.d("StudyViewModel", "Loaded ${cards.size} cards")
                _flashcards.postValue(cards)
                loadStats(deckId)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun recordGrade(cardId: String, status: String) {
        viewModelScope.launch {
            currentDeckId?.let { deckId ->
                repository.recordStudyProgress(cardId, deckId, status)
                repository.updateDeckLastStudied(deckId)
                loadStats(deckId)
            }
        }
    }

    fun recordSession(cardId: String) {
        viewModelScope.launch {
            currentDeckId?.let { deckId ->
                repository.recordStudyProgress(
                    cardId,
                    deckId,
                    FlashcardDatabaseHelper.STATUS_SESSION_STARTED
                )
                repository.updateDeckLastStudied(deckId)
            }
        }
    }

    private fun loadStats(deckId: String) {
        viewModelScope.launch {
            val stats = repository.getStudyStatsForDeck(deckId)
            _studyStats.postValue(stats)
        }
    }

    fun shuffleCards() {
        _flashcards.value?.let { cards ->
            _flashcards.postValue(cards.shuffled())
        }
    }

}
