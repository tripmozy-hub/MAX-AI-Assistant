package com.max.assistant.util

import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun testDetectEnglish() {
        // Pure English with no Hindi/Bengali keywords → ENGLISH
        assertEquals(VoiceLanguage.ENGLISH, LanguageDetector.detectLanguage("System diagnostics"))
        assertEquals(VoiceLanguage.ENGLISH, LanguageDetector.detectLanguage("Play Arijit Singh"))
        assertEquals(VoiceLanguage.ENGLISH, LanguageDetector.detectLanguage("Navigate to Delhi"))
    }

    @Test
    fun testDetectHindiDevanagari() {
        // Pure Devanagari script → HINDI
        assertEquals(VoiceLanguage.HINDI, LanguageDetector.detectLanguage("नमस्ते मैक्स, आप कैसे हैं?"))
        assertEquals(VoiceLanguage.HINDI, LanguageDetector.detectLanguage("यूट्यूब खोलो"))
        assertEquals(VoiceLanguage.HINDI, LanguageDetector.detectLanguage("समय क्या है?"))
    }

    @Test
    fun testDetectHinglishTransliterated() {
        // Romanized Hindi words (with or without English) → HINGLISH
        assertEquals(VoiceLanguage.HINGLISH, LanguageDetector.detectLanguage("youtube kholo"))
        assertEquals(VoiceLanguage.HINGLISH, LanguageDetector.detectLanguage("samay kya hai batao"))
        assertEquals(VoiceLanguage.HINGLISH, LanguageDetector.detectLanguage("kaise ho max"))
        assertEquals(VoiceLanguage.HINGLISH, LanguageDetector.detectLanguage("mood khrb hai"))
        assertEquals(VoiceLanguage.HINGLISH, LanguageDetector.detectLanguage("gana chalao sir"))
    }

    @Test
    fun testDetectBengaliScript() {
        assertEquals(VoiceLanguage.BENGALI, LanguageDetector.detectLanguage("নমস্কার ম্যাক্স, কেমন আছেন?"))
        assertEquals(VoiceLanguage.BENGALI, LanguageDetector.detectLanguage("এখন সময় কত?"))
    }

    @Test
    fun testDetectBengaliTransliterated() {
        assertEquals(VoiceLanguage.BENGALI, LanguageDetector.detectLanguage("kemon aachen max"))
        assertEquals(VoiceLanguage.BENGALI, LanguageDetector.detectLanguage("nomoshkar max"))
    }
}
