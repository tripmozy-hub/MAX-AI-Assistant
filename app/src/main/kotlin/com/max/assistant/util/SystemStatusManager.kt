package com.max.assistant.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import com.max.assistant.tts.VoiceLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemStatusManager(private val context: Context) {

    companion object {
        private const val TAG = "SystemStatusManager"
    }

    /**
     * Gets current battery percentage (0-100) and charging status.
     */
    fun getBatteryStatus(): Pair<Int, Boolean> {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging == true
        return Pair(level, isCharging)
    }

    /**
     * Gets natural localized battery statement.
     */
    fun getBatteryStatement(language: VoiceLanguage): String {
        val (level, isCharging) = getBatteryStatus()
        val chargeTextEn = if (isCharging) "and currently charging" else "and not charging"
        val chargeTextHi = if (isCharging) "और फ़ोन चार्ज हो रहा है" else "और चार्जिंग पर नहीं है"
        val chargeTextBn = if (isCharging) "এবং বর্তমানে চার্জ হচ্ছে" else "এবং চার্জিং হচ্ছে না"

        return when (language) {
            VoiceLanguage.HINGLISH -> {
                if (level >= 0) "Sir, battery $level percent hai${if (isCharging) " aur charging chal rahi hai" else " aur charging nahi ho rahi"}."
                else "Sir, battery ki information abhi available nahi hai."
            }
            VoiceLanguage.HINDI -> {
                if (level >= 0) "सर, आपकी बैटरी $level प्रतिशत है $chargeTextHi।"
                else "सर, बैटरी स्तर की जानकारी उपलब्ध नहीं है।"
            }
            VoiceLanguage.BENGALI -> {
                if (level >= 0) "স্যার, আপনার ব্যাটারি $level শতাংশ $chargeTextBn।"
                else "স্যার, ব্যাটারি তথ্য অনুপলব্ধ।"
            }
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> {
                if (level >= 0) "Sir, the battery is at $level percent $chargeTextEn."
                else "Sir, battery status is currently unavailable."
            }
        }
    }

    /**
     * Checks internet connectivity (Wi-Fi, Cellular, None).
     */
    fun getNetworkStatus(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Unknown"
        val network = cm.activeNetwork ?: return "Disconnected"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Disconnected"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (High-Speed)"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 5G/LTE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    fun getNetworkStatement(language: VoiceLanguage): String {
        val net = getNetworkStatus()
        val isOnline = net != "Disconnected" && net != "Unknown"

        return when (language) {
            VoiceLanguage.HINGLISH -> {
                if (isOnline) "Sir, internet active hai via $net. Sab kuch smooth chal raha hai."
                else "Sir, internet connection nahi hai abhi. Phone offline hai."
            }
            VoiceLanguage.HINDI -> {
                if (isOnline) "सर, इंटरनेट सुचारू रूप से सक्रिय है ($net)।"
                else "सर, कोई इंटरनेट कनेक्शन नहीं है। डिवाइस ऑफ़लाइन है।"
            }
            VoiceLanguage.BENGALI -> {
                if (isOnline) "স্যার, ইন্টারনেট সংযোগ সক্রিয় রয়েছে ($net)।"
                else "স্যার, কোনো ইন্টারনেট সংযোগ নেই। ডিভাইসটি অফলাইনে আছে।"
            }
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> {
                if (isOnline) "Sir, the internet connection is active and stable via $net."
                else "Sir, there is currently no active internet connection. We are operating offline."
            }
        }
    }

    /**
     * Formats current time in requested language.
     */
    fun getCurrentTime(language: VoiceLanguage): String {
        val locale = when (language) {
            VoiceLanguage.HINGLISH -> Locale("hi", "IN")
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.BENGALI -> Locale("bn", "IN")
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> Locale.UK
        }
        val timeFormat = SimpleDateFormat("hh:mm a", locale)
        val timeStr = timeFormat.format(Date())

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, abhi $timeStr baj rahe hain."
            VoiceLanguage.HINDI -> "सर, अभी समय $timeStr हुआ है।"
            VoiceLanguage.BENGALI -> "স্যার, এখন সময় $timeStr।"
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Sir, the current time is $timeStr."
        }
    }

    /**
     * Formats today's date in requested language.
     */
    fun getTodayDate(language: VoiceLanguage): String {
        val locale = when (language) {
            VoiceLanguage.HINGLISH -> Locale("hi", "IN")
            VoiceLanguage.HINDI -> Locale("hi", "IN")
            VoiceLanguage.BENGALI -> Locale("bn", "IN")
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> Locale.UK
        }
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", locale)
        val dateStr = dateFormat.format(Date())

        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, aaj ki date $dateStr hai."
            VoiceLanguage.HINDI -> "सर, आज की तारीख है $dateStr।"
            VoiceLanguage.BENGALI -> "স্যার, আজকের তারিখ হলো $dateStr।"
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Sir, today is $dateStr."
        }
    }

    /**
     * Sets an alarm using Android's standard AlarmClock intent.
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "MAX Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm: ${e.message}")
            false
        }
    }

    /**
     * Sets a timer in seconds using standard AlarmClock intent.
     */
    fun setTimer(seconds: Int, message: String = "MAX Timer"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer: ${e.message}")
            false
        }
    }

    /**
     * Opens Calendar to add an event.
     */
    fun openCalendarNewEvent(title: String = "New Event"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open calendar: ${e.message}")
            false
        }
    }
}
