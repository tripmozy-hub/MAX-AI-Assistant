package com.max.assistant.dictionary

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.max.assistant.ai.OnlineKnowledgeService
import com.max.assistant.ai.SearchResultSummary
import com.max.assistant.tts.VoiceLanguage

/**
 * WebSearchManager handles Google searches, Chrome launching, official portal lookups, and web summarization.
 */
class WebSearchManager(
    private val context: Context,
    private val onlineKnowledge: OnlineKnowledgeService = OnlineKnowledgeService()
) {

    fun searchGoogle(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openWebpage("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun openChrome(): Boolean {
        val chromePackage = "com.android.chrome"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(chromePackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            openWebpage("https://www.google.com")
        }
    }

    fun openWebpage(url: String): Boolean {
        return try {
            val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchOnlineKnowledge(query: String, language: VoiceLanguage): SearchResultSummary? {
        return onlineKnowledge.searchWebIntelligent(query, language)
    }

    fun searchGovernmentJobs(language: VoiceLanguage): Boolean {
        return searchGoogle("latest government jobs recruitment UPSC SSC Railway")
    }

    fun findOfficialWebsite(query: String): Boolean {
        return searchGoogle("$query official website portal")
    }
}
