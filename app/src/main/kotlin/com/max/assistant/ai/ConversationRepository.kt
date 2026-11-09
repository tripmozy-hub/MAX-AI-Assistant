package com.max.assistant.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ConversationRepository(private val context: Context? = null) {

    companion object {
        private const val PREFS_NAME = "max_conversation_history"
        private const val KEY_MESSAGES = "history_json"
        private const val MAX_SAVED_MESSAGES = 50

        @Volatile
        private var instance: ConversationRepository? = null

        fun getInstance(context: Context? = null): ConversationRepository {
            return instance ?: synchronized(this) {
                instance ?: ConversationRepository(context).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        loadHistory()
    }

    @Synchronized
    fun addMessage(role: MessageRole, text: String, language: com.max.assistant.tts.VoiceLanguage = com.max.assistant.tts.VoiceLanguage.AUTO): ChatMessage {
        val msg = ChatMessage(role = role, text = text, language = language)
        val currentList = _messages.value.toMutableList()
        currentList.add(msg)
        if (currentList.size > MAX_SAVED_MESSAGES) {
            currentList.removeAt(0)
        }
        _messages.value = currentList
        saveHistory()
        return msg
    }

    fun getRecentHistory(limit: Int = 10): List<ChatMessage> {
        val current = _messages.value
        return if (current.size > limit) current.takeLast(limit) else current
    }

    fun getLastUserQuery(): String? {
        return _messages.value.lastOrNull { it.role == MessageRole.USER }?.text
    }

    fun getLastBotResponse(): String? {
        return _messages.value.lastOrNull { it.role == MessageRole.MAX }?.text
    }

    fun clearHistory() {
        _messages.value = emptyList()
        prefs?.edit()?.remove(KEY_MESSAGES)?.apply()
    }

    private fun saveHistory() {
        val p = prefs ?: return
        try {
            val arr = JSONArray()
            _messages.value.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role.name)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("language", msg.language.name)
                }
                arr.put(obj)
            }
            p.edit().putString(KEY_MESSAGES, arr.toString()).apply()
        } catch (e: Exception) {
            // Non-critical persistence failure
        }
    }

    private fun loadHistory() {
        val p = prefs ?: return
        val json = p.getString(KEY_MESSAGES, null) ?: return
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val msg = ChatMessage(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    role = MessageRole.valueOf(obj.optString("role", MessageRole.USER.name)),
                    text = obj.optString("text", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    language = com.max.assistant.tts.VoiceLanguage.valueOf(obj.optString("language", com.max.assistant.tts.VoiceLanguage.AUTO.name))
                )
                list.add(msg)
            }
            _messages.value = list
        } catch (e: Exception) {
            _messages.value = emptyList()
        }
    }
}
