package com.example.flashcardstudy.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.Flashcard

class FlashcardDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_DECKS (
                $COLUMN_DECK_ID TEXT PRIMARY KEY,
                $COLUMN_DECK_NAME TEXT NOT NULL,
                $COLUMN_DECK_SUBTITLE TEXT,
                $COLUMN_DECK_CARD_COUNT INTEGER DEFAULT 0,
                $COLUMN_DECK_LAST_STUDIED INTEGER DEFAULT 0,
                $COLUMN_DECK_COLOR TEXT DEFAULT '#6C63FF',
                $COLUMN_DECK_ICON TEXT DEFAULT '',
                $COLUMN_DECK_IS_PUBLIC INTEGER DEFAULT 0
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_FLASHCARDS (
                $COLUMN_CARD_ID TEXT PRIMARY KEY,
                $COLUMN_CARD_DECK_ID TEXT NOT NULL,
                $COLUMN_CARD_QUESTION TEXT NOT NULL,
                $COLUMN_CARD_ANSWER TEXT NOT NULL,
                FOREIGN KEY($COLUMN_CARD_DECK_ID) REFERENCES $TABLE_DECKS($COLUMN_DECK_ID) ON DELETE CASCADE
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_STUDY_PROGRESS (
                $COLUMN_PROGRESS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PROGRESS_CARD_ID TEXT NOT NULL,
                $COLUMN_PROGRESS_DECK_ID TEXT NOT NULL,
                $COLUMN_PROGRESS_STATUS TEXT NOT NULL,
                $COLUMN_PROGRESS_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_PROGRESS_CARD_ID) REFERENCES $TABLE_FLASHCARDS($COLUMN_CARD_ID) ON DELETE CASCADE,
                FOREIGN KEY($COLUMN_PROGRESS_DECK_ID) REFERENCES $TABLE_DECKS($COLUMN_DECK_ID) ON DELETE CASCADE
            )
        """
        )

        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_DECKS ADD COLUMN $COLUMN_DECK_LAST_STUDIED INTEGER DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_DECKS ADD COLUMN $COLUMN_DECK_COLOR TEXT DEFAULT '#6C63FF'")
            db.execSQL("ALTER TABLE $TABLE_DECKS ADD COLUMN $COLUMN_DECK_ICON TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_DECKS ADD COLUMN $COLUMN_DECK_IS_PUBLIC INTEGER DEFAULT 0")
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        val biologyDeck = ContentValues().apply {
            put(COLUMN_DECK_ID, "deck_biology")
            put(COLUMN_DECK_NAME, "Biology")
            put(COLUMN_DECK_SUBTITLE, "Ch. 1")
            put(COLUMN_DECK_CARD_COUNT, 5)
        }
        db.insert(TABLE_DECKS, null, biologyDeck)

        val spanishDeck = ContentValues().apply {
            put(COLUMN_DECK_ID, "deck_spanish")
            put(COLUMN_DECK_NAME, "Spanish")
            put(COLUMN_DECK_SUBTITLE, "Basics")
            put(COLUMN_DECK_CARD_COUNT, 3)
        }
        db.insert(TABLE_DECKS, null, spanishDeck)

        val bioCards = listOf(
            Triple("fc_bio_1", "What is the powerhouse of the cell?", "Mitochondria"),
            Triple("fc_bio_2", "Which molecule carries genetic information?", "DNA"),
            Triple("fc_bio_3", "What is the main function of red blood cells?", "Carrying oxygen"),
            Triple(
                "fc_bio_4",
                "What is the term for keeping internal conditions stable?",
                "Homeostasis"
            ),
            Triple("fc_bio_5", "What is the largest organ in the human body?", "Skin")
        )

        bioCards.forEach { (id, question, answer) ->
            val card = ContentValues().apply {
                put(COLUMN_CARD_ID, id)
                put(COLUMN_CARD_DECK_ID, "deck_biology")
                put(COLUMN_CARD_QUESTION, question)
                put(COLUMN_CARD_ANSWER, answer)
            }
            db.insert(TABLE_FLASHCARDS, null, card)
        }

        val spanishCards = listOf(
            Triple("fc_es_1", "How do you say \"Hello\"?", "Hola"),
            Triple("fc_es_2", "How do you say \"Thank you\"?", "Gracias"),
            Triple("fc_es_3", "How do you say \"Goodbye\"?", "Adiós")
        )

        spanishCards.forEach { (id, question, answer) ->
            val card = ContentValues().apply {
                put(COLUMN_CARD_ID, id)
                put(COLUMN_CARD_DECK_ID, "deck_spanish")
                put(COLUMN_CARD_QUESTION, question)
                put(COLUMN_CARD_ANSWER, answer)
            }
            db.insert(TABLE_FLASHCARDS, null, card)
        }
    }

    fun getAllDecks(): List<Deck> {
        val decks = mutableListOf<Deck>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DECKS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DECK_LAST_STUDIED DESC, $COLUMN_DECK_NAME ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val deck = Deck(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_NAME)),
                    subtitle = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_SUBTITLE)) ?: "",
                    cardCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_DECK_CARD_COUNT)),
                    color = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_COLOR)) ?: "#6C63FF",
                    icon = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_ICON)) ?: "",
                    isPublic = it.getInt(it.getColumnIndexOrThrow(COLUMN_DECK_IS_PUBLIC)) != 0
                )
                decks.add(deck)
            }
        }
        return decks
    }

    fun insertDeck(deck: Deck): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DECK_ID, deck.id)
            put(COLUMN_DECK_NAME, deck.name)
            put(COLUMN_DECK_SUBTITLE, deck.subtitle)
            put(COLUMN_DECK_CARD_COUNT, deck.cardCount)
            put(COLUMN_DECK_COLOR, deck.color)
            put(COLUMN_DECK_ICON, deck.icon)
            put(COLUMN_DECK_IS_PUBLIC, if (deck.isPublic) 1 else 0)
        }
        return db.insert(TABLE_DECKS, null, values) != -1L
    }

    fun updateDeckLastStudied(deckId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DECK_LAST_STUDIED, System.currentTimeMillis())
        }
        db.update(TABLE_DECKS, values, "$COLUMN_DECK_ID = ?", arrayOf(deckId))
    }

    fun getMostRecentlyStudiedDeck(): Deck? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DECKS,
            null,
            "$COLUMN_DECK_LAST_STUDIED > 0",
            null,
            null,
            null,
            "$COLUMN_DECK_LAST_STUDIED DESC",
            "1"
        )

        cursor.use {
            if (it.moveToFirst()) {
                return Deck(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_NAME)),
                    subtitle = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_SUBTITLE)) ?: "",
                    cardCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_DECK_CARD_COUNT)),
                    color = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_COLOR)) ?: "#6C63FF",
                    icon = it.getString(it.getColumnIndexOrThrow(COLUMN_DECK_ICON)) ?: "",
                    isPublic = it.getInt(it.getColumnIndexOrThrow(COLUMN_DECK_IS_PUBLIC)) != 0
                )
            }
        }
        return null
    }

    fun insertFlashcard(flashcard: Flashcard): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CARD_ID, flashcard.id)
            put(COLUMN_CARD_DECK_ID, flashcard.deckId)
            put(COLUMN_CARD_QUESTION, flashcard.question)
            put(COLUMN_CARD_ANSWER, flashcard.answer)
        }
        val inserted = db.insert(TABLE_FLASHCARDS, null, values) != -1L
        if (inserted) {
            db.execSQL(
                "UPDATE $TABLE_DECKS SET $COLUMN_DECK_CARD_COUNT = $COLUMN_DECK_CARD_COUNT + 1 WHERE $COLUMN_DECK_ID = ?",
                arrayOf(flashcard.deckId)
            )
        }
        return inserted
    }

    fun importDeckWithCards(deck: Deck, cards: List<Flashcard>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val deckValues = ContentValues().apply {
                put(COLUMN_DECK_ID, deck.id)
                put(COLUMN_DECK_NAME, deck.name)
                put(COLUMN_DECK_SUBTITLE, deck.subtitle)
                put(COLUMN_DECK_CARD_COUNT, cards.size)
                put(COLUMN_DECK_COLOR, deck.color)
                put(COLUMN_DECK_ICON, deck.icon)
                put(COLUMN_DECK_IS_PUBLIC, if (deck.isPublic) 1 else 0)
            }

            val deckInserted = db.insert(TABLE_DECKS, null, deckValues) != -1L
            if (!deckInserted) {
                return false
            }

            cards.forEach { card ->
                val cardValues = ContentValues().apply {
                    put(COLUMN_CARD_ID, card.id)
                    put(COLUMN_CARD_DECK_ID, card.deckId)
                    put(COLUMN_CARD_QUESTION, card.question)
                    put(COLUMN_CARD_ANSWER, card.answer)
                }
                val cardInserted = db.insert(TABLE_FLASHCARDS, null, cardValues) != -1L
                if (!cardInserted) {
                    return false
                }
            }

            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun getFlashcardsForDeck(deckId: String): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FLASHCARDS,
            null,
            "$COLUMN_CARD_DECK_ID = ?",
            arrayOf(deckId),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val flashcard = Flashcard(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_CARD_ID)),
                    deckId = it.getString(it.getColumnIndexOrThrow(COLUMN_CARD_DECK_ID)),
                    question = it.getString(it.getColumnIndexOrThrow(COLUMN_CARD_QUESTION)),
                    answer = it.getString(it.getColumnIndexOrThrow(COLUMN_CARD_ANSWER))
                )
                flashcards.add(flashcard)
            }
        }
        return flashcards
    }

    fun recordStudyProgress(cardId: String, deckId: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROGRESS_CARD_ID, cardId)
            put(COLUMN_PROGRESS_DECK_ID, deckId)
            put(COLUMN_PROGRESS_STATUS, status)
            put(COLUMN_PROGRESS_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_STUDY_PROGRESS, null, values)
    }

    fun getStudyProgressForDeck(deckId: String): List<StudyProgress> {
        val progressList = mutableListOf<StudyProgress>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_STUDY_PROGRESS,
            null,
            "$COLUMN_PROGRESS_DECK_ID = ?",
            arrayOf(deckId),
            null,
            null,
            "$COLUMN_PROGRESS_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val progress = StudyProgress(
                    id = it.getInt(it.getColumnIndexOrThrow(COLUMN_PROGRESS_ID)),
                    cardId = it.getString(it.getColumnIndexOrThrow(COLUMN_PROGRESS_CARD_ID)),
                    deckId = it.getString(it.getColumnIndexOrThrow(COLUMN_PROGRESS_DECK_ID)),
                    status = it.getString(it.getColumnIndexOrThrow(COLUMN_PROGRESS_STATUS)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_PROGRESS_TIMESTAMP))
                )
                progressList.add(progress)
            }
        }
        return progressList
    }

    fun getStudyStatsForDeck(deckId: String): StudyStats {
        val db = readableDatabase

        val totalCards = getFlashcardsForDeck(deckId).size

        val query = """
            SELECT $COLUMN_PROGRESS_STATUS, COUNT(DISTINCT $COLUMN_PROGRESS_CARD_ID) as count
            FROM $TABLE_STUDY_PROGRESS
            WHERE $COLUMN_PROGRESS_DECK_ID = ?
            AND $COLUMN_PROGRESS_ID IN (
                SELECT MAX($COLUMN_PROGRESS_ID)
                FROM $TABLE_STUDY_PROGRESS
                WHERE $COLUMN_PROGRESS_DECK_ID = ?
                GROUP BY $COLUMN_PROGRESS_CARD_ID
            )
            GROUP BY $COLUMN_PROGRESS_STATUS
        """

        val cursor = db.rawQuery(query, arrayOf(deckId, deckId))

        var masteredCount = 0
        var learningCount = 0

        cursor.use {
            while (it.moveToNext()) {
                val status = it.getString(0)
                val count = it.getInt(1)
                when (status) {
                    "got_it" -> masteredCount = count
                    "still_learning" -> learningCount = count
                }
            }
        }

        return StudyStats(
            totalCards = totalCards,
            masteredCards = masteredCount,
            learningCards = learningCount
        )
    }

    companion object {
        private const val DATABASE_NAME = "flashcard_study.db"
        private const val DATABASE_VERSION = 3

        // Table names
        const val TABLE_DECKS = "decks"
        const val TABLE_FLASHCARDS = "flashcards"
        const val TABLE_STUDY_PROGRESS = "study_progress"

        // Deck columns
        const val COLUMN_DECK_ID = "id"
        const val COLUMN_DECK_NAME = "name"
        const val COLUMN_DECK_SUBTITLE = "subtitle"
        const val COLUMN_DECK_CARD_COUNT = "card_count"
        const val COLUMN_DECK_LAST_STUDIED = "last_studied"
        const val COLUMN_DECK_COLOR = "color"
        const val COLUMN_DECK_ICON = "icon"
        const val COLUMN_DECK_IS_PUBLIC = "is_public"

        // Flashcard columns
        const val COLUMN_CARD_ID = "id"
        const val COLUMN_CARD_DECK_ID = "deck_id"
        const val COLUMN_CARD_QUESTION = "question"
        const val COLUMN_CARD_ANSWER = "answer"

        // Study Progress columns
        const val COLUMN_PROGRESS_ID = "id"
        const val COLUMN_PROGRESS_CARD_ID = "card_id"
        const val COLUMN_PROGRESS_DECK_ID = "deck_id"
        const val COLUMN_PROGRESS_STATUS = "status"
        const val COLUMN_PROGRESS_TIMESTAMP = "timestamp"
    }
}

data class StudyProgress(
    val id: Int,
    val cardId: String,
    val deckId: String,
    val status: String,
    val timestamp: Long
)
