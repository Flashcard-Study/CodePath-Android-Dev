package com.example.flashcardstudy.ui.study

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

class StudyViewModel : ViewModel() {
    private val repository = RepositoryProvider.flashcardRepository

    private val _flashcards = MutableLiveData<List<Flashcard>>(emptyList())
    val flashcards: LiveData<List<Flashcard>> = _flashcards

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadDeckFlashcards()
    }

    fun loadDeckFlashcards(deckId: String? = null) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val resolvedDeckId = deckId ?: repository.getDecks().firstOrNull()?.id
                val cards = if (resolvedDeckId != null) {
                    repository.getFlashcardsForDeck(resolvedDeckId)
                } else {
                    emptyList()
                }
                _flashcards.postValue(cards)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
