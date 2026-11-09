package com.max.assistant.util

import com.max.assistant.tts.VoiceLanguage

object LanguageDetector {

    // Unicode block for Bengali: 0980 - 09FF
    private val BENGALI_REGEX = Regex("[\u0980-\u09FF]")

    // Unicode block for Devanagari (Hindi): 0900 - 097F
    private val DEVANAGARI_REGEX = Regex("[\u0900-\u097F]")

    // Common Hindi transliterated Roman words (Hinglish vocab)
    private val HINDI_ROMAN_KEYWORDS = setOf(
        "kholo", "chalao", "band", "ruk", "jao", "kaise", "kya", "kahan", "kab",
        "kaun", "mera", "meri", "aap", "accha", "achha", "theek", "bolo",
        "namaste", "dhanyawad", "samay", "batao", "tasveer", "madad", "chup",
        "haan", "nahi", "nhi", "bhai", "yaar", "suno", "dekho", "karo", "hai",
        "ho", "tha", "thi", "kar", "mil", "mujhe", "tumhe", "usse", "isko",
        "woh", "yeh", "ye", "vo", "bahut", "thoda", "abhi", "baad", "pehle",
        "gana", "bajao", "sunao", "khana", "paani", "ghar", "bahar", "andar",
        "kal", "aaj", "parso", "subah", "raat", "dopahar", "jaldi", "dhire",
        "bolna", "dekhna", "karna", "sona", "uthna", "khana", "peena",
        "mood", "khrb", "kharab", "accha", "bura", "theek", "bilkul",
        "lg", "raha", "rhi", "chalo", "chalte", "bata", "bataiye",
        "sir", "bhai", "yaar", "dost", "phir", "toh", "lekin", "aur",
        "mujhe", "chahiye", "please", "zaroor", "pakka", "sahi"
    )

    // Common Bengali transliterated Roman words
    private val BENGALI_ROMAN_KEYWORDS = setOf(
        "kemon", "aachen", "bolo", "kothay", "kokhon", "ki", "korcho", "shon",
        "chup", "thamo", "nomoshkar", "dhonnobad", "somoy", "chobi"
    )

    // Hinglish signature: English words mixed with Hindi Romanized words
    // Detect when input has BOTH English structural words AND Hindi Romanized words
    private val ENGLISH_FILLER_WORDS = setOf(
        "open", "play", "call", "send", "find", "show", "set", "stop",
        "start", "get", "go", "make", "help", "tell", "check", "turn",
        "what", "where", "when", "how", "why", "who", "is", "are",
        "can", "do", "the", "a", "an", "my", "your", "this", "that",
        "it", "and", "or", "in", "on", "at", "for", "to", "of"
    )

    fun detectLanguage(text: String): VoiceLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return VoiceLanguage.HINGLISH

        // 1. Script checks — pure Bengali/Devanagari text
        if (BENGALI_REGEX.containsMatchIn(trimmed)) {
            return VoiceLanguage.BENGALI
        }
        if (DEVANAGARI_REGEX.containsMatchIn(trimmed)) {
            // If Devanagari text also has Latin words, it's a Hindi-English mix → Hinglish
            val hasLatin = Regex("[a-zA-Z]{2,}").containsMatchIn(trimmed)
            return if (hasLatin) VoiceLanguage.HINGLISH else VoiceLanguage.HINDI
        }

        // 2. Romanized keyword checks
        val words = trimmed.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val bengaliHits = words.count { BENGALI_ROMAN_KEYWORDS.contains(it) }
        val hindiHits = words.count { HINDI_ROMAN_KEYWORDS.contains(it) }

        return when {
            bengaliHits > 0 && bengaliHits >= hindiHits -> VoiceLanguage.BENGALI
            // Has Hindi Romanized keywords → Hinglish (natural mix of Hindi + English)
            // This covers "youtube kholo", "kaise ho", "gana chalao" etc.
            hindiHits > 0 -> VoiceLanguage.HINGLISH
            else -> VoiceLanguage.ENGLISH
        }
    }
}
