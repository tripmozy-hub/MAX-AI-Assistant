package com.max.assistant.humour

/**
 * 5 Adjustable Humour & Personality Levels for MAX:
 * 1. Professional – Minimal humour, formal and precise.
 * 2. Friendly – Warm, light jokes and polite encouragement.
 * 3. Witty (Default) – Quick clever remarks, intelligent JARVIS-like charm.
 * 4. Sarcastic – Playful teasing, cheeky banter and sarcastic comebacks.
 * 5. Savage – Bold comedy, dramatic roasts, and spicy wit (never cruel or offensive).
 */
enum class HumourLevel(val levelValue: Int, val displayName: String) {
    PROFESSIONAL(1, "Professional"),
    FRIENDLY(2, "Friendly"),
    WITTY(3, "Witty"),
    SARCASTIC(4, "Sarcastic"),
    SAVAGE(5, "Savage");

    fun nextLevel(): HumourLevel {
        return when (this) {
            PROFESSIONAL -> FRIENDLY
            FRIENDLY -> WITTY
            WITTY -> SARCASTIC
            SARCASTIC -> SAVAGE
            SAVAGE -> SAVAGE
        }
    }

    fun previousLevel(): HumourLevel {
        return when (this) {
            SAVAGE -> SARCASTIC
            SARCASTIC -> WITTY
            WITTY -> FRIENDLY
            FRIENDLY -> PROFESSIONAL
            PROFESSIONAL -> PROFESSIONAL
        }
    }

    companion object {
        fun fromValue(value: Int): HumourLevel {
            return entries.firstOrNull { it.levelValue == value } ?: WITTY
        }

        fun fromString(name: String): HumourLevel {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WITTY
        }
    }
}
