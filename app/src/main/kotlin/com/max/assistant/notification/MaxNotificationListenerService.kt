package com.max.assistant.notification

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class CachedNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val isOngoing: Boolean,
    val isUrgent: Boolean
)

class MaxNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MaxNotificationListener"

        @Volatile
        var isServiceConnected: Boolean = false
            private set

        @Volatile
        var activeInstance: MaxNotificationListenerService? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        activeInstance = this
        Log.i(TAG, "NotificationListenerService connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        activeInstance = null
        Log.w(TAG, "NotificationListenerService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: ""
        // Ignore self notifications
        if (pkg == packageName) return

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val isOngoing = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0

        // Skip blank or purely ongoing media/download items
        if (title.isBlank() && text.isBlank()) return

        val isUrgent = pkg.contains("dialer", ignoreCase = true) ||
                pkg.contains("telecom", ignoreCase = true) ||
                title.contains("missed call", ignoreCase = true) ||
                title.contains("missed", ignoreCase = true) ||
                text.contains("emergency", ignoreCase = true)

        val cached = CachedNotification(
            key = sbn.key,
            packageName = pkg,
            title = title,
            text = text,
            postTime = sbn.postTime,
            isOngoing = isOngoing,
            isUrgent = isUrgent
        )

        IntelligentNotificationManager.getInstance(applicationContext).recordNotification(cached)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) {
            IntelligentNotificationManager.getInstance(applicationContext).removeNotification(sbn.key)
        }
    }
}
