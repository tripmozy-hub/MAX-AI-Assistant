package com.max.assistant.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import java.util.concurrent.ConcurrentHashMap

class IntelligentNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "IntelligentNotifMgr"

        @Volatile
        private var instance: IntelligentNotificationManager? = null

        fun getInstance(context: Context): IntelligentNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: IntelligentNotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val notificationMap = ConcurrentHashMap<String, CachedNotification>()

    fun recordNotification(notification: CachedNotification) {
        notificationMap[notification.key] = notification
        Log.d(TAG, "Recorded notification: [${notification.packageName}] ${notification.title}")
    }

    fun removeNotification(key: String) {
        notificationMap.remove(key)
    }

    fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val cn = ComponentName(context, MaxNotificationListenerService::class.java)
        return flat != null && flat.contains(cn.flattenToString())
    }

    fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings: ${e.message}")
        }
    }

    fun summarizeImportantNotifications(language: VoiceLanguage, isPrivateMode: Boolean = false): String =
        getImportantNotificationsSummary(language, isPrivateMode)

    fun getImportantNotificationsSummary(language: VoiceLanguage, isPrivateMode: Boolean = false): String {
        if (!isNotificationAccessGranted()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, notifications padhne ke liye Notification Access permission ki zaroorat hai. Settings mein MAX ko permission dein."
                VoiceLanguage.BENGALI -> "স্যার, নোটিফিকেশন পড়ার জন্য নোটিফিকেশন এক্সেস অনুমতি প্রয়োজন। দয়া করে সেটিংস থেকে অনুমতি দিন।"
                else -> "Sir, reading notifications requires Notification Access permission. Please grant permission in Settings."
            }
        }

        val all = notificationMap.values.filter { !it.isOngoing }
        if (all.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, abhi koi naya notification nahi hai."
                VoiceLanguage.BENGALI -> "স্যার, এই মুহূর্তে কোনো নতুন নোটিফিকেশন নেই।"
                else -> "Sir, you have no pending notifications at the moment."
            }
        }

        val missedCalls = all.filter {
            it.title.contains("missed call", ignoreCase = true) ||
            it.text.contains("missed call", ignoreCase = true) ||
            it.packageName.contains("dialer")
        }

        val whatsAppMsgs = all.filter {
            it.packageName == "com.whatsapp" || it.packageName == "com.whatsapp.w4b"
        }

        val others = all.filter { !missedCalls.contains(it) && !whatsAppMsgs.contains(it) }

        if (isPrivateMode) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, private mode on hai. Aapke paas ${all.size} notifications hain (${missedCalls.size} missed calls, ${whatsAppMsgs.size} messages)."
                VoiceLanguage.BENGALI -> "স্যার, প্রাইভেট মোড সক্রিয়। আপনার ${all.size} টি নোটিফিকেশন রয়েছে।"
                else -> "Sir, private mode is active. You have ${all.size} total notifications (${missedCalls.size} missed calls, ${whatsAppMsgs.size} messages)."
            }
        }

        val sb = StringBuilder()
        when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                sb.append("Sir, aapke paas ")
                val parts = mutableListOf<String>()
                if (missedCalls.isNotEmpty()) parts.add("${missedCalls.size} missed call")
                if (whatsAppMsgs.isNotEmpty()) parts.add("${whatsAppMsgs.size} WhatsApp message")
                if (others.isNotEmpty()) parts.add("${others.size} other alert")
                sb.append(parts.joinToString(", "))
                sb.append(" hain.\n")

                if (missedCalls.isNotEmpty()) {
                    sb.append("Missed calls: ")
                    sb.append(missedCalls.take(2).joinToString("; ") { it.title.ifBlank { it.text } })
                    sb.append(".\n")
                }
                if (whatsAppMsgs.isNotEmpty()) {
                    sb.append("Recent messages: ")
                    sb.append(whatsAppMsgs.take(2).joinToString("; ") { "${it.title}: ${it.text.take(35)}" })
                }
            }
            VoiceLanguage.BENGALI -> {
                sb.append("স্যার, আপনার ${all.size} টি নোটিফিকেশন রয়েছে: ")
                if (missedCalls.isNotEmpty()) sb.append("${missedCalls.size} টি মিসড কল, ")
                if (whatsAppMsgs.isNotEmpty()) sb.append("${whatsAppMsgs.size} টি মেসেজ।")
            }
            else -> {
                sb.append("Sir, you have ")
                val parts = mutableListOf<String>()
                if (missedCalls.isNotEmpty()) parts.add("${missedCalls.size} missed call(s)")
                if (whatsAppMsgs.isNotEmpty()) parts.add("${whatsAppMsgs.size} WhatsApp message(s)")
                if (others.isNotEmpty()) parts.add("${others.size} other alert(s)")
                sb.append(parts.joinToString(", "))
                sb.append(".\n")

                if (missedCalls.isNotEmpty()) {
                    sb.append("Missed calls from: ")
                    sb.append(missedCalls.take(2).joinToString(", ") { it.title })
                    sb.append(".\n")
                }
                if (whatsAppMsgs.isNotEmpty()) {
                    sb.append("Latest WhatsApp: ")
                    sb.append(whatsAppMsgs.take(2).joinToString("; ") { "${it.title} says '${it.text.take(40)}'" })
                }
            }
        }

        return sb.toString().trim()
    }

    fun readWhatsAppMessagesAloud(language: VoiceLanguage, isPrivateMode: Boolean = false): String {
        if (!isNotificationAccessGranted()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, WhatsApp notifications padhne ke liye Notification Access enable karein."
                else -> "Sir, please grant notification access to read WhatsApp messages aloud."
            }
        }

        if (isPrivateMode) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, private mode active hai. Screen par dekhne ke liye dashboard open karein."
                else -> "Sir, private mode is active. WhatsApp content cannot be spoken aloud."
            }
        }

        val whatsAppMsgs = notificationMap.values.filter {
            (it.packageName == "com.whatsapp" || it.packageName == "com.whatsapp.w4b") && !it.isOngoing
        }

        if (whatsAppMsgs.isEmpty()) {
            return when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, WhatsApp par koi unread message notification nahi hai."
                VoiceLanguage.BENGALI -> "স্যার, হোয়াটসঅ্যাপে কোনো নতুন নোটিফিকেশন নেই।"
                else -> "Sir, there are no unread WhatsApp message notifications."
            }
        }

        val sb = StringBuilder()
        when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                sb.append("Sir, aapke paas ${whatsAppMsgs.size} WhatsApp message hain:\n")
                whatsAppMsgs.take(3).forEachIndexed { i, m ->
                    sb.append("${i + 1}. ${m.title} se: \"${m.text}\"\n")
                }
            }
            VoiceLanguage.BENGALI -> {
                sb.append("স্যার, হোয়াটসঅ্যাপ মেসেজগুলি হলো:\n")
                whatsAppMsgs.take(3).forEachIndexed { i, m ->
                    sb.append("${i + 1}. ${m.title}: \"${m.text}\"\n")
                }
            }
            else -> {
                sb.append("Sir, here are your latest WhatsApp messages:\n")
                whatsAppMsgs.take(3).forEachIndexed { i, m ->
                    sb.append("${i + 1}. From ${m.title}: \"${m.text}\"\n")
                }
            }
        }
        return sb.toString().trim()
    }
}
