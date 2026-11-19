package com.max.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.max.assistant.MainActivity
import com.max.assistant.R
import com.max.assistant.ai.BrainDecision
import com.max.assistant.ai.MaxAiBrain
import com.max.assistant.launcher.AppLauncher
import com.max.assistant.launcher.LaunchResult
import com.max.assistant.planner.MultiStepPlan
import com.max.assistant.planner.TaskPlanner
import com.max.assistant.protection.PaymentProtectionManager
import com.max.assistant.hud.ActivityCategory
import com.max.assistant.hud.ActivityEvent
import com.max.assistant.hud.ActiveTaskState
import com.max.assistant.hud.EventStatus
import com.max.assistant.hud.ScreenVisionUiState
import com.max.assistant.hud.SystemTelemetryInfo
import com.max.assistant.speech.MaxSpeechManager
import com.max.assistant.telephony.PhoneActionResult
import com.max.assistant.telephony.PhoneCommunicationManager
import com.max.assistant.tts.MaxVoiceEngine
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.tts.VoiceSettings
import com.max.assistant.updater.AutoUpdateManager
import com.max.assistant.updater.UpdateStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SystemDiagnosticInfo(
    val aiConnectionStatus: String,
    val micPermissionGranted: Boolean,
    val speechRecognitionAvailable: Boolean,
    val ttsEngineStatus: String,
    val selectedLanguage: String,
    val accessibilityEnabled: Boolean,
    val commandExecutorStatus: String,
    val batteryOptimizationStatus: String = "Unrestricted"
)

class MaxForegroundService : Service() {

    companion object {
        private const val TAG = "MaxForegroundService"
        const val CHANNEL_ID = "max_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_MUTE_TOGGLE = "ACTION_MUTE_TOGGLE"
        const val ACTION_STOP_MAX = "ACTION_STOP_MAX"
        const val ACTION_EXECUTE_COMMAND = "ACTION_EXECUTE_COMMAND"
        const val EXTRA_COMMAND = "EXTRA_COMMAND"

        var activeInstance: MaxForegroundService? = null
            private set

        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, MaxForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MaxForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MaxForegroundService = this@MaxForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var voiceEngine: MaxVoiceEngine
        private set
    lateinit var speechManager: MaxSpeechManager
        private set
    lateinit var appLauncher: AppLauncher
        private set
    lateinit var phoneManager: PhoneCommunicationManager
        private set
    lateinit var updateManager: AutoUpdateManager
        private set
    lateinit var systemStatusManager: com.max.assistant.util.SystemStatusManager
        private set
    lateinit var commandExecutor: com.max.assistant.executor.AndroidCommandExecutor
        private set
    lateinit var conversationRepo: com.max.assistant.ai.ConversationRepository
        private set
    val confirmationManager = com.max.assistant.action.ActionConfirmationManager()
    val automationHub = com.max.assistant.automation.AutomationHub()
    val taskPlanner = TaskPlanner()
    lateinit var aiBrain: MaxAiBrain
        private set
    lateinit var screenVisionManager: com.max.assistant.vision.ScreenVisionManager
        private set
    lateinit var studyEducationManager: com.max.assistant.education.StudyEducationManager
        private set
    lateinit var reminderScheduler: com.max.assistant.database.ReminderScheduler
        private set
    lateinit var accessibilityController: com.max.assistant.accessibility.AndroidAccessibilityController
        private set
    lateinit var appInteractionManager: com.max.assistant.appcontrol.AppInteractionManager
        private set
    lateinit var taskExecutionEngine: com.max.assistant.planner.TaskExecutionEngine
        private set
    lateinit var serviceController: com.max.assistant.service.MaxServiceController
        private set
    lateinit var masterCommandExecutor: com.max.assistant.dictionary.MasterCommandExecutor
        private set
    lateinit var babyEntertainmentManager: com.max.assistant.baby.BabyEntertainmentManager
        private set
    lateinit var contextMemoryManager: com.max.assistant.memory.ContextMemoryManager
        private set
    lateinit var healthMonitor: com.max.assistant.healing.MaxHealthMonitor
        private set
    lateinit var notificationManager: com.max.assistant.notification.IntelligentNotificationManager
        private set
    lateinit var researchEngine: com.max.assistant.research.LiveInternetResearchEngine
        private set
    lateinit var performanceAnalytics: com.max.assistant.analytics.PerformanceAnalyticsEngine
        private set

    fun isBabyEntertainmentManagerInitialized(): Boolean = ::babyEntertainmentManager.isInitialized
    val intentRecognizer = com.max.assistant.dictionary.MultilingualIntentRecognizer()

    var pendingScreenPrompt: String? = null
    var pendingScreenLang: VoiceLanguage = VoiceLanguage.HINGLISH

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _activityEvents = MutableStateFlow<List<ActivityEvent>>(emptyList())
    val activityEvents: StateFlow<List<ActivityEvent>> = _activityEvents.asStateFlow()

    private val _activeTaskState = MutableStateFlow<ActiveTaskState?>(null)
    val activeTaskState: StateFlow<ActiveTaskState?> = _activeTaskState.asStateFlow()

    private val _systemTelemetry = MutableStateFlow(SystemTelemetryInfo())
    val systemTelemetry: StateFlow<SystemTelemetryInfo> = _systemTelemetry.asStateFlow()

    val screenVisionUiState: StateFlow<ScreenVisionUiState>
        get() = if (::screenVisionManager.isInitialized) screenVisionManager.screenVisionUiState else MutableStateFlow(ScreenVisionUiState()).asStateFlow()

    lateinit var workspaceManager: com.max.assistant.workspace.MaxWorkspaceManager
        private set

    val workspaceUiState: StateFlow<com.max.assistant.workspace.WorkspaceUiState>
        get() = if (::workspaceManager.isInitialized) workspaceManager.workspaceState else MutableStateFlow(com.max.assistant.workspace.WorkspaceUiState()).asStateFlow()

    lateinit var ecosystemClient: com.max.assistant.ecosystem.MaxEcosystemClient
        private set

    fun publishActivityEvent(
        title: String,
        detail: String = "",
        category: ActivityCategory = ActivityCategory.SYSTEM,
        status: EventStatus = EventStatus.SUCCESS
    ) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val event = ActivityEvent(
            timestamp = time,
            title = title,
            detail = detail,
            category = category,
            status = status
        )
        val current = _activityEvents.value.toMutableList()
        current.add(0, event)
        if (current.size > 80) current.removeAt(current.size - 1)
        _activityEvents.value = current
        addLog("[$category] $title ${if (detail.isNotBlank()) "— $detail" else ""}")
    }

    fun updateActiveTask(
        name: String,
        command: String,
        reasoning: String,
        targetApp: String? = null,
        step: String,
        progress: Int? = null,
        tool: String? = null,
        isCancellable: Boolean = true
    ) {
        _activeTaskState.value = ActiveTaskState(
            taskName = name,
            recognizedCommand = command,
            reasoningSummary = reasoning,
            targetApp = targetApp,
            currentStep = step,
            progressPercent = progress,
            toolUsed = tool,
            isCancellable = isCancellable
        )
    }

    fun clearActiveTask() {
        _activeTaskState.value = null
    }

    fun cancelCurrentTask() {
        val active = _activeTaskState.value
        if (active != null) {
            publishActivityEvent(
                title = "Task Cancelled by Sir",
                detail = active.taskName,
                category = ActivityCategory.EXECUTION,
                status = EventStatus.WARNING
            )
            clearActiveTask()
            speechManager.setAssistantState(com.max.assistant.speech.AssistantState.IDLE)
            val cancelMsg = when (voiceEngine.settings.value.outputLanguage) {
                VoiceLanguage.HINDI -> "जी सर, टास्क रद्द कर दिया गया है।"
                VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কাজটি বাতিল করা হয়েছে।"
                else -> "Task cancelled as requested, Sir."
            }
            speakWithCoordination(cancelMsg, voiceEngine.settings.value.outputLanguage)
        }
    }

    private fun startTelemetryPolling() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val (batteryLvl, isCharging) = systemStatusManager.getBatteryStatus()
                    val netStatus = systemStatusManager.getNetworkStatus()
                    val isOnline = netStatus != "Disconnected" && netStatus != "Unknown"
                    val isBtOn = try {
                        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                        bm?.adapter?.isEnabled == true
                    } catch (_: Exception) { false }
                    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                    val aiStatus = if (::aiBrain.isInitialized && aiBrain.aiProvider?.isConfigured == true) "Gemini Neural Engine Connected" else "Local Neural Engine"

