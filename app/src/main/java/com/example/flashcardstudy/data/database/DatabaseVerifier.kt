package com.example.flashcardstudy.data.database

import android.content.Context
import android.util.Log

object DatabaseVerifier {
    private const val TAG = "DatabaseVerifier"

    fun verifyStudyProgress(context: Context) {
        val dbHelper = FlashcardDatabaseHelper(context)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM ${FlashcardDatabaseHelper.TABLE_STUDY_PROGRESS} ORDER BY ${FlashcardDatabaseHelper.COLUMN_PROGRESS_TIMESTAMP} DESC LIMIT 10",
            null
        )

        Log.d(TAG, "=== Study Progress Records ===")
        Log.d(TAG, "Total records: ${cursor.count}")

        cursor.use {
            while (it.moveToNext()) {
                val cardId =
                    it.getString(it.getColumnIndexOrThrow(FlashcardDatabaseHelper.COLUMN_PROGRESS_CARD_ID))
                val deckId =
                    it.getString(it.getColumnIndexOrThrow(FlashcardDatabaseHelper.COLUMN_PROGRESS_DECK_ID))
                val status =
                    it.getString(it.getColumnIndexOrThrow(FlashcardDatabaseHelper.COLUMN_PROGRESS_STATUS))
                val timestamp =
                    it.getLong(it.getColumnIndexOrThrow(FlashcardDatabaseHelper.COLUMN_PROGRESS_TIMESTAMP))

                Log.d(
                    TAG,
                    "Record: cardId=$cardId, deckId=$deckId, status=$status, timestamp=$timestamp"
                )
            }
        }
        Log.d(TAG, "=== End of Records ===")
    }
}
