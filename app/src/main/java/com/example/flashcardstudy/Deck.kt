package com.example.flashcardstudy

data class Deck(
    val id: String,
    val name: String,
    val cardCount: Int,
    val subtitle: String = ""
)
