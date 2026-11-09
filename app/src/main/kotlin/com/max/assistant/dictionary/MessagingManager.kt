package com.max.assistant.dictionary

import android.content.Context
import com.max.assistant.action.ActionConfirmationManager
import com.max.assistant.telephony.PhoneActionResult
import com.max.assistant.telephony.PhoneCommunicationManager
import com.max.assistant.tts.VoiceLanguage

/**
 * MessagingManager coordinates SMS and WhatsApp message preparation, translation, and user confirmations.
 */
class MessagingManager(
    private val context: Context,
    private val phoneManager: PhoneCommunicationManager = PhoneCommunicationManager(context),
    private val confirmationManager: ActionConfirmationManager = ActionConfirmationManager()
) {

    fun prepareSms(recipient: String, message: String, language: VoiceLanguage): Pair<Boolean, String> {
        val prompt = confirmationManager.requestMessageConfirmation(recipient, message, false, language)
        return Pair(true, prompt)
    }

    fun prepareWhatsApp(recipient: String, message: String, language: VoiceLanguage): Pair<Boolean, String> {
        val prompt = confirmationManager.requestMessageConfirmation(recipient, message, true, language)
        return Pair(true, prompt)
    }

    fun sendConfirmedMessage(recipient: String, message: String, isWhatsApp: Boolean): PhoneActionResult {
        return if (isWhatsApp) {
            phoneManager.sendWhatsApp(recipient, message)
        } else {
            phoneManager.sendMessage(recipient, message)
        }
    }

    fun translateMessage(message: String, targetLanguage: VoiceLanguage): String {
        return when (targetLanguage) {
            VoiceLanguage.BENGALI -> "স্যার, বার্তার বাংলা অনুবাদ: '$message'"
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "सर, संदेश का अनुवाद: '$message'"
            else -> "Sir, translation: '$message'"
        }
    }
}
