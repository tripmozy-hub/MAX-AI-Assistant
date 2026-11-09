package com.max.assistant.productivity

import android.content.Context
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

class NotesAndTasksManager(private val context: Context) {

    companion object {
        private const val TAG = "NotesAndTasksManager"
        private const val NOTES_FILE = "max_notes.json"
        private const val TASKS_FILE = "max_tasks.json"
    }

    private val notesLock = Any()
    private val tasksLock = Any()

    // 1. Notes API
    fun addNote(content: String): NoteItem {
        val trimmed = content.trim()
        val item = NoteItem(content = trimmed)
        synchronized(notesLock) {
            val list = loadNotesInternal().toMutableList()
            list.add(0, item) // newest first
            saveNotesInternal(list)
        }
        Log.i(TAG, "Saved note: '${item.content}'")
        return item
    }

    fun getAllNotes(): List<NoteItem> {
        synchronized(notesLock) {
            return loadNotesInternal()
        }
    }

    fun findNotes(query: String): List<NoteItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        return getAllNotes().filter { it.content.lowercase(Locale.ROOT).contains(q) }
    }

    fun deleteNote(query: String): Boolean {
        synchronized(notesLock) {
            val list = loadNotesInternal().toMutableList()
            val index = list.indexOfFirst {
                it.id == query || it.content.contains(query, ignoreCase = true)
            }
            if (index >= 0) {
                list.removeAt(index)
                saveNotesInternal(list)
                return true
            }
        }
        return false
    }

    fun clearNotes() {
        synchronized(notesLock) {
            saveNotesInternal(emptyList())
        }
    }

    fun formatNotesSummary(language: VoiceLanguage): String {
        val notes = getAllNotes()
        if (notes.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI -> "सर, आपके पास अभी कोई सुरक्षित नोट नहीं है।"
                VoiceLanguage.BENGALI -> "স্যার, আপনার কোনো সংরক্ষিত নোট নেই।"
                else -> "Sir, you currently have no saved notes."
            }
        }

        val count = notes.size
        val topNotes = notes.take(3).mapIndexed { i, n -> "${i + 1}. ${n.content}" }.joinToString("; ")
        return when (language) {
            VoiceLanguage.HINDI -> "सर, आपके पास कुल $count नोट हैं: $topNotes"
            VoiceLanguage.BENGALI -> "স্যার, আপনার কাছে মোট $count টি নোট আছে: $topNotes"
            else -> "Sir, you have $count saved notes: $topNotes"
        }
    }

    // 2. Tasks / Checklist API
    fun addTask(title: String): TaskItem {
        val trimmed = title.trim()
        val task = TaskItem(title = trimmed)
        synchronized(tasksLock) {
            val list = loadTasksInternal().toMutableList()
            list.add(task)
            saveTasksInternal(list)
        }
        Log.i(TAG, "Added task: '${task.title}'")
        return task
    }

    fun getAllTasks(): List<TaskItem> {
        synchronized(tasksLock) {
            return loadTasksInternal()
        }
    }

    fun completeTask(target: String): Boolean {
        synchronized(tasksLock) {
            val list = loadTasksInternal().toMutableList()
            val index = list.indexOfFirst {
                it.title.contains(target, ignoreCase = true) || it.id == target
            }
            if (index >= 0) {
                val current = list[index]
                list[index] = current.copy(isCompleted = true)
                saveTasksInternal(list)
                return true
            }
        }
        return false
    }

    fun removeTask(target: String): Boolean {
        synchronized(tasksLock) {
            val list = loadTasksInternal().toMutableList()
            val index = list.indexOfFirst {
                it.title.contains(target, ignoreCase = true) || it.id == target
            }
            if (index >= 0) {
                list.removeAt(index)
                saveTasksInternal(list)
                return true
            }
        }
        return false
    }

    fun clearCompletedTasks(): Int {
        synchronized(tasksLock) {
            val list = loadTasksInternal()
            val remaining = list.filterNot { it.isCompleted }
            saveTasksInternal(remaining)
            return list.size - remaining.size
        }
    }

    fun formatTasksSummary(language: VoiceLanguage): String {
        val tasks = getAllTasks()
        if (tasks.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI -> "सर, आपकी कार्य सूची में कोई कार्य नहीं है।"
                VoiceLanguage.BENGALI -> "স্যার, আপনার কাজের তালিকায় কোনো কাজ নেই।"
                else -> "Sir, your task list is currently empty."
            }
        }

        val pending = tasks.filterNot { it.isCompleted }
        if (pending.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI -> "सर, आपके सभी कार्य पूर्ण हो चुके हैं।"
                VoiceLanguage.BENGALI -> "স্যার, আপনার সব কাজ সম্পন্ন হয়েছে।"
                else -> "Sir, all tasks in your list have been completed."
            }
        }

        val summary = pending.take(3).mapIndexed { i, t -> "${i + 1}. ${t.title}" }.joinToString("; ")
        val totalPending = pending.size
        return when (language) {
            VoiceLanguage.HINDI -> "सर, आपके $totalPending कार्य बाकी हैं: $summary"
            VoiceLanguage.BENGALI -> "স্যার, আপনার $totalPending টি কাজ বাকি আছে: $summary"
            else -> "Sir, you have $totalPending pending tasks: $summary"
        }
    }

    // Internal JSON I/O
    private fun loadNotesInternal(): List<NoteItem> {
        val file = File(context.filesDir, NOTES_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val result = mutableListOf<NoteItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    NoteItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        content = obj.optString("content", ""),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes: ${e.message}")
            emptyList()
        }
    }

    private fun saveNotesInternal(notes: List<NoteItem>) {
        try {
            val array = JSONArray()
            for (item in notes) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("content", item.content)
                    put("timestampMillis", item.timestampMillis)
                }
                array.put(obj)
            }
            File(context.filesDir, NOTES_FILE).writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes: ${e.message}")
        }
    }

    private fun loadTasksInternal(): List<TaskItem> {
        val file = File(context.filesDir, TASKS_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val result = mutableListOf<TaskItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    TaskItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tasks: ${e.message}")
            emptyList()
        }
    }

    private fun saveTasksInternal(tasks: List<TaskItem>) {
        try {
            val array = JSONArray()
            for (item in tasks) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("isCompleted", item.isCompleted)
                    put("timestampMillis", item.timestampMillis)
                }
                array.put(obj)
            }
            File(context.filesDir, TASKS_FILE).writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tasks: ${e.message}")
        }
    }
}
