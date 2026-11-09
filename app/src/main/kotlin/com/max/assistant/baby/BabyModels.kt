package com.max.assistant.baby

import com.max.assistant.tts.VoiceLanguage

/**
 * Baby Activity Types supported by MAX Baby Entertainment Mode.
 */
enum class BabyActivityType(val displayName: String) {
    LULLABY("Gentle Lullaby"),
    RHYMES("Nursery Rhymes"),
    ANIMAL_SOUNDS("Animal Sounds & Games"),
    STORY("Animated Story"),
    CARTOON_DANCE("Cartoon Dance"),
    ANIMAL_GAME("Animal Guessing Game")
}

/**
 * Cute, friendly cartoon characters featured in the Baby Mode animations.
 */
enum class BabyCharacter(val displayName: String, val emoji: String, val primaryColorHex: Long) {
    BUNNY("Barnaby Bunny", "🐰", 0xFFFFB5C5),
    TEDDY("Theo Teddy", "🧸", 0xFFFFD180),
    DUCK("Daffy Duckling", "🐥", 0xFFFFF176),
    STAR("Twinkle Star", "⭐", 0xFF80DEEA),
    ELEPHANT("Jumbo Elephant", "🐘", 0xFFB39DDB),
    KITTEN("Mimi Kitten", "🐱", 0xFFA7FFEB)
}

/**
 * Synthesizer acoustic sound themes for soothing baby sounds.
 */
enum class BabySoundTheme {
    LULLABY_CHIMES,
    ANIMAL_WOOF,
    ANIMAL_MEOW,
    ANIMAL_MOO,
    ANIMAL_QUACK,
    BUBBLE_PLAY,
    GENTLE_CHIME,
    NONE
}

/**
 * A curated entertainment item (song, rhyme, story, game).
 */
data class BabyContentItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val activityType: BabyActivityType,
    val character: BabyCharacter,
    val spokenText: String,
    val displayText: String,
    val language: VoiceLanguage,
    val soundTheme: BabySoundTheme = BabySoundTheme.LULLABY_CHIMES,
    val interactiveHint: String = "Tap the character or floating bubbles!"
)

/**
 * Floating bubble model for interactive screen visuals.
 */
data class BabyBubble(
    val id: Int,
    val xRatio: Float, // 0.0f to 1.0f
    val yRatio: Float, // 0.0f to 1.0f
    val sizeDp: Float,
    val colorHex: Long,
    val speed: Float,
    val isPopped: Boolean = false
)

/**
 * Reactive state of the Baby Entertainment Mode.
 */
data class BabyModeState(
    val isActive: Boolean = false,
    val currentActivity: BabyActivityType = BabyActivityType.LULLABY,
    val currentContent: BabyContentItem? = null,
    val isPlayingSound: Boolean = false,
    val safeVolumeActive: Boolean = true,
    val originalSystemVolume: Int = 10,
    val caregiverTip: String? = null,
    val cryingTriggerCount: Int = 0,
    val bubbles: List<BabyBubble> = emptyList(),
    val totalBubblesPopped: Int = 0,
    val friendlyNarration: String = ""
)
