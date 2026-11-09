package com.max.assistant.action

import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PendingAction {
    data class MakeCall(val target: String) : PendingAction()
    data class SendMessage(val recipient: String, val message: String, val isWhatsApp: Boolean = false) : PendingAction()
    data class DeleteData(val target: String) : PendingAction()
    data class AwaitingCallRecipient(val isPhone: Boolean = true) : PendingAction()
    data class AwaitingReplyRecipient(val isWhatsApp: Boolean = true) : PendingAction()
    data class AwaitingReplyContent(val recipient: String, val isWhatsApp: Boolean = true) : PendingAction()
    data class AwaitingMusicConfirmation(val query: String = "uplifting relaxing music") : PendingAction()
}

class ActionConfirmationManager {

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    fun requestCallRecipient() {
        _pendingAction.value = PendingAction.AwaitingCallRecipient()
    }

    fun requestReplyRecipient(isWhatsApp: Boolean = true) {
        _pendingAction.value = PendingAction.AwaitingReplyRecipient(isWhatsApp)
    }

    fun requestReplyContent(recipient: String, isWhatsApp: Boolean = true) {
        _pendingAction.value = PendingAction.AwaitingReplyContent(recipient, isWhatsApp)
    }

    fun requestMusicConfirmation(query: String = "uplifting relaxing music") {
        _pendingAction.value = PendingAction.AwaitingMusicConfirmation(query)
    }

