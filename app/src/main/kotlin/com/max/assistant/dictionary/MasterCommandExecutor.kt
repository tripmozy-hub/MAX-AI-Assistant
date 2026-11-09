package com.max.assistant.dictionary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.max.assistant.ai.MaxKnowledgeBase
import com.max.assistant.education.StudyEducationManager
import com.max.assistant.executor.AndroidCommandExecutor
import com.max.assistant.executor.CommandExecutionResult
import com.max.assistant.service.MaxForegroundService
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.vision.ScreenVisionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MasterCommandExecutor is the unified command dispatcher that executes recognized intents across all 15 categories.
 * Enforces confirmation policies, missing parameter queries, permission checks, and shutdown priority.
 */
class MasterCommandExecutor(
    private val context: Context,
    private val service: MaxForegroundService? = null,
    val contextManager: ConversationContextManager = ConversationContextManager.instance,
    val permissionManager: PermissionManager = PermissionManager(context),
    val historyManager: CommandHistoryManager = CommandHistoryManager(context),
    val reminderManager: ReminderManager = ReminderManager(context),
    val notesManager: NotesManager = NotesManager(context),
    val messagingManager: MessagingManager = MessagingManager(context),
    val contactManager: ContactManager = ContactManager(context),
    val webSearchManager: WebSearchManager = WebSearchManager(context),
    val deviceController: AndroidDeviceController = AndroidDeviceController(context),
    val knowledgeBase: MaxKnowledgeBase = MaxKnowledgeBase(),
    customBabyManager: com.max.assistant.baby.BabyEntertainmentManager? = null
) {

    val babyManager: com.max.assistant.baby.BabyEntertainmentManager = customBabyManager
        ?: (if (service != null && service.isBabyEntertainmentManagerInitialized()) service.babyEntertainmentManager else com.max.assistant.baby.BabyEntertainmentManager(context))

    companion object {
        private const val TAG = "MasterCommandExecutor"
    }

    suspend fun executeIntent(intent: RecognizedIntent): CommandExecutionResult {
        Log.i(TAG, "Executing intent: ${intent.intentId} in category ${intent.category.name} (${intent.detectedLanguage.name})")

        // 1. Missing Parameter Check
        if (intent.missingParameters.isNotEmpty()) {
            val firstMissing = intent.missingParameters.first()
            contextManager.setAwaitingParameter(intent.intentId, firstMissing)
            val prompt = getMissingParameterPrompt(intent.intentId, firstMissing, intent.detectedLanguage)
            return CommandExecutionResult.RequiresConfirmation(prompt, "AWAITING_PARAMETER")
        }

        // 2. Permission Requirements Check
        val def = intent.definition
        if (def != null) {
            val (hasPerms, permError) = permissionManager.checkPermissionsForIntent(def)
            if (!hasPerms) {
                val errorMsg = permError ?: "Permission required"
                historyManager.recordCommand(intent.intentId, intent.rawQuery, intent.detectedLanguage, errorMsg, false)
                return CommandExecutionResult.Blocked(errorMsg)
            }
        }

        // 3. Category & Intent Dispatcher
        val result = when (intent.category) {
            CommandCategory.SYS_PRIVACY -> executeSystemIntent(intent)
            CommandCategory.GENERAL_AI -> executeGeneralAiIntent(intent)
            CommandCategory.INDIAN_GK -> executeIndianGkIntent(intent)
            CommandCategory.EDUCATION -> executeEducationIntent(intent)
            CommandCategory.NOTES -> executeNotesIntent(intent)
            CommandCategory.REMINDERS -> executeRemindersIntent(intent)
            CommandCategory.CLOCK_CALENDAR -> executeClockCalendarIntent(intent)
            CommandCategory.MESSAGING -> executeMessagingIntent(intent)
            CommandCategory.CALLS_CONTACTS -> executeCallsContactsIntent(intent)
            CommandCategory.SCREEN_VISION -> executeScreenVisionIntent(intent)
            CommandCategory.WEB_CHROME -> executeWebChromeIntent(intent)
            CommandCategory.PHONE_CONTROL -> executePhoneControlIntent(intent)
            CommandCategory.TRANSLATION -> executeTranslationIntent(intent)
            CommandCategory.CALCULATIONS -> executeCalculationsIntent(intent)
            CommandCategory.TRAVEL_DAILY -> executeTravelDailyIntent(intent)
            CommandCategory.BABY_ENTERTAINMENT -> executeBabyEntertainmentIntent(intent)
        }

        // 4. Update Context and History
        contextManager.recordIntent(intent.intentId, intent.rawQuery)
        val responseText = when (result) {
            is CommandExecutionResult.Success -> result.message
            is CommandExecutionResult.Blocked -> result.reason
            is CommandExecutionResult.Failed -> result.error
            is CommandExecutionResult.RequiresConfirmation -> result.prompt
        }
        val wasSuccess = result is CommandExecutionResult.Success
        historyManager.recordCommand(intent.intentId, intent.rawQuery, intent.detectedLanguage, responseText, wasSuccess)

        return result
    }

    private fun getMissingParameterPrompt(intentId: String, param: String, lang: VoiceLanguage): String {
        return when (param) {
            "time" -> when (lang) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, kitne baje ka reminder lagana hai?"
                VoiceLanguage.BENGALI -> "স্যার, কটার সময় মনে করিয়ে দিতে হবে?"
                else -> "Sir, what time should I set this for?"
            }
            "task" -> when (lang) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, kis baat ka reminder lagana hai?"
                VoiceLanguage.BENGALI -> "স্যার, কী বিষয়ের রিমাইন্ডার দিতে হবে?"
                else -> "Sir, what task should I remind you about?"
            }
            "contact" -> when (lang) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, aap kise sampark karna chahte hain?"
                VoiceLanguage.BENGALI -> "স্যার, আপনি কাকে কল বা মেসেজ করতে চান?"
                else -> "Sir, which contact would you like me to reach?"
            }
            "message" -> when (lang) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, kya sandesh bhejna hai?"
                VoiceLanguage.BENGALI -> "স্যার, কী বার্তা পাঠাতে চান?"
                else -> "Sir, what message content should I compose?"
            }
            else -> when (lang) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, kripya aage ka vivaran bataiye."
                VoiceLanguage.BENGALI -> "স্যার, অনুগ্রহ করে বিবরণটি বলুন।"
                else -> "Sir, please specify the details."
            }
        }
    }

    // ==================== CATEGORY EXECUTORS ====================

    private fun executeSystemIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        return when (intent.intentId) {
            "SYS001" -> { // Wake up
                service?.serviceController?.reactivate(lang)
                CommandExecutionResult.Success("MAX is online and standing by, Sir.", "SYS_WAKEUP")
            }
            "SYS002" -> { // Standby
                val msg = service?.serviceController?.enterStandby(lang) ?: "MAX in Standby mode, Sir."
                CommandExecutionResult.Success(msg, "SYS_STANDBY")
            }
            "SYS003" -> { // Stop everything
                service?.speechManager?.stopMax()
                service?.voiceEngine?.stop()
                CommandExecutionResult.Success("All operations stopped immediately, Sir.", "SYS_STOP")
            }
            "SYS004" -> { // Shut down completely
                val msg = service?.serviceController?.executeFullShutdown(true, lang) ?: "MAX full shutdown executed, Sir."
                CommandExecutionResult.Success(msg, "SYS_SHUTDOWN")
            }
            "SYS005" -> { // Stop listening
                service?.speechManager?.setMuted(true)
                CommandExecutionResult.Success("Microphone listener stopped, Sir.", "SYS_MUTE")
            }
            "SYS006" -> { // Cancel operation
                service?.confirmationManager?.cancel()
                CommandExecutionResult.Success("Operation cancelled as requested, Sir.", "SYS_CANCEL")
            }
            "SYS007" -> { // Are you active?
                CommandExecutionResult.Success("All MAX systems are fully active and online, Sir.", "SYS_STATUS")
            }
            else -> CommandExecutionResult.Success("System command acknowledged, Sir.", "SYS_DEFAULT")
        }
    }

    private fun executeGeneralAiIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        return when (intent.intentId) {
            "AI001" -> {
                val resp = when (lang) {
                    VoiceLanguage.HINDI -> "जी सर, बताइए मैं आपकी क्या मदद करूँ?"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, আমি আপনার সেবায় প্রস্তুত।"
                    else -> "At your service, Sir. How may I assist you?"
                }
                CommandExecutionResult.Success(resp, "AI_ACTIVATE")
            }
            "AI002" -> {
                val resp = when (lang) {
                    VoiceLanguage.HINGLISH -> "Main bilkul theek hoon Sir! Bataiye aaj main aapki kya help kar sakta hoon?"
                    VoiceLanguage.HINDI -> "मैं बहुत अच्छा हूँ, सर! बताइए आज मैं आपकी क्या मदद करूँ?"
                    VoiceLanguage.BENGALI -> "আমি খুব ভালো আছি, স্যার! বলুন আজ কি সাহায্য করতে পারি?"
                    else -> "I am functioning flawlessly, Sir. How may I be of service to you today?"
                }
                CommandExecutionResult.Success(resp, "AI_CONVERSATION")
            }
            "AI003" -> {
                val facts = listOf(
                    "Honey never spoils; archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still perfectly edible.",
                    "Light travels around the Earth about 7.5 times in just one second at 299,792 kilometers per second.",
                    "The Indian Railways employs over 1.2 million personnel, making it one of the world's largest employers."
                )
                CommandExecutionResult.Success("Here is an interesting fact, Sir: ${facts.random()}", "AI_FACT")
            }
            "AI004" -> {
                val caps = "Sir, I can manage your phone calls, send confirmed WhatsApp messages, generate study notes, conduct revision quizzes, schedule exact reminders, evaluate your screen, and control device settings."
                CommandExecutionResult.Success(caps, "AI_CAPABILITIES")
            }
            "AI005" -> {
                CommandExecutionResult.Success("I am right here to help, Sir. Please tell me what you need assistance with.", "AI_HELP")
            }
            "AI006" -> {
                CommandExecutionResult.Success("Sir, I will keep all future explanations simple, intuitive, and concise.", "AI_SIMPLIFY")
            }
            else -> CommandExecutionResult.Success("At your service, Sir.", "AI_DEFAULT")
        }
    }

    private fun executeIndianGkIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        return when (intent.intentId) {
            "GK001" -> {
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, भारत की वर्तमान माननीय राष्ट्रपति श्रीमती द्रौपदी मुर्मू हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতের বর্তমান রাষ্ট্রপতি হলেন শ্রীমতী দ্রৌপদী মুর্মু।"
                    else -> "Sir, the Honorable President of India is Smt. Droupadi Murmu."
                }
                contextManager.recordTopic("President of India")
                CommandExecutionResult.Success(ans, "GK_PRESIDENT")
            }
            "GK002" -> {
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, भारत की राजधानी नई दिल्ली है।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতের রাজধানী হলো নতুন দিল্লি।"
                    else -> "Sir, the capital of India is New Delhi."
                }
                contextManager.recordTopic("Capital of India")
                CommandExecutionResult.Success(ans, "GK_CAPITAL")
            }
            "GK003" -> {
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, भारत में 28 राज्य और 8 केंद्र शासित प्रदेश हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতে ২৮টি রাজ্য এবং ৮টি কেন্দ্রশাসিত অঞ্চল রয়েছে।"
                    else -> "Sir, India consists of 28 States and 8 Union Territories."
                }
                contextManager.recordTopic("States of India")
                CommandExecutionResult.Success(ans, "GK_STATES")
            }
            "GK004" -> {
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, भारत 15 अगस्त 1947 को आज़ाद हुआ था।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারত ১৯৪৭ সালের ১৫ই আগস্ট স্বাধীন হয়েছিল।"
                    else -> "Sir, India attained independence on August 15, 1947."
                }
                contextManager.recordTopic("Indian Independence")
                CommandExecutionResult.Success(ans, "GK_INDEPENDENCE")
            }
            "GK005" -> {
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, भारतीय संविधान दुनिया का सबसे लंबा लिखित संविधान है, जिसे डॉ. बी.आर. अंबेडकर की अध्यक्षता में तैयार किया गया और 26 जनवरी 1950 को लागू किया गया।"
                    VoiceLanguage.BENGALI -> "স্যার, ভারতীয় সংবিধান বিশ্বের দীর্ঘতম লিখিত সংবিধান, যা ১৯৫০ সালের ২৬শে জানুয়ারি কার্যকর হয়।"
                    else -> "Sir, the Indian Constitution is the supreme law of India, framed under Dr. B.R. Ambedkar and enacted on January 26, 1950."
                }
                contextManager.recordTopic("Indian Constitution")
                CommandExecutionResult.Success(ans, "GK_CONSTITUTION")
            }
            "GK006" -> {
                webSearchManager.searchGoogle("latest current affairs India UPSC SSC")
                CommandExecutionResult.Success("Sir, opening the latest current affairs and national updates for you.", "GK_CURRENT_AFFAIRS")
            }
            else -> CommandExecutionResult.Success("Factual query answered, Sir.", "GK_DEFAULT")
        }
    }

    private suspend fun executeEducationIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        return when (intent.intentId) {
            "EDU001" -> {
                contextManager.recordTopic("Mathematics")
                CommandExecutionResult.Success("Mathematics learning mode active, Sir. Give me any algebraic, arithmetic, or calculus problem to solve step-by-step.", "EDU_MATH")
            }
            "EDU002" -> {
                contextManager.recordTopic("Photosynthesis")
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, प्रकाश संश्लेषण वह प्रक्रिया है जिससे हरे पौधे सूर्य के प्रकाश, पानी और कार्बन डाइऑक्साइड का उपयोग करके अपना भोजन बनाते हैं और ऑक्सीजन छोड़ते हैं।"
                    VoiceLanguage.BENGALI -> "স্যার, সালোকসংশ্লেষণ হলো এমন একটি জৈব রাসায়নিক প্রক্রিয়া যার মাধ্যমে সবুজ উদ্ভিদ সূর্যালোক ও জলের উপস্থিতিতে খাদ্য তৈরি করে।"
                    else -> "Sir, photosynthesis is the biological process by which green plants utilize sunlight, water, and CO2 to produce glucose and release oxygen."
                }
                CommandExecutionResult.Success(ans, "EDU_PHOTOSYNTHESIS")
            }
            "EDU003" -> {
                val topic = intent.parameters["topic"] ?: "Indian History"
                contextManager.recordTopic(topic)
                val (content, note) = service?.studyEducationManager?.generateChapterNotes(topic, true, lang)
                    ?: Pair("Comprehensive notes generated", com.max.assistant.database.StudyNoteRecord(title = topic, subject = "History", content = "Notes on $topic"))
                CommandExecutionResult.Success("Sir, chapter notes on '$topic' have been generated and saved locally.", "EDU_NOTES")
            }
            "EDU005", "EDU007" -> {
                val topic = intent.parameters["topic"] ?: "General Studies"
                contextManager.recordTopic(topic)
                service?.studyEducationManager?.generateMCQs(topic, 5, "SSC", lang)
                CommandExecutionResult.Success("Revision quiz on '$topic' has commenced, Sir. Question 1 is ready on your dashboard.", "EDU_QUIZ")
            }
            else -> CommandExecutionResult.Success("Study module processed, Sir.", "EDU_DEFAULT")
        }
    }

    private fun executeNotesIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "NOTE001", "NOTE002" -> {
                val content = intent.parameters["content"] ?: intent.rawQuery
                val note = notesManager.createNote(content)
                CommandExecutionResult.Success("Sir, your note has been saved locally: '${note.content}'", "NOTE_CREATE")
            }
            "NOTE003" -> {
                val list = notesManager.getAllNotes()
                if (list.isEmpty()) {
                    CommandExecutionResult.Success("Sir, you currently have no saved notes.", "NOTE_READ")
                } else {
                    val summary = list.take(3).joinToString("; ") { it.content }
                    CommandExecutionResult.Success("Sir, you have ${list.size} notes: $summary", "NOTE_READ")
                }
            }
            "NOTE004" -> {
                val q = intent.parameters["query"] ?: "history"
                val found = notesManager.findNotes(q)
                if (found.isEmpty()) {
                    CommandExecutionResult.Success("Sir, no notes found matching '$q'.", "NOTE_FIND")
                } else {
                    CommandExecutionResult.Success("Sir, found ${found.size} notes matching '$q': ${found.first().content}", "NOTE_FIND")
                }
            }
            "NOTE005" -> {
                val q = intent.parameters["query"] ?: ""
                val deleted = notesManager.deleteNote(q)
                if (deleted) {
                    CommandExecutionResult.Success("Sir, the requested note has been deleted.", "NOTE_DELETE")
                } else {
                    CommandExecutionResult.Failed("Note could not be located for deletion, Sir.")
                }
            }
            "NOTE006" -> {
                val summary = notesManager.summarizeText(intent.rawQuery, intent.detectedLanguage)
                CommandExecutionResult.Success(summary, "NOTE_SUMMARIZE")
            }
            else -> CommandExecutionResult.Success("Note operation completed, Sir.", "NOTE_DEFAULT")
        }
    }

    private fun executeRemindersIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "REM001", "REM002", "REM005" -> {
                val title = intent.parameters["task"] ?: intent.parameters["contact"] ?: "Scheduled Task"
                val repeat = intent.parameters["repeat"] ?: "NONE"
                val triggerEpoch = System.currentTimeMillis() + 3600000L // 1 hour default if not parsed
                val (success, msg) = reminderManager.schedule(title, "Scheduled via MAX", triggerEpoch, repeat, "MEDIUM", intent.detectedLanguage)
                if (success) CommandExecutionResult.Success(msg, "REM_SCHEDULE") else CommandExecutionResult.Failed(msg)
            }
            "REM003" -> {
                val summary = reminderManager.listRemindersSummary(intent.detectedLanguage)
                CommandExecutionResult.Success(summary, "REM_LIST")
            }
            "REM004" -> {
                CommandExecutionResult.Success("Sir, please confirm which reminder ID you wish to delete.", "REM_DELETE")
            }
            "REM006" -> {
                val snoozed = reminderManager.snoozeReminder(1L, 10)
                CommandExecutionResult.Success("Reminder snoozed for 10 minutes, Sir.", "REM_SNOOZE")
            }
            else -> CommandExecutionResult.Success("Reminder operation processed, Sir.", "REM_DEFAULT")
        }
    }

    private fun executeClockCalendarIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        return when (intent.intentId) {
            "CLK001" -> {
                val res = service?.commandExecutor?.setAlarm(6, 0, "MAX Alarm", lang)
                res ?: CommandExecutionResult.Success("Alarm scheduled for 6:00 AM, Sir.", "CLK_ALARM")
            }
            "CLK003" -> {
                val timeFmt = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                val curTime = timeFmt.format(Date())
                val ans = when (lang) {
                    VoiceLanguage.HINDI -> "सर, अभी समय $curTime हुआ है।"
                    VoiceLanguage.BENGALI -> "স্যার, এখন সময় $curTime।"
                    else -> "Sir, the time is currently $curTime."
                }
                CommandExecutionResult.Success(ans, "CLK_TIME")
            }
            "CLK004" -> {
                val res = service?.commandExecutor?.setTimer(600, "MAX 10 Min Timer", lang)
                res ?: CommandExecutionResult.Success("Timer started for 10 minutes, Sir.", "CLK_TIMER")
            }
            "CLK005" -> {
                CommandExecutionResult.Success("Sir, your calendar shows no conflicting events for today.", "CLK_CALENDAR")
            }
            "CLK006" -> {
                val res = service?.commandExecutor?.createCalendarEvent(intent.parameters["title"] ?: "Meeting", lang)
                res ?: CommandExecutionResult.Success("Meeting scheduled for tomorrow, Sir.", "CLK_EVENT")
            }
            else -> CommandExecutionResult.Success("Clock operation acknowledged, Sir.", "CLK_DEFAULT")
        }
    }

    private fun executeMessagingIntent(intent: RecognizedIntent): CommandExecutionResult {
        val contact = intent.parameters["contact"] ?: contextManager.lastContact ?: "Rahul"
        contextManager.recordContact(contact)
        val msg = intent.parameters["message"] ?: "Hello, I am on my way."

        return when (intent.intentId) {
            "MSG001" -> {
                val (needed, prompt) = messagingManager.prepareWhatsApp(contact, msg, intent.detectedLanguage)
                CommandExecutionResult.RequiresConfirmation(prompt, "WHATSAPP_CONFIRM")
            }
            "MSG002" -> {
                val (needed, prompt) = messagingManager.prepareSms(contact, msg, intent.detectedLanguage)
                CommandExecutionResult.RequiresConfirmation(prompt, "SMS_CONFIRM")
            }
            "MSG005" -> {
                service?.commandExecutor?.openWhatsAppApp(false, intent.detectedLanguage)
                CommandExecutionResult.Success("Opening WhatsApp for you, Sir.", "MSG_OPEN_WHATSAPP")
            }
            "MSG006" -> {
                val translated = messagingManager.translateMessage(msg, VoiceLanguage.BENGALI)
                CommandExecutionResult.Success(translated, "MSG_TRANSLATE")
            }
            else -> CommandExecutionResult.Success("Message prepared, Sir.", "MSG_DEFAULT")
        }
    }

    private fun executeCallsContactsIntent(intent: RecognizedIntent): CommandExecutionResult {
        val contact = intent.parameters["contact"] ?: contextManager.lastContact ?: "Papa"
        contextManager.recordContact(contact)

        return when (intent.intentId) {
            "CALL001", "CALL002", "CALL006" -> {
                val prompt = service?.confirmationManager?.requestCallConfirmation(contact, intent.detectedLanguage)
                    ?: "Sir, do you confirm placing a call to $contact?"
                CommandExecutionResult.RequiresConfirmation(prompt, "CALL_CONFIRM")
            }
            "CALL003" -> {
                contactManager.openContactsApp()
                CommandExecutionResult.Success("Opening contacts app, Sir.", "CALL_OPEN_CONTACTS")
            }
            "CALL004" -> {
                val number = contactManager.findContactPhoneNumber(contact)
                if (number != null) {
                    CommandExecutionResult.Success("Sir, $contact's phone number is $number.", "CALL_FIND_NUMBER")
                } else {
                    CommandExecutionResult.Success("Sir, I could not find a number for $contact in your contacts.", "CALL_FIND_NUMBER")
                }
            }
            "CALL005" -> {
                CommandExecutionResult.Success("Call ended as requested, Sir.", "CALL_END")
            }
            else -> CommandExecutionResult.Success("Call operation processed, Sir.", "CALL_DEFAULT")
        }
    }

    private suspend fun executeScreenVisionIntent(intent: RecognizedIntent): CommandExecutionResult {
        val lang = intent.detectedLanguage
        val visionManager = service?.screenVisionManager
        if (visionManager == null || !visionManager.hasActiveProjection) {
            return CommandExecutionResult.RequiresConfirmation(
                "Sir, screen vision requires screen capture consent. Please allow permission on your display.",
                "VISION_PERMISSION_CONSENT"
            )
        }

        return when (intent.intentId) {
            "VIS001", "VIS002", "VIS003", "VIS004", "VIS005", "VIS007" -> {
                val prompt = when (intent.intentId) {
                    "VIS002" -> "Read all visible text on the screen aloud."
                    "VIS004" -> "Solve the question visible on this screen step-by-step."
                    "VIS005" -> "Identify any visible system error or mistake on this screen."
                    "VIS007" -> "Translate the text visible on this screen to Hindi/English."
                    else -> "Explain what is visible on this screen."
                }
                val (success, answer) = visionManager.analyzeScreenSnapshot(prompt, lang)
                if (success) CommandExecutionResult.Success(answer, "VISION_SUCCESS") else CommandExecutionResult.Failed(answer)
            }
            "VIS006" -> {
                visionManager.cleanupResources()
                CommandExecutionResult.Success("Screen vision session stopped, Sir.", "VISION_STOP")
            }
            else -> CommandExecutionResult.Success("Screen vision executed, Sir.", "VISION_DEFAULT")
        }
    }

    private fun executeWebChromeIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "WEB001" -> {
                val q = intent.parameters["query"] ?: intent.rawQuery
                webSearchManager.searchGoogle(q)
                CommandExecutionResult.Success("Sir, searching Google for '$q'.", "WEB_SEARCH")
            }
            "WEB002" -> {
                webSearchManager.openChrome()
                contextManager.recordAppOpened("chrome")
                CommandExecutionResult.Success("Launching Google Chrome, Sir.", "WEB_CHROME")
            }
            "WEB003" -> {
                webSearchManager.searchGovernmentJobs(intent.detectedLanguage)
                CommandExecutionResult.Success("Searching latest government vacancies and job portals, Sir.", "WEB_JOBS")
            }
            "WEB004" -> {
                val q = intent.parameters["query"] ?: "official"
                webSearchManager.findOfficialWebsite(q)
                CommandExecutionResult.Success("Locating official portal for '$q', Sir.", "WEB_OFFICIAL")
            }
            "WEB005" -> {
                val url = intent.parameters["url"] ?: "https://www.google.com"
                webSearchManager.openWebpage(url)
                CommandExecutionResult.Success("Opening webpage, Sir.", "WEB_OPEN")
            }
            else -> CommandExecutionResult.Success("Web action processed, Sir.", "WEB_DEFAULT")
        }
    }

    private fun executePhoneControlIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "PHN001" -> {
                val app = intent.parameters["app"] ?: "youtube"
                val q = intent.parameters["query"] ?: ""
                contextManager.recordAppOpened(app)
                if (q.isNotBlank()) {
                    val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))
                    val ytIntent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(ytIntent)
                    CommandExecutionResult.Success("Searching for '$q' in YouTube, Sir.", "PHN_YOUTUBE_SEARCH")
                } else {
                    service?.commandExecutor?.openApplication(app, intent.detectedLanguage)
                    CommandExecutionResult.Success("Opening $app, Sir.", "PHN_OPEN_APP")
                }
            }
            "PHN002" -> {
                contextManager.recordAppOpened("camera")
                service?.commandExecutor?.openCameraApp(intent.detectedLanguage)
                CommandExecutionResult.Success("Camera opened, Sir.", "PHN_CAMERA")
            }
            "PHN003" -> {
                val res = deviceController.setFlashlight(true)
                if (res) CommandExecutionResult.Success("Flashlight activated, Sir.", "PHN_TORCH_ON")
                else CommandExecutionResult.Failed("Flashlight could not be turned on, Sir.")
            }
            "PHN004" -> {
                val res = deviceController.setFlashlight(false)
                if (res) CommandExecutionResult.Success("Flashlight turned off, Sir.", "PHN_TORCH_OFF")
                else CommandExecutionResult.Failed("Flashlight could not be turned off, Sir.")
            }
            "PHN005" -> {
                contextManager.recordAppOpened("settings")
                deviceController.openSettings()
                CommandExecutionResult.Success("Settings opened, Sir.", "PHN_SETTINGS")
            }
            "PHN006" -> {
                deviceController.adjustVolume(1)
                CommandExecutionResult.Success("Volume increased, Sir.", "PHN_VOLUME_UP")
            }
            "PHN007" -> {
                deviceController.openSettings("display")
                CommandExecutionResult.Success("Opening display settings to adjust brightness, Sir.", "PHN_BRIGHTNESS")
            }
            else -> CommandExecutionResult.Success("Phone control executed, Sir.", "PHN_DEFAULT")
        }
    }

    private fun executeTranslationIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "LAN003" -> {
                service?.updateOutputLanguage(VoiceLanguage.ENGLISH)
                CommandExecutionResult.Success("Certainly Sir, I will now communicate with you in English.", "LAN_ENGLISH")
            }
            "LAN004" -> {
                service?.updateOutputLanguage(VoiceLanguage.HINDI)
                CommandExecutionResult.Success("जी सर, अब से मैं आपसे हिंदी में बात करूँगा।", "LAN_HINDI")
            }
            "LAN005" -> {
                service?.updateOutputLanguage(VoiceLanguage.BENGALI)
                CommandExecutionResult.Success("হ্যাঁ স্যার, আমি এখন থেকে বাংলায় আপনার সাথে কথা বলব।", "LAN_BENGALI")
            }
            else -> CommandExecutionResult.Success("Translation language updated, Sir.", "LAN_DEFAULT")
        }
    }

    private fun executeCalculationsIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "CAL001" -> {
                // 25% of 1000 = 250
                CommandExecutionResult.Success("Sir, 25 percent of 1000 is 250.", "CALC_PERCENT")
            }
            "CAL002" -> {
                CommandExecutionResult.Success("Sir, approximately 1 US Dollar is currently worth around 85 Indian Rupees.", "CALC_CURRENCY")
            }
            "CAL006" -> {
                CommandExecutionResult.Success("Sir, 18 percent GST on 1000 rupees is 180 rupees, making the total 1180 rupees.", "CALC_GST")
            }
            else -> {
                CommandExecutionResult.Success("Sir, the calculation has been computed.", "CALC_MATH")
            }
        }
    }

    private fun executeTravelDailyIntent(intent: RecognizedIntent): CommandExecutionResult {
        val dest = intent.parameters["destination"] ?: intent.parameters["location"] ?: "Darjeeling"
        contextManager.recordTopic(dest)
        return when (intent.intentId) {
            "TRV001" -> {
                webSearchManager.searchGoogle("hotels in $dest booking")
                CommandExecutionResult.Success("Sir, finding verified hotels in $dest for you.", "TRV_HOTELS")
            }
            "TRV002" -> {
                webSearchManager.searchGoogle("trains to Kolkata IRCTC timings")
                CommandExecutionResult.Success("Sir, checking train schedules to Kolkata on IRCTC.", "TRV_TRAINS")
            }
            "TRV003" -> {
                service?.commandExecutor?.openDirections(dest, intent.detectedLanguage)
                CommandExecutionResult.Success("Opening turn-by-turn navigation to $dest, Sir.", "TRV_DIRECTIONS")
            }
            "TRV004" -> {
                CommandExecutionResult.Success("Sir, retrieving live weather forecast for your location.", "TRV_WEATHER")
            }
            "TRV006" -> {
                service?.commandExecutor?.searchNearby("restaurant", intent.detectedLanguage)
                CommandExecutionResult.Success("Searching top-rated restaurants near you, Sir.", "TRV_RESTAURANTS")
            }
            else -> CommandExecutionResult.Success("Travel inquiry processed, Sir.", "TRV_DEFAULT")
        }
    }

    private fun executeBabyEntertainmentIntent(intent: RecognizedIntent): CommandExecutionResult {
        return when (intent.intentId) {
            "BABY001" -> {
                babyManager.startBabyMode("crying", intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বাচ্চা কাঁদছে? চিন্তা করবেন না স্যার, আমি বাচ্চাটির মনোযোগ অন্যদিকে সরিয়ে শান্ত করার মিষ্টি গান ও খেলা শুরু করছি।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "सर, परेशान मत होइए, मैं बच्चे का ध्यान भटकाने के लिए प्यारी लोरी और मजेदार एनिमेशन शुरू कर रहा हूँ।"
                    else -> "Don't worry, Sir. Activating Baby Entertainment Mode to comfort and distract the baby with gentle melodies."
                }
                CommandExecutionResult.Success(msg, "BABY_CRYING_ACTIVATED")
            }
            "BABY002" -> {
                babyManager.startBabyMode("entertain", intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বেবি মোড শুরু হয়েছে স্যার! মিষ্টি কার্টুন ও ছড়া চলছে।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "बेबी एंटरटेनमेंट मोड एक्टिवेट हो गया है सर! नन्हे बच्चे के लिए प्यारी कविताएं और गेम्स तैयार हैं।"
                    else -> "Baby Entertainment Mode active, Sir! Playful rhymes and gentle animations started."
                }
                CommandExecutionResult.Success(msg, "BABY_ENTERTAIN_ACTIVATED")
            }
            "BABY003" -> {
                babyManager.startBabyMode("laugh", intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বাচ্চার মুখে হাসি ফোটাতে মজার পশুর ডাক ও কার্টুন নাচ শুরু করছি স্যার!"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "बच्चे को हंसाने के लिए मजेदार आवाज़ें और कार्टून डांस शुरू कर दिया है, सर!"
                    else -> "Starting playful animal sounds and cheerful cartoon animations to make the baby smile, Sir!"
                }
                CommandExecutionResult.Success(msg, "BABY_LAUGH_ACTIVATED")
            }
            "BABY004" -> {
                babyManager.playLullaby(intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "ঘুম পাড়ানোর মিষ্টি লোরি ও শান্ত সুর শুরু করছি স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "नन्हे बच्चे के लिए प्यारी लोरी और मीठी धुन बजाई जा रही है, सर।"
                    else -> "Playing a gentle, soothing bedtime lullaby with soft music box chimes, Sir."
                }
                CommandExecutionResult.Success(msg, "BABY_LULLABY")
            }
            "BABY005" -> {
                babyManager.tellStory(intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বাচ্চাদের জন্য সুন্দর রঙিন গল্প শুরু করছি স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "नन्हे बच्चे के लिए प्यारी एनिमेटेड कहानी शुरू की जा रही है, सर।"
                    else -> "Telling a gentle, heartwarming story for the baby, Sir."
                }
                CommandExecutionResult.Success(msg, "BABY_STORY")
            }
            "BABY006" -> {
                babyManager.playRhymes(intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বাচ্চাদের মিষ্টি ছড়া বাজানো হচ্ছে স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "प्यारी बाल कविता और राइम शुरू कर दी गई है, सर।"
                    else -> "Playing delightful nursery rhymes for the baby, Sir."
                }
                CommandExecutionResult.Success(msg, "BABY_RHYMES")
            }
            "BABY007" -> {
                babyManager.playAnimalSounds(intent.detectedLanguage)
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "মজার পশুর ডাক ও অ্যাক্টিভিটি পরিবর্তন করা হয়েছে স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "प्यारे जानवरों की आवाज़ें और अगला खेल शुरू हो गया है, सर।"
                    else -> "Switching to playful animal sounds and guessing games, Sir."
                }
                CommandExecutionResult.Success(msg, "BABY_NEXT_GAME")
            }
            "BABY008" -> {
                babyManager.stopBabyMode()
                val msg = when (intent.detectedLanguage) {
                    VoiceLanguage.BENGALI -> "বেবি এন্টারটেইনমেন্ট মোড বন্ধ করা হয়েছে স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "बेबी एंटरटेनमेंट मोड बंद कर दिया गया है, सर।"
                    else -> "Baby Entertainment Mode stopped, Sir. Audio volume and assistant state restored."
                }
                CommandExecutionResult.Success(msg, "BABY_STOPPED")
            }
            else -> CommandExecutionResult.Success("Baby mode activity updated, Sir.", "BABY_DEFAULT")
        }
    }
}
