package com.max.assistant.ai

import android.content.Context
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiAiProvider(
    private val context: Context? = null
) : IAiProvider {

    companion object {
        private const val TAG = "GeminiAiProvider"
        private const val PREFS_NAME = "max_ai_config"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_BACKUP_KEY = "backup_ai_key"
        const val DEFAULT_API_KEY = "AQ.Ab8RN6KrQ3f8RmpYIpfNFS2EoFABCZ0v_EMi_7aEbB6XzTBwrg"
        const val DEFAULT_BACKUP_KEY = "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"
        private const val DEFAULT_MODEL = "gemini-flash-lite-latest"
        private const val FALLBACK_MODEL = "gemini-3.1-flash-lite"
        private const val SECONDARY_FALLBACK_MODEL = "gemini-flash-latest"
        private const val GROQ_MODEL = "qwen/qwen3.8-27b"
        private const val GROQ_FALLBACK_MODEL = "openai/gpt-oss-120b"
    }

    override val providerName: String = "Google Gemini AI (Flash Lite) + Groq LPU Backup"

    override val isConfigured: Boolean
        get() {
            val key = getApiKey()
            return key.isNotBlank()
        }

    fun getApiKey(): String {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_API_KEY, "")?.trim() ?: ""
        return if (saved.isNotBlank() && saved != "AQ.Ab8RN6ICkxgqQi22imL7bu5tSJtAkWcA708LZhbiyQdUldQmvg") saved else DEFAULT_API_KEY
    }

    fun saveApiKey(apiKey: String) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(KEY_API_KEY, apiKey.trim())?.apply()
    }

    fun getBackupApiKey(): String {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_BACKUP_KEY, "")?.trim() ?: ""
        return if (saved.isNotBlank()) saved else DEFAULT_BACKUP_KEY
    }

    fun saveBackupApiKey(apiKey: String) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(KEY_BACKUP_KEY, apiKey.trim())?.apply()
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage>,
        preferredLanguage: VoiceLanguage,
        systemInstructionOverride: String?
    ): AiResponse = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext AiResponse.Error("API Key is not configured for Gemini AI.", isNetworkError = false)
        }

        try {
            val memoryContext = if (context != null) {
                try {
                    com.max.assistant.memory.ContextMemoryManager(context).getPromptContextEnrichment()
                } catch (_: Exception) { "" }
            } else ""

            // System prompt emphasizing JARVIS persona & addressing user as Sir, unless customized
            val systemInstruction = if (!systemInstructionOverride.isNullOrBlank()) {
                if (memoryContext.isNotBlank()) "$systemInstructionOverride\n$memoryContext" else systemInstructionOverride
            } else {
                val langInstruction = when (preferredLanguage) {
                    VoiceLanguage.HINGLISH -> "Reply in natural Hinglish — a smooth, conversational mix of Hindi and English the way young Indians actually speak. Use English words naturally within Hindi sentences. For example: 'Sir, aapka kaam ho jayega, bas ek second.' or 'Bilkul Sir, main abhi check kar leta hoon.' Address user as 'Sir'. Never use pure formal Hindi or pure formal English — keep it natural, warm, and conversational."
                    VoiceLanguage.HINDI -> "Reply strictly in natural Indian Hindi, addressing the user as 'सर' (Sir)."
                    VoiceLanguage.BENGALI -> "Reply strictly in natural Bengali, addressing the user as 'স্যার' (Sir)."
                    VoiceLanguage.ENGLISH -> "Reply in natural British-inspired English, addressing the user as 'Sir'."
                    VoiceLanguage.AUTO -> "Detect user's language. If the user mixes Hindi and English (Hinglish), reply in natural Hinglish the way young Indians speak — casual, warm, mixing both languages smoothly. Otherwise reply in their detected language. Always address the user respectfully as 'Sir'."
                }

                "You are MAX, an intelligent personal AI assistant inspired by JARVIS. " +
                        "Your personality is calm, witty, friendly, and deeply knowledgeable — like a trusted friend who also happens to be brilliant. " +
                        "Payment & Financial Directive: Performing payments, UPI transfers, or banking transactions is permanently disabled in MAX for user security. Never claim or simulate that you can execute payments. " +
                        "$langInstruction " +
                        (if (memoryContext.isNotBlank()) "\n$memoryContext\n" else "") +
                        "Keep answers concise, clear, and direct for audio speech synthesis. Avoid Markdown formatting like asterisks or hashtags."
            }

            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray()

                // Add past conversation turns for context
                val recentTurns = history.takeLast(6)
                for (turn in recentTurns) {
                    val turnObj = JSONObject().apply {
                        put("role", if (turn.role == MessageRole.USER) "user" else "model")
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", turn.text) })
                        }
                        put("parts", partsArr)
                    }
                    contentsArr.put(turnObj)
                }

                // Add current user prompt
                contentsArr.put(JSONObject().apply {
                    put("role", "user")
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArr)
                })

                put("contents", contentsArr)

                put("systemInstruction", JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", parts)
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 500)
                })
            }

            var (responseCode, responseStr) = sendPostRequest(DEFAULT_MODEL, key, jsonBody)

            // If primary model 404s/503s, attempt fallbacks
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 503 || responseCode == 500) {
                Log.w(TAG, "Primary model $DEFAULT_MODEL returned $responseCode, trying fallback $FALLBACK_MODEL")
                val fallbackResult = sendPostRequest(FALLBACK_MODEL, key, jsonBody)
                responseCode = fallbackResult.first
                responseStr = fallbackResult.second
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 503 || responseCode == 500) {
                    Log.w(TAG, "Fallback $FALLBACK_MODEL returned $responseCode, trying secondary $SECONDARY_FALLBACK_MODEL")
                    val secondaryResult = sendPostRequest(SECONDARY_FALLBACK_MODEL, key, jsonBody)
                    responseCode = secondaryResult.first
                    responseStr = secondaryResult.second
                }
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val respJson = JSONObject(responseStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "").trim()
                        val sanitized = sanitizeForSpeech(rawText)
                        return@withContext AiResponse.Success(sanitized, preferredLanguage)
                    }
                }
            }

            // Primary models failed/empty: invoke ultra-low latency Groq backup
            Log.w(TAG, "Gemini models failed (code $responseCode). Switching to ultra-fast Groq backup...")
            val groqFallback = queryGroqBackup(prompt, history, preferredLanguage, systemInstruction)
            if (!groqFallback.isNullOrBlank()) {
                Log.i(TAG, "Groq backup answered successfully!")
                return@withContext AiResponse.Success(groqFallback, preferredLanguage)
            }

            Log.e(TAG, "Gemini API HTTP Error $responseCode: $responseStr")
            return@withContext AiResponse.Error("Gemini API Error (HTTP $responseCode)", isNetworkError = true)
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "Gemini API timed out. Engaging ultra-fast Groq backup...")
            try {
                val groqFallback = queryGroqBackup(prompt, history, preferredLanguage, "")
                if (!groqFallback.isNullOrBlank()) {
                    Log.i(TAG, "Groq backup answered after Gemini timeout!")
                    return@withContext AiResponse.Success(groqFallback, preferredLanguage)
                }
            } catch (_: Exception) {}
            return@withContext AiResponse.Error("Connection timed out, Sir.", isNetworkError = true)
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Network IO error on Gemini. Engaging Groq backup: ${e.message}")
            try {
                val groqFallback = queryGroqBackup(prompt, history, preferredLanguage, "")
                if (!groqFallback.isNullOrBlank()) {
                    Log.i(TAG, "Groq backup answered after network failure!")
                    return@withContext AiResponse.Success(groqFallback, preferredLanguage)
                }
            } catch (_: Exception) {}
            return@withContext AiResponse.Error("Network connection failure, Sir.", isNetworkError = true)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API unexpected error: ${e.message}. Trying Groq backup...")
            try {
                val groqFallback = queryGroqBackup(prompt, history, preferredLanguage, "")
                if (!groqFallback.isNullOrBlank()) {
                    return@withContext AiResponse.Success(groqFallback, preferredLanguage)
                }
            } catch (_: Exception) {}
            return@withContext AiResponse.Error("System error: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    override suspend fun analyzeImage(
        base64Jpeg: String,
        prompt: String,
        preferredLanguage: VoiceLanguage
    ): AiResponse = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext AiResponse.Error("API Key is not configured for Gemini AI.", isNetworkError = false)
        }

        try {
            val langInstruction = when (preferredLanguage) {
                VoiceLanguage.HINGLISH -> "Reply in natural Hinglish, smooth mix of Hindi and English. Address user as 'Sir'."
                VoiceLanguage.HINDI -> "Reply strictly in natural Indian Hindi, addressing the user as 'सर' (Sir)."
                VoiceLanguage.BENGALI -> "Reply strictly in natural Bengali, addressing the user as 'স্যার' (Sir)."
                VoiceLanguage.ENGLISH -> "Reply in natural British-inspired English, addressing the user as 'Sir'."
                VoiceLanguage.AUTO -> "Detect context and reply in natural Hinglish or English, addressing user as 'Sir'."
            }

            val systemInstruction = "You are MAX, an intelligent personal AI assistant equipped with Screen Vision. " +
                    "Examine the user's screen capture carefully to read text, understand diagrams, diagnose errors, explain questions, solve math, or describe interface elements. " +
                    "$langInstruction Keep answers concise, direct, and voice-friendly without markdown asterisks."

            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray()
                val turnObj = JSONObject().apply {
                    put("role", "user")
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Jpeg)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", if (prompt.isNotBlank()) prompt else "Examine this screen and tell me what you see, explain any key questions or errors.")
                        })
                    }
                    put("parts", partsArr)
                }
                contentsArr.put(turnObj)
                put("contents", contentsArr)

                put("systemInstruction", JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", parts)
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 500)
                })
            }

            var (responseCode, responseStr) = sendPostRequest(DEFAULT_MODEL, key, jsonBody)
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 503 || responseCode == 500) {
                Log.w(TAG, "Primary vision model $DEFAULT_MODEL returned $responseCode, trying fallback $FALLBACK_MODEL")
                val fallbackResult = sendPostRequest(FALLBACK_MODEL, key, jsonBody)
                responseCode = fallbackResult.first
                responseStr = fallbackResult.second
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND || responseCode == 503 || responseCode == 500) {
                    Log.w(TAG, "Fallback vision $FALLBACK_MODEL returned $responseCode, trying secondary $SECONDARY_FALLBACK_MODEL")
                    val secondaryResult = sendPostRequest(SECONDARY_FALLBACK_MODEL, key, jsonBody)
                    responseCode = secondaryResult.first
                    responseStr = secondaryResult.second
                }
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val respJson = JSONObject(responseStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "").trim()
                        val sanitized = sanitizeForSpeech(rawText)
                        return@withContext AiResponse.Success(sanitized, preferredLanguage)
                    }
                }
                return@withContext AiResponse.Error("Empty vision response structure from Gemini API.")
            } else {
                Log.e(TAG, "Gemini Vision HTTP Error $responseCode: $responseStr")
                return@withContext AiResponse.Error("Gemini Vision Error (HTTP $responseCode)", isNetworkError = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Vision unexpected error: ${e.message}")
            return@withContext AiResponse.Error("Vision analysis error: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    private fun sendPostRequest(model: String, key: String, jsonBody: JSONObject): Pair<Int, String> {
        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
        val url = URL(endpointUrl)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody.toString())
            writer.flush()
        }

        val code = connection.responseCode
        val content = if (code == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        return Pair(code, content)
    }

    private fun queryGroqBackup(
        prompt: String,
        history: List<ChatMessage>,
        preferredLanguage: VoiceLanguage,
        systemInstruction: String
    ): String? {
        val groqKey = getBackupApiKey()
        if (groqKey.isBlank()) return null

        try {
            val sysPrompt = if (systemInstruction.isNotBlank()) systemInstruction else {
                "You are MAX, an intelligent personal AI assistant inspired by JARVIS. " +
                        "Address the user respectfully as 'Sir'. Keep responses concise, direct, warm, and natural for speech without asterisks or markdown."
            }

            val jsonBody = JSONObject().apply {
                put("model", GROQ_MODEL)
                val messagesArr = JSONArray()
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", sysPrompt)
                })
                val recentTurns = history.takeLast(6)
                for (turn in recentTurns) {
                    messagesArr.put(JSONObject().apply {
                        put("role", if (turn.role == MessageRole.USER) "user" else "assistant")
                        put("content", turn.text)
                    })
                }
                messagesArr.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
                put("messages", messagesArr)
                put("temperature", 0.7)
                put("max_tokens", 400)
            }

            var (code, respStr) = sendGroqPostRequest(groqKey, jsonBody)
            if (code != HttpURLConnection.HTTP_OK) {
                jsonBody.put("model", GROQ_FALLBACK_MODEL)
                val fallbackRes = sendGroqPostRequest(groqKey, jsonBody)
                code = fallbackRes.first
                respStr = fallbackRes.second
            }

            if (code == HttpURLConnection.HTTP_OK) {
                val respJson = JSONObject(respStr)
                val choices = respJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    val content = message?.optString("content", "")?.trim() ?: ""
                    if (content.isNotBlank()) {
                        return sanitizeForSpeech(content)
                    }
                }
            } else {
                Log.w(TAG, "Groq backup HTTP error $code: $respStr")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Groq backup query exception: ${e.message}")
        }
        return null
    }

    private fun sendGroqPostRequest(key: String, jsonBody: JSONObject): Pair<Int, String> {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3500
            readTimeout = 4000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $key")
        }

        OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody.toString())
            writer.flush()
        }

        val code = connection.responseCode
        val content = if (code == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        return Pair(code, content)
    }

    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Bold **text** -> text
            .replace(Regex("\\*(.*?)\\*"), "$1")       // Italic *text* -> text
            .replace(Regex("`{1,3}[^`]*`{1,3}"), "")  // Code snippets
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "") // Markdown headings
            .replace(Regex("^[-*•]\\s*", RegexOption.MULTILINE), "") // Bullets
            .trim()
    }
}
