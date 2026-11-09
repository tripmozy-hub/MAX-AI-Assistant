package com.max.assistant.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.max.assistant.database.ReminderScheduler
import com.max.assistant.speech.AssistantState
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.vision.ScreenVisionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MaxOperatingMode {
    ACTIVE,        // Normal active assistant mode
    STANDBY,       // Low-power wake word listening only
    FULL_SHUTDOWN  // Completely turned off; microphone released, no background restarts
}

class MaxServiceController(
    private val context: Context,
    private val service: MaxForegroundService,
    private val screenVisionManager: ScreenVisionManager
) {
    companion object {
        private const val TAG = "MaxServiceController"
        private const val PREFS_NAME = "max_service_controller"
        private const val KEY_OPERATING_MODE = "operating_mode"
        private const val KEY_KEEP_REMINDERS_ON_SHUTDOWN = "keep_reminders_on_shutdown"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _operatingMode = MutableStateFlow(loadOperatingMode())
    val operatingMode: StateFlow<MaxOperatingMode> = _operatingMode.asStateFlow()

    private fun loadOperatingMode(): MaxOperatingMode {
        val name = prefs.getString(KEY_OPERATING_MODE, MaxOperatingMode.ACTIVE.name) ?: MaxOperatingMode.ACTIVE.name
        return try {
            MaxOperatingMode.valueOf(name)
        } catch (_: Exception) {
            MaxOperatingMode.ACTIVE
        }
    }

    fun isFullyShutdown(): Boolean = _operatingMode.value == MaxOperatingMode.FULL_SHUTDOWN

    fun isStandby(): Boolean = _operatingMode.value == MaxOperatingMode.STANDBY

    fun isKeepRemindersActiveOnShutdown(): Boolean {
        return prefs.getBoolean(KEY_KEEP_REMINDERS_ON_SHUTDOWN, true)
    }

    fun setKeepRemindersActiveOnShutdown(keep: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_REMINDERS_ON_SHUTDOWN, keep).apply()
    }

    /**
     * Mode A: STANDBY
     * Pauses conversations, stops screen vision, halts browser/AI ops.
     * Retains only minimum wake-word listener.
     */
    fun enterStandby(language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        Log.i(TAG, "Entering STANDBY mode.")
        service.voiceEngine.stop()
        screenVisionManager.cleanupResources()
        service.taskExecutionEngine.cancelActivePlan()
        service.taskPlanner.cancelActivePlan("Standby mode activated")

        service.speechManager.enterStandby()

        _operatingMode.value = MaxOperatingMode.STANDBY
        prefs.edit().putString(KEY_OPERATING_MODE, MaxOperatingMode.STANDBY.name).apply()

        service.updateNotification("MAX is on Standby (Say 'Hey MAX' or 'MAX')")

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, MAX Standby mode mein switch ho gaya hai. Sirf wake-word active rahega."
            VoiceLanguage.HINDI -> "सर, मैक्स स्टैंडबाई मोड में है। केवल वेक-वर्ड सक्रिय रहेगा।"
            VoiceLanguage.BENGALI -> "স্যার, ম্যাক্স স্ট্যান্ডবাই মোডে আছে।"
            else -> "Sir, MAX is now in Standby mode. Only the wake-word listener remains active."
        }
    }

    /**
     * Mode B: FULL SHUTDOWN
     * Completely stops speech recognition, wake-word, TTS, AI requests, screen vision.
     * Releases microphone hardware and terminates foreground service.
     * Manual reactivation required via app icon.
     */
    fun executeFullShutdown(
        keepReminders: Boolean = isKeepRemindersActiveOnShutdown(),
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): String {
        Log.i(TAG, "Executing FULL SHUTDOWN. keepReminders=$keepReminders")

        // 1. Spoken acknowledgment before killing audio
        val goodbyeMsg = when (language) {
            VoiceLanguage.HINGLISH -> "Sir, MAX completely shutdown ho raha hai. Sabhi background services aur microphone release kiye ja rahe hain. Dobara shuru karne ke liye app icon tap karein."
            VoiceLanguage.HINDI -> "सर, मैक्स पूरी तरह बंद हो रहा है। माइक्रोफोन और सभी सेवाएँ समाप्त की जा रही हैं। ऐप आइकन से पुनः शुरू करें।"
            VoiceLanguage.BENGALI -> "স্যার, ম্যাক্স সম্পূর্ণরূপে বন্ধ হচ্ছে। পুনরায় শুরু করতে অ্যাপ আইকন ব্যবহার করুন।"
            else -> "Sir, executing complete shutdown of MAX. All foreground services and microphone resources are now terminated. Tap the app icon to reactivate."
        }

        // 2. Stop vision
        screenVisionManager.cleanupResources()

        // 3. Stop speech & release mic
        service.speechManager.destroy()

        // 4. Optionally clear or retain reminders
        if (!keepReminders) {
            Log.i(TAG, "Reminders disabled during shutdown.")
        } else {
            Log.i(TAG, "Reminders retained active in Android AlarmManager.")
        }

        // 5. Update state
        _operatingMode.value = MaxOperatingMode.FULL_SHUTDOWN
        prefs.edit().putString(KEY_OPERATING_MODE, MaxOperatingMode.FULL_SHUTDOWN.name).apply()

        // 6. Stop Foreground Service
        service.stopForeground(true)
        service.stopSelf()

        return goodbyeMsg
    }

    /**
     * Reactivates MAX when opened manually from MainActivity.
     */
    fun reactivate(language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        Log.i(TAG, "Reactivating MAX from shutdown/standby.")
        _operatingMode.value = MaxOperatingMode.ACTIVE
        prefs.edit().putString(KEY_OPERATING_MODE, MaxOperatingMode.ACTIVE.name).apply()

        service.speechManager.startListeningLoop()
        service.updateNotification("MAX Intelligent Assistant Active")

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, MAX reactivate ho chuka hai. All systems online."
            VoiceLanguage.HINDI -> "सर, मैक्स पुनः सक्रिय हो गया है। सभी प्रणालियाँ तैयार हैं।"
            VoiceLanguage.BENGALI -> "স্যার, ম্যাক্স পুনরায় সক্রিয় হয়েছে।"
            else -> "All systems online, Sir. MAX is fully reactivated."
        }
    }
}