                    _systemTelemetry.value = SystemTelemetryInfo(
                        batteryPercent = if (batteryLvl >= 0) batteryLvl else 100,
                        isCharging = isCharging,
                        networkType = netStatus,
                        isOnline = isOnline,
                        bluetoothEnabled = isBtOn,
                        micActive = !voiceEngine.isMuted.value && speechManager.assistantState.value != com.max.assistant.speech.AssistantState.MUTED,
                        serviceRunning = true,
                        aiProviderStatus = aiStatus,
                        currentTime = timeStr,
                        appVersion = "v2.3.0"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Telemetry update error: ${e.message}")
                }
                delay(5000L)
            }
        }
    }

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        _serviceRunning.value = true

        createNotificationChannel()
        appLauncher = AppLauncher(this)
        phoneManager = PhoneCommunicationManager(this)
        updateManager = AutoUpdateManager(this)
        systemStatusManager = com.max.assistant.util.SystemStatusManager(this)
        commandExecutor = com.max.assistant.executor.AndroidCommandExecutor(this, appLauncher, phoneManager, systemStatusManager)
        conversationRepo = com.max.assistant.ai.ConversationRepository.getInstance(this)
        accessibilityController = com.max.assistant.accessibility.AndroidAccessibilityController(this)
        appInteractionManager = com.max.assistant.appcontrol.AppInteractionManager(this, accessibilityController, appLauncher)
        taskExecutionEngine = com.max.assistant.planner.TaskExecutionEngine(this, accessibilityController, appInteractionManager, appLauncher)
        screenVisionManager = com.max.assistant.vision.ScreenVisionManager(this, com.max.assistant.ai.GeminiAiProvider(this), accessibilityController)

        val humourEngine = com.max.assistant.humour.HumourEngine(this)
        aiBrain = MaxAiBrain(
            aiProvider = screenVisionManager.aiProvider,
            humourEngine = humourEngine,
            screenContextManager = screenVisionManager.screenContextManager
        )

        studyEducationManager = com.max.assistant.education.StudyEducationManager(this, aiBrain.aiProvider)
        reminderScheduler = com.max.assistant.database.ReminderScheduler(this)
        serviceController = com.max.assistant.service.MaxServiceController(this, this, screenVisionManager)

        contextMemoryManager = com.max.assistant.memory.ContextMemoryManager(this)
        healthMonitor = com.max.assistant.healing.MaxHealthMonitor.getInstance(this)
        notificationManager = com.max.assistant.notification.IntelligentNotificationManager.getInstance(this)
        researchEngine = com.max.assistant.research.LiveInternetResearchEngine(this)
        performanceAnalytics = com.max.assistant.analytics.PerformanceAnalyticsEngine(this)

        voiceEngine = MaxVoiceEngine(this) {
            voiceEngine.setMute(false)
            val welcome = when (voiceEngine.settings.value.outputLanguage) {
                VoiceLanguage.HINDI -> "मैक्स ऑनलाइन है, सर। मैं आपकी सेवा में तैयार हूँ।"
                VoiceLanguage.BENGALI -> "ম্যাক্স অনলাইন আছে, স্যার। আমি আপনার সেবায় প্রস্তুত।"
                else -> "MAX is online and standing by at your service, Sir."
            }
            speakWithCoordination(welcome, voiceEngine.settings.value.outputLanguage)
            addLog("MAX online. Ready for command, Sir.")
        }

        babyEntertainmentManager = com.max.assistant.baby.BabyEntertainmentManager(this, voiceEngine)
        masterCommandExecutor = com.max.assistant.dictionary.MasterCommandExecutor(this, this, customBabyManager = babyEntertainmentManager)

        workspaceManager = com.max.assistant.workspace.MaxWorkspaceManager(
            context = this,
            scope = serviceScope,
            videoSearchManager = com.max.assistant.workspace.MaxVideoSearchManager(this, aiBrain.aiProvider as? com.max.assistant.ai.GeminiAiProvider),
            researchEngine = researchEngine
        )

        ecosystemClient = com.max.assistant.ecosystem.MaxEcosystemClient(
            context = this,
            scope = serviceScope,
            onExecuteRemoteCommand = { action, params ->
                when (action) {
                    "flashlight" -> {
                        commandExecutor.setFlashlight(true, voiceEngine.settings.value.outputLanguage)
                        "Flashlight turned on, Sir."
                    }
                    "screen_vision" -> {
                        serviceScope.launch {
                            screenVisionManager.analyzeScreenSnapshot("What is on screen?", voiceEngine.settings.value.outputLanguage)
                        }
                        "Screen vision initiated, Sir."
                    }
                    "open_app" -> {
                        val app = params["appName"] ?: "Chrome"
                        commandExecutor.openApplication(app, voiceEngine.settings.value.outputLanguage)
                        "Opening $app on Android, Sir."
                    }
                    else -> "Action '$action' processed on Android, Sir."
                }
            }
        )

        speechManager = MaxSpeechManager(
            context = this,
            onCommandReceived = { command -> handleIncomingCommand(command) },
            onWakeWordDetected = {
                addLog("Wake word detected! Listening, Sir.")
                publishActivityEvent("Wake Word Detected", "Awakened by voice trigger, Sir", ActivityCategory.VOICE)
                publishActivityEvent("Listening to Sir", "Microphone actively recording command", ActivityCategory.VOICE)
                val currentLang = voiceEngine.settings.value.outputLanguage
                val ack = when (currentLang) {
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার?"
                    VoiceLanguage.ENGLISH -> "Yes, Sir?"
                    else -> "जी सर?"
                }
                speakWithCoordination(ack, currentLang, listenForFollowUp = true)
            },
            onPinRequested = {
                _showPinDialog.value = true
                speakWithCoordination("कृपया सुरक्षा पिन दर्ज करें, सर।", VoiceLanguage.HINDI)
                addLog("PIN entry requested.")
            },
            onStandbyTransition = {
                addLog("Inactivity timeout (15s): MAX is now on Standby. Waiting for wake command.")
                updateNotification("MAX is on Standby (Say 'Hey MAX')")
            }
        ).apply {
            onUnmuteRequested = {
                setMuted(false)
            }
            setMuted(false)
            allowShortWakeWord = voiceEngine.settings.value.allowShortWakeWord
            continuousConversationMode = voiceEngine.settings.value.continuousListening
            muteListeningBeep = true
            enableBargeIn = false
            onBargeInDetected = {
                Log.i(TAG, "Barge-in detected: User interrupted MAX speech.")
                voiceEngine.stop()
                addLog("Interrupted by user. Listening, Sir.")
            }
        }

        serviceScope.launch {
            speechManager.assistantState.collect { state ->
                val desc = when (state) {
                    com.max.assistant.speech.AssistantState.IDLE -> "MAX Ready"
                    com.max.assistant.speech.AssistantState.LISTENING_FOR_WAKE_WORD -> "MAX Standby (Say 'Hey MAX')"
                    com.max.assistant.speech.AssistantState.LISTENING_FOR_COMMAND -> "Listening for command, Sir..."
                    com.max.assistant.speech.AssistantState.THINKING,
                    com.max.assistant.speech.AssistantState.PROCESSING -> "Processing command..."
                    com.max.assistant.speech.AssistantState.SPEAKING -> "Speaking..."
                    com.max.assistant.speech.AssistantState.EXECUTING_COMMAND -> "Executing action..."
                    com.max.assistant.speech.AssistantState.VERIFYING -> "Verifying outcome..."
                    com.max.assistant.speech.AssistantState.RECOVERING -> "Self-healing & recovering..."
                    com.max.assistant.speech.AssistantState.SCREEN_SHARING -> "Screen Vision Active"
                    com.max.assistant.speech.AssistantState.MUTED -> "Microphone Muted"
                    com.max.assistant.speech.AssistantState.STOPPED -> "MAX Stopped (Locked)"
                    com.max.assistant.speech.AssistantState.AWAITING_PIN -> "Awaiting Security PIN 1212..."
                    com.max.assistant.speech.AssistantState.COMPLETED -> "Ready for 'Hey MAX'"
                    com.max.assistant.speech.AssistantState.FAILED -> "Microphone Alert"
                }
                updateNotification(desc)
            }
        }

        startAutoUpdateLoop()
        startTelemetryPolling()
        Log.i(TAG, "MaxForegroundService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fgsType = if (hasMicPerm) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        0
                    }
                    try {
                        androidx.core.app.ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            buildNotification("MAX Voice Assistant is Active"),
                            fgsType
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "ServiceCompat.startForeground with type failed: ${e.message}")
                        try {
                            startForeground(NOTIFICATION_ID, buildNotification("MAX Voice Assistant is Active"))
                        } catch (e2: Exception) {
                            Log.e(TAG, "startForeground fallback failed: ${e2.message}")
                        }
                    }
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification("MAX Voice Assistant is Active"))
                }

                if (hasMicPerm) {
                    speechManager.startListeningLoop()
                    addLog("MAX listening service active for Sir.")
                } else {
                    addLog("Awaiting microphone permission from Sir.")
                }
            }
            ACTION_MUTE_TOGGLE -> {
                toggleMute()
            }
            ACTION_STOP_MAX -> {
                executeStopMax()
            }
            ACTION_EXECUTE_COMMAND -> {
                val cmd = intent?.getStringExtra(EXTRA_COMMAND)
                if (!cmd.isNullOrBlank()) {
                    handleIncomingCommand(cmd)
                }
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }

        return START_STICKY
    }

    fun speakWithCoordination(
        text: String,
        targetLanguage: VoiceLanguage? = null,
        listenForFollowUp: Boolean = false,
        onDone: (() -> Unit)? = null
    ) {
        val langToUse = targetLanguage ?: voiceEngine.settings.value.outputLanguage
        speechManager.notifyAssistantSpeakingStarted(text)
        voiceEngine.speak(
            text = text,
            targetLanguage = langToUse,
            onSpeechStarted = {
                speechManager.notifyAssistantSpeakingStarted(text)
            },
            onSpeechFinished = {
                speechManager.resumeAfterSpeaking(listenForFollowUp)
                onDone?.invoke()
            }
        )
    }

    fun handleIncomingCommand(command: String) {
        addLog("Received Command: $command")
        publishActivityEvent("Voice Command Recognized", command, ActivityCategory.VOICE)
        publishActivityEvent("Understanding Request", "Formulating optimal execution plan", ActivityCategory.REASONING)
        updateActiveTask("Executing Command", command, "Analyzing intent and executing verified action", null, "Evaluating command", 25, "Neural Brain")
        val currentOutputLang = voiceEngine.settings.value.outputLanguage
        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.USER, command)

        // 1. Check if user is responding to an active confirmation request or interactive prompt
        val activePending = confirmationManager.pendingAction.value
        val cleanCommand = command.replace(
            Regex("^(?i)(hey\\s+max|ok\\s+max|hello\\s+max|max|hey\\s+jarvis|ok\\s+jarvis|hello\\s+jarvis|jarvis|मैक्स|जार्विस|ম্যাক্স|জার্ভিস|হেই\\s+ম্যাক্স|হে\\s+ম্যাক্স|হে\\s+जार्विस|হেই\\s+জার্ভিস)[,\\s:]*"),
            ""
        ).trim('\'', '"', '?', '!', '.', ',', ' ', ':', ';', '-')
        val textToUse = if (cleanCommand.isNotEmpty()) cleanCommand else command
        Log.i(TAG, "handleIncomingCommand: activePending=$activePending, textToUse='$textToUse'")

        if (activePending != null) {
            if (confirmationManager.isConfirmationNegative(textToUse)) {
                confirmationManager.cancel()
                val cancelMsg = when (currentOutputLang) {
                    VoiceLanguage.HINDI -> "जी सर, कार्रवाई रद्द कर दी गई है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কাজটি বাতিল করা হয়েছে।"
                    else -> "Action cancelled as requested, Sir."
                }
                speakWithCoordination(cancelMsg, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg)
                addLog("Action Cancelled by Sir.")
                return
            }

            when (activePending) {
                is com.max.assistant.action.PendingAction.AwaitingReplyRecipient -> {
                    val recipient = textToUse.removePrefix("to ").removePrefix("to").removeSuffix(" ko").removeSuffix("কে").trim()
                    confirmationManager.requestReplyContent(recipient, activePending.isWhatsApp)
                    val prompt = when (currentOutputLang) {
                        VoiceLanguage.HINDI -> "जी सर, $recipient को क्या उत्तर भेजना है?"
                        VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, $recipient কে কি উত্তর পাঠাতে হবে?"
                        else -> "Sir, what reply should I send to $recipient?"
                    }
                    speechManager.setAwaitingCommand(true)
                    speakWithCoordination(prompt, currentOutputLang, listenForFollowUp = true)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, prompt, currentOutputLang)
                    addLog("Recipient set to '$recipient', awaiting message content.")
                    return
                }
                is com.max.assistant.action.PendingAction.AwaitingReplyContent -> {
                    val recipient = activePending.recipient
                    val isWhatsApp = activePending.isWhatsApp
                    confirmationManager.cancel()
                    val result = if (isWhatsApp) {
                        phoneManager.sendWhatsApp(recipient, textToUse)
                    } else {
                        phoneManager.sendMessage(recipient, textToUse)
                    }
                    speakWithCoordination(result.message, currentOutputLang)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message, currentOutputLang)
                    addLog("Sent reply to $recipient: $textToUse")
                    return
                }
                is com.max.assistant.action.PendingAction.AwaitingCallRecipient -> {
                    val target = textToUse.removePrefix("to ").removePrefix("to").removeSuffix(" ko").removeSuffix("के").trim()
                    confirmationManager.cancel()
                    val result = phoneManager.makeCall(target)
                    speakWithCoordination(result.message, currentOutputLang)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message)
                    addLog("Placed call to $target")
                    return
                }
                is com.max.assistant.action.PendingAction.MakeCall -> {
                    if (confirmationManager.isConfirmationAffirmative(textToUse)) {
                        val confirmedAction = confirmationManager.confirm() as? com.max.assistant.action.PendingAction.MakeCall
                        if (confirmedAction != null) {
                            val result = phoneManager.makeCall(confirmedAction.target)
                            speakWithCoordination(result.message, currentOutputLang)
                            conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message)
                            addLog("Confirmed Call to ${confirmedAction.target}")
                        }
                        return
                    } else if (confirmationManager.isConfirmationNegative(textToUse)) {
                        confirmationManager.cancel()
                        val cancelMsg = when (currentOutputLang) {
                            VoiceLanguage.HINDI -> "जी सर, कॉल रद्द कर दिया गया है।"
                            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কল বাতিল করা হয়েছে।"
                            else -> "Call cancelled, Sir."
                        }
                        speakWithCoordination(cancelMsg, currentOutputLang)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg, currentOutputLang)
                        addLog("Call cancelled by user.")
                        return
                    }
                }
                is com.max.assistant.action.PendingAction.SendMessage -> {
                    if (confirmationManager.isConfirmationAffirmative(textToUse)) {
                        val confirmedAction = confirmationManager.confirm() as? com.max.assistant.action.PendingAction.SendMessage
                        if (confirmedAction != null) {
                            val result = if (confirmedAction.isWhatsApp) {
                                phoneManager.sendWhatsApp(confirmedAction.recipient, confirmedAction.message)
                            } else {
                                phoneManager.sendMessage(confirmedAction.recipient, confirmedAction.message)
                            }
                            speakWithCoordination(result.message, currentOutputLang)
                            conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message)
                            addLog("Confirmed Message to ${confirmedAction.recipient}")
                        }
                        return
                    } else if (confirmationManager.isConfirmationNegative(textToUse)) {
                        confirmationManager.cancel()
                        val cancelMsg = when (currentOutputLang) {
                            VoiceLanguage.HINDI -> "जी सर, संदेश रद्द कर दिया गया है।"
                            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, বার্তা বাতিল করা হয়েছে।"
                            else -> "Message cancelled, Sir."
                        }
                        speakWithCoordination(cancelMsg, currentOutputLang)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg, currentOutputLang)
                        addLog("Message cancelled by user.")
                        return
                    }
                }
                is com.max.assistant.action.PendingAction.AwaitingMusicConfirmation -> {
                    if (confirmationManager.isConfirmationAffirmative(textToUse)) {
                        val query = activePending.query
                        confirmationManager.cancel()
                        val result = commandExecutor.playMusic(query, "spotify", currentOutputLang)
                        handleCommandExecutionResult(result, currentOutputLang)
                        addLog("User accepted music offer. Playing: $query")
                        return
                    } else if (confirmationManager.isConfirmationNegative(textToUse)) {
                        confirmationManager.cancel()
                        val cancelMsg = when (currentOutputLang) {
                            VoiceLanguage.HINDI -> "कोई बात नहीं सर, जब भी आपको मेरी ज़रूरत हो, मैं यहीं हूँ।"
                            VoiceLanguage.BENGALI -> "কোনো ব্যাপার নয় স্যার, আমি সবসময় আপনার সাথে আছি।"
                            else -> "Understood, Sir. Whenever you need anything, I am right here."
                        }
                        speakWithCoordination(cancelMsg, currentOutputLang)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg, currentOutputLang)
                        addLog("Music offer declined by user.")
                        return
                    }
                }
                else -> {}
            }
        }

        // Check if user wants to cancel an active multi-step plan
        val activePlan = taskPlanner.activePlan.value
        if (activePlan != null && !activePlan.isCompleted) {
            val isPlanCancel = textToUse.contains("cancel", ignoreCase = true) ||
                    textToUse.contains("रद्द", ignoreCase = true) ||
                    textToUse.contains("বাতিল", ignoreCase = true) ||
                    textToUse.equals("stop", ignoreCase = true) ||
                    textToUse.equals("रुको", ignoreCase = true)
            if (isPlanCancel) {
                taskPlanner.cancelActivePlan("User cancelled")
                val cancelMsg = when (currentOutputLang) {
                    VoiceLanguage.HINDI -> "जी सर, कार्य योजना रद्द कर दी गई है।"
                    VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, কাজের পরিকল্পনা বাতিল করা হয়েছে।"
                    else -> "Multi-step plan cancelled as requested, Sir."
                }
                speakWithCoordination(cancelMsg, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg)
                addLog("Task Plan cancelled by Sir.")
                return
            }
        }

        // Instant Stop for Baby Entertainment Mode
        if (::babyEntertainmentManager.isInitialized && babyEntertainmentManager.state.value.isActive) {
            val lowerCheck = textToUse.lowercase(Locale.ROOT)
            val isStopBaby = lowerCheck == "stop" || lowerCheck == "band karo" || lowerCheck == "বন্ধ করো" ||
                    lowerCheck.contains("stop baby mode") || lowerCheck.contains("baby mode band") ||
                    lowerCheck.contains("বেবি মোড বন্ধ") || lowerCheck == "stop baby" || lowerCheck == "रुको"
            if (isStopBaby) {
                babyEntertainmentManager.stopBabyMode()
                val stopMsg = when (currentOutputLang) {
                    VoiceLanguage.BENGALI -> "বেবি মোড বন্ধ করা হয়েছে স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "बेबी एंटरटेनमेंट मोड बंद कर दिया गया है, सर।"
                    else -> "Baby Entertainment Mode stopped immediately, Sir."
                }
                speakWithCoordination(stopMsg, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, stopMsg, currentOutputLang)
                addLog("Baby Entertainment Mode stopped by user command: '$textToUse'")
                return
            }
        }

        // Holographic Workspace Voice Commands
        if (::workspaceManager.isInitialized) {
            val wsReply = workspaceManager.handleWorkspaceVoiceCommand(textToUse, currentOutputLang)
            if (wsReply != null) {
                publishActivityEvent(
                    title = "Holographic Workspace",
                    detail = wsReply,
                    category = ActivityCategory.VOICE,
                    status = EventStatus.SUCCESS
                )
                speakWithCoordination(wsReply, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, wsReply, currentOutputLang)
                addLog("Workspace voice command processed: '$textToUse' -> $wsReply")
                return
            }
        }

        // Cross-Device Ecosystem Voice Commands (Phone to Laptop / Watch)
        if (::ecosystemClient.isInitialized) {
            val ecoReply = ecosystemClient.handleEcosystemVoiceCommand(textToUse, currentOutputLang)
            if (ecoReply != null) {
                publishActivityEvent(
                    title = "Ecosystem Dispatch",
                    detail = ecoReply,
                    category = ActivityCategory.SYSTEM,
                    status = EventStatus.SUCCESS
                )
                speakWithCoordination(ecoReply, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, ecoReply, currentOutputLang)
                addLog("Ecosystem command: '$textToUse' -> $ecoReply")
                return
            }
        }

        // 2. Query Processing with History Context (Async for AI Brain & Online Knowledge)
        speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val decision = aiBrain.processQueryAsync(
                    input = command,
                    outputLanguageSetting = currentOutputLang,
                    history = conversationRepo.getRecentHistory()
                )
                executeBrainDecision(decision, currentOutputLang)
                val duration = System.currentTimeMillis() - startTime
                performanceAnalytics.trackExecution(
                    rawCommand = command,
                    normalizedIntent = decision.javaClass.simpleName,
                    executionTimeMs = duration,
                    success = true,
                    language = currentOutputLang
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command '$command': ${e.message}", e)
                val duration = System.currentTimeMillis() - startTime
                performanceAnalytics.trackExecution(
                    rawCommand = command,
                    normalizedIntent = "EXCEPTION",
                    executionTimeMs = duration,
                    success = false,
                    failureReason = e.message ?: "Unknown execution error",
                    language = currentOutputLang
                )
                healthMonitor.reportIncident(
                    component = com.max.assistant.healing.MonitoredComponent.GEMINI_API,
                    errorType = "COMMAND_EXECUTION_EXCEPTION",
                    message = e.message ?: "Execution error",
                    severity = "MEDIUM"
                )
            }
        }
    }

    private fun executeBrainDecision(decision: BrainDecision, currentOutputLang: VoiceLanguage) {
        Log.i(TAG, "executeBrainDecision: decision=$decision")
        when (decision) {
            is BrainDecision.SpeakResponse -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                addLog("AI Brain: ${decision.text}")
                speakWithCoordination(decision.text, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.text, decision.language)
            }
            is BrainDecision.OpenCamera -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openCameraApp(currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenSettings -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openSettingsApp("", currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenBrowser -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openBrowser("https://www.google.com", currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenMaps -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openMaps(decision.query, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenPhone -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openPhoneApp(currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenContacts -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openContactsApp(currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.OpenMessages -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openMessagesApp(currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.SetFlashlight -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.setFlashlight(decision.enabled, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.AdjustVolume -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.adjustVolume(decision.direction, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.CreateCalendarEvent -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.createCalendarEvent(decision.title, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.SwitchLanguage -> {
                updateOutputLanguage(decision.newLanguage)
                speechManager.setInputLanguage(decision.newLanguage)
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(decision.message, decision.newLanguage)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.message, decision.newLanguage)
                addLog("Language switched to ${decision.newLanguage.name}")
            }
            is BrainDecision.OpenWhatsApp -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openWhatsAppApp(decision.business, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.SearchWeb -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                if (::workspaceManager.isInitialized) {
                    workspaceManager.initiateResearchSession(decision.query, decision.language)
                }
                val result = commandExecutor.searchWeb(decision.query, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.StopSpeaking -> {
                stopSpeaking()
                addLog("MAX speech interrupted upon user command, Sir.")
            }
            is BrainDecision.LaunchApp -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openApplication(decision.query, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.MakePhoneCall -> {
                if (decision.requiresConfirmation) {
                    val prompt = confirmationManager.requestCallConfirmation(decision.target, currentOutputLang)
                    speechManager.setAwaitingCommand(true)
                    addLog("Requested call confirmation for ${decision.target}, prompt: '$prompt'")
                    speakWithCoordination(prompt, currentOutputLang, listenForFollowUp = true)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, prompt, currentOutputLang)
                } else {
                    val result = phoneManager.makeCall(decision.target)
                    speakWithCoordination(result.message, currentOutputLang)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message, currentOutputLang)
                }
            }
            is BrainDecision.SendMessage -> {
                if (decision.requiresConfirmation) {
                    val prompt = confirmationManager.requestMessageConfirmation(decision.recipient, decision.message, decision.isWhatsApp, currentOutputLang)
                    speechManager.setAwaitingCommand(true)
                    speakWithCoordination(prompt, currentOutputLang, listenForFollowUp = true)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, prompt, currentOutputLang)
                } else {
                    val result = if (decision.isWhatsApp) {
                        phoneManager.sendWhatsApp(decision.recipient, decision.message)
                    } else {
                        phoneManager.sendMessage(decision.recipient, decision.message)
                    }
                    speakWithCoordination(result.message, currentOutputLang)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.message, currentOutputLang)
                }
            }
            is BrainDecision.RequestReplyRecipient -> {
                confirmationManager.requestReplyRecipient(decision.isWhatsApp)
                speechManager.setAwaitingCommand(true)
                speakWithCoordination(decision.prompt, currentOutputLang, listenForFollowUp = true)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.prompt, currentOutputLang)
                addLog("Awaiting WhatsApp reply recipient, Sir.")
            }
            is BrainDecision.RequestReplyContent -> {
                confirmationManager.requestReplyContent(decision.recipient, decision.isWhatsApp)
                speechManager.setAwaitingCommand(true)
                speakWithCoordination(decision.prompt, currentOutputLang, listenForFollowUp = true)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.prompt, currentOutputLang)
                addLog("Awaiting reply content for ${decision.recipient}, Sir.")
            }
            is BrainDecision.RequestCallRecipient -> {
                confirmationManager.requestCallRecipient()
                speechManager.setAwaitingCommand(true)
                speakWithCoordination(decision.prompt, currentOutputLang, listenForFollowUp = true)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.prompt, currentOutputLang)
                addLog("Awaiting call recipient, Sir.")
            }
            is BrainDecision.QueryBattery -> {
                val statement = systemStatusManager.getBatteryStatement(currentOutputLang)
                speakWithCoordination(statement, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, statement, currentOutputLang)
                addLog("Battery query answered: $statement")
            }
            is BrainDecision.QueryNetwork -> {
                val statement = systemStatusManager.getNetworkStatement(currentOutputLang)
                speakWithCoordination(statement, currentOutputLang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, statement, currentOutputLang)
                addLog("Network query answered: $statement")
            }
            is BrainDecision.SetAlarm -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.setAlarm(decision.hour, decision.minute, decision.message, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.QueryWeather -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val weatherInfo = automationHub.weatherService.getWeather(decision.location)
                    val report = automationHub.weatherService.formatWeatherReport(weatherInfo, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(report, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, report, decision.language)
                    addLog("Live Weather for '${decision.location}': $report")
                }
            }
            is BrainDecision.CheckUpdates -> {
                checkForUpdates()
            }
            is BrainDecision.TriggerStopMax -> {
                executeStopMax()
            }
            is BrainDecision.TriggerMute -> {
                setMuted(decision.mute)
            }
            is BrainDecision.SetTimer -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.setTimer(decision.seconds, decision.message, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.ControlMedia -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.controlMedia(decision.action, currentOutputLang)
                handleCommandExecutionResult(result, currentOutputLang)
            }
            is BrainDecision.PlayMusic -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.playMusic(decision.query, decision.app, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.OfferEmotionalSupport -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                addLog("Emotional Support: ${decision.text}")
                speakWithCoordination(decision.text, decision.language, listenForFollowUp = true)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, decision.text, decision.language)
                confirmationManager.requestMusicConfirmation("uplifting relaxing music")
                speechManager.setAwaitingCommand(true)
            }
            is BrainDecision.SearchNearby -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.searchNearby(decision.category, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.OpenDirections -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openDirections(decision.destination, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.CopyToClipboard -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.copyToClipboard(decision.text, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.ShareText -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.shareText(decision.text, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.CreateNote -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.createNote(decision.content, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.ReadNotes -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.readNotes(decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.AddTask -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.addTask(decision.title, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.ReadTasks -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.readTasks(decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.DraftEmail -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.draftEmail(decision.recipient, decision.subject, decision.body, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.FindContact -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.findContact(decision.name, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.OpenSettingsSubpage -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val result = commandExecutor.openSettingsApp(decision.subpage, decision.language)
                handleCommandExecutionResult(result, decision.language)
            }
            is BrainDecision.ExecuteMultiStepPlan -> {
                executeMultiStepPlan(decision.plan, decision.initialPrompt, decision.language)
            }

            // Study & Education Mode
            is BrainDecision.CreateStudyNotes -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val (content, note) = studyEducationManager.generateChapterNotes(decision.topic, decision.isDetailed, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    val spokenSummary = when (decision.language) {
                        VoiceLanguage.HINGLISH -> "Sir, '${decision.topic}' par study notes ready ho gaye hain aur save kar diye gaye hain. Summary: ${note.summary}"
                        VoiceLanguage.HINDI -> "सर, '${decision.topic}' पर नोट्स तैयार कर लिए गए हैं।"
                        VoiceLanguage.BENGALI -> "স্যার, '${decision.topic}'-এর নোটস প্রস্তুত এবং সংরক্ষিত হয়েছে।"
                        else -> "Sir, study notes on '${decision.topic}' have been generated and saved locally."
                    }
                    speakWithCoordination(spokenSummary, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, "$spokenSummary\n\n$content", decision.language)
                    addLog("Generated notes for '${decision.topic}'")
                }
            }
            is BrainDecision.StartStudyQuiz -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val session = studyEducationManager.generateMCQs(decision.topic, count = 5, examType = decision.examType, language = decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    val q1 = session.currentQuestion()
                    if (q1 != null) {
                        val optsFormatted = q1.options.mapIndexed { idx, opt -> "${com.max.assistant.education.StudyEducationManager.getLetter(idx)}) $opt" }.joinToString("\n")
                        val intro = when (decision.language) {
                            VoiceLanguage.HINGLISH -> "Sir, ${decision.examType} level quiz shuru ho raha hai topic '${decision.topic}' par.\n\nQuestion 1 of ${session.questions.size}:\n${q1.question}\n$optsFormatted"
                            VoiceLanguage.HINDI -> "सर, ${decision.examType} क्विज़ शुरू हो रहा है: '${decision.topic}'।\n\nप्रश्न 1:\n${q1.question}\n$optsFormatted"
                            VoiceLanguage.BENGALI -> "স্যার, ${decision.examType} কুইজ শুরু হচ্ছে: '${decision.topic}'।\n\nপ্রশ্ন 1:\n${q1.question}\n$optsFormatted"
                            else -> "Sir, commencing ${decision.examType} revision quiz on '${decision.topic}'.\n\nQuestion 1 of ${session.questions.size}:\n${q1.question}\n$optsFormatted"
                        }
                        speakWithCoordination(intro, decision.language)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, intro, decision.language)
                        addLog("Started ${decision.examType} quiz on '${decision.topic}'")
                    }
                }
            }
            is BrainDecision.AnswerStudyQuiz -> {
                val feedback = studyEducationManager.answerCurrentQuestion(decision.answer, decision.language)
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(feedback, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, feedback, decision.language)
            }
            is BrainDecision.SolveMath -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val solution = studyEducationManager.solveMath(decision.problem, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(solution, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, solution, decision.language)
                    addLog("Solved math: ${decision.problem}")
                }
            }
            is BrainDecision.ExplainGrammar -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val explanation = studyEducationManager.explainGrammar(decision.query, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(explanation, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, explanation, decision.language)
                }
            }

            // Multimodal Screen Vision
            is BrainDecision.CaptureScreenVision -> {
                if (!screenVisionManager.hasActiveProjection && !accessibilityController.isServiceActive) {
                    pendingScreenPrompt = decision.prompt
                    pendingScreenLang = decision.language
                    val askMsg = when (decision.language) {
                        VoiceLanguage.HINGLISH -> "Sir, screen dekhne ke liye Accessibility permission enable karni hogi. Settings open kar raha hoon, wahan MAX Assistant ko turn on karein."
                        VoiceLanguage.HINDI -> "सर, स्क्रीन देखने के लिए एक्सेसिबिलिटी अनुमति चालू करें। मैं सेटिंग्स खोल रहा हूँ।"
                        VoiceLanguage.BENGALI -> "স্যার, স্ক্রিন দেখার জন্য অ্যাক্সেসিবিলিটি পারমিশন চালু করুন। আমি সেটিংস খুলছি।"
                        else -> "Sir, screen vision requires Accessibility permission. Opening Accessibility settings for you now."
                    }
                    speakWithCoordination(askMsg, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, askMsg, decision.language)
                    accessibilityController.openAccessibilitySettings()
                    try {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                    MainActivity.activeInstance?.requestScreenCaptureConsent()
                } else {
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.SCREEN_SHARING)
                    publishActivityEvent("Screen Vision Activated", "Scanning authorized display frame", ActivityCategory.VISION)
                    updateActiveTask("Screen Vision Analysis", decision.prompt, "Analyzing visible UI elements and text", null, "Processing display frame", 60, "Gemini Multimodal Vision")
                    serviceScope.launch {
                        val (success, answer) = screenVisionManager.analyzeScreenSnapshot(decision.prompt, decision.language)
                        speechManager.setAssistantState(if (success) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                        speakWithCoordination(answer, decision.language)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, answer, decision.language)
                        publishActivityEvent(if (success) "Screen Analysis Complete" else "Screen Analysis Notice", answer.take(80), ActivityCategory.VISION, if (success) EventStatus.SUCCESS else EventStatus.WARNING)
                        clearActiveTask()
                    }
                }
            }
            is BrainDecision.StopScreenVision -> {
                screenVisionManager.cleanupResources()
                val stopMsg = when (decision.language) {
                    VoiceLanguage.HINGLISH -> "Sir, screen vision session band kar diya gaya hai aur resources release kar diye gaye hain."
                    VoiceLanguage.HINDI -> "सर, स्क्रीन विज़न समाप्त कर दिया गया है।"
                    VoiceLanguage.BENGALI -> "স্যার, স্ক্রিন ভিশন বন্ধ করা হয়েছে।"
                    else -> "Screen vision session stopped and display resources released, Sir."
                }
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(stopMsg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, stopMsg, decision.language)
            }
            is BrainDecision.ScreenQuestionFollowUp -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val answer = screenVisionManager.screenContextManager.answerFollowUpQuery(
                        decision.question,
                        aiBrain.aiProvider,
                        decision.language
                    )
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(answer, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, answer, decision.language)
                    addLog("Screen Follow-Up Answered: '${decision.question}'")
                }
            }
            is BrainDecision.ClickScreenElement -> {
                if (PaymentProtectionManager.isPaymentAction(decision.target)) {
                    val msg = PaymentProtectionManager.getBlockedPaymentMessage(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.FAILED)
                    speakWithCoordination(msg, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
                    return
                }
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val clicked = accessibilityController.clickByText(decision.target) ||
                            accessibilityController.clickByDescription(decision.target) ||
                            accessibilityController.clickByViewId(decision.target) ||
                            appInteractionManager.openWhatsAppChat(decision.target)
                    val msg = if (clicked) {
                        when (decision.language) {
                            VoiceLanguage.HINDI -> "जी सर, '${decision.target}' पर क्लिक कर दिया है।"
                            VoiceLanguage.BENGALI -> "হ্যাঁ স্যার, '${decision.target}'-এ ক্লিক করা হয়েছে।"
                            VoiceLanguage.HINGLISH -> "Sir, '${decision.target}' par click kar diya hai."
                            else -> "Clicked '${decision.target}', Sir."
                        }
                    } else {
                        when (decision.language) {
                            VoiceLanguage.HINDI -> "सर, स्क्रीन पर '${decision.target}' नहीं मिला।"
                            VoiceLanguage.BENGALI -> "স্যার, স্ক্রিনে '${decision.target}' পাওয়া যায়নি।"
                            VoiceLanguage.HINGLISH -> "Sir, screen par '${decision.target}' nahi mila."
                            else -> "Could not locate element '${decision.target}' on the visible screen, Sir."
                        }
                    }
                    speechManager.setAssistantState(if (clicked) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                    speakWithCoordination(msg, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
                }
            }
            is BrainDecision.ClickNthScreenOption -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val ok = accessibilityController.clickNthOption(decision.n)
                val msg = if (ok) "Clicked option ${decision.n}, Sir." else "Could not click option ${decision.n}, Sir."
                speechManager.setAssistantState(if (ok) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.ScrollScreen -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val scrolled = if (decision.isDown) accessibilityController.scrollForward() else accessibilityController.scrollBackward()
                val dir = if (decision.isDown) "down" else "up"
                val msg = if (scrolled) "Scrolled $dir, Sir." else "Screen cannot be scrolled $dir further, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.TypeScreenText -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val typed = accessibilityController.typeText(decision.text)
                val msg = if (typed) "Typed '${decision.text}', Sir." else "No active input field was found to type into, Sir."
                speechManager.setAssistantState(if (typed) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.ReadScreenContent -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val text = accessibilityController.extractVisibleText()
                val unread = appInteractionManager.findUnreadWhatsAppMessages()
                val resultText = if (unread.contains("unread")) unread else if (text.isNotBlank()) "Screen content: ${text.take(300)}" else "No readable text detected on screen, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(resultText, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, resultText, decision.language)
            }
            is BrainDecision.NavigateBack -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val ok = accessibilityController.pressBack()
                val msg = if (ok) "Navigated back, Sir." else "Could not navigate back, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.SwitchApp -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                appLauncher.launchAppByVoice(decision.targetApp)
                val msg = "Switching to ${decision.targetApp}, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.CloseScreen -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                accessibilityController.pressHome()
                val msg = "Returned to home screen, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.FillFormAssistance -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                val fields = appInteractionManager.inspectVisibleFormFields()
                val msg = if (fields.isEmpty()) {
                    "No interactive form fields found on the current screen, Sir."
                } else {
                    "I identified ${fields.size} form fields. Please tell me which field you would like me to fill first, Sir."
                }
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
            }
            is BrainDecision.ExecuteCompoundTask -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val plan = taskExecutionEngine.createPlanForQuery(decision.query)
                    if (plan != null) {
                        speakWithCoordination("Starting task: ${plan.title}", decision.language)
                        val (success, resultMsg) = taskExecutionEngine.executePlan(plan) { progress ->
                            addLog("Task progress: $progress")
                        }
                        speechManager.setAssistantState(if (success) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                        speakWithCoordination(resultMsg, decision.language)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, resultMsg, decision.language)
                    } else {
                        val fallback = "I could not construct a verified multi-step plan for that command, Sir."
                        speechManager.setAssistantState(com.max.assistant.speech.AssistantState.FAILED)
                        speakWithCoordination(fallback, decision.language)
                    }
                }
            }
            is BrainDecision.CancelTask -> {
                val cancelMsg = taskExecutionEngine.cancelActivePlan()
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(cancelMsg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, cancelMsg, decision.language)
            }
            is BrainDecision.ExplainTaskProgress -> {
                val prog = taskExecutionEngine.getActivePlanProgress() ?: "No multi-step task is currently executing, Sir."
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(prog, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, prog, decision.language)
            }

            // Smart Persistent Reminders
            is BrainDecision.ScheduleSmartReminder -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                val (success, msg) = reminderScheduler.scheduleReminder(
                    title = decision.title,
                    notes = decision.notes,
                    triggerEpoch = decision.triggerEpoch,
                    repeatInterval = decision.repeat,
                    priority = decision.priority,
                    language = decision.language
                )
                speechManager.setAssistantState(if (success) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
                speakWithCoordination(msg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
                addLog("Reminder scheduled: success=$success, title='${decision.title}'")
            }
            is BrainDecision.ListSmartReminders -> {
                val summary = reminderScheduler.getActiveRemindersSummary(decision.language)
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(summary, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, summary, decision.language)
            }

            // Standby & Full Shutdown Architecture
            is BrainDecision.EnterStandbyMode -> {
                val msg = serviceController.enterStandby(decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
                addLog("MAX switched to Standby mode: $msg")
                speakWithCoordination(msg, decision.language, listenForFollowUp = false) {
                    speechManager.enterStandby()
                }
            }
            is BrainDecision.ExecuteFullShutdown -> {
                val msg = when (decision.language) {
                    VoiceLanguage.HINGLISH -> "Sir, MAX completely shutdown ho raha hai. Sabhi background services aur microphone release kiye ja rahe hain. Dobara shuru karne ke liye app icon tap karein."
                    VoiceLanguage.HINDI -> "सर, मैक्स पूरी तरह बंद हो रहा है। माइक्रोफोन और सभी सेवाएँ समाप्त की जा रही हैं। ऐप आइकन से पुनः शुरू करें।"
                    VoiceLanguage.BENGALI -> "স্যার, ম্যাক্স সম্পূর্ণরূপে বন্ধ হচ্ছে। পুনরায় শুরু করতে অ্যাপ আইকন ব্যবহার করুন।"
                    else -> "Sir, executing complete shutdown of MAX. All foreground services and microphone resources are now terminated. Tap the app icon to reactivate."
                }
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, decision.language)
                addLog("MAX Executing Full Shutdown")
                speakWithCoordination(msg, decision.language, listenForFollowUp = false) {
                    serviceController.executeFullShutdown(decision.keepReminders, decision.language)
                }
            }

            // Baby Entertainment Mode Decisions
            is BrainDecision.StartBabyMode -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.startBabyMode(decision.reason, decision.language)
                addLog("Baby Entertainment Mode started (${decision.reason})")
            }
            is BrainDecision.NextBabyActivity -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.nextActivity(decision.language)
                addLog("Baby Entertainment Mode advanced to next activity")
            }
            is BrainDecision.PlayBabyLullaby -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.playLullaby(decision.language)
                addLog("Baby Entertainment Mode playing lullaby")
            }
            is BrainDecision.PlayBabyAnimalSounds -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.playAnimalSounds(decision.language)
                addLog("Baby Entertainment Mode playing animal sounds")
            }
            is BrainDecision.TellBabyStory -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.tellStory(decision.language)
                addLog("Baby Entertainment Mode telling story")
            }
            is BrainDecision.PlayBabyRhyme -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.playRhymes(decision.language)
                addLog("Baby Entertainment Mode playing rhymes")
            }
            is BrainDecision.StopBabyMode -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                babyEntertainmentManager.stopBabyMode()
                val stopMsg = when (decision.language) {
                    VoiceLanguage.BENGALI -> "বেবি মোড বন্ধ করা হয়েছে স্যার।"
                    VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "बेबी एंटरटेनमेंट मोड बंद कर दिया गया है, सर।"
                    else -> "Baby Entertainment Mode stopped, Sir."
                }
                speakWithCoordination(stopMsg, decision.language)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, stopMsg, decision.language)
                addLog("Baby Entertainment Mode stopped")
            }

            // Advanced Contextual Memory Decisions
            is BrainDecision.RecallYesterdayDiscussion -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val reply = contextMemoryManager.recallYesterdayDiscussion(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(reply, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, reply, decision.language)
                }
            }
            is BrainDecision.RecallProjectTasks -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val reply = contextMemoryManager.recallProjectTasks(decision.projectName, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(reply, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, reply, decision.language)
                }
            }
            is BrainDecision.SaveUserMemory -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val reply = contextMemoryManager.saveUserMemory(decision.fact, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(reply, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, reply, decision.language)
                }
            }
            is BrainDecision.ForgetUserMemory -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val reply = contextMemoryManager.forgetMemory(decision.query, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(reply, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, reply, decision.language)
                }
            }
            is BrainDecision.RecallUserMemories -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val reply = contextMemoryManager.recallUserMemories(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(reply, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, reply, decision.language)
                }
            }

            // Self-Healing & Health Monitor Decisions
            is BrainDecision.CheckSystemHealth -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val summary = healthMonitor.getHealthSummary(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(summary, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, summary, decision.language)
                }
            }
            is BrainDecision.RunSelfDiagnostics -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.VERIFYING)
                serviceScope.launch {
                    val diag = healthMonitor.runSelfDiagnostics(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(diag, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, diag, decision.language)
                }
            }

            // Intelligent Notification Decisions
            is BrainDecision.SummarizeNotifications -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val notifs = notificationManager.summarizeImportantNotifications(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(notifs, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, notifs, decision.language)
                }
            }
            is BrainDecision.ReadWhatsAppNotifications -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.EXECUTING_COMMAND)
                serviceScope.launch {
                    val msgs = notificationManager.readWhatsAppMessagesAloud(decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(msgs, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msgs, decision.language)
                }
            }

            // Live Internet Research Decisions
            is BrainDecision.ConductLiveResearch -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
                serviceScope.launch {
                    val report = researchEngine.conductResearch(decision.query, decision.language)
                    speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                    speakWithCoordination(report.spokenSummary, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, report.spokenSummary, decision.language)
                }
            }

            // Performance Analytics Decisions
            is BrainDecision.QueryPerformanceStats -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                serviceScope.launch {
                    val stats = performanceAnalytics.getPerformanceSummary(decision.language)
                    speakWithCoordination(stats, decision.language)
                    conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, stats, decision.language)
                }
            }
        }
    }

    fun triggerPendingScreenAnalysis() {
        val prompt = pendingScreenPrompt ?: "Explain what is on the screen"
        val lang = pendingScreenLang
        pendingScreenPrompt = null
        speechManager.setAssistantState(com.max.assistant.speech.AssistantState.THINKING)
        serviceScope.launch {
            val (success, answer) = screenVisionManager.analyzeScreenSnapshot(prompt, lang)
            speechManager.setAssistantState(if (success) com.max.assistant.speech.AssistantState.COMPLETED else com.max.assistant.speech.AssistantState.FAILED)
            speakWithCoordination(answer, lang)
            conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, answer, lang)
            addLog("Pending Screen Vision analyzed: success=$success")
        }
    }

    fun updateForegroundServiceForMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            try {
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification("MAX Screen Vision Active"),
                    type
                )
            } catch (e: Exception) {
                Log.w(TAG, "updateForegroundServiceForMediaProjection error: ${e.message}")
            }
        }
    }

    fun releaseMediaProjectionForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification("MAX Voice Assistant Active"),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (e: Exception) {
                Log.w(TAG, "releaseMediaProjectionForegroundService error: ${e.message}")
            }
        }
    }

    private fun handleCommandExecutionResult(res: com.max.assistant.executor.CommandExecutionResult, lang: VoiceLanguage) {
        when (res) {
            is com.max.assistant.executor.CommandExecutionResult.Success -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.COMPLETED)
                speakWithCoordination(res.message, lang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, res.message, lang)
                publishActivityEvent("Execution Succeeded", "${res.actionType}: ${res.message}", ActivityCategory.EXECUTION, EventStatus.SUCCESS)
                clearActiveTask()
            }
            is com.max.assistant.executor.CommandExecutionResult.Blocked -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.FAILED)
                speakWithCoordination(res.reason, lang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, res.reason, lang)
                publishActivityEvent("Action Blocked / Security Shield", res.reason, ActivityCategory.SECURITY, EventStatus.WARNING)
                clearActiveTask()
            }
            is com.max.assistant.executor.CommandExecutionResult.Failed -> {
                speechManager.setAssistantState(com.max.assistant.speech.AssistantState.FAILED)
                speakWithCoordination(res.error, lang)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, res.error, lang)
                publishActivityEvent("Execution Failed", res.error, ActivityCategory.EXECUTION, EventStatus.ERROR)
                clearActiveTask()
            }
            is com.max.assistant.executor.CommandExecutionResult.RequiresConfirmation -> {
                speechManager.setAwaitingCommand(true)
                speakWithCoordination(res.prompt, lang, listenForFollowUp = true)
                conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, res.prompt, lang)
                addLog("CONFIRMATION: ${res.prompt}")
            }
        }
    }

    private fun executeMultiStepPlan(
        plan: com.max.assistant.planner.MultiStepPlan,
        initialPrompt: String,
        lang: VoiceLanguage
    ) {
        addLog("Starting Multi-Step Plan: '${plan.goal}' with ${plan.steps.size} steps.")
        if (initialPrompt.isNotBlank()) {
            speakWithCoordination(initialPrompt, lang)
            conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, initialPrompt, lang)
        }

        serviceScope.launch {
            // Give initial utterance a short delay before launching first step intent
            kotlinx.coroutines.delay(1000)

            for (step in plan.steps) {
                val active = taskPlanner.activePlan.value
                if (active == null || active.isCompleted) {
                    addLog("Multi-step plan '${plan.goal}' cancelled or halted.")
                    break
                }

                taskPlanner.markStepExecuting(step.stepIndex)
                addLog("Running Step ${step.stepIndex + 1}/${plan.steps.size}: ${step.description}")

                val result = executeStep(step.decision, lang)
                when (result) {
                    is com.max.assistant.executor.CommandExecutionResult.Success -> {
                        taskPlanner.markStepSuccess(step.stepIndex, result.message)
                        addLog("Step ${step.stepIndex + 1} succeeded: ${result.message}")
                    }
                    is com.max.assistant.executor.CommandExecutionResult.Blocked -> {
                        taskPlanner.markStepFailed(step.stepIndex, result.reason)
                        addLog("Step ${step.stepIndex + 1} blocked: ${result.reason}")
                        speakWithCoordination(result.reason, lang)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.reason, lang)
                        break
                    }
                    is com.max.assistant.executor.CommandExecutionResult.Failed -> {
                        taskPlanner.markStepFailed(step.stepIndex, result.error)
                        addLog("Step ${step.stepIndex + 1} failed: ${result.error}")
                        speakWithCoordination(result.error, lang)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.error, lang)
                        break
                    }
                    is com.max.assistant.executor.CommandExecutionResult.RequiresConfirmation -> {
                        addLog("Step ${step.stepIndex + 1} requires confirmation.")
                        speechManager.setAwaitingCommand(true)
                        speakWithCoordination(result.prompt, lang, listenForFollowUp = true)
                        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, result.prompt, lang)
                        break
                    }
                }

                // Stagger intents so Android Activity Manager has time to transition
                kotlinx.coroutines.delay(1200)
            }
        }
    }

    private fun executeStep(
        decision: BrainDecision,
        lang: VoiceLanguage
    ): com.max.assistant.executor.CommandExecutionResult {
        return when (decision) {
            is BrainDecision.SearchNearby -> commandExecutor.searchNearby(decision.category, lang)
            is BrainDecision.OpenDirections -> commandExecutor.openDirections(decision.destination, lang)
            is BrainDecision.SetTimer -> commandExecutor.setTimer(decision.seconds, decision.message, lang)
            is BrainDecision.ControlMedia -> commandExecutor.controlMedia(decision.action, lang)
            is BrainDecision.PlayMusic -> commandExecutor.playMusic(decision.query, decision.app, lang)
            is BrainDecision.CreateNote -> commandExecutor.createNote(decision.content, lang)
            is BrainDecision.ReadNotes -> commandExecutor.readNotes(lang)
            is BrainDecision.AddTask -> commandExecutor.addTask(decision.title, lang)
            is BrainDecision.ReadTasks -> commandExecutor.readTasks(lang)
            is BrainDecision.DraftEmail -> commandExecutor.draftEmail(decision.recipient, decision.subject, decision.body, lang)
            is BrainDecision.CopyToClipboard -> commandExecutor.copyToClipboard(decision.text, lang)
            is BrainDecision.ShareText -> commandExecutor.shareText(decision.text, lang)
            is BrainDecision.FindContact -> commandExecutor.findContact(decision.name, lang)
            is BrainDecision.OpenSettingsSubpage -> commandExecutor.openSettingsApp(decision.subpage, lang)
            is BrainDecision.LaunchApp -> commandExecutor.openApplication(decision.query, lang)
            is BrainDecision.SetAlarm -> commandExecutor.setAlarm(decision.hour, decision.minute, decision.message, lang)
            is BrainDecision.SetFlashlight -> commandExecutor.setFlashlight(decision.enabled, lang)
            is BrainDecision.AdjustVolume -> commandExecutor.adjustVolume(decision.direction, lang)
            is BrainDecision.QueryBattery -> {
                val statement = systemStatusManager.getBatteryStatement(lang)
                com.max.assistant.executor.CommandExecutionResult.Success(statement, "BATTERY")
            }
            is BrainDecision.QueryNetwork -> {
                val statement = systemStatusManager.getNetworkStatement(lang)
                com.max.assistant.executor.CommandExecutionResult.Success(statement, "NETWORK")
            }
            else -> com.max.assistant.executor.CommandExecutionResult.Success("Completed", "GENERIC")
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices?.any { it.resolveInfo.serviceInfo.packageName == packageName } == true
    }

    fun getDiagnosticInfo(hasMicPermission: Boolean): SystemDiagnosticInfo {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(packageName) == true
        val batteryDesc = if (isIgnoringBatteryOptimizations) {
            "Unrestricted (Active 24/7 background listening)"
        } else {
            "Optimized by Android/Samsung (Background listening may pause in sleep. Exclude MAX in Battery Settings)."
        }

        return SystemDiagnosticInfo(
            aiConnectionStatus = if (aiBrain.aiProvider?.isConfigured == true) "Configured (${aiBrain.aiProvider?.providerName})" else "Offline Knowledge Base (Active)",
            micPermissionGranted = hasMicPermission,
            speechRecognitionAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(this),
            ttsEngineStatus = voiceEngine.settings.value.engineStatusMessage,
            selectedLanguage = "${voiceEngine.settings.value.outputLanguage.displayName} (${voiceEngine.settings.value.outputLanguage.recognitionTag})",
            accessibilityEnabled = isAccessibilityEnabled(),
            commandExecutorStatus = "Active (Camera, Audio, PackageManager, Telephony)",
            batteryOptimizationStatus = batteryDesc
        )
    }

    fun updateAllowShortWakeWord(allow: Boolean) {
        voiceEngine.updateAllowShortWakeWord(allow)
        speechManager.allowShortWakeWord = allow
        addLog(if (allow) "Short wake phrase ('MAX') enabled by Sir." else "Short wake phrase disabled ('Hey MAX' required).")
    }

    fun updateMuteListeningBeep(mute: Boolean) {
        voiceEngine.updateMuteListeningBeep(mute)
        speechManager.muteListeningBeep = mute
        addLog(if (mute) "Listening beep muted (Silent mode active)." else "Listening beep unmuted.")
    }

    private var updateJob: Job? = null

    private fun startAutoUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            // Wait 20 seconds after launch before first update check
            delay(20_000L)
            while (isActive) {
                try {
                    addLog("Cloud Auto-Update: Checking latest release...")
                    val checkResult = updateManager.checkVersion()
                    if (checkResult is UpdateStatus.UpdateAvailable) {
                        addLog("Cloud Auto-Update: Version ${checkResult.latestVersion} found. Downloading in background...")
                        speakWithCoordination("सर, MAX का नया अपडेट ${checkResult.latestVersion} आ गया है। बैकग्राउंड में डाउनलोड किया जा रहा है।", VoiceLanguage.HINDI)
                        val status = updateManager.downloadAndInstallUpdate(checkResult.downloadUrl)
                        if (status is UpdateStatus.ReadyToInstall) {
                            speakWithCoordination("सर, नया अपडेट तैयार है। स्क्रीन पर इंस्टॉलेशन कन्फर्म करें।", VoiceLanguage.HINDI)
                            addLog("Cloud Auto-Update: Downloaded. PackageInstaller prompt triggered.")
                        }
                    } else {
                        addLog("Cloud Auto-Update: MAX is on the latest release.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Auto-update loop check failed: ${e.message}")
                }
                // Check every 6 hours
                delay(6 * 3600 * 1000L)
            }
        }
    }

    private fun checkForUpdates() {
        speakWithCoordination("अपडेट जांचा जा रहा है, सर। कृपया प्रतीक्षा करें।", VoiceLanguage.HINDI)
        addLog("Checking updates from GitHub Releases & CDN...")
        serviceScope.launch {
            when (val checkResult = updateManager.checkVersion()) {
                is UpdateStatus.UpdateAvailable -> {
                    speakWithCoordination("सर, नया अपडेट ${checkResult.latestVersion} उपलब्ध है। डाउनलोड किया जा रहा है।", VoiceLanguage.HINDI)
                    addLog("Update ${checkResult.latestVersion} found. Downloading...")
                    val status = updateManager.downloadAndInstallUpdate(checkResult.downloadUrl)
                    when (status) {
                        is UpdateStatus.ReadyToInstall -> {
                            speakWithCoordination("नया अपडेट डाउनलोड हो गया है, सर। इंस्टॉलेशन शुरू किया जा रहा है।", VoiceLanguage.HINDI)
                            addLog("Update ready: PackageInstaller triggered.")
                        }
                        is UpdateStatus.Error -> {
                            speakWithCoordination("सर, डाउनलोड में समस्या आई: ${status.message}", VoiceLanguage.HINDI)
                            addLog("Update download failed: ${status.message}")
                        }
                        else -> {}
                    }
                }
                is UpdateStatus.UpToDate -> {
                    speakWithCoordination("सर, MAX पहले से ही नवीनतम संस्करण पर अपडेटेड है।", VoiceLanguage.HINDI)
                    addLog("MAX is already on the latest version.")
                }
                is UpdateStatus.Error -> {
                    speakWithCoordination("सर, अपडेट सर्वर से संपर्क नहीं हो पाया।", VoiceLanguage.HINDI)
                    addLog("Update check notice: ${checkResult.message}")
                }
                else -> {
                    speakWithCoordination("सर, MAX पहले से ही नवीनतम संस्करण पर अपडेटेड है।", VoiceLanguage.HINDI)
                    addLog("MAX is up-to-date.")
                }
            }
        }
    }

    fun updateVoicePitch(pitch: Float) {
        voiceEngine.updatePitch(pitch)
    }

    fun updateVoiceSpeed(speed: Float) {
        voiceEngine.updateSpeed(speed)
    }

    fun updateInputLanguage(language: VoiceLanguage) {
        voiceEngine.updateInputLanguage(language)
        speechManager.setInputLanguage(language)
    }

    fun updateOutputLanguage(language: VoiceLanguage) {
        voiceEngine.updateOutputLanguage(language)
    }

    fun testVoice(sampleText: String? = null) {
        val currentLang = voiceEngine.settings.value.outputLanguage
        val defaultSample = when (currentLang) {
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "MAX JARVIS system online and standing by at your service, Sir."
            VoiceLanguage.BENGALI -> "ম্যাক্স অনলাইন আছে, স্যার। আমি আপনার সেবায় প্রস্তুত।"
            VoiceLanguage.HINDI -> "नमस्ते सर! मैक्स आवाज प्रणाली पूरी तरह से तैयार है।"
            VoiceLanguage.HINGLISH -> "Hello Sir! MAX system taiyaar hai, what can I do for you?"
        }
        val text = sampleText ?: defaultSample
        speakWithCoordination(text, currentLang)
    }

    fun stopSpeaking() {
        voiceEngine.stop()
        speechManager.resumeAfterSpeaking()
    }

    fun executeStopMax() {
        if (::babyEntertainmentManager.isInitialized) {
            babyEntertainmentManager.stopBabyMode()
        }
        taskPlanner.cancelActivePlan("MAX stopped by user")
        taskExecutionEngine.cancelActivePlan()
        screenVisionManager.cleanupResources()
        voiceEngine.stop()
        speechManager.stopMax()
        val curLang = voiceEngine.settings.value.outputLanguage
        val stopMsg = when (curLang) {
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "MAX systems entering standby mode, Sir."
            VoiceLanguage.BENGALI -> "ম্যাক্স স্থগিত করা হয়েছে, স্যার।"
            else -> "मैक्स रोक दिया गया है, सर।"
        }
        addLog("MAX standby initiated, Sir.")
        updateNotification("MAX is Standby")
        speakWithCoordination(stopMsg, curLang, listenForFollowUp = false) {
            speechManager.stopMax()
        }
    }

    fun requestPinDialog() {
        _showPinDialog.value = true
        val curLang = voiceEngine.settings.value.outputLanguage
        val pinMsg = when (curLang) {
            VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Please enter your security PIN, Sir."
            VoiceLanguage.BENGALI -> "দয়া করে আপনার নিরাপত্তা পিন লিখুন, স্যার।"
            else -> "कृपया सुरक्षा पिन दर्ज करें, सर।"
        }
        speakWithCoordination(pinMsg, curLang)
    }

    fun verifyPin(pin: String): Boolean {
        val success = speechManager.verifyPinAndUnlock(pin)
        val curLang = voiceEngine.settings.value.outputLanguage
        if (success) {
            _showPinDialog.value = false
            val authMsg = when (curLang) {
                VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Authorization confirmed. Welcome back, Sir."
                VoiceLanguage.BENGALI -> "অনুমতি নিশ্চিত করা হয়েছে। স্বাগতম, স্যার।"
                else -> "अनुमति पुष्ट हुई। स्वागत है सर।"
            }
            speakWithCoordination(authMsg, curLang)
            addLog("Security PIN verified. MAX restarted for Sir.")
            updateNotification("MAX Voice Assistant Active")
        } else {
            val deniedMsg = when (curLang) {
                VoiceLanguage.ENGLISH, VoiceLanguage.AUTO -> "Access denied. Incorrect security PIN, Sir."
                VoiceLanguage.BENGALI -> "ভুল পিন, স্যার।"
                else -> "गलत पिन, सर।"
            }
            speakWithCoordination(deniedMsg, curLang)
            addLog("Incorrect PIN attempt.")
        }
        return success
    }

    fun dismissPinDialog() {
        _showPinDialog.value = false
        speechManager.cancelPinEntry()
    }

    fun addLog(entry: String) {
        Log.i(TAG, entry)
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $entry"
        val current = _logs.value.toMutableList()
        current.add(0, formatted)
        if (current.size > 50) current.removeAt(current.size - 1)
        _logs.value = current
    }

    fun toggleMute(): Boolean {
        val newMute = !voiceEngine.isMuted.value
        setMuted(newMute)
        return newMute
    }

    fun setMuted(muted: Boolean) {
        voiceEngine.setMute(muted)
        speechManager.setMuted(muted)
        val currentOutputLang = voiceEngine.settings.value.outputLanguage
        val msg = if (muted) {
            when (currentOutputLang) {
                VoiceLanguage.BENGALI -> "আওয়াজ এবং মাইক্রোফোন বন্ধ করা হয়েছে, স্যার।"
                VoiceLanguage.HINDI -> "आवाज़ और माइक्रोफ़ोन म्यूट कर दिए गए हैं, सर।"
                VoiceLanguage.HINGLISH -> "Voice aur microphone mute kar diye gaye hain, Sir."
                else -> "Voice and microphone muted, Sir."
            }
        } else {
            when (currentOutputLang) {
                VoiceLanguage.BENGALI -> "আওয়াজ এবং মাইক্রোফোন চালু করা হয়েছে, স্যার।"
                VoiceLanguage.HINDI -> "आवाज़ और माइक्रोफ़ोन चालू कर दिए गए हैं, सर।"
                VoiceLanguage.HINGLISH -> "Voice aur microphone unmute ho gaye hain, Sir."
                else -> "Voice and microphone unmuted, Sir."
            }
        }
        addLog(msg)
        conversationRepo.addMessage(com.max.assistant.ai.MessageRole.MAX, msg, currentOutputLang)
        updateNotification(if (muted) "MAX is Muted (Tap to Unmute)" else "MAX Voice Assistant Active")
        if (!muted) {
            speakWithCoordination(msg, currentOutputLang)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAX Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MAX continuous voice assistant service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, MaxForegroundService::class.java).apply {
            action = ACTION_MUTE_TOGGLE
        }
        val mutePending = PendingIntent.getService(
            this, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MaxForegroundService::class.java).apply {
            action = ACTION_STOP_MAX
        }
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isMuted = voiceEngine.isMuted.value
        val muteLabel = if (isMuted) "Unmute" else "Mute"
        val muteIcon = if (isMuted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAX Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(muteIcon, muteLabel, mutePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop MAX", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun stopForegroundService() {
        if (::babyEntertainmentManager.isInitialized) {
            babyEntertainmentManager.stopBabyMode()
        }
        speechManager.pauseListening()
        voiceEngine.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        activeInstance = null
        _serviceRunning.value = false
        if (::babyEntertainmentManager.isInitialized) {
            babyEntertainmentManager.stopBabyMode()
        }
        speechManager.destroy()
        voiceEngine.shutdown()
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "MaxForegroundService destroyed.")
    }
}
