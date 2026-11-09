package com.max.assistant.humour

import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HumourEngineTest {

    private lateinit var engine: HumourEngine
    private lateinit var brain: MaxAiBrain

    @Before
    fun setUp() {
        engine = HumourEngine(context = null)
        brain = MaxAiBrain(humourEngine = engine)
    }

    @Test
    fun testDefaultLevelIsWitty() {
        assertEquals(HumourLevel.WITTY, engine.currentLevel.value)
    }

    @Test
    fun testLevelProgression() {
        engine.setLevel(HumourLevel.PROFESSIONAL)
        assertEquals(HumourLevel.PROFESSIONAL, engine.currentLevel.value)

        val l2 = engine.increaseLevel()
        assertEquals(HumourLevel.FRIENDLY, l2)

        val l3 = engine.increaseLevel()
        assertEquals(HumourLevel.WITTY, l3)

        val l4 = engine.increaseLevel()
        assertEquals(HumourLevel.SARCASTIC, l4)

        val l5 = engine.increaseLevel()
        assertEquals(HumourLevel.SAVAGE, l5)

        // Savage cannot increase further
        val lMax = engine.increaseLevel()
        assertEquals(HumourLevel.SAVAGE, lMax)

        // Decrease down
        val d4 = engine.decreaseLevel()
        assertEquals(HumourLevel.SARCASTIC, d4)

        val d3 = engine.decreaseLevel()
        assertEquals(HumourLevel.WITTY, d3)

        val d2 = engine.decreaseLevel()
        assertEquals(HumourLevel.FRIENDLY, d2)

        val d1 = engine.decreaseLevel()
        assertEquals(HumourLevel.PROFESSIONAL, d1)

        val dMin = engine.decreaseLevel()
        assertEquals(HumourLevel.PROFESSIONAL, dMin)
    }

    @Test
    fun testVoiceCommandsForLevelAdjustment() {
        // Savage mode command
        val respSavage = engine.handleLevelCommand("activate savage mode", VoiceLanguage.ENGLISH)
        assertNotNull(respSavage)
        assertEquals(HumourLevel.SAVAGE, engine.currentLevel.value)
        assertTrue(respSavage!!.contains("Savage mode", ignoreCase = true))

        // Professional mode command
        val respProf = engine.handleLevelCommand("stop being sarcastic", VoiceLanguage.ENGLISH)
        assertNotNull(respProf)
        assertEquals(HumourLevel.PROFESSIONAL, engine.currentLevel.value)

        // Increase humour command
        val respIncrease = engine.handleLevelCommand("be funnier", VoiceLanguage.ENGLISH)
        assertNotNull(respIncrease)
        assertEquals(HumourLevel.FRIENDLY, engine.currentLevel.value)

        // Witty command
        val respWitty = engine.handleLevelCommand("be witty", VoiceLanguage.ENGLISH)
        assertNotNull(respWitty)
        assertEquals(HumourLevel.WITTY, engine.currentLevel.value)
    }

    @Test
    fun testSeriousSituationOverride() {
        // Medical emergencies
        assertTrue(engine.isSeriousSituation("I am having chest pain, call an ambulance"))
        assertTrue(engine.isSeriousSituation("mujhe saans nahi aa rahi doctor ko bulao"))
        assertTrue(engine.isSeriousSituation("জরুরি রক্তের প্রয়োজন হাসপাতালে যাও"))

        // Accidents and physical danger
        assertTrue(engine.isSeriousSituation("There is a fire in the building help me"))
        assertTrue(engine.isSeriousSituation("bachao aag lag gayi hai"))
        assertTrue(engine.isSeriousSituation("বিপদে পড়েছি বাঁচাও"))

        // Distress & Grief
        assertTrue(engine.isSeriousSituation("I want to die, I am depressed"))
        assertTrue(engine.isSeriousSituation("bahut udaas hoon rona aa raha hai"))
        assertTrue(engine.isSeriousSituation("খুব কষ্ট হচ্ছে মারা গেছে"))

        // Police & Legal
        assertTrue(engine.isSeriousSituation("The police is here and my bank was hacked"))
        assertTrue(engine.isSeriousSituation("paisa chori ho gaya police complaint"))

        // Casual questions should NOT be serious
        assertFalse(engine.isSeriousSituation("are you stupid"))
        assertFalse(engine.isSeriousSituation("tell me a joke"))
        assertFalse(engine.isSeriousSituation("tu pagal hai kya"))
        assertFalse(engine.isSeriousSituation("bored ho raha hoon"))
    }

    @Test
    fun testJokesAndRoastsGeneration() {
        val jokeEn = engine.generateJoke(VoiceLanguage.ENGLISH)
        assertTrue(jokeEn.isNotBlank())

        val jokeHi = engine.generateJoke(VoiceLanguage.HINDI)
        assertTrue(jokeHi.isNotBlank())

        val jokeHinglish = engine.generateJoke(VoiceLanguage.HINGLISH)
        assertTrue(jokeHinglish.isNotBlank())

        val jokeBn = engine.generateJoke(VoiceLanguage.BENGALI)
        assertTrue(jokeBn.isNotBlank())

        val roastEn = engine.generateRoast(VoiceLanguage.ENGLISH)
        assertTrue(roastEn.isNotBlank())

        val roastHi = engine.generateRoast(VoiceLanguage.HINDI)
        assertTrue(roastHi.isNotBlank())

        val roastBn = engine.generateRoast(VoiceLanguage.BENGALI)
        assertTrue(roastBn.isNotBlank())
    }

    @Test
    fun testTeasingComebacksAcrossLevels() {
        // 1. "Are you stupid?"
        engine.setLevel(HumourLevel.WITTY)
        val wittyComeback = engine.generateComeback("are you stupid", VoiceLanguage.ENGLISH)
        assertTrue(wittyComeback.contains("human-inspired confusion", ignoreCase = true))

        engine.setLevel(HumourLevel.SAVAGE)
        val savageComeback = engine.generateComeback("tu pagal hai", VoiceLanguage.HINGLISH)
        assertTrue(savageComeback.contains("hire", ignoreCase = true) || savageComeback.contains("chuna", ignoreCase = true))

        // 2. "I'm bored"
        engine.setLevel(HumourLevel.WITTY)
        val boredComeback = engine.generateComeback("i am bored", VoiceLanguage.ENGLISH)
        assertTrue(boredComeback.contains("emergency", ignoreCase = true) || boredComeback.contains("entertain", ignoreCase = true))

        // 3. "You are useless"
        val uselessComeback = engine.generateComeback("you are useless", VoiceLanguage.ENGLISH)
        assertTrue(uselessComeback.contains("review", ignoreCase = true) || uselessComeback.contains("five stars", ignoreCase = true))

        // 4. "I am tired"
        val tiredComeback = engine.generateComeback("i am tired", VoiceLanguage.ENGLISH)
        assertTrue(tiredComeback.contains("productive", ignoreCase = true) || tiredComeback.contains("workload", ignoreCase = true))
    }

    @Test
    fun testMaxAiBrainEndToEndIntegration() {
        // 1. Level switch command via voice
        val decision1 = brain.processQuery("MAX, activate savage mode", VoiceLanguage.ENGLISH)
        assertTrue(decision1 is BrainDecision.SpeakResponse)
        assertEquals(HumourLevel.SAVAGE, engine.currentLevel.value)

        // 2. Joke command
        val decision2 = brain.processQuery("MAX, tell me a joke", VoiceLanguage.ENGLISH)
        assertTrue(decision2 is BrainDecision.SpeakResponse)
        val jokeText = (decision2 as BrainDecision.SpeakResponse).text
        assertTrue(jokeText.isNotBlank())

        // 3. Roast command
        val decision3 = brain.processQuery("MAX, roast me", VoiceLanguage.ENGLISH)
        assertTrue(decision3 is BrainDecision.SpeakResponse)
        val roastText = (decision3 as BrainDecision.SpeakResponse).text
        assertTrue(roastText.isNotBlank())

        // 4. Teasing comeback
        val decision4 = brain.processQuery("MAX, are you stupid?", VoiceLanguage.ENGLISH)
        assertTrue(decision4 is BrainDecision.SpeakResponse)
        val comebackText = (decision4 as BrainDecision.SpeakResponse).text
        assertTrue(comebackText.isNotBlank())

        // 5. Bengali teasing
        val decision5 = brain.processQuery("ম্যাক্স, তুই পাগল নাকি?", VoiceLanguage.BENGALI)
        assertTrue(decision5 is BrainDecision.SpeakResponse)
        val bnComeback = (decision5 as BrainDecision.SpeakResponse).text
        assertTrue(bnComeback.isNotBlank())

        // 6. Serious situation override should NOT trigger comeback or joke
        val decision6 = brain.processQuery("MAX, I am having a heart attack, help me", VoiceLanguage.ENGLISH)
        // Should not be a humorous comeback
        if (decision6 is BrainDecision.SpeakResponse) {
            assertFalse(decision6.text.contains("stars", ignoreCase = true))
            assertFalse(decision6.text.contains("confusion", ignoreCase = true))
        }
    }

    @Test
    fun testAiSystemInstructionBuilding() {
        engine.setLevel(HumourLevel.SAVAGE)
        val instrSavage = engine.buildSystemInstructionForAi(VoiceLanguage.HINGLISH, isSerious = false)
        assertTrue(instrSavage.contains("SAVAGE", ignoreCase = true))

        // In serious situation, humour must be completely disabled
        val instrSerious = engine.buildSystemInstructionForAi(VoiceLanguage.ENGLISH, isSerious = true)
        assertTrue(instrSerious.contains("CRITICAL SAFETY OVERRIDE", ignoreCase = true))
        assertTrue(instrSerious.contains("NO HUMOUR", ignoreCase = true))
    }
}
