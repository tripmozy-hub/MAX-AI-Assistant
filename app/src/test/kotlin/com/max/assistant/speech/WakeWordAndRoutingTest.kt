package com.max.assistant.speech

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WakeWordAndRoutingTest {

    private val brain = MaxAiBrain()

    // Helper mirror function of MaxSpeechManager wake word detection logic
    private fun containsWakeWord(text: String, allowShortWakeWord: Boolean): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        val primary = listOf(
            "hey max", "hello max", "ok max", "okay max",
            "hey mac", "hello mac", "ok mac", "okay mac",
            "hey mex", "hello mex", "ok mex", "okay mex",
            "hey maks", "hello maks", "ok maks", "okay maks",
            "hey macks", "hello macks", "ok macks",
            "हे मैक्स", "हे मेक्स", "सुनो मैक्स", "सुनो मेक्स",
            "হেই ম্যাক্স", "ম্যাক্স শোনো", "হে ম্যাক্স", "মেক্স শোনো"
        )
        for (p in primary) {
            if (lower.contains(p)) return true
        }

        if (allowShortWakeWord) {
            val shortWords = listOf("max", "mac", "mex", "maks", "macks", "macs", "mags", "maggs", "मैक्स", "मेक्स", "ম্যাক্স", "মেক্স")
            for (sw in shortWords) {
                val wordRegex = Regex("(^|\\s|,|:|-|\\.)$sw(\\s|,|:|-|\\.|\$)", RegexOption.IGNORE_CASE)
                if (wordRegex.containsMatchIn(lower)) return true
            }
        }
        return false
    }

    private fun extractCommandAfterWakeWord(text: String, allowShortWakeWord: Boolean): String {
        val lower = text.lowercase(Locale.ROOT)
        val primary = listOf(
            "hey max", "hello max", "ok max", "okay max",
            "hey mac", "hello mac", "ok mac", "okay mac",
            "hey mex", "hello mex", "ok mex", "okay mex",
            "hey maks", "hello maks", "ok maks", "okay maks",
            "hey macks", "hello macks", "ok macks",
            "हे मैक्स", "हे मेक्स", "सुनो मैक्स", "सुनो मेक्स",
            "হেই ম্যাক্স", "ম্যাক্স শোনো", "হে ম্যাক্স", "মেক্স শোনো"
        )
        for (ww in primary) {
            if (lower.contains(ww)) {
                val idx = lower.indexOf(ww)
                val after = text.substring(idx + ww.length)
                    .trimStart(',', ' ', ':', '-', ';', '.', '\'', '"')
                    .trim()
                if (after.isNotEmpty()) return after
            }
        }
        if (allowShortWakeWord) {
            val shortWords = listOf("max", "mac", "mex", "maks", "macks", "macs", "mags", "maggs", "मैक्स", "मेक्स", "ম্যাক্স", "মেক্স")
            for (sw in shortWords) {
                val regex = Regex("\\b$sw\\b", RegexOption.IGNORE_CASE)
                val match = regex.find(lower)
                if (match != null) {
                    val after = text.substring(match.range.last + 1)
                        .trimStart(',', ' ', ':', '-', ';', '.', '\'', '"')
                        .trim()
                    if (after.isNotEmpty()) return after
                }
            }
        }
        return ""
    }

    @Test
    fun testWakeWordDetection_primaryPhrases() {
        assertTrue(containsWakeWord("Hey MAX", false))
        assertTrue(containsWakeWord("hey max", false))
        assertTrue(containsWakeWord("Hello Max, please assist", false))
        assertTrue(containsWakeWord("Ok Max, what's up", false))
        assertTrue(containsWakeWord("hey mex", false))
        assertTrue(containsWakeWord("hey mac", false))
        assertTrue(containsWakeWord("हे मैक्स", false))
        assertTrue(containsWakeWord("सुनो मैक्स क्या हाल है", false))
        assertTrue(containsWakeWord("হেই ম্যাক্স", false))
        assertTrue(containsWakeWord("ম্যাক্স শোনো ক্যামেরা খোলো", false))
    }

    @Test
    fun testShortWakeWord_preferMaxAndMatchingVariants() {
        // When allowShortWakeWord is TRUE: single word "MAX" and common phonetic matches trigger
        assertTrue("Single word 'MAX' must trigger", containsWakeWord("MAX", true))
        assertTrue("Phonetic match 'Mac' must trigger as MAX", containsWakeWord("Mac", true))
        assertTrue("Phonetic match 'Mex' must trigger as MAX", containsWakeWord("Mex", true))
        assertTrue("Phonetic match 'Maks' must trigger as MAX", containsWakeWord("Maks", true))
        assertTrue(containsWakeWord("मैक्स", true))
        assertTrue(containsWakeWord("मेक्स", true))
        assertTrue(containsWakeWord("ম্যাক্স", true))
        assertTrue(containsWakeWord("মেক্স", true))
        assertTrue(containsWakeWord("MAX, open camera", true))
        assertTrue(containsWakeWord("Mex, open camera", true))
        assertTrue(containsWakeWord("Mac, open settings", true))

        // Even with short wake words enabled, words containing 'max' as substring MUST NOT trigger
        assertFalse("Substring match 'maximum' must never trigger", containsWakeWord("maximum capacity", true))
        assertFalse("Substring match 'climax' must never trigger", containsWakeWord("climax scene", true))
        assertFalse("Substring match 'macaroni' must never trigger", containsWakeWord("macaroni cheese", true))
    }

    @Test
    fun testDirectCommandExtraction() {
        assertEquals("open camera", extractCommandAfterWakeWord("Hey MAX, open camera", false))
        assertEquals("what time is it?", extractCommandAfterWakeWord("Hey MAX, what time is it?", false))
        assertEquals("राहुल को कॉल करो", extractCommandAfterWakeWord("हे मैक्स राहुल को कॉल करो", false))
        assertEquals("ক্যামেরা খোলো", extractCommandAfterWakeWord("ম্যাক্স শোনো ক্যামেরা খোলো", false))
        assertEquals("open camera", extractCommandAfterWakeWord("Mex open camera", true))
        assertEquals("open settings", extractCommandAfterWakeWord("Mac open settings", true))
        assertEquals("open camera", extractCommandAfterWakeWord("Max open camera", true))
        assertEquals("कैमरा खोलो", extractCommandAfterWakeWord("मेक्स कैमरा खोलो", true))
        assertEquals("", extractCommandAfterWakeWord("Hey MAX", false))
        assertEquals("", extractCommandAfterWakeWord("हे मैक्स", false))
        assertEquals("", extractCommandAfterWakeWord("MAX", true))
        assertEquals("", extractCommandAfterWakeWord("Mex", true))
        assertEquals("", extractCommandAfterWakeWord("Mac", true))
    }

    @Test
    fun testWhatsAppHijackingFix_generalQueriesNeverLaunchWhatsApp() {
        // "what is the time" must NEVER trigger WhatsApp!
        val timeDecision = brain.processQuery("what is the time")
        assertFalse("Must not resolve to OpenWhatsApp: $timeDecision", timeDecision is BrainDecision.OpenWhatsApp)
        assertTrue("Must resolve to SpeakResponse: $timeDecision", timeDecision is BrainDecision.SpeakResponse)

        // "what is the weather" must NEVER trigger WhatsApp!
        val weatherDecision = brain.processQuery("what is the weather")
        assertFalse("Must not resolve to OpenWhatsApp: $weatherDecision", weatherDecision is BrainDecision.OpenWhatsApp)
        assertTrue("Must resolve to QueryWeather: $weatherDecision", weatherDecision is BrainDecision.QueryWeather)

        // "what is AI" must NEVER trigger WhatsApp!
        val aiDecision = brain.processQuery("what is artificial intelligence")
        assertFalse("Must not resolve to OpenWhatsApp: $aiDecision", aiDecision is BrainDecision.OpenWhatsApp)
        assertTrue("Must resolve to SpeakResponse: $aiDecision", aiDecision is BrainDecision.SpeakResponse)

        // "what can you do" must NEVER trigger WhatsApp!
        val identityDecision = brain.processQuery("what can you do")
        assertFalse("Must not resolve to OpenWhatsApp: $identityDecision", identityDecision is BrainDecision.OpenWhatsApp)
    }

    @Test
    fun testActionRouting_cameraAndPhoneAndBrowser() {
        // Camera in English, Hindi, Bengali
        assertTrue(brain.processQuery("open camera") is BrainDecision.OpenCamera)
        assertTrue(brain.processQuery("camera kholo") is BrainDecision.OpenCamera)
        assertTrue(brain.processQuery("कैमरा खोलो") is BrainDecision.OpenCamera)
        assertTrue(brain.processQuery("ক্যামেরা খোলো") is BrainDecision.OpenCamera)
        assertTrue(brain.processQuery("camera chalao") is BrainDecision.OpenCamera)

        // Gallery Protection check: "photo" or "gallery" must be blocked
        val galleryDecision = brain.processQuery("open gallery")
        assertTrue(galleryDecision is BrainDecision.SpeakResponse)
        assertTrue((galleryDecision as BrainDecision.SpeakResponse).text.contains("सुरक्षा नीति") || galleryDecision.text.contains("restricted"))

        // Phone / Dialer in English, Hindi, Bengali
        assertTrue(brain.processQuery("open phone") is BrainDecision.OpenPhone)
        assertTrue(brain.processQuery("dialer kholo") is BrainDecision.OpenPhone)
        assertTrue(brain.processQuery("कॉल खोलो") is BrainDecision.OpenPhone)
        assertTrue(brain.processQuery("फोन खोलो") is BrainDecision.OpenPhone)
        assertTrue(brain.processQuery("কল খোলো") is BrainDecision.OpenPhone)

        // Browser / Chrome
        assertTrue(brain.processQuery("open chrome") is BrainDecision.OpenBrowser)
        assertTrue(brain.processQuery("chrome kholo") is BrainDecision.OpenBrowser)
        assertTrue(brain.processQuery("क्रोम खोलो") is BrainDecision.OpenBrowser)

        // Settings
        assertTrue(brain.processQuery("open settings") is BrainDecision.OpenSettings)
        assertTrue(brain.processQuery("settings kholo") is BrainDecision.OpenSettings)
        assertTrue(brain.processQuery("सेटिंग्स खोलो") is BrainDecision.OpenSettings)

        // Explicit WhatsApp (ONLY when WhatsApp is explicitly named)
        assertTrue(brain.processQuery("open WhatsApp") is BrainDecision.OpenWhatsApp)
        assertTrue(brain.processQuery("whatsapp kholo") is BrainDecision.OpenWhatsApp)
        assertTrue(brain.processQuery("व्हाट्सएप खोलो") is BrainDecision.OpenWhatsApp)

        // Calling with name
        val callDecision = brain.processQuery("call Rahul")
        assertTrue(callDecision is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (callDecision as BrainDecision.MakePhoneCall).target)

        val callHindi = brain.processQuery("राहुल को कॉल करो")
        assertTrue(callHindi is BrainDecision.MakePhoneCall)
        assertEquals("राहुल", (callHindi as BrainDecision.MakePhoneCall).target)
    }

    @Test
    fun testWhatsAppReplyFlows() {
        // 1. Generic recipient: user asks MAX to open WhatsApp and reply to whoever they want
        val generic1 = brain.processQuery("open whats aap and reply to whome i want to")
        assertTrue("Should request reply recipient: $generic1", generic1 is BrainDecision.RequestReplyRecipient)
        assertTrue((generic1 as BrainDecision.RequestReplyRecipient).isWhatsApp)

        val generic2 = brain.processQuery("open whats app and reply")
        assertTrue("Should request reply recipient: $generic2", generic2 is BrainDecision.RequestReplyRecipient)

        val generic3 = brain.processQuery("open whatsapp and reply to someone")
        assertTrue("Should request reply recipient: $generic3", generic3 is BrainDecision.RequestReplyRecipient)

        // 2. Interactive flow with recipient specified (no message yet)
        val withRecipient1 = brain.processQuery("open whats aap and reply to Rahul")
        assertTrue("Should request reply content: $withRecipient1", withRecipient1 is BrainDecision.RequestReplyContent)
        val contentDecision1 = withRecipient1 as BrainDecision.RequestReplyContent
        assertEquals("Rahul", contentDecision1.recipient)
        assertTrue(contentDecision1.isWhatsApp)

        val withRecipient2 = brain.processQuery("open whatsapp and reply to Mom")
        assertTrue("Should request reply content: $withRecipient2", withRecipient2 is BrainDecision.RequestReplyContent)
        assertEquals("Mom", (withRecipient2 as BrainDecision.RequestReplyContent).recipient)

        val withRecipientHindi = brain.processQuery("whatsapp par Rahul ko reply karo")
        assertTrue("Should request reply content: $withRecipientHindi", withRecipientHindi is BrainDecision.RequestReplyContent)
        assertEquals("Rahul", (withRecipientHindi as BrainDecision.RequestReplyContent).recipient)

        val withRecipientBengali = brain.processQuery("হোয়াটসঅ্যাপে রাহুলকে রিপ্লাই দাও")
        assertTrue("Should request reply content: $withRecipientBengali", withRecipientBengali is BrainDecision.RequestReplyContent)
        assertEquals("রাহুল", (withRecipientBengali as BrainDecision.RequestReplyContent).recipient)

        // 3. Complete single-breath reply command (both recipient and message specified)
        val complete1 = brain.processQuery("open whats aap and reply to Rahul: I am on the way")
        assertTrue("Should be direct SendMessage: $complete1", complete1 is BrainDecision.SendMessage)
        val send1 = complete1 as BrainDecision.SendMessage
        assertEquals("Rahul", send1.recipient)
        assertEquals("I am on the way", send1.message)
        assertTrue(send1.isWhatsApp)
        assertFalse("Single-breath reply should not require extra confirmation", send1.requiresConfirmation)

        val complete2 = brain.processQuery("open whatsapp and reply to Mom that I will be late")
        assertTrue("Should be direct SendMessage: $complete2", complete2 is BrainDecision.SendMessage)
        val send2 = complete2 as BrainDecision.SendMessage
        assertEquals("Mom", send2.recipient)
        assertEquals("I will be late", send2.message)
        assertTrue(send2.isWhatsApp)

        val completeHindi = brain.processQuery("whatsapp par Rahul ko reply karo ki main 5 minute me aa raha hoon")
        assertTrue("Should be direct SendMessage: $completeHindi", completeHindi is BrainDecision.SendMessage)
        val sendHindi = completeHindi as BrainDecision.SendMessage
        assertEquals("Rahul", sendHindi.recipient)
        assertEquals("main 5 minute me aa raha hoon", sendHindi.message)
        assertTrue(sendHindi.isWhatsApp)

        val completeBengali = brain.processQuery("হোয়াটসঅ্যাপে রাহুলকে রিপ্লাই দাও যে আমি আসছি")
        assertTrue("Should be direct SendMessage: $completeBengali", completeBengali is BrainDecision.SendMessage)
        val sendBengali = completeBengali as BrainDecision.SendMessage
        assertEquals("রাহুল", sendBengali.recipient)
        assertEquals("আমি আসছি", sendBengali.message)
        assertTrue(sendBengali.isWhatsApp)
    }

    @Test
    fun testCallingCommands_callKroCallLagaoMeryBaatKrwao() {
        // 1. "call lagao" with name
        val lagao1 = brain.processQuery("Rahul ko call lagao")
        assertTrue("Should be MakePhoneCall: $lagao1", lagao1 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (lagao1 as BrainDecision.MakePhoneCall).target)
        assertTrue("Call command requires user confirmation", lagao1.requiresConfirmation)

        val lagao2 = brain.processQuery("call lagao Rahul ko")
        assertTrue("Should be MakePhoneCall: $lagao2", lagao2 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (lagao2 as BrainDecision.MakePhoneCall).target)

        val lagao3 = brain.processQuery("call lagao Mom")
        assertTrue("Should be MakePhoneCall: $lagao3", lagao3 is BrainDecision.MakePhoneCall)
        assertEquals("Mom", (lagao3 as BrainDecision.MakePhoneCall).target)

        // 2. "call kro" / "call karo" with name
        val kro1 = brain.processQuery("Rahul ko call kro")
        assertTrue("Should be MakePhoneCall: $kro1", kro1 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (kro1 as BrainDecision.MakePhoneCall).target)

        val kro2 = brain.processQuery("call kro Rahul")
        assertTrue("Should be MakePhoneCall: $kro2", kro2 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (kro2 as BrainDecision.MakePhoneCall).target)

        val karo1 = brain.processQuery("call karo Dad")
        assertTrue("Should be MakePhoneCall: $karo1", karo1 is BrainDecision.MakePhoneCall)
        assertEquals("Dad", (karo1 as BrainDecision.MakePhoneCall).target)

        // 3. "mery baat krwao" / "meri baat karwao" with name
        val baat1 = brain.processQuery("Rahul se mery baat krwao")
        assertTrue("Should be MakePhoneCall: $baat1", baat1 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (baat1 as BrainDecision.MakePhoneCall).target)

        val baat2 = brain.processQuery("Rahul se meri baat karwao")
        assertTrue("Should be MakePhoneCall: $baat2", baat2 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (baat2 as BrainDecision.MakePhoneCall).target)

        val baat3 = brain.processQuery("mery baat krwao Rahul se")
        assertTrue("Should be MakePhoneCall: $baat3", baat3 is BrainDecision.MakePhoneCall)
        assertEquals("Rahul", (baat3 as BrainDecision.MakePhoneCall).target)

        val baat4 = brain.processQuery("meri baat karwao Mom se")
        assertTrue("Should be MakePhoneCall: $baat4", baat4 is BrainDecision.MakePhoneCall)
        assertEquals("Mom", (baat4 as BrainDecision.MakePhoneCall).target)

        // 4. Hindi & Bengali script variants
        val hindiLagao = brain.processQuery("राहुल को कॉल लगाओ")
        assertTrue("Should be MakePhoneCall: $hindiLagao", hindiLagao is BrainDecision.MakePhoneCall)
        assertEquals("राहुल", (hindiLagao as BrainDecision.MakePhoneCall).target)

        val hindiBaat = brain.processQuery("राहुल से मेरी बात करवाओ")
        assertTrue("Should be MakePhoneCall: $hindiBaat", hindiBaat is BrainDecision.MakePhoneCall)
        assertEquals("राहुल", (hindiBaat as BrainDecision.MakePhoneCall).target)

        val bengaliBaat = brain.processQuery("রাহুলের সাথে কথা বলিয়ে দাও")
        assertTrue("Should be MakePhoneCall: $bengaliBaat", bengaliBaat is BrainDecision.MakePhoneCall)
        assertEquals("রাহুল", (bengaliBaat as BrainDecision.MakePhoneCall).target)

        // 5. Unspecified recipient (Interactive flow)
        val generic1 = brain.processQuery("call lagao")
        assertTrue("Should request call recipient: $generic1", generic1 is BrainDecision.RequestCallRecipient)

        val generic2 = brain.processQuery("call kro")
        assertTrue("Should request call recipient: $generic2", generic2 is BrainDecision.RequestCallRecipient)

        val generic3 = brain.processQuery("mery baat krwao")
        assertTrue("Should request call recipient: $generic3", generic3 is BrainDecision.RequestCallRecipient)

        val generic4 = brain.processQuery("meri baat karwao")
        assertTrue("Should request call recipient: $generic4", generic4 is BrainDecision.RequestCallRecipient)
    }

    @Test
    fun testQuestionAnsweringAsync() = kotlinx.coroutines.runBlocking {
        // Question about Earth (Knowledge base)
        val ans1 = brain.processQueryAsync("What is the Earth?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue("Earth question must return SpeakResponse: $ans1", ans1 is BrainDecision.SpeakResponse)
        val text1 = (ans1 as BrainDecision.SpeakResponse).text
        assertTrue("Must have meaningful answer: $text1", text1.contains("पृथ्वी") || text1.contains("सर"))

        // Question about Python
        val ans2 = brain.processQueryAsync("Python kya hai?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue("Python question must return SpeakResponse: $ans2", ans2 is BrainDecision.SpeakResponse)
        val text2 = (ans2 as BrainDecision.SpeakResponse).text
        assertTrue("Must address as 'सर': $text2", text2.contains("सर"))
    }

    @Test
    fun testProperStopMuteAndStandbyRouting() {
        // Stop MAX commands
        val stop1 = brain.processQuery("stop max")
        assertTrue("stop max should be TriggerStopMax: $stop1", stop1 is BrainDecision.TriggerStopMax)

        val stop2 = brain.processQuery("stop")
        assertTrue("stop should be TriggerStopMax: $stop2", stop2 is BrainDecision.TriggerStopMax)

        val stop3 = brain.processQuery("ruk jao")
        assertTrue("ruk jao should be TriggerStopMax: $stop3", stop3 is BrainDecision.TriggerStopMax)

        val stop4 = brain.processQuery("thamo")
        assertTrue("thamo should be TriggerStopMax: $stop4", stop4 is BrainDecision.TriggerStopMax)

        // Mute commands
        val mute1 = brain.processQuery("mute")
        assertTrue("mute should be TriggerMute(true): $mute1", mute1 is BrainDecision.TriggerMute && mute1.mute)

        val mute2 = brain.processQuery("mute max")
        assertTrue("mute max should be TriggerMute(true): $mute2", mute2 is BrainDecision.TriggerMute && mute2.mute)

        val mute3 = brain.processQuery("mute voice")
        assertTrue("mute voice should be TriggerMute(true): $mute3", mute3 is BrainDecision.TriggerMute && mute3.mute)

        val mute4 = brain.processQuery("awaz band karo")
        assertTrue("awaz band karo should be TriggerMute(true): $mute4", mute4 is BrainDecision.TriggerMute && mute4.mute)

        // Unmute commands
        val unmute1 = brain.processQuery("unmute")
        assertTrue("unmute should be TriggerMute(false): $unmute1", unmute1 is BrainDecision.TriggerMute && !unmute1.mute)

        val unmute2 = brain.processQuery("unmute max")
        assertTrue("unmute max should be TriggerMute(false): $unmute2", unmute2 is BrainDecision.TriggerMute && !unmute2.mute)

        val unmute3 = brain.processQuery("awaz chalu karo")
        assertTrue("awaz chalu karo should be TriggerMute(false): $unmute3", unmute3 is BrainDecision.TriggerMute && !unmute3.mute)

        // Standby commands
        val standby1 = brain.processQuery("max standby")
        assertTrue("max standby should be EnterStandbyMode: $standby1", standby1 is BrainDecision.EnterStandbyMode)

        val standby2 = brain.processQuery("standby")
        assertTrue("standby should be EnterStandbyMode: $standby2", standby2 is BrainDecision.EnterStandbyMode)

        val standby3 = brain.processQuery("stand by")
        assertTrue("stand by should be EnterStandbyMode: $standby3", standby3 is BrainDecision.EnterStandbyMode)

        val standby4 = brain.processQuery("so jao")
        assertTrue("so jao should be EnterStandbyMode: $standby4", standby4 is BrainDecision.EnterStandbyMode)

        val standby5 = brain.processQuery("sleep mode")
        assertTrue("sleep mode should be EnterStandbyMode: $standby5", standby5 is BrainDecision.EnterStandbyMode)
    }
}
