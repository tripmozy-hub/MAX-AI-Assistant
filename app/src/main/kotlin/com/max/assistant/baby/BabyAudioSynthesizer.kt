package com.max.assistant.baby

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure Kotlin acoustic audio synthesizer for safe, gentle baby entertainment.
 * Generates soft sine/harmonic waveforms with smooth ADSR windowing (zero sudden clicks, zero loud bursts).
 * Enforces safe baby volume ceilings at all times.
 */
class BabyAudioSynthesizer {

    companion object {
        private const val TAG = "BabyAudioSynthesizer"
        private const val SAMPLE_RATE = 44100
        private const val SAFE_MAX_AMPLITUDE = 0.28f // Safe moderate ceiling for infant ears
    }

    private var activeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentTrack: AudioTrack? = null

    /**
     * Stop all synthesized audio immediately and release resources.
     */
    @Synchronized
    fun stopAudio() {
        activeJob?.cancel()
        activeJob = null
        try {
            currentTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopAudio cleanup error: ${e.message}")
        } finally {
            currentTrack = null
        }
    }

    /**
     * Play a soothing music box lullaby melody with soft celesta tones.
     */
    fun playLullaby(onComplete: (() -> Unit)? = null) {
        stopAudio()
        activeJob = scope.launch {
            try {
                // Brahms' Lullaby notes (frequency in Hz, duration in ms)
                val melody = listOf(
                    329.63f to 350, // E4
                    329.63f to 350, // E4
                    392.00f to 700, // G4
                    329.63f to 350, // E4
                    329.63f to 350, // E4
                    392.00f to 700, // G4
                    329.63f to 250, // E4
                    392.00f to 250, // G4
                    523.25f to 500, // C5
                    493.88f to 350, // B4
                    440.00f to 350, // A4
                    440.00f to 350, // A4
                    392.00f to 700, // G4
                    293.66f to 250, // D4
                    329.63f to 250, // E4
                    349.23f to 500, // F4
                    293.66f to 350, // D4
                    349.23f to 500, // F4
                    392.00f to 700  // G4
                )
                playMelodySequence(melody)
                onComplete?.invoke()
            } catch (e: CancellationException) {
                // Expected on stop
            } catch (e: Exception) {
                Log.w(TAG, "playLullaby error: ${e.message}")
            }
        }
    }

    /**
     * Play gentle Twinkle Twinkle Little Star melody.
     */
    fun playTwinkleMelody(onComplete: (() -> Unit)? = null) {
        stopAudio()
        activeJob = scope.launch {
            try {
                val melody = listOf(
                    261.63f to 380, // C4
                    261.63f to 380, // C4
                    392.00f to 380, // G4
                    392.00f to 380, // G4
                    440.00f to 380, // A4
                    440.00f to 380, // A4
                    392.00f to 760, // G4
                    349.23f to 380, // F4
                    349.23f to 380, // F4
                    329.63f to 380, // E4
                    329.63f to 380, // E4
                    293.66f to 380, // D4
                    293.66f to 380, // D4
                    261.63f to 760  // C4
                )
                playMelodySequence(melody)
                onComplete?.invoke()
            } catch (e: CancellationException) {
                // Cancelled smoothly
            } catch (e: Exception) {
                Log.w(TAG, "playTwinkleMelody error: ${e.message}")
            }
        }
    }

