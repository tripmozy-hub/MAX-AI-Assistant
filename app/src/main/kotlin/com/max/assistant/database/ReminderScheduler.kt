package com.max.assistant.database

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "ReminderScheduler"
        const val CHANNEL_ID = "max_reminders_channel"
        const val ACTION_REMINDER_TRIGGER = "com.max.assistant.action.TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_NOTES = "extra_reminder_notes"
        const val EXTRA_REMINDER_PRIORITY = "extra_reminder_priority"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "MAX Reminders & Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Timely alerts, study schedules, and task reminders scheduled by MAX AI"
                    enableVibration(true)
                    enableLights(true)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    private val dbHelper = MaxDatabaseHelper.getInstance(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    init {
        createNotificationChannel(context)
    }

    fun scheduleReminder(
        title: String,
        notes: String = "",
        triggerEpoch: Long,
        repeatInterval: String = "NONE",
        priority: String = "MEDIUM",
        language: VoiceLanguage = VoiceLanguage.HINGLISH
    ): Pair<Boolean, String> {
        val am = alarmManager ?: return Pair(false, "Alarm manager service is unavailable on this device, Sir.")

        val now = System.currentTimeMillis()
        val effectiveEpoch = if (triggerEpoch <= now) now + 60000L else triggerEpoch

        val record = ReminderRecord(
            title = title.trim(),
            notes = notes.trim(),
            triggerTimeEpoch = effectiveEpoch,
            repeatInterval = repeatInterval.uppercase(Locale.ROOT),
            isActive = true,
            priority = priority.uppercase(Locale.ROOT)
        )

        val id = dbHelper.insertReminder(record)
        if (id <= 0) {
            val errMsg = when (language) {
                VoiceLanguage.HINGLISH -> "Sir, database mein reminder save nahi ho paya."
                VoiceLanguage.HINDI -> "सर, डेटाबेस में रिमाइंडर सहेजा नहीं जा सका।"
                VoiceLanguage.BENGALI -> "স্যার, ডাটাবেসে রিমাইন্ডার সংরক্ষণ করা যায়নি।"
                else -> "Sir, failed to write the reminder into the local database."
            }
            return Pair(false, errMsg)
        }

        val success = setExactAlarm(id, title, notes, effectiveEpoch, priority)
        if (!success) {
            dbHelper.deleteReminder(id)
            val failMsg = when (language) {
                VoiceLanguage.HINGLISH -> "Sir, phone system ne alarm schedule karne ki permission nahi di."
                VoiceLanguage.HINDI -> "सर, सिस्टम ने अलार्म शेड्यूल करने की अनुमति नहीं दी।"
                VoiceLanguage.BENGALI -> "স্যার, ফোন সিস্টেম এলার্ম নির্ধারণের অনুমতি দেয়নি।"
                else -> "Sir, the Android system denied exact alarm scheduling permission."
            }
            return Pair(false, failMsg)
        }

        val timeFmt = SimpleDateFormat("h:mm a, d MMM", Locale.ENGLISH)
        val formattedTime = timeFmt.format(Date(effectiveEpoch))

        val confirmMsg = when (language) {
            VoiceLanguage.HINGLISH -> "Sir, '$title' ka reminder $formattedTime ke liye set kar diya hai."
            VoiceLanguage.HINDI -> "सर, '$title' का रिमाइंडर $formattedTime के लिए सफलतापूर्वक शेड्यूल कर दिया गया है।"
            VoiceLanguage.BENGALI -> "স্যার, '$title'-এর রিমাইন্ডার $formattedTime-এর জন্য নির্ধারিত হয়েছে।"
            else -> "Sir, your reminder for '$title' has been successfully scheduled for $formattedTime."
        }
        return Pair(true, confirmMsg)
    }

    private fun setExactAlarm(id: Long, title: String, notes: String, triggerEpoch: Long, priority: String): Boolean {
        val am = alarmManager ?: return false
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_NOTES, notes)
            putExtra(EXTRA_REMINDER_PRIORITY, priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pendingIntent)
            }
            Log.i(TAG, "Exact alarm scheduled for reminder id=$id at epoch=$triggerEpoch")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm: ${e.message}")
            false
        }
    }

    fun cancelReminder(id: Long): Boolean {
        val am = alarmManager ?: return false
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            am.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        return dbHelper.deleteReminder(id)
    }

    fun rescheduleAllActiveReminders() {
        val list = dbHelper.getAllActiveReminders()
        val now = System.currentTimeMillis()
        Log.i(TAG, "Rescheduling ${list.size} active reminders after boot/recovery.")
        for (rem in list) {
            if (rem.triggerTimeEpoch > now) {
                setExactAlarm(rem.id, rem.title, rem.notes, rem.triggerTimeEpoch, rem.priority)
            } else if (rem.repeatInterval != "NONE") {
                // Calculate next occurrence
                val intervalMillis = when (rem.repeatInterval) {
                    "DAILY" -> 86400000L
                    "WEEKLY" -> 604800000L
                    "MONTHLY" -> 2592000000L
                    else -> 86400000L
                }
                var nextTrigger = rem.triggerTimeEpoch
                while (nextTrigger <= now) {
                    nextTrigger += intervalMillis
                }
                dbHelper.updateReminderTrigger(rem.id, nextTrigger)
                setExactAlarm(rem.id, rem.title, rem.notes, nextTrigger, rem.priority)
            } else {
                dbHelper.setReminderActive(rem.id, false)
            }
        }
    }

    fun getActiveRemindersSummary(language: VoiceLanguage): String {
        val list = dbHelper.getAllActiveReminders()
        if (list.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINGLISH -> "Sir, abhi aapka koi scheduled reminder pending nahi hai."
                VoiceLanguage.HINDI -> "सर, अभी आपके पास कोई सक्रिय रिमाइंडर नहीं है।"
                VoiceLanguage.BENGALI -> "স্যার, এই মুহূর্তে কোনো সক্রিয় রিমাইন্ডার নেই।"
                else -> "Sir, you currently have no active reminders scheduled."
            }
        }

        val timeFmt = SimpleDateFormat("h:mm a, d MMM", Locale.ENGLISH)
        val formattedList = list.take(3).joinToString("; ") { rem ->
            "${rem.title} (${timeFmt.format(Date(rem.triggerTimeEpoch))})"
        }

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, aapke ${list.size} active reminders hain: $formattedList"
            VoiceLanguage.HINDI -> "सर, आपके पास ${list.size} सक्रिय रिमाइंडर हैं: $formattedList"
            VoiceLanguage.BENGALI -> "স্যার, আপনার ${list.size} টি সক্রিয় রিমাইন্ডার রয়েছে: $formattedList"
            else -> "Sir, you have ${list.size} active reminders: $formattedList"
        }
    }
}
