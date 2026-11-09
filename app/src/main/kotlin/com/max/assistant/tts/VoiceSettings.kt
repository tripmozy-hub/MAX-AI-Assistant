package com.max.assistant.tts

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

enum class VoiceLanguage(val displayName: String, val locale: Locale, val recognitionTag: String) {
    AUTO("Auto-Detect (স্বয়ংক্রিয় / स्वचालित)", Locale.UK, "en-IN"),
    HINGLISH("Hinglish (Hindi + English Mix)", Locale("hi", "IN"), "hi-IN"),
    HINDI("Hindi (हिन्दी)", Locale("hi", "IN"), "hi-IN"),
    ENGLISH("English (India/UK)", Locale.UK, "en-IN"),
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
    val pitch: Float = 0.88f,                       // Deep male JARVIS pitch
    val speed: Float = 0.98f,                       // Calm, intelligent cadence
    val wakeWordEnabled: Boolean = true,
    val allowShortWakeWord: Boolean = true,        // Prefer "MAX" and common matching variants by default
    val continuousListening: Boolean = true,
    val muteListeningBeep: Boolean = true,
    val currentVoiceName: String = "JARVIS Voice Engine",
    val engineStatusMessage: String = "Multi-lingual System Ready"
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

        fun load(context: Context): VoiceSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val inLang = try { VoiceLanguage.valueOf(p.getString(KEY_INPUT_LANG, VoiceLanguage.HINGLISH.name) ?: VoiceLanguage.HINGLISH.name) } catch (e: IllegalArgumentException) { VoiceLanguage.HINGLISH }
            val outLang = try { VoiceLanguage.valueOf(p.getString(KEY_OUTPUT_LANG, VoiceLanguage.HINGLISH.name) ?: VoiceLanguage.HINGLISH.name) } catch (e: IllegalArgumentException) { VoiceLanguage.HINGLISH }
            val pitch = p.getFloat(KEY_PITCH, 0.88f)
            val speed = p.getFloat(KEY_SPEED, 0.98f)
            val wake = p.getBoolean(KEY_WAKE_WORD, true)
            val shortWake = p.getBoolean(KEY_ALLOW_SHORT_WAKE, true)
            val continuous = p.getBoolean(KEY_CONTINUOUS, true)
            val muteBeep = p.getBoolean(KEY_MUTE_BEEP, true)

            return VoiceSettings(
                inputLanguage = inLang,
                outputLanguage = outLang,
                pitch = pitch,
                speed = speed,
                wakeWordEnabled = wake,
                allowShortWakeWord = shortWake,
                continuousListening = continuous,
                muteListeningBeep = muteBeep
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
            .apply()
    }
}
