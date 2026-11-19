package com.max.assistant.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class AssistantState {
    IDLE,
    LISTENING_FOR_WAKE_WORD,
    LISTENING_FOR_COMMAND,
    THINKING,
    PROCESSING,
    SPEAKING,
    EXECUTING_COMMAND,
    COMPLETED,
    FAILED,
    MUTED,
    STOPPED,
    AWAITING_PIN,
    VERIFYING,
    RECOVERING,
    SCREEN_SHARING
}

class MaxSpeechManager(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit,
    private val onWakeWordDetected: () -> Unit,
    private val onPinRequested: () -> Unit,
    var onStandbyTransition: (() -> Unit)? = null
) {
    companion object {
        private const val TAG = "MaxSpeechManager"
        const val INACTIVITY_STANDBY_TIMEOUT_MS = 15000L // 15 seconds standby timeout

        // Reliable multi-word wake phrases (English, Hindi, Bengali) — includes JARVIS & far-field variants
        private val PRIMARY_WAKE_WORDS = listOf(
            // MAX variations
            "hey max", "hello max", "ok max", "okay max", "hi max", "yo max",
            "hey mac", "hello mac", "ok mac", "okay mac", "hi mac",
            "hey mex", "hello mex", "ok mex", "okay mex", "hi mex",
            "hey maks", "hello maks", "ok maks", "okay maks",
            "hey macks", "hello macks", "ok macks",
            "bhai max", "are max", "oye max", "hey mix", "hi mex",
            "हे मैक्स", "हे मेक्स", "सुनो मैक्स", "सुनो मेक्स", "मैक्स सुनो", "मेक्स सुनो",
            "अरे मैक्स", "ओए मैक्स", "भाई मैक्स",
            "হেই ম্যাক্স", "ম্যাক্স শোনো", "হে ম্যাক্স", "মেক্স শোনো", "এই ম্যাক্স", "আরে ম্যাক্স",
            // JARVIS variations (Iron Man vocal identity)
            "hey jarvis", "hello jarvis", "ok jarvis", "okay jarvis", "hi jarvis", "yo jarvis",
            "hey jarves", "hello jarves", "ok jarves", "hey travis", "hello travis",
            "hey service", "ok service", "hey jarv", "hello jarv",
            "हे जार्विस", "सुनो जार्विस", "जार्विस सुनो", "अरे जार्विस", "ओए जार्विस",
            "হেই জার্ভিস", "জার্ভিস শোনো", "হে জার্ভিস"
        )

        // Shorter standalone wake words & common phonetic variants matching "MAX" & "JARVIS" (including far-field microphone distortions)
        private val SHORT_WAKE_WORDS = listOf(
            // MAX & phonetic far-field captures
            "max", "mac", "mex", "maks", "macks", "macs", "mags", "maggs", "mack", "maths", "math", "matts",
            "marx", "marks", "mix", "masks", "mask", "maax", "mexx", "mox",
            "मैक्स", "मेक्स", "मैथ्स", "मैक", "मैक्सो", "मार्क्स", "मिक्स", "मास्क",
            "ম্যাক্স", "মেক্স", "মাক্স", "মার্ক্স",
            // JARVIS & phonetic far-field captures
            "jarvis", "jarves", "travis", "jarv", "garvis", "harvis", "jarvish",
            "जार्विस", "जार्विश", "जारविस", "জার্ভিস"
        )

        const val SECURITY_PIN = "1212"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var isListeningLoopActive = false
    private var isAwaitingCommand = false
    private var isStopped = false
    private var isPausedForSpeaking = false
    private var isPausedForProcessing = false
    var isMuted: Boolean = false
        private set

    // Full-Duplex Barge-in Support (disabled on device speakers to prevent phone speech feedback cutting off MAX)
    var enableBargeIn: Boolean = false
    var onBargeInDetected: (() -> Unit)? = null
    var isAssistantSpeaking: Boolean = false
        private set
    private var assistantSpeechStartTime: Long = 0L
    private var currentAssistantSpokenText: String = ""
    private var isBargeInInterrupted: Boolean = false

    // Preference: allow single-word "MAX" and common phonetic matching variants (enabled by default)
    var allowShortWakeWord: Boolean = true

    // Continuous conversation mode
    var continuousConversationMode: Boolean = true

    private var inputLanguage: VoiceLanguage = VoiceLanguage.AUTO

    // Audio focus tracking
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Session token to prevent duplicate callbacks within the same recognition turn
    private val sessionCounter = AtomicLong(0)
    private var currentSessionId: Long = 0
    private var sessionHandled = false
    private var busyErrorCount = 0
    private var clientErrorCount = 0
    private var awaitingCommandRetryCount = 0

    // 15-second Inactivity Timeout Management
    private var awaitingCommandStartTime: Long = 0L

    var onUnmuteRequested: (() -> Unit)? = null

    private val restartListeningRunnable = Runnable {
        if (isListeningLoopActive && !isPausedForSpeaking && !isPausedForProcessing && !isStopped && !isMuted && _assistantState.value != AssistantState.AWAITING_PIN) {
            startListeningIntent()
        }
    }

    private val standbyTimeoutRunnable = Runnable {
        handleInactivityTimeout()
    }

    // Absolute volume muting for listening beeps and done tones
    private var isMutedForBeep = false
    var muteListeningBeep: Boolean = true
    private var originalSystemVolume = -1

    fun startInactivityTimer() {
        handler.removeCallbacks(standbyTimeoutRunnable)
        awaitingCommandStartTime = System.currentTimeMillis()
        handler.postDelayed(standbyTimeoutRunnable, INACTIVITY_STANDBY_TIMEOUT_MS)
        Log.d(TAG, "15s Inactivity timer started.")
    }

    fun resetInactivityTimer() {
        if (isAwaitingCommand) {
            handler.removeCallbacks(standbyTimeoutRunnable)
            val elapsed = System.currentTimeMillis() - awaitingCommandStartTime
            val remaining = (INACTIVITY_STANDBY_TIMEOUT_MS - elapsed).coerceAtLeast(2000L)
            handler.postDelayed(standbyTimeoutRunnable, remaining)
        }
    }

    fun cancelInactivityTimer() {
        handler.removeCallbacks(standbyTimeoutRunnable)
        awaitingCommandStartTime = 0L
    }

    // Far-Field Auto-Endpointing Engine (Allows long-distance room listening without word clipping)
    private var lastPartialText: String = ""
    private var lastVoiceActivityTimestamp: Long = 0L
    private var isEndpointTriggered = false

    private val autoEndpointRunnable = Runnable {
        if (sessionHandled || isPausedForSpeaking || isPausedForProcessing || isMuted || isStopped) return@Runnable
        val partial = lastPartialText.trim()
        if (partial.length < 2) return@Runnable

        val elapsed = System.currentTimeMillis() - lastVoiceActivityTimestamp
        // For far-field distance listening (3-5m), allow natural breathing and cadence pauses (1500ms-1800ms)
        val requiredSilence = if (isAwaitingCommand) 1500L else 1800L
        if (elapsed >= requiredSilence && !isEndpointTriggered) {
            isEndpointTriggered = true
            Log.i(TAG, "Far-field auto-endpoint triggered after ${elapsed}ms silence for: '$partial'")
            _rmsLevel.value = 0f
            if (_assistantState.value == AssistantState.LISTENING_FOR_COMMAND ||
                (_assistantState.value == AssistantState.LISTENING_FOR_WAKE_WORD && (containsWakeWord(partial) || isLikelyCommand(partial.lowercase(Locale.ROOT))))) {
                _assistantState.value = AssistantState.PROCESSING
            }
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.d(TAG, "autoEndpoint stopListening: ${e.message}")
            }

            // Fast fallback: if onResults is not invoked by the engine within 400ms, finalize with partial
            handler.postDelayed({
                if (!sessionHandled && !isPausedForSpeaking && !isPausedForProcessing && !isMuted && !isStopped && isListeningLoopActive) {
                    Log.i(TAG, "Fallback: Finalizing command from partial results: '$partial'")
                    sessionHandled = true
                    _assistantState.value = AssistantState.PROCESSING
                    pauseListening()
                    handleRecognizedSpeech(partial)
                }
            }, 400L)
        }
    }

    fun scheduleAutoEndpoint(delayMs: Long = 1600L) {
        handler.removeCallbacks(autoEndpointRunnable)
        handler.postDelayed(autoEndpointRunnable, delayMs)
    }

    fun cancelAutoEndpoint() {
        handler.removeCallbacks(autoEndpointRunnable)
        isEndpointTriggered = false
    }

    private fun handleInactivityTimeout() {
        if (!isListeningLoopActive || isStopped) return
        if (isAwaitingCommand) {
            Log.i(TAG, "Inactivity timeout (15s): No command received. Transitioning MAX to STANDBY mode waiting for wake command.")
            isAwaitingCommand = false
            awaitingCommandRetryCount = 0
            cancelInactivityTimer()
            _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
            _liveTranscript.value = ""
            triggerSubtleHaptic()
            onStandbyTransition?.invoke()
            if (!isPausedForSpeaking) {
                restartListening()
            }
        }
    }

    init {
        initializeRecognizer()
    }

    private var useOnDeviceRecognizer = false

    private fun initializeRecognizer() {
        handler.post {
            destroyRecognizerInternal()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition is not available on this device.")
                _assistantState.value = AssistantState.FAILED
                return@post
            }

            try {
                speechRecognizer = if (useOnDeviceRecognizer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    Log.i(TAG, "Initializing On-Device SpeechRecognizer for seamless music & speech coexistence.")
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                } else {
                    Log.i(TAG, "Initializing Standard Multi-lingual SpeechRecognizer for MAX.")
                    SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }
                Log.i(TAG, "Created Multi-lingual SpeechRecognizer for MAX.")
                busyErrorCount = 0
                clientErrorCount = 0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SpeechRecognizer: ${e.message}")
            }
        }
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Exception destroying recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    fun setInputLanguage(lang: VoiceLanguage) {
        inputLanguage = lang
    }

    fun setAssistantState(state: AssistantState) {
        _assistantState.value = state
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        private var listenerSessionId = 0L

        private fun triggerBargeIn(source: String) {
            if (!isAssistantSpeaking || !enableBargeIn) return
            Log.i(TAG, "Barge-in triggered via $source: interrupting speech immediately!")
            isAssistantSpeaking = false
            isBargeInInterrupted = true
            isAwaitingCommand = true
            _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
            startInactivityTimer()
            handler.post {
                onBargeInDetected?.invoke()
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            listenerSessionId = currentSessionId
            lastPartialText = ""
            lastVoiceActivityTimestamp = System.currentTimeMillis()
            isEndpointTriggered = false
            cancelAutoEndpoint()
            if (isStopped) {
                _assistantState.value = AssistantState.STOPPED
            } else if (isMuted) {
                _assistantState.value = AssistantState.MUTED
            } else if (isAwaitingCommand) {
                _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
            } else {
                _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
            }
        }

        override fun onBeginningOfSpeech() {
            lastVoiceActivityTimestamp = System.currentTimeMillis()
            cancelAutoEndpoint()
            if (isAssistantSpeaking && enableBargeIn) {
                val elapsed = System.currentTimeMillis() - assistantSpeechStartTime
                if (elapsed > 400L) {
                    triggerBargeIn("onBeginningOfSpeech (elapsed ${elapsed}ms)")
                }
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (isMuted || isStopped) {
                _rmsLevel.value = 0f
                return
            }
            // High-Sensitivity formula for distant room audio (3-5m pickup):
            // Boost low-energy far-field acoustic waves for visual HUD and activity detection
            val normalized = ((rmsdB + 2f) / 10f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
            if (isAssistantSpeaking && enableBargeIn) {
                val elapsed = System.currentTimeMillis() - assistantSpeechStartTime
                if (elapsed > 400L && normalized > 0.40f) {
                    triggerBargeIn("onRmsChanged (normalized $normalized)")
                }
            } else if (!isPausedForSpeaking && !isMuted) {
                // Lower detection threshold to 0.05f so far-field distance speech (2-5m) registers as active voice energy
                if (normalized > 0.05f || rmsdB > -2.0f) {
                    lastVoiceActivityTimestamp = System.currentTimeMillis()
                    if (isAwaitingCommand) {
                        resetInactivityTimer()
                    }
                    if (lastPartialText.isNotBlank() && !isEndpointTriggered) {
                        scheduleAutoEndpoint(1600L)
                    }
                }
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _rmsLevel.value = 0f
            cancelAutoEndpoint()
            val partial = lastPartialText.trim()
            if (_assistantState.value == AssistantState.LISTENING_FOR_COMMAND ||
                (_assistantState.value == AssistantState.LISTENING_FOR_WAKE_WORD && (containsWakeWord(partial) || isLikelyCommand(partial.lowercase(Locale.ROOT))))) {
                _assistantState.value = AssistantState.PROCESSING
            }
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.d(TAG, "stopListening on onEndOfSpeech: ${e.message}")
            }

            // Far-field speech fallback: give engine 800ms to deliver full results before finalizing with partial
            if (partial.length >= 2 && !sessionHandled) {
                handler.postDelayed({
                    if (!sessionHandled && !isPausedForSpeaking && !isPausedForProcessing && !isMuted && !isStopped && isListeningLoopActive) {
                        Log.i(TAG, "onEndOfSpeech fallback: Finalizing command from partial: '$partial'")
                        sessionHandled = true
                        _assistantState.value = AssistantState.PROCESSING
                        pauseListening()
                        handleRecognizedSpeech(partial)
                    }
                }, 800L)
            }
        }

        override fun onError(error: Int) {
            _rmsLevel.value = 0f
            cancelAutoEndpoint()

            if (isAssistantSpeaking && enableBargeIn) {
                Log.d(TAG, "SpeechRecognizer onError during TTS ($error). Maintaining barge-in listener.")
                if (isListeningLoopActive && !isStopped && !isMuted) {
                    handler.postDelayed({
                        if (isListeningLoopActive && isAssistantSpeaking && !isMuted) {
                            restartListening()
                        }
                    }, 250L)
                }
                return
            }

            if (isPausedForSpeaking || isPausedForProcessing || isMuted || isStopped || _assistantState.value == AssistantState.AWAITING_PIN) {
                return
            }

            // Log error with descriptive name
            val errorName = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                else -> "ERROR_$error"
            }
            Log.d(TAG, "SpeechRecognizer onError: $errorName (code $error)")

            // Handle permission denial
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _assistantState.value = AssistantState.FAILED
                isListeningLoopActive = false
                Log.e(TAG, "Microphone permission denied for MAX.")
                return
            }

            // Handle speech timeout or no-match: if user already spoke partial text, finalize it immediately!
            if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                val partial = lastPartialText.trim()
                if (partial.length >= 2 && !sessionHandled) {
                    Log.i(TAG, "SpeechRecognizer error ($errorName) but captured partial exists: '$partial'. Finalizing command!")
                    sessionHandled = true
                    _assistantState.value = AssistantState.PROCESSING
                    pauseListening()
                    handleRecognizedSpeech(partial)
                    return
                }

                if (isAwaitingCommand) {
                    val elapsed = System.currentTimeMillis() - awaitingCommandStartTime
                    if (elapsed >= INACTIVITY_STANDBY_TIMEOUT_MS) {
                        cancelInactivityTimer()
                        handleInactivityTimeout()
                        return
                    } else {
                        Log.d(TAG, "Awaiting command: ${elapsed / 1000}s of 15s elapsed. Continuing listening loop.")
                    }
                }
            }

            // Recover from busy error by recreating if stuck
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                busyErrorCount++
                if (busyErrorCount >= 2) {
                    Log.w(TAG, "Recognizer stuck busy, rebuilding instance cleanly...")
                    busyErrorCount = 0
                    initializeRecognizer()
                    handler.postDelayed({
                        if (isListeningLoopActive && !isPausedForSpeaking && !isMuted) {
                            restartListening()
                        }
                    }, 600L)
                    return
                }
            } else {
                busyErrorCount = 0
            }

            // Recover from client error loop (code 5) by cleanly recreating recognizer
            if (error == SpeechRecognizer.ERROR_CLIENT) {
                clientErrorCount++
                if (clientErrorCount >= 2) {
                    Log.w(TAG, "Recognizer client error loop detected ($clientErrorCount), rebuilding instance cleanly...")
                    clientErrorCount = 0
                    initializeRecognizer()
                    handler.postDelayed({
                        if (isListeningLoopActive && !isPausedForSpeaking && !isMuted) {
                            restartListening()
                        }
                    }, 800L)
                    return
                }
            } else {
                clientErrorCount = 0
            }

            if (isListeningLoopActive && !isStopped && !isMuted) {
                // If background music is playing, do not rapid-fire restart in 50ms to prevent audio focus thrashing
                val isMusicPlaying = audioManager?.isMusicActive == true
                val delay = if (isMusicPlaying) {
                    600L
                } else {
                    when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 200L
                        SpeechRecognizer.ERROR_CLIENT -> 200L
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> 50L
                        else -> 200L
                    }
                }
                handler.postDelayed({
                    if (isListeningLoopActive && !isPausedForSpeaking && !isMuted && !isStopped) {
                        restartListening()
                    }
                }, delay)
            }
        }

        override fun onResults(results: Bundle?) {
            _rmsLevel.value = 0f
            cancelAutoEndpoint()
            if (isAssistantSpeaking && enableBargeIn) {
                triggerBargeIn("onResults")
            }
            if (isPausedForSpeaking && !isBargeInInterrupted) return

            // Guard against duplicate callback for the same utterance
            if (sessionHandled || listenerSessionId != currentSessionId || isMuted) {
                Log.d(TAG, "Ignoring duplicate or muted onResults callback.")
                return
            }

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val candidate: String? = if (!matches.isNullOrEmpty()) {
                if (isAwaitingCommand) {
                    matches.firstOrNull { isLikelyCommand(it.lowercase(Locale.ROOT)) } ?: matches[0]
                } else {
                    val wakeCandidates = matches.filter { containsWakeWord(it) }
                    if (wakeCandidates.isNotEmpty()) {
                        wakeCandidates.maxByOrNull { extractCommandAfterWakeWord(it).length } ?: wakeCandidates[0]
                    } else {
                        matches[0]
                    }
                }
            } else if (lastPartialText.isNotBlank()) {
                lastPartialText
            } else null

            if (!candidate.isNullOrBlank()) {
                val recognizedText = candidate.trim()
                Log.i(TAG, "Speech Recognized: '$recognizedText' (from ${matches?.size ?: 0} candidates: $matches, partial: '$lastPartialText')")
                _liveTranscript.value = recognizedText

                sessionHandled = true
                handleRecognizedSpeech(recognizedText)
            } else {
                if (isListeningLoopActive && !isPausedForSpeaking && !isMuted) {
                    restartListening()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches.isNullOrEmpty()) return
            val partial = matches[0].trim()
            if (partial.isBlank()) return

            if (isAssistantSpeaking && enableBargeIn) {
                val elapsed = System.currentTimeMillis() - assistantSpeechStartTime
                if (elapsed > 300L) {
                    triggerBargeIn("onPartialResults: '$partial'")
                }
            }

            if (isPausedForSpeaking || sessionHandled || isMuted) return

            lastPartialText = partial
            lastVoiceActivityTimestamp = System.currentTimeMillis()
            _liveTranscript.value = partial

            if (isAwaitingCommand) {
                resetInactivityTimer()
                scheduleAutoEndpoint(1600L)
            } else if (containsWakeWord(partial) || isLikelyCommand(partial.lowercase(Locale.ROOT))) {
                scheduleAutoEndpoint(1800L)
            }

            // Only emergency stop commands are intercepted in partials to avoid cutting off longer sentences
            val lower = partial.lowercase(Locale.ROOT)
            val isUrgentStop = lower == "stop" || lower == "stop max" || lower == "ruk jao" || lower == "ruko" || lower == "thamo"
            if (isUrgentStop) {
                Log.i(TAG, "Urgent stop command captured in partial: '$partial'")
                cancelInactivityTimer()
                cancelAutoEndpoint()
                sessionHandled = true
                pauseListening()
                stopMax()
                onCommandReceived("STOP_MAX")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Checks if the given text contains any configured wake phrase.
     */
    fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()

        // 1. Primary multi-word wake phrases (always checked)
        for (phrase in PRIMARY_WAKE_WORDS) {
            if (lower.contains(phrase)) return true
        }

        // 2. Short single-word wake phrases & phonetic variants matching "MAX"
        if (allowShortWakeWord) {
            for (shortWord in SHORT_WAKE_WORDS) {
                val wordRegex = Regex("(^|\\s|,|:|-|\\.)$shortWord(\\s|,|:|-|\\.|\$)", RegexOption.IGNORE_CASE)
                if (wordRegex.containsMatchIn(lower)) return true
            }
        }

        return false
    }

    /**
     * Extracts the command portion following the wake word.
     * Returns empty string if only the wake word was spoken.
     */
    fun extractCommandAfterWakeWord(text: String): String {
        val lower = text.lowercase(Locale.ROOT).trim()

        // 1. Check primary phrases first (e.g. "hey max", "hello max", "हे मैक्स", etc.)
        for (ww in PRIMARY_WAKE_WORDS) {
            val idx = lower.indexOf(ww)
            if (idx >= 0) {
                val clean = (text.substring(0, idx) + " " + text.substring(idx + ww.length))
                    .trim('\'', '"', '?', '!', '.', ',', ' ', ':', ';', '-')
                if (clean.isNotBlank()) return normalizeWakeWordsInCommand(clean)
            }
        }

        // 2. Check short wake words & common phonetic variants matching "MAX"
        // Uses unicode delimiter regex instead of ASCII-only \b so Hindi/Bengali scripts work reliably
        if (allowShortWakeWord) {
            for (sw in SHORT_WAKE_WORDS) {
                val pattern = Regex("(?:^|[\\s,:;.-])${Regex.escape(sw)}(?:[\\s,:;.-]|$)", RegexOption.IGNORE_CASE)
                val match = pattern.find(lower)
                if (match != null) {
                    val wordIndex = lower.indexOf(sw, match.range.first)
                    if (wordIndex >= 0) {
                        val clean = (text.substring(0, wordIndex) + " " + text.substring(wordIndex + sw.length))
                            .trim('\'', '"', '?', '!', '.', ',', ' ', ':', ';', '-')
                        if (clean.isNotBlank()) return normalizeWakeWordsInCommand(clean)
                    }
                }
            }
        }

        return ""
    }

    private fun normalizeWakeWordsInCommand(command: String): String {
        return command.replace(Regex("(?i)(^|\\s)(mac|mex|maks|macks|macs|मेक्स)($|\\s)"), "$1MAX$2").trim()
    }

    private fun onWakeWordHeard() {
        if (isStopped) {
            _assistantState.value = AssistantState.AWAITING_PIN
            pauseListening()
            onPinRequested()
        } else {
            triggerWakeWord()
        }
    }

    private fun handleRecognizedSpeech(text: String) {
        val lower = text.lowercase(Locale.ROOT).trim()

        if (isStopped) {
            return
        }

        if (isMuted) {
            val isUnmuteCommand = lower == "unmute" || lower == "unmute max" || lower == "awaz chalu" || lower == "awaz chalu karo" ||
                lower == "आवाज़ चालू" || lower == "आवाज़ चालू करो" || lower == "अनम्यूट" || lower == "अनम्यूट करो" || lower == "কথা বলো" ||
                lower.contains("unmute") || lower.contains("awaz chalu") || lower.contains("आवाज़ चालू")
            if (isUnmuteCommand || containsWakeWord(text)) {
                val extracted = extractCommandAfterWakeWord(text)
                if (isUnmuteCommand || extracted.contains("unmute") || extracted.contains("awaz chalu")) {
                    Log.i(TAG, "Unmute command recognized while muted: '$text'")
                    onUnmuteRequested?.invoke() ?: onCommandReceived("unmute")
                    return
                }
            }
            Log.d(TAG, "Ignored speech while muted: '$text'")
            return
        }

        // Direct stop check
        val isExplicitStop = lower == "stop" || lower == "stop max" || lower == "ruk jao" || lower == "रुक जाओ" ||
            lower == "ruko" || lower == "रुको" || lower == "band ho jao" || lower == "बंद हो जाओ" || lower == "thamo" || lower == "থামো"
        if (isExplicitStop) {
            stopMax()
            onCommandReceived("STOP_MAX")
            return
        }

        // Direct standby / mute check even without explicit wake-word prefix
        val isExplicitStandby = lower == "standby" || lower == "stand by" || lower == "go to standby" ||
            lower == "standby mode" || lower == "max standby" || lower == "hey max standby" ||
            lower == "so jao" || lower == "sojao" || lower == "सो जाओ" ||
            lower == "স্ট্যান্ডবাই" || lower == "sleep mode"
        if (isExplicitStandby) {
            cancelInactivityTimer()
            _assistantState.value = AssistantState.PROCESSING
            pauseListening()
            onCommandReceived("standby")
            return
        }

        val isExplicitMute = lower == "mute" || lower == "mute max" || lower == "chup ho jao" || lower == "chup" ||
            lower == "म्यूट" || lower == "म्यूट करो" || lower == "চুপ" || lower == "নীরব"
        if (isExplicitMute) {
            cancelInactivityTimer()
            _assistantState.value = AssistantState.PROCESSING
            pauseListening()
            onCommandReceived("mute")
            return
        }

        if (isAwaitingCommand) {
            // In command mode: process full recognized utterance as command
            cancelInactivityTimer()
            isAwaitingCommand = false
            awaitingCommandRetryCount = 0
            _assistantState.value = AssistantState.PROCESSING
            pauseListening()
            onCommandReceived(text)
        } else {
            // In wake-word standby mode
            if (containsWakeWord(text)) {
                cancelInactivityTimer()
                val extractedCommand = extractCommandAfterWakeWord(text)
                if (extractedCommand.isNotEmpty()) {
                    Log.i(TAG, "Direct command with wake word: '$extractedCommand'")
                    _assistantState.value = AssistantState.PROCESSING
                    pauseListening()
                    onCommandReceived(extractedCommand)
                } else {
                    // Only wake word spoken ("Hey MAX", "मैक्स"): acknowledge and await next command for 15s
                    triggerWakeWord()
                }
            } else if (continuousConversationMode && (isLikelyCommand(lower) || isValidUtterance(text))) {
                // In continuous conversation mode: accept recognized commands and questions even without explicit wake-word prefix
                Log.i(TAG, "Continuous conversation mode: accepting command without wake word: '$text'")
                cancelInactivityTimer()
                _assistantState.value = AssistantState.PROCESSING
                pauseListening()
                onCommandReceived(text)
            } else {
                // In Standby waiting for wake command: ignore background speech
                Log.d(TAG, "Standby mode: waiting for wake command, ignoring: '$text'")
                if (isListeningLoopActive && !isPausedForSpeaking && !isMuted) {
                    restartListening()
                }
            }
        }
    }

    private fun isValidUtterance(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 3) return false
        val lower = trimmed.lowercase(Locale.ROOT)
        val fillerNoises = setOf("uh", "um", "ah", "er", "mm", "hmm", "haan", "hmmm", "oh", "ha", "acha", "theek")
        if (fillerNoises.contains(lower)) return false
        return lower.contains(" ") || isLikelyCommand(lower)
    }

    private fun isLikelyCommand(lower: String): Boolean {
        // 1. Calling & communication keywords
        if (lower.contains("call") || lower.contains("phone") || lower.contains("dial") ||
            lower.contains("कॉल") || lower.contains("फोन") || lower.contains("डायल") ||
            lower.contains("কল") || lower.contains("ফোন") ||
            lower.contains("baat krwao") || lower.contains("baat karwao") || lower.contains("बात करवाओ") ||
            lower.contains("बात कराओ") || lower.contains("কথা বলিয়ে")
        ) return true

        // 2. Questions & Knowledge queries
        if (lower.startsWith("what") || lower.startsWith("who") || lower.startsWith("where") ||
            lower.startsWith("when") || lower.startsWith("how") || lower.startsWith("why") ||
            lower.contains("kya hai") || lower.contains("kaun hai") || lower.contains("kaha hai") ||
            lower.contains("क्या है") || lower.contains("कौन है") || lower.contains("कहाँ है") ||
            lower.contains("কি") || lower.contains("কে") || lower.contains("কোথায়")
        ) return true

        // 3. Status queries: battery, weather, time, network
        if (lower.contains("battery") || lower.contains("बैटरी") || lower.contains("weather") ||
            lower.contains("mausam") || lower.contains("मौसम") || lower.contains("time") ||
            lower.contains("samay") || lower.contains("समय") || lower.contains("wifi") ||
            lower.contains("internet") || lower.contains("इंटरनेट") || lower.contains("network")
        ) return true

        // 4. App opening, gallery & controls
        if (lower.contains("open ") || lower.contains("kholo") || lower.contains("खोलो") || lower.contains("खोल") ||
            lower.contains("launch ") || lower.contains("chalu karo") || lower.contains("चालू करो") ||
            lower.contains("gallery") || lower.contains("गैलरी") || lower.contains("फोटो") || lower.contains("photo") ||
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("alarm") ||
            lower.contains("अलार्म") || lower.contains("camera") || lower.contains("कैमरा") ||
            lower.contains("batao") || lower.contains("बताओ") || lower.contains("kya tum") || lower.contains("क्या तुम")
        ) return true

        // 5. Screen vision, app control & UI automation queries
        if (lower.contains("screen") || lower.contains("vision") || lower.contains("dekh") ||
            lower.contains("kam kar") || lower.contains("kaam kar") || lower.contains("click") ||
            lower.contains("scroll") || lower.contains("tap") || lower.contains("dabao") ||
            lower.contains("back") || lower.contains("peeche") || lower.contains("home")
        ) return true

        return false
    }

    private fun triggerWakeWord() {
        isAwaitingCommand = true
        awaitingCommandRetryCount = 0
        _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
        startInactivityTimer()
        onWakeWordDetected()
    }

    fun startListeningLoop() {
        isListeningLoopActive = true
        isAwaitingCommand = false
        cancelInactivityTimer()
        isStopped = false
        isMuted = false
        isPausedForSpeaking = false
        _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
        silencePromptStreams()
        restartListening()
    }

    fun triggerCommandMode() {
        if (isStopped) {
            _assistantState.value = AssistantState.AWAITING_PIN
            onPinRequested()
            return
        }
        if (isMuted) {
            onUnmuteRequested?.invoke()
            return
        }
        triggerSubtleHaptic()
        isListeningLoopActive = true
        isAwaitingCommand = true
        awaitingCommandRetryCount = 0
        isPausedForSpeaking = false
        _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
        startInactivityTimer()
        silencePromptStreams()
        restartListening()
    }

    private fun triggerSubtleHaptic() {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(android.os.VibrationEffect.createOneShot(25, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun setAwaitingCommand(awaiting: Boolean) {
        isAwaitingCommand = awaiting
        awaitingCommandRetryCount = 0
        if (awaiting) {
            _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
            startInactivityTimer()
        } else {
            cancelInactivityTimer()
            _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
        }
    }

    fun notifyAssistantSpeakingStarted(text: String) {
        cancelAutoEndpoint()
        isAssistantSpeaking = true
        isPausedForProcessing = false
        isBargeInInterrupted = false
        assistantSpeechStartTime = System.currentTimeMillis()
        currentAssistantSpokenText = text.lowercase(Locale.ROOT).trim()
        _rmsLevel.value = 0f
        _assistantState.value = AssistantState.SPEAKING

        if (enableBargeIn) {
            isPausedForSpeaking = false
            // Keep recognizer running or restart if needed so user can interrupt MAX!
            if (isListeningLoopActive && !isStopped && _assistantState.value != AssistantState.MUTED && _assistantState.value != AssistantState.AWAITING_PIN) {
                handler.post {
                    try {
                        speechRecognizer?.cancel()
                        startListeningIntent()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to start barge-in recognizer: ${e.message}")
                    }
                }
            }
        } else {
            pauseForSpeaking()
        }
    }

    fun pauseForSpeaking() {
        cancelAutoEndpoint()
        isPausedForSpeaking = true
        isPausedForProcessing = false
        _rmsLevel.value = 0f
        _assistantState.value = AssistantState.SPEAKING
        handler.removeCallbacks(restartListeningRunnable)
        handler.removeCallbacks(standbyTimeoutRunnable)
        handler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Cancel recognizer on speak: ${e.message}")
            }
        }
    }

    fun resumeAfterSpeaking(listenForFollowUp: Boolean = false) {
        isAssistantSpeaking = false
        isPausedForProcessing = false
        if (isBargeInInterrupted) {
            Log.d(TAG, "resumeAfterSpeaking ignored because barge-in is handling user command")
            isBargeInInterrupted = false
            return
        }

        // If stopped, stay strictly stopped and do not resume listening
        if (isStopped) {
            _assistantState.value = AssistantState.STOPPED
            _rmsLevel.value = 0f
            isListeningLoopActive = false
            handler.removeCallbacks(restartListeningRunnable)
            handler.post {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
            }
            return
        }

        if (isMuted) {
            _assistantState.value = AssistantState.MUTED
            _rmsLevel.value = 0f
            handler.removeCallbacks(restartListeningRunnable)
            handler.post {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
            }
            return
        }

        // Enforce acoustic settling delay (500ms) so mic does not pick up trailing TTS sound
        handler.postDelayed({
            if (isStopped) {
                _assistantState.value = AssistantState.STOPPED
                isListeningLoopActive = false
                return@postDelayed
            }
            if (isMuted) {
                _assistantState.value = AssistantState.MUTED
                _rmsLevel.value = 0f
                return@postDelayed
            }
            isPausedForSpeaking = false
            isPausedForProcessing = false
            if (listenForFollowUp && _assistantState.value != AssistantState.AWAITING_PIN) {
                isAwaitingCommand = true
                _assistantState.value = AssistantState.LISTENING_FOR_COMMAND
                startInactivityTimer()
            } else {
                isAwaitingCommand = false
                cancelInactivityTimer()
                _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
            }
            if (isListeningLoopActive && !isStopped && !isMuted && _assistantState.value != AssistantState.AWAITING_PIN) {
                restartListening()
            }
        }, 500L)
    }

    fun resumeListening(asStandby: Boolean = true) {
        isPausedForProcessing = false
        isPausedForSpeaking = false
        if (isStopped) {
            _assistantState.value = AssistantState.STOPPED
            return
        }
        if (isMuted) {
            _assistantState.value = AssistantState.MUTED
            return
        }
        if (asStandby) {
            isAwaitingCommand = false
            cancelInactivityTimer()
            _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
        }
        if (isListeningLoopActive && _assistantState.value != AssistantState.AWAITING_PIN) {
            restartListening()
        }
    }

    private fun restartListening() {
        if (!isListeningLoopActive || isPausedForSpeaking || isPausedForProcessing || isStopped || isMuted || _assistantState.value == AssistantState.AWAITING_PIN) return

        handler.removeCallbacks(restartListeningRunnable)
        handler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Cancel recognizer: ${e.message}")
            }
            handler.postDelayed(restartListeningRunnable, 150L) // Crucial settling window for Android SpeechRecognizer IPC
        }
    }

    fun silencePromptStreams() {
        if (!muteListeningBeep) return
        val am = audioManager ?: (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return
        // Never touch or mute stream volumes if music is actively playing in the background
        if (am.isMusicActive) {
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
            }
            isMutedForBeep = true
        } catch (_: Exception) {}
    }

    fun muteStreamsForRecognition() {
        silencePromptStreams()
    }

    fun restoreAllOriginalStreams() {
        if (!isMutedForBeep) return
        try {
            val am = audioManager ?: (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
            }
            isMutedForBeep = false
        } catch (_: Exception) {}
    }

    private fun startListeningIntent(preferWakeWord: Boolean = true) {
        if (!isListeningLoopActive || isPausedForSpeaking || isPausedForProcessing || isStopped || isMuted || _assistantState.value == AssistantState.AWAITING_PIN) {
            Log.d(TAG, "startListeningIntent skipped: loopActive=$isListeningLoopActive, speaking=$isPausedForSpeaking, proc=$isPausedForProcessing, stopped=$isStopped, muted=$isMuted")
            return
        }

        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        currentSessionId = sessionCounter.incrementAndGet()
        sessionHandled = false

        try {
            // Far-Field microphone enhancement: beamforming & automatic gain control
            audioManager?.setParameters("beamforming=enable;noise_suppression=on;far_field=on;agc=on;voice_recognition=on")
        } catch (_: Exception) {}

        val isMusicPlaying = audioManager?.isMusicActive == true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 15)
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra("android.speech.extra.ENABLE_BIASING_DEVICE_CONTEXT", true)
            putExtra("android.speech.extra.AUDIO_SOURCE", android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION)

            // Music coexistence: do not request exclusive audio focus when background music is active
            if (isMusicPlaying) {
                putExtra("android.speech.extra.REQUEST_AUDIO_FOCUS", false)
                putExtra("REQUEST_AUDIO_FOCUS", false)
                putExtra("android.speech.extra.AUDIO_FOCUS_GAIN_TYPE", AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }

            // Absolute silence extras: suppress vendor & Google prompt tones
            putExtra("android.speech.extra.BEEP", false)
            putExtra("android.speech.extra.SPEECH_INPUT_BEEP_SUPPRESS", true)
            putExtra("android.speech.extra.PROMPT_SOUND", false)
            putExtra("android.speech.extra.SUPPRESS_START_BEEP", true)
            putExtra("android.speech.extra.SUPPRESS_END_BEEP", true)
            putExtra("sound", false)

            // Far-Field conversational endpointing: enables distant voice capture (3-5m) without clipping
            // If music is actively playing, extend timeout window so recognizer doesn't cycle and pause playback
            val completeSilence = if (isMusicPlaying) 8000L else if (isAwaitingCommand) 2400L else 2800L
            val possiblyCompleteSilence = if (isMusicPlaying) 5000L else if (isAwaitingCommand) 1600L else 2000L
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, completeSilence)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, possiblyCompleteSilence)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100L)
            putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 100L)
            putExtra("android.speech.extra.DICTATION_MODE", false)

            // Multi-language recognition
            when (inputLanguage) {
                VoiceLanguage.HINGLISH -> {
                    // Hinglish: use hi-IN as primary + en-IN as additional for mixed speech
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                }
                VoiceLanguage.HINDI -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN"))
                }
                VoiceLanguage.BENGALI -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "bn-IN"))
                }
                VoiceLanguage.ENGLISH -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-GB")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-GB", "en-US", "en-IN", "hi-IN"))
                }
                VoiceLanguage.AUTO -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-GB")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-GB", "hi-IN", "en-IN", "en-US", "bn-IN"))
                }
            }
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}")
            if (e.message?.contains("busy", ignoreCase = true) == true) {
                initializeRecognizer()
            }
        }
    }

    private fun requestAssistantAudioFocus() {
        val am = audioManager ?: return
        if (hasAudioFocus) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> {
                                Log.d(TAG, "AudioFocus loss")
                                hasAudioFocus = false
                            }
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                Log.d(TAG, "AudioFocus regained")
                                hasAudioFocus = true
                            }
                        }
                    }
                    .build()
                val res = am.requestAudioFocus(audioFocusRequest!!)
                hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioFocus request error: ${e.message}")
        }
    }

    private fun releaseAssistantAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            }
            hasAudioFocus = false
        } catch (e: Exception) {
            Log.w(TAG, "AudioFocus release error: ${e.message}")
        }
    }

    fun pauseListening() {
        cancelAutoEndpoint()
        isPausedForProcessing = true
        _rmsLevel.value = 0f
        handler.removeCallbacks(restartListeningRunnable)
        handler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling recognizer: ${e.message}")
            }
        }
    }

    fun setMuted(muted: Boolean) {
        Log.i(TAG, "setMuted: $muted")
        isMuted = muted
        if (muted) {
            cancelAutoEndpoint()
            isAwaitingCommand = false
            isPausedForProcessing = false
            isPausedForSpeaking = false
            cancelInactivityTimer()
            _assistantState.value = AssistantState.MUTED
            _rmsLevel.value = 0f
            _liveTranscript.value = "MAX is Muted"
            handler.removeCallbacks(restartListeningRunnable)
            handler.post {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}
            }
        } else {
            _assistantState.value = if (isStopped) AssistantState.STOPPED else AssistantState.LISTENING_FOR_WAKE_WORD
            _liveTranscript.value = ""
            if (!isStopped && !isPausedForSpeaking) {
                isListeningLoopActive = true
                restartListening()
            }
        }
    }

    fun stopMax() {
        Log.w(TAG, "STOP MAX ACTIVATED")
        isStopped = true
        cancelAutoEndpoint()
        isListeningLoopActive = false
        isAwaitingCommand = false
        cancelInactivityTimer()
        _assistantState.value = AssistantState.STOPPED
        _rmsLevel.value = 0f
        _liveTranscript.value = ""
        handler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling recognizer on stopMax: ${e.message}")
            }
        }
    }

    fun enterStandby() {
        Log.i(TAG, "enterStandby: transitioning to LISTENING_FOR_WAKE_WORD")
        isStopped = false
        isMuted = false
        isAwaitingCommand = false
        cancelInactivityTimer()
        _assistantState.value = AssistantState.LISTENING_FOR_WAKE_WORD
        _rmsLevel.value = 0f
        _liveTranscript.value = ""
        isListeningLoopActive = true
        if (!isPausedForSpeaking) {
            restartListening()
        }
    }

    fun verifyPinAndUnlock(pin: String): Boolean {
        return if (pin.trim() == SECURITY_PIN) {
            isStopped = false
            _assistantState.value = AssistantState.IDLE
            _liveTranscript.value = ""
            startListeningLoop()
            true
        } else {
            false
        }
    }

    fun cancelPinEntry() {
        if (isStopped) {
            _assistantState.value = AssistantState.STOPPED
            startListeningLoop()
        }
    }

    fun destroy() {
        isListeningLoopActive = false
        isPausedForSpeaking = false
        restoreAllOriginalStreams()
        releaseAssistantAudioFocus()
        handler.post {
            destroyRecognizerInternal()
        }
    }
}