    /**
     * Play a gentle interactive bubble pop sound effect (soft, non-startling).
     */
    fun playBubblePop() {
        scope.launch {
            try {
                val durationMs = 80
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    // Soft pitch sweep from 680Hz down to 340Hz
                    val freq = 680f - (340f * progress)
                    // Hann window envelope for gentle pop with zero click
                    val envelope = sin(PI * progress).toFloat()
                    val sample = sin(2.0 * PI * freq * i / SAMPLE_RATE).toFloat() * envelope * 0.22f
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                playPcmBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    /**
     * Play cute animal audio effect.
     */
    fun playAnimalSound(theme: BabySoundTheme) {
        stopAudio()
        activeJob = scope.launch {
            try {
                when (theme) {
                    BabySoundTheme.ANIMAL_MEOW -> {
                        // Soft kitten purr and meow sweep
                        val numSamples = (SAMPLE_RATE * 650) / 1000
                        val buffer = ShortArray(numSamples)
                        for (i in 0 until numSamples) {
                            val p = i.toFloat() / numSamples
                            val freq = if (p < 0.4f) 460f + 160f * (p / 0.4f) else 620f - 200f * ((p - 0.4f) / 0.6f)
                            val env = sin(PI * p).toFloat() * 0.20f
                            val s = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * 0.8 + sin(4.0 * PI * freq * i / SAMPLE_RATE) * 0.2).toFloat()
                            buffer[i] = (s * env * Short.MAX_VALUE).toInt().toShort()
                        }
                        playPcmBuffer(buffer)
                    }
                    BabySoundTheme.ANIMAL_WOOF -> {
                        // Playful gentle puppy double-blip
                        playBoingNote(280f, 160)
                        delay(90)
                        playBoingNote(340f, 200)
                    }
                    BabySoundTheme.ANIMAL_MOO -> {
                        // Warm low-frequency cow hum
                        val numSamples = (SAMPLE_RATE * 800) / 1000
                        val buffer = ShortArray(numSamples)
                        for (i in 0 until numSamples) {
                            val p = i.toFloat() / numSamples
                            val freq = 160f - 20f * p
                            val env = sin(PI * p).toFloat() * 0.22f
                            val s = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * 0.7 + sin(4.0 * PI * freq * i / SAMPLE_RATE) * 0.3).toFloat()
                            buffer[i] = (s * env * Short.MAX_VALUE).toInt().toShort()
                        }
                        playPcmBuffer(buffer)
                    }
                    BabySoundTheme.ANIMAL_QUACK -> {
                        // Gentle cartoon duck quack
                        playBoingNote(400f, 140)
                        delay(70)
                        playBoingNote(420f, 160)
                    }
                    BabySoundTheme.GENTLE_CHIME -> {
                        // Soft chime chord (E5, G#5, B5)
                        val chord = listOf(659.25f to 450, 830.61f to 450, 987.77f to 700)
                        playMelodySequence(chord)
                    }
                    else -> {
                        playLullaby()
                    }
                }
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                Log.w(TAG, "playAnimalSound error: ${e.message}")
            }
        }
    }

    private suspend fun playBoingNote(freq: Float, durationMs: Int) {
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val p = i.toFloat() / numSamples
            val env = sin(PI * p).toFloat() * 0.22f
            val s = sin(2.0 * PI * freq * i / SAMPLE_RATE).toFloat()
            buffer[i] = (s * env * Short.MAX_VALUE).toInt().toShort()
        }
        playPcmBuffer(buffer)
    }

    private suspend fun playMelodySequence(melody: List<Pair<Float, Int>>) {
        for ((freq, durationMs) in melody) {
            if (!coroutineContext.isActive) break
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            val attackSamples = (SAMPLE_RATE * 0.04f).toInt().coerceAtLeast(1)
            val decaySamples = numSamples - attackSamples

            for (i in 0 until numSamples) {
                val envelope = if (i < attackSamples) {
                    (i.toFloat() / attackSamples)
                } else {
                    val decayProgress = (i - attackSamples).toFloat() / decaySamples
                    (1.0f - decayProgress * 0.75f).coerceAtLeast(0f)
                } * SAFE_MAX_AMPLITUDE

                // Pure sine fundamental + soft warm second harmonic
                val s1 = sin(2.0 * PI * freq * i / SAMPLE_RATE) * 0.85
                val s2 = sin(4.0 * PI * freq * i / SAMPLE_RATE) * 0.15
                val sample = (s1 + s2).toFloat() * envelope

                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }

            playPcmBuffer(buffer)
            delay(40) // Gentle pause between notes for music box cadence
        }
    }

    private suspend fun playPcmBuffer(buffer: ShortArray) = withContext(Dispatchers.IO) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackSize = buffer.size * 2
            val finalBufferSize = maxOf(minBufSize, trackSize)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(finalBufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            currentTrack = track
            track.write(buffer, 0, buffer.size)
            track.play()

            // Wait for duration of track
            val durationMs = (buffer.size * 1000L) / SAMPLE_RATE
            delay(durationMs)

            track.stop()
            track.release()
            currentTrack = null
        } catch (_: Exception) {}
    }
}
