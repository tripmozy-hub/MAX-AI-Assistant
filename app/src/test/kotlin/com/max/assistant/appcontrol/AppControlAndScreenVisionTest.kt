package com.max.assistant.appcontrol

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.vision.ScreenContextManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppControlAndScreenVisionTest {

    private lateinit var brain: MaxAiBrain
    private lateinit var screenContextManager: ScreenContextManager

    @Before
    fun setUp() {
        screenContextManager = ScreenContextManager()
        brain = MaxAiBrain(screenContextManager = screenContextManager)
    }

    @Test
    fun testScreenVisionTriggerPhrases() {
        // Hindi/Hinglish
        val d1 = brain.processQuery("MAX, kya tum ye dekh rahe ho?", VoiceLanguage.HINGLISH)
        assertTrue(d1 is BrainDecision.CaptureScreenVision)

        val d2 = brain.processQuery("MAX, isme kya likha hai?", VoiceLanguage.HINGLISH)
        assertTrue(d2 is BrainDecision.CaptureScreenVision)

        val d3 = brain.processQuery("MAX, ye error kya hai?", VoiceLanguage.HINGLISH)
        assertTrue(d3 is BrainDecision.CaptureScreenVision)

        // English
        val d4 = brain.processQuery("MAX, can you see my screen?", VoiceLanguage.ENGLISH)
        assertTrue(d4 is BrainDecision.CaptureScreenVision)

        val d5 = brain.processQuery("MAX, look at this", VoiceLanguage.ENGLISH)
        assertTrue(d5 is BrainDecision.CaptureScreenVision)

        val d6 = brain.processQuery("MAX, what am I looking at?", VoiceLanguage.ENGLISH)
        assertTrue(d6 is BrainDecision.CaptureScreenVision)

        val d7 = brain.processQuery("MAX, explain this diagram", VoiceLanguage.ENGLISH)
        assertTrue(d7 is BrainDecision.CaptureScreenVision)

        // Bengali
        val d8 = brain.processQuery("ম্যাক্স, স্ক্রিন দেখো", VoiceLanguage.BENGALI)
        assertTrue(d8 is BrainDecision.CaptureScreenVision)

        val d9 = brain.processQuery("ম্যাক্স, এতে কি লেখা আছে?", VoiceLanguage.BENGALI)
        assertTrue(d9 is BrainDecision.CaptureScreenVision)

        // Stop vision
        val dStop = brain.processQuery("MAX, stop watching", VoiceLanguage.ENGLISH)
        assertTrue(dStop is BrainDecision.StopScreenVision)
    }

    @Test
    fun testScreenFollowUpQuestionsWhenContextActive() {
        // Without active context, "what is this" is not a screen follow-up
        assertFalse(screenContextManager.hasValidContext())

        // Provide active screen context
        screenContextManager.updateScreenContext(
            summary = "Hotel booking page in Darjeeling with Room details and Login error",
            visibleText = "Error 403: Invalid credentials. Room 102 Deluxe.",
            bitmap = null
        )
        assertTrue(screenContextManager.hasValidContext())

        // Follow-up questions
        val q1 = brain.processQuery("What is this?", VoiceLanguage.ENGLISH)
        assertTrue(q1 is BrainDecision.ScreenQuestionFollowUp)

        val q2 = brain.processQuery("Explain the second option", VoiceLanguage.ENGLISH)
        assertTrue(q2 is BrainDecision.ScreenQuestionFollowUp)

        val q3 = brain.processQuery("What should I click?", VoiceLanguage.ENGLISH)
        assertTrue(q3 is BrainDecision.ScreenQuestionFollowUp)

        val q4 = brain.processQuery("Is this safe?", VoiceLanguage.ENGLISH)
        assertTrue(q4 is BrainDecision.ScreenQuestionFollowUp)

        val q5 = brain.processQuery("What does this error mean?", VoiceLanguage.ENGLISH)
        assertTrue(q5 is BrainDecision.ScreenQuestionFollowUp)

        val q6 = brain.processQuery("Ab kya karu?", VoiceLanguage.HINGLISH)
        assertTrue(q6 is BrainDecision.ScreenQuestionFollowUp)
    }

    @Test
    fun testAppControlCommands() {
        // Scrolling
        val dScrollDown = brain.processQuery("MAX, scroll down", VoiceLanguage.ENGLISH)
        assertTrue(dScrollDown is BrainDecision.ScrollScreen && dScrollDown.isDown)

        val dScrollUp = brain.processQuery("MAX, upar scroll karo", VoiceLanguage.HINGLISH)
        assertTrue(dScrollUp is BrainDecision.ScrollScreen && !dScrollUp.isDown)

        // Navigation
        val dBack = brain.processQuery("MAX, go back", VoiceLanguage.ENGLISH)
        assertTrue(dBack is BrainDecision.NavigateBack)

        val dClose = brain.processQuery("MAX, close this screen", VoiceLanguage.ENGLISH)
        assertTrue(dClose is BrainDecision.CloseScreen)

        // Click nth option
        val dClick2nd = brain.processQuery("MAX, click the second option", VoiceLanguage.ENGLISH)
        assertTrue(dClick2nd is BrainDecision.ClickNthScreenOption && dClick2nd.n == 2)

        val dFirstResult = brain.processQuery("MAX, open the first result", VoiceLanguage.ENGLISH)
        assertTrue(dFirstResult is BrainDecision.ClickNthScreenOption && dFirstResult.n == 1)

        // Typing
        val dType = brain.processQuery("MAX, type hello, how are you?", VoiceLanguage.ENGLISH)
        assertTrue(dType is BrainDecision.TypeScreenText)
        assertEquals("hello, how are you?", (dType as BrainDecision.TypeScreenText).text)

        // WhatsApp specific element
        val dWa = brain.processQuery("MAX, open Rahul's chat", VoiceLanguage.ENGLISH)
        assertTrue(dWa is BrainDecision.ClickScreenElement)
        assertTrue((dWa as BrainDecision.ClickScreenElement).target.contains("Rahul", ignoreCase = true))

        // Switch to app
        val dSwitch = brain.processQuery("MAX, switch to Chrome", VoiceLanguage.ENGLISH)
        assertTrue(dSwitch is BrainDecision.SwitchApp)
        assertEquals("chrome", (dSwitch as BrainDecision.SwitchApp).targetApp)
    }

    @Test
    fun testMultiStepTaskCommands() {
        // Compound execution task
        val dTask1 = brain.processQuery("MAX, open YouTube, search for a Bengali travel documentary and play the first result", VoiceLanguage.ENGLISH)
        assertTrue(dTask1 is BrainDecision.ExecuteCompoundTask)

        val dTask2 = brain.processQuery("MAX, open Chrome, search for hotels in Darjeeling and open the first result", VoiceLanguage.ENGLISH)
        assertTrue(dTask2 is BrainDecision.ExecuteCompoundTask)

        // Cancel task
        val dCancel = brain.processQuery("MAX, cancel this operation", VoiceLanguage.ENGLISH)
        assertTrue(dCancel is BrainDecision.CancelTask)

        // Explain progress
        val dExplain = brain.processQuery("MAX, explain what you are doing", VoiceLanguage.ENGLISH)
        assertTrue(dExplain is BrainDecision.ExplainTaskProgress)

        // Form filling assistance
        val dForm = brain.processQuery("MAX, help me fill this form", VoiceLanguage.ENGLISH)
        assertTrue(dForm is BrainDecision.FillFormAssistance)
    }

    @Test
    fun testStrictGalleryExclusion() {
        GalleryProtectionManager.setProtectionActive(true)

        // Voice command to open gallery must be blocked with exact required message
        val dGallery = brain.processQuery("MAX, open Gallery", VoiceLanguage.ENGLISH)
        assertTrue(dGallery is BrainDecision.SpeakResponse)
        val text = (dGallery as BrainDecision.SpeakResponse).text
        assertTrue(text.contains("permission to access your gallery", ignoreCase = true) || text.contains("restricted", ignoreCase = true))

        val dPhotos = brain.processQuery("MAX, show my photos", VoiceLanguage.ENGLISH)
        assertTrue(dPhotos is BrainDecision.SpeakResponse)
        val textPhotos = (dPhotos as BrainDecision.SpeakResponse).text
        assertTrue(textPhotos.contains("permission to access your gallery", ignoreCase = true) || textPhotos.contains("restricted", ignoreCase = true))

        // Hindi
        val dHindi = brain.processQuery("गैलरी खोलो", VoiceLanguage.HINDI)
        assertTrue(dHindi is BrainDecision.SpeakResponse)

        // Bengali
        val dBen = brain.processQuery("গ্যালারি খোলো", VoiceLanguage.BENGALI)
        assertTrue(dBen is BrainDecision.SpeakResponse)
    }
}
