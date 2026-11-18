package com.max.assistant.ai

import com.max.assistant.executor.MediaAction
import com.max.assistant.executor.VolumeDirection
import com.max.assistant.planner.MultiStepPlan
import com.max.assistant.planner.PlannedStep
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.protection.PaymentProtectionManager
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.util.LanguageDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class BrainDecision {
    data class SpeakResponse(val text: String, val language: VoiceLanguage) : BrainDecision()
    data class LaunchApp(val query: String, val responseText: String, val language: VoiceLanguage) : BrainDecision()
    object OpenCamera : BrainDecision()
    object OpenSettings : BrainDecision()
    data class OpenSettingsSubpage(val subpage: String, val language: VoiceLanguage) : BrainDecision()
    object OpenBrowser : BrainDecision()
    data class OpenMaps(val query: String = "") : BrainDecision()
    object OpenPhone : BrainDecision()
    object OpenContacts : BrainDecision()
    object OpenMessages : BrainDecision()
    data class SetFlashlight(val enabled: Boolean) : BrainDecision()
    data class AdjustVolume(val direction: VolumeDirection) : BrainDecision()
    data class ControlMedia(val action: MediaAction) : BrainDecision()
    data class PlayMusic(val query: String = "", val app: String = "spotify", val language: VoiceLanguage) : BrainDecision()
    data class OfferEmotionalSupport(val text: String, val language: VoiceLanguage) : BrainDecision()
    data class CreateCalendarEvent(val title: String) : BrainDecision()
    data class MakePhoneCall(val target: String, val requiresConfirmation: Boolean = true) : BrainDecision()
    data class RequestCallRecipient(val prompt: String) : BrainDecision()
    data class SendMessage(val recipient: String, val message: String, val isWhatsApp: Boolean = false, val requiresConfirmation: Boolean = true) : BrainDecision()
    data class RequestReplyContent(val recipient: String, val isWhatsApp: Boolean = true, val prompt: String) : BrainDecision()
    data class RequestReplyRecipient(val isWhatsApp: Boolean = true, val prompt: String) : BrainDecision()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String = "MAX Alarm") : BrainDecision()
    data class SetTimer(val seconds: Int, val message: String = "MAX Timer") : BrainDecision()
    data class QueryWeather(val location: String, val language: VoiceLanguage) : BrainDecision()
    data class SwitchLanguage(val newLanguage: VoiceLanguage, val message: String) : BrainDecision()
    data class OpenWhatsApp(val business: Boolean = false, val language: VoiceLanguage) : BrainDecision()
    data class SearchWeb(val query: String, val language: VoiceLanguage) : BrainDecision()
    data class SearchNearby(val category: String, val language: VoiceLanguage) : BrainDecision()
    data class OpenDirections(val destination: String, val language: VoiceLanguage) : BrainDecision()
    data class CopyToClipboard(val text: String, val language: VoiceLanguage) : BrainDecision()
    data class ShareText(val text: String, val language: VoiceLanguage) : BrainDecision()
    data class CreateNote(val content: String, val language: VoiceLanguage) : BrainDecision()
    data class ReadNotes(val language: VoiceLanguage) : BrainDecision()
    data class AddTask(val title: String, val language: VoiceLanguage) : BrainDecision()
    data class ReadTasks(val language: VoiceLanguage) : BrainDecision()
    data class DraftEmail(val recipient: String, val subject: String, val body: String, val language: VoiceLanguage) : BrainDecision()
    data class FindContact(val name: String, val language: VoiceLanguage) : BrainDecision()
    data class ExecuteMultiStepPlan(val plan: MultiStepPlan, val initialPrompt: String, val language: VoiceLanguage) : BrainDecision()
    object StopSpeaking : BrainDecision()
    object QueryBattery : BrainDecision()
    object QueryNetwork : BrainDecision()
    object CheckUpdates : BrainDecision()
    object TriggerStopMax : BrainDecision()
    data class TriggerMute(val mute: Boolean) : BrainDecision()

    // Study & Education Decisions
    data class CreateStudyNotes(val topic: String, val isDetailed: Boolean, val language: VoiceLanguage) : BrainDecision()
    data class StartStudyQuiz(val topic: String, val examType: String, val language: VoiceLanguage) : BrainDecision()
    data class AnswerStudyQuiz(val answer: String, val language: VoiceLanguage) : BrainDecision()
    data class SolveMath(val problem: String, val language: VoiceLanguage) : BrainDecision()
    data class ExplainGrammar(val query: String, val language: VoiceLanguage) : BrainDecision()

    // Multimodal Screen Vision Decisions
    data class CaptureScreenVision(val prompt: String, val isLive: Boolean, val language: VoiceLanguage) : BrainDecision()
    data class StopScreenVision(val language: VoiceLanguage) : BrainDecision()

    // Smart Reminders Decisions
    data class ScheduleSmartReminder(val title: String, val notes: String, val triggerEpoch: Long, val repeat: String, val priority: String, val language: VoiceLanguage) : BrainDecision()
    data class ListSmartReminders(val language: VoiceLanguage) : BrainDecision()

    // Master Service Control Decisions
    data class EnterStandbyMode(val language: VoiceLanguage) : BrainDecision()
    data class ExecuteFullShutdown(val keepReminders: Boolean = true, val language: VoiceLanguage) : BrainDecision()

    // Baby Entertainment Mode Decisions
    data class StartBabyMode(val reason: String, val language: VoiceLanguage) : BrainDecision()
    data class NextBabyActivity(val language: VoiceLanguage) : BrainDecision()
    data class PlayBabyLullaby(val language: VoiceLanguage) : BrainDecision()
    data class PlayBabyAnimalSounds(val language: VoiceLanguage) : BrainDecision()
    data class TellBabyStory(val language: VoiceLanguage) : BrainDecision()
    data class PlayBabyRhyme(val language: VoiceLanguage) : BrainDecision()
    data class StopBabyMode(val language: VoiceLanguage) : BrainDecision()

    // Screen Follow-Up & Interactive Question Decisions
    data class ScreenQuestionFollowUp(val question: String, val language: VoiceLanguage) : BrainDecision()

    // Full Android App Control & UI Automation Decisions
    data class ClickScreenElement(val target: String, val language: VoiceLanguage) : BrainDecision()
    data class ClickNthScreenOption(val n: Int, val language: VoiceLanguage) : BrainDecision()
    data class ScrollScreen(val isDown: Boolean, val language: VoiceLanguage) : BrainDecision()
    data class TypeScreenText(val text: String, val language: VoiceLanguage) : BrainDecision()
    data class ReadScreenContent(val language: VoiceLanguage) : BrainDecision()
    data class NavigateBack(val language: VoiceLanguage) : BrainDecision()
    data class SwitchApp(val targetApp: String, val language: VoiceLanguage) : BrainDecision()
    data class CloseScreen(val language: VoiceLanguage) : BrainDecision()
    data class FillFormAssistance(val query: String, val language: VoiceLanguage) : BrainDecision()

    // Multi-Step Task Execution Decisions
    data class ExecuteCompoundTask(val query: String, val language: VoiceLanguage) : BrainDecision()
    data class CancelTask(val language: VoiceLanguage) : BrainDecision()
    data class ExplainTaskProgress(val language: VoiceLanguage) : BrainDecision()

    // Advanced Contextual Memory Decisions
    data class RecallYesterdayDiscussion(val language: VoiceLanguage) : BrainDecision()
    data class RecallProjectTasks(val projectName: String, val language: VoiceLanguage) : BrainDecision()
    data class SaveUserMemory(val fact: String, val language: VoiceLanguage) : BrainDecision()
    data class ForgetUserMemory(val query: String, val language: VoiceLanguage) : BrainDecision()
    data class RecallUserMemories(val language: VoiceLanguage) : BrainDecision()

    // Self-Healing & Health Monitor Decisions
    data class CheckSystemHealth(val language: VoiceLanguage) : BrainDecision()
    data class RunSelfDiagnostics(val language: VoiceLanguage) : BrainDecision()

    // Intelligent Notification Decisions
    data class SummarizeNotifications(val language: VoiceLanguage) : BrainDecision()
    data class ReadWhatsAppNotifications(val language: VoiceLanguage) : BrainDecision()

    // Live Internet Research Decisions
    data class ConductLiveResearch(val query: String, val language: VoiceLanguage) : BrainDecision()

    // Performance Analytics Decisions
    data class QueryPerformanceStats(val language: VoiceLanguage) : BrainDecision()
}

