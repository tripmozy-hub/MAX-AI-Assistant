package com.max.assistant.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.max.assistant.tts.VoiceLanguage

interface IWebSearchService {
    fun searchWeb(query: String, context: Context): Boolean
}

class DefaultWebSearchService : IWebSearchService {
    override fun searchWeb(query: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

interface IReminderService {
    fun scheduleReminder(title: String, timestampMillis: Long): Boolean
}

interface ISmartHomeService {
    fun getDeviceStatus(deviceName: String): String
    fun toggleDevice(deviceName: String, state: Boolean): Boolean
}

interface IEmailService {
    fun composeEmail(recipient: String, subject: String, body: String, context: Context): Boolean
}

class DefaultEmailService : IEmailService {
    override fun composeEmail(recipient: String, subject: String, body: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

class AutomationHub(
    val weatherService: IWeatherService = LiveOpenMeteoWeatherService(),
    val webSearchService: IWebSearchService = DefaultWebSearchService(),
    val emailService: IEmailService = DefaultEmailService()
)
