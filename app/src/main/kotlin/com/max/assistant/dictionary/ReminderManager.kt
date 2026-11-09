package com.max.assistant.dictionary

import android.content.Context
import com.max.assistant.database.ReminderScheduler
import com.max.assistant.tts.VoiceLanguage

/**
 * ReminderManager manages reminders, exact alarms, repeat intervals, and snoozing.
 */
class ReminderManager(
    private val context: Context,
    private val scheduler: ReminderScheduler = ReminderScheduler(context)
) {

    fun schedule(
        title: String,
        notes: String = "",
        triggerEpoch: Long,
        repeatInterval: String = "NONE",
        priority: String = "MEDIUM",
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): Pair<Boolean, String> {
        return scheduler.scheduleReminder(title, notes, triggerEpoch, repeatInterval, priority, language)
    }

    fun listRemindersSummary(language: VoiceLanguage): String {
        return scheduler.getActiveRemindersSummary(language)
    }

    fun snoozeReminder(reminderId: Long, snoozeMinutes: Int = 10): Boolean {
        // Cancel existing and reschedule 10 minutes later
        val newTrigger = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        scheduler.cancelReminder(reminderId)
        val res = scheduler.scheduleReminder("Snoozed Reminder", "Snoozed by MAX", newTrigger, "NONE", "MEDIUM")
        return res.first
    }

    fun deleteReminder(reminderId: Long): Boolean {
        return scheduler.cancelReminder(reminderId)
    }
}
