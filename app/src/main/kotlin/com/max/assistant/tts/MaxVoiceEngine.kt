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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        // JARVIS Style: deep, calm, confident British cadence (Paul Bettany profile)
        const val DEFAULT_JARVIS_PITCH = 0.83f
        const val DEFAULT_JARVIS_SPEED = 0.94f
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val fishAudioService = FishAudioService(context, engineScope)

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
        initTtsEngine()
    }

    private fun initTtsEngine() {
        val hasGoogleTts = try {
            context.packageManager.getPackageInfo(GOOGLE_TTS_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }

        val engineToUse = if (hasGoogleTts) GOOGLE_TTS_PACKAGE else null
        Log.i(TAG, "Initializing TextToSpeech with preferred engine: $engineToUse")

        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureAudioAttributes()
                applyVoiceSettings(_settings.value)
                isReady = true
                Log.i(TAG, "MAX JARVIS Multilingual Voice Engine initialized successfully.")
                onInitCompleted?.invoke()
            } else {
                Log.w(TAG, "Failed to initialize TextToSpeech with engine $engineToUse (status=$status). Trying default engine.")
                if (engineToUse != null) {
                    tts = TextToSpeech(context) { fallbackStatus ->
                        if (fallbackStatus == TextToSpeech.SUCCESS) {
                            configureAudioAttributes()
                            applyVoiceSettings(_settings.value)
                            isReady = true
                            Log.i(TAG, "MAX JARVIS Voice Engine initialized with system default.")
                            onInitCompleted?.invoke()
                        } else {
                            Log.e(TAG, "Fallback TTS initialization failed: status=$fallbackStatus")
                            _settings.value = _settings.value.copy(
                                engineStatusMessage = "TTS Engine Init Failed (Status $fallbackStatus)"
                            )
                        }
                    }
                } else {
                    _settings.value = _settings.value.copy(
                        engineStatusMessage = "TTS Engine Init Failed (Status $status)"
                    )
                }
            }
        }, engineToUse)
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
            VoiceLanguage.AUTO -> Locale.UK // Default to British English for JARVIS persona
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
            Log.w(TAG, "Locale ${locale.displayName} unavailable (code $availability), falling back to UK English")
            engine.language = Locale.UK
            return "Fallback Default"
        }

        engine.language = locale

        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val matchingVoices = voices.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
                if (matchingVoices.isNotEmpty()) {
                    val isUkTarget = locale.country.equals("GB", ignoreCase = true) || locale.language.equals("en", ignoreCase = true)

                    // Rank voices to prioritize deep, natural British male models inspired by JARVIS
                    val bestCandidate = matchingVoices.maxByOrNull { voice ->
                        val name = voice.name.lowercase(Locale.ROOT)
                        val country = voice.locale.country
                        var score = 0

                        if (isUkTarget) {
                            // Heavy preference for UK English voices
                            if (country.equals("GB", ignoreCase = true) || name.contains("en-gb") || name.contains("en_gb")) {
                                score += 200
                            } else if (country.equals("US", ignoreCase = true) || name.contains("en-us") || name.contains("en_us")) {
                                score += 20 // Secondary fallback
                            }

                            // Iconic Google & Samsung British male JARVIS voice profiles
                            if (name.contains("rjs")) score += 350      // Google en-gb-x-rjs: The premier JARVIS voice
                            if (name.contains("cfl")) score += 200      // Google en-gb-x-cfl: Refined British male
                            if (name.contains("fis")) score += 180      // Google en-gb-x-fis: Deep British male
                            if (name.contains("gbr")) score += 150      // Google en-gb-x-gbr
                            if (name.contains("male") && !name.contains("female")) score += 120
                        } else if (locale.language.equals("hi", ignoreCase = true)) {
                            // Premier Google Hindi male voice profiles for JARVIS in Hindi/Hinglish
                            if (name.contains("hie")) score += 350      // Google hi-in-x-hie: The premier deep Hindi male
                            if (name.contains("cfc")) score += 220      // Google hi-in-x-cfc: Natural Hindi male
                            if (name.contains("male") && !name.contains("female")) score += 140
                            if (country.equals("IN", ignoreCase = true)) score += 100
                        } else {
                            if (country.equals(locale.country, ignoreCase = true)) score += 150
                            if (name.contains("male") && !name.contains("female")) score += 100
                        }

                        // Disqualify / heavily penalize female voices for the JARVIS male persona
                        if (name.contains("female") || name.contains("woman") || name.contains("girl") || name.contains("f00") || name.contains("zita")) {
                            score -= 1000
                        }

                        // Neural / Natural network synthesis quality
                        if (name.contains("neural") || name.contains("natural")) score += 80
                        if (name.contains("network")) score += 40
                        if (voice.quality == Voice.QUALITY_VERY_HIGH || voice.quality == Voice.QUALITY_HIGH) score += 30
                        if (!voice.isNetworkConnectionRequired) score += 15 // Available offline bonus
                        if (voice.latency == Voice.LATENCY_VERY_LOW || voice.latency == Voice.LATENCY_LOW) score += 10

                        score
                    }

                    if (bestCandidate != null) {
                        engine.voice = bestCandidate
                        Log.i(TAG, "Assigned Natural JARVIS Voice: ${bestCandidate.name} for ${locale.displayName}")
                        return bestCandidate.name
                    }
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

        val currentSettings = _settings.value

        // Try Neural Cloud JARVIS Voice (Fish Audio) first if enabled & configured
        if (currentSettings.useNeuralCloudVoice && currentSettings.fishAudioApiKey.isNotBlank()) {
            audioManager.requestAssistantFocus()
            unmuteForVoicePlayback()
            _isSpeaking.value = true
            _lastSpokenText.value = text

            fishAudioService.speak(
                text = text,
                apiKey = currentSettings.fishAudioApiKey,
                voiceId = currentSettings.fishAudioVoiceId,
                onSpeechStarted = {
                    _isSpeaking.value = true
                    onSpeechStarted?.invoke()
                },
                onSpeechFinished = {
                    _isSpeaking.value = false
                    audioManager.releaseAssistantFocus()
                    onSpeechFinished?.invoke()
                },
                onFallbackRequired = {
                    Log.i(TAG, "Fish Audio offline or timed out: falling back to on-device Android TTS seamlessly.")
                    speakWithAndroidTts(text, targetLanguage, onSpeechStarted, onSpeechFinished)
                }
            )
            return
        }

        speakWithAndroidTts(text, targetLanguage, onSpeechStarted, onSpeechFinished)
    }

    private fun speakWithAndroidTts(
        text: String,
        targetLanguage: VoiceLanguage?,
        onSpeechStarted: (() -> Unit)?,
        onSpeechFinished: (() -> Unit)?
    ) {
        val engine = tts
        if (!isReady || engine == null) {
            Log.w(TAG, "TTS not ready yet: $text")
            onSpeechFinished?.invoke()
            return
        }

        val detectedLang = LanguageDetector.detectLanguage(text)
        val langToUse = when {
            targetLanguage != null && targetLanguage != VoiceLanguage.AUTO -> targetLanguage
            detectedLang == VoiceLanguage.HINDI || detectedLang == VoiceLanguage.BENGALI -> detectedLang
            _settings.value.outputLanguage != VoiceLanguage.AUTO -> _settings.value.outputLanguage
            else -> VoiceLanguage.ENGLISH
        }

        val localeToSet = when (langToUse) {
            VoiceLanguage.HINGLISH -> Locale("hi", "IN") // hi-IN TTS engine pronounces English words inside Hindi sentences naturally
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.BENGALI -> Locale("bn", "IN")
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> Locale.UK // Authentic British JARVIS
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

        Log.i(TAG, "Speaking aloud via Android TTS: '$textToSpeak'")
        val result = engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TextToSpeech.speak failed with code: $result")
            audioManager.releaseAssistantFocus()
            _isSpeaking.value = false
            pendingCallbacks.remove(utteranceId)?.second?.invoke()
        }
    }

    override fun stop() {
        fishAudioService.stop()
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

    fun updateFishAudioKey(key: String) {
        _settings.value = _settings.value.copy(fishAudioApiKey = key.trim())
        _settings.value.save(context)
    }

    fun updateFishAudioVoiceId(voiceId: String) {
        _settings.value = _settings.value.copy(fishAudioVoiceId = voiceId.trim())
        _settings.value.save(context)
    }

    fun updateUseNeuralCloudVoice(use: Boolean) {
        _settings.value = _settings.value.copy(useNeuralCloudVoice = use)
        _settings.value.save(context)
    }

    override fun shutdown() {
        stop()
        fishAudioService.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
