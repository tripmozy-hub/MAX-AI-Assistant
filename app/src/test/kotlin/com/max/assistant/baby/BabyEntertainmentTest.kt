package com.max.assistant.baby

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.dictionary.CommandCategory
import com.max.assistant.dictionary.ConversationContextManager
import com.max.assistant.dictionary.MultilingualIntentRecognizer
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BabyEntertainmentTest {

    private lateinit var recognizer: MultilingualIntentRecognizer
    private lateinit var brain: MaxAiBrain

    @Before
    fun setUp() {
        val contextManager = ConversationContextManager.instance
        contextManager.clearContext()
        recognizer = MultilingualIntentRecognizer(contextManager)
        brain = MaxAiBrain(intentRecognizer = recognizer)
    }

    @Test
    fun testEnglishBabyCommandsRecognition() {
        val r1 = recognizer.recognize("MAX, the baby is crying.", VoiceLanguage.ENGLISH)
        assertEquals("BABY001", r1.intentId)
        assertEquals(CommandCategory.BABY_ENTERTAINMENT, r1.category)

        val r2 = recognizer.recognize("MAX, entertain the baby.", VoiceLanguage.ENGLISH)
        assertEquals("BABY002", r2.intentId)

        val r3 = recognizer.recognize("MAX, distract the baby.", VoiceLanguage.ENGLISH)
        assertEquals("BABY001", r3.intentId)

        val r4 = recognizer.recognize("MAX, make the baby laugh.", VoiceLanguage.ENGLISH)
        assertEquals("BABY003", r4.intentId)

        val r5 = recognizer.recognize("MAX, play something for the baby.", VoiceLanguage.ENGLISH)
        assertEquals("BABY002", r5.intentId)

        val r6 = recognizer.recognize("MAX, tell the baby a story.", VoiceLanguage.ENGLISH)
        assertEquals("BABY005", r6.intentId)

        val r7 = recognizer.recognize("MAX, help the baby sleep.", VoiceLanguage.ENGLISH)
        assertEquals("BABY004", r7.intentId)

        val r8 = recognizer.recognize("MAX, next game.", VoiceLanguage.ENGLISH)
        assertEquals("BABY007", r8.intentId)

        val r9 = recognizer.recognize("MAX, animal sounds.", VoiceLanguage.ENGLISH)
        assertEquals("BABY007", r9.intentId)

        val r10 = recognizer.recognize("MAX, stop baby mode.", VoiceLanguage.ENGLISH)
        assertEquals("BABY008", r10.intentId)
    }

    @Test
    fun testHindiHinglishBabyCommandsRecognition() {
        val r1 = recognizer.recognize("MAX, baccha ro raha hai.", VoiceLanguage.HINDI)
        assertEquals("BABY001", r1.intentId)

        val r2 = recognizer.recognize("MAX, iska dhyan bhatkao.", VoiceLanguage.HINDI)
        assertEquals("BABY001", r2.intentId)

        val r3 = recognizer.recognize("MAX, baby ko entertain karo.", VoiceLanguage.HINGLISH)
        assertEquals("BABY002", r3.intentId)

        val r4 = recognizer.recognize("MAX, bachche ko hansaao.", VoiceLanguage.HINDI)
        assertEquals("BABY003", r4.intentId)

        val r5 = recognizer.recognize("MAX, kuch funny karo.", VoiceLanguage.HINDI)
        assertEquals("BABY003", r5.intentId)

        val r6 = recognizer.recognize("MAX, lori sunao.", VoiceLanguage.HINDI)
        assertEquals("BABY004", r6.intentId)

        val r7 = recognizer.recognize("MAX, baby ko kahani sunao.", VoiceLanguage.HINGLISH)
        assertEquals("BABY005", r7.intentId)
    }

    @Test
    fun testBengaliBabyCommandsRecognition() {
        val r1 = recognizer.recognize("MAX, বাচ্চা কাঁদছে।", VoiceLanguage.BENGALI)
        assertEquals("BABY001", r1.intentId)

        val r2 = recognizer.recognize("MAX, বাচ্চার মনোযোগ অন্যদিকে ঘোরাও।", VoiceLanguage.BENGALI)
        assertEquals("BABY001", r2.intentId)

        val r3 = recognizer.recognize("MAX, বাচ্চাকে আনন্দ দাও।", VoiceLanguage.BENGALI)
        assertEquals("BABY002", r3.intentId)

        val r4 = recognizer.recognize("MAX, বাচ্চাকে হাসাও।", VoiceLanguage.BENGALI)
        assertEquals("BABY003", r4.intentId)

        val r5 = recognizer.recognize("MAX, একটা ছড়া শোনাও।", VoiceLanguage.BENGALI)
        assertEquals("BABY006", r5.intentId)

        val r6 = recognizer.recognize("MAX, একটা গল্প বলো।", VoiceLanguage.BENGALI)
        assertEquals("BABY005", r6.intentId)

        val r7 = recognizer.recognize("MAX, ঘুম পাড়ানোর গান শোনাও।", VoiceLanguage.BENGALI)
        assertEquals("BABY004", r7.intentId)
    }

    @Test
    fun testBrainBabyDecisions() {
        val d1 = brain.processQuery("MAX, the baby is crying.")
        assertTrue("Expected StartBabyMode for crying baby, got: $d1", d1 is BrainDecision.StartBabyMode)
        assertEquals("crying", (d1 as BrainDecision.StartBabyMode).reason)

        val d2 = brain.processQuery("MAX, lori sunao.")
        assertTrue("Expected PlayBabyLullaby, got: $d2", d2 is BrainDecision.PlayBabyLullaby)

        val d3 = brain.processQuery("MAX, একটা গল্প বলো।")
        assertTrue("Expected TellBabyStory, got: $d3", d3 is BrainDecision.TellBabyStory)

        val d4 = brain.processQuery("MAX, stop baby mode.")
        assertTrue("Expected StopBabyMode, got: $d4", d4 is BrainDecision.StopBabyMode)
    }

    @Test
    fun testContentRepositoryTrilingualCompleteness() {
        // Bengali content
        val bnLullabies = BabyContentRepository.getLullabies(VoiceLanguage.BENGALI)
        assertFalse(bnLullabies.isEmpty())
        assertTrue(bnLullabies.any { it.title.contains("খোকা") })

        val bnRhymes = BabyContentRepository.getRhymes(VoiceLanguage.BENGALI)
        assertFalse(bnRhymes.isEmpty())
        assertTrue(bnRhymes.any { it.title.contains("হাট্টিমা") })

        // Hindi content
        val hiLullabies = BabyContentRepository.getLullabies(VoiceLanguage.HINDI)
        assertFalse(hiLullabies.isEmpty())
        assertTrue(hiLullabies.any { it.title.contains("चंदा मामा") })

        val hiRhymes = BabyContentRepository.getRhymes(VoiceLanguage.HINDI)
        assertFalse(hiRhymes.isEmpty())
        assertTrue(hiRhymes.any { it.title.contains("मछली") })

        // English content
        val enLullabies = BabyContentRepository.getLullabies(VoiceLanguage.ENGLISH)
        assertFalse(enLullabies.isEmpty())
        assertTrue(enLullabies.any { it.title.contains("Twinkle") })

        // Caregiver Safety Advice
        val enAdvice = BabyContentRepository.getCaregiverDistressTip(VoiceLanguage.ENGLISH)
        assertTrue(enAdvice.contains("hunger") && enAdvice.contains("diaper") && enAdvice.contains("pediatrician"))

        val hiAdvice = BabyContentRepository.getCaregiverDistressTip(VoiceLanguage.HINDI)
        assertTrue(hiAdvice.contains("भूख") && hiAdvice.contains("डायपर") && hiAdvice.contains("डॉक्टर"))
    }
}