    fun requestCallConfirmation(target: String, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        _pendingAction.value = PendingAction.MakeCall(target)
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, kya aap $target ko call lagana chahte hain?"
            VoiceLanguage.HINDI -> "सर, क्या आप $target को कॉल मिलाना चाहते हैं?"
            VoiceLanguage.BENGALI -> "স্যার, আপনি কি $target কে কল করতে চান?"
            else -> "Sir, would you like me to call $target?"
        }
    }

    fun requestMessageConfirmation(recipient: String, message: String, isWhatsApp: Boolean, language: VoiceLanguage = VoiceLanguage.HINGLISH): String {
        _pendingAction.value = PendingAction.SendMessage(recipient, message, isWhatsApp)
        val platform = if (isWhatsApp) "WhatsApp" else "SMS"
        return when (language) {
            VoiceLanguage.HINGLISH -> "Sir, kya aap $recipient ko yeh $platform message send karna chahte hain?"
            VoiceLanguage.HINDI -> "सर, क्या आप $recipient को $platform संदेश भेजना चाहते हैं?"
            VoiceLanguage.BENGALI -> "স্যার, আপনি কি $recipient কে $platform বার্তা পাঠাতে চান?"
            else -> "Sir, should I send this $platform message to $recipient?"
        }
    }

    fun getConfirmationPrompt(action: PendingAction, language: VoiceLanguage): String {
        return when (action) {
            is PendingAction.MakeCall -> when (language) {
                VoiceLanguage.HINGLISH -> "Sir, kya aap ${action.target} ko call lagana chahte hain?"
                VoiceLanguage.HINDI -> "सर, क्या आप ${action.target} को कॉल मिलाना चाहते हैं?"
                VoiceLanguage.BENGALI -> "স্যার, আপনি কি ${action.target} কে কল করতে চান?"
                else -> "Sir, would you like me to place a call to ${action.target}?"
            }
            is PendingAction.SendMessage -> {
                val app = if (action.isWhatsApp) "WhatsApp" else "SMS"
                when (language) {
                    VoiceLanguage.HINGLISH -> "Sir, kya aap ${action.recipient} ko yeh $app message bhejna chahte hain?"
                    VoiceLanguage.HINDI -> "सर, क्या आप ${action.recipient} को $app संदेश भेजना चाहते हैं?"
                    VoiceLanguage.BENGALI -> "স্যার, আপনি কি ${action.recipient} কে $app বার্তা পাঠাতে চান?"
                    else -> "Sir, would you like me to send this $app message to ${action.recipient}?"
                }
            }
            is PendingAction.DeleteData -> when (language) {
                VoiceLanguage.HINDI -> "सर, क्या आप इस जानकारी को मिटाना चाहते हैं?"
                VoiceLanguage.BENGALI -> "স্যার, আপনি কি এই তথ্যটি মুছে ফেলতে চান?"
                else -> "Sir, are you certain you wish to delete this data?"
            }
            is PendingAction.AwaitingCallRecipient -> when (language) {
                VoiceLanguage.HINDI -> "जी सर, आप किसे कॉल मिलाना चाहते हैं?"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনি কাকে কল করতে চান?"
                else -> "Sir, whom would you like me to call?"
            }
            is PendingAction.AwaitingReplyRecipient -> when (language) {
                VoiceLanguage.HINDI -> "जी सर, आप किसे उत्तर भेजना चाहते हैं?"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনি কাকে উত্তর পাঠাতে চান?"
                else -> "Sir, whom would you like to reply to?"
            }
            is PendingAction.AwaitingReplyContent -> when (language) {
                VoiceLanguage.HINDI -> "जी सर, ${action.recipient} को क्या उत्तर भेजना है?"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, ${action.recipient} কে কি উত্তর পাঠাতে হবে?"
                else -> "Sir, what reply should I send to ${action.recipient}?"
            }
            is PendingAction.AwaitingMusicConfirmation -> when (language) {
                VoiceLanguage.HINDI -> "सर, क्या आपका मूड ठीक करने के लिए स्पॉटीफ़ाई पर सुकून भरा संगीत चला दूँ?"
                VoiceLanguage.BENGALI -> "স্যার, আপনার মন ভালো করার জন্য কি স্পটিফাইতে সুন্দর গান চালিয়ে দেব?"
                else -> "Sir, would you like me to play some relaxing music on Spotify to lift your spirits?"
            }
        }
    }

    fun confirm(): PendingAction? {
        val action = _pendingAction.value
        _pendingAction.value = null
        return action
    }

    fun cancel() {
        _pendingAction.value = null
    }

    fun isConfirmationAffirmative(input: String): Boolean {
        val lower = input.lowercase().trim()
        val tokens = lower.split(Regex("[\\s,;:.!?]+")).filter { it.isNotEmpty() }

        val singleTokens = setOf(
            "yes", "yeah", "yup", "yep", "confirm", "proceed", "sure", "do",
            "send", "call", "haan", "ha", "bilkul", "karo", "milao", "bhejo",
            "theek", "sahi", "kardo", "bhej", "lagao", "chalao", "bajao", "ok", "okay",
            "हाँ", "हां", "हाँजी", "भेजो", "करो", "मिलाओ", "चलाओ", "बजाओ", "लगाओ",
            "হ্যাঁ", "হাঁ", "করুন", "পাঠান", "বাজাও"
        )
        if (tokens.any { it in singleTokens }) return true

        val affirmativePhrases = listOf(
            "do it", "send it", "bhej do", "kar do", "call karo", "call lagao",
            "yes please", "sure thing", "go ahead", "haan ji", "ha kardo",
            "theek hai", "sahi hai", "bilkul karo", "bhej dijiye", "gana chalao", "music chalao"
        )
        return affirmativePhrases.any { lower.contains(it) }
    }

    fun isConfirmationNegative(input: String): Boolean {
        val lower = input.lowercase().trim()
        val tokens = lower.split(Regex("[\\s,;:.!?]+")).filter { it.isNotEmpty() }

        val negativeTokens = setOf(
            "no", "nope", "cancel", "stop", "don't", "abort",
            "nahin", "nahi", "नहीं", "ना", "na", "ruko",
            "না", "করবেন না", "বাতিল"
        )
        if (tokens.any { it in negativeTokens }) return true

        val negativePhrases = listOf(
            "mat karo", "mat bhejo", "cancel karo", "ruk jao", "rehne do", "don't do it", "leave it"
        )
        return negativePhrases.any { lower.contains(it) }
    }
}
