package com.max.assistant.tts

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

enum class VoiceLanguage(val displayName: String, val locale: Locale, val recognitionTag: String) {
    AUTO("Auto-Detect (JARVIS / Multilingual)", Locale.UK, "en-GB"),
    HINGLISH("Hinglish (Hindi + English Mix)", Locale("hi", "IN"), "hi-IN"),
    HINDI("Hindi (हिन्दी)", Locale("hi", "IN"), "hi-IN"),
    ENGLISH("English (JARVIS British)", Locale.UK, "en-GB"),
    BENGALI("Bengali (বাংলা)", Locale("bn", "IN"), "bn-IN");

    companion object {
        fun fromTag(tag: String?): VoiceLanguage {
            return when {
                tag.isNullOrEmpty() -> AUTO
                tag.equals("hinglish", ignoreCase = true) -> HINGLISH
                tag.startsWith("hi", ignoreCase = true) -> HINDI
                tag.startsWith("bn", ignoreCase = true) -> BENGALI
                tag.startsWith("en", ignoreCase = true) -> ENGLISH
                else -> AUTO
            }
        }
    }
}

data class VoiceSettings(
    val inputLanguage: VoiceLanguage = VoiceLanguage.HINGLISH,
    val outputLanguage: VoiceLanguage = VoiceLanguage.HINGLISH,
    val pitch: Float = 0.82f,                       // Refined deep intelligent baritone (JARVIS)
    val speed: Float = 0.94f,                       // Calm, measured intelligent cadence (JARVIS)
    val wakeWordEnabled: Boolean = true,
    val allowShortWakeWord: Boolean = true,        // Prefer "MAX" and common matching variants by default
    val continuousListening: Boolean = true,
    val muteListeningBeep: Boolean = true,
    val currentVoiceName: String = "JARVIS Neural Cloud Matrix",
    val engineStatusMessage: String = "Fish Audio JARVIS Voice Active",
    val useNeuralCloudVoice: Boolean = true,
    val fishAudioApiKey: String = FishAudioService.DEFAULT_FISH_API_KEY,
    val fishAudioVoiceId: String = FishAudioService.DEFAULT_JARVIS_VOICE_ID
) {
    companion object {
        private const val PREFS = "max_voice_settings"
        private const val KEY_INPUT_LANG = "input_lang"
        private const val KEY_OUTPUT_LANG = "output_lang"
        private const val KEY_PITCH = "pitch"
        private const val KEY_SPEED = "speed"
        private const val KEY_WAKE_WORD = "wake_word_enabled"
        private const val KEY_ALLOW_SHORT_WAKE = "allow_short_wake_word"
        private const val KEY_CONTINUOUS = "continuous_listening"
        private const val KEY_MUTE_BEEP = "mute_listening_beep"
        private const val KEY_JARVIS_VOICE_VER = "jarvis_voice_v7_fish"
        private const val KEY_USE_CLOUD_VOICE = "use_neural_cloud_voice"
        private const val KEY_FISH_API_KEY = "fish_audio_api_key"
        private const val KEY_FISH_VOICE_ID = "fish_audio_voice_id"

        fun load(context: Context): VoiceSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            // Auto-upgrade to JARVIS Fish Audio neural voice if not yet migrated to v7
            val isMigrated = p.getBoolean(KEY_JARVIS_VOICE_VER, false)
            if (!isMigrated) {
                p.edit()
                    .putBoolean(KEY_JARVIS_VOICE_VER, true)
                    .putString(KEY_INPUT_LANG, VoiceLanguage.HINGLISH.name)
                    .putString(KEY_OUTPUT_LANG, VoiceLanguage.HINGLISH.name)
                    .putFloat(KEY_PITCH, 0.82f)
                    .putFloat(KEY_SPEED, 0.94f)
                    .putBoolean(KEY_USE_CLOUD_VOICE, true)
                    .putString(KEY_FISH_API_KEY, FishAudioService.DEFAULT_FISH_API_KEY)
                    .putString(KEY_FISH_VOICE_ID, FishAudioService.DEFAULT_JARVIS_VOICE_ID)
                    .apply()
                return VoiceSettings(
                    inputLanguage = VoiceLanguage.HINGLISH,
                    outputLanguage = VoiceLanguage.HINGLISH,
                    pitch = 0.82f,
                    speed = 0.94f,
                    useNeuralCloudVoice = true,
                    fishAudioApiKey = FishAudioService.DEFAULT_FISH_API_KEY,
                    fishAudioVoiceId = FishAudioService.DEFAULT_JARVIS_VOICE_ID
                )
            }

            val inLang = try { VoiceLanguage.valueOf(p.getString(KEY_INPUT_LANG, VoiceLanguage.HINGLISH.name) ?: VoiceLanguage.HINGLISH.name) } catch (e: IllegalArgumentException) { VoiceLanguage.HINGLISH }
            val outLang = try { VoiceLanguage.valueOf(p.getString(KEY_OUTPUT_LANG, VoiceLanguage.HINGLISH.name) ?: VoiceLanguage.HINGLISH.name) } catch (e: IllegalArgumentException) { VoiceLanguage.HINGLISH }
            val pitch = p.getFloat(KEY_PITCH, 0.82f)
            val speed = p.getFloat(KEY_SPEED, 0.94f)
            val wake = p.getBoolean(KEY_WAKE_WORD, true)
            val shortWake = p.getBoolean(KEY_ALLOW_SHORT_WAKE, true)
            val continuous = p.getBoolean(KEY_CONTINUOUS, true)
            val muteBeep = p.getBoolean(KEY_MUTE_BEEP, true)
            val useCloud = p.getBoolean(KEY_USE_CLOUD_VOICE, true)
            val fishKey = p.getString(KEY_FISH_API_KEY, FishAudioService.DEFAULT_FISH_API_KEY) ?: FishAudioService.DEFAULT_FISH_API_KEY
            val fishVoice = p.getString(KEY_FISH_VOICE_ID, FishAudioService.DEFAULT_JARVIS_VOICE_ID) ?: FishAudioService.DEFAULT_JARVIS_VOICE_ID

            return VoiceSettings(
                inputLanguage = inLang,
                outputLanguage = outLang,
                pitch = pitch,
                speed = speed,
                wakeWordEnabled = wake,
                allowShortWakeWord = shortWake,
                continuousListening = continuous,
                muteListeningBeep = muteBeep,
                useNeuralCloudVoice = useCloud,
                fishAudioApiKey = fishKey,
                fishAudioVoiceId = fishVoice
            )
        }
    }

    fun save(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit()
            .putString(KEY_INPUT_LANG, inputLanguage.name)
            .putString(KEY_OUTPUT_LANG, outputLanguage.name)
            .putFloat(KEY_PITCH, pitch)
            .putFloat(KEY_SPEED, speed)
            .putBoolean(KEY_WAKE_WORD, wakeWordEnabled)
            .putBoolean(KEY_ALLOW_SHORT_WAKE, allowShortWakeWord)
            .putBoolean(KEY_CONTINUOUS, continuousListening)
            .putBoolean(KEY_MUTE_BEEP, muteListeningBeep)
            .putBoolean(KEY_USE_CLOUD_VOICE, useNeuralCloudVoice)
            .putString(KEY_FISH_API_KEY, fishAudioApiKey)
            .putString(KEY_FISH_VOICE_ID, fishAudioVoiceId)
            .apply()
    }
}
