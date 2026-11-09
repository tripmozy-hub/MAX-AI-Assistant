package com.max.assistant.baby

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.max.assistant.tts.IVoiceEngine
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * BabyEntertainmentManager coordinates all activities in MAX Baby Entertainment Mode:
 * Safe volume control, acoustic synthesis, gentle voice narration, interactive floating bubbles,
 * trilingual lullabies/rhymes/stories, and caregiver safety guidance.
 */
class BabyEntertainmentManager(
    private val context: Context,
    private val voiceEngine: IVoiceEngine? = null,
    val synthesizer: BabyAudioSynthesizer = BabyAudioSynthesizer()
) {

    companion object {
        private const val TAG = "BabyEntertainmentMgr"
        private const val SAFE_VOLUME_PERCENTAGE = 0.35f // 35% safe ceiling for baby ears
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(BabyModeState())
    val state: StateFlow<BabyModeState> = _state.asStateFlow()

    private var currentLullabyIndex = 0
    private var currentRhymeIndex = 0
    private var currentAnimalIndex = 0
    private var currentStoryIndex = 0

    init {
        // Initialize friendly floating pastel bubbles
        initBubbles()
    }

    private fun initBubbles() {
        val bubbleColors = listOf(
            0x66FFB5C5L, // Soft Pastel Pink
            0x6680DEEAL, // Soft Pastel Cyan
            0x66FFF176L, // Soft Pastel Yellow
            0x66B39DDBL, // Soft Pastel Purple
            0x66A7FFEBL, // Soft Pastel Mint
            0x66FFD180L  // Soft Pastel Peach
        )
        val initialBubbles = (0 until 8).map { id ->
            BabyBubble(
                id = id,
                xRatio = 0.1f + Random.nextFloat() * 0.8f,
                yRatio = 0.2f + Random.nextFloat() * 0.7f,
                sizeDp = 44f + Random.nextFloat() * 32f,
                colorHex = bubbleColors[id % bubbleColors.size],
                speed = 0.8f + Random.nextFloat() * 0.6f,
                isPopped = false
            )
        }
        _state.value = _state.value.copy(bubbles = initialBubbles)
    }

    /**
     * Start Baby Entertainment Mode safely.
     */
    fun startBabyMode(triggerReason: String = "general", language: VoiceLanguage = VoiceLanguage.HINDI) {
        Log.i(TAG, "Starting Baby Entertainment Mode (reason=$triggerReason, lang=${language.name})")

        // 1. Enforce Safe Volume Ceiling
        val origVol = enforceSafeVolume()

        // 2. Track Crying Triggers
        val isCrying = triggerReason.contains("crying") || triggerReason.contains("ro raha") || triggerReason.contains("কাঁদছে")
        val updatedCryingCount = if (isCrying) _state.value.cryingTriggerCount + 1 else _state.value.cryingTriggerCount

        val caregiverTip = if (updatedCryingCount >= 2) {
            BabyContentRepository.getCaregiverDistressTip(language)
        } else null

        // 3. Choose starting activity (Lullaby for crying/sleep, Animal/Rhymes for general/laugh)
        val initialContent = if (isCrying || triggerReason.contains("sleep") || triggerReason.contains("lori")) {
            BabyContentRepository.getLullabies(language).firstOrNull()
        } else if (triggerReason.contains("laugh") || triggerReason.contains("funny") || triggerReason.contains("hasao")) {
            BabyContentRepository.getAnimalSoundsAndGames(language).firstOrNull()
        } else {
            BabyContentRepository.getRhymes(language).firstOrNull()
        }

        _state.value = _state.value.copy(
            isActive = true,
            currentActivity = initialContent?.activityType ?: BabyActivityType.LULLABY,
            currentContent = initialContent,
            safeVolumeActive = true,
            originalSystemVolume = origVol,
            cryingTriggerCount = updatedCryingCount,
            caregiverTip = caregiverTip,
            isPlayingSound = true,
            friendlyNarration = initialContent?.spokenText ?: ""
        )

        // 4. Play Gentle Sound Theme & Friendly Character Voice
        executeContentPlayback(initialContent, language)
    }

    /**
     * Advance to the next interactive activity.
     */
    fun nextActivity(language: VoiceLanguage) {
        if (!_state.value.isActive) {
            startBabyMode("general", language)
            return
        }

        val nextType = when (_state.value.currentActivity) {
            BabyActivityType.LULLABY -> BabyActivityType.RHYMES
            BabyActivityType.RHYMES -> BabyActivityType.ANIMAL_SOUNDS
            BabyActivityType.ANIMAL_SOUNDS -> BabyActivityType.STORY
            BabyActivityType.STORY -> BabyActivityType.LULLABY
            else -> BabyActivityType.LULLABY
        }

        when (nextType) {
            BabyActivityType.LULLABY -> playLullaby(language)
            BabyActivityType.RHYMES -> playRhymes(language)
            BabyActivityType.ANIMAL_SOUNDS -> playAnimalSounds(language)
            BabyActivityType.STORY -> tellStory(language)
            else -> playLullaby(language)
        }
    }

    /**
     * Play soothing lullaby.
     */
    fun playLullaby(language: VoiceLanguage) {
        val list = BabyContentRepository.getLullabies(language)
        if (list.isEmpty()) return
        currentLullabyIndex = (currentLullabyIndex + 1) % list.size
        val item = list[currentLullabyIndex]

        _state.value = _state.value.copy(
            currentActivity = BabyActivityType.LULLABY,
            currentContent = item,
            isPlayingSound = true,
            friendlyNarration = item.spokenText
        )
        executeContentPlayback(item, language)
    }

    /**
     * Play delightful nursery rhyme.
     */
    fun playRhymes(language: VoiceLanguage) {
        val list = BabyContentRepository.getRhymes(language)
        if (list.isEmpty()) return
        currentRhymeIndex = (currentRhymeIndex + 1) % list.size
        val item = list[currentRhymeIndex]

        _state.value = _state.value.copy(
            currentActivity = BabyActivityType.RHYMES,
            currentContent = item,
            isPlayingSound = true,
            friendlyNarration = item.spokenText
        )
        executeContentPlayback(item, language)
    }

    /**
     * Play cute animal sounds and games.
     */
    fun playAnimalSounds(language: VoiceLanguage) {
        val list = BabyContentRepository.getAnimalSoundsAndGames(language)
        if (list.isEmpty()) return
        currentAnimalIndex = (currentAnimalIndex + 1) % list.size
        val item = list[currentAnimalIndex]

        _state.value = _state.value.copy(
            currentActivity = BabyActivityType.ANIMAL_SOUNDS,
            currentContent = item,
            isPlayingSound = true,
            friendlyNarration = item.spokenText
        )
        executeContentPlayback(item, language)
    }

    /**
     * Tell short animated heartwarming story.
     */
    fun tellStory(language: VoiceLanguage) {
        val list = BabyContentRepository.getStories(language)
        if (list.isEmpty()) return
        currentStoryIndex = (currentStoryIndex + 1) % list.size
        val item = list[currentStoryIndex]

        _state.value = _state.value.copy(
            currentActivity = BabyActivityType.STORY,
            currentContent = item,
            isPlayingSound = true,
            friendlyNarration = item.spokenText
        )
        executeContentPlayback(item, language)
    }

    /**
     * Interactive bubble popping on screen.
     */
    fun popBubble(bubbleId: Int) {
        val currentBubbles = _state.value.bubbles.toMutableList()
        val index = currentBubbles.indexOfFirst { it.id == bubbleId }
        if (index != -1 && !currentBubbles[index].isPopped) {
            val popped = currentBubbles[index].copy(isPopped = true)
            currentBubbles[index] = popped
            val newTotal = _state.value.totalBubblesPopped + 1
            _state.value = _state.value.copy(bubbles = currentBubbles, totalBubblesPopped = newTotal)

            // Play gentle soft pop sound effect
            synthesizer.playBubblePop()

            // Respawn bubble smoothly from bottom after delay
            scope.launch {
                delay(1200)
                val respawned = popped.copy(
                    xRatio = 0.1f + Random.nextFloat() * 0.8f,
                    yRatio = 0.95f,
                    isPopped = false
                )
                val freshList = _state.value.bubbles.toMutableList()
                val targetIdx = freshList.indexOfFirst { it.id == bubbleId }
                if (targetIdx != -1) {
                    freshList[targetIdx] = respawned
                    _state.value = _state.value.copy(bubbles = freshList)
                }
            }
        }
    }

    /**
     * Stop Baby Entertainment Mode immediately, releasing all audio and restoring normal volume.
     */
    @Synchronized
    fun stopBabyMode() {
        Log.i(TAG, "Stopping Baby Entertainment Mode immediately.")
        synthesizer.stopAudio()
        restoreOriginalVolume()
        _state.value = _state.value.copy(
            isActive = false,
            isPlayingSound = false,
            friendlyNarration = ""
        )
    }

    private fun executeContentPlayback(item: BabyContentItem?, language: VoiceLanguage) {
        if (item == null) return

        // 1. Play Acoustic Sound Theme
        when (item.soundTheme) {
            BabySoundTheme.LULLABY_CHIMES -> synthesizer.playLullaby()
            BabySoundTheme.ANIMAL_WOOF,
            BabySoundTheme.ANIMAL_MEOW,
            BabySoundTheme.ANIMAL_MOO,
            BabySoundTheme.ANIMAL_QUACK,
            BabySoundTheme.GENTLE_CHIME -> synthesizer.playAnimalSound(item.soundTheme)
            else -> {}
        }

        // 2. Friendly Voice Narration (Warm, gentle pitch)
        voiceEngine?.speak(
            text = item.spokenText,
            targetLanguage = language
        )
    }

    private fun enforceSafeVolume(): Int {
        val am = audioManager ?: return 10
        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val safeCeiling = (maxVol * SAFE_VOLUME_PERCENTAGE).toInt().coerceAtLeast(1)

        if (currentVol > safeCeiling) {
            try {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, safeCeiling, 0)
                Log.i(TAG, "Adjusted volume to safe baby level: $safeCeiling / $maxVol (was $currentVol)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to adjust safe volume: ${e.message}")
            }
        }
        return currentVol
    }

    private fun restoreOriginalVolume() {
        val am = audioManager ?: return
        val orig = _state.value.originalSystemVolume
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, orig, 0)
            Log.i(TAG, "Restored original system volume: $orig")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore original volume: ${e.message}")
        }
    }
}
