package com.example.flashcardstudy.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = RepositoryProvider.flashcardRepository

    private val _decks = MutableLiveData<List<Deck>>(emptyList())
    val decks: LiveData<List<Deck>> = _decks

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadDecks()
    }

    fun loadDecks() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val deckList = repository.getDecks()
                val uiDecks = deckList.map { dbDeck ->
                    val stats = repository.getStudyStatsForDeck(dbDeck.id)
                    val trackedCards = stats.masteredCards + stats.learningCards
                    val masteryPercent = if (trackedCards > 0) {
                        ((stats.masteredCards.toFloat() / trackedCards.toFloat()) * 100f).toInt()
                    } else {
                        null
                    }
                    Deck.fromDatabaseDeck(dbDeck, masteryPercent)
                }
                _decks.postValue(uiDecks)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
