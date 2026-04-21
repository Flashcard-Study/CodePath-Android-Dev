package com.example.flashcardstudy

data class Deck(
    val id: String,
    val name: String,
    val cardCount: Int,
    val subtitle: String = "",
    val color: String = "#6C63FF",
    val icon: String = "",
    val isPublic: Boolean = false
)
