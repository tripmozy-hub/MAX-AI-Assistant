package com.max.assistant.dictionary

import com.max.assistant.tts.VoiceLanguage

enum class CommandCategory(val code: String, val displayName: String) {
    GENERAL_AI("CAT1", "General AI Conversation"),
    INDIAN_GK("CAT2", "Indian General Knowledge"),
    EDUCATION("CAT3", "Study and Education"),
    NOTES("CAT4", "Notes and Documents"),
    REMINDERS("CAT5", "Reminders and Tasks"),
    CLOCK_CALENDAR("CAT6", "Alarm, Clock and Calendar"),
    MESSAGING("CAT7", "SMS and WhatsApp"),
    CALLS_CONTACTS("CAT8", "Phone Calls and Contacts"),
    SCREEN_VISION("CAT9", "Screen Vision and Image Understanding"),
    WEB_CHROME("CAT10", "Google, Internet and Chrome"),
    PHONE_CONTROL("CAT11", "Phone and App Control"),
    TRANSLATION("CAT12", "Translation and Language"),
    CALCULATIONS("CAT13", "Calculations and Finance Basics"),
    TRAVEL_DAILY("CAT14", "Travel and Daily Information"),
    SYS_PRIVACY("CAT15", "MAX Personal Control and Privacy"),
    BABY_ENTERTAINMENT("CAT16", "Baby Entertainment Mode")
}

data class CommandDefinition(
    val intentId: String,
    val category: CommandCategory,
    val englishPhrases: List<String>,
    val hindiPhrases: List<String>,
    val bengaliPhrases: List<String>,
    val requiredParameters: List<String> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val confirmationRequired: Boolean = false,
    val description: String = ""
)

data class RecognizedIntent(
    val intentId: String,
    val category: CommandCategory,
    val rawQuery: String,
    val detectedLanguage: VoiceLanguage,
    val parameters: Map<String, String> = emptyMap(),
    val missingParameters: List<String> = emptyList(),
    val definition: CommandDefinition? = null
)
