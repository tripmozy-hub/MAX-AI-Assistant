package com.max.assistant.ai

import com.max.assistant.tts.VoiceLanguage

sealed class AiResponse {
    data class Success(val text: String, val language: VoiceLanguage) : AiResponse()
    data class Error(val message: String, val isNetworkError: Boolean = false) : AiResponse()
}

interface IAiProvider {
    val providerName: String
    val isConfigured: Boolean

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        preferredLanguage: VoiceLanguage,
        systemInstructionOverride: String? = null
    ): AiResponse

    suspend fun analyzeImage(
        base64Jpeg: String,
        prompt: String,
        preferredLanguage: VoiceLanguage
    ): AiResponse
}
