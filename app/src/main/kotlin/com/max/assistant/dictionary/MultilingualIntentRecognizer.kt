package com.max.assistant.dictionary

import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.util.LanguageDetector
import java.util.Calendar
import java.util.Locale

/**
 * MultilingualIntentRecognizer maps voice inputs in English, Hindi/Hinglish, and Bengali
 * into structured RecognizedIntent objects with dynamic parameter extraction and context awareness.
 */
class MultilingualIntentRecognizer(
    private val contextManager: ConversationContextManager = ConversationContextManager.instance
) {

    companion object {
        private const val TAG = "IntentRecognizer"
    }

    fun recognize(input: String, languagePreference: VoiceLanguage = VoiceLanguage.AUTO): RecognizedIntent {
        val trimmed = input.trim().trim('\'', '"')
        val lower = trimmed.lowercase(Locale.ROOT)
        val cleanLower = lower.replace(Regex("^(hey\\s+max|ok\\s+max|hello\\s+max|max|मैक्स|ম্যাক্স|হেই\\s+ম্যাক্স|হে\\s+ম্যাক্স)[,\\s:]*"), "")
            .trimStart(',', ' ', ':', '-', ';', '.', '\'', '"', '।')
            .trimEnd(',', ' ', ':', '-', ';', '.', '?', '!', '\'', '"', '।')
            .trim()

        val detectedLang = if (languagePreference != VoiceLanguage.AUTO) {
            languagePreference
        } else {
            LanguageDetector.detectLanguage(trimmed)
        }

        // 1. Check if input is a pure wake word or greeting (AI001)
        if (cleanLower.isEmpty() || cleanLower == "max" || lower in setOf("hey max", "hello max", "hi max", "max", "मैक्स", "ম্যাক্স", "হেই ম্যাক্স", "হে ম্যাক্স")) {
            return RecognizedIntent(
                intentId = "AI001",
                category = CommandCategory.GENERAL_AI,
                rawQuery = trimmed,
                detectedLanguage = detectedLang,
                definition = CommandDictionary.findDefinitionById("AI001")
            )
        }

        // 1b. Check for explicit web searches before travel category hijacking ("search the internet for...")
        if (cleanLower.contains("search the internet") || cleanLower.contains("search internet") || cleanLower.contains("google search")) {
            val q = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                .replace(Regex("(?i)^(search\\s+the\\s+internet\\s+for|search\\s+internet\\s+for|search\\s+google\\s+for|search\\s+for|search\\s+)"), "")
                .trimEnd('.', '?', '!', ' ')
                .trim()
            return RecognizedIntent(
                intentId = "WEB001",
                category = CommandCategory.WEB_CHROME,
                rawQuery = trimmed,
                detectedLanguage = detectedLang,
                parameters = mapOf("query" to q),
                definition = CommandDictionary.findDefinitionById("WEB001")
            )
        }

        // 1c. Check Baby Entertainment Mode commands (prioritize over generic 'stop' phrase matching)
        val babyIntent = recognizeBabyIntent(cleanLower, trimmed, detectedLang)
        if (babyIntent != null) {
            return babyIntent
        }

        // 2. Check if user is supplying a missing parameter from previous turn
        val pendingParam = contextManager.pendingMissingParameter
        if (pendingParam != null && cleanLower.isNotBlank()) {
            val (intentId, paramName) = pendingParam
            contextManager.clearAwaitingParameter()
            val def = CommandDictionary.findDefinitionById(intentId)
            if (def != null) {
                return RecognizedIntent(
                    intentId = intentId,
                    category = def.category,
                    rawQuery = trimmed,
                    detectedLanguage = detectedLang,
                    parameters = mapOf(paramName to trimmed),
                    definition = def
                )
            }
        }

        // 3. Check for contextual follow-up (e.g. YouTube search)
        val followUp = contextManager.resolveContextualFollowUp(cleanLower)
        if (followUp != null) {
            val (app, query) = followUp
            if (app == "youtube") {
                return RecognizedIntent(
                    intentId = "PHN001",
                    category = CommandCategory.PHONE_CONTROL,
                    rawQuery = trimmed,
                    detectedLanguage = detectedLang,
                    parameters = mapOf("app" to "youtube", "query" to query),
                    definition = CommandDictionary.findDefinitionById("PHN001")
                )
            }
        }

        // 4. Match against Master Command Dictionary definitions
        for (def in CommandDictionary.getAllDefinitions()) {
            if (def.intentId == "AI001") continue

            val matches = when (detectedLang) {
                VoiceLanguage.BENGALI -> matchesAnyPhrase(cleanLower, lower, def.bengaliPhrases) || matchesAnyPhrase(cleanLower, lower, def.englishPhrases)
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> matchesAnyPhrase(cleanLower, lower, def.hindiPhrases) || matchesAnyPhrase(cleanLower, lower, def.englishPhrases)
                else -> matchesAnyPhrase(cleanLower, lower, def.englishPhrases) || matchesAnyPhrase(cleanLower, lower, def.hindiPhrases) || matchesAnyPhrase(cleanLower, lower, def.bengaliPhrases)
            }

            if (matches) {
                val params = extractParametersForIntent(def, cleanLower, trimmed)
                val missing = def.requiredParameters.filter { !params.containsKey(it) || params[it].isNullOrBlank() }

                return RecognizedIntent(
                    intentId = def.intentId,
                    category = def.category,
                    rawQuery = trimmed,
                    detectedLanguage = detectedLang,
                    parameters = params,
                    missingParameters = missing,
                    definition = def
                )
            }
        }

        // 4. Dynamic Semantic Fallback Matcher (handles flexible sentence structures)
        val dynamicIntent = recognizeDynamicIntent(cleanLower, trimmed, detectedLang)
        if (dynamicIntent != null) {
            return dynamicIntent
        }

        // 5. Default General AI Conversational Intent
        return RecognizedIntent(
            intentId = "AI002",
            category = CommandCategory.GENERAL_AI,
            rawQuery = trimmed,
            detectedLanguage = detectedLang,
            parameters = mapOf("query" to trimmed),
            definition = CommandDictionary.findDefinitionById("AI002")
        )
    }

    private fun matchesAnyPhrase(cleanLower: String, lower: String, phrases: List<String>): Boolean {
        for (phrase in phrases) {
            val p = phrase.lowercase(Locale.ROOT)
            if (cleanLower == p || cleanLower.startsWith("$p ") || cleanLower.endsWith(" $p") || cleanLower.contains(" $p ") ||
                lower == p || lower.startsWith("$p ") || lower.endsWith(" $p") || lower.contains(" $p ")
            ) {
                return true
            }
        }
        return false
    }

    private fun extractParametersForIntent(def: CommandDefinition, cleanLower: String, original: String): Map<String, String> {
        val map = mutableMapOf<String, String>()

        when (def.intentId) {
            // Reminders & Clock
            "REM001", "CLK001" -> {
                extractTime(cleanLower)?.let { map["time"] = it }
            }
            "REM002" -> {
                extractContactName(cleanLower, original)?.let { map["contact"] = it }
                map["task"] = "call ${map["contact"] ?: "someone"}"
            }
            "REM005" -> {
                map["repeat"] = "WEEKLY"
                map["day"] = "Sunday"
            }
            "CLK004" -> {
                extractDuration(cleanLower)?.let { map["duration"] = it }
            }
            "CLK006" -> {
                map["title"] = "Meeting"
                map["date"] = "tomorrow"
            }
            // Messaging & Calls
            "MSG001", "MSG002", "CALL001", "CALL002", "CALL004" -> {
                extractContactName(cleanLower, original)?.let { map["contact"] = it }
                extractMessageBody(cleanLower, original)?.let { map["message"] = it }
            }
            // Notes & Documents
            "NOTE001", "NOTE004" -> {
                val content = original.replace(Regex("(?i)^(create a new note:?|naya note banao:?|mere history notes dhundo:?|find my history notes:?)"), "").trim()
                if (content.isNotBlank()) {
                    map["content"] = content
                    map["query"] = content
                }
            }
            // Education & Study
            "EDU003", "EDU006" -> {
                val topic = original.replace(Regex("(?i)^(make notes on|history ke notes banao|explain this chapter:?)"), "").trim()
                map["topic"] = if (topic.isNotBlank()) topic else "History"
                map["chapter"] = if (topic.isNotBlank()) topic else "Chapter 1"
            }
            // Web & Travel
            "WEB001", "WEB004", "WEB005" -> {
                val q = original.replace(Regex("(?i)^(search google for|isko google par search karo|find the official website for|open this website:?)"), "").trim()
                map["query"] = q
                map["url"] = q
            }
            "TRV001", "TRV002", "TRV003" -> {
                if (cleanLower.contains("darjeeling")) map["location"] = "Darjeeling"
                if (cleanLower.contains("kolkata")) map["destination"] = "Kolkata"
            }
        }
        return map
    }

    private fun recognizeDynamicIntent(cleanLower: String, original: String, lang: VoiceLanguage): RecognizedIntent? {
        // SET_ALARM
        if (cleanLower.contains("alarm") || cleanLower.contains("अलार्म") || cleanLower.contains("অ্যালার্ম")) {
            val time = extractTime(cleanLower)
            val params = if (time != null) mapOf("time" to time) else emptyMap()
            val missing = if (time == null) listOf("time") else emptyList()
            return RecognizedIntent("CLK001", CommandCategory.CLOCK_CALENDAR, original, lang, params, missing, CommandDictionary.findDefinitionById("CLK001"))
        }

        // CREATE_REMINDER
        if (cleanLower.contains("remind") || cleanLower.contains("yaad dilana") || cleanLower.contains("মনে করিয়ে")) {
            val contact = extractContactName(cleanLower, original)
            val time = extractTime(cleanLower)
            val params = mutableMapOf<String, String>()
            if (contact != null) params["contact"] = contact
            if (time != null) params["time"] = time
            params["task"] = original
            return RecognizedIntent("REM001", CommandCategory.REMINDERS, original, lang, params, emptyList(), CommandDictionary.findDefinitionById("REM001"))
        }

        // MAKE_CALL
        if (cleanLower.startsWith("call ") || cleanLower.contains(" ko call") || cleanLower.contains("কে ফোন") || cleanLower.contains("ফোন করো")) {
            val contact = extractContactName(cleanLower, original)
            val params = if (contact != null) mapOf("contact" to contact) else emptyMap()
            val missing = if (contact == null) listOf("contact") else emptyList()
            return RecognizedIntent("CALL001", CommandCategory.CALLS_CONTACTS, original, lang, params, missing, CommandDictionary.findDefinitionById("CALL001"))
        }

        // SEND_MESSAGE
        if (cleanLower.contains("message") || cleanLower.contains("sms") || cleanLower.contains("whatsapp") || cleanLower.contains("মেসেজ")) {
            val contact = extractContactName(cleanLower, original)
            val msg = extractMessageBody(cleanLower, original)
            val params = mutableMapOf<String, String>()
            if (contact != null) params["contact"] = contact
            if (msg != null) params["message"] = msg
            val id = if (cleanLower.contains("whatsapp")) "MSG001" else "MSG002"
            return RecognizedIntent(id, CommandCategory.MESSAGING, original, lang, params, emptyList(), CommandDictionary.findDefinitionById(id))
        }

        // OPEN_APP
        if (cleanLower.startsWith("open ") || cleanLower.contains("kholo") || cleanLower.contains("খোলো")) {
            val app = cleanLower.removePrefix("open ").replace("kholo", "").replace("খোলো", "").trim()
            return RecognizedIntent("PHN001", CommandCategory.PHONE_CONTROL, original, lang, mapOf("app" to app), emptyList(), CommandDictionary.findDefinitionById("PHN001"))
        }

        // CALCULATE
        val isBatteryOrNetwork = cleanLower.contains("battery") || cleanLower.contains("charge") || cleanLower.contains("charging") ||
                cleanLower.contains("बैटरी") || cleanLower.contains("चार्ज") || cleanLower.contains("ব্যাটারি")
        if (!isBatteryOrNetwork && (cleanLower.contains("calculate") || cleanLower.contains("percent of") || cleanLower.contains("pratishat") || cleanLower.contains("শতাংশ") || cleanLower.contains("+") || cleanLower.contains("*") || cleanLower.contains("/"))) {
            return RecognizedIntent("CAL001", CommandCategory.CALCULATIONS, original, lang, mapOf("expression" to cleanLower), emptyList(), CommandDictionary.findDefinitionById("CAL001"))
        }

        // WEB_SEARCH
        if (cleanLower.startsWith("search ") || cleanLower.contains("search karo") || cleanLower.contains("সার্চ করো")) {
            val q = original.replace(Regex("(?i)^(search\\s+|google\\s+search\\s+|search\\s+karo\\s+)"), "").trim()
            return RecognizedIntent("WEB001", CommandCategory.WEB_CHROME, original, lang, mapOf("query" to q), emptyList(), CommandDictionary.findDefinitionById("WEB001"))
        }

        // BABY ENTERTAINMENT MODE
        if (cleanLower.contains("baby is crying") || cleanLower.contains("baccha ro raha") || cleanLower.contains("bacha ro raha") ||
            cleanLower.contains("বাচ্চা কাঁদছে") || cleanLower.contains("distract the baby") || cleanLower.contains("dhyan bhatkao") ||
            cleanLower.contains("বাচ্চার মনোযোগ")
        ) {
            return RecognizedIntent("BABY001", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY001"))
        }

        if (cleanLower.contains("stop baby mode") || cleanLower.contains("baby mode band") || cleanLower.contains("বেবি মোড বন্ধ")) {
            return RecognizedIntent("BABY008", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY008"))
        }

        if ((cleanLower.contains("baby") || cleanLower.contains("bacche") || cleanLower.contains("বাচ্চা")) &&
            (cleanLower.contains("entertain") || cleanLower.contains("play") || cleanLower.contains("আনন্দ"))
        ) {
            return RecognizedIntent("BABY002", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY002"))
        }

        if (cleanLower.contains("make the baby laugh") || cleanLower.contains("hansaao") || cleanLower.contains("hasao") ||
            cleanLower.contains("হাসাও") || cleanLower.contains("kuch funny karo")
        ) {
            return RecognizedIntent("BABY003", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY003"))
        }

        if (cleanLower.contains("help the baby sleep") || cleanLower.contains("lori sunao") || cleanLower.contains("play a lullaby") ||
            cleanLower.contains("ঘুম পাড়ানোর গান") || cleanLower.contains("lullaby")
        ) {
            return RecognizedIntent("BABY004", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY004"))
        }

        if (cleanLower.contains("tell the baby a story") || cleanLower.contains("baby ko kahani") || cleanLower.contains("একটা গল্প বলো") ||
            (cleanLower.contains("story") && cleanLower.contains("baby"))
        ) {
            return RecognizedIntent("BABY005", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY005"))
        }

        if (cleanLower.contains("next game") || cleanLower.contains("next activity") || cleanLower.contains("animal sounds") ||
            cleanLower.contains("janwar ki awaz") || cleanLower.contains("পশুর ডাক") || cleanLower.contains("পরের খেলা")
        ) {
            return RecognizedIntent("BABY007", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY007"))
        }

        // STOP_OPERATION / SHUTDOWN
        if (cleanLower == "stop" || cleanLower.contains("stop everything") || cleanLower.contains("sab band karo")) {
            return RecognizedIntent("SYS003", CommandCategory.SYS_PRIVACY, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("SYS003"))
        }

        return null
    }

    private fun extractTime(lower: String): String? {
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje)?")
        val match = regex.find(lower) ?: return null
        return match.value
    }

    private fun extractDuration(lower: String): String? {
        val regex = Regex("(\\d+)\\s*(?:minutes?|mins?|min|मिनट|মিনিট|seconds?|secs?|sec)")
        val match = regex.find(lower) ?: return null
        return match.value
    }

    private fun extractContactName(lower: String, original: String): String? {
        val knownNames = listOf("rahul", "papa", "amit", "mom", "mummy", "bhai", "didi", "sir")
        for (name in knownNames) {
            if (lower.contains(name)) {
                return name.replaceFirstChar { it.uppercase() }
            }
        }
        val contactMatch = Regex("(?i)(?:call|sms to|message to|to|ko|কে)\\s+([a-zA-Z\u0900-\u097F\u0980-\u09FF]+)").find(original)
        return contactMatch?.groupValues?.get(1)?.trim()
    }

    private fun extractMessageBody(lower: String, original: String): String? {
        val delimiters = listOf("ki ", "that ", "বল যে ", ": ")
        for (delim in delimiters) {
            if (original.contains(delim, ignoreCase = true)) {
                return original.substringAfter(delim).trim()
            }
        }
        return null
    }

    private fun recognizeBabyIntent(cleanLower: String, original: String, lang: VoiceLanguage): RecognizedIntent? {
        val clean = cleanLower.trimEnd('.', '?', '!', ' ', '।')
        if (clean == "stop baby mode" || clean.contains("baby mode band") || clean.contains("বেবি মোড বন্ধ") || clean == "stop baby" || clean == "exit baby mode") {
            return RecognizedIntent("BABY008", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY008"))
        }
        if (clean.contains("the baby is crying") || clean.contains("baby is crying") || clean.contains("baccha ro raha") || clean.contains("bacha ro raha") ||
            clean.contains("বাচ্চা কাঁদছে") || clean.contains("distract the baby") || clean.contains("iska dhyan bhatkao") || clean.contains("বাচ্চার মনোযোগ অন্যদিকে") ||
            clean.contains("dhyan bhatkao")
        ) {
            return RecognizedIntent("BABY001", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY001"))
        }
        if (clean.contains("entertain the baby") || clean.contains("play something for the baby") || clean.contains("baby ko entertain") ||
            clean.contains("বাচ্চাকে আনন্দ দাও") || clean.contains("start baby mode") || clean.contains("baby mode chalu")
        ) {
            return RecognizedIntent("BABY002", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY002"))
        }
        if (clean.contains("make the baby laugh") || clean.contains("bachche ko hansaao") || clean.contains("bachhe ko hasao") ||
            clean.contains("বাচ্চাকে হাসাও") || clean.contains("kuch funny karo") || clean.contains("baby ko hasao")
        ) {
            return RecognizedIntent("BABY003", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY003"))
        }
        if (clean.contains("help the baby sleep") || clean.contains("lori sunao") || clean.contains("play a lullaby") ||
            clean.contains("sing a lullaby") || clean.contains("ঘুম পাড়ানোর গান") || clean.contains("baby ko sulao")
        ) {
            return RecognizedIntent("BABY004", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY004"))
        }
        if (clean.contains("tell the baby a story") || clean.contains("baby ko kahani") || clean.contains("বাচ্চাকে গল্প শোনাও") ||
            clean.contains("একটা গল্প বলো") || (clean.contains("story") && clean.contains("baby"))
        ) {
            return RecognizedIntent("BABY005", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY005"))
        }
        if (clean.contains("recite a rhyme") || clean.contains("play a rhyme") || clean.contains("rhyme sunao") ||
            clean.contains("একটা ছড়া শোনাও") || clean.contains("বাচ্চাদের ছড়া") || clean.contains("ছড়া শোনাও") || clean.contains("bal geet")
        ) {
            return RecognizedIntent("BABY006", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY006"))
        }
        if (clean.contains("next game") || clean.contains("next activity") || clean.contains("animal sounds") ||
            clean.contains("janwar ki awaz") || clean.contains("পশুর ডাক") || clean.contains("পরের খেলা") || clean.contains("khel badlo")
        ) {
            return RecognizedIntent("BABY007", CommandCategory.BABY_ENTERTAINMENT, original, lang, emptyMap(), emptyList(), CommandDictionary.findDefinitionById("BABY007"))
        }
        return null
    }
}
