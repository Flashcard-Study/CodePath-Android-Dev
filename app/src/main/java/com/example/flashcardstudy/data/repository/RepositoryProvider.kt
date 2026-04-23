package com.example.flashcardstudy.data.repository

import android.content.Context

object RepositoryProvider {
    private var repository: FlashcardRepository? = null

    fun initialize(context: Context) {
        if (repository == null) {
            repository = SqliteFlashcardRepository(context.applicationContext)
        }
    }

    val flashcardRepository: FlashcardRepository
        get() = repository
            ?: throw IllegalStateException("RepositoryProvider must be initialized first")
}
