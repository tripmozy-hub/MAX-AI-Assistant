package com.max.assistant.e2e

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.*
import org.junit.Test

class TwelveCommandsVerificationTest {

    private val brain = MaxAiBrain()

    @Test
    fun testCommand1_openWhatsApp() {
        val decision = brain.processQuery("Hey MAX, open WhatsApp.")
        assertTrue("Command 1 must resolve to OpenWhatsApp: $decision", decision is BrainDecision.OpenWhatsApp)
    }

    @Test
    fun testCommand2_openChrome() {
        val decision = brain.processQuery("Hey MAX, open Chrome.")
        assertTrue("Command 2 must resolve to OpenBrowser: $decision", decision is BrainDecision.OpenBrowser)
    }

    @Test
    fun testCommand3_openSettings() {
        val decision = brain.processQuery("Hey MAX, open Settings.")
        assertTrue("Command 3 must resolve to OpenSettings: $decision", decision is BrainDecision.OpenSettings)
    }

    @Test
    fun testCommand4_callRahul() {
        val decision = brain.processQuery("Hey MAX, call Rahul.")
        assertTrue("Command 4 must resolve to MakePhoneCall: $decision", decision is BrainDecision.MakePhoneCall)
        val call = decision as BrainDecision.MakePhoneCall
        assertEquals("Rahul", call.target)
        assertTrue("Calling must require confirmation", call.requiresConfirmation)
    }

    @Test
    fun testCommand5_weatherInSiliguri() {
        val decision = brain.processQuery("Hey MAX, what is the weather in Siliguri?")
        assertTrue("Command 5 must resolve to QueryWeather: $decision", decision is BrainDecision.QueryWeather)
        val weather = decision as BrainDecision.QueryWeather
        assertEquals("Siliguri", weather.location)
    }

    @Test
    fun testCommand6_searchInternetForHotelsInGoa() {
        val decision = brain.processQuery("Hey MAX, search the internet for hotels in Goa.")
        assertTrue("Command 6 must resolve to SearchWeb: $decision", decision is BrainDecision.SearchWeb)
        val search = decision as BrainDecision.SearchWeb
        assertTrue("Search query must contain hotels in Goa: ${search.query}", search.query.contains("hotels in Goa", ignoreCase = true))
    }

    @Test
    fun testCommand7_explainArtificialIntelligence() {
        val decision = brain.processQuery("Hey MAX, explain artificial intelligence.")
        assertTrue("Command 7 must resolve to SpeakResponse: $decision", decision is BrainDecision.SpeakResponse)
        val text = (decision as BrainDecision.SpeakResponse).text
        assertTrue("Explanation addresses user as Sir: $text", text.contains("Sir"))
        assertTrue("Explanation mentions intelligence/machines/systems: $text", text.contains("intelligence") || text.contains("machines") || text.contains("problem-solving"))
    }

    @Test
    fun testCommand8_nowSpeakBengali() {
        val decision = brain.processQuery("Hey MAX, এখন বাংলায় কথা বলো।")
        assertTrue("Command 8 must resolve to SwitchLanguage: $decision", decision is BrainDecision.SwitchLanguage)
        val sw = decision as BrainDecision.SwitchLanguage
        assertEquals(VoiceLanguage.BENGALI, sw.newLanguage)
        assertTrue("Bengali greeting must contain 'স্যার': ${sw.message}", sw.message.contains("স্যার"))
    }

    @Test
    fun testCommand9_nowSpeakHindi() {
        val decision = brain.processQuery("Hey MAX, अब हिंदी में बात करो।")
        assertTrue("Command 9 must resolve to SwitchLanguage: $decision", decision is BrainDecision.SwitchLanguage)
        val sw = decision as BrainDecision.SwitchLanguage
        assertEquals(VoiceLanguage.HINDI, sw.newLanguage)
        assertTrue("Hindi greeting must contain 'सर': ${sw.message}", sw.message.contains("सर"))
    }

    @Test
    fun testCommand10_stopSpeaking() {
        val decision = brain.processQuery("Hey MAX, stop speaking.")
        assertTrue("Command 10 must resolve to StopSpeaking: $decision", decision is BrainDecision.StopSpeaking)
    }

    @Test
    fun testCommand11_openGoogleMaps() {
        val decision = brain.processQuery("Hey MAX, open Google Maps.")
        assertTrue("Command 11 must resolve to OpenMaps: $decision", decision is BrainDecision.OpenMaps)
    }

    @Test
    fun testCommand12_batteryPercentage() {
        val decision = brain.processQuery("Hey MAX, what is my battery percentage?")
        assertTrue("Command 12 must resolve to QueryBattery: $decision", decision is BrainDecision.QueryBattery)
    }
}
