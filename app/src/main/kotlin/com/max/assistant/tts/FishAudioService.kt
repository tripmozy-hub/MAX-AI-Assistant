package com.max.assistant.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class FishAudioService(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "FishAudioService"
        const val DEFAULT_FISH_API_KEY = "sk-fish-b06X3QPdnaWD4AV3MO9Klj0DtvYuiWnkoil-Cdv6PEM"
        const val DEFAULT_JARVIS_VOICE_ID = "14129c3e320149449d6bada6862f7338" // Official MCU JARVIS Voice Clone
        private const val TTS_ENDPOINT = "https://api.fish.audio/v1/tts"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentSynthesisJob: Job? = null
    private val cacheDir = File(context.cacheDir, "fish_audio_cache").apply { mkdirs() }

    fun speak(
        text: String,
        apiKey: String = DEFAULT_FISH_API_KEY,
        voiceId: String = DEFAULT_JARVIS_VOICE_ID,
        onSpeechStarted: (() -> Unit)? = null,
        onSpeechFinished: (() -> Unit)? = null,
        onFallbackRequired: () -> Unit
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onSpeechFinished?.invoke()
            return
        }

        stop()

        currentSynthesisJob = scope.launch(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(voiceId, cleanText)
                if (!cacheFile.exists() || cacheFile.length() < 1000) {
                    val success = downloadTtsAudio(cleanText, apiKey, voiceId, cacheFile)
                    if (!success) {
                        Log.w(TAG, "Fish Audio download unsuccessful, invoking fallback.")
                        withContext(Dispatchers.Main) { onFallbackRequired() }
                        return@launch
                    }
                }

                withContext(Dispatchers.Main) {
                    playAudioFile(cacheFile, onSpeechStarted, onSpeechFinished, onFallbackRequired)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Fish Audio coroutine cancelled cleanly.")
                    return@launch
                }
                Log.w(TAG, "Fish Audio synthesis error: ${e.message}")
                withContext(Dispatchers.Main) { onFallbackRequired() }
            }
        }
    }

    private fun downloadTtsAudio(text: String, apiKey: String, voiceId: String, destFile: File): Boolean {
        return try {
            val url = URL(TTS_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 9000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("model", "s2.1-pro")
            }

            val body = JSONObject().apply {
                put("text", text)
                put("reference_id", voiceId)
                put("format", "mp3")
                put("latency", "low")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(body.toString())
                it.flush()
            }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.mp3")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (tempFile.length() > 500) {
                    if (destFile.exists()) destFile.delete()
                    val renamed = tempFile.renameTo(destFile)
                    if (!renamed) {
                        tempFile.copyTo(destFile, overwrite = true)
                        tempFile.delete()
                    }
                    Log.i(TAG, "Fish Audio synthesized successfully: ${destFile.length()} bytes for: '${text.take(40)}'")
                    true
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Fish Audio returned error HTTP $code: $err")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fish Audio download exception: ${e.message}")
            false
        }
    }

    private fun stopMediaPlayerOnly() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun playAudioFile(
        file: File,
        onSpeechStarted: (() -> Unit)?,
        onSpeechFinished: (() -> Unit)?,
        onFallbackRequired: () -> Unit
    ) {
        try {
            stopMediaPlayerOnly()
            mediaPlayer = MediaPlayer().apply {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                setAudioAttributes(attrs)
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    onSpeechStarted?.invoke()
                }
                setOnCompletionListener {
                    stopMediaPlayerOnly()
                    onSpeechFinished?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopMediaPlayerOnly()
                    onFallbackRequired()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer play error: ${e.message}")
            stopMediaPlayerOnly()
            onFallbackRequired()
        }
    }

    fun stop() {
        currentSynthesisJob?.cancel()
        currentSynthesisJob = null
        stopMediaPlayerOnly()
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    private fun getCacheFile(voiceId: String, text: String): File {
        val key = "$voiceId:$text"
        val hash = md5(key)
        return File(cacheDir, "jarvis_$hash.mp3")
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
