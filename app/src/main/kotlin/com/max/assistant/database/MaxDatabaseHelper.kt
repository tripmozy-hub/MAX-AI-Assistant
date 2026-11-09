package com.max.assistant.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class ReminderRecord(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val triggerTimeEpoch: Long,
    val repeatInterval: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val isActive: Boolean = true,
    val priority: String = "MEDIUM",     // HIGH, MEDIUM, LOW
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyNoteRecord(
    val id: Long = 0,
    val title: String,
    val subject: String,
    val content: String,
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyQuizRecord(
    val id: Long = 0,
    val topic: String,
    val examType: String,
    val questionsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

class MaxDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "MaxDatabaseHelper"
        private const val DATABASE_NAME = "max_assistant.db"
        private const val DATABASE_VERSION = 1

        // Table Reminders
        const val TABLE_REMINDERS = "reminders"
        const val COL_REM_ID = "id"
        const val COL_REM_TITLE = "title"
        const val COL_REM_NOTES = "notes"
        const val COL_REM_TRIGGER = "trigger_time_epoch"
        const val COL_REM_REPEAT = "repeat_interval"
        const val COL_REM_ACTIVE = "is_active"
        const val COL_REM_PRIORITY = "priority"
        const val COL_REM_CREATED = "created_at"

        // Table Study Notes
        const val TABLE_STUDY_NOTES = "study_notes"
        const val COL_NOTE_ID = "id"
        const val COL_NOTE_TITLE = "title"
        const val COL_NOTE_SUBJECT = "subject"
        const val COL_NOTE_CONTENT = "content"
        const val COL_NOTE_SUMMARY = "summary"
        const val COL_NOTE_CREATED = "created_at"

        // Table Study Quizzes
        const val TABLE_STUDY_QUIZZES = "study_quizzes"
        const val COL_QUIZ_ID = "id"
        const val COL_QUIZ_TOPIC = "topic"
        const val COL_QUIZ_EXAM = "exam_type"
        const val COL_QUIZ_QUESTIONS = "questions_json"
        const val COL_QUIZ_CREATED = "created_at"

        @Volatile
        private var instance: MaxDatabaseHelper? = null

        fun getInstance(context: Context): MaxDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: MaxDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_REMINDERS (
                $COL_REM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REM_TITLE TEXT NOT NULL,
                $COL_REM_NOTES TEXT,
                $COL_REM_TRIGGER INTEGER NOT NULL,
                $COL_REM_REPEAT TEXT NOT NULL DEFAULT 'NONE',
                $COL_REM_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $COL_REM_PRIORITY TEXT NOT NULL DEFAULT 'MEDIUM',
                $COL_REM_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_STUDY_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_TITLE TEXT NOT NULL,
                $COL_NOTE_SUBJECT TEXT NOT NULL,
                $COL_NOTE_CONTENT TEXT NOT NULL,
                $COL_NOTE_SUMMARY TEXT,
                $COL_NOTE_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_STUDY_QUIZZES (
                $COL_QUIZ_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_QUIZ_TOPIC TEXT NOT NULL,
                $COL_QUIZ_EXAM TEXT NOT NULL,
                $COL_QUIZ_QUESTIONS TEXT NOT NULL,
                $COL_QUIZ_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        Log.i(TAG, "MAX Assistant database initialized successfully with reminders and study tables.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration logic for future schema increments
    }

    // ==================== REMINDERS CRUD ====================

    fun insertReminder(reminder: ReminderRecord): Long {
        val values = ContentValues().apply {
            put(COL_REM_TITLE, reminder.title)
            put(COL_REM_NOTES, reminder.notes)
            put(COL_REM_TRIGGER, reminder.triggerTimeEpoch)
            put(COL_REM_REPEAT, reminder.repeatInterval)
            put(COL_REM_ACTIVE, if (reminder.isActive) 1 else 0)
            put(COL_REM_PRIORITY, reminder.priority)
            put(COL_REM_CREATED, reminder.createdAt)
        }
        return writableDatabase.insert(TABLE_REMINDERS, null, values)
    }

    fun getAllActiveReminders(): List<ReminderRecord> {
        val list = mutableListOf<ReminderRecord>()
        val cursor = readableDatabase.query(
            TABLE_REMINDERS,
            null,
            "$COL_REM_ACTIVE = 1",
            null,
            null,
            null,
            "$COL_REM_TRIGGER ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_REM_ID)
            val titleIdx = c.getColumnIndexOrThrow(COL_REM_TITLE)
            val notesIdx = c.getColumnIndexOrThrow(COL_REM_NOTES)
            val triggerIdx = c.getColumnIndexOrThrow(COL_REM_TRIGGER)
            val repeatIdx = c.getColumnIndexOrThrow(COL_REM_REPEAT)
            val activeIdx = c.getColumnIndexOrThrow(COL_REM_ACTIVE)
            val priorityIdx = c.getColumnIndexOrThrow(COL_REM_PRIORITY)
            val createdIdx = c.getColumnIndexOrThrow(COL_REM_CREATED)

            while (c.moveToNext()) {
                list.add(
                    ReminderRecord(
                        id = c.getLong(idIdx),
                        title = c.getString(titleIdx),
                        notes = c.getString(notesIdx) ?: "",
                        triggerTimeEpoch = c.getLong(triggerIdx),
                        repeatInterval = c.getString(repeatIdx) ?: "NONE",
                        isActive = c.getInt(activeIdx) == 1,
                        priority = c.getString(priorityIdx) ?: "MEDIUM",
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    fun getReminderById(id: Long): ReminderRecord? {
        val cursor = readableDatabase.query(
            TABLE_REMINDERS,
            null,
            "$COL_REM_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return ReminderRecord(
                    id = c.getLong(c.getColumnIndexOrThrow(COL_REM_ID)),
                    title = c.getString(c.getColumnIndexOrThrow(COL_REM_TITLE)),
                    notes = c.getString(c.getColumnIndexOrThrow(COL_REM_NOTES)) ?: "",
                    triggerTimeEpoch = c.getLong(c.getColumnIndexOrThrow(COL_REM_TRIGGER)),
                    repeatInterval = c.getString(c.getColumnIndexOrThrow(COL_REM_REPEAT)) ?: "NONE",
                    isActive = c.getInt(c.getColumnIndexOrThrow(COL_REM_ACTIVE)) == 1,
                    priority = c.getString(c.getColumnIndexOrThrow(COL_REM_PRIORITY)) ?: "MEDIUM",
                    createdAt = c.getLong(c.getColumnIndexOrThrow(COL_REM_CREATED))
                )
            }
        }
        return null
    }

    fun updateReminderTrigger(id: Long, newTriggerTimeEpoch: Long): Boolean {
        val values = ContentValues().apply {
            put(COL_REM_TRIGGER, newTriggerTimeEpoch)
        }
        return writableDatabase.update(TABLE_REMINDERS, values, "$COL_REM_ID = ?", arrayOf(id.toString())) > 0
    }

    fun setReminderActive(id: Long, active: Boolean): Boolean {
        val values = ContentValues().apply {
            put(COL_REM_ACTIVE, if (active) 1 else 0)
        }
        return writableDatabase.update(TABLE_REMINDERS, values, "$COL_REM_ID = ?", arrayOf(id.toString())) > 0
    }

    fun deleteReminder(id: Long): Boolean {
        return writableDatabase.delete(TABLE_REMINDERS, "$COL_REM_ID = ?", arrayOf(id.toString())) > 0
    }

    fun deleteRemindersByTitle(titlePattern: String): Int {
        return writableDatabase.delete(TABLE_REMINDERS, "$COL_REM_TITLE LIKE ?", arrayOf("%$titlePattern%"))
    }

    // ==================== STUDY NOTES CRUD ====================

    fun insertStudyNote(note: StudyNoteRecord): Long {
        val values = ContentValues().apply {
            put(COL_NOTE_TITLE, note.title)
            put(COL_NOTE_SUBJECT, note.subject)
            put(COL_NOTE_CONTENT, note.content)
            put(COL_NOTE_SUMMARY, note.summary)
            put(COL_NOTE_CREATED, note.createdAt)
        }
        return writableDatabase.insert(TABLE_STUDY_NOTES, null, values)
    }

    fun getAllStudyNotes(): List<StudyNoteRecord> {
        val list = mutableListOf<StudyNoteRecord>()
        val cursor = readableDatabase.query(
            TABLE_STUDY_NOTES,
            null,
            null,
            null,
            null,
            null,
            "$COL_NOTE_CREATED DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_NOTE_ID)
            val titleIdx = c.getColumnIndexOrThrow(COL_NOTE_TITLE)
            val subjIdx = c.getColumnIndexOrThrow(COL_NOTE_SUBJECT)
            val contentIdx = c.getColumnIndexOrThrow(COL_NOTE_CONTENT)
            val summaryIdx = c.getColumnIndexOrThrow(COL_NOTE_SUMMARY)
            val createdIdx = c.getColumnIndexOrThrow(COL_NOTE_CREATED)

            while (c.moveToNext()) {
                list.add(
                    StudyNoteRecord(
                        id = c.getLong(idIdx),
                        title = c.getString(titleIdx),
                        subject = c.getString(subjIdx),
                        content = c.getString(contentIdx),
                        summary = c.getString(summaryIdx) ?: "",
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    fun searchStudyNotes(query: String): List<StudyNoteRecord> {
        val list = mutableListOf<StudyNoteRecord>()
        val pattern = "%$query%"
        val cursor = readableDatabase.query(
            TABLE_STUDY_NOTES,
            null,
            "$COL_NOTE_TITLE LIKE ? OR $COL_NOTE_SUBJECT LIKE ? OR $COL_NOTE_CONTENT LIKE ?",
            arrayOf(pattern, pattern, pattern),
            null,
            null,
            "$COL_NOTE_CREATED DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_NOTE_ID)
            val titleIdx = c.getColumnIndexOrThrow(COL_NOTE_TITLE)
            val subjIdx = c.getColumnIndexOrThrow(COL_NOTE_SUBJECT)
            val contentIdx = c.getColumnIndexOrThrow(COL_NOTE_CONTENT)
            val summaryIdx = c.getColumnIndexOrThrow(COL_NOTE_SUMMARY)
            val createdIdx = c.getColumnIndexOrThrow(COL_NOTE_CREATED)

            while (c.moveToNext()) {
                list.add(
                    StudyNoteRecord(
                        id = c.getLong(idIdx),
                        title = c.getString(titleIdx),
                        subject = c.getString(subjIdx),
                        content = c.getString(contentIdx),
                        summary = c.getString(summaryIdx) ?: "",
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    // ==================== STUDY QUIZZES CRUD ====================

    fun insertStudyQuiz(quiz: StudyQuizRecord): Long {
        val values = ContentValues().apply {
            put(COL_QUIZ_TOPIC, quiz.topic)
            put(COL_QUIZ_EXAM, quiz.examType)
            put(COL_QUIZ_QUESTIONS, quiz.questionsJson)
            put(COL_QUIZ_CREATED, quiz.createdAt)
        }
        return writableDatabase.insert(TABLE_STUDY_QUIZZES, null, values)
    }

    fun getLatestQuizForTopic(topic: String): StudyQuizRecord? {
        val cursor = readableDatabase.query(
            TABLE_STUDY_QUIZZES,
            null,
            "$COL_QUIZ_TOPIC LIKE ?",
            arrayOf("%$topic%"),
            null,
            null,
            "$COL_QUIZ_CREATED DESC",
            "1"
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return StudyQuizRecord(
                    id = c.getLong(c.getColumnIndexOrThrow(COL_QUIZ_ID)),
                    topic = c.getString(c.getColumnIndexOrThrow(COL_QUIZ_TOPIC)),
                    examType = c.getString(c.getColumnIndexOrThrow(COL_QUIZ_EXAM)),
                    questionsJson = c.getString(c.getColumnIndexOrThrow(COL_QUIZ_QUESTIONS)),
                    createdAt = c.getLong(c.getColumnIndexOrThrow(COL_QUIZ_CREATED))
                )
            }
        }
        return null
    }
}
