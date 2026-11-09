package com.max.assistant.analytics

import android.content.Context
import android.util.Log
import com.max.assistant.database.CommandAnalyticsRecord
import com.max.assistant.database.LearnedSynonymRecord
import com.max.assistant.database.MaxDatabaseHelper
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PerformanceAnalyticsEngine tracks command execution latency, success/failure metrics,
 * learns phrase-to-intent synonyms automatically, and identifies optimization areas.
 */
class PerformanceAnalyticsEngine(
    private val context: Context,
    private val dbHelper: MaxDatabaseHelper = MaxDatabaseHelper.getInstance(context)
) {

    companion object {
        private const val TAG = "PerformanceAnalytics"

        // Default built-in Hindi/English common synonyms
        private val BUILTIN_SYNONYMS = mapOf(
            "batti jalao" to "FLASHLIGHT_ON",
            "torch jalao" to "FLASHLIGHT_ON",
            "light jalao" to "FLASHLIGHT_ON",
            "flash on karo" to "FLASHLIGHT_ON",
            "batti bujhao" to "FLASHLIGHT_OFF",
            "torch band karo" to "FLASHLIGHT_OFF",
            "light band karo" to "FLASHLIGHT_OFF",
            "awaz badhao" to "VOLUME_UP",
            "awaz tez karo" to "VOLUME_UP",
            "sound badhao" to "VOLUME_UP",
            "awaz kam karo" to "VOLUME_DOWN",
            "awaz dheere karo" to "VOLUME_DOWN",
            "mute karo" to "VOLUME_MUTE",
            "chup raho" to "MUTE_MAX",
            "shant raho" to "MUTE_MAX",
            "net band karo" to "SETTINGS_WIFI",
            "net chalu karo" to "SETTINGS_WIFI"
        )
    }

    suspend fun trackExecution(
        rawCommand: String,
        normalizedIntent: String,
        executionTimeMs: Long,
        success: Boolean,
        failureReason: String? = null,
        language: VoiceLanguage = VoiceLanguage.ENGLISH
    ) = withContext(Dispatchers.IO) {
        try {
            val record = CommandAnalyticsRecord(
                rawCommand = rawCommand,
                normalizedIntent = normalizedIntent,
                executionTimeMs = executionTimeMs,
                success = success,
                failureReason = failureReason ?: "",
                language = language.name,
                timestamp = System.currentTimeMillis()
            )
            dbHelper.insertCommandAnalytics(record)
            Log.d(TAG, "Tracked command '$rawCommand' ($normalizedIntent) in ${executionTimeMs}ms, success: $success")
        } catch (e: Exception) {
            Log.w(TAG, "Failed tracking command analytics: ${e.message}")
        }
    }

    suspend fun learnSynonym(phrase: String, canonicalIntent: String) = withContext(Dispatchers.IO) {
        try {
            dbHelper.recordOrIncrementSynonym(phrase, canonicalIntent)
            Log.i(TAG, "Learned/updated synonym: '$phrase' -> '$canonicalIntent'")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record synonym: ${e.message}")
        }
    }

    suspend fun resolveSynonym(phrase: String): String? = withContext(Dispatchers.IO) {
        val clean = phrase.trim().lowercase()
        // 1. Check built-in dictionary
        val builtin = BUILTIN_SYNONYMS[clean]
        if (builtin != null) return@withContext builtin

        // 2. Check dynamic database
        return@withContext dbHelper.getCanonicalIntentForPhrase(clean)
    }

    suspend fun getPerformanceSummary(language: VoiceLanguage): String = withContext(Dispatchers.IO) {
        val successRate = dbHelper.getOverallSuccessRate()
        val synonyms = dbHelper.getAllLearnedSynonyms()
        val roundedRate = String.format("%.1f", successRate)

        return@withContext when (language) {
            VoiceLanguage.HINDI -> "सर, मैक्स का कमांड सफलता दर $roundedRate% है, और सिस्टम ने ${synonyms.size} अनुकूलित बोलचाल के वाक्यांश सीखे हैं।"
            VoiceLanguage.BENGALI -> "স্যার, ম্যাক্সের কমান্ড সাফল্যের হার $roundedRate%, এবং সিস্টেমটি ${synonyms.size}টি শেখা বাক্যাংশ মনে রেখেছে।"
            VoiceLanguage.HINGLISH -> "Sir, MAX ka command success rate $roundedRate% hai, aur humne ${synonyms.size} custom synonyms learn kiye hain."
            else -> "Sir, MAX command success rate is $roundedRate% with ${synonyms.size} learned voice adaptations in active memory."
        }
    }
}
