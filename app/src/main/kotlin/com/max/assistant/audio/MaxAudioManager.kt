package com.max.assistant.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class MaxAudioManager(context: Context) {

    companion object {
        private const val TAG = "MaxAudioManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus permanent loss")
                hasAudioFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AudioFocus transient loss")
                hasAudioFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AudioFocus ducking")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus regained")
                hasAudioFocus = true
            }
        }
    }

    /**
     * Request transient audio focus for speech playback with assistant usage.
     * Uses AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK so background media (Spotify, YouTube Music, etc.)
     * gently ducks instead of being paused or terminated.
     */
    fun requestAssistantFocus(): Boolean {
        if (audioManager == null) return false

        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()

                audioManager.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }

            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.d(TAG, "Request AudioFocus result=$result, granted=$hasAudioFocus")
            hasAudioFocus
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request audio focus: ${e.message}")
            false
        }
    }

    /**
     * Release the assistant audio focus after speaking finishes.
     */
    fun releaseAssistantFocus() {
        if (audioManager == null || !hasAudioFocus) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
            hasAudioFocus = false
            Log.d(TAG, "AudioFocus released")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio focus: ${e.message}")
        }
    }
}
