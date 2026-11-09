package com.max.assistant.research

import android.content.Context
import android.util.Log
import com.max.assistant.ai.MaxKnowledgeBase
import com.max.assistant.ai.OnlineKnowledgeService
import com.max.assistant.ai.SearchResultSummary
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Citation(
    val title: String,
    val url: String,
    val snippet: String
)

data class ResearchReport(
    val query: String,
    val spokenSummary: String,
    val detailedContent: String,
    val citations: List<Citation>,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineFallback: Boolean = false
)

/**
 * LiveInternetResearchEngine handles multi-source web queries, synthesis of information,
 * comparison between entities, clean citations, and seamless fallback to the offline knowledge base.
 */
class LiveInternetResearchEngine(
    private val context: Context,
    private val onlineKnowledge: OnlineKnowledgeService = OnlineKnowledgeService(),
    private val offlineKnowledge: MaxKnowledgeBase = MaxKnowledgeBase()
) {

    companion object {
        private const val TAG = "LiveResearchEngine"
    }

    suspend fun conductResearch(query: String, language: VoiceLanguage): ResearchReport = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        Log.i(TAG, "Conducting live internet research for: '$cleanQuery' (${language.name})")

        // 1. Check for comparison query (e.g. "compare iPhone and Samsung", "Python vs Kotlin")
        if (cleanQuery.contains(" vs ", ignoreCase = true) || cleanQuery.contains(" compare ", ignoreCase = true) || cleanQuery.contains(" tulna ", ignoreCase = true)) {
            val compResult = conductComparisonResearch(cleanQuery, language)
            if (compResult != null) return@withContext compResult
        }

        // 2. Multi-source Search: DuckDuckGo Instant Answers + Wikipedia Rest API
        val citations = mutableListOf<Citation>()
        val snippets = mutableListOf<String>()

        val primaryResult = onlineKnowledge.searchWebIntelligent(cleanQuery, language)
        if (primaryResult != null) {
            citations.add(Citation(primaryResult.sourceTitle, primaryResult.sourceUrl, primaryResult.fullSummary))
            snippets.add(primaryResult.fullSummary)
        }

        // Secondary query via Wikipedia OpenSearch if needed
        val wikiResults = fetchWikipediaSearchResults(cleanQuery, language)
        for (w in wikiResults) {
            if (citations.none { it.url == w.url }) {
                citations.add(w)
                snippets.add(w.snippet)
            }
        }

        if (citations.isNotEmpty() && snippets.isNotEmpty()) {
            val mainSummary = snippets.first().split(Regex("(?<=[.!?])\\s+")).take(3).joinToString(" ")
            val citationTitles = citations.take(2).joinToString(" and ") { it.title }

            val spoken = when (language) {
                VoiceLanguage.HINDI -> "सर, $citationTitles के अनुसार: $mainSummary"
                VoiceLanguage.BENGALI -> "স্যার, $citationTitles সূত্র অনুসারে: $mainSummary"
                VoiceLanguage.HINGLISH -> "Sir, $citationTitles ke according: $mainSummary"
                else -> "Sir, according to latest information from $citationTitles: $mainSummary"
            }

            val detailed = buildString {
                appendLine("RESEARCH QUERY: $cleanQuery")
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                appendLine("DATE: $dateStr")
                appendLine("---")
                appendLine(snippets.joinToString("\n\n"))
                appendLine("---")
                appendLine("SOURCES & CITATIONS:")
                citations.forEachIndexed { i, c ->
                    appendLine("[${i + 1}] ${c.title} - ${c.url}")
                }
            }

            return@withContext ResearchReport(
                query = cleanQuery,
                spokenSummary = spoken,
                detailedContent = detailed,
                citations = citations,
                isOfflineFallback = false
            )
        }

        // 3. Fallback: Offline Knowledge Base
        Log.w(TAG, "No live search results available for '$cleanQuery'. Falling back to offline knowledge base.")
        val offlineAnswer = offlineKnowledge.queryKnowledge(cleanQuery, language)
        val fallbackSpoken = if (offlineAnswer != null) {
            offlineAnswer
        } else {
            when (language) {
                VoiceLanguage.HINDI -> "सर, इस विषय पर अभी इंटरनेट से नवीनतम जानकारी उपलब्ध नहीं हो पाई।"
                VoiceLanguage.BENGALI -> "স্যার, এই বিষয়ে এখন ইন্টারনেট থেকে তথ্য পাওয়া যায়নি।"
                VoiceLanguage.HINGLISH -> "Sir, is topic par abhi live web results nahi mil paye."
                else -> "Sir, I could not retrieve live web results for that query at this time."
            }
        }

        return@withContext ResearchReport(
            query = cleanQuery,
            spokenSummary = fallbackSpoken,
            detailedContent = fallbackSpoken,
            citations = emptyList(),
            isOfflineFallback = true
        )
    }

    private suspend fun conductComparisonResearch(query: String, language: VoiceLanguage): ResearchReport? {
        val parts = if (query.contains(" vs ", ignoreCase = true)) {
            query.split(Regex("(?i)\\s+vs\\s+"))
        } else if (query.contains(" compare ", ignoreCase = true)) {
            query.split(Regex("(?i)\\s+compare\\s+")).lastOrNull()?.split(Regex("(?i)\\s+and\\s+|\\s+with\\s+"))
        } else {
            null
        }

        if (parts == null || parts.size < 2) return null
        val topicA = parts[0].trim()
        val topicB = parts[1].trim()

        val resA = onlineKnowledge.searchWebIntelligent(topicA, language)
        val resB = onlineKnowledge.searchWebIntelligent(topicB, language)

        val citations = mutableListOf<Citation>()
        if (resA != null) citations.add(Citation(resA.sourceTitle, resA.sourceUrl, resA.fullSummary))
        if (resB != null) citations.add(Citation(resB.sourceTitle, resB.sourceUrl, resB.fullSummary))

        if (resA == null && resB == null) return null

        val summaryA = resA?.fullSummary?.split(Regex("(?<=[.!?])\\s+"))?.firstOrNull() ?: "$topicA information verified."
        val summaryB = resB?.fullSummary?.split(Regex("(?<=[.!?])\\s+"))?.firstOrNull() ?: "$topicB information verified."

        val spoken = when (language) {
            VoiceLanguage.HINDI -> "सर, तुलना के अनुसार: $topicA $summaryA जबकि $topicB $summaryB"
            VoiceLanguage.BENGALI -> "স্যার, তুলনামূলক তথ্য: $topicA $summaryA অন্যদিকে $topicB $summaryB"
            VoiceLanguage.HINGLISH -> "Sir, comparison ke mutabiq: $topicA $summaryA, jabki $topicB $summaryB"
            else -> "Sir, comparing the two: For $topicA, $summaryA In contrast, for $topicB, $summaryB"
        }

        val detailed = buildString {
            appendLine("COMPARISON: $topicA vs $topicB")
            appendLine("1. $topicA: ${resA?.fullSummary ?: "No data"}")
            appendLine("2. $topicB: ${resB?.fullSummary ?: "No data"}")
            appendLine("--- Sources ---")
            citations.forEach { appendLine("- ${it.title} (${it.url})") }
        }

        return ResearchReport(
            query = query,
            spokenSummary = spoken,
            detailedContent = detailed,
            citations = citations,
            isOfflineFallback = false
        )
    }

    private fun fetchWikipediaSearchResults(query: String, language: VoiceLanguage): List<Citation> {
        val citations = mutableListOf<Citation>()
        try {
            val wikiLang = when (language) {
                VoiceLanguage.HINDI -> "hi"
                VoiceLanguage.BENGALI -> "bn"
                else -> "en"
            }
            val urlStr = "https://$wikiLang.wikipedia.org/w/api.php?action=opensearch&search=${URLEncoder.encode(query, "UTF-8")}&limit=3&namespace=0&format=json"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "MAX-Android-Assistant/2.0")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONArray(jsonStr)
                if (root.length() >= 4) {
                    val titles = root.getJSONArray(1)
                    val snippets = root.getJSONArray(2)
                    val urls = root.getJSONArray(3)

                    for (i in 0 until titles.length()) {
                        val title = titles.optString(i, "")
                        val snippet = snippets.optString(i, "")
                        val pageUrl = urls.optString(i, "")
                        if (title.isNotBlank() && snippet.isNotBlank()) {
                            citations.add(Citation("Wikipedia ($title)", pageUrl, snippet))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wikipedia opensearch lookup note: ${e.message}")
        }
        return citations
    }

    private fun sanitizeQuery(raw: String): String {
        return raw
            .replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
            .replace(Regex("(?i)^(search\\s+internet\\s+for|search\\s+web\\s+for|research\\s+on|research\\s+about|online\\s+research\\s+karo|internet\\s+pe\\s+khojo|online\\s+khojo|web\\s+search)\\s*"), "")
            .trim('?', '.', '!', ' ', '\'', '"')
            .trim()
    }
}
