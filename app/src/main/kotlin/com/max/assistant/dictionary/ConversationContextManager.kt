package com.max.assistant.dictionary

import android.util.Log

/**
 * ConversationContextManager maintains multi-turn dialog state and contextual references.
 * Resolves implicit follow-up commands (e.g. "Open YouTube" -> "Search for physics lectures").
 */
class ConversationContextManager {

    companion object {
        private const val TAG = "ConversationContext"
        val instance: ConversationContextManager by lazy { ConversationContextManager() }
    }

    var lastAppOpened: String? = null
        private set

    var lastContact: String? = null
        private set

    var lastTopic: String? = null
        private set

    var lastQuery: String? = null
        private set

    var lastIntentId: String? = null
        private set

    var pendingMissingParameter: Pair<String, String>? = null // (intentId, paramName)
        private set

    fun recordAppOpened(appName: String) {
        lastAppOpened = appName.lowercase().trim()
        Log.i(TAG, "Context updated: lastAppOpened=$lastAppOpened")
    }

    fun recordContact(contactName: String) {
        lastContact = contactName.trim()
        Log.i(TAG, "Context updated: lastContact=$lastContact")
    }

    fun recordTopic(topic: String) {
        lastTopic = topic.trim()
        Log.i(TAG, "Context updated: lastTopic=$lastTopic")
    }

    fun recordIntent(intentId: String, query: String) {
        lastIntentId = intentId
        lastQuery = query
    }

    fun setAwaitingParameter(intentId: String, paramName: String) {
        pendingMissingParameter = Pair(intentId, paramName)
        Log.i(TAG, "Awaiting missing parameter '$paramName' for intent '$intentId'")
    }

    fun clearAwaitingParameter() {
        pendingMissingParameter = null
    }

    fun clearContext() {
        lastAppOpened = null
        lastContact = null
        lastTopic = null
        lastQuery = null
        lastIntentId = null
        pendingMissingParameter = null
    }

    /**
     * Inspects whether an input like "Search for physics lectures" is a follow-up to the last opened app (e.g. YouTube).
     */
    fun resolveContextualFollowUp(input: String): Pair<String, String>? {
        val lower = input.lowercase().trim()
        val app = lastAppOpened

        if (app != null && (lower.startsWith("search for ") || lower.startsWith("search ") || lower.contains("khojo") || lower.contains("dhundo"))) {
            val query = lower.removePrefix("search for ").removePrefix("search ").replace("khojo", "").replace("dhundo", "").trim()
            if (query.isNotBlank()) {
                return Pair(app, query)
            }
        }
        return null
    }
}
