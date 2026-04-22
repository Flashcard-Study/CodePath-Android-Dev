package com.example.flashcardstudy.data.transfer

import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard

data class DeckTransferPayload(
    val deck: Deck,
    val cards: List<Flashcard>
)
