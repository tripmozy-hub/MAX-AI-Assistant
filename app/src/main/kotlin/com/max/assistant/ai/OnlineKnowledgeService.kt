package com.max.assistant.ai

import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResultSummary(
    val query: String,
    val spokenAnswer: String,
    val fullSummary: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val isVerified: Boolean = true
)

class OnlineKnowledgeService {

    companion object {
        private const val TAG = "OnlineKnowledgeService"
    }

    /**
     * Queries free, open web knowledge sources (Wikipedia & DuckDuckGo Instant Answers)
     * to provide factual, educational, and verified responses without requiring a paid API key.
     */
    suspend fun queryOnlineKnowledge(query: String, language: VoiceLanguage): String? = withContext(Dispatchers.IO) {
        val result = searchWebIntelligent(query, language)
        return@withContext result?.spokenAnswer
    }

    suspend fun searchWebIntelligent(query: String, language: VoiceLanguage): SearchResultSummary? = withContext(Dispatchers.IO) {
        val cleanQuery = query
            .replace(Regex("^(hey\\s+max|ok\\s+max|hello\\s+max|max|mac|mex|maks)[,\\s:]*"), "")
            .replace(Regex("(?i)^(search\\s+google\\s+for|google\\s+search|search\\s+the\\s+web\\s+for|search\\s+the\\s+internet\\s+for|search\\s+for|search|khojo|dhundho|what\\s+is|who\\s+is|tell\\s+me\\s+about|explain|define|kya\\s+hai|kaun\\s+hai|ki\\s+hoy|ke\\s+hote|kon\\s+hai)\\s*"), "")
            .trim('?', '.', '!', ' ', '\'', '"')
            .trim()

        if (cleanQuery.isEmpty() || cleanQuery.length < 2) return@withContext null

        // 1. Try DuckDuckGo Instant Answer API
        try {
            val ddgUrl = "https://api.duckduckgo.com/?q=${URLEncoder.encode(cleanQuery, "UTF-8")}&format=json&no_html=1&skip_disambig=1"
            val conn = (URL(ddgUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
                setRequestProperty("User-Agent", "MAX-Android-Assistant/2.0")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val abstractText = json.optString("AbstractText", "").trim()
                val abstractSource = json.optString("AbstractSource", "Web Knowledge")
                val abstractUrl = json.optString("AbstractURL", "https://duckduckgo.com/?q=" + URLEncoder.encode(cleanQuery, "UTF-8"))

                if (abstractText.isNotEmpty() && abstractText.length > 20) {
                    val firstSentence = abstractText.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ")
                    val spoken = when (language) {
                        VoiceLanguage.HINGLISH -> "Sir, verified source ($abstractSource) ke according: $firstSentence"
                        VoiceLanguage.HINDI -> "सर, सत्यापित स्रोत ($abstractSource) के अनुसार: $firstSentence"
                        VoiceLanguage.BENGALI -> "স্যার, যাচাইকৃত উৎস ($abstractSource) অনুযায়ী: $firstSentence"
                        else -> "Sir, according to verified source $abstractSource: $firstSentence"
                    }
                    return@withContext SearchResultSummary(
                        query = cleanQuery,
                        spokenAnswer = spoken,
                        fullSummary = abstractText,
                        sourceTitle = abstractSource,
                        sourceUrl = abstractUrl,
                        isVerified = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo Instant Answer error: ${e.message}")
        }

        // 2. Try Wikipedia Summary API (Language-specific: Hindi, Bengali, or English)
        try {
            val wikiLang = when (language) {
                VoiceLanguage.HINDI -> "hi"
                VoiceLanguage.BENGALI -> "bn"
                else -> "en"
            }
            val formattedTitle = cleanQuery.replace(" ", "_")
            val wikiUrl = "https://$wikiLang.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(formattedTitle, "UTF-8")}"
            val conn = (URL(wikiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
                setRequestProperty("User-Agent", "MAX-Android-Assistant/2.0")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val extract = json.optString("extract", "").trim()
                val pageTitle = json.optString("title", cleanQuery)
                val pageUrl = json.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page", "") ?: "https://$wikiLang.wikipedia.org/wiki/$formattedTitle"

                if (extract.isNotEmpty()) {
                    val sentences = extract.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ")
                    val spoken = when (language) {
                        VoiceLanguage.HINGLISH -> "Sir, Wikipedia ke anusaar: $sentences"
                        VoiceLanguage.HINDI -> "सर, विकिपीडिया के अनुसार: $sentences"
                        VoiceLanguage.BENGALI -> "স্যার, উইকিপিডিয়া অনুসারে: $sentences"
                        else -> "Sir, according to Wikipedia: $sentences"
                    }
                    return@withContext SearchResultSummary(
                        query = cleanQuery,
                        spokenAnswer = spoken,
                        fullSummary = extract,
                        sourceTitle = "Wikipedia ($pageTitle)",
                        sourceUrl = pageUrl,
                        isVerified = true
                    )
                }
            } else if (wikiLang != "en") {
                // Fallback to English Wikipedia if regional article not found
                val fallbackUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(formattedTitle, "UTF-8")}"
                val fallbackConn = (URL(fallbackUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "MAX-Android-Assistant/2.0")
                }
                if (fallbackConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonStr = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val extract = json.optString("extract", "").trim()
                    val pageTitle = json.optString("title", cleanQuery)
                    val pageUrl = json.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page", "") ?: "https://en.wikipedia.org/wiki/$formattedTitle"

                    if (extract.isNotEmpty()) {
                        val sentences = extract.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ")
                        val spoken = when (language) {
                            VoiceLanguage.HINGLISH -> "Sir, Wikipedia ke anusaar: $sentences"
                            VoiceLanguage.HINDI -> "सर, विकिपीडिया के अनुसार: $sentences"
                            VoiceLanguage.BENGALI -> "স্যার, উইকিপিডিয়া অনুসারে: $sentences"
                            else -> "Sir, according to Wikipedia: $sentences"
                        }
                        return@withContext SearchResultSummary(
                            query = cleanQuery,
                            spokenAnswer = spoken,
                            fullSummary = extract,
                            sourceTitle = "Wikipedia ($pageTitle)",
                            sourceUrl = pageUrl,
                            isVerified = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia Summary error: ${e.message}")
        }

        return@withContext null
    }
}
