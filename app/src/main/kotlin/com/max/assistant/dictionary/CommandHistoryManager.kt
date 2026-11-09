package com.max.assistant.dictionary

import android.content.Context
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import java.util.Collections

data class CommandHistoryEntry(
    val intentId: String,
    val query: String,
    val language: VoiceLanguage,
    val response: String,
    val wasSuccessful: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * CommandHistoryManager maintains an audit log of voice commands and execution results.
 */
class CommandHistoryManager(private val context: Context) {

    companion object {
        private const val TAG = "CommandHistory"
        private const val MAX_HISTORY_SIZE = 100
    }

    private val historyList = Collections.synchronizedList(mutableListOf<CommandHistoryEntry>())

    fun recordCommand(
        intentId: String,
        query: String,
        language: VoiceLanguage,
        response: String,
        wasSuccessful: Boolean
    ) {
        val entry = CommandHistoryEntry(intentId, query, language, response, wasSuccessful)
        historyList.add(0, entry)
        if (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(historyList.size - 1)
        }
        Log.i(TAG, "Recorded command: intent=$intentId, success=$wasSuccessful")
    }

    fun getRecentHistory(limit: Int = 20): List<CommandHistoryEntry> {
        return historyList.take(limit)
    }

    fun clearHistory() {
        historyList.clear()
    }
}