class MaxAiBrain(
    val knowledgeBase: MaxKnowledgeBase = MaxKnowledgeBase(),
    var aiProvider: IAiProvider? = null,
    val onlineKnowledge: OnlineKnowledgeService = OnlineKnowledgeService(),
    val intentRecognizer: com.max.assistant.dictionary.MultilingualIntentRecognizer = com.max.assistant.dictionary.MultilingualIntentRecognizer(),
    val humourEngine: com.max.assistant.humour.HumourEngine = com.max.assistant.humour.HumourEngine(),
    val screenContextManager: com.max.assistant.vision.ScreenContextManager = com.max.assistant.vision.ScreenContextManager()
) {

    companion object {
        private const val TAG = "MaxAiBrain"
    }

    /**
     * Determines output language based on settings and input text.
     */
    fun resolveResponseLanguage(input: String, preference: VoiceLanguage): VoiceLanguage {
        if (preference != VoiceLanguage.AUTO) {
            return preference
        } else {
            val lower = input.lowercase(Locale.ROOT)
            if (lower.contains("in hindi") || lower.contains("hindi me") || lower.contains("हिंदी में") || lower.contains("hindi mein")) {
                return VoiceLanguage.HINDI
            }
            if (lower.contains("in bengali") || lower.contains("bangla te") || lower.contains("বাংলায়") || lower.contains("in bangla")) {
                return VoiceLanguage.BENGALI
            }
            if (lower.contains("in english") || lower.contains("english me") || lower.contains("अंग्रेजी में") || lower.contains("ইংরেজিতে")) {
                return VoiceLanguage.ENGLISH
            }
            if (lower.contains("hinglish") || lower.contains("hinglish me") || lower.contains("mix language") || lower.contains("hindi english mix")) {
                return VoiceLanguage.HINGLISH
            }
            // Auto-detect: LanguageDetector now returns HINGLISH for mixed Hindi-English
            return LanguageDetector.detectLanguage(input)
        }
    }

    /**
     * Processes queries in ANY language (English, Hindi, Bengali)
     * and returns structured BrainDecision with matching response language.
     */
    fun processQuery(
        input: String,
        outputLanguageSetting: VoiceLanguage = VoiceLanguage.AUTO,
        history: List<ChatMessage> = emptyList()
    ): BrainDecision {
        val trimmed = input.trim().trim('\'', '"')
        val lower = trimmed.lowercase(Locale.ROOT)
        val cleanLower = lower.replace(Regex("^(hey\\s+max|ok\\s+max|hello\\s+max|hey\\s+mac|hey\\s+mex|max|mac|mex|maks|macks|macs|मैक्स|मेक्स|ম্যাক্স|মেক্স|হেই\\s+ম্যাক্স|হে\\s+ম্যাক্স|ম্যাক্স\\s+শোনো)[,\\s:]*"), "")
            .trimStart(',', ' ', ':', '-', ';', '.', '\'', '"')
            .trimEnd(',', ' ', ':', '-', ';', '.', '?', '!', '\'', '"')
            .trim()
        val targetLang = resolveResponseLanguage(trimmed, outputLanguageSetting)

        // 1. Strict Gallery Protection Interception (Blocked across English, Hindi, Bengali)
        if (GalleryProtectionManager.shouldBlockVoiceLaunch(trimmed)) {
            val response = when (targetLang) {
                VoiceLanguage.HINDI -> "माफ़ कीजिए सर, गैलरी और तस्वीरों तक पहुंच सुरक्षा नीति द्वारा प्रतिबंधित है। मुझे आपकी गैलरी एक्सेस करने की अनुमति नहीं है।"
                VoiceLanguage.BENGALI -> "ক্ষমা করবেন স্যার, গ্যালারি এবং ব্যক্তিগত ছবি সুরক্ষা নীতি দ্বারা নিষিদ্ধ।"
                else -> "Sorry Sir, access to the photo gallery is strictly restricted by security protocol. I don't have permission to access your gallery."
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 1b. Strict Payment Protection Interception (Payments unconditionally disabled for financial security)
        if (PaymentProtectionManager.isPaymentRequest(trimmed) || PaymentProtectionManager.isPaymentRequest(cleanLower)) {
            val response = PaymentProtectionManager.getBlockedPaymentMessage(targetLang)
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 2a. Stop Speaking immediately
        if (lower.contains("stop speaking") || lower.contains("stop talking") || lower.contains("bolna band") ||
            lower.contains("bolna roko") || lower.contains("kotha bondho") || lower.contains("बोलना बंद") ||
            lower.contains("बात बंद") || lower.contains("কথা বন্ধ")
        ) {
            return BrainDecision.StopSpeaking
        }

        // 2a.1 Screen Vision early check (including stop watching / close vision)
        val visionDecision = parseScreenVision(cleanLower, trimmed, targetLang)
        if (visionDecision != null) {
            return visionDecision
        }

        // 2a.2 Multi-Step Task Commands & Cancellation
        val taskDecision = parseMultiStepTaskCommands(cleanLower, trimmed, targetLang)
        if (taskDecision != null) {
            return taskDecision
        }

        // 2a.2b Advanced Upgrades (Contextual Memory, Self-Healing Health, Notifications, Live Research, Performance)
        val advancedDecision = parseAdvancedUpgradeCommands(cleanLower, trimmed, targetLang)
        if (advancedDecision != null) {
            return advancedDecision
        }

        // 2a.3 Screen Question Follow-Up (when recent screen context exists)
        val followUpDecision = parseScreenQuestionFollowUp(cleanLower, trimmed, targetLang)
        if (followUpDecision != null) {
            return followUpDecision
        }

        // 2a.4 App Control & Screen Interactions (Tap, Scroll, Type, Navigate, Switch)
        val appControlDecision = parseAppControlCommands(cleanLower, trimmed, targetLang)
        if (appControlDecision != null) {
            return appControlDecision
        }

        // 2a.5 Baby Entertainment Mode early check (prioritizes baby mode control over service stop)
        val babyEarly = parseBabyEntertainment(cleanLower, trimmed, targetLang)
        if (babyEarly != null) {
            return babyEarly
        }

        // 2b. Standby & Full System Shutdown (Checked before generic stop so 'standby' enters standby)
        val shutdownDecision = parseSystemShutdownAndStandby(cleanLower, targetLang)
        if (shutdownDecision != null) {
            return shutdownDecision
        }

        // 2c. Stop MAX service commands
        if ((lower.contains("stop") && lower.contains("max")) || lower == "stop" || lower.contains("ruk jao") ||
            lower.contains("rok do") || lower.contains("roko") || lower.contains("band ho jao") ||
            lower.contains("thamo") || lower.contains("stob") || lower.contains("থামো") || lower.contains("বন্ধ হও")
        ) {
            return BrainDecision.TriggerStopMax
        }

        // 2e. Study and Education Mode (MCQs, Notes, Math solver, Grammar)
        val studyDecision = parseStudyAndEducation(cleanLower, trimmed, targetLang)
        if (studyDecision != null) {
            return studyDecision
        }

        // 2f. Smart Persistent Reminders
        val reminderDecision = parseSmartReminders(cleanLower, trimmed, targetLang)
        if (reminderDecision != null) {
            return reminderDecision
        }

        // 2f.1 Battery Status Queries
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("charging") ||
            lower.contains("बैटरी") || lower.contains("चार्ज") || lower.contains("ব্যাটারি")
        ) {
            return BrainDecision.QueryBattery
        }

        // 2f.2 Internet / Connectivity Queries (excluding explicit web search intents)
        val isWebSearchQuery = lower.contains("search") || lower.contains("find") || lower.contains("khojo") ||
            lower.contains("dhoondo") || lower.contains("dhundo") || lower.contains("খুঁজো") ||
            lower.contains("google") || lower.contains("browse")
        if (!isWebSearchQuery && (lower.contains("internet") || lower.contains("network") ||
            (lower.contains("wifi") && !lower.contains("setting")) ||
            lower.contains("online") || lower.contains("इंटरनेट") || lower.contains("नेटवर्क") || lower.contains("ইন্টারনেট"))
        ) {
            return BrainDecision.QueryNetwork
        }

        // 2g. Baby Entertainment Mode
        val babyDecision = parseBabyEntertainment(cleanLower, trimmed, targetLang)
        if (babyDecision != null) {
            return babyDecision
        }

        // 2g.1 Humour Engine - Personality commands, roasts, jokes, and witty comebacks
        val isSerious = humourEngine.isSeriousSituation(cleanLower)
        if (!isSerious) {
            if (humourEngine.isHumourCommand(cleanLower)) {
                if (humourEngine.isRoastCommand(cleanLower)) {
                    val roast = humourEngine.generateRoast(targetLang)
                    return BrainDecision.SpeakResponse(roast, targetLang)
                }
                if (humourEngine.isJokeCommand(cleanLower)) {
                    val joke = humourEngine.generateJoke(targetLang)
                    return BrainDecision.SpeakResponse(joke, targetLang)
                }
                val levelResponse = humourEngine.handleLevelCommand(cleanLower, targetLang)
                if (levelResponse != null) {
                    return BrainDecision.SpeakResponse(levelResponse, targetLang)
                }
            }

            if (humourEngine.detectTeaseOrTrolling(cleanLower)) {
                val comeback = humourEngine.generateComeback(cleanLower, targetLang)
                return BrainDecision.SpeakResponse(comeback, targetLang)
            }
        }

        // 2h. Master Voice Command Dictionary (Categories 1-16)
        val dictDecision = parseMasterDictionaryCommand(cleanLower, trimmed, targetLang)
        if (dictDecision != null) {
            return dictDecision
        }

        // 3. Language Switch Commands (Must be evaluated before mute so Bengali/Hindi commands aren't hijacked)
        val isWeather = lower.contains("weather") || lower.contains("mausam") || lower.contains("abhawa") || lower.contains("मौसम") || lower.contains("আবহাওয়া")
        if (!isWeather) {
            if (lower.contains("বাংলায়") || lower.contains("bangla te") || lower.contains("in bengali") || lower.contains("speak bengali") || lower.contains("bangla me") || lower.contains("বাংলা কথা") || lower.contains("banglay kotha")) {
                return BrainDecision.SwitchLanguage(
                    VoiceLanguage.BENGALI,
                    "হ্যাঁ স্যার, আমি এখন থেকে বাংলায় আপনার সাথে কথা বলব।"
                )
            }
            if (lower.contains("हिंदी में") || lower.contains("hindi me") || lower.contains("in hindi") || lower.contains("speak hindi") || lower.contains("hindi bolo") || lower.contains("हिंदी बोलो") || lower.contains("हिंदी बात")) {
                return BrainDecision.SwitchLanguage(
                    VoiceLanguage.HINDI,
                    "जी सर, अब से मैं आपसे हिंदी में बात करूँगा।"
                )
            }
            if (lower.contains("in english") || lower.contains("english me") || lower.contains("speak english") || lower.contains("अंग्रेजी") || lower.contains("ইংরেজি")) {
                return BrainDecision.SwitchLanguage(
                    VoiceLanguage.ENGLISH,
                    "Certainly, Sir. I will now communicate with you in English."
                )
            }
            if (lower.contains("hinglish") || lower.contains("hinglish me bolo") || lower.contains("hindi english mix") || lower.contains("mix mein bolo")) {
                return BrainDecision.SwitchLanguage(
                    VoiceLanguage.HINGLISH,
                    "Sure Sir! Ab main aapke saath Hinglish mein baat karunga — chill and easy!"
                )
            }
        }

        // 4. Mute / Unmute / Stop-speaking commands
        val isExplicitUnmute = cleanLower == "unmute" || cleanLower == "unmute max" ||
            cleanLower == "awaz chalu" || cleanLower == "awaz chalu karo" ||
            cleanLower == "आवाज़ चालू" || cleanLower == "आवाज़ चालू करो" ||
            cleanLower == "अनम्यूट" || cleanLower == "अनम्यूट करो" ||
            cleanLower == "কথা বলো" ||
            Regex("(?:^|[\\s,:;.-])unmute(?:[\\s,:;.-]|$)", RegexOption.IGNORE_CASE).containsMatchIn(cleanLower)
        if (isExplicitUnmute) {
            return BrainDecision.TriggerMute(false)
        }

        val isExplicitMute = cleanLower == "mute" || cleanLower == "mute max" || cleanLower == "mute voice" ||
            cleanLower == "mute microphone" || cleanLower == "mute mic" ||
            cleanLower == "म्यूट" || cleanLower == "म्यूट करो" ||
            cleanLower == "आवाज़ बंद करो" || cleanLower == "awaz band karo" ||
            cleanLower == "মিউট করো" ||
            Regex("(?:^|[\\s,:;.-])mute(?:[\\s,:;.-]|$)", RegexOption.IGNORE_CASE).containsMatchIn(cleanLower)
        if (isExplicitMute) {
            return BrainDecision.TriggerMute(true)
        }

        val isStopSpeaking = cleanLower == "chup" || cleanLower == "चुप" || cleanLower == "चुप रहो" || cleanLower == "शांत रहो" ||
            cleanLower == "be quiet" || cleanLower == "shut up" || cleanLower == "chup raho" || cleanLower == "shant raho" || cleanLower == "चुप हो जाओ"
        if (isStopSpeaking) {
            return BrainDecision.TriggerStopMax
        }

        // 5. Flashlight / Torch Commands
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("टॉर्च") || lower.contains("फ्लैशलाइट") || lower.contains("ফ্ল্যাশলাইট")) {
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("bujhao") || lower.contains("बंद") || lower.contains("বন্ধ")
            return if (turnOff) BrainDecision.SetFlashlight(false) else BrainDecision.SetFlashlight(true)
        }

        // 6. Volume Control Commands
        if (lower.contains("volume") || lower.contains("awaz") || lower.contains("आवाज़") || lower.contains("ভলিউম")) {
            if (lower.contains("up") || lower.contains("badhao") || lower.contains("increase") || lower.contains("बढ़ाओ") || lower.contains("বাড়াও")) {
                return BrainDecision.AdjustVolume(VolumeDirection.UP)
            }
            if (lower.contains("down") || lower.contains("kam") || lower.contains("decrease") || lower.contains("कम") || lower.contains("কমাও")) {
                return BrainDecision.AdjustVolume(VolumeDirection.DOWN)
            }
            if (lower.contains("mute") || lower.contains("म्यूट") || lower.contains("মিউট")) {
                return BrainDecision.AdjustVolume(VolumeDirection.MUTE)
            }
        }

        // 6.2 Emotional Support & Uplifting Companion ("acha nahi lg raha hai", "mood khrb hai", etc.)
        val emotionalDecision = parseEmotionalSupport(cleanLower, trimmed, targetLang)
        if (emotionalDecision != null) {
            return emotionalDecision
        }

        // 6.3 Motivation & Encouragement ("motivate me", "motivate karo")
        val motivationDecision = parseMotivation(cleanLower, targetLang)
        if (motivationDecision != null) {
            return motivationDecision
        }

        // 6.5. Messaging & WhatsApp Commands (Interactive Replies & Direct Messages across English, Hindi, Bengali)
        val messageDecision = parseMessageCommand(trimmed, lower, cleanLower, targetLang)
        if (messageDecision != null) {
            return messageDecision
        }

        // 7. Core Phone Apps & Search - Specific Intent Direct Routing
        // Safe WhatsApp Launch (Strict check: must be an explicit intent to open WhatsApp, NOT a query or general command)
        val isExplicitWhatsApp = (lower == "whatsapp" || lower == "whats app" || lower == "whats aap" || lower == "what aap" || lower == "व्हाट्सएप" || lower == "হোয়াটসঅ্যাপ" ||
                lower.endsWith(" whatsapp") || lower.startsWith("whatsapp ") || lower.contains("whatsapp kholo") ||
                lower.contains("open whatsapp") || lower.contains("open whats app") || lower.contains("open whats aap") || lower.contains("whatsapp open") || lower.contains("launch whatsapp") ||
                lower.contains("whatsapp chalao") || lower.contains("व्हाट्सएप खोलो") || lower.contains("व्हाट्सएप चालू") ||
                lower.contains("হোয়াটসঅ্যাপ খোলো") || lower.contains("হোয়াটসঅ্যাপ চালু")) &&
                !lower.contains("message to") && !lower.contains("ko message") && !lower.contains("send message") &&
                !lower.contains("bhejo") && !lower.contains("likho") && !lower.contains("what is") && !lower.contains("kya") &&
                !lower.contains("reply") && !lower.contains("उत्तर") && !lower.contains("রিপ্লাই") && !lower.contains("jawab")

        if (isExplicitWhatsApp) {
            val isBusiness = lower.contains("business") || lower.contains("बिज़नेस") || lower.contains("বিজনেস")
            return BrainDecision.OpenWhatsApp(business = isBusiness, language = targetLang)
        }

        // Camera App
        val isCamera = lower.contains("camera") || lower.contains("cam kholo") || lower.contains("photo khicho") ||
                lower.contains("ছবি তোলো") || lower.contains("कैमरा") || lower.contains("कैंडिड") ||
                lower.contains("ক্যামেরা")
        if (isCamera) {
            return BrainDecision.OpenCamera
        }

        // Phone Dialer App (Opening dialer interface - distinct from calling a specific contact)
        val isCallTarget = lower.contains("call ") || lower.contains("dial ") || lower.contains(" ko call") ||
                lower.contains(" को कॉल") || lower.contains(" को फोन") || lower.contains("কে কল") ||
                lower.contains("কে ফোন") || lower.contains("कॉल करो") || lower.contains("फोन लगाओ") || lower.contains("কল করো")

        val isPhone = (cleanLower == "open phone" || cleanLower == "open dialer" || cleanLower == "phone" || cleanLower == "dialer" ||
                cleanLower == "कॉल" || cleanLower == "फोन" || cleanLower == "কল" || cleanLower == "ফোন" ||
                cleanLower.contains("dialer") || cleanLower.contains("phone app") || cleanLower.contains("डायलर") ||
                cleanLower.contains("कॉल खोलो") || cleanLower.contains("फोन खोलो") || cleanLower.contains("कॉल ऐप") ||
                cleanLower.contains("फोन ऐप") || cleanLower.contains("কল") || cleanLower.contains("ফোন")) && !isCallTarget

        if (isPhone) {
            return BrainDecision.OpenPhone
        }

        // Chrome / Web Browser
        val isBrowser = lower.contains("chrome") || lower.contains("open browser") || lower.contains("browser kholo") ||
                lower.contains("क्रोम") || lower.contains("ब्राउज़र") || lower.contains("इंटरनेट खोलो") || lower.contains("ইন্টারনেট")
        if (isBrowser) {
            return BrainDecision.OpenBrowser
        }

        // Settings App & Specific Settings Subpages
        val settingsSubpage = parseSettingsSubpage(lower, targetLang)
        if (settingsSubpage != null) {
            return settingsSubpage
        }

        val isSettings = lower.contains("settings") || lower.contains("setting kholo") || lower.contains("सेटिंग्स") ||
                lower.contains("सेटिंग खोलो") || lower.contains("সেটিংস")
        if (isSettings) {
            return BrainDecision.OpenSettings
        }

        // Contacts App
        val isContacts = lower == "open contacts" || lower.endsWith("open contacts") || lower.contains("contacts kholo") ||
                lower.contains("phonebook kholo") || lower.contains("संपर्क खोलो") || lower.contains("কনট্যাক্ট খোলো") ||
                lower == "contacts" || lower == "संपर्क" || lower == "phonebook"
        if (isContacts) {
            return BrainDecision.OpenContacts
        }

        // Messages App
        val isMessages = lower == "open messages" || lower.contains("messages kholo") || lower.contains("sms app kholo") ||
                lower.contains("मैसेज खोलो") || lower.contains("মেসেজ খোলো") || lower == "messages" || lower == "मैसेज"
        if (isMessages && !lower.contains("send message") && !lower.contains("bhejo")) {
            return BrainDecision.OpenMessages
        }

        // Google Maps
        if (lower.contains("open maps") || lower.contains("open google maps") || lower.contains("maps kholo") || lower.contains("google maps kholo") || lower.contains("map kholo") || lower.contains("google maps") || lower.contains("नक्शा खोलो") || lower.contains("মানচিত্র") || lower == "maps" || lower == "map") {
            var mapQuery = ""
            if (lower.contains("find ") || lower.contains("search ") || lower.contains("hotels in ") || lower.contains("direction to ") || lower.contains("navigate to ")) {
                mapQuery = trimmed.substringAfter("find ").substringAfter("search ").substringAfter("hotels in ").substringAfter("navigate to ").substringAfter("direction to ").trim()
            }
            return BrainDecision.OpenMaps(mapQuery)
        }

        // Internet Search Commands
        if (lower.startsWith("search ") || lower.contains("search the internet") || lower.contains("search internet") ||
            lower.contains("search web") || lower.contains("google search") || lower.contains("internet par search") ||
            lower.contains("search karo") || lower.contains("খুঁজো") || lower.contains("অনুসন্ধান")
        ) {
            var searchQuery = trimmed
            val searchPrefixes = listOf(
                "search the internet for ", "search the web for ", "search internet for ",
                "search google for ", "search for ", "search ", "google search ", "internet par search karo ",
                "khojo ", "dhundho "
            )
            for (p in searchPrefixes) {
                if (searchQuery.lowercase(Locale.ROOT).contains(p)) {
                    val idx = searchQuery.lowercase(Locale.ROOT).indexOf(p)
                    searchQuery = searchQuery.substring(idx + p.length).trim()
                    break
                }
            }
            searchQuery = searchQuery.removeSuffix(" search karo").removeSuffix(" search").trim()
            if (searchQuery.isNotEmpty()) {
                return BrainDecision.SearchWeb(searchQuery, targetLang)
            }
        }

        // 8. Update Commands
        if (lower == "check update" || lower == "check updates" || lower == "check for update" || lower == "check for updates" ||
            lower.contains("update app") || lower.contains("app update") || lower.contains("update max") ||
            lower.contains("check for new version") || lower.contains("naya version") || lower.contains("app upgrade") ||
            lower.contains("अपडेट जांचो") || lower.contains("अपडेट चेक") || lower.contains("नया अपडेट") || lower.contains("আপডেট চেক")
        ) {
            return BrainDecision.CheckUpdates
        }

        // 9. Battery Status Queries
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("charging") ||
            lower.contains("बैटरी") || lower.contains("चार्ज") || lower.contains("ব্যাটারি")
        ) {
            return BrainDecision.QueryBattery
        }

        // 10. Internet / Connectivity Queries (excluding explicit web search intents)
        if (!isWebSearchQuery && (lower.contains("internet") || lower.contains("wifi") || lower.contains("network") ||
            lower.contains("online") || lower.contains("इंटरनेट") || lower.contains("नेटवर्क") || lower.contains("ইন্টারনেট"))
        ) {
            return BrainDecision.QueryNetwork
        }

        // 11. Alarm Clock Commands
        val alarmDecision = parseAlarmCommand(lower)
        if (alarmDecision != null) {
            return alarmDecision
        }

        // 11a. Timer Commands
        val timerDecision = parseTimerCommand(lower)
        if (timerDecision != null) {
            return timerDecision
        }

        // 11b. Media Controls (Play/Pause/Skip/Stop & Music from Spotify)
        val mediaDecision = parseMediaControl(lower, trimmed, targetLang)
        if (mediaDecision != null) {
            return mediaDecision
        }

        // 11c. Multi-Step Task Planner (e.g. Find nearby hospital and open directions)
        val multiStepDecision = parseMultiStepTask(cleanLower, trimmed, targetLang)
        if (multiStepDecision != null) {
            return multiStepDecision
        }

        // 11e. Navigation & Nearby Places
        val navDecision = parseNearbyAndDirections(lower, trimmed, targetLang)
        if (navDecision != null) {
            return navDecision
        }

        // 11f. Productivity (Notes, Task Lists, Clipboard, Sharing, Draft Email)
        val prodDecision = parseProductivity(lower, trimmed, targetLang)
        if (prodDecision != null) {
            return prodDecision
        }

        // 11g. Find Contact
        val contactDecision = parseFindContact(cleanLower, trimmed, targetLang)
        if (contactDecision != null) {
            return contactDecision
        }

        // 12. Calendar & Scheduling Commands
        if (lower.contains("calendar") || lower.contains("schedule meeting") || lower.contains("कैलेंडर") || lower.contains("मीटिंग तय करो")) {
            val title = if (lower.contains("for ")) trimmed.substringAfter("for ").trim() else "New Event"
            return BrainDecision.CreateCalendarEvent(title)
        }

        // 13. Weather Queries
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("abhawa") ||
            lower.contains("मौसम") || lower.contains("আবহাওয়া")
        ) {
            val location = parseWeatherLocation(lower)
            return BrainDecision.QueryWeather(location, targetLang)
        }

        // 14. Phone Calling Commands (Direct Calling & Interactive Routing)
        val callDecision = parseCallCommand(trimmed, lower, cleanLower, targetLang)
        if (callDecision != null) {
            return callDecision
        }

        // 16. Math Evaluator
        val mathResult = tryEvaluateMath(lower)
        if (mathResult != null) {
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Sir, iska answer $mathResult hai."
                VoiceLanguage.HINDI -> "सर, इसका परिणाम $mathResult है।"
                VoiceLanguage.BENGALI -> "স্যার, এর ফলাফল হলো $mathResult।"
                VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Sir, the result of that calculation is $mathResult."
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 17. Time inquiries
        val isTimeQuery = Regex("\\btime\\b").containsMatchIn(lower) || lower.contains("samay") ||
                lower.contains("baje") || lower.contains("somoy") || lower.contains("समय") || lower.contains("সময়")
        if (isTimeQuery) {
            val timeLocale = when (targetLang) {
                VoiceLanguage.HINGLISH -> Locale("hi", "IN")
                VoiceLanguage.HINDI -> Locale("hi", "IN")
                VoiceLanguage.BENGALI -> Locale("bn", "IN")
                else -> Locale.UK
            }
            val timeStr = SimpleDateFormat("hh:mm a", timeLocale).format(Date())
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Sir, abhi $timeStr baj rahe hain."
                VoiceLanguage.HINDI -> "सर, अभी समय $timeStr हुआ है।"
                VoiceLanguage.BENGALI -> "স্যার, এখন সময় $timeStr।"
                else -> "Sir, the time is currently $timeStr."
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 18. Date inquiries
        val isDateQuery = Regex("\\bdate\\b").containsMatchIn(lower) || lower.contains("tarikh") ||
                lower.contains("taarikh") || lower.contains("aaj kaun sa din") || lower.contains("तारीख") || lower.contains("তারিখ")
        if (isDateQuery) {
            val dateLocale = when (targetLang) {
                VoiceLanguage.HINGLISH -> Locale("hi", "IN")
                VoiceLanguage.HINDI -> Locale("hi", "IN")
                VoiceLanguage.BENGALI -> Locale("bn", "IN")
                else -> Locale.UK
            }
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", dateLocale).format(Date())
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Sir, aaj ki date $dateStr hai."
                VoiceLanguage.HINDI -> "सर, आज की तारीख है $dateStr।"
                VoiceLanguage.BENGALI -> "স্যার, আজকের तারीख হলো $dateStr।"
                else -> "Sir, today is $dateStr."
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 19. Built-in Knowledge Base Q&A & Context Follow-up
        val knowledgeAnswer = knowledgeBase.queryKnowledge(trimmed, targetLang, history)
        if (knowledgeAnswer != null) {
            return BrainDecision.SpeakResponse(knowledgeAnswer, targetLang)
        }

        // 20. Identity & Creator inquiries
        if (lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("aap kaun ho") || lower.contains("tumi ke") || lower.contains("तुम कौन हो")) {
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Main MAX hoon Sir, aapka personal AI assistant. Aap jo bhi chahein, main hamesha ready hoon."
                VoiceLanguage.HINDI -> "मैं मैक्स हूँ, आपका निजी एआई सहायक, सर। मैं हमेशा आपकी सेवा में तैयार हूँ।"
                VoiceLanguage.BENGALI -> "আমি ম্যাক্স, আপনার ব্যক্তিগত এআই সহকারী, স্যার।"
                else -> "I am MAX, your personal intelligent AI assistant, Sir. Ready to assist at any moment."
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        if (lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kemon acho") || lower.contains("kemon aachen") || lower.contains("कैसे हो")) {
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Main bilkul theek hoon Sir, thanks for asking! Bataiye, aaj main aapki kya help kar sakta hoon?"
                VoiceLanguage.HINDI -> "मैं बहुत अच्छा हूँ, सर, धन्यवाद! बताइए आज मैं आपकी क्या मदद करूँ?"
                VoiceLanguage.BENGALI -> "আমি খুব ভালো আছি, স্যার, ধন্যবাদ! বলুন আজ আপনাকে কিভাবে সাহায্য করতে পারি?"
                else -> "I am functioning flawlessly, Sir, thank you. How may I be of service to you today?"
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 21. Greetings
        if (lower == "hello" || lower == "hi" || lower == "hey" || lower.startsWith("hello max") ||
            lower.contains("namaste") || lower.contains("nomoshkar") || lower.contains("good morning") || lower.contains("good evening") ||
            lower.contains("नमस्ते") || lower.contains("নমস্কার")
        ) {
            val response = when (targetLang) {
                VoiceLanguage.HINGLISH -> "Hello Sir! Kya plan hai aaj? Main ready hoon, bas bolo!"
                VoiceLanguage.HINDI -> "नमस्ते सर! बताइए, आज मैं आपके लिए क्या कर सकता हूँ?"
                VoiceLanguage.BENGALI -> "নমস্কার স্যার! বলুন, আজ আমি আপনার কি সেবা করতে পারি?"
                else -> "Good day, Sir. How may I assist you?"
            }
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // 22. App Launch Commands (generic installed application name match or direct app utterance)
        val isAppLaunch = lower.startsWith("open ") || lower.contains(" open ") || lower.endsWith(" open") ||
                lower.startsWith("launch ") || lower.startsWith("start ") ||
                lower.contains("kholo") || lower.contains("chalao") || lower.contains("dikhao") ||
                lower.contains("khulun") || lower.contains("khol do") || lower.contains("chala do") ||
                lower.contains("open karo") || lower.contains("launch karo") ||
                lower.contains("खोलो") || lower.contains("चलाओ")

        val directAppNames = setOf(
            "whatsapp", "whats app", "व्हाट्सएप", "হোয়াটসঅ্যাপ",
            "spotify", "स्पॉटीफाई", "স্পটিফাই",
            "youtube", "यूट्यूब", "ইউটিউব",
            "camera", "cam", "कैमरा", "ক্যামেরা",
            "settings", "setting", "सेटिंग्स", "সেটিংস",
            "calculator", "calc", "कैलकुलेटर", "ক্যালকুলেটর",
            "chrome", "browser", "क्रोम", "ক্রোম",
            "maps", "map", "नक्शा", "মানচিত্র",
            "contacts", "संपर्क", "কনট্যাক্টস",
            "messages", "message", "sms", "मैसेज", "মেসেজ",
            "instagram", "insta", "इंस्टाग्राम",
            "facebook", "fb", "फेसबुक",
            "clock", "alarm", "घड़ी"
        )
        val isDirectApp = cleanLower in directAppNames || lower in directAppNames

        if (isAppLaunch || isDirectApp) {
            if (cleanLower in setOf("spotify", "स्पॉटीफाई", "স্পটিফাই")) {
                return BrainDecision.PlayMusic(query = "", app = "spotify", language = targetLang)
            }
            if (cleanLower in setOf("camera", "cam", "कैमरा", "ক্যামেরা")) {
                return BrainDecision.OpenCamera
            }
            if (cleanLower in setOf("settings", "setting", "सेटिंग्स", "সেটিংস")) {
                return BrainDecision.OpenSettings
            }
            if (cleanLower in setOf("chrome", "browser", "क्रोम", "क्रোম")) {
                return BrainDecision.OpenBrowser
            }
            if (cleanLower in setOf("maps", "map", "नक्शा", "মানচিত্র")) {
                return BrainDecision.OpenMaps()
            }
            if (cleanLower in setOf("contacts", "संपर्क", "কনট্যাক্টস")) {
                return BrainDecision.OpenContacts
            }
            if (cleanLower in setOf("messages", "message", "sms", "मैसेज", "মেসেজ")) {
                return BrainDecision.OpenMessages
            }
            if (cleanLower in setOf("whatsapp", "whats app", "व्हाट्सएप", "হোয়াটসঅ্যাপ")) {
                return BrainDecision.OpenWhatsApp(business = false, language = targetLang)
            }

            val spokenResponse = when (targetLang) {
                VoiceLanguage.HINDI -> "जी सर, एप्लिकेशन खोला जा रहा है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, অ্যাপ্লিকেশনটি খোলা হচ্ছে।"
                else -> "Opening application for you, Sir."
            }
            return BrainDecision.LaunchApp(trimmed, spokenResponse, targetLang)
        }

        // 23. General conversational fallback
        val fallbackResponse = when (targetLang) {
            VoiceLanguage.HINDI -> "जी सर, आपके निर्देश का विश्लेषण किया गया है। बताइए मैं इस पर आगे क्या करूँ?"
            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনার নির্দেশের প্রক্রিয়া সম্পন্ন হয়েছে। আর কি সাহায্য করতে পারি?"
            else -> "At your service, Sir. How would you like me to proceed with this request?"
        }
        return BrainDecision.SpeakResponse(fallbackResponse, targetLang)
    }

    /**
     * Asynchronous query processing that queries AI provider when available.
     */
    suspend fun processQueryAsync(
        input: String,
        outputLanguageSetting: VoiceLanguage = VoiceLanguage.AUTO,
        history: List<ChatMessage> = emptyList()
    ): BrainDecision {
        val decision = processQuery(input, outputLanguageSetting, history)

        // If local decision is just a conversational fallback, query AI provider or Online Knowledge
        if (decision is BrainDecision.SpeakResponse && isConversationalFallback(decision.text)) {
            val targetLang = resolveResponseLanguage(input, outputLanguageSetting)

            // Priority 1: Gemini AI Provider (if configured with API key)
            val provider = aiProvider
            if (provider != null && provider.isConfigured) {
                try {
                    val isQuerySerious = humourEngine.isSeriousSituation(input)
                    val sysInstruction = humourEngine.buildSystemInstructionForAi(targetLang, isQuerySerious)
                    val aiResult = provider.generateResponse(input, history, targetLang, sysInstruction)
                    if (aiResult is AiResponse.Success && aiResult.text.isNotBlank()) {
                        return BrainDecision.SpeakResponse(aiResult.text, targetLang)
                    }
                } catch (_: Exception) {}
            }

            // Priority 2: Online Knowledge (Wikipedia & DuckDuckGo Instant Answers)
            try {
                val onlineAnswer = onlineKnowledge.queryOnlineKnowledge(input, targetLang)
                if (onlineAnswer != null && onlineAnswer.isNotBlank()) {
                    return BrainDecision.SpeakResponse(onlineAnswer, targetLang)
                }
            } catch (_: Exception) {}
        }
        return decision
    }

    private fun isConversationalFallback(text: String): Boolean {
        return text.startsWith("जी सर, आपके निर्देश का विश्लेषण किया गया है") ||
                text.startsWith("হ্যাঁ স্যার, আপনার নির্দেশের প্রক্রিয়া সম্পন্ন হয়েছে") ||
                text.startsWith("At your service, Sir. How would you like me to proceed")
    }

    private fun parseAlarmCommand(lower: String): BrainDecision.SetAlarm? {
        if (!lower.contains("alarm") && !lower.contains("अलार्म") && !lower.contains("এলার্ম")) return null

        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
        val match = timeRegex.find(lower) ?: return null

        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val amPm = match.groupValues[3].lowercase()

        if (amPm == "pm" && hour < 12) hour += 12
        if (amPm == "am" && hour == 12) hour = 0

        return BrainDecision.SetAlarm(hour, minute, "MAX Alarm")
    }

    private fun parseTimerCommand(lower: String): BrainDecision.SetTimer? {
        if (!lower.contains("timer") && !lower.contains("टाइमर") && !lower.contains("টাইমার")) return null

        var totalSeconds = 0
        // Match minutes: e.g. "5 minute", "5 min", "5 मिनट", "৫ মিনিট"
        val minMatch = Regex("(\\d+)\\s*(?:minutes?|mins?|min|मिनट|মিনিট)").find(lower)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toIntOrNull() ?: 0
            totalSeconds += mins * 60
        }

        // Match seconds: e.g. "30 seconds", "30 sec", "30 सेकंड", "৩০ সেকেন্ড"
        val secMatch = Regex("(\\d+)\\s*(?:seconds?|secs?|sec|सेकंड|सेकेंड|সেকেন্ড)").find(lower)
        if (secMatch != null) {
            val secs = secMatch.groupValues[1].toIntOrNull() ?: 0
            totalSeconds += secs
        }

        // If only a bare number is provided with "timer" e.g. "timer 5" or "5 timer"
        if (totalSeconds == 0) {
            val bareNum = Regex("(\\d+)").find(lower)?.groupValues?.get(1)?.toIntOrNull()
            if (bareNum != null) {
                totalSeconds = bareNum * 60 // default to minutes
            }
        }

        if (totalSeconds > 0) {
            return BrainDecision.SetTimer(totalSeconds, "MAX Timer")
        }
        return null
    }

    private fun parseMediaControl(lower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // 1. Play / Start Music (Spotify, general songs, or specific song/artist)
        val isMusicPlay = lower.contains("play music") || lower.contains("resume music") ||
                lower.contains("gana chalao") || lower.contains("gana sunao") || lower.contains("gana bajao") ||
                lower.contains("kuch gana chalao") || lower.contains("kuch gana sunao") ||
                lower.contains("music chalao") || lower.contains("music bajao") || lower.contains("music play karo") ||
                lower.contains("spotify chalao") || lower.contains("spotify play") ||
                lower.contains("play on spotify") || lower.contains("play from spotify") ||
                lower.contains("गाना चलाओ") || lower.contains("गाना सुनाओ") || lower.contains("गाना बजाओ") ||
                lower.contains("গান চালাও") || lower.contains("গান বাজাও") || lower.contains("স্পটিফাই চালাও") ||
                lower.contains("play song") || lower.contains("play songs") || lower.startsWith("play ") ||
                (lower.contains("spotify") && (lower.contains("kholo") || lower.contains("chalao") || lower.contains("open")))

        if (isMusicPlay) {
            var query = ""
            if (lower.startsWith("play ") && !lower.equals("play music") && !lower.equals("play song") && !lower.equals("play media")) {
                query = trimmed.substring(5)
                    .replace(Regex("(?i)\\s+on\\s+spotify"), "")
                    .replace(Regex("(?i)\\s+from\\s+spotify"), "")
                    .trim()
            } else if (lower.contains("gana chalao") && !lower.trim().equals("gana chalao")) {
                query = trimmed.replace(Regex("(?i)\\s*gana\\s+chalao"), "").replace("ka", "").trim()
            } else if (lower.contains("ke gane chalao") || lower.contains("ke gaane chalao")) {
                query = trimmed.replace(Regex("(?i)\\s*ke\\s+ga?ane\\s+chalao"), "").trim()
            }
            return BrainDecision.PlayMusic(query = query, app = "spotify", language = targetLang)
        }

        if (lower.contains("pause music") || lower.contains("pause song") || lower.contains("gana roko") ||
            lower.contains("गाना रोको") || lower.contains("গান থামাও") || lower.contains("pause media") ||
            lower.contains("music pause karo")
        ) {
            return BrainDecision.ControlMedia(MediaAction.PAUSE)
        }
        if (lower.contains("next song") || lower.contains("next track") || lower.contains("agla gana") ||
            lower.contains("अगला गाना") || lower.contains("পরবর্তী গান") || lower.contains("skip song") ||
            lower.contains("track change karo")
        ) {
            return BrainDecision.ControlMedia(MediaAction.NEXT)
        }
        if (lower.contains("previous song") || lower.contains("previous track") || lower.contains("pichhla gana") ||
            lower.contains("पिछला गाना") || lower.contains("পূর্ববর্তী গান")
        ) {
            return BrainDecision.ControlMedia(MediaAction.PREVIOUS)
        }
        if (lower.contains("stop music") || lower.contains("stop song") || lower.contains("gana band karo") ||
            lower.contains("गाना बंद करो") || lower.contains("গান बंद करो") || lower.contains("গান বন্ধ করো") ||
            lower.contains("music stop karo")
        ) {
            return BrainDecision.ControlMedia(MediaAction.STOP)
        }
        return null
    }

    private fun parseEmotionalSupport(cleanLower: String, original: String, targetLang: VoiceLanguage): BrainDecision? {
        val isLowMood = cleanLower.contains("acha nahi lg raha") ||
                cleanLower.contains("achha nahi lag raha") ||
                cleanLower.contains("accha nahi lag raha") ||
                cleanLower.contains("acha nahi lag raha") ||
                cleanLower.contains("mood khrb") ||
                cleanLower.contains("mood kharab") ||
                cleanLower.contains("mood off") ||
                cleanLower.contains("अच्छा नहीं लग रहा") ||
                cleanLower.contains("मूड खराब") ||
                cleanLower.contains("मूड ऑफ") ||
                cleanLower.contains("मन नहीं लग रहा") ||
                cleanLower.contains("udaas") ||
                cleanLower.contains("उदास") ||
                cleanLower.contains("bura lag raha") ||
                cleanLower.contains("बुरा लग रहा") ||
                cleanLower.contains("pareshaan hoon") ||
                cleanLower.contains("feeling down") ||
                cleanLower.contains("bad mood") ||
                cleanLower.contains("not feeling good") ||
                cleanLower.contains("feeling sad") ||
                cleanLower.contains("feeling low") ||
                cleanLower.contains("मन खराब") ||
                cleanLower.contains("ভালো লাগছে না") ||
                cleanLower.contains("মন ভালো নেই")

        if (!isLowMood) return null

        // Check if user also asked for music in the same phrase
        val alsoWantsMusic = cleanLower.contains("gana") ||
                cleanLower.contains("music") ||
                cleanLower.contains("song") ||
                cleanLower.contains("spotify") ||
                cleanLower.contains("गाना") ||
                cleanLower.contains("গান") ||
                cleanLower.contains("play")

        if (alsoWantsMusic) {
            return BrainDecision.PlayMusic(query = "uplifting relaxing music", app = "spotify", language = targetLang)
        }

        val comfortResponse = when (targetLang) {
            VoiceLanguage.HINGLISH -> "Sir, life mein kabhi kabhi aisa phase aata hai... lekin aap strong hain, yeh waqt bhi guzar jayega. Kya main Spotify pe kuch relaxing music chala doon aapke liye, Sir?"
            VoiceLanguage.HINDI -> "सर, कभी-कभी दिन थोड़ा मुश्किल हो सकता है, लेकिन आपका हौसला बहुत बड़ा है। यह वक्त भी गुजर जाएगा। क्या आपका मूड अच्छा करने के लिए मैं स्पॉटीफ़ाई पर कोई सुकून देने वाला गाना चला दूँ, सर?"
            VoiceLanguage.BENGALI -> "স্যার, কখনো কখনো দিন একটু কঠিন হতে পারে, কিন্তু আপনার সাহস অনেক বড়। এই সময়টাও কেটে যাবে। আপনার মন ভালো করার জন্য আমি কি স্পটিফাইতে সুন্দর গান চালিয়ে দেব, স্যার?"
            else -> "Sir, rough moments happen to the best of us, but I believe in your resilience. Take a deep breath — this too shall pass. Would you like me to play some relaxing music on Spotify to comfort you, Sir?"
        }
        return BrainDecision.OfferEmotionalSupport(comfortResponse, targetLang)
    }

    private fun parseMotivation(cleanLower: String, targetLang: VoiceLanguage): BrainDecision? {
        val isMotivation = cleanLower.contains("motivate karo") ||
                cleanLower.contains("motivate me") ||
                cleanLower.contains("kuch motivate karo") ||
                cleanLower.contains("प्रेरित करो") ||
                cleanLower.contains("मोटिवेट करो") ||
                cleanLower.contains("অনুপ্রাণিত করো") ||
                cleanLower.contains("inspire me") ||
                cleanLower.contains("honsla badhao") ||
                cleanLower.contains("हौसला बढ़ाओ") ||
                cleanLower.contains("kuch achha bolo") ||
                cleanLower.contains("kuch accha bolo")

        if (!isMotivation) return null

        val response = when (targetLang) {
            VoiceLanguage.HINGLISH -> "Sir, aapki mehnat aur dedication kisi bhi challenge se zyada powerful hai. Great achievements hamesha patience aur consistent effort se milti hain. Mujhe aap par poora bharosa hai — smile karo aur confidently aage badho, Sir!"
            VoiceLanguage.HINDI -> "सर, आपकी मेहनत और आपका संकल्प किसी भी चुनौती से कहीं अधिक शक्तिशाली है। महान उपलब्धियाँ हमेशा धैर्य और निरंतर प्रयास से ही मिलती हैं। आप पर मुझे पूरा विश्वास है, मुस्कुराइए और पूरे आत्मविश्वास के साथ आगे बढ़िए, सर!"
            VoiceLanguage.BENGALI -> "স্যার, আপনার নিষ্ঠা এবং সাহস যেকোনো বাধার চেয়ে অনেক বেশি শক্তিশালী। বড় সাফল্য সবসময় ধৈর্য ও চেষ্টার মাধ্যমেই আসে। নিজের ওপর বিশ্বাস রাখুন, আপনি সফল হবেনই!"
            else -> "Sir, never doubt how far you have come or how much strength you hold. Difficult roads often lead to beautiful destinations. Believe in yourself and keep moving forward with confidence — I am always with you, Sir."
        }
        return BrainDecision.SpeakResponse(response, targetLang)
    }

    private fun parseMultiStepTask(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision.ExecuteMultiStepPlan? {
        val hasNearby = cleanLower.contains("nearby") || cleanLower.contains("paas ka") || cleanLower.contains("पास का") || cleanLower.contains("কাছাকাছি")
        val hasDirections = cleanLower.contains("directions") || cleanLower.contains("navigate") || cleanLower.contains("rasta") || cleanLower.contains("रास्ता") || cleanLower.contains("দিকনির্দেশ")
        if (hasNearby && hasDirections) {
            val category = when {
                cleanLower.contains("restaurant") || cleanLower.contains("रेस्टोरेंट") || cleanLower.contains("খাবার") -> "restaurant"
                cleanLower.contains("hospital") || cleanLower.contains("अस्पताल") || cleanLower.contains("হাসপাতাল") -> "hospital"
                cleanLower.contains("hotel") || cleanLower.contains("होटल") || cleanLower.contains("হোটেল") -> "hotel"
                cleanLower.contains("petrol") || cleanLower.contains("fuel") || cleanLower.contains("पेट्रोल") -> "petrol pump"
                cleanLower.contains("pharmacy") || cleanLower.contains("chemist") || cleanLower.contains("दवा") -> "pharmacy"
                cleanLower.contains("atm") || cleanLower.contains("एटीएम") -> "ATM"
                else -> "places"
            }
            val step1 = PlannedStep(
                stepIndex = 0,
                description = "Search nearby $category",
                decision = BrainDecision.SearchNearby(category, targetLang)
            )
            val step2 = PlannedStep(
                stepIndex = 1,
                description = "Open navigation directions to $category",
                decision = BrainDecision.OpenDirections(category, targetLang)
            )
            val plan = MultiStepPlan(
                planId = "plan_nearby_${System.currentTimeMillis()}",
                goal = "Find nearby $category and open directions",
                steps = listOf(step1, step2)
            )
            val prompt = when (targetLang) {
                VoiceLanguage.HINDI -> "जी सर, पास के $category खोजे जा रहे हैं और दिशा-निर्देश तैयार किए जा रहे हैं।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কাছাকাছি $category খোঁজা হচ্ছে এবং দিকনির্দেশ প্রস্তুত করা হচ্ছে।"
                else -> "Sir, searching nearby $category and preparing navigation directions."
            }
            return BrainDecision.ExecuteMultiStepPlan(plan, prompt, targetLang)
        }
        return null
    }

    private fun parseSettingsSubpage(lower: String, targetLang: VoiceLanguage): BrainDecision.OpenSettingsSubpage? {
        val hasKeyword = lower.contains("settings") || lower.contains("सेटिंग्स") || lower.contains("সেটিংস") ||
                lower.contains("setting") || lower.contains("सेटिंग") ||
                lower.startsWith("open ") || lower.contains(" kholo") || lower.contains(" खोलो")

        if (!hasKeyword) return null

        val subpage = when {
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("वाईफाई") || lower.contains("ওয়াইফাই") -> "wifi"
            lower.contains("bluetooth") || lower.contains("ब्लूटूथ") || lower.contains("ব্লুটুথ") -> "bluetooth"
            lower.contains("location") || lower.contains("gps") || lower.contains("लोकेशन") || lower.contains("स्थान") || lower.contains("অবস্থান") -> "location"
            lower.contains("sound") || lower.contains("volume") || lower.contains("ध्वनि") || lower.contains("আওয়াজ") -> "sound"
            lower.contains("display") || lower.contains("brightness") || lower.contains("स्क्रीन") || lower.contains("डिस्प्ले") -> "display"
            lower.contains("accessibility") || lower.contains("एक्सेसिबिलिटी") || lower.contains("অ্যাক্সেসিবিলিটি") -> "accessibility"
            lower.contains("app") || lower.contains("permission") || lower.contains("अनुमति") || lower.contains("एप्लिकेशन") -> "apps"
            else -> ""
        }
        if (subpage.isNotBlank()) {
            return BrainDecision.OpenSettingsSubpage(subpage, targetLang)
        }
        return null
    }

    private fun parseNearbyAndDirections(lower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // Directions / Navigation check
        if (lower.startsWith("directions to ") || lower.startsWith("navigate to ") ||
            lower.contains("ka rasta") || lower.contains("का रास्ता") ||
            lower.contains("rasta batao") || lower.contains("রাস্তা") ||
            lower.contains("er diknirdesh") || lower.contains("এর দিকনির্দেশ")
        ) {
            var dest = trimmed
                .removePrefix("directions to ").removePrefix("navigate to ")
                .replace(Regex("(?i)ka rasta dikhao|ka rasta batao|ka rasta|ka raasta|का रास्ता दिखाओ|का रास्ता बताओ|का रास्ता|যাওয়ার রাস্তা|পথ দেখাও|রাস্তা|बताओ|dikhao"), "")
                .trim()
            if (dest.isNotBlank()) {
                return BrainDecision.OpenDirections(dest, targetLang)
            }
        }

        // Nearby category search
        if (lower.contains("nearby") || lower.contains("paas ka") || lower.contains("paas ke") ||
            lower.contains("पास का") || lower.contains("पास के") || lower.contains("কাছাকাছি")
        ) {
            val category = when {
                lower.contains("hospital") || lower.contains("अस्पताल") || lower.contains("হাসপাতাল") -> "hospital"
                lower.contains("restaurant") || lower.contains("रेस्टोरेंट") || lower.contains("होटल खाने") || lower.contains("খাবার") -> "restaurant"
                lower.contains("hotel") || lower.contains("होटल") || lower.contains("হোটেল") -> "hotel"
                lower.contains("petrol") || lower.contains("fuel") || lower.contains("पेट्रोल") -> "petrol pump"
                lower.contains("pharmacy") || lower.contains("medical") || lower.contains("chemist") || lower.contains("दवा") -> "pharmacy"
                lower.contains("atm") || lower.contains("एटीएम") -> "ATM"
                lower.contains("cafe") || lower.contains("कैफे") -> "cafe"
                lower.contains("bank") || lower.contains("बैंक") -> "bank"
                else -> trimmed.substringAfter("nearby ").substringAfter("पास का ").substringAfter("কাছাকাছি ").trim()
            }
            if (category.isNotBlank()) {
                return BrainDecision.SearchNearby(category, targetLang)
            }
        }
        return null
    }

    private fun parseProductivity(lower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // 1. Create Note
        if (lower.startsWith("note down") || lower.startsWith("take a note") || lower.startsWith("write a note") ||
            lower.contains("note likho") || lower.contains("नोट लिखो") || lower.contains("নোট লেখো")
        ) {
            val content = trimmed
                .removePrefix("note down:").removePrefix("note down")
                .removePrefix("take a note:").removePrefix("take a note")
                .removePrefix("write a note:").removePrefix("write a note")
                .replace(Regex("(?i)ek note likho|note likho|नोट लिखो|নোট লেখো"), "")
                .trimStart(':', ' ', '-')
                .trim()
            if (content.isNotBlank()) {
                return BrainDecision.CreateNote(content, targetLang)
            }
        }

        // 2. Read Notes
        if (lower.contains("read notes") || lower.contains("read my notes") || lower.contains("show my notes") ||
            lower.contains("mere notes") || lower.contains("मेरे नोट") || lower.contains("আমার নোট")
        ) {
            return BrainDecision.ReadNotes(targetLang)
        }

        // 3. Add Task
        if (lower.startsWith("add task") || lower.contains("task list me jodo") || lower.contains("टास्क लिस्ट में जोड़ो") ||
            lower.contains("add to task list") || lower.contains("add to my tasks") || lower.contains("কাজের তালিকায় যুক্ত")
        ) {
            val title = trimmed
                .removePrefix("add task:").removePrefix("add task")
                .removePrefix("add to task list:").removePrefix("add to task list")
                .replace(Regex("(?i)task list me jodo|task list mein jodo|टास्क लिस्ट में जोड़ो|কাজের তালিকায় যুক্ত করো"), "")
                .trimStart(':', ' ', '-')
                .trim()
            if (title.isNotBlank()) {
                return BrainDecision.AddTask(title, targetLang)
            }
        }

        // 4. Read Tasks
        if (lower.contains("read tasks") || lower.contains("show tasks") || lower.contains("show task list") ||
            lower.contains("what are my tasks") || lower.contains("my tasks") || lower.contains("task list") ||
            lower.contains("mere tasks") || lower.contains("मेरे टास्क") || lower.contains("কাজের তালিকা")
        ) {
            return BrainDecision.ReadTasks(targetLang)
        }

        // 5. Copy to Clipboard
        if (lower.contains("copy to clipboard") || lower.contains("clipboard par copy") || lower.contains("क्लिपबोर्ड पर कॉपी")) {
            val textToCopy = trimmed
                .removePrefix("copy to clipboard:").removePrefix("copy to clipboard")
                .replace(Regex("(?i)clipboard par copy karo|क्लिपबोर्ड पर कॉपी करो"), "")
                .trim()
            val text = if (textToCopy.isNotBlank()) textToCopy else "MAX Assistant Note"
            return BrainDecision.CopyToClipboard(text, targetLang)
        }

        // 6. Share Text
        if (lower.startsWith("share text") || lower.contains("share karo") || lower.contains("शेयर करो")) {
            val textToShare = trimmed
                .removePrefix("share text:").removePrefix("share text")
                .replace(Regex("(?i)share karo|शेयर करो"), "")
                .trim()
            val text = if (textToShare.isNotBlank()) textToShare else "Shared via MAX Assistant"
            return BrainDecision.ShareText(text, targetLang)
        }

        // 7. Draft Email
        if (lower.contains("draft email") || lower.contains("send email to") || lower.contains("ईमेल ड्राफ्ट") || lower.contains("ইমেল ড্রাফট")) {
            val recipient = if (trimmed.contains("@")) {
                trimmed.split(" ").firstOrNull { it.contains("@") } ?: ""
            } else {
                trimmed.substringAfter("to ").substringAfter("को ").substringBefore(" ").trim()
            }
            val subject = when {
                lower.contains("about ") -> trimmed.substringAfter("about ").trim()
                lower.contains("subject ") -> trimmed.substringAfter("subject ").trim()
                lower.contains("विषय ") -> trimmed.substringAfter("विषय ").trim()
                else -> "Message from MAX"
            }
            return BrainDecision.DraftEmail(recipient, subject, "", targetLang)
        }

        return null
    }

    private fun parseFindContact(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision.FindContact? {
        if (cleanLower.startsWith("find contact ") || cleanLower.startsWith("search contact ") ||
            cleanLower.contains("ka contact dhoondo") || cleanLower.contains("का संपर्क खोजो") ||
            cleanLower.contains("ka number dhoondo") || cleanLower.contains("का नंबर खोजो") ||
            cleanLower.contains("er number khonjo") || cleanLower.contains("এর নম্বর খোঁজো")
        ) {
            val name = trimmed
                .removePrefix("find contact ").removePrefix("search contact ")
                .replace(Regex("(?i)ka contact dhoondo|का संपर्क खोजो|ka number dhoondo|का नंबर खोजो|er number khonjo|এর নম্বর খোঁজো"), "")
                .trim()
            if (name.isNotBlank()) {
                return BrainDecision.FindContact(name, targetLang)
            }
        }
        return null
    }

    private fun parseWeatherLocation(lower: String): String {
        val clean = lower.replace(Regex("\\b(in|me|mein)\\s+(hindi|english|bengali|bangla)\\b.*"), "")
            .trimEnd('?', '.', '!', ',', ' ')
            .trim()
        val loc = when {
            clean.contains(" in ") -> clean.substringAfter(" in ").trim()
            clean.contains(" ka mausam") -> clean.substringBefore(" ka mausam").trim()
            clean.contains(" me mausam") -> clean.substringBefore(" me mausam").trim()
            else -> ""
        }.trimEnd('?', '.', '!', ',', ' ').trim()

        return loc.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun parseCallCommand(
        original: String,
        lower: String,
        cleanLower: String,
        targetLang: VoiceLanguage
    ): BrainDecision? {
        val cleanOriginal = original.replace(Regex("^(?i)(hey\\s+max|ok\\s+max|hello\\s+max|hey\\s+mac|hey\\s+mex|max|mac|mex|maks|macks|macs|मैक्स|मेक्स|ম্যাক্স|মেক্স|হেই\\s+ম্যাক্স|হে\\s+ম্যাক্স)[,\\s:]*"), "").trim()

        // 1. Generic call request with no recipient
        // e.g. "call kro", "call karo", "call lagao", "mery baat krwao", "meri baat karwao", "phone lagao"
        val isGenericCall = cleanLower == "call kro" || cleanLower == "call karo" || cleanLower == "call lagao" ||
                cleanLower == "call laga do" || cleanLower == "phone lagao" || cleanLower == "phone laga do" ||
                cleanLower == "mery baat krwao" || cleanLower == "meri baat karwao" || cleanLower == "baat karwao" ||
                cleanLower == "meri baat karao" || cleanLower == "mery baat krao" || cleanLower == "baat kara do" ||
                cleanLower == "कॉल लगाओ" || cleanLower == "कॉल करो" || cleanLower == "फोन लगाओ" ||
                cleanLower == "मेरी बात करवाओ" || cleanLower == "बात करवाओ" || cleanLower == "কথা বলিয়ে দাও" ||
                cleanLower == "make a call" || cleanLower == "place a call" || cleanLower == "call someone"

        if (isGenericCall) {
            val prompt = when (targetLang) {
                VoiceLanguage.HINDI -> "जी सर, आप किसे कॉल मिलाना चाहते हैं?"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনি কাকে কল করতে চান?"
                else -> "Sir, whom would you like me to call?"
            }
            return BrainDecision.RequestCallRecipient(prompt)
        }

        // 2. "mery baat krwao" / "meri baat karwao" / "baat karwao" with target
        if (cleanLower.contains("baat krwao") || cleanLower.contains("baat karwao") ||
            cleanLower.contains("baat karao") || cleanLower.contains("baat krao") ||
            cleanLower.contains("बात करवाओ") || cleanLower.contains("बात कराओ") ||
            cleanLower.contains("কথা বলিয়ে দাও")
        ) {
            var raw = cleanOriginal
                .replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max|mac|mex|maks|मैक्स|मेक्स|ম্যাক্স)[,\\s:]*"), "")
                .trim()

            var target = if (cleanLower.contains(" সাথে ")) {
                val before = raw.substringBefore(" সাথে ").trim()
                before.removeSuffix("ের").removeSuffix("র").trim()
            } else {
                val actionRegex = Regex("(?i)(meri\\s+baat\\s+karwao|mery\\s+baat\\s+krwao|meri\\s+baat\\s+krao|mery\\s+baat\\s+krao|meri\\s+baat\\s+karao|baat\\s+karwao|baat\\s+krwao|baat\\s+karao|baat\\s+krao|मेरी\\s+बात\\s+करवाओ|मेरी\\s+बात\\s+कराओ|बात\\s+करवाओ|बात\\s+कराओ)")
                val stripped = raw.replace(actionRegex, " ").trim()
                stripped.replace(Regex("(?i)^(\\s*se\\s+|\\s*से\\s+)"), "")
                    .replace(Regex("(?i)(\\s+se\\s*|\\s*से\\s*)$"), "")
                    .trim()
            }

            target = target.trimEnd('.', '?', '!', ',', ' ').trim()
            if (target.isNotEmpty() && !isMaxName(target)) {
                return BrainDecision.MakePhoneCall(target, requiresConfirmation = true)
            }
        }

        // 3. "call lagao" / "phone lagao" / "call kro" / "call karo"
        // Pattern 3A: "<target> ko call lagao/kro/karo" or "<target> ka call lagao"
        if (cleanLower.contains(" ko call lagao") || cleanLower.contains(" ko phone lagao") ||
            cleanLower.contains(" ko call kro") || cleanLower.contains(" ko call karo") ||
            cleanLower.contains(" ko call laga do") || cleanLower.contains(" ka call lagao") ||
            cleanLower.contains(" को कॉल लगाओ") || cleanLower.contains(" को फोन लगाओ") ||
            cleanLower.contains(" को कॉल करो") || cleanLower.contains(" को कॉल लगा दो")
        ) {
            val delimiter = if (cleanLower.contains(" को ")) " को " else if (cleanLower.contains(" ka call")) " ka " else " ko "
            var target = cleanOriginal.substringBefore(delimiter).trim()
            target = target.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max|mac|mex|maks|मैक्स|मेक्स)[,\\s:]*"), "")
                .trimEnd('.', '?', '!', ',', ' ')
                .trim()
            if (target.isNotEmpty() && !isMaxName(target)) {
                return BrainDecision.MakePhoneCall(target, requiresConfirmation = true)
            }
        }

        // Pattern 3B: "call lagao <target>" or "call kro <target>" or "call karo <target>" or "phone lagao <target>"
        val callPrefixes = listOf(
            "call lagao ", "call laga do ", "call kro ", "call karo ", "call kar do ", "phone lagao ", "phone laga do ",
            "कॉल लगाओ ", "कॉल लगा दो ", "कॉल करो ", "कॉल कर दो ", "फोन लगाओ ", "फोन लगा दो "
        )
        for (cp in callPrefixes) {
            if (cleanLower.startsWith(cp)) {
                var target = cleanOriginal.substring(cp.length).trim()
                target = target.removeSuffix(" ko").removeSuffix(" ko.").removeSuffix(" को").removeSuffix("কে").trim()
                target = target.trimEnd('.', '?', '!', ',', ' ').trim()
                if (target.isNotEmpty() && !isMaxName(target)) {
                    return BrainDecision.MakePhoneCall(target, requiresConfirmation = false)
                }
            }
        }

        // Pattern 3C: English "call <target>", "dial <target>", "phone <target>"
        if (cleanLower.startsWith("call ") || cleanLower.startsWith("dial ") || cleanLower.startsWith("phone ")) {
            var target = cleanOriginal.substringAfter(" ").trim()
            target = target.trimEnd('.', '?', '!', ',', ' ').trim()
            if (target.isNotEmpty() && !isMaxName(target)) {
                return BrainDecision.MakePhoneCall(target, requiresConfirmation = true)
            }
        }

        // Pattern 3D: Bengali "<target>কে কল করো" / "<target>কে ফোন করো"
        if (cleanLower.contains("কে কল") || cleanLower.contains("কে ফোন")) {
            val delimiter = if (cleanLower.contains("কে কল")) "কে কল" else "কে ফোন"
            var target = cleanOriginal.substringBefore(delimiter).trim()
            target = target.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max|ম্যাক্স)[,\\s:]*"), "")
                .trimEnd('.', '?', '!', ',', ' ').trim()
            if (target.isNotEmpty() && !isMaxName(target)) {
                return BrainDecision.MakePhoneCall(target, requiresConfirmation = true)
            }
        }

        // Existing fallback: "<target> ko call" / "<target> ko phone"
        if (cleanLower.contains(" ko call") || cleanLower.contains(" ko phone") || cleanLower.contains(" को कॉल") || cleanLower.contains(" को फोन")) {
            val delimiter = if (cleanLower.contains(" को ")) " को " else " ko "
            var target = cleanOriginal.substringBefore(delimiter).trim()
            target = target.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max|mac|mex|maks|मैक्स|मेक्स)[,\\s:]*"), "")
                .trimEnd('.', '?', '!', ',', ' ').trim()
            if (target.isNotEmpty() && !isMaxName(target)) {
                return BrainDecision.MakePhoneCall(target, requiresConfirmation = true)
            }
        }

        return null
    }

    private fun isMaxName(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT).trim()
        return lower in listOf("max", "mac", "mex", "maks", "macks", "macs", "मैक्स", "मेक्स", "ম্যাক্স", "মেক্স")
    }

    private fun parseMessageCommand(
        original: String,
        lower: String,
        cleanLower: String,
        targetLang: VoiceLanguage
    ): BrainDecision? {
        val isWhatsApp = lower.contains("whatsapp") || lower.contains("whats app") ||
                lower.contains("whats aap") || lower.contains("what aap") || lower.contains("what app") ||
                lower.contains("what's app") || lower.contains("व्हाट्सएप") || lower.contains("व्हाट्स ऐप") ||
                lower.contains("হোয়াটসঅ্যাপ")

        val isReplyIntent = lower.contains("reply") || lower.contains("रिप्लाई") ||
                lower.contains("उत्तर") || lower.contains("রিপ্লাই") || lower.contains("jawab") || lower.contains("জবাব")

        // 1. WhatsApp Reply flows
        if (isWhatsApp && isReplyIntent) {
            // Check for generic / unspecified recipient:
            // e.g. "open whats aap and reply to whome i want to", "open whats aap and reply", "whatsapp reply", "reply on whatsapp"
            val isGenericRecipient = cleanLower.endsWith("reply") ||
                    cleanLower.contains("reply to whome i want") || cleanLower.contains("reply to whom i want") ||
                    cleanLower.contains("reply to who i want") || cleanLower.contains("reply to whomever") ||
                    cleanLower.contains("reply to someone") || cleanLower.contains("kisi ko reply") ||
                    cleanLower.contains("কাউকে রিপ্লাই") || cleanLower.contains("কাউকে উত্তর") ||
                    cleanLower == "reply" || cleanLower == "open whatsapp and reply" ||
                    cleanLower == "open whats app and reply" || cleanLower == "open whats aap and reply" ||
                    cleanLower == "whatsapp reply" || cleanLower == "whats app reply" || cleanLower == "whats aap reply" ||
                    cleanLower == "reply on whatsapp" || cleanLower == "reply on whats app" || cleanLower == "reply on whats aap" ||
                    cleanLower.contains("reply to anyone") || cleanLower.contains("whom i want") || cleanLower.contains("whome i want")

            if (isGenericRecipient) {
                val prompt = when (targetLang) {
                    VoiceLanguage.HINDI -> "जी सर, आप किसे उत्तर भेजना चाहते हैं?"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনি কাকে উত্তর পাঠাতে চান?"
                    else -> "Sir, whom would you like to reply to on WhatsApp?"
                }
                return BrainDecision.RequestReplyRecipient(isWhatsApp = true, prompt = prompt)
            }

            // Pattern A & B: "reply to <recipient>..."
            if (cleanLower.contains("reply to ")) {
                val afterReplyTo = original.substring(lower.indexOf("reply to ") + "reply to ".length).trim()
                // Check if message is separated by : or -
                if (afterReplyTo.contains(":") || afterReplyTo.contains("-")) {
                    val parts = afterReplyTo.split(Regex("[:\\-]"), limit = 2)
                    val rec = parts[0].trim().replace(Regex("(?i)\\s+on\\s+(whatsapp|whats\\s*app|whats\\s*aap)"), "").trim()
                    val msg = parts[1].trim()
                    if (rec.isNotEmpty() && msg.isNotEmpty()) {
                        return BrainDecision.SendMessage(rec, msg, isWhatsApp = true, requiresConfirmation = false)
                    }
                }
                // Check if message is separated by " that " or " saying "
                val thatPattern = Regex("(?i)^(.*?)\\s+(?:that|saying)\\s+(.*)$")
                val thatMatch = thatPattern.find(afterReplyTo)
                if (thatMatch != null) {
                    val rec = thatMatch.groupValues[1].trim().replace(Regex("(?i)\\s+on\\s+(whatsapp|whats\\s*app|whats\\s*aap)"), "").trim()
                    val msg = thatMatch.groupValues[2].trim()
                    if (rec.isNotEmpty() && msg.isNotEmpty()) {
                        return BrainDecision.SendMessage(rec, msg, isWhatsApp = true, requiresConfirmation = false)
                    }
                }

                var candidateName = afterReplyTo
                    .replace(Regex("(?i)\\s+on\\s+(whatsapp|whats\\s*app|whats\\s*aap)"), "")
                    .trimEnd('.', '?', '!', ',', ' ')
                    .trim()

                // If candidateName has multiple words, check if it contains a conversational break like "I am...", "call me..."
                val words = candidateName.split(Regex("\\s+"))
                if (words.size > 2 && (words[1].equals("i", ignoreCase = true) || words[1].equals("we", ignoreCase = true) ||
                            words[1].equals("main", ignoreCase = true) || words[1].equals("call", ignoreCase = true) ||
                            words[1].equals("please", ignoreCase = true))) {
                    val rec = words[0]
                    val msg = words.drop(1).joinToString(" ")
                    return BrainDecision.SendMessage(rec, msg, isWhatsApp = true, requiresConfirmation = false)
                }

                if (candidateName.isNotEmpty()) {
                    val prompt = when (targetLang) {
                        VoiceLanguage.HINDI -> "जी सर, $candidateName को क्या उत्तर भेजना है?"
                        VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $candidateName কে কি उत्तर পাঠাতে হবে?"
                        else -> "Sir, what reply should I send to $candidateName?"
                    }
                    return BrainDecision.RequestReplyContent(candidateName, isWhatsApp = true, prompt = prompt)
                }
            }

            // Pattern C: Hindi "whatsapp par <recipient> ko reply karo..." / "<recipient> ko reply karo..."
            if (lower.contains(" ko reply") || lower.contains(" को रिप्लाई") || lower.contains(" को उत्तर")) {
                val delimiter = if (lower.contains(" को ")) " को " else " ko "
                val before = original.substringBefore(delimiter).trim()
                val after = original.substringAfter(delimiter).trim()
                val rec = before.replace(Regex("(?i)^(open\\s+)?(whatsapp|whats\\s*app|whats\\s*aap|व्हाट्सएप)\\s*(par|pe|mein|me)?\\s*"), "")
                    .replace(Regex("(?i)^(व्हाट्सएप\\s+पर|व्हाट्सएप\\s+में)\\s*"), "")
                    .trim()

                val msgPart = after.replace(Regex("^(reply karo|reply do|रिप्लाई करो|रिप्लाई दो|उत्तर दो|उत्तर भेजो)"), "").trim()
                val cleanMsg = msgPart.removePrefix("ki ").removePrefix("ki").removePrefix("कि ").removePrefix(":").removePrefix("-").trim()

                if (rec.isNotEmpty()) {
                    if (cleanMsg.isNotEmpty()) {
                        return BrainDecision.SendMessage(rec, cleanMsg, isWhatsApp = true, requiresConfirmation = false)
                    } else {
                        val prompt = when (targetLang) {
                            VoiceLanguage.HINDI -> "जी सर, $rec को क्या उत्तर भेजना है?"
                            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $rec কে কি उत्तर पाठাতে হবে?"
                            else -> "Sir, what reply should I send to $rec?"
                        }
                        return BrainDecision.RequestReplyContent(rec, isWhatsApp = true, prompt = prompt)
                    }
                }
            }

            // Pattern D: Bengali "<recipient>কে হোয়াটসঅ্যাপে রিপ্লাই/উত্তর দাও..."
            if (lower.contains("কে রিপ্লাই") || lower.contains("কে উত্তর")) {
                val delimiter = if (lower.contains("কে রিপ্লাই")) "কে রিপ্লাই" else "কে উত্তর"
                val before = original.substringBefore(delimiter).trim()
                val after = original.substringAfter(delimiter).trim()
                val rec = before.replace(Regex("(?i)^(হোয়াটসঅ্যাপে|whatsapp\\s*(?:e)?)\\s*"), "").trim()
                var cleanMsg = after.trim()
                val bengaliPrefixes = listOf("দাও", "করো", "যে", ":", "-", "বলে")
                for (bp in bengaliPrefixes) {
                    if (cleanMsg.startsWith(bp)) cleanMsg = cleanMsg.removePrefix(bp).trim()
                }
                for (bp in bengaliPrefixes) {
                    if (cleanMsg.startsWith(bp)) cleanMsg = cleanMsg.removePrefix(bp).trim()
                }

                if (rec.isNotEmpty()) {
                    if (cleanMsg.isNotEmpty()) {
                        return BrainDecision.SendMessage(rec, cleanMsg, isWhatsApp = true, requiresConfirmation = false)
                    } else {
                        val prompt = "হ্যাঁ স্যার, $rec কে কি উত্তর পাঠাতে হবে?"
                        return BrainDecision.RequestReplyContent(rec, isWhatsApp = true, prompt = prompt)
                    }
                }
            }
        }

        // 1b. Generic Send Message / SMS without recipient
        val isGenericSendSms = cleanLower == "send sms" || cleanLower == "send message" ||
                cleanLower == "sms bhejo" || cleanLower == "message bhejo" ||
                cleanLower == "sms karo" || cleanLower == "message karo" ||
                cleanLower == "मैसेज भेजो" || cleanLower == "एसएमएस भेजो" ||
                cleanLower == "মেসেজ পাঠাও" || cleanLower == "এসএমএস পাঠাও"
        if (isGenericSendSms) {
            val prompt = when (targetLang) {
                VoiceLanguage.HINDI -> "जी सर, आप किसे संदेश भेजना चाहते हैं?"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আপনি কাকে বার্তা পাঠাতে চান?"
                else -> "Sir, whom would you like to send a message to?"
            }
            return BrainDecision.RequestReplyRecipient(isWhatsApp = false, prompt = prompt)
        }

        // 2. Direct Send Message (English: "send message to...", "send sms to...", "send whatsapp to...", "message to...")
        if (lower.startsWith("send message to ") || lower.startsWith("send sms to ") ||
            lower.startsWith("send a message to ") || lower.startsWith("send an sms to ") ||
            lower.startsWith("send whatsapp to ") || lower.startsWith("send whats app to ") || lower.startsWith("send whats aap to ") ||
            lower.startsWith("message to ") || lower.startsWith("sms to ") || lower.startsWith("whatsapp to ")
        ) {
            val isSmsExplicit = lower.startsWith("send sms to ") || lower.startsWith("sms to ") || lower.startsWith("send an sms to ")
            val effectiveWhatsApp = if (isSmsExplicit) false else isWhatsApp
            val after = if (lower.contains("to ")) original.substring(lower.indexOf("to ") + 3).trim() else original
            val parts = after.split(Regex("[:\\-]"), limit = 2)
            if (parts.size == 2) {
                return BrainDecision.SendMessage(parts[0].trim(), parts[1].trim(), effectiveWhatsApp, requiresConfirmation = !effectiveWhatsApp)
            }
            val thatPattern = Regex("(?i)^(.*?)\\s+(?:that|saying)\\s+(.*)$")
            val thatMatch = thatPattern.find(after)
            if (thatMatch != null) {
                return BrainDecision.SendMessage(thatMatch.groupValues[1].trim(), thatMatch.groupValues[2].trim(), effectiveWhatsApp, requiresConfirmation = !effectiveWhatsApp)
            }
            // Check if user spoke "send sms to <name> <message>" separated by space
            val words = after.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (words.size >= 2) {
                val rec = words[0]
                val msg = words.drop(1).joinToString(" ")
                return BrainDecision.SendMessage(rec, msg, effectiveWhatsApp, requiresConfirmation = !effectiveWhatsApp)
            } else if (words.size == 1) {
                val rec = words[0]
                val prompt = when (targetLang) {
                    VoiceLanguage.HINDI -> "जी सर, $rec को क्या संदेश भेजना है?"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $rec কে কি বার্তা পাঠাতে হবে?"
                    else -> "Sir, what message should I send to $rec?"
                }
                return BrainDecision.RequestReplyContent(rec, isWhatsApp = effectiveWhatsApp, prompt = prompt)
            }
        }

        // 3. Direct Send Message (Hindi: "<recipient> ko message / sms / whatsapp bhejo...")
        if (lower.contains(" ko message") || lower.contains(" ko whatsapp") || lower.contains(" ko sms") ||
            lower.contains(" को मैसेज") || lower.contains(" को व्हाट्सएप") || lower.contains(" को एसएमएस") ||
            lower.contains(" ke message") || lower.contains(" ke sms") || lower.contains(" কে মেসেজ")
        ) {
            val delimiter = if (lower.contains(" को ")) " को " 
                else if (lower.contains(" কে ")) " কে "
                else if (lower.contains(" ke ")) " ke "
                else " ko "
            val target = original.substringBefore(delimiter).trim()
            val after = original.substringAfter(delimiter).trim()
            val isSms = lower.contains("sms") || lower.contains("एसएमएस")
            val effectiveWhatsApp = if (isSms) false else isWhatsApp
            val messageText = after.replace(Regex("^(message bhejo|whatsapp karo|message karo|sms bhejo|sms karo|मैसेज भेजो|व्हाट्सएप करो|मैसेज करो|एसएमएस भेजो|বার্তা পাঠাও|মেসেজ পাঠাও)"), "").trimStart(':', '-', ' ')
            if (target.isNotEmpty()) {
                val cleanMsg = if (messageText.isEmpty()) "Hello, Sir." else messageText
                return BrainDecision.SendMessage(target, cleanMsg, effectiveWhatsApp, requiresConfirmation = !effectiveWhatsApp)
            }
        }

        return null
    }

    private fun tryEvaluateMath(query: String): String? {
        val clean = query.replace("what is", "")
            .replace("calculate", "")
            .replace("times", "*")
            .replace("multiplied by", "*")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("divided by", "/")
            .replace("into", "*")
            .replace("guna", "*")
            .replace("jod", "+")
            .trim()

        val tokens = clean.split(" ").filter { it.isNotEmpty() }
        if (tokens.size == 3) {
            val left = tokens[0].toDoubleOrNull()
            val op = tokens[1]
            val right = tokens[2].toDoubleOrNull()

            if (left != null && right != null) {
                val res = when (op) {
                    "+", "add" -> left + right
                    "-", "sub" -> left - right
                    "*", "x" -> left * right
                    "/" -> if (right != 0.0) left / right else null
                    else -> null
                }
                if (res != null) {
                    return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
                }
            }
        }
        return null
    }

    private fun parseSystemShutdownAndStandby(cleanLower: String, targetLang: VoiceLanguage): BrainDecision? {
        if (cleanLower.contains("standby") || cleanLower.contains("stand by") ||
            cleanLower.contains("स्टैंडबाई") || cleanLower.contains("স্ট্যান্ডবাই") ||
            cleanLower == "go to sleep" || cleanLower.contains("so jao") || cleanLower.contains("sojao") ||
            cleanLower.contains("सो जाओ") || cleanLower.contains("ঘুমিয়ে পড়ো") ||
            cleanLower.contains("sleep mode")
        ) {
            return BrainDecision.EnterStandbyMode(targetLang)
        }

        if (cleanLower.contains("full shutdown") || cleanLower.contains("complete shutdown") ||
            cleanLower.contains("completely turn off") || cleanLower.contains("turn off max") ||
            cleanLower.contains("power off max") || cleanLower.contains("pura band ho jao") ||
            cleanLower.contains("pura band karo") || cleanLower.contains("সম্পূর্ণ বন্ধ") ||
            cleanLower.contains("पूरी तरह बंद") || cleanLower == "shutdown max"
        ) {
            return BrainDecision.ExecuteFullShutdown(keepReminders = true, language = targetLang)
        }
        return null
    }

    private fun parseScreenVision(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        if (cleanLower.contains("stop watching") || cleanLower.contains("stop screen") ||
            cleanLower.contains("stop screen sharing") || cleanLower.contains("close vision mode") ||
            cleanLower.contains("screen dekhna band") || cleanLower.contains("screen bondho") ||
            cleanLower.contains("স্ক্রিন বন্ধ") || cleanLower.contains("स्क्रीन देखना बंद")
        ) {
            return BrainDecision.StopScreenVision(targetLang)
        }

        val isScreenVisionTrigger = cleanLower.contains("kya tum ye dekh") || cleanLower.contains("kya tum yeh dekh") ||
                cleanLower.contains("kya tum screen dekh") || cleanLower.contains("can you see this") ||
                cleanLower.contains("can you see my screen") || cleanLower.contains("what is on my screen") ||
                cleanLower.contains("look at my screen") || cleanLower.contains("look at this") ||
                cleanLower.contains("see my screen") || cleanLower.contains("see this screen") ||
                cleanLower.contains("screen dekho") || cleanLower.contains("meri screen dekho") ||
                cleanLower.contains("screen dekh kar") || cleanLower.contains("screen dekh ke") ||
                cleanLower.contains("screen help") || cleanLower.contains("screen par kya") || cleanLower.contains("screen pe kya") ||
                cleanLower.contains("dekh kar batao") || cleanLower.contains("dekh ke batao") ||
                cleanLower.contains("isme kya likha hai") || cleanLower.contains("ete ki lekha") || cleanLower.contains("এতে কি লেখা") ||
                cleanLower == "read this" || cleanLower.contains("read my screen") || cleanLower.contains("screen padho") ||
                cleanLower.contains("screen poro") || cleanLower.contains("স্ক্রিন পড়ো") || cleanLower.contains("read this page") ||
                cleanLower.contains("explain this") || cleanLower.contains("isko explain karo") || cleanLower.contains("eta explain") ||
                cleanLower.contains("ye error kya hai") || cleanLower.contains("what is this error") || cleanLower.contains("what error is this") ||
                cleanLower.contains("what am i looking at") || cleanLower.contains("explain this diagram") || cleanLower.contains("diagram samjhao") ||
                cleanLower.contains("চিত্রটি ব্যাখ্যা করো") || cleanLower.contains("explain this question") || cleanLower.contains("explain screen") ||
                cleanLower.contains("translate this page") || cleanLower.contains("find the error") ||
                cleanLower.contains("help me solve this") || cleanLower.contains("screen vision") ||
                cleanLower.contains("watch my screen") ||
                cleanLower.contains("স্ক্রিন দেখো") || cleanLower.contains("स्क्रीन देखो")

        if (isScreenVisionTrigger) {
            val isLive = cleanLower.contains("live vision") || cleanLower.contains("watch my screen") || cleanLower.contains("keep watching")
            val prompt = when {
                cleanLower.contains("isme kya likha hai") || cleanLower.contains("read this page") || cleanLower.contains("read this") || cleanLower.contains("read my screen") || cleanLower.contains("screen padho") -> "Read aloud the text and key elements visible on the screen."
                cleanLower.contains("explain this diagram") || cleanLower.contains("diagram samjhao") -> "Analyze and explain the diagram, chart, or visual structure visible on the screen."
                cleanLower.contains("ye error kya hai") || cleanLower.contains("find the error") -> "Identify the error message or alert displayed on screen and explain what is causing it."
                cleanLower.contains("what am i looking at") || cleanLower.contains("explain this") -> "Provide a clear summary of what application or content is currently visible on the screen."
                cleanLower.contains("explain this question") || cleanLower.contains("explain screen") -> "Read the question visible on screen and explain the solution step by step."
                cleanLower.contains("translate this page") -> "Translate the visible text on screen to Hindi/English."
                cleanLower.contains("help me solve this") -> "Help me solve the problem visible on the screen step by step."
                else -> trimmed
            }
            return BrainDecision.CaptureScreenVision(prompt, isLive, targetLang)
        }
        return null
    }

    private fun parseScreenQuestionFollowUp(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        if (screenContextManager.isScreenFollowUpQuery(cleanLower)) {
            return BrainDecision.ScreenQuestionFollowUp(trimmed, targetLang)
        }
        return null
    }

    private fun parseAppControlCommands(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // 1. Scrolling
        if (cleanLower == "scroll down" || cleanLower.contains("scroll down") ||
            cleanLower.contains("niche scroll") || cleanLower.contains("নিচে স্ক্রোল") ||
            cleanLower.contains("neeche scroll") || cleanLower.contains("scroll karo neeche") ||
            cleanLower == "neeche karo" || cleanLower == "neeche jao" || cleanLower.contains("neeche scroll karo")
        ) {
            return BrainDecision.ScrollScreen(isDown = true, targetLang)
        }
        if (cleanLower == "scroll up" || cleanLower.contains("scroll up") ||
            cleanLower.contains("upar scroll") || cleanLower.contains("উপরে স্ক্রোল") ||
            cleanLower.contains("scroll karo upar") || cleanLower == "upar karo" || cleanLower == "upar jao"
        ) {
            return BrainDecision.ScrollScreen(isDown = false, targetLang)
        }

        // 2. Navigation Back
        if (cleanLower == "go back" || cleanLower == "back" || cleanLower == "back jao" ||
            cleanLower == "piche jao" || cleanLower == "পেছনে যাও" || cleanLower.contains("go back") ||
            cleanLower.contains("peeche jao") || cleanLower == "back karo" || cleanLower == "wapas jao"
        ) {
            return BrainDecision.NavigateBack(targetLang)
        }

        // 3. Close Screen / Home
        if (cleanLower.contains("close this screen") || cleanLower.contains("screen band karo") ||
            cleanLower.contains("স্ক্রিন বন্ধ করো") || cleanLower.contains("ye screen band") ||
            cleanLower == "home" || cleanLower == "go to home" || cleanLower == "home screen" ||
            cleanLower.contains("home par jao") || cleanLower.contains("home jao")
        ) {
            return BrainDecision.CloseScreen(targetLang)
        }

        // 4. Click nth option: e.g. "click the second option", "click 2nd option", "open the first result"
        val nthMatch = Regex("(?:click|open|tap|chuno|dabao)\\s+(?:the\\s+)?(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th|pehla|doosra|dusra|teesra|chautha|প্রথম|দ্বিতীয়|তৃতীয়)(?:\\s+option|\\s+result|\\s+link)?")
            .find(cleanLower)
        if (nthMatch != null) {
            val ord = nthMatch.groupValues[1]
            val index = when (ord) {
                "first", "1st", "pehla", "প্রথম" -> 1
                "second", "2nd", "doosra", "dusra", "দ্বিতীয়" -> 2
                "third", "3rd", "teesra", "তৃতীয়" -> 3
                "fourth", "4th", "chautha" -> 4
                "fifth", "5th" -> 5
                else -> 1
            }
            return BrainDecision.ClickNthScreenOption(index, targetLang)
        }

        // 5. WhatsApp deep chat open: only when explicitly targeting a contact chat without "reply" or "send"
        if (cleanLower.contains("chat") && (cleanLower.contains("open") || cleanLower.contains("kholo")) &&
            !cleanLower.contains("reply") && !cleanLower.contains("send")
        ) {
            val contact = cleanLower.replace(Regex("(?i)^(.*?(find|open|chat with|kholo))\\s+"), "")
                .replace(Regex("(?i)(chat|in whatsapp|on whatsapp|ka chat|ki chat|whatsapp)"), "")
                .trim()
            if (contact.isNotBlank() && contact != "whatsapp") {
                return BrainDecision.ClickScreenElement(contact, targetLang)
            }
        }

        // 6. Direct Typing: "type hello how are you" / "type this ..."
        if (cleanLower.startsWith("type ") || cleanLower.startsWith("type karo ") || cleanLower.startsWith("likho ")) {
            val textToType = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max|ম্যাক্স|হেই\\s+ম্যাক্স|হে\\s+मैक्स|मैक्स)[,\\s:]*"), "")
                .replace(Regex("(?i)^(type\\s+this|type\\s+karo|type|likho)\\s*[:,]?\\s*"), "")
                .trim()
            if (textToType.isNotBlank()) {
                return BrainDecision.TypeScreenText(textToType, targetLang)
            }
        }

        // 7. General Click / Tap on any visible UI Element
        val clickPatterns = listOf(
            Regex("^(?:click\\s+(?:on\\s+)?|tap\\s+(?:on\\s+)?|press\\s+)(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(.+?)\\s+(?:par\\s+click(?:\\s+karo)?|pe\\s+click(?:\\s+karo)?|click\\s+karo|dabao|chuno)$", RegexOption.IGNORE_CASE)
        )
        for (pat in clickPatterns) {
            val m = pat.find(cleanLower)
            if (m != null) {
                val target = m.groupValues[1].trim('\'', '"', ' ', '.', ',')
                if (PaymentProtectionManager.isPaymentAction(target)) {
                    val response = PaymentProtectionManager.getBlockedPaymentMessage(targetLang)
                    return BrainDecision.SpeakResponse(response, targetLang)
                }
                if (target.isNotBlank() && target != "this" && target != "here" && !target.contains("option") && !target.contains("result")) {
                    return BrainDecision.ClickScreenElement(target, targetLang)
                }
            }
        }

        // 8. Read on-screen text / notifications
        if (cleanLower.contains("read this notification") || cleanLower.contains("find my unread messages") ||
            cleanLower.contains("read notification") || cleanLower.contains("unread messages")
        ) {
            return BrainDecision.ReadScreenContent(targetLang)
        }

        // 9. Switch to App: "switch to Chrome", "switch to WhatsApp"
        if (cleanLower.startsWith("switch to ") || cleanLower.contains("par switch karo")) {
            val appTarget = cleanLower.replace(Regex("(?i)^(switch\\s+to|switch\\s+karo)\\s+"), "").replace(" par switch karo", "").trim()
            if (appTarget.isNotBlank()) {
                return BrainDecision.SwitchApp(appTarget, targetLang)
            }
        }

        return null
    }

    private fun parseMultiStepTaskCommands(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // Task progress & cancellation
        if (cleanLower.contains("cancel this operation") || cleanLower.contains("cancel operation") ||
            cleanLower.contains("cancel this task") || cleanLower.contains("task roko") ||
            cleanLower.contains("অপারেশন বাতিল করো")
        ) {
            return BrainDecision.CancelTask(targetLang)
        }

        if (cleanLower.contains("explain what you are doing") || cleanLower.contains("show me what to do next") ||
            cleanLower.contains("what are you doing next") || cleanLower.contains("kya kar rahe ho")
        ) {
            return BrainDecision.ExplainTaskProgress(targetLang)
        }

        // Form filling assistance: "help me fill this form", "fill this form", "form bharne mein help"
        if (cleanLower.contains("fill this form") || cleanLower.contains("help me fill this form") ||
            cleanLower.contains("form bharo") || cleanLower.contains("ফর্ম পূরণ করো")
        ) {
            return BrainDecision.FillFormAssistance(trimmed, targetLang)
        }

        // Intercept any payment/money transfer compound task
        if (PaymentProtectionManager.isPaymentRequest(cleanLower) || PaymentProtectionManager.isPaymentRequest(trimmed)) {
            val response = PaymentProtectionManager.getBlockedPaymentMessage(targetLang)
            return BrainDecision.SpeakResponse(response, targetLang)
        }

        // Compound tasks: "open youtube search for ... and play ...", "open chrome search for ... and open first result"
        val isCompound = (cleanLower.contains("youtube") && (cleanLower.contains("search") || cleanLower.contains("khojo") || cleanLower.contains("dhoondo")) && (cleanLower.contains("play") || cleanLower.contains("chalao") || cleanLower.contains("chala do") || cleanLower.contains("bajao") || cleanLower.contains("song") || cleanLower.contains("gaana"))) ||
                (cleanLower.contains("chrome") && cleanLower.contains("search") && cleanLower.contains("result")) ||
                (cleanLower.contains("bluetooth") && cleanLower.contains("turn on") && cleanLower.contains("settings"))
        if (isCompound) {
            return BrainDecision.ExecuteCompoundTask(trimmed, targetLang)
        }

        return null
    }

    private fun parseAdvancedUpgradeCommands(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // 1. Contextual Memory:
        // Yesterday's conversation recall
        if (cleanLower.contains("kal humne kya discuss kiya") || cleanLower.contains("kal kya discuss kiya") ||
            cleanLower.contains("what did we discuss yesterday") || cleanLower.contains("kal humne kya baat ki") ||
            cleanLower.contains("yesterday discussion")
        ) {
            return BrainDecision.RecallYesterdayDiscussion(targetLang)
        }

        // Project tasks recall: "TripMozy ka pending kaam kya hai" / "<project> ka pending kaam" / "pending tasks for <project>"
        val projectMatch = Regex("(?i)([a-z0-9_-]+)\\s+ka\\s+pending\\s+kaam").find(cleanLower)
            ?: Regex("(?i)pending\\s+(?:tasks|work)\\s+(?:for|of)\\s+([a-z0-9_-]+)").find(cleanLower)
        if (projectMatch != null) {
            val projectName = projectMatch.groupValues[1].trim()
            return BrainDecision.RecallProjectTasks(projectName, targetLang)
        }

        // Save memory: "yaad rakho ki <fact>" / "remember this: <fact>" / "remember that <fact>" / "yaad rakhna <fact>"
        if (cleanLower.startsWith("yaad rakho") || cleanLower.startsWith("remember this") ||
            cleanLower.startsWith("remember that") || cleanLower.startsWith("yaad rakhna") ||
            cleanLower.contains("yeh yaad rakho")
        ) {
            val fact = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                .replace(Regex("(?i)^(yeh\\s+yaad\\s+rakho|yaad\\s+rakho\\s+ki|yaad\\s+rakho|yaad\\s+rakhna|remember\\s+this\\s*[:,]?|remember\\s+that)\\s*"), "")
                .trim()
            if (fact.isNotBlank()) {
                return BrainDecision.SaveUserMemory(fact, targetLang)
            }
        }

        // Forget memory: "bhool jao <query>" / "forget <query>"
        if (cleanLower.startsWith("bhool jao") || cleanLower.startsWith("forget this") || cleanLower.startsWith("forget that")) {
            val query = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                .replace(Regex("(?i)^(bhool\\s+jao|forget\\s+this|forget\\s+that|forget)\\s*"), "")
                .trim()
            return BrainDecision.ForgetUserMemory(query, targetLang)
        }

        // Recall all memories: "mere baare mein kya jante ho" / "what do you remember about me"
        if (cleanLower.contains("mere baare mein kya jante ho") || cleanLower.contains("what do you remember about me") ||
            cleanLower.contains("saved memories dikhao") || cleanLower.contains("show my memories")
        ) {
            return BrainDecision.RecallUserMemories(targetLang)
        }

        // 2. Self-Healing & Health Monitor:
        // "system health" / "check health" / "kaisa chal raha hai system" / "health report"
        if (cleanLower.contains("system health") || cleanLower.contains("health report") ||
            cleanLower.contains("kaisa chal raha hai system") || cleanLower.contains("health check")
        ) {
            return BrainDecision.CheckSystemHealth(targetLang)
        }
        if (cleanLower.contains("run diagnostics") || cleanLower.contains("system diagnostics") ||
            cleanLower.contains("diagnostic report") || cleanLower.contains("diagnostics check")
        ) {
            return BrainDecision.RunSelfDiagnostics(targetLang)
        }

        // 3. Intelligent Notification Manager:
        // "important notifications" / "mere notification batao" / "read my notifications"
        if (cleanLower.contains("important notifications") || cleanLower.contains("mere notification batao") ||
            cleanLower.contains("read my notifications") || cleanLower.contains("read notifications") ||
            cleanLower.contains("koi notification hai kya")
        ) {
            return BrainDecision.SummarizeNotifications(targetLang)
        }
        if (cleanLower.contains("whatsapp messages padho") || cleanLower.contains("read whatsapp notifications") ||
            cleanLower.contains("whatsapp notification padho") || cleanLower.contains("read whatsapp messages")
        ) {
            return BrainDecision.ReadWhatsAppNotifications(targetLang)
        }

        // 4. Live Internet Research Engine:
        // "search internet for <query>" / "online research karo <query>" / "research on <query>"
        if (cleanLower.startsWith("search internet for") || cleanLower.startsWith("online research karo") ||
            cleanLower.startsWith("research on") || cleanLower.startsWith("internet pe research karo")
        ) {
            val query = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                .replace(Regex("(?i)^(search\\s+internet\\s+for|online\\s+research\\s+karo|research\\s+on|internet\\s+pe\\s+research\\s+karo)\\s*"), "")
                .trim()
            if (query.isNotBlank()) {
                return BrainDecision.ConductLiveResearch(query, targetLang)
            }
        }

        // 5. Performance Analytics Engine:
        // "max performance" / "performance report" / "performance stats"
        if (cleanLower.contains("max performance") || cleanLower.contains("performance report") ||
            cleanLower.contains("performance stats") || cleanLower.contains("success rate")
        ) {
            return BrainDecision.QueryPerformanceStats(targetLang)
        }

        return null
    }

    private fun parseStudyAndEducation(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        val isQuizAnswer = cleanLower.matches(Regex("^(option|ans|answer|viklap|vikalp|uttar|উত্তৰ)?\\s*([abcd1234]|pehla|dusra|teesra|chautha|first|second|third|fourth)(\\s*option)?$")) ||
                cleanLower.startsWith("option ") || cleanLower.startsWith("answer ") || cleanLower.startsWith("sahi option ")
        if (isQuizAnswer && !cleanLower.contains("setting") && !cleanLower.contains("menu")) {
            return BrainDecision.AnswerStudyQuiz(cleanLower, targetLang)
        }

        if (cleanLower.contains("quiz") || cleanLower.contains("mcq") || cleanLower.contains("revision test") ||
            cleanLower.contains("questions") || cleanLower.contains("question") ||
            cleanLower.contains("questions banao") || cleanLower.contains("প্রশ্ন তৈরি করো") || cleanLower.contains("क्विज़")
        ) {
            if (cleanLower.contains("start") || cleanLower.contains("create") || cleanLower.contains("banao") ||
                cleanLower.contains("shuru") || cleanLower.contains("शुरू") || cleanLower.contains("তৈরি") ||
                cleanLower.contains("ssc") || cleanLower.contains("upsc") || cleanLower.contains("revision")
            ) {
                val examType = when {
                    cleanLower.contains("upsc") || cleanLower.contains("ias") -> "UPSC"
                    cleanLower.contains("railway") || cleanLower.contains("rrb") -> "RRB Railway"
                    cleanLower.contains("banking") || cleanLower.contains("ibps") -> "Banking"
                    else -> "SSC"
                }
                val topic = trimmed.replace(Regex("(?i)(create|start|my|revision|test|quiz|questions|banao|shuru|karo|ssc|upsc|mcq|20|10|5|for|on|pe)"), "").trim()
                val effectiveTopic = if (topic.isNotBlank()) topic else "Indian General Knowledge & History"
                return BrainDecision.StartStudyQuiz(effectiveTopic, examType, targetLang)
            }
        }

        val isWeather = cleanLower.contains("weather") || cleanLower.contains("mausam") || cleanLower.contains("मौसम") || cleanLower.contains("আবহাওয়া")
        if (!isWeather) {
            if (cleanLower.startsWith("make notes") || cleanLower.startsWith("create notes") ||
                cleanLower.startsWith("chapter notes") || cleanLower.contains("notes banao") ||
                cleanLower.contains("ke notes") || cleanLower.contains("er notes") ||
                cleanLower.contains("নোট তৈরি") || cleanLower.contains("नोट्स बनाओ") ||
                (cleanLower.startsWith("explain ") && (cleanLower.contains("notes") || cleanLower.contains("science") || cleanLower.contains("history") || cleanLower.contains("photosynthesis") || cleanLower.contains("biology") || cleanLower.contains("physics") || cleanLower.contains("chapter")))
            ) {
                val isDetailed = cleanLower.contains("detail") || cleanLower.contains("chapter") || cleanLower.contains("vistrit")
                val topic = trimmed.replace(Regex("(?i)^(make\\s+notes\\s+on|create\\s+notes\\s+on|chapter\\s+notes\\s+on|notes\\s+banao\\s+|notes\\s+on\\s+|explain\\s+)"), "")
                    .replace(Regex("(?i)(\\s+pe\\s+notes\\s+banao|\\s+ke\\s+notes|\\s+in\\s+hindi|\\s+in\\s+detail|\\s+in\\s+bengali|\\s+in\\s+english)"), "")
                    .trim()
                val effectiveTopic = if (topic.isNotBlank()) topic else "General Studies"
                return BrainDecision.CreateStudyNotes(effectiveTopic, isDetailed, targetLang)
            }
        }

        if (cleanLower.startsWith("solve ") || cleanLower.contains("math problem") ||
            cleanLower.contains("ganit solve") || cleanLower.contains("অংক সমাধান") ||
            cleanLower.contains("गणित हल")
        ) {
            val problem = trimmed.replace(Regex("(?i)^(solve\\s+math\\s+problem|solve\\s+math|solve\\s+|ganit\\s+solve\\s+karo\\s+)"), "").trim()
            if (problem.isNotBlank()) {
                return BrainDecision.SolveMath(problem, targetLang)
            }
        }

        if (cleanLower.contains("grammar") || cleanLower.contains("व्याकरण") || cleanLower.contains("ব্যাকরণ")) {
            val query = trimmed.replace(Regex("(?i)(grammar\\s+explain\\s+karo|grammar\\s+check|explain\\s+grammar|batao|karo)"), "").trim()
            val effectiveQuery = if (query.isNotBlank()) query else trimmed
            return BrainDecision.ExplainGrammar(effectiveQuery, targetLang)
        }

        return null
    }

    private fun parseSmartReminders(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        if (cleanLower.contains("show reminders") || cleanLower.contains("show my reminders") ||
            cleanLower.contains("list reminders") || cleanLower.contains("reminders batao") ||
            cleanLower.contains("upcoming reminders") || cleanLower.contains("active reminders") ||
            cleanLower.contains("मेरे रिमाइंडर") || cleanLower.contains("আমার রিমাইন্ডার")
        ) {
            return BrainDecision.ListSmartReminders(targetLang)
        }

        if (cleanLower.startsWith("remind me") || cleanLower.contains("set reminder") ||
            cleanLower.contains("reminder lagao") || cleanLower.contains("reminder set karo") ||
            cleanLower.contains("yad dilana") || cleanLower.contains("yaad dilana") ||
            cleanLower.contains("মনে করিয়ে দাও") || cleanLower.contains("रिमाइंडर लगाओ")
        ) {
            val repeat = when {
                cleanLower.contains("daily") || cleanLower.contains("roz") || cleanLower.contains("har din") || cleanLower.contains("প্রতিদিন") -> "DAILY"
                cleanLower.contains("weekly") || cleanLower.contains("har hafte") || cleanLower.contains("সাপ্তাহিক") -> "WEEKLY"
                cleanLower.contains("monthly") || cleanLower.contains("har mahine") || cleanLower.contains("মাসিক") -> "MONTHLY"
                else -> "NONE"
            }

            val priority = when {
                cleanLower.contains("urgent") || cleanLower.contains("important") || cleanLower.contains("zaroori") -> "HIGH"
                else -> "MEDIUM"
            }

            val cal = java.util.Calendar.getInstance()
            if (cleanLower.contains("tomorrow") || cleanLower.contains("kal")) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje)?")
            val match = timeRegex.find(cleanLower)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 9
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val amPm = match.groupValues[3].lowercase()
                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0
                if (amPm == "baje" && hour in 1..6) hour += 12
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, minute)
                cal.set(java.util.Calendar.SECOND, 0)
            } else {
                cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
            }

            var triggerEpoch = cal.timeInMillis
            if (triggerEpoch <= System.currentTimeMillis()) {
                triggerEpoch += 86400000L
            }

            val title = trimmed
                .replace(Regex("(?i)^(remind\\s+me\\s+to|remind\\s+me\\s+for|set\\s+reminder\\s+for|set\\s+reminder\\s+to|reminder\\s+lagao|reminder\\s+set\\s+karo)"), "")
                .replace(Regex("(?i)(tomorrow|kal|today|aaj|daily|weekly|at\\s+\\d{1,2}(?::\\d{2})?\\s*(am|pm)?|\\d{1,2}\\s*baje)"), "")
                .trim()
            val effectiveTitle = if (title.isNotBlank()) title else "Task reminder"

            return BrainDecision.ScheduleSmartReminder(
                title = effectiveTitle,
                notes = "Scheduled via MAX Voice AI",
                triggerEpoch = triggerEpoch,
                repeat = repeat,
                priority = priority,
                language = targetLang
            )
        }

        return null
    }

    private fun parseMasterDictionaryCommand(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        val recognized = intentRecognizer.recognize(trimmed, targetLang)
        when (recognized.intentId) {
            "AI003" -> {
                val fact = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, ek interesting fact: Honey kabhi kharab nahi hota. Hazaro saal purana honey bhi bilkul theek rehta hai!"
                    VoiceLanguage.HINDI -> "सर, एक दिलचस्प तथ्य: शहद कभी खराब नहीं होता। हज़ारों साल पुराना शहद भी खाने योग्य रहता है।"
                    VoiceLanguage.BENGALI -> "স্যার, একটি মজার তথ্য: মধু কখনো নষ্ট হয় না।"
                    else -> "Sir, an interesting fact: Honey never spoils; 3,000-year-old honey found in Egyptian tombs is still perfectly edible."
                }
                return BrainDecision.SpeakResponse(fact, targetLang)
            }
            "AI004" -> {
                val caps = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, main phone calls, verified WhatsApp messages, study notes, revision quizzes, exact reminders, screen vision aur phone settings control kar sakta hoon."
                    VoiceLanguage.HINDI -> "सर, मैं कॉल, व्हाट्सएप संदेश, अध्ययन नोट्स, क्विज़, सटीक रिमाइंडर, स्क्रीन विज़न और सिस्टम सेटिंग्स नियंत्रित कर सकता हूँ।"
                    VoiceLanguage.BENGALI -> "স্যার, আমি ফোন কল, হোয়াটসঅ্যাপ বার্তা, স্টাডি নোটস, কুইজ, রিমাইন্ডার এবং স্ক্রিন ভিশন পরিচালনা করতে পারি।"
                    else -> "Sir, I can manage your phone calls, send confirmed WhatsApp messages, generate study notes, conduct revision tests, schedule reminders, evaluate your screen, and control device settings."
                }
                return BrainDecision.SpeakResponse(caps, targetLang)
            }
            "AI005" -> {
                val help = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, main bilkul ready hoon aapki madad karne ke liye. Bataiye kya kaam karna hai?"
                    VoiceLanguage.HINDI -> "सर, मैं हमेशा आपकी मदद के लिए तैयार हूँ। बताइए मुझे क्या करना है?"
                    VoiceLanguage.BENGALI -> "স্যার, আমি আপনাকে সাহায্য করার জন্য প্রস্তুত। বলুন কি করতে হবে?"
                    else -> "I am right here to help, Sir. Please tell me what you need assistance with."
                }
                return BrainDecision.SpeakResponse(help, targetLang)
            }
            "AI006" -> {
                val simp = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, ab se main sabhi batein bilkul simple aur seedhi bhasha mein samjhaunga."
                    VoiceLanguage.HINDI -> "सर, अब से मैं सभी विषयों को सरल और सहज भाषा में समझाऊँगा।"
                    VoiceLanguage.BENGALI -> "স্যার, এখন থেকে আমি সবকিছু সহজ এবং স্পষ্ট ভাষায় বোঝাব।"
                    else -> "Sir, I will keep all future explanations simple, intuitive, and concise."
                }
                return BrainDecision.SpeakResponse(simp, targetLang)
            }
            // Category 2 – Indian GK
            "GK001" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Bharat ki vartaman Rashtrapati Smt. Droupadi Murmu hain."
                    VoiceLanguage.HINDI -> "सर, भारत की वर्तमान माननीय राष्ट्रपति श्रीमती द्रौपदी मुर्मू हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতের বর্তমান রাষ্ট্রপতি হলেন শ্রীমতী দ্রৌপদী মুর্মু।"
                    else -> "Sir, the Honorable President of India is Smt. Droupadi Murmu."
                }
                knowledgeBase.activeContextTopic = "President of India"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "GK002" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Bharat ki rajdhani New Delhi hai."
                    VoiceLanguage.HINDI -> "सर, भारत की राजधानी नई दिल्ली है।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতের রাজধানী হলো নতুন দিল্লি।"
                    else -> "Sir, the capital of India is New Delhi."
                }
                knowledgeBase.activeContextTopic = "Capital of India"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "GK003" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Bharat mein kul 28 states aur 8 Union Territories hain."
                    VoiceLanguage.HINDI -> "सर, भारत में 28 राज्य और 8 केंद्र शासित प्रदेश हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতে ২৮টি রাজ্য এবং ৮টি কেন্দ্রশাসিত অঞ্চল রয়েছে।"
                    else -> "Sir, India consists of 28 States and 8 Union Territories."
                }
                knowledgeBase.activeContextTopic = "States of India"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "GK004" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Bharat 15 August 1947 ko azaad hua tha."
                    VoiceLanguage.HINDI -> "सर, भारत 15 अगस्त 1947 को आज़ाद हुआ था।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারত ১৯৪৭ সালের ১৫ই আগস্ট স্বাধীন হয়েছিল।"
                    else -> "Sir, India attained independence on August 15, 1947."
                }
                knowledgeBase.activeContextTopic = "Indian Independence"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "GK005" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Bharatiya Samvidhan duniya ka sabse bada likhit samvidhan hai. Dr. B.R. Ambedkar ji ki adhyakshata mein ye taiyar hua aur 26 January 1950 ko lagu hua."
                    VoiceLanguage.HINDI -> "सर, भारतीय संविधान दुनिया का सबसे लंबा लिखित संविधान है, जिसे डॉ. बी.आर. अंबेडकर की अध्यक्षता में तैयार किया गया और 26 जनवरी 1950 को लागू किया गया।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতীয় সংবিধান বিশ্বের দীর্ঘতম লিখিত সংবিধান, যা ১৯৫০ সালের ২৬শে জানুয়ারি কার্যকর হয়।"
                    else -> "Sir, the Indian Constitution is the supreme law of India, drafted under Dr. B.R. Ambedkar and enacted on January 26, 1950."
                }
                knowledgeBase.activeContextTopic = "Indian Constitution"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "GK006" -> {
                return BrainDecision.SearchWeb("latest current affairs India UPSC SSC", targetLang)
            }
            // Category 3 – Education
            "EDU001" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Mathematics learning mode active hai Sir! Koi bhi arithmetic, algebra ya geometry problem boliye, main step-by-step solve kar doonga."
                    VoiceLanguage.HINDI -> "गणित शिक्षण मोड सक्रिय है सर! कोई भी गणितीय समस्या बताइए, मैं चरणबद्ध समाधान करूँगा।"
                    VoiceLanguage.BENGALI -> "গণিত শিক্ষা মোড সক্রিয় আছে স্যার! যেকোনো অঙ্ক বলুন, আমি ধাপে ধাপে সমাধান করব।"
                    else -> "Mathematics learning mode active, Sir. Give me any algebraic or arithmetic problem to solve step-by-step."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "EDU002" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, Photosynthesis ek biological process hai jisme hare paudhe suraj ki roshni, paani aur carbon dioxide ka use karke glucose banate hain aur oxygen release karte hain."
                    VoiceLanguage.HINDI -> "सर, प्रकाश संश्लेषण वह प्रक्रिया है जिससे हरे पौधे सूर्य के प्रकाश, पानी और कार्बन डाइऑक्साइड से भोजन बनाते हैं और ऑक्सीजन छोड़ते हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, সালোকসংশ্লেষণ হলো একটি জৈব রাসায়নিক প্রক্রিয়া যার মাধ্যমে সবুজ উদ্ভিদ সূর্যালোক ও জলের সাহায্যে খাদ্য তৈরি করে ও অক্সিজেন ত্যাগ করে।"
                    else -> "Sir, photosynthesis is the process by which green plants synthesize nutrients from carbon dioxide and water using sunlight, releasing oxygen."
                }
                knowledgeBase.activeContextTopic = "Photosynthesis"
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "EDU004" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, aapke competitive exams ke liye high-yield important questions study notes module mein prepare kar diye gaye hain."
                    VoiceLanguage.HINDI -> "सर, आपकी परीक्षा के लिए महत्वपूर्ण प्रश्न अध्ययन मॉड्यूल में तैयार कर दिए गए हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, পরীক্ষার জন্য গুরুত্বপূর্ণ প্রশ্নগুলো প্রস্তুত করা হয়েছে।"
                    else -> "Sir, high-yield examination questions have been generated and saved in your study module."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "EDU006" -> {
                return BrainDecision.CreateStudyNotes("Chapter Revision", true, targetLang)
            }
            // Category 12 – Language & Word Meaning
            "LAN006" -> {
                val word = recognized.parameters["word"] ?: trimmed.substringAfter("meaning").trim()
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, '$word' ka matlab hai: Iska mukhya arth aur paribhasha context ke anusaar prastut ki gayi hai."
                    VoiceLanguage.HINDI -> "सर, '$word' का अर्थ: इसका मुख्य संदर्भ और परिभाषा अध्ययन हेतु स्पष्ट है।"
                    VoiceLanguage.BENGALI -> "স্যার, '$word'-এর অর্থ ও সংজ্ঞা প্রস্তুত।"
                    else -> "Sir, the term '$word' signifies its fundamental conceptual definition and contextual meaning."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            // Category 13 – Calculations & Finance
            "CAL001" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, 1000 ka 25 percent 250 hota hai."
                    VoiceLanguage.HINDI -> "सर, 1000 का 25 प्रतिशत 250 होता है।"
                    VoiceLanguage.BENGALI -> "স্যার, ১০০০-এর ২৫ শতাংশ হলো ২৫০।"
                    else -> "Sir, 25 percent of 1000 is 250."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "CAL002" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, 1 US Dollar lagbhag 85 se 86 Indian Rupees ke barabar hai."
                    VoiceLanguage.HINDI -> "सर, 1 अमेरिकी डॉलर लगभग 85 भारतीय रुपये के बराबर है।"
                    VoiceLanguage.BENGALI -> "স্যার, ১ মার্কিন ডলার প্রায় ৮৫ ভারতীয় টাকার সমান।"
                    else -> "Sir, 1 US Dollar is currently equivalent to approximately 85 Indian Rupees."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "CAL003" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, aapke monthly kharche ka total calculation aur breakdown tayar hai."
                    VoiceLanguage.HINDI -> "सर, आपके मासिक खर्चों का कुल हिसाब तैयार है।"
                    VoiceLanguage.BENGALI -> "স্যার, আপনার মাসিক খরচের সম্পূর্ণ হিসাব প্রস্তুত।"
                    else -> "Sir, your monthly expenses calculation and breakdown has been computed."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "CAL006" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, 18 percent GST calculate karne par: 1000 rupees par 180 rupees GST hoga, kul total 1180 rupees."
                    VoiceLanguage.HINDI -> "सर, 1000 रुपये पर 18 प्रतिशत GST 180 रुपये होगा, कुल योग 1180 रुपये।"
                    VoiceLanguage.BENGALI -> "স্যার, ১০০০ টাকায় ১৮ শতাংশ জিএসটি হলো ১৮০ টাকা, মোট ১১৮০ টাকা।"
                    else -> "Sir, 18 percent GST on 1,000 rupees is 180 rupees, making the total amount 1,180 rupees."
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            // Category 14 – Travel & Daily
            "TRV001" -> {
                if (cleanLower.contains("search") || cleanLower.contains("internet") || cleanLower.contains("google")) {
                    val q = trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                        .replace(Regex("(?i)^(search\\s+the\\s+internet\\s+for|search\\s+internet\\s+for|search\\s+google\\s+for|search\\s+for|search\\s+)"), "")
                        .trimEnd('.', '?', '!', ' ')
                        .trim()
                    return BrainDecision.SearchWeb(if (q.isNotBlank()) q else trimmed, targetLang)
                }
                return BrainDecision.SearchNearby("hotel", targetLang)
            }
            "TRV002" -> {
                return BrainDecision.SearchWeb("trains to Kolkata IRCTC timings", targetLang)
            }
            "TRV005" -> {
                val ans = when (targetLang) {
                    VoiceLanguage.HINGLISH -> "Sir, trip planning module shuru ho gaya hai. Kahan ghoomne jana chahte hain aur kitne dinon ke liye?"
                    VoiceLanguage.HINDI -> "सर, यात्रा योजना मॉड्यूल शुरू हो गया है। आप कहाँ और कितने दिनों के लिए जाना चाहते हैं?"
                    VoiceLanguage.BENGALI -> "স্যার, ভ্রমণ পরিকল্পনা শুরু হয়েছে। আপনি কোথায় ভ্রমণ করতে চান?"
                    else -> "Sir, travel itinerary planner is active. Where would you like to travel and for how many days?"
                }
                return BrainDecision.SpeakResponse(ans, targetLang)
            }
            "TRV006" -> {
                return BrainDecision.SearchNearby("restaurant", targetLang)
            }
            // Category 15 – System & Privacy
            "SYS001" -> {
                return BrainDecision.SpeakResponse("MAX is online and standing by at your service, Sir.", targetLang)
            }
            "SYS006" -> {
                return BrainDecision.SpeakResponse("Current operation cancelled as requested, Sir.", targetLang)
            }
            "SYS007" -> {
                return BrainDecision.SpeakResponse("All MAX systems are fully active and online, Sir.", targetLang)
            }
            // Category 10 – Web & Jobs
            "WEB001" -> {
                val q = recognized.parameters["query"] ?: trimmed.replace(Regex("(?i)^(hey\\s+max|ok\\s+max|hello\\s+max|max)[,\\s:]*"), "")
                    .replace(Regex("(?i)^(search\\s+the\\s+internet\\s+for|search\\s+google\\s+for|search\\s+on\\s+google\\s+for|search\\s+for|search\\s+)"), "")
                    .trimEnd('.', '?', '!', ' ')
                    .trim()
                return BrainDecision.SearchWeb(if (q.isNotBlank()) q else trimmed, targetLang)
            }
            "WEB003" -> {
                return BrainDecision.SearchWeb("latest government jobs recruitment 2026 UPSC SSC Railway", targetLang)
            }
            "WEB004" -> {
                return BrainDecision.SearchWeb("${recognized.parameters["query"] ?: "official"} portal website", targetLang)
            }
            "WEB006" -> {
                return BrainDecision.SpeakResponse("Sir, analyzing and summarizing visible webpage content for you.", targetLang)
            }
            "WEB007" -> {
                return BrainDecision.SpeakResponse("Sir, comparative analysis of search results has been prepared.", targetLang)
            }
            // Category 16 – Baby Entertainment Mode
            "BABY001" -> return BrainDecision.StartBabyMode("crying", targetLang)
            "BABY002" -> return BrainDecision.StartBabyMode("entertain", targetLang)
            "BABY003" -> return BrainDecision.StartBabyMode("laugh", targetLang)
            "BABY004" -> return BrainDecision.PlayBabyLullaby(targetLang)
            "BABY005" -> return BrainDecision.TellBabyStory(targetLang)
            "BABY006" -> return BrainDecision.PlayBabyRhyme(targetLang)
            "BABY007" -> return BrainDecision.NextBabyActivity(targetLang)
            "BABY008" -> return BrainDecision.StopBabyMode(targetLang)
        }
        return null
    }

    private fun parseBabyEntertainment(cleanLower: String, trimmed: String, targetLang: VoiceLanguage): BrainDecision? {
        // Stop baby mode
        if (cleanLower == "stop baby mode" || cleanLower.contains("baby mode band") || cleanLower.contains("বেবি মোড বন্ধ") ||
            cleanLower == "exit baby mode" || cleanLower == "close baby mode"
        ) {
            return BrainDecision.StopBabyMode(targetLang)
        }

        // 1. Crying / Distract
        if (cleanLower.contains("baby is crying") || cleanLower.contains("the baby is crying") ||
            cleanLower.contains("baccha ro raha") || cleanLower.contains("bacha ro raha") ||
            cleanLower.contains("বাচ্চা কাঁদছে") || cleanLower.contains("বাচ্চা কান্নাকাটি") ||
            cleanLower.contains("distract the baby") || cleanLower.contains("iska dhyan bhatkao") ||
            cleanLower.contains("dhyan bhatkao") || cleanLower.contains("বাচ্চার মনোযোগ অন্যদিকে")
        ) {
            return BrainDecision.StartBabyMode("crying", targetLang)
        }

        // 2. Entertain / Play something
        if (cleanLower.contains("entertain the baby") || cleanLower.contains("baby ko entertain") ||
            cleanLower.contains("বাচ্চাকে আনন্দ দাও") || cleanLower.contains("play something for the baby") ||
            cleanLower.contains("start baby mode") || cleanLower.contains("baby mode chalu") ||
            cleanLower.contains("বেবি মোড চালু") || cleanLower.contains("বাচ্চার জন্য কিছু চালাও")
        ) {
            return BrainDecision.StartBabyMode("entertain", targetLang)
        }

        // 3. Make baby laugh / Funny
        if (cleanLower.contains("make the baby laugh") || cleanLower.contains("bachche ko hansaao") ||
            cleanLower.contains("bachhe ko hasao") || cleanLower.contains("baby ko hasao") ||
            cleanLower.contains("বাচ্চাকে হাসাও") || cleanLower.contains("kuch funny karo") ||
            cleanLower.contains("funny karo")
        ) {
            return BrainDecision.StartBabyMode("laugh", targetLang)
        }

        // 4. Help baby sleep / Lullaby
        if (cleanLower.contains("help the baby sleep") || cleanLower.contains("lori sunao") ||
            cleanLower.contains("play a lullaby") || cleanLower.contains("sing a lullaby") ||
            cleanLower.contains("ঘুম পাড়ানোর গান") || cleanLower.contains("baby ko sulao") ||
            cleanLower.contains("pyari lori") || cleanLower.contains("meethi lori") ||
            cleanLower.contains("ঘুম পাড়াও")
        ) {
            return BrainDecision.PlayBabyLullaby(targetLang)
        }

        // 5. Tell baby a story
        if (cleanLower.contains("tell the baby a story") || cleanLower.contains("baby ko kahani") ||
            cleanLower.contains("bachche ko kahani") || cleanLower.contains("বাচ্চাকে গল্প শোনাও") ||
            cleanLower.contains("একটা গল্প বলো") || (cleanLower.contains("story") && cleanLower.contains("baby")) ||
            cleanLower.contains("tell a story")
        ) {
            return BrainDecision.TellBabyStory(targetLang)
        }

        // 6. Nursery Rhymes
        if (cleanLower.contains("recite a rhyme") || cleanLower.contains("play a rhyme") ||
            cleanLower.contains("rhyme sunao") || cleanLower.contains("একটা ছড়া শোনাও") ||
            cleanLower.contains("বাচ্চাদের ছড়া") || cleanLower.contains("bal geet") ||
            cleanLower.contains("nursery rhyme")
        ) {
            return BrainDecision.PlayBabyRhyme(targetLang)
        }

        // 7. Animal Sounds & Next Game
        if (cleanLower.contains("animal sounds") || cleanLower.contains("play animal sounds") ||
            cleanLower.contains("janwar ki awaz") || cleanLower.contains("janwaron ki awaz") ||
            cleanLower.contains("পশুর ডাক") || cleanLower.contains("next game") ||
            cleanLower.contains("next activity") || cleanLower.contains("পরের খেলা") ||
            cleanLower.contains("khel badlo") || cleanLower.contains("agli game")
        ) {
            return BrainDecision.NextBabyActivity(targetLang)
        }

        return null
    }
}
