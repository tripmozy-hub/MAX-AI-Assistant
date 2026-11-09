package com.max.assistant.tts

import kotlinx.coroutines.flow.StateFlow

data class VoiceProfileInfo(
    val id: String,
    val name: String,
    val language: String,
    val isMale: Boolean,
    val isAvailable: Boolean
)

/**
 * Common interface for MAX Voice Engines.
 * Allows seamless switching between native Android TTS and future Cloud TTS providers (e.g. ElevenLabs, Google Cloud Neural2).
 */
interface IVoiceEngine {
    val isMuted: StateFlow<Boolean>
    val isSpeaking: StateFlow<Boolean>
    val lastSpokenText: StateFlow<String>
    val settings: StateFlow<VoiceSettings>

    fun speak(
        text: String,
        targetLanguage: VoiceLanguage? = null,
        onSpeechStarted: (() -> Unit)? = null,
        onSpeechFinished: (() -> Unit)? = null
    )

    fun stop()
    fun setMute(mute: Boolean)
    fun toggleMute(): Boolean
    fun updatePitch(pitch: Float)
    fun updateSpeed(speed: Float)
    fun updateInputLanguage(language: VoiceLanguage)
    fun updateOutputLanguage(language: VoiceLanguage)
    fun updateAllowShortWakeWord(allow: Boolean)
    fun updateMuteListeningBeep(mute: Boolean)
    fun getAvailableVoices(): List<VoiceProfileInfo>
    fun shutdown()
}
