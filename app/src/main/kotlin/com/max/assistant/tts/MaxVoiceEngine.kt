package com.max.assistant.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.max.assistant.audio.MaxAudioManager
import com.max.assistant.util.LanguageDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MaxVoiceEngine(
    private val context: Context,
    private val onInitCompleted: (() -> Unit)? = null
) : IVoiceEngine {

    companion object {
        private const val TAG = "MaxVoiceEngine"
        // JARVIS Style: deep, calm, confident British cadence and clear Indian articulation
        const val DEFAULT_JARVIS_PITCH = 0.88f
        const val DEFAULT_JARVIS_SPEED = 0.98f
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val audioManager = MaxAudioManager(context)
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager

    private fun unmuteForVoicePlayback() {
        // Do not alter or override device media volume
    }

    private fun muteAfterVoicePlayback() {
        // Do not force system media volume to 0 after speaking
    }

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    override val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _settings = MutableStateFlow(VoiceSettings.load(context))
    override val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private val pendingCallbacks = mutableMapOf<String, Pair<(() -> Unit)?, (() -> Unit)?>>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureAudioAttributes()
                applyVoiceSettings(_settings.value)
                isReady = true
                Log.i(TAG, "MAX JARVIS Multilingual Voice Engine initialized successfully.")
                onInitCompleted?.invoke()
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
                _settings.value = _settings.value.copy(
                    engineStatusMessage = "TTS Engine Init Failed (Status $status)"
                )
            }
        }
    }

    private fun configureAudioAttributes() {
        val engine = tts ?: return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttributes)

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                unmuteForVoicePlayback()
                utteranceId?.let { id ->
                    pendingCallbacks[id]?.first?.invoke()
                }
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                muteAfterVoicePlayback()
                audioManager.releaseAssistantFocus()
                utteranceId?.let { id ->
                    pendingCallbacks.remove(id)?.second?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                muteAfterVoicePlayback()
                audioManager.releaseAssistantFocus()
                utteranceId?.let { id ->
                    pendingCallbacks.remove(id)?.second?.invoke()
                }
                Log.w(TAG, "TTS Utterance error for: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                muteAfterVoicePlayback()
                audioManager.releaseAssistantFocus()
                utteranceId?.let { id ->
                    pendingCallbacks.remove(id)?.second?.invoke()
                }
                Log.w(TAG, "TTS Utterance error code $errorCode for: $utteranceId")
            }
        })
    }

    private fun applyVoiceSettings(currentSettings: VoiceSettings) {
        val engine = tts ?: return
        engine.setPitch(currentSettings.pitch)
        engine.setSpeechRate(currentSettings.speed)

        val targetLocale = when (currentSettings.outputLanguage) {
            VoiceLanguage.HINGLISH -> Locale("hi", "IN") // hi-IN voice handles both Hindi words & English words well
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.BENGALI -> Locale("bn", "IN")
            VoiceLanguage.ENGLISH -> Locale.UK
            VoiceLanguage.AUTO -> Locale("hi", "IN") // Default to hi-IN for Hinglish auto mode
        }

        val selectedVoiceName = selectBestVoiceForLocale(targetLocale)
        _settings.value = currentSettings.copy(
            currentVoiceName = selectedVoiceName,
            engineStatusMessage = "JARVIS Voice: $selectedVoiceName (${targetLocale.displayLanguage})"
        )
    }

    private fun selectBestVoiceForLocale(locale: Locale): String {
        val engine = tts ?: return "Default Engine Voice"
        val availability = engine.isLanguageAvailable(locale)

        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Locale ${locale.displayName} unavailable (code $availability), falling back to default")
            engine.language = Locale.UK
            return "Fallback Default"
        }

        engine.language = locale

        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val matchingVoices = voices.filter { it.locale.language == locale.language }
                // Rank voices to prioritize deep, natural male models inspired by JARVIS
                val bestCandidate = matchingVoices.maxByOrNull { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    var score = 0
                    if (name.contains("neural") || name.contains("natural")) score += 50
                    if (name.contains("network")) score += 30
                    if ((name.contains("male") || name.contains("rjs") || name.contains("cfl") || name.contains("iol") || name.contains("sfg")) && !name.contains("female")) score += 35
                    if (voice.quality == Voice.QUALITY_VERY_HIGH || voice.quality == Voice.QUALITY_HIGH) score += 20
                    if (voice.latency == Voice.LATENCY_VERY_LOW || voice.latency == Voice.LATENCY_LOW) score += 10
                    score
                }

                if (bestCandidate != null) {
                    engine.voice = bestCandidate
                    Log.i(TAG, "Assigned Natural JARVIS Voice: ${bestCandidate.name} for ${locale.displayName}")
                    return bestCandidate.name
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection exception: ${e.message}")
        }

        return "${locale.displayLanguage} (System Default)"
    }

    override fun speak(
        text: String,
        targetLanguage: VoiceLanguage?,
        onSpeechStarted: (() -> Unit)?,
        onSpeechFinished: (() -> Unit)?
    ) {
        if (_isMuted.value) {
            Log.d(TAG, "MAX is muted. Speech skipped: $text")
            _lastSpokenText.value = "[MUTED] $text"
            onSpeechFinished?.invoke()
            return
        }

        val engine = tts
        if (!isReady || engine == null) {
            Log.w(TAG, "TTS not ready yet: $text")
            onSpeechFinished?.invoke()
            return
        }

        val langToUse = when {
            targetLanguage != null && targetLanguage != VoiceLanguage.AUTO -> targetLanguage
            _settings.value.outputLanguage != VoiceLanguage.AUTO -> _settings.value.outputLanguage
            else -> LanguageDetector.detectLanguage(text)
        }

        val localeToSet = when (langToUse) {
            VoiceLanguage.HINGLISH -> Locale("hi", "IN") // hi-IN TTS engine pronounces English words inside Hindi sentences naturally
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.BENGALI -> Locale("bn", "IN")
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> Locale("hi", "IN") // Default to hi-IN for Hinglish
        }

        // Verify language availability
        val availability = engine.isLanguageAvailable(localeToSet)
        val textToSpeak: String
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language ${localeToSet.displayName} is missing data. Warning user naturally.")
            engine.language = Locale.UK
            selectBestVoiceForLocale(Locale.UK)
            textToSpeak = "Sir, voice data for ${localeToSet.displayLanguage} is not yet installed on this device. $text"
        } else {
            engine.language = localeToSet
            selectBestVoiceForLocale(localeToSet)
            textToSpeak = text
        }

        engine.setPitch(_settings.value.pitch)
        engine.setSpeechRate(_settings.value.speed)

        // Request transient audio focus and ensure volume is ready before audio streaming begins
        audioManager.requestAssistantFocus()
        unmuteForVoicePlayback()

        _lastSpokenText.value = textToSpeak
        val utteranceId = "MAX_${System.currentTimeMillis()}"
        pendingCallbacks[utteranceId] = Pair(onSpeechStarted, onSpeechFinished)

        // Safety watchdog: ensure listener recovery even if TTS engine fails to fire onDone
        val estimatedDurationMs = (textToSpeak.length * 150L + 3500L).coerceIn(4000L, 30000L)
        val watchdogHandler = Handler(Looper.getMainLooper())
        watchdogHandler.postDelayed({
            pendingCallbacks.remove(utteranceId)?.let { (_, onFinished) ->
                Log.w(TAG, "Safety watchdog: TTS onDone was not called within ${estimatedDurationMs}ms. Recovering listener.")
                _isSpeaking.value = false
                audioManager.releaseAssistantFocus()
                onFinished?.invoke()
            }
        }, estimatedDurationMs)

        // Use STREAM_MUSIC so Samsung One UI plays through main stereo speakers without call routing chimes
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }

        Log.i(TAG, "Speaking aloud: '$textToSpeak'")
        val result = engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech.speak failed with code: $result")
            audioManager.releaseAssistantFocus()
            _isSpeaking.value = false
            pendingCallbacks.remove(utteranceId)?.second?.invoke()
        }
    }

    override fun stop() {
        tts?.stop()
        muteAfterVoicePlayback()
        audioManager.releaseAssistantFocus()
        _isSpeaking.value = false
        val callbacks = pendingCallbacks.values.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it.second?.invoke() }
    }

    override fun setMute(mute: Boolean) {
        _isMuted.value = mute
        if (mute) {
            stop()
        }
    }

    override fun toggleMute(): Boolean {
        val newState = !_isMuted.value
        setMute(newState)
        return newState
    }

    override fun updatePitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.5f)
        _settings.value = _settings.value.copy(pitch = clamped)
        tts?.setPitch(clamped)
        _settings.value.save(context)
    }

    override fun updateSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 1.5f)
        _settings.value = _settings.value.copy(speed = clamped)
        tts?.setSpeechRate(clamped)
        _settings.value.save(context)
    }

    override fun updateInputLanguage(language: VoiceLanguage) {
        _settings.value = _settings.value.copy(inputLanguage = language)
        _settings.value.save(context)
    }

    override fun updateOutputLanguage(language: VoiceLanguage) {
        _settings.value = _settings.value.copy(outputLanguage = language)
        applyVoiceSettings(_settings.value)
        _settings.value.save(context)
    }

    override fun updateAllowShortWakeWord(allow: Boolean) {
        _settings.value = _settings.value.copy(allowShortWakeWord = allow)
        _settings.value.save(context)
    }

    override fun updateMuteListeningBeep(mute: Boolean) {
        _settings.value = _settings.value.copy(muteListeningBeep = mute)
        _settings.value.save(context)
    }

    override fun getAvailableVoices(): List<VoiceProfileInfo> {
        val engine = tts ?: return emptyList()
        return try {
            val list = mutableListOf<VoiceProfileInfo>()
            val voices = engine.voices
            if (voices != null) {
                for (voice in voices) {
                    val lang = voice.locale.displayName
                    val nameLower = voice.name.lowercase(Locale.ROOT)
                    val isMale = (nameLower.contains("male") || nameLower.contains("rjs") || nameLower.contains("cfl") || nameLower.contains("sfg")) &&
                            !nameLower.contains("female")
                    list.add(
                        VoiceProfileInfo(
                            id = voice.name,
                            name = voice.name,
                            language = lang,
                            isMale = isMale,
                            isAvailable = !voice.isNetworkConnectionRequired
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating voices: ${e.message}")
            emptyList()
        }
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
