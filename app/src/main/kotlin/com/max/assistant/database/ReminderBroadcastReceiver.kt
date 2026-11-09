package com.max.assistant.database

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.max.assistant.MainActivity
import com.max.assistant.service.MaxForegroundService
import com.max.assistant.tts.VoiceLanguage

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ReminderScheduler.ACTION_REMINDER_TRIGGER) return

        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "MAX Reminder"
        val notes = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_NOTES) ?: ""
        val priority = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_PRIORITY) ?: "MEDIUM"

        Log.i(TAG, "Reminder triggered: id=$reminderId, title='$title'")

        // 1. Post High-Priority Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_COMMAND", "read my reminders")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifBuilder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ MAX Reminder: $title")
            .setContentText(if (notes.isNotBlank()) notes else "Sir, it's time for your scheduled reminder.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(Color.CYAN, 3000, 3000)

        notificationManager.notify(reminderId.toInt(), notifBuilder.build())

        // 2. Announce through active MAX Assistant instance if service is running
        val spokenAlert = "Sir, attention please! Scheduled reminder: $title."
        MaxForegroundService.activeInstance?.voiceEngine?.speak(spokenAlert, VoiceLanguage.HINGLISH)

        // 3. Update database / Repeat interval
        val dbHelper = MaxDatabaseHelper.getInstance(context)
        val record = dbHelper.getReminderById(reminderId)
        if (record != null) {
            if (record.repeatInterval != "NONE") {
                val intervalMillis = when (record.repeatInterval) {
                    "DAILY" -> 86400000L
                    "WEEKLY" -> 604800000L
                    "MONTHLY" -> 2592000000L
                    else -> 86400000L
                }
                val nextTrigger = System.currentTimeMillis() + intervalMillis
                dbHelper.updateReminderTrigger(reminderId, nextTrigger)
                ReminderScheduler(context).scheduleReminder(
                    title = record.title,
                    notes = record.notes,
                    triggerEpoch = nextTrigger,
                    repeatInterval = record.repeatInterval,
                    priority = record.priority
                )
            } else {
                dbHelper.setReminderActive(reminderId, false)
            }
        }
    }
}
