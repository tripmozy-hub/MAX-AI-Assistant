package com.max.assistant.ai

import com.max.assistant.tts.VoiceLanguage

enum class MessageRole {
    USER,
    MAX,
    SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: VoiceLanguage = VoiceLanguage.AUTO
)
