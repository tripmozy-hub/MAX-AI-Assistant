package com.max.assistant.dictionary

import android.content.Context
import com.max.assistant.database.MaxDatabaseHelper
import com.max.assistant.database.StudyNoteRecord
import com.max.assistant.productivity.NoteItem
import com.max.assistant.productivity.NotesAndTasksManager
import com.max.assistant.tts.VoiceLanguage

/**
 * NotesManager provides unified CRUD and document summarization for local and study notes.
 */
class NotesManager(
    private val context: Context,
    private val generalNotes: NotesAndTasksManager = NotesAndTasksManager(context),
    private val dbHelper: MaxDatabaseHelper = MaxDatabaseHelper.getInstance(context)
) {

    fun createNote(content: String): NoteItem {
        return generalNotes.addNote(content)
    }

    fun getAllNotes(): List<NoteItem> {
        return generalNotes.getAllNotes()
    }

    fun findNotes(query: String): List<NoteItem> {
        return generalNotes.findNotes(query)
    }

    fun findStudyNotes(query: String): List<StudyNoteRecord> {
        val all = dbHelper.getAllStudyNotes()
        val q = query.lowercase().trim()
        return all.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
    }

    fun deleteNote(query: String): Boolean {
        return generalNotes.deleteNote(query)
    }

    fun summarizeText(text: String, language: VoiceLanguage): String {
        val lines = text.split("\n").filter { it.isNotBlank() }
        val bulletPoints = lines.take(3).joinToString("\n• ")
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, document summary:\n• $bulletPoints"
            VoiceLanguage.HINDI -> "सर, दस्तावेज़ का सारांश:\n• $bulletPoints"
            VoiceLanguage.BENGALI -> "স্যার, সারসংক্ষেপ:\n• $bulletPoints"
            else -> "Sir, document summary:\n• $bulletPoints"
        }
    }
}
