package com.max.assistant.vision

import android.graphics.Bitmap
import android.util.Log
import com.max.assistant.ai.ChatMessage
import com.max.assistant.ai.IAiProvider
import com.max.assistant.ai.MessageRole
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class ScreenSnapshotContext(
    val summary: String,
    val visibleText: String,
    val bitmap: Bitmap?,
    val timestamp: Long = System.currentTimeMillis()
)

class ScreenContextManager {

    companion object {
        private const val TAG = "ScreenContextManager"
        private const val CONTEXT_EXPIRY_MS = 120_000L // 2 minutes context retention
    }

    private val _currentContext = MutableStateFlow<ScreenSnapshotContext?>(null)
    val currentContext: StateFlow<ScreenSnapshotContext?> = _currentContext.asStateFlow()

    private val _isLiveSession = MutableStateFlow(false)
    val isLiveSession: StateFlow<Boolean> = _isLiveSession.asStateFlow()

    fun updateScreenContext(summary: String, visibleText: String, bitmap: Bitmap?) {
        _currentContext.value = ScreenSnapshotContext(
            summary = summary,
            visibleText = visibleText,
            bitmap = bitmap
        )
        Log.i(TAG, "Screen context updated (${visibleText.length} chars OCR, summary length: ${summary.length})")
    }

    fun setLiveSession(active: Boolean) {
        _isLiveSession.value = active
        if (!active) {
            Log.i(TAG, "Live screen session ended.")
        } else {
            Log.i(TAG, "Live screen session started.")
        }
    }

    fun hasValidContext(): Boolean {
        val ctx = _currentContext.value ?: return false
        val elapsed = System.currentTimeMillis() - ctx.timestamp
        return elapsed < CONTEXT_EXPIRY_MS
    }

    fun isScreenFollowUpQuery(input: String): Boolean {
        if (!hasValidContext()) return false
        val lower = input.lowercase(Locale.ROOT).trim()

        val followUpTriggers = listOf(
            "what is this", "ye kya hai", "eta ki", "explain this", "isko samjhao",
            "second option", "doosra option", "next option", "pehla option", "first option",
            "what should i click", "kahan click karu", "kya dabau", "kothay click korbo",
            "is this safe", "kya ye safe hai", "eta ki safe", "safe hai kya",
            "translate this", "translate", "hindi mein translate", "bangla te translate",
            "read the small text", "chhota text", "choto lekha",
            "what does this error mean", "error ka matlab", "error kya hai",
            "what should i do next", "ab kya karu", "ebar ki korbo", "what next",
            "explain more", "aur batao", "aro bolo", "read this", "ye padho"
        )

        for (trigger in followUpTriggers) {
            if (lower.contains(trigger)) return true
        }
        return false
    }

    suspend fun answerFollowUpQuery(
        query: String,
        aiProvider: IAiProvider?,
        targetLang: VoiceLanguage
    ): String {
        val ctx = _currentContext.value ?: return "Sir, I do not have recent screen context. Please ask me to look at your screen first."

        val prompt = "The user is asking a follow-up question about what was just visible on their Android phone screen.\n" +
                "Previous screen content summary:\n${ctx.summary}\n\n" +
                "Extracted text on screen:\n${ctx.visibleText.take(1500)}\n\n" +
                "User follow-up question: '$query'\n\n" +
                "Answer directly, helpfully, and concisely for voice response in ${targetLang.name}."

        val provider = aiProvider
        if (provider != null && provider.isConfigured) {
            try {
                val history = listOf(
                    ChatMessage(role = MessageRole.MAX, text = "I analyzed your screen: ${ctx.summary}")
                )
                val response = provider.generateResponse(prompt, history, targetLang)
                if (response is com.max.assistant.ai.AiResponse.Success && response.text.isNotBlank()) {
                    return response.text
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error generating AI response for screen follow-up: ${e.message}")
            }
        }

        // Local fallback if AI is offline
        return when (targetLang) {
            VoiceLanguage.HINDI -> "सर, पिछली स्क्रीन के अनुसार: ${ctx.summary.take(120)}..."
            VoiceLanguage.BENGALI -> "স্যার, পূর্ববর্তী স্ক্রিন অনুসারে: ${ctx.summary.take(120)}..."
            VoiceLanguage.HINGLISH -> "Sir, previous screen ke mutabiq: ${ctx.summary.take(120)}..."
            else -> "Sir, based on the captured screen: ${ctx.summary.take(120)}..."
        }
    }

    fun clear() {
        _currentContext.value = null
        _isLiveSession.value = false
    }
}
