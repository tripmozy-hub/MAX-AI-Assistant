package com.max.assistant.e2e

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.*
import org.junit.Test

class TenCommandsVerificationTest {

    private val brain = MaxAiBrain()

    @Test
    fun verifyCommand1_whatTimeIsIt() {
        val decision = brain.processQuery("Hey MAX, what time is it?")
        assertTrue(decision is BrainDecision.SpeakResponse)
        val text = (decision as BrainDecision.SpeakResponse).text
        assertTrue("Addresses Sir: $text", text.contains("Sir"))
        assertTrue("Mentions time: $text", text.contains("time"))
    }

    @Test
    fun verifyCommand2_openChrome() {
        val decision = brain.processQuery("MAX, open Chrome")
        assertTrue("Resolves to OpenBrowser: $decision", decision is BrainDecision.OpenBrowser)
    }

    @Test
    fun verifyCommand3_openSettings() {
        val decision = brain.processQuery("MAX, open Settings")
        assertTrue("Resolves to OpenSettings: $decision", decision is BrainDecision.OpenSettings)
    }

    @Test
    fun verifyCommand4_openCamera() {
        val decision = brain.processQuery("MAX, open Camera")
        assertTrue("Resolves to OpenCamera: $decision", decision is BrainDecision.OpenCamera)
    }

    @Test
    fun verifyCommand5_batteryPercentage() {
        val decision = brain.processQuery("MAX, tell me the battery percentage")
        assertTrue("Resolves to QueryBattery: $decision", decision is BrainDecision.QueryBattery)
    }

    @Test
    fun verifyCommand6_setAlarmFor7AM() {
        val decision = brain.processQuery("MAX, set an alarm for 7 AM")
        assertTrue("Resolves to SetAlarm: $decision", decision is BrainDecision.SetAlarm)
        val alarm = decision as BrainDecision.SetAlarm
        assertEquals(7, alarm.hour)
        assertEquals(0, alarm.minute)
    }

    @Test
    fun verifyCommand7_explainWeatherInHindi() {
        val decision = brain.processQuery("MAX, explain the weather in Hindi")
        assertTrue("Resolves to QueryWeather: $decision", decision is BrainDecision.QueryWeather)
        val weather = decision as BrainDecision.QueryWeather
        assertEquals(VoiceLanguage.HINDI, weather.language)
    }

    @Test
    fun verifyCommand8_banglaSwitch() {
        val decision = brain.processQuery("MAX, বাংলায় কথা বলো")
        assertTrue("Resolves to SwitchLanguage: $decision", decision is BrainDecision.SwitchLanguage)
        val sw = decision as BrainDecision.SwitchLanguage
        assertEquals(VoiceLanguage.BENGALI, sw.newLanguage)
        assertTrue(sw.message.contains("স্যার"))
    }

    @Test
    fun verifyCommand9_explainArtificialIntelligence() {
        val decision = brain.processQuery("MAX, explain artificial intelligence")
        assertTrue("Resolves to SpeakResponse: $decision", decision is BrainDecision.SpeakResponse)
        val text = (decision as BrainDecision.SpeakResponse).text
        assertTrue("Mentions intelligence/machines: $text", text.contains("intelligence") || text.contains("machines") || text.contains("problem-solving"))
    }

    @Test
    fun verifyCommand10_stop() {
        val decision = brain.processQuery("MAX, stop")
        assertTrue("Resolves to TriggerStopMax: $decision", decision is BrainDecision.TriggerStopMax)
    }
}
