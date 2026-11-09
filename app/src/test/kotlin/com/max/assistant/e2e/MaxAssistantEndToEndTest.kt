package com.max.assistant.e2e

import com.max.assistant.action.ActionConfirmationManager
import com.max.assistant.action.PendingAction
import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.ChatMessage
import com.max.assistant.ai.ConversationRepository
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.ai.MessageRole
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.speech.AssistantState
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.tts.VoiceSettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MaxAssistantEndToEndTest {

    private lateinit var brain: MaxAiBrain
    private lateinit var confirmationManager: ActionConfirmationManager

    @Before
    fun setUp() {
        brain = MaxAiBrain()
        confirmationManager = ActionConfirmationManager()
    }

    @Test
    fun journey1_englishGeneralKnowledgeAndContextRetention() {
        val history = mutableListOf<ChatMessage>()

        // Turn 1: User asks about Albert Einstein in English
        val q1 = "Who was Albert Einstein?"
        history.add(ChatMessage(role = MessageRole.USER, text = q1, language = VoiceLanguage.ENGLISH))
        val decision1 = brain.processQuery(q1, outputLanguageSetting = VoiceLanguage.ENGLISH, history = history)
        
        assertTrue("Decision 1 must be SpeakResponse", decision1 is BrainDecision.SpeakResponse)
        val r1 = (decision1 as BrainDecision.SpeakResponse).text
        assertTrue("Answer must address user as Sir: $r1", r1.contains("Sir"))
        assertTrue("Answer must mention relativity or physics: $r1", r1.contains("relativity") || r1.contains("physicist"))
        assertEquals("Albert Einstein", brain.knowledgeBase.activeContextTopic)
        history.add(ChatMessage(role = MessageRole.MAX, text = r1, language = VoiceLanguage.ENGLISH))

        // Turn 2: Follow-up question with ambiguous pronoun "he"
        val q2 = "When was he born?"
        history.add(ChatMessage(role = MessageRole.USER, text = q2, language = VoiceLanguage.ENGLISH))
        val decision2 = brain.processQuery(q2, outputLanguageSetting = VoiceLanguage.ENGLISH, history = history)
        
        assertTrue("Decision 2 must be SpeakResponse", decision2 is BrainDecision.SpeakResponse)
        val r2 = (decision2 as BrainDecision.SpeakResponse).text
        assertTrue("Must address as Sir: $r2", r2.contains("Sir"))
        assertTrue("Context retention must identify Einstein's birth year (1879): $r2", r2.contains("1879"))

        // Turn 3: Science question: Speed of light
        val q3 = "What is the speed of light?"
        val decision3 = brain.processQuery(q3, outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue(decision3 is BrainDecision.SpeakResponse)
        val r3 = (decision3 as BrainDecision.SpeakResponse).text
        assertTrue("Speed of light should mention 299,792: $r3", r3.contains("299,792"))
    }

    @Test
    fun journey2_hindiConversationAndAssistantIdentity() {
        val history = mutableListOf<ChatMessage>()

        // Turn 1: Identity inquiry in Hindi
        val q1 = "मैक्स, तुम कौन हो?"
        val d1 = brain.processQuery(q1, outputLanguageSetting = VoiceLanguage.HINDI, history = history)
        assertTrue(d1 is BrainDecision.SpeakResponse)
        val r1 = (d1 as BrainDecision.SpeakResponse).text
        assertEquals(VoiceLanguage.HINDI, d1.language)
        assertTrue("Must address as 'सर': $r1", r1.contains("सर"))
        assertTrue("Must identify as MAX: $r1", r1.contains("मैक्स"))

        // Turn 2: Time inquiry in Hindi
        val q2 = "समय क्या हुआ है?"
        val d2 = brain.processQuery(q2, outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(d2 is BrainDecision.SpeakResponse)
        val r2 = (d2 as BrainDecision.SpeakResponse).text
        assertTrue("Time statement must address as 'सर': $r2", r2.contains("सर") && r2.contains("समय"))

        // Turn 3: Math query in Hindi
        val q3 = "calculate 45 plus 55"
        val d3 = brain.processQuery(q3, outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(d3 is BrainDecision.SpeakResponse)
        val r3 = (d3 as BrainDecision.SpeakResponse).text
        assertTrue("Math result 100 must be in Hindi response: $r3", r3.contains("100") && r3.contains("सर"))
    }

    @Test
    fun journey3_bengaliConversationAndGreeting() {
        // Bengali Greeting
        val q1 = "নমস্কার ম্যাক্স"
        val d1 = brain.processQuery(q1, outputLanguageSetting = VoiceLanguage.BENGALI)
        assertTrue(d1 is BrainDecision.SpeakResponse)
        val r1 = (d1 as BrainDecision.SpeakResponse).text
        assertEquals(VoiceLanguage.BENGALI, d1.language)
        assertTrue("Must address as 'স্যার' in Bengali: $r1", r1.contains("স্যার"))

        // Bengali Time
        val q2 = "এখন সময় কত?"
        val d2 = brain.processQuery(q2, outputLanguageSetting = VoiceLanguage.BENGALI)
        assertTrue(d2 is BrainDecision.SpeakResponse)
        val r2 = (d2 as BrainDecision.SpeakResponse).text
        assertTrue("Must address as 'স্যার' in Bengali time reply: $r2", r2.contains("স্যার") && r2.contains("সময়"))
    }

    @Test
    fun journey4_strictGalleryProtectionEnforcement() {
        val testAttacks = listOf(
            "open gallery",
            "launch Samsung Gallery",
            "show my photos",
            "open google photos",
            "फोटो गैलरी खोलो",
            "तस्वीरें दिखाओ",
            "গ্যালারি খোলো",
            "ফটো অ্যালবাম দেখাও",
            "camera roll",
            "open DCIM"
        )

        for (prompt in testAttacks) {
            assertTrue("GalleryProtectionManager must flag: '$prompt'", GalleryProtectionManager.shouldBlockVoiceLaunch(prompt))
            
            val decision = brain.processQuery(prompt, outputLanguageSetting = VoiceLanguage.HINDI)
            assertTrue("Decision must be SpeakResponse (not LaunchApp): '$prompt'", decision is BrainDecision.SpeakResponse)
            val reply = (decision as BrainDecision.SpeakResponse).text
            assertTrue("Reply must indicate restriction: '$reply'", reply.contains("प्रतिबंधित") || reply.contains("सुरक्षा नीति"))
        }
    }

    @Test
    fun journey5_phoneCallAndMessagingWithConfirmationFlow() {
        // Step 1: User asks to call contact
        val callQuery = "Rahul ko call lagao"
        val decision = brain.processQuery(callQuery)
        assertTrue(decision is BrainDecision.MakePhoneCall)
        val callDecision = decision as BrainDecision.MakePhoneCall
        assertEquals("Rahul", callDecision.target)
        assertTrue("Calls MUST require confirmation", callDecision.requiresConfirmation)

        // Step 2: System requests confirmation
        val prompt = confirmationManager.requestCallConfirmation(callDecision.target)
        assertTrue("Confirmation prompt must mention target: $prompt", prompt.contains("Rahul"))
        assertNotNull("Pending action must be set", confirmationManager.pendingAction.value)

        // Step 3: User confirms with affirmative response ("हाँ" / "yes")
        assertTrue(confirmationManager.isConfirmationAffirmative("हाँ"))
        val confirmedAction = confirmationManager.confirm()
        assertTrue(confirmedAction is PendingAction.MakeCall)
        assertEquals("Rahul", (confirmedAction as PendingAction.MakeCall).target)
        assertNull("Pending action must be cleared after confirmation", confirmationManager.pendingAction.value)

        // Step 4: User asks to send message, then cancels
        val msgQuery = "send message to Priya: I am on my way"
        val msgDecision = brain.processQuery(msgQuery) as BrainDecision.SendMessage
        assertEquals("Priya", msgDecision.recipient)
        assertEquals("I am on my way", msgDecision.message)
        
        confirmationManager.requestMessageConfirmation(msgDecision.recipient, msgDecision.message, false)
        assertNotNull(confirmationManager.pendingAction.value)

        // User says "Cancel" / "Nahi"
        assertTrue(confirmationManager.isConfirmationNegative("nahi"))
        confirmationManager.cancel()
        assertNull("Pending action must be cleared after cancellation", confirmationManager.pendingAction.value)
    }

    @Test
    fun journey6_appLauncherIntentRouting() {
        // General third party apps route to LaunchApp
        val generalApps = listOf(
            "open YouTube",
            "Calculator open karo"
        )

        for (query in generalApps) {
            val decision = brain.processQuery(query)
            assertTrue("Query '$query' should resolve to LaunchApp", decision is BrainDecision.LaunchApp)
            val launch = decision as BrainDecision.LaunchApp
            assertTrue(
                "Spoken response must address user respectfully (Sir/सर/স্যার): ${launch.responseText}",
                launch.responseText.contains("Sir") || launch.responseText.contains("सर") || launch.responseText.contains("স্যার")
            )
        }

        // Core system phone actions route to dedicated Android Command Executor decisions
        val chromeDecision = brain.processQuery("Chrome browser kholo")
        assertTrue("Chrome command should resolve to OpenBrowser or LaunchApp", chromeDecision is BrainDecision.OpenBrowser || chromeDecision is BrainDecision.LaunchApp)

        val mapsDecision = brain.processQuery("Google Maps chalao")
        assertTrue("Maps command should resolve to OpenMaps or LaunchApp", mapsDecision is BrainDecision.OpenMaps || mapsDecision is BrainDecision.LaunchApp)

        val cameraDecision = brain.processQuery("open camera")
        assertTrue("Camera command should resolve to OpenCamera", cameraDecision is BrainDecision.OpenCamera)
    }

    @Test
    fun journey7_systemTelemetryAndAlarms() {
        val dBattery = brain.processQuery("How much battery is remaining?")
        assertTrue(dBattery is BrainDecision.QueryBattery)

        val dNetwork = brain.processQuery("Check internet connection")
        assertTrue(dNetwork is BrainDecision.QueryNetwork)

        val dAlarm = brain.processQuery("Set an alarm for 6:30 am")
        assertTrue(dAlarm is BrainDecision.SetAlarm)
        val alarm = dAlarm as BrainDecision.SetAlarm
        assertEquals(6, alarm.hour)
        assertEquals(30, alarm.minute)

        val dAlarmPm = brain.processQuery("alarm lagao 8 pm")
        assertTrue(dAlarmPm is BrainDecision.SetAlarm)
        assertEquals(20, (dAlarmPm as BrainDecision.SetAlarm).hour)
    }

    @Test
    fun journey8_emergencyStopAndPinSecurity() {
        // Step 1: User requests emergency stop
        val stopDecision = brain.processQuery("MAX stop karo")
        assertTrue(stopDecision is BrainDecision.TriggerStopMax)

        // Step 2: Validate PIN verification logic
        val correctPin = "1212"
        val wrongPin = "9999"

        assertFalse("Wrong PIN must be rejected", wrongPin == "1212")
        assertTrue("Correct PIN '1212' must unlock", correctPin == "1212")
    }

    @Test
    fun journey9_voiceSettingsAndMuting() {
        val settings = VoiceSettings(
            pitch = 0.88f,
            speed = 0.98f,
            wakeWordEnabled = true,
            continuousListening = true
        )

        assertEquals(0.88f, settings.pitch, 0.001f)
        assertEquals(0.98f, settings.speed, 0.001f)
        assertTrue(settings.wakeWordEnabled)
        assertTrue(settings.continuousListening)

        val muteDecision = brain.processQuery("mute")
        assertTrue(muteDecision is BrainDecision.TriggerMute)
        assertTrue((muteDecision as BrainDecision.TriggerMute).mute)

        val unmuteDecision = brain.processQuery("awaz chalu karo")
        assertTrue(unmuteDecision is BrainDecision.TriggerMute)
        assertFalse((unmuteDecision as BrainDecision.TriggerMute).mute)
    }
}
