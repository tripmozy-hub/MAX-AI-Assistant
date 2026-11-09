package com.max.assistant.dictionary

import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.ai.BrainDecision
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasterCommandDictionaryTest {

    private lateinit var recognizer: MultilingualIntentRecognizer
    private lateinit var contextManager: ConversationContextManager
    private lateinit var brain: MaxAiBrain

    @Before
    fun setUp() {
        contextManager = ConversationContextManager.instance
        contextManager.clearContext()
        recognizer = MultilingualIntentRecognizer(contextManager)
        brain = MaxAiBrain(intentRecognizer = recognizer)
    }

    @Test
    fun testCategory1_GeneralAi() {
        // EN
        val r1 = recognizer.recognize("Hey MAX", VoiceLanguage.ENGLISH)
        assertEquals("AI001", r1.intentId)

        // HI
        val r2 = recognizer.recognize("Kaise ho MAX?", VoiceLanguage.HINDI)
        assertEquals("AI002", r2.intentId)

        // BN
        val r3 = recognizer.recognize("মজার কিছু বলো", VoiceLanguage.BENGALI)
        assertEquals("AI003", r3.intentId)

        val r4 = recognizer.recognize("Tum kya kya kar sakte ho?", VoiceLanguage.HINGLISH)
        assertEquals("AI004", r4.intentId)

        val r5 = recognizer.recognize("আমাকে সাহায্য করো", VoiceLanguage.BENGALI)
        assertEquals("AI005", r5.intentId)

        val r6 = recognizer.recognize("Aasan bhasha mein samjhao", VoiceLanguage.HINDI)
        assertEquals("AI006", r6.intentId)
    }

    @Test
    fun testCategory2_IndianGeneralKnowledge() {
        // GK001 - President
        val r1 = recognizer.recognize("Who is the President of India?", VoiceLanguage.ENGLISH)
        assertEquals("GK001", r1.intentId)
        val r1Hi = recognizer.recognize("Bharat ke Rashtrapati kaun hain?", VoiceLanguage.HINDI)
        assertEquals("GK001", r1Hi.intentId)
        val r1Bn = recognizer.recognize("ভারতের রাষ্ট্রপতি কে?", VoiceLanguage.BENGALI)
        assertEquals("GK001", r1Bn.intentId)

        // GK002 - Capital
        val r2 = recognizer.recognize("Bharat ki rajdhani kya hai?", VoiceLanguage.HINDI)
        assertEquals("GK002", r2.intentId)

        // GK003 - States
        val r3 = recognizer.recognize("How many states are in India?", VoiceLanguage.ENGLISH)
        assertEquals("GK003", r3.intentId)

        // GK004 - Independence
        val r4 = recognizer.recognize("ভারত কবে স্বাধীন হয়?", VoiceLanguage.BENGALI)
        assertEquals("GK004", r4.intentId)

        // GK005 - Constitution
        val r5 = recognizer.recognize("Bharatiya Samvidhan ke baare mein batao", VoiceLanguage.HINDI)
        assertEquals("GK005", r5.intentId)

        // GK006 - Current affairs
        val r6 = recognizer.recognize("Aaj ke current affairs batao", VoiceLanguage.HINGLISH)
        assertEquals("GK006", r6.intentId)
    }

    @Test
    fun testCategory3_StudyAndEducation() {
        // EDU001 - Maths
        val r1 = recognizer.recognize("Mujhe maths sikhao", VoiceLanguage.HINDI)
        assertEquals("EDU001", r1.intentId)

        // EDU002 - Photosynthesis
        val r2 = recognizer.recognize("সালোকসংশ্লেষণ বোঝাও", VoiceLanguage.BENGALI)
        assertEquals("EDU002", r2.intentId)

        // EDU003 - History notes
        val r3 = recognizer.recognize("History ke notes banao", VoiceLanguage.HINDI)
        assertEquals("EDU003", r3.intentId)

        // EDU005 - Quiz
        val r5 = recognizer.recognize("Start my study quiz", VoiceLanguage.ENGLISH)
        assertEquals("EDU005", r5.intentId)

        // EDU007 - 20 MCQs
        val r7 = recognizer.recognize("20 MCQ do", VoiceLanguage.HINDI)
        assertEquals("EDU007", r7.intentId)
    }

    @Test
    fun testCategory4_NotesAndDocuments() {
        val r1 = recognizer.recognize("Create a new note", VoiceLanguage.ENGLISH)
        assertEquals("NOTE001", r1.intentId)

        val r3 = recognizer.recognize("Mere notes padho", VoiceLanguage.HINDI)
        assertEquals("NOTE003", r3.intentId)

        val r4 = recognizer.recognize("আমার ইতিহাসের নোট খোঁজো", VoiceLanguage.BENGALI)
        assertEquals("NOTE004", r4.intentId)
    }

    @Test
    fun testCategory5_RemindersAndTasks() {
        val r1 = recognizer.recognize("Kal subah 7 baje yaad dilana", VoiceLanguage.HINDI)
        assertEquals("REM001", r1.intentId)

        val r2 = recognizer.recognize("Rahul ko call karna yaad dilana", VoiceLanguage.HINDI)
        assertEquals("REM002", r2.intentId)

        val r3 = recognizer.recognize("Mere reminders kya hain?", VoiceLanguage.HINDI)
        assertEquals("REM003", r3.intentId)

        val r5 = recognizer.recognize("Har Sunday yaad dilana", VoiceLanguage.HINGLISH)
        assertEquals("REM005", r5.intentId)
    }

    @Test
    fun testCategory6_AlarmClockCalendar() {
        val r1 = recognizer.recognize("Subah 6 baje alarm lagao", VoiceLanguage.HINDI)
        assertEquals("CLK001", r1.intentId)

        val r2 = recognizer.recognize("Alarm band karo", VoiceLanguage.HINDI)
        assertEquals("CLK002", r2.intentId)

        val r3 = recognizer.recognize("এখন কটা বাজে?", VoiceLanguage.BENGALI)
        assertEquals("CLK003", r3.intentId)

        val r4 = recognizer.recognize("10 minute ka timer lagao", VoiceLanguage.HINDI)
        assertEquals("CLK004", r4.intentId)
    }

    @Test
    fun testCategory7_SmsAndWhatsApp() {
        val r1 = recognizer.recognize("Rahul ko WhatsApp message bhejo", VoiceLanguage.HINDI)
        assertEquals("MSG001", r1.intentId)
        assertEquals(true, r1.definition?.confirmationRequired)

        val r2 = recognizer.recognize("Papa ko SMS bhejo", VoiceLanguage.HINDI)
        assertEquals("MSG002", r2.intentId)

        val r5 = recognizer.recognize("Mera WhatsApp kholo", VoiceLanguage.HINDI)
        assertEquals("MSG005", r5.intentId)
    }

    @Test
    fun testCategory8_PhoneCallsAndContacts() {
        val r1 = recognizer.recognize("Papa ko call karo", VoiceLanguage.HINDI)
        assertEquals("CALL001", r1.intentId)
        assertEquals(true, r1.definition?.confirmationRequired)

        val r3 = recognizer.recognize("Mere contacts kholo", VoiceLanguage.HINDI)
        assertEquals("CALL003", r3.intentId)

        val r4 = recognizer.recognize("Amit ka number dhundo", VoiceLanguage.HINDI)
        assertEquals("CALL004", r4.intentId)
    }

    @Test
    fun testCategory9_ScreenVision() {
        val r1 = recognizer.recognize("MAX, kya tum ye dekh rahe ho?", VoiceLanguage.HINGLISH)
        assertEquals("VIS001", r1.intentId)

        val r2 = recognizer.recognize("Screen par kya likha hai padho", VoiceLanguage.HINDI)
        assertEquals("VIS002", r2.intentId)

        val r6 = recognizer.recognize("Screen dekhna band karo", VoiceLanguage.HINDI)
        assertEquals("VIS006", r6.intentId)
    }

    @Test
    fun testCategory10_GoogleWebChrome() {
        val r1 = recognizer.recognize("Isko Google par search karo", VoiceLanguage.HINDI)
        assertEquals("WEB001", r1.intentId)

        val r2 = recognizer.recognize("Chrome kholo", VoiceLanguage.HINDI)
        assertEquals("WEB002", r2.intentId)

        val r3 = recognizer.recognize("Sarkari naukri search karo", VoiceLanguage.HINDI)
        assertEquals("WEB003", r3.intentId)
    }

    @Test
    fun testCategory11_PhoneControl() {
        val r1 = recognizer.recognize("YouTube kholo", VoiceLanguage.HINDI)
        assertEquals("PHN001", r1.intentId)

        val r2 = recognizer.recognize("Camera kholo", VoiceLanguage.HINDI)
        assertEquals("PHN002", r2.intentId)

        val r3 = recognizer.recognize("Torch on karo", VoiceLanguage.HINDI)
        assertEquals("PHN003", r3.intentId)

        val r4 = recognizer.recognize("Torch off karo", VoiceLanguage.HINDI)
        assertEquals("PHN004", r4.intentId)

        val r5 = recognizer.recognize("Settings kholo", VoiceLanguage.HINDI)
        assertEquals("PHN005", r5.intentId)

        val r6 = recognizer.recognize("Volume badhao", VoiceLanguage.HINDI)
        assertEquals("PHN006", r6.intentId)
    }

    @Test
    fun testCategory12_TranslationAndLanguage() {
        val r1 = recognizer.recognize("Isko Hindi mein translate karo", VoiceLanguage.HINDI)
        assertEquals("LAN001", r1.intentId)

        val r2 = recognizer.recognize("Isko Bengali mein translate karo", VoiceLanguage.HINDI)
        assertEquals("LAN002", r2.intentId)

        val r3 = recognizer.recognize("English mein baat karo", VoiceLanguage.HINDI)
        assertEquals("LAN003", r3.intentId)

        val r4 = recognizer.recognize("Hindi mein baat karo", VoiceLanguage.HINDI)
        assertEquals("LAN004", r4.intentId)

        val r5 = recognizer.recognize("বাংলায় কথা বলো", VoiceLanguage.BENGALI)
        assertEquals("LAN005", r5.intentId)
    }

    @Test
    fun testCategory13_Calculations() {
        val r1 = recognizer.recognize("1000 ka 25 percent nikalo", VoiceLanguage.HINDI)
        assertEquals("CAL001", r1.intentId)

        val r2 = recognizer.recognize("Rupees ko dollars mein convert karo", VoiceLanguage.HINDI)
        assertEquals("CAL002", r2.intentId)

        val r6 = recognizer.recognize("18 percent GST nikalo", VoiceLanguage.HINDI)
        assertEquals("CAL006", r6.intentId)
    }

    @Test
    fun testCategory14_TravelDaily() {
        val r1 = recognizer.recognize("Darjeeling mein hotel dhundo", VoiceLanguage.HINDI)
        assertEquals("TRV001", r1.intentId)

        val r2 = recognizer.recognize("Kolkata jaane wali train dhundo", VoiceLanguage.HINDI)
        assertEquals("TRV002", r2.intentId)

        val r4 = recognizer.recognize("Aaj mausam kaisa hai?", VoiceLanguage.HINDI)
        assertEquals("TRV004", r4.intentId)

        val r6 = recognizer.recognize("Paas mein restaurant dhundo", VoiceLanguage.HINDI)
        assertEquals("TRV006", r6.intentId)
    }

    @Test
    fun testCategory15_SystemPrivacy() {
        val r1 = recognizer.recognize("MAX, jaag jao", VoiceLanguage.HINDI)
        assertEquals("SYS001", r1.intentId)

        val r2 = recognizer.recognize("MAX, standby ho jao", VoiceLanguage.HINDI)
        assertEquals("SYS002", r2.intentId)

        val r3 = recognizer.recognize("MAX, sab kuch band karo", VoiceLanguage.HINDI)
        assertEquals("SYS003", r3.intentId)

        val r4 = recognizer.recognize("MAX, completely band ho jao", VoiceLanguage.HINDI)
        assertEquals("SYS004", r4.intentId)

        val r5 = recognizer.recognize("MAX, sunna band karo", VoiceLanguage.HINDI)
        assertEquals("SYS005", r5.intentId)

        val r7 = recognizer.recognize("MAX, kya tum active ho?", VoiceLanguage.HINDI)
        assertEquals("SYS007", r7.intentId)
    }

    @Test
    fun testFollowUpContextResolution() {
        // Step 1: User says "Open YouTube"
        val r1 = recognizer.recognize("Open YouTube", VoiceLanguage.ENGLISH)
        assertEquals("PHN001", r1.intentId)
        contextManager.recordAppOpened("youtube")

        // Step 2: User says "Search for physics lectures"
        val r2 = recognizer.recognize("Search for physics lectures", VoiceLanguage.ENGLISH)
        assertEquals("PHN001", r2.intentId)
        assertEquals("youtube", r2.parameters["app"])
        assertEquals("physics lectures", r2.parameters["query"])
    }

    @Test
    fun testBrainIntegrationForMasterDictionary() {
        // Verify that MaxAiBrain resolves dictionary commands to valid BrainDecisions
        val presidentDecision = brain.processQuery("Who is the President of India?")
        assertTrue("President query should speak response: $presidentDecision", presidentDecision is BrainDecision.SpeakResponse)
        val pText = (presidentDecision as BrainDecision.SpeakResponse).text
        assertTrue("Text must mention Murmu: $pText", pText.contains("Murmu") || pText.contains("मुर्मू"))

        val capitalDecision = brain.processQuery("Bharat ki rajdhani kya hai?")
        assertTrue(capitalDecision is BrainDecision.SpeakResponse)
        assertTrue((capitalDecision as BrainDecision.SpeakResponse).text.contains("Delhi") || (capitalDecision as BrainDecision.SpeakResponse).text.contains("दिल्ली"))

        val gstDecision = brain.processQuery("18 percent GST nikalo")
        assertTrue(gstDecision is BrainDecision.SpeakResponse)
        assertTrue((gstDecision as BrainDecision.SpeakResponse).text.contains("180"))
    }
}
