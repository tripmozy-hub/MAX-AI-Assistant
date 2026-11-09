package com.max.assistant.ai

import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaxAiBrainTest {

    private val brain = MaxAiBrain()

    @Test
    fun testRepliesInHindiWhenHindiSettingSelected() {
        val decisionEn = brain.processQuery("What is the time?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(decisionEn is BrainDecision.SpeakResponse)
        val speakEn = decisionEn as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.HINDI, speakEn.language)
        assertTrue("Must address as 'सर' in Hindi: ${speakEn.text}", speakEn.text.contains("सर"))

        val decisionHow = brain.processQuery("How are you?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(decisionHow is BrainDecision.SpeakResponse)
        val speakHow = decisionHow as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.HINDI, speakHow.language)
        assertTrue(speakHow.text.contains("सर"))

        val decisionHi = brain.processQuery("समय क्या है?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(decisionHi is BrainDecision.SpeakResponse)
        val speakHi = decisionHi as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.HINDI, speakHi.language)
        assertTrue(speakHi.text.contains("सर"))
    }

    @Test
    fun testAutoLanguageDetectionAndRespectfulAddress() {
        // English -> English reply with "Sir"
        val decisionEn = brain.processQuery("Who are you?", outputLanguageSetting = VoiceLanguage.AUTO)
        assertTrue(decisionEn is BrainDecision.SpeakResponse)
        val speakEn = decisionEn as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.ENGLISH, speakEn.language)
        assertTrue(speakEn.text.contains("Sir"))

        // Hindi -> Hindi reply with "सर"
        val decisionHi = brain.processQuery("आप कौन हो?", outputLanguageSetting = VoiceLanguage.AUTO)
        assertTrue(decisionHi is BrainDecision.SpeakResponse)
        val speakHi = decisionHi as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.HINDI, speakHi.language)
        assertTrue(speakHi.text.contains("सर"))

        // Bengali -> Bengali reply with "স্যার"
        val decisionBn = brain.processQuery("তুমি কে?", outputLanguageSetting = VoiceLanguage.AUTO)
        assertTrue(decisionBn is BrainDecision.SpeakResponse)
        val speakBn = decisionBn as BrainDecision.SpeakResponse
        assertEquals(VoiceLanguage.BENGALI, speakBn.language)
        assertTrue(speakBn.text.contains("স্যার"))
    }

    @Test
    fun testGeneralKnowledgeAndContextRetention() {
        // 1. Ask about Albert Einstein
        val einsteinResp = brain.processQuery("Who was Albert Einstein?", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue(einsteinResp is BrainDecision.SpeakResponse)
        val einsteinText = (einsteinResp as BrainDecision.SpeakResponse).text
        assertTrue("Contains relativity or physics: $einsteinText", einsteinText.contains("relativity") || einsteinText.contains("physicist"))
        assertEquals("Albert Einstein", brain.knowledgeBase.activeContextTopic)

        // 2. Follow-up: "When was he born?" -> Context memory should correctly answer for Einstein
        val followUp = brain.processQuery("When was he born?", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue(followUp is BrainDecision.SpeakResponse)
        val followUpText = (followUp as BrainDecision.SpeakResponse).text
        assertTrue("Follow-up should state Einstein's birth 1879: $followUpText", followUpText.contains("1879"))

        // 3. World Facts
        val speedLight = brain.processQuery("What is the speed of light?", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue(speedLight is BrainDecision.SpeakResponse)
        assertTrue((speedLight as BrainDecision.SpeakResponse).text.contains("299,792"))

        val largestPlanet = brain.processQuery("Which is the largest planet?", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(largestPlanet is BrainDecision.SpeakResponse)
        assertTrue((largestPlanet as BrainDecision.SpeakResponse).text.contains("बृहस्पति") || (largestPlanet as BrainDecision.SpeakResponse).text.contains("Jupiter"))
    }

    @Test
    fun testGalleryProtectionBlockedAcrossAllLanguages() {
        val decisionEn = brain.processQuery("Open my gallery please", outputLanguageSetting = VoiceLanguage.ENGLISH)
        assertTrue(decisionEn is BrainDecision.SpeakResponse)
        val textEn = (decisionEn as BrainDecision.SpeakResponse).text
        assertTrue(textEn.contains("Sir") && textEn.contains("gallery"))

        val decisionHi = brain.processQuery("फोटो गैलरी खोलो", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(decisionHi is BrainDecision.SpeakResponse)
        val textHi = (decisionHi as BrainDecision.SpeakResponse).text
        assertTrue(textHi.contains("सर") && (textHi.contains("सुरक्षा") || textHi.contains("प्रतिबंधित")))

        val decisionBn = brain.processQuery("গ্যালারি খোলো", outputLanguageSetting = VoiceLanguage.BENGALI)
        assertTrue(decisionBn is BrainDecision.SpeakResponse)
        val textBn = (decisionBn as BrainDecision.SpeakResponse).text
        assertTrue(textBn.contains("স্যার") && (textBn.contains("গ্যালারি") || textBn.contains("নিষিদ্ধ")))
    }

    @Test
    fun testBatteryAndNetworkQueries() {
        assertTrue(brain.processQuery("Check battery percentage") is BrainDecision.QueryBattery)
        assertTrue(brain.processQuery("बैटरी कितनी है?") is BrainDecision.QueryBattery)
        assertTrue(brain.processQuery("Are we connected to internet?") is BrainDecision.QueryNetwork)
        assertTrue(brain.processQuery("इंटरनेट स्टेटस क्या है?") is BrainDecision.QueryNetwork)
    }

    @Test
    fun testAlarmCommandParsing() {
        val alarmDecision = brain.processQuery("set an alarm for 7:30 am")
        assertTrue(alarmDecision is BrainDecision.SetAlarm)
        val alarm = alarmDecision as BrainDecision.SetAlarm
        assertEquals(7, alarm.hour)
        assertEquals(30, alarm.minute)

        val alarmPm = brain.processQuery("set alarm 6 pm")
        assertTrue(alarmPm is BrainDecision.SetAlarm)
        assertEquals(18, (alarmPm as BrainDecision.SetAlarm).hour)
    }

    @Test
    fun testPhoneCallingAndMessagingCommands() {
        val call1 = brain.processQuery("call Rahul")
        assertTrue(call1 is BrainDecision.MakePhoneCall)
        val cp1 = call1 as BrainDecision.MakePhoneCall
        assertEquals("Rahul", cp1.target)
        assertTrue("Phone calls must require confirmation", cp1.requiresConfirmation)

        val msg1 = brain.processQuery("send message to Rahul: I am on the way")
        assertTrue(msg1 is BrainDecision.SendMessage)
        val sm1 = msg1 as BrainDecision.SendMessage
        assertEquals("Rahul", sm1.recipient)
        assertEquals("I am on the way", sm1.message)
        assertTrue("Sending messages must require confirmation", sm1.requiresConfirmation)
    }

    @Test
    fun testMathCalculation() {
        val mathDecision = brain.processQuery("calculate 25 times 4", outputLanguageSetting = VoiceLanguage.HINDI)
        assertTrue(mathDecision is BrainDecision.SpeakResponse)
        val speakMath = mathDecision as BrainDecision.SpeakResponse
        assertTrue("Should calculate 100 in Hindi: ${speakMath.text}", speakMath.text.contains("100") && speakMath.text.contains("सर"))
    }

    @Test
    fun testStopAndMuteCommands() {
        assertTrue(brain.processQuery("stop max") is BrainDecision.TriggerStopMax)
        assertTrue(brain.processQuery("ruk jao") is BrainDecision.TriggerStopMax)

        val mute = brain.processQuery("mute microphone")
        assertTrue(mute is BrainDecision.TriggerMute && mute.mute)

        val unmute = brain.processQuery("unmute")
        assertTrue(unmute is BrainDecision.TriggerMute && !unmute.mute)
    }

    @Test
    fun testEverydayTimerCommands() {
        val timer1 = brain.processQuery("set a timer for 5 minutes")
        assertTrue("Expected SetTimer: $timer1", timer1 is BrainDecision.SetTimer)
        assertEquals(300, (timer1 as BrainDecision.SetTimer).seconds)

        val timer2 = brain.processQuery("10 मिनट का टाइमर लगाओ")
        assertTrue("Expected SetTimer: $timer2", timer2 is BrainDecision.SetTimer)
        assertEquals(600, (timer2 as BrainDecision.SetTimer).seconds)
    }

    @Test
    fun testEverydayMediaControls() {
        val play = brain.processQuery("play music")
        assertTrue("Expected PlayMusic: $play", play is BrainDecision.PlayMusic)
        assertEquals("spotify", (play as BrainDecision.PlayMusic).app)

        val pause = brain.processQuery("pause music")
        assertTrue(pause is BrainDecision.ControlMedia)
        assertEquals(com.max.assistant.executor.MediaAction.PAUSE, (pause as BrainDecision.ControlMedia).action)

        val next = brain.processQuery("next song")
        assertTrue(next is BrainDecision.ControlMedia)
        assertEquals(com.max.assistant.executor.MediaAction.NEXT, (next as BrainDecision.ControlMedia).action)

        val stopHindi = brain.processQuery("गाना बंद करो")
        assertTrue(stopHindi is BrainDecision.ControlMedia)
        assertEquals(com.max.assistant.executor.MediaAction.STOP, (stopHindi as BrainDecision.ControlMedia).action)
    }

    @Test
    fun testEmotionalSupportAndMotivation() {
        val lowMoodHindi = brain.processQuery("max acha nahi lg raha hai")
        assertTrue("Expected OfferEmotionalSupport: $lowMoodHindi", lowMoodHindi is BrainDecision.OfferEmotionalSupport)

        val moodKhrb = brain.processQuery("mood khrb hai")
        assertTrue("Expected OfferEmotionalSupport: $moodKhrb", moodKhrb is BrainDecision.OfferEmotionalSupport)

        val feelingDown = brain.processQuery("feeling down today")
        assertTrue("Expected OfferEmotionalSupport: $feelingDown", feelingDown is BrainDecision.OfferEmotionalSupport)

        val bengaliMood = brain.processQuery("আজ মন ভালো নেই")
        assertTrue("Expected OfferEmotionalSupport: $bengaliMood", bengaliMood is BrainDecision.OfferEmotionalSupport)

        val motivateMe = brain.processQuery("motivate me")
        assertTrue("Expected SpeakResponse: $motivateMe", motivateMe is BrainDecision.SpeakResponse)

        val motivateHindi = brain.processQuery("हौसला बढ़ाओ")
        assertTrue("Expected SpeakResponse: $motivateHindi", motivateHindi is BrainDecision.SpeakResponse)
    }

    @Test
    fun testMusicAndSpotifyCommands() {
        val spotifyLaunch = brain.processQuery("spotify chalao")
        assertTrue("Expected PlayMusic: $spotifyLaunch", spotifyLaunch is BrainDecision.PlayMusic)

        val playArtist = brain.processQuery("play Arijit Singh on spotify")
        assertTrue("Expected PlayMusic: $playArtist", playArtist is BrainDecision.PlayMusic)
        val p = playArtist as BrainDecision.PlayMusic
        assertEquals("spotify", p.app)
        assertEquals("Arijit Singh", p.query)

        val ganaChalao = brain.processQuery("gana chalao")
        assertTrue("Expected PlayMusic: $ganaChalao", ganaChalao is BrainDecision.PlayMusic)
    }

    @Test
    fun testDirectAppLaunchingAndSms() {
        val directSpotify = brain.processQuery("spotify")
        assertTrue("Expected PlayMusic: $directSpotify", directSpotify is BrainDecision.PlayMusic)
        assertEquals("spotify", (directSpotify as BrainDecision.PlayMusic).app)

        val directCamera = brain.processQuery("camera")
        assertTrue("Expected OpenCamera: $directCamera", directCamera is BrainDecision.OpenCamera)

        val directCalculator = brain.processQuery("calculator")
        assertTrue("Expected LaunchApp: $directCalculator", directCalculator is BrainDecision.LaunchApp)

        val smsCommand = brain.processQuery("send sms to Rahul hello how are you")
        assertTrue("Expected SendMessage: $smsCommand", smsCommand is BrainDecision.SendMessage)
        val s = smsCommand as BrainDecision.SendMessage
        assertEquals("Rahul", s.recipient)
        assertEquals("hello how are you", s.message)
    }

    @Test
    fun testNearbyAndDirectionsCommands() {
        val nearbyHospital = brain.processQuery("find nearby hospital")
        assertTrue(nearbyHospital is BrainDecision.SearchNearby)
        assertEquals("hospital", (nearbyHospital as BrainDecision.SearchNearby).category)

        val nearbyHindi = brain.processQuery("पास का पेट्रोल पंप दिखाओ")
        assertTrue(nearbyHindi is BrainDecision.SearchNearby)

        val directions = brain.processQuery("directions to Kolkata")
        assertTrue(directions is BrainDecision.OpenDirections)
        assertTrue((directions as BrainDecision.OpenDirections).destination.contains("Kolkata", ignoreCase = true))

        val directionsHindi = brain.processQuery("सिलिगुड़ी का रास्ता बताओ")
        assertTrue(directionsHindi is BrainDecision.OpenDirections)
        assertTrue((directionsHindi as BrainDecision.OpenDirections).destination.contains("सिलिगुड़ी"))
    }

    @Test
    fun testMultiStepTaskPlanParsing() {
        val multi = brain.processQuery("find nearby restaurant and open directions")
        assertTrue("Expected ExecuteMultiStepPlan: $multi", multi is BrainDecision.ExecuteMultiStepPlan)
        val planDecision = multi as BrainDecision.ExecuteMultiStepPlan
        assertEquals(2, planDecision.plan.steps.size)
        assertTrue(planDecision.plan.steps[0].decision is BrainDecision.SearchNearby)
        assertTrue(planDecision.plan.steps[1].decision is BrainDecision.OpenDirections)
    }

    @Test
    fun testNotesAndTasksParsing() {
        val note = brain.processQuery("take a note: Meeting with client at 4 PM")
        assertTrue("Expected CreateNote: $note", note is BrainDecision.CreateNote)
        assertTrue((note as BrainDecision.CreateNote).content.contains("Meeting with client at 4 PM"))

        val readNotes = brain.processQuery("read my notes")
        assertTrue("Expected ReadNotes: $readNotes", readNotes is BrainDecision.ReadNotes)

        val addTask = brain.processQuery("add task: buy groceries tomorrow")
        assertTrue("Expected AddTask: $addTask", addTask is BrainDecision.AddTask)
        assertTrue((addTask as BrainDecision.AddTask).title.contains("buy groceries tomorrow"))

        val readTasks = brain.processQuery("what are my tasks?")
        assertTrue("Expected ReadTasks: $readTasks", readTasks is BrainDecision.ReadTasks)
    }

    @Test
    fun testDraftEmailAndFindContact() {
        val draft = brain.processQuery("send email to john@example.com about Project Update")
        assertTrue("Expected DraftEmail: $draft", draft is BrainDecision.DraftEmail)
        val d = draft as BrainDecision.DraftEmail
        assertEquals("john@example.com", d.recipient)
        assertTrue(d.subject.contains("Project Update"))

        val contact = brain.processQuery("find contact Rahul")
        assertTrue("Expected FindContact: $contact", contact is BrainDecision.FindContact)
        assertEquals("Rahul", (contact as BrainDecision.FindContact).name)
    }

    @Test
    fun testSettingsSubpagesParsing() {
        val wifi = brain.processQuery("open wifi settings")
        assertTrue("Expected OpenSettingsSubpage: $wifi", wifi is BrainDecision.OpenSettingsSubpage)
        assertEquals("wifi", (wifi as BrainDecision.OpenSettingsSubpage).subpage)

        val bluetooth = brain.processQuery("open bluetooth settings")
        assertTrue(bluetooth is BrainDecision.OpenSettingsSubpage)
        assertEquals("bluetooth", (bluetooth as BrainDecision.OpenSettingsSubpage).subpage)

        val display = brain.processQuery("open display settings")
        assertTrue(display is BrainDecision.OpenSettingsSubpage)
        assertEquals("display", (display as BrainDecision.OpenSettingsSubpage).subpage)
    }

    @Test
    fun testStudyModeParsing() {
        val notes = brain.processQuery("make notes on Indian history")
        assertTrue("Expected CreateStudyNotes: $notes", notes is BrainDecision.CreateStudyNotes)
        val n = notes as BrainDecision.CreateStudyNotes
        assertTrue(n.topic.contains("Indian history", ignoreCase = true))

        val quiz = brain.processQuery("create 20 ssc questions")
        assertTrue("Expected StartStudyQuiz: $quiz", quiz is BrainDecision.StartStudyQuiz)
        val q = quiz as BrainDecision.StartStudyQuiz
        assertEquals("SSC", q.examType)

        val ans = brain.processQuery("option b")
        assertTrue("Expected AnswerStudyQuiz: $ans", ans is BrainDecision.AnswerStudyQuiz)
        assertEquals("option b", (ans as BrainDecision.AnswerStudyQuiz).answer)

        val math = brain.processQuery("solve 2x + 5 = 15")
        assertTrue("Expected SolveMath: $math", math is BrainDecision.SolveMath)
        assertTrue((math as BrainDecision.SolveMath).problem.contains("2x + 5 = 15"))

        val grammar = brain.processQuery("grammar check: He go to school")
        assertTrue("Expected ExplainGrammar: $grammar", grammar is BrainDecision.ExplainGrammar)
    }

    @Test
    fun testScreenVisionParsing() {
        val visionHinglish = brain.processQuery("kya tum ye dekh rahe ho")
        assertTrue("Expected CaptureScreenVision: $visionHinglish", visionHinglish is BrainDecision.CaptureScreenVision)

        val visionRead = brain.processQuery("read my screen")
        assertTrue("Expected CaptureScreenVision: $visionRead", visionRead is BrainDecision.CaptureScreenVision)

        val stopVision = brain.processQuery("stop watching my screen")
        assertTrue("Expected StopScreenVision: $stopVision", stopVision is BrainDecision.StopScreenVision)
    }

    @Test
    fun testSmartRemindersParsing() {
        val reminder = brain.processQuery("remind me to call Rahul tomorrow at 5 PM")
        assertTrue("Expected ScheduleSmartReminder: $reminder", reminder is BrainDecision.ScheduleSmartReminder)
        val r = reminder as BrainDecision.ScheduleSmartReminder
        assertTrue("Title should contain call Rahul: ${r.title}", r.title.contains("call Rahul", ignoreCase = true))

        val list = brain.processQuery("show upcoming reminders")
        assertTrue("Expected ListSmartReminders: $list", list is BrainDecision.ListSmartReminders)
    }

    @Test
    fun testStandbyAndShutdownParsing() {
        val standby = brain.processQuery("MAX standby ho jao")
        assertTrue("Expected EnterStandbyMode: $standby", standby is BrainDecision.EnterStandbyMode)

        val shutdown = brain.processQuery("full shutdown")
        assertTrue("Expected ExecuteFullShutdown: $shutdown", shutdown is BrainDecision.ExecuteFullShutdown)
    }
}
