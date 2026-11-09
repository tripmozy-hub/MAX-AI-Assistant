package com.max.assistant.upgrade

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.planner.TaskExecutionEngine
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedUpgradeTest {

    private val brain = MaxAiBrain()

    // ==================== 1. CONTEXTUAL MEMORY INTENT TESTS ====================

    @Test
    fun testRecallYesterdayDiscussionIntent() {
        val decision = brain.processQuery("MAX, kal humne kya discuss kiya tha?", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be RecallYesterdayDiscussion, was $decision", decision is BrainDecision.RecallYesterdayDiscussion)
    }

    @Test
    fun testRecallProjectTasksIntent() {
        val decision = brain.processQuery("MAX, TripMozy ka pending kaam kya hai?", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be RecallProjectTasks, was $decision", decision is BrainDecision.RecallProjectTasks)
        val proj = (decision as BrainDecision.RecallProjectTasks).projectName
        assertTrue("Project name must contain 'tripmozy': $proj", proj.contains("tripmozy", ignoreCase = true))
    }

    @Test
    fun testSaveAndForgetUserMemoryIntent() {
        val saveDecision = brain.processQuery("MAX, yaad rakho ki client meeting 5 baje hai", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be SaveUserMemory, was $saveDecision", saveDecision is BrainDecision.SaveUserMemory)
        val fact = (saveDecision as BrainDecision.SaveUserMemory).fact
        assertTrue("Fact must capture meeting info: $fact", fact.contains("5 baje") || fact.contains("client meeting"))

        val forgetDecision = brain.processQuery("MAX, bhool jao client meeting", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be ForgetUserMemory, was $forgetDecision", forgetDecision is BrainDecision.ForgetUserMemory)

        val recallAllDecision = brain.processQuery("MAX, tum mere baare mein kya jante ho?", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be RecallUserMemories, was $recallAllDecision", recallAllDecision is BrainDecision.RecallUserMemories)
    }

    // ==================== 2. SELF-HEALING & HEALTH MONITOR INTENT TESTS ====================

    @Test
    fun testHealthMonitorAndDiagnosticsIntents() {
        val healthDecision = brain.processQuery("MAX, system health kaisa hai?", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be CheckSystemHealth, was $healthDecision", healthDecision is BrainDecision.CheckSystemHealth)

        val diagDecision = brain.processQuery("MAX, run diagnostics check", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("Decision must be RunSelfDiagnostics, was $diagDecision", diagDecision is BrainDecision.RunSelfDiagnostics)
    }

    // ==================== 3. INTELLIGENT NOTIFICATION INTENT TESTS ====================

    @Test
    fun testNotificationIntents() {
        val notifDecision = brain.processQuery("MAX, important notifications batao", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be SummarizeNotifications, was $notifDecision", notifDecision is BrainDecision.SummarizeNotifications)

        val waDecision = brain.processQuery("MAX, mere WhatsApp messages padho", outputLanguageSetting = VoiceLanguage.HINGLISH)
        assertTrue("Decision must be ReadWhatsAppNotifications, was $waDecision", waDecision is BrainDecision.ReadWhatsAppNotifications)
    }

    // ==================== 4. LIVE INTERNET RESEARCH & PERFORMANCE INTENT TESTS ====================

    @Test
    fun testLiveInternetResearchIntent() {
        val researchDecision = brain.processQuery("MAX, search internet for latest AI agents in 2026", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("Decision must be ConductLiveResearch, was $researchDecision", researchDecision is BrainDecision.ConductLiveResearch)
        val query = (researchDecision as BrainDecision.ConductLiveResearch).query
        assertTrue("Query must capture topic: $query", query.contains("latest AI agents") || query.contains("2026"))
    }

    @Test
    fun testPerformanceAnalyticsIntent() {
        val perfDecision = brain.processQuery("MAX, show me MAX performance report", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("Decision must be QueryPerformanceStats, was $perfDecision", perfDecision is BrainDecision.QueryPerformanceStats)
    }

    // ==================== 5. STRICT GALLERY PROTECTION PERMANENCE ====================

    @Test
    fun testStrictGalleryProtectionBlocked() {
        val hindiBlock = brain.processQuery("गैलरी खोलो", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue("Hindi gallery open must be blocked with SpeakResponse", hindiBlock is BrainDecision.SpeakResponse)
        val textHi = (hindiBlock as BrainDecision.SpeakResponse).text
        assertTrue("Must contain restriction explanation: $textHi", textHi.contains("प्रतिबंधित") || textHi.contains("अनुमति नहीं"))

        val englishBlock = brain.processQuery("MAX, open my gallery photos", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("English gallery open must be blocked with SpeakResponse", englishBlock is BrainDecision.SpeakResponse)
        val textEn = (englishBlock as BrainDecision.SpeakResponse).text
        assertTrue("Must contain restriction explanation: $textEn", textEn.contains("restricted", ignoreCase = true) || textEn.contains("permission", ignoreCase = true))

        val bengaliBlock = brain.processQuery("ম্যাক্স গ্যালারি ওপেন করো", outputLanguageSetting = VoiceLanguage.BENGALI)
        assertTrue("Bengali gallery open must be blocked", bengaliBlock is BrainDecision.SpeakResponse)
    }

    // ==================== 6. MULTI-STEP COMPOUND TASK TRANSLATION ====================

    @Test
    fun testYouTubeCompoundTaskDecision() {
        val decision = brain.processQuery("open youtube search for Arijit Singh and play", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("Must trigger compound task or YouTube plan: $decision", decision is BrainDecision.ExecuteCompoundTask || decision is BrainDecision.ExecuteMultiStepPlan)
    }

    @Test
    fun testChromeCompoundTaskDecision() {
        val decision = brain.processQuery("open chrome search for Darjeeling hotels and open first result", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue("Must trigger compound task: $decision", decision is BrainDecision.ExecuteCompoundTask || decision is BrainDecision.ExecuteMultiStepPlan)
    }
}
