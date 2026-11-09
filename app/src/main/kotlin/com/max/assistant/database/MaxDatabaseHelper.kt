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

data class UserMemoryRecord(
    val id: Long = 0,
    val memoryKey: String,
    val content: String,
    val category: String = "PREFERENCE", // PREFERENCE, FACT, WORKFLOW, IMPORTANT_INFO
    val tags: String = "",
    val expiryEpoch: Long = 0L,         // 0 = never expires
    val isUserApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectTaskRecord(
    val id: Long = 0,
    val projectName: String,
    val taskTitle: String,
    val status: String = "PENDING",     // PENDING, IN_PROGRESS, COMPLETED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ConversationRecord(
    val id: Long = 0,
    val sessionId: String,
    val role: String,                   // USER, MAX, SYSTEM
    val message: String,
    val language: String = "en",
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String = "",
    val isSensitive: Boolean = false
)

data class HealthIncidentRecord(
    val id: Long = 0,
    val component: String,
    val errorType: String,
    val severity: String = "MEDIUM",    // LOW, MEDIUM, HIGH, CRITICAL
    val message: String,
    val recoveryLevel: Int = 1,         // 1 = Automatic, 2 = Assisted, 3 = Developer
    val resolution: String = "RESOLVED", // RESOLVED, RETRYING, ACTION_REQUIRED, FAILED
    val timestamp: Long = System.currentTimeMillis()
)

data class CommandAnalyticsRecord(
    val id: Long = 0,
    val rawCommand: String,
    val normalizedIntent: String,
    val executionTimeMs: Long,
    val success: Boolean,
    val failureReason: String = "",
    val language: String = "en",
    val timestamp: Long = System.currentTimeMillis()
)

data class LearnedSynonymRecord(
    val id: Long = 0,
    val phrase: String,
    val canonicalIntent: String,
    val usageCount: Int = 1,
    val isConfirmed: Boolean = true,
    val lastUsedAt: Long = System.currentTimeMillis()
)

class MaxDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "MaxDatabaseHelper"
        private const val DATABASE_NAME = "max_assistant.db"
        private const val DATABASE_VERSION = 2

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

        // Table User Memories (Persistent Contextual Memory)
        const val TABLE_USER_MEMORIES = "user_memories"
        const val COL_MEM_ID = "id"
        const val COL_MEM_KEY = "memory_key"
        const val COL_MEM_CONTENT = "content"
        const val COL_MEM_CATEGORY = "category"
        const val COL_MEM_TAGS = "tags"
        const val COL_MEM_EXPIRY = "expiry_epoch"
        const val COL_MEM_APPROVED = "is_user_approved"
        const val COL_MEM_CREATED = "created_at"
        const val COL_MEM_UPDATED = "updated_at"

        // Table Project Tasks
        const val TABLE_PROJECT_TASKS = "project_tasks"
        const val COL_TASK_ID = "id"
        const val COL_TASK_PROJECT = "project_name"
        const val COL_TASK_TITLE = "task_title"
        const val COL_TASK_STATUS = "status"
        const val COL_TASK_NOTES = "notes"
        const val COL_TASK_CREATED = "created_at"
        const val COL_TASK_UPDATED = "updated_at"

        // Table Conversation Records
        const val TABLE_CONVERSATIONS = "conversation_records"
        const val COL_CONV_ID = "id"
        const val COL_CONV_SESSION = "session_id"
        const val COL_CONV_ROLE = "role"
        const val COL_CONV_MSG = "message"
        const val COL_CONV_LANG = "language"
        const val COL_CONV_TIME = "timestamp"
        const val COL_CONV_SUMMARY = "summary"
        const val COL_CONV_SENSITIVE = "is_sensitive"

        // Table Health Incidents (Self-Healing Monitor)
        const val TABLE_HEALTH_INCIDENTS = "health_incidents"
        const val COL_INC_ID = "id"
        const val COL_INC_COMPONENT = "component"
        const val COL_INC_TYPE = "error_type"
        const val COL_INC_SEVERITY = "severity"
        const val COL_INC_MSG = "message"
        const val COL_INC_LEVEL = "recovery_level"
        const val COL_INC_RESOLUTION = "resolution"
        const val COL_INC_TIME = "timestamp"

        // Table Command Analytics (Performance Learning)
        const val TABLE_COMMAND_ANALYTICS = "command_analytics"
        const val COL_ANA_ID = "id"
        const val COL_ANA_COMMAND = "raw_command"
        const val COL_ANA_INTENT = "normalized_intent"
        const val COL_ANA_EXEC_MS = "execution_time_ms"
        const val COL_ANA_SUCCESS = "success"
        const val COL_ANA_REASON = "failure_reason"
        const val COL_ANA_LANG = "language"
        const val COL_ANA_TIME = "timestamp"

        // Table Learned Synonyms
        const val TABLE_LEARNED_SYNONYMS = "learned_synonyms"
        const val COL_SYN_ID = "id"
        const val COL_SYN_PHRASE = "phrase"
        const val COL_SYN_INTENT = "canonical_intent"
        const val COL_SYN_COUNT = "usage_count"
        const val COL_SYN_CONFIRMED = "is_confirmed"
        const val COL_SYN_LAST_USED = "last_used_at"

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

        createVersion2Tables(db)
        Log.i(TAG, "MAX Assistant database initialized successfully with complete tables (v$DATABASE_VERSION).")
    }

    private fun createVersion2Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_MEMORIES (
                $COL_MEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MEM_KEY TEXT NOT NULL UNIQUE,
                $COL_MEM_CONTENT TEXT NOT NULL,
                $COL_MEM_CATEGORY TEXT NOT NULL DEFAULT 'PREFERENCE',
                $COL_MEM_TAGS TEXT,
                $COL_MEM_EXPIRY INTEGER NOT NULL DEFAULT 0,
                $COL_MEM_APPROVED INTEGER NOT NULL DEFAULT 1,
                $COL_MEM_CREATED INTEGER NOT NULL,
                $COL_MEM_UPDATED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PROJECT_TASKS (
                $COL_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TASK_PROJECT TEXT NOT NULL,
                $COL_TASK_TITLE TEXT NOT NULL,
                $COL_TASK_STATUS TEXT NOT NULL DEFAULT 'PENDING',
                $COL_TASK_NOTES TEXT,
                $COL_TASK_CREATED INTEGER NOT NULL,
                $COL_TASK_UPDATED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CONVERSATIONS (
                $COL_CONV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONV_SESSION TEXT NOT NULL,
                $COL_CONV_ROLE TEXT NOT NULL,
                $COL_CONV_MSG TEXT NOT NULL,
                $COL_CONV_LANG TEXT NOT NULL DEFAULT 'en',
                $COL_CONV_TIME INTEGER NOT NULL,
                $COL_CONV_SUMMARY TEXT,
                $COL_CONV_SENSITIVE INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_HEALTH_INCIDENTS (
                $COL_INC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INC_COMPONENT TEXT NOT NULL,
                $COL_INC_TYPE TEXT NOT NULL,
                $COL_INC_SEVERITY TEXT NOT NULL DEFAULT 'MEDIUM',
                $COL_INC_MSG TEXT NOT NULL,
                $COL_INC_LEVEL INTEGER NOT NULL DEFAULT 1,
                $COL_INC_RESOLUTION TEXT NOT NULL DEFAULT 'RESOLVED',
                $COL_INC_TIME INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COMMAND_ANALYTICS (
                $COL_ANA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ANA_COMMAND TEXT NOT NULL,
                $COL_ANA_INTENT TEXT NOT NULL,
                $COL_ANA_EXEC_MS INTEGER NOT NULL,
                $COL_ANA_SUCCESS INTEGER NOT NULL,
                $COL_ANA_REASON TEXT,
                $COL_ANA_LANG TEXT NOT NULL DEFAULT 'en',
                $COL_ANA_TIME INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_LEARNED_SYNONYMS (
                $COL_SYN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SYN_PHRASE TEXT NOT NULL UNIQUE,
                $COL_SYN_INTENT TEXT NOT NULL,
                $COL_SYN_COUNT INTEGER NOT NULL DEFAULT 1,
                $COL_SYN_CONFIRMED INTEGER NOT NULL DEFAULT 1,
                $COL_SYN_LAST_USED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createVersion2Tables(db)
            Log.i(TAG, "Upgraded MAX database from v$oldVersion to v$newVersion.")
        }
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
                        repeatInterval = c.getString(repeatIdx),
                        isActive = c.getInt(activeIdx) == 1,
                        priority = c.getString(priorityIdx),
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteReminder(id: Long): Boolean {
        return writableDatabase.delete(TABLE_REMINDERS, "$COL_REM_ID = ?", arrayOf(id.toString())) > 0
    }

    fun deleteRemindersByTitle(titlePattern: String): Int {
        return writableDatabase.delete(TABLE_REMINDERS, "$COL_REM_TITLE LIKE ?", arrayOf("%$titlePattern%"))
    }

    fun getActiveReminders(): List<ReminderRecord> = getAllActiveReminders()

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
                    repeatInterval = c.getString(c.getColumnIndexOrThrow(COL_REM_REPEAT)),
                    isActive = c.getInt(c.getColumnIndexOrThrow(COL_REM_ACTIVE)) == 1,
                    priority = c.getString(c.getColumnIndexOrThrow(COL_REM_PRIORITY)),
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
        val cv = ContentValues().apply {
            put(COL_REM_ACTIVE, if (active) 1 else 0)
        }
        return writableDatabase.update(TABLE_REMINDERS, cv, "$COL_REM_ID = ?", arrayOf(id.toString())) > 0
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
        val cursor = readableDatabase.query(
            TABLE_STUDY_NOTES,
            null,
            "$COL_NOTE_TITLE LIKE ? OR $COL_NOTE_SUBJECT LIKE ? OR $COL_NOTE_CONTENT LIKE ?",
            arrayOf("%$query%", "%$query%", "%$query%"),
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

    // ==================== USER MEMORIES CRUD (SECTION 2) ====================

    fun upsertUserMemory(memory: UserMemoryRecord): Long {
        val cv = ContentValues().apply {
            put(COL_MEM_KEY, memory.memoryKey.trim().lowercase())
            put(COL_MEM_CONTENT, memory.content)
            put(COL_MEM_CATEGORY, memory.category)
            put(COL_MEM_TAGS, memory.tags)
            put(COL_MEM_EXPIRY, memory.expiryEpoch)
            put(COL_MEM_APPROVED, if (memory.isUserApproved) 1 else 0)
            put(COL_MEM_CREATED, memory.createdAt)
            put(COL_MEM_UPDATED, System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_USER_MEMORIES,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun searchUserMemories(query: String): List<UserMemoryRecord> {
        cleanExpiredMemories()
        val list = mutableListOf<UserMemoryRecord>()
        val clean = query.trim().lowercase()
        val cursor = readableDatabase.query(
            TABLE_USER_MEMORIES,
            null,
            "$COL_MEM_KEY LIKE ? OR $COL_MEM_CONTENT LIKE ? OR $COL_MEM_TAGS LIKE ?",
            arrayOf("%$clean%", "%$clean%", "%$clean%"),
            null,
            null,
            "$COL_MEM_UPDATED DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COL_MEM_ID)
            val keyIdx = c.getColumnIndexOrThrow(COL_MEM_KEY)
            val contentIdx = c.getColumnIndexOrThrow(COL_MEM_CONTENT)
            val catIdx = c.getColumnIndexOrThrow(COL_MEM_CATEGORY)
            val tagsIdx = c.getColumnIndexOrThrow(COL_MEM_TAGS)
            val expIdx = c.getColumnIndexOrThrow(COL_MEM_EXPIRY)
            val appIdx = c.getColumnIndexOrThrow(COL_MEM_APPROVED)
            val crIdx = c.getColumnIndexOrThrow(COL_MEM_CREATED)
            val upIdx = c.getColumnIndexOrThrow(COL_MEM_UPDATED)

            while (c.moveToNext()) {
                list.add(
                    UserMemoryRecord(
                        id = c.getLong(idIdx),
                        memoryKey = c.getString(keyIdx),
                        content = c.getString(contentIdx),
                        category = c.getString(catIdx),
                        tags = c.getString(tagsIdx) ?: "",
                        expiryEpoch = c.getLong(expIdx),
                        isUserApproved = c.getInt(appIdx) == 1,
                        createdAt = c.getLong(crIdx),
                        updatedAt = c.getLong(upIdx)
                    )
                )
            }
        }
        return list
    }

    fun getAllUserMemories(): List<UserMemoryRecord> {
        cleanExpiredMemories()
        val list = mutableListOf<UserMemoryRecord>()
        val cursor = readableDatabase.query(
            TABLE_USER_MEMORIES,
            null,
            null,
            null,
            null,
            null,
            "$COL_MEM_UPDATED DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    UserMemoryRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_MEM_ID)),
                        memoryKey = c.getString(c.getColumnIndexOrThrow(COL_MEM_KEY)),
                        content = c.getString(c.getColumnIndexOrThrow(COL_MEM_CONTENT)),
                        category = c.getString(c.getColumnIndexOrThrow(COL_MEM_CATEGORY)),
                        tags = c.getString(c.getColumnIndexOrThrow(COL_MEM_TAGS)) ?: "",
                        expiryEpoch = c.getLong(c.getColumnIndexOrThrow(COL_MEM_EXPIRY)),
                        isUserApproved = c.getInt(c.getColumnIndexOrThrow(COL_MEM_APPROVED)) == 1,
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COL_MEM_CREATED)),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow(COL_MEM_UPDATED))
                    )
                )
            }
        }
        return list
    }

    fun deleteUserMemoryByKey(key: String): Boolean {
        return writableDatabase.delete(TABLE_USER_MEMORIES, "$COL_MEM_KEY = ?", arrayOf(key.trim().lowercase())) > 0
    }

    fun cleanExpiredMemories(): Int {
        val now = System.currentTimeMillis()
        return writableDatabase.delete(TABLE_USER_MEMORIES, "$COL_MEM_EXPIRY > 0 AND $COL_MEM_EXPIRY <= ?", arrayOf(now.toString()))
    }

    // ==================== PROJECT TASKS CRUD (SECTION 2) ====================

    fun insertProjectTask(task: ProjectTaskRecord): Long {
        val cv = ContentValues().apply {
            put(COL_TASK_PROJECT, task.projectName.trim())
            put(COL_TASK_TITLE, task.taskTitle.trim())
            put(COL_TASK_STATUS, task.status)
            put(COL_TASK_NOTES, task.notes)
            put(COL_TASK_CREATED, task.createdAt)
            put(COL_TASK_UPDATED, task.updatedAt)
        }
        return writableDatabase.insert(TABLE_PROJECT_TASKS, null, cv)
    }

    fun getTasksForProject(projectName: String): List<ProjectTaskRecord> {
        val list = mutableListOf<ProjectTaskRecord>()
        val cursor = readableDatabase.query(
            TABLE_PROJECT_TASKS,
            null,
            "$COL_TASK_PROJECT LIKE ?",
            arrayOf("%${projectName.trim()}%"),
            null,
            null,
            "$COL_TASK_UPDATED DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ProjectTaskRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_TASK_ID)),
                        projectName = c.getString(c.getColumnIndexOrThrow(COL_TASK_PROJECT)),
                        taskTitle = c.getString(c.getColumnIndexOrThrow(COL_TASK_TITLE)),
                        status = c.getString(c.getColumnIndexOrThrow(COL_TASK_STATUS)),
                        notes = c.getString(c.getColumnIndexOrThrow(COL_TASK_NOTES)) ?: "",
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COL_TASK_CREATED)),
                        updatedAt = c.getLong(c.getColumnIndexOrThrow(COL_TASK_UPDATED))
                    )
                )
            }
        }
        return list
    }

    fun getAllProjects(): List<String> {
        val list = mutableListOf<String>()
        val cursor = readableDatabase.rawQuery("SELECT DISTINCT $COL_TASK_PROJECT FROM $TABLE_PROJECT_TASKS ORDER BY $COL_TASK_PROJECT ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(c.getString(0))
            }
        }
        return list
    }

    fun updateProjectTaskStatus(id: Long, newStatus: String): Boolean {
        val cv = ContentValues().apply {
            put(COL_TASK_STATUS, newStatus)
            put(COL_TASK_UPDATED, System.currentTimeMillis())
        }
        return writableDatabase.update(TABLE_PROJECT_TASKS, cv, "$COL_TASK_ID = ?", arrayOf(id.toString())) > 0
    }

    // ==================== CONVERSATION RECORDS CRUD (SECTION 2 & 17) ====================

    fun insertConversationRecord(record: ConversationRecord): Long {
        val cv = ContentValues().apply {
            put(COL_CONV_SESSION, record.sessionId)
            put(COL_CONV_ROLE, record.role)
            put(COL_CONV_MSG, record.message)
            put(COL_CONV_LANG, record.language)
            put(COL_CONV_TIME, record.timestamp)
            put(COL_CONV_SUMMARY, record.summary)
            put(COL_CONV_SENSITIVE, if (record.isSensitive) 1 else 0)
        }
        return writableDatabase.insert(TABLE_CONVERSATIONS, null, cv)
    }

    fun searchConversationRecords(query: String, limit: Int = 20): List<ConversationRecord> {
        val list = mutableListOf<ConversationRecord>()
        val clean = query.trim()
        val cursor = readableDatabase.query(
            TABLE_CONVERSATIONS,
            null,
            "$COL_CONV_MSG LIKE ? AND $COL_CONV_SENSITIVE = 0",
            arrayOf("%$clean%"),
            null,
            null,
            "$COL_CONV_TIME DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ConversationRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_CONV_ID)),
                        sessionId = c.getString(c.getColumnIndexOrThrow(COL_CONV_SESSION)),
                        role = c.getString(c.getColumnIndexOrThrow(COL_CONV_ROLE)),
                        message = c.getString(c.getColumnIndexOrThrow(COL_CONV_MSG)),
                        language = c.getString(c.getColumnIndexOrThrow(COL_CONV_LANG)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_CONV_TIME)),
                        summary = c.getString(c.getColumnIndexOrThrow(COL_CONV_SUMMARY)) ?: "",
                        isSensitive = c.getInt(c.getColumnIndexOrThrow(COL_CONV_SENSITIVE)) == 1
                    )
                )
            }
        }
        return list
    }

    fun getConversationsForDateRange(startEpoch: Long, endEpoch: Long): List<ConversationRecord> {
        val list = mutableListOf<ConversationRecord>()
        val cursor = readableDatabase.query(
            TABLE_CONVERSATIONS,
            null,
            "$COL_CONV_TIME >= ? AND $COL_CONV_TIME <= ? AND $COL_CONV_SENSITIVE = 0",
            arrayOf(startEpoch.toString(), endEpoch.toString()),
            null,
            null,
            "$COL_CONV_TIME ASC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ConversationRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_CONV_ID)),
                        sessionId = c.getString(c.getColumnIndexOrThrow(COL_CONV_SESSION)),
                        role = c.getString(c.getColumnIndexOrThrow(COL_CONV_ROLE)),
                        message = c.getString(c.getColumnIndexOrThrow(COL_CONV_MSG)),
                        language = c.getString(c.getColumnIndexOrThrow(COL_CONV_LANG)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_CONV_TIME)),
                        summary = c.getString(c.getColumnIndexOrThrow(COL_CONV_SUMMARY)) ?: "",
                        isSensitive = c.getInt(c.getColumnIndexOrThrow(COL_CONV_SENSITIVE)) == 1
                    )
                )
            }
        }
        return list
    }

    // ==================== HEALTH INCIDENTS CRUD (SECTION 4 SELF-HEALING) ====================

    fun insertHealthIncident(incident: HealthIncidentRecord): Long {
        val cv = ContentValues().apply {
            put(COL_INC_COMPONENT, incident.component)
            put(COL_INC_TYPE, incident.errorType)
            put(COL_INC_SEVERITY, incident.severity)
            put(COL_INC_MSG, incident.message)
            put(COL_INC_LEVEL, incident.recoveryLevel)
            put(COL_INC_RESOLUTION, incident.resolution)
            put(COL_INC_TIME, incident.timestamp)
        }
        return writableDatabase.insert(TABLE_HEALTH_INCIDENTS, null, cv)
    }

    fun getRecentIncidents(limit: Int = 30): List<HealthIncidentRecord> {
        val list = mutableListOf<HealthIncidentRecord>()
        val cursor = readableDatabase.query(
            TABLE_HEALTH_INCIDENTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_INC_TIME DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    HealthIncidentRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_INC_ID)),
                        component = c.getString(c.getColumnIndexOrThrow(COL_INC_COMPONENT)),
                        errorType = c.getString(c.getColumnIndexOrThrow(COL_INC_TYPE)),
                        severity = c.getString(c.getColumnIndexOrThrow(COL_INC_SEVERITY)),
                        message = c.getString(c.getColumnIndexOrThrow(COL_INC_MSG)),
                        recoveryLevel = c.getInt(c.getColumnIndexOrThrow(COL_INC_LEVEL)),
                        resolution = c.getString(c.getColumnIndexOrThrow(COL_INC_RESOLUTION)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COL_INC_TIME))
                    )
                )
            }
        }
        return list
    }

    // ==================== COMMAND ANALYTICS & SYNONYMS CRUD (SECTION 15) ====================

    fun insertCommandAnalytics(record: CommandAnalyticsRecord): Long {
        val cv = ContentValues().apply {
            put(COL_ANA_COMMAND, record.rawCommand)
            put(COL_ANA_INTENT, record.normalizedIntent)
            put(COL_ANA_EXEC_MS, record.executionTimeMs)
            put(COL_ANA_SUCCESS, if (record.success) 1 else 0)
            put(COL_ANA_REASON, record.failureReason)
            put(COL_ANA_LANG, record.language)
            put(COL_ANA_TIME, record.timestamp)
        }
        return writableDatabase.insert(TABLE_COMMAND_ANALYTICS, null, cv)
    }

    fun getOverallSuccessRate(): Double {
        val cursor = readableDatabase.rawQuery("SELECT AVG($COL_ANA_SUCCESS) FROM $TABLE_COMMAND_ANALYTICS", null)
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getDouble(0) * 100.0
            }
        }
        return 100.0
    }

    fun recordOrIncrementSynonym(phrase: String, canonicalIntent: String) {
        val clean = phrase.trim().lowercase()
        writableDatabase.execSQL(
            """
            INSERT INTO $TABLE_LEARNED_SYNONYMS ($COL_SYN_PHRASE, $COL_SYN_INTENT, $COL_SYN_COUNT, $COL_SYN_CONFIRMED, $COL_SYN_LAST_USED)
            VALUES (?, ?, 1, 1, ?)
            ON CONFLICT($COL_SYN_PHRASE) DO UPDATE SET
                $COL_SYN_COUNT = $COL_SYN_COUNT + 1,
                $COL_SYN_LAST_USED = ?
            """.trimIndent(),
            arrayOf<Any>(clean, canonicalIntent, System.currentTimeMillis(), System.currentTimeMillis())
        )
    }

    fun getCanonicalIntentForPhrase(phrase: String): String? {
        val clean = phrase.trim().lowercase()
        val cursor = readableDatabase.query(
            TABLE_LEARNED_SYNONYMS,
            arrayOf(COL_SYN_INTENT),
            "$COL_SYN_PHRASE = ?",
            arrayOf(clean),
            null,
            null,
            null,
            "1"
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0)
            }
        }
        return null
    }

    fun getAllLearnedSynonyms(): List<LearnedSynonymRecord> {
        val list = mutableListOf<LearnedSynonymRecord>()
        val cursor = readableDatabase.query(
            TABLE_LEARNED_SYNONYMS,
            null,
            null,
            null,
            null,
            null,
            "$COL_SYN_COUNT DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    LearnedSynonymRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_SYN_ID)),
                        phrase = c.getString(c.getColumnIndexOrThrow(COL_SYN_PHRASE)),
                        canonicalIntent = c.getString(c.getColumnIndexOrThrow(COL_SYN_INTENT)),
                        usageCount = c.getInt(c.getColumnIndexOrThrow(COL_SYN_COUNT)),
                        isConfirmed = c.getInt(c.getColumnIndexOrThrow(COL_SYN_CONFIRMED)) == 1,
                        lastUsedAt = c.getLong(c.getColumnIndexOrThrow(COL_SYN_LAST_USED))
                    )
                )
            }
        }
        return list
    }
}
