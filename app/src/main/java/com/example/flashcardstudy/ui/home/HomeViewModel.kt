package com.example.flashcardstudy.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = RepositoryProvider.flashcardRepository

    private val _decks = MutableLiveData<List<Deck>>(emptyList())
    val decks: LiveData<List<Deck>> = _decks

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshDecks()
    }

    fun refreshDecks() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                _decks.postValue(repository.getDecks())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
