package com.max.assistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.max.assistant.hud.ActivityEvent
import com.max.assistant.hud.ActiveTaskState
import com.max.assistant.hud.ScreenVisionUiState
import com.max.assistant.hud.SystemTelemetryInfo
import com.max.assistant.service.MaxForegroundService
import com.max.assistant.speech.AssistantState
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.tts.VoiceSettings
import com.max.assistant.ui.dashboard.FuturisticDashboard
import com.max.assistant.ui.theme.MAXTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var activeInstance: MainActivity? = null
            private set
    }

    private var foregroundService by mutableStateOf<MaxForegroundService?>(null)
    private var isBound by mutableStateOf(false)
    private var pendingIntentCommand: String? = null
    private var screenCaptureCallbackLauncher: androidx.activity.result.ActivityResultLauncher<Intent>? = null

    fun requestScreenCaptureConsent() {
        runOnUiThread {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? android.media.projection.MediaProjectionManager
            if (mediaProjectionManager != null) {
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                screenCaptureCallbackLauncher?.launch(captureIntent)
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MaxForegroundService.LocalBinder
            val svc = binder.getService()
            foregroundService = svc
            isBound = true
            pendingIntentCommand?.let { cmd ->
                svc.handleIncomingCommand(cmd)
                pendingIntentCommand = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = this
        enableEdgeToEdge()
        // Completely eliminate all UI touch sound effects - silence all non-voice audio
        window.decorView.isSoundEffectsEnabled = false
        handleIntentCommand(intent)

        setContent {
            MAXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07090E)
                ) {
                    var hasMicPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
                        hasMicPermission = micGranted
                        if (micGranted) {
                            startAndBindAssistantService()
                        }
                    }

                    val screenCaptureLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK && result.data != null) {
                            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? android.media.projection.MediaProjectionManager
                            val projection = mediaProjectionManager?.getMediaProjection(result.resultCode, result.data!!)
                            if (projection != null) {
                                val metrics = resources.displayMetrics
                                val svc = foregroundService ?: MaxForegroundService.activeInstance
                                svc?.updateForegroundServiceForMediaProjection()
                                svc?.screenVisionManager?.setMediaProjection(
                                    projection = projection,
                                    width = metrics.widthPixels,
                                    height = metrics.heightPixels,
                                    densityDpi = metrics.densityDpi
                                )
                                svc?.triggerPendingScreenAnalysis()
                            }
                        }
                    }

                    SideEffect {
                        screenCaptureCallbackLauncher = screenCaptureLauncher
                    }

                    LaunchedEffect(Unit) {
                        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.SEND_SMS)
                        }

                        if (!hasMicPermission || permissionsToRequest.size > 1) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            startAndBindAssistantService()
                        }
                    }

                    val service = foregroundService ?: MaxForegroundService.activeInstance
                    val speechManager = service?.speechManager
                    val voiceEngine = service?.voiceEngine

                    val defaultStateFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(AssistantState.IDLE) }
                    val defaultRmsFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(0f) }
                    val defaultTranscriptFlow = remember { kotlinx.coroutines.flow.MutableStateFlow("") }
                    val defaultMuteFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
                    val defaultSpeakingFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
                    val defaultSettingsFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(VoiceSettings()) }
                    val defaultPinFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
                    val defaultPendingActionFlow = remember { kotlinx.coroutines.flow.MutableStateFlow<com.max.assistant.action.PendingAction?>(null) }
                    val defaultBabyFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(com.max.assistant.baby.BabyModeState()) }
                    val defaultEventsFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<ActivityEvent>()) }
                    val defaultTaskFlow = remember { kotlinx.coroutines.flow.MutableStateFlow<ActiveTaskState?>(null) }
                    val defaultVisionFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(ScreenVisionUiState()) }
                    val defaultTelemetryFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(SystemTelemetryInfo()) }
                    val defaultWorkspaceFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(com.max.assistant.workspace.WorkspaceUiState()) }
                    val defaultDevicesFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.max.assistant.ecosystem.ConnectedDevice>()) }

                    val assistantState by (speechManager?.assistantState ?: defaultStateFlow).collectAsState()
                    val rmsLevel by (speechManager?.rmsLevel ?: defaultRmsFlow).collectAsState()
                    val liveTranscript by (speechManager?.liveTranscript ?: defaultTranscriptFlow).collectAsState()
                    val isMuted by (voiceEngine?.isMuted ?: defaultMuteFlow).collectAsState()
                    val isSpeaking by (voiceEngine?.isSpeaking ?: defaultSpeakingFlow).collectAsState()
                    val voiceSettings by (voiceEngine?.settings ?: defaultSettingsFlow).collectAsState()
                    val showPinDialog by (service?.showPinDialog ?: defaultPinFlow).collectAsState()
                    val pendingAction by (service?.confirmationManager?.pendingAction ?: defaultPendingActionFlow).collectAsState()
                    val babyModeState by (service?.babyEntertainmentManager?.state ?: defaultBabyFlow).collectAsState()
                    val activityEvents by (service?.activityEvents ?: defaultEventsFlow).collectAsState()
                    val activeTaskState by (service?.activeTaskState ?: defaultTaskFlow).collectAsState()
                    val screenVisionState by (service?.screenVisionUiState ?: defaultVisionFlow).collectAsState()
                    val systemTelemetry by (service?.systemTelemetry ?: defaultTelemetryFlow).collectAsState()
                    val workspaceState by (service?.workspaceUiState ?: defaultWorkspaceFlow).collectAsState()
                    val connectedDevices by (service?.ecosystemClient?.connectedDevices ?: defaultDevicesFlow).collectAsState()

                    val conversationRepo = remember(service) {
                        service?.conversationRepo ?: com.max.assistant.ai.ConversationRepository.getInstance(this@MainActivity)
                    }
                    val recentHistory by conversationRepo.messages.collectAsState()
                    val diagnosticInfo = remember(service, hasMicPermission, assistantState) {
                        service?.getDiagnosticInfo(hasMicPermission)
                    }

                    if (babyModeState.isActive) {
                        com.max.assistant.baby.BabyEntertainmentScreen(
                            state = babyModeState,
                            currentLanguage = voiceSettings.outputLanguage,
                            onNextActivity = { service?.babyEntertainmentManager?.nextActivity(voiceSettings.outputLanguage) },
                            onSelectLullaby = { service?.babyEntertainmentManager?.playLullaby(voiceSettings.outputLanguage) },
                            onSelectRhymes = { service?.babyEntertainmentManager?.playRhymes(voiceSettings.outputLanguage) },
                            onSelectAnimalSounds = { service?.babyEntertainmentManager?.playAnimalSounds(voiceSettings.outputLanguage) },
                            onSelectStory = { service?.babyEntertainmentManager?.tellStory(voiceSettings.outputLanguage) },
                            onPopBubble = { bubbleId -> service?.babyEntertainmentManager?.popBubble(bubbleId) },
                            onStopBabyMode = { service?.babyEntertainmentManager?.stopBabyMode() }
                        )
                    } else {
                        FuturisticDashboard(
                            assistantState = assistantState,
                            rmsLevel = rmsLevel,
                            liveTranscript = liveTranscript,
                            isMuted = isMuted,
                            isSpeaking = isSpeaking,
                            voiceSettings = voiceSettings,
                            showPinDialog = showPinDialog,
                            pendingAction = pendingAction,
                            diagnosticInfo = diagnosticInfo,
                            recentHistory = recentHistory,
                            activityEvents = activityEvents,
                            activeTaskState = activeTaskState,
                            screenVisionState = screenVisionState,
                            systemTelemetry = systemTelemetry,
                            workspaceState = workspaceState,
                            connectedDevices = connectedDevices,
                            onToggleWorkspace = { service?.workspaceManager?.toggleWorkspaceVisibility() },
                            onCloseWorkspace = { service?.workspaceManager?.setWorkspaceVisible(false) },
                            onToggleCompactCore = { service?.workspaceManager?.toggleCompactCore() },
                            onCreateWorkspaceTab = { title -> service?.workspaceManager?.createTab(title) },
                            onSwitchWorkspaceTab = { tabId -> service?.workspaceManager?.switchTab(tabId) },
                            onCloseWorkspaceTab = { tabId -> service?.workspaceManager?.closeTab(tabId) },
                            onRenameWorkspaceTab = { tabId, newTitle -> service?.workspaceManager?.renameTab(tabId, newTitle) },
                            onSetWorkspaceLayoutMode = { tabId, mode -> service?.workspaceManager?.setLayoutMode(tabId, mode) },
                            onCloseWorkspacePanel = { tabId, pId -> service?.workspaceManager?.closePanel(tabId, pId) },
                            onMinimizeWorkspacePanel = { tabId, pId -> service?.workspaceManager?.minimizePanel(tabId, pId) },
                            onMaximizeWorkspacePanel = { tabId, pId -> service?.workspaceManager?.maximizePanel(tabId, pId) },
                            onRestoreWorkspacePanel = { tabId, pId -> service?.workspaceManager?.restorePanel(tabId, pId) },
                            onUpdateWorkspacePanelPosition = { tabId, pId, dx, dy -> service?.workspaceManager?.updatePanelPosition(tabId, pId, dx, dy) },
                            onPlayWorkspaceVideo = { video -> service?.workspaceManager?.playVideo(video) },
                            onSummarizeWorkspaceVideo = { video -> service?.workspaceManager?.summarizeVideo(video) },
                            onSearchWorkspaceVideos = { query -> service?.workspaceManager?.searchAndOpenVideos(query) },
                            onSpeakWorkspaceSummary = { summary -> service?.speakWithCoordination(summary, voiceSettings.outputLanguage) },
                            onToggleMute = {
                                service?.toggleMute()
                            },
                            onStopMax = {
                                if (assistantState == AssistantState.STOPPED) {
                                    service?.requestPinDialog()
                                } else {
                                    service?.executeStopMax()
                                }
                            },
                            onVerifyPin = { pin ->
                                service?.verifyPin(pin)
                            },
                            onDismissPin = {
                                service?.dismissPinDialog()
                            },
                            onCoreTapped = {
                                if (assistantState == AssistantState.STOPPED) {
                                    service?.requestPinDialog()
                                } else {
                                    if (isMuted) {
                                        service?.setMuted(false)
                                    } else {
                                        service?.speechManager?.triggerCommandMode()
                                    }
                                }
                            },
                            onSendText = { text ->
                                service?.handleIncomingCommand(text)
                            },
                            onConfirmAction = {
                                service?.handleIncomingCommand("yes")
                            },
                            onCancelAction = {
                                service?.handleIncomingCommand("no")
                            },
                            onCancelTask = {
                                service?.cancelCurrentTask()
                            },
                            onTriggerScreenVision = {
                                service?.handleIncomingCommand("kya tum ye dekh rahe ho")
                            },
                            onInputLanguageChange = { lang ->
                                service?.updateInputLanguage(lang)
                            },
                            onOutputLanguageChange = { lang ->
                                service?.updateOutputLanguage(lang)
                            },
                            onPitchChange = { pitch ->
                                service?.updateVoicePitch(pitch)
                            },
                            onSpeedChange = { speed ->
                                service?.updateVoiceSpeed(speed)
                            },
                            onTestVoice = {
                                service?.testVoice()
                            },
                            onStopSpeaking = {
                                service?.stopSpeaking()
                            },
                            onToggleShortWakeWord = { allow ->
                                service?.updateAllowShortWakeWord(allow)
                            },
                            onToggleMuteListeningBeep = { mute ->
                                service?.updateMuteListeningBeep(mute)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startAndBindAssistantService() {
        MaxForegroundService.startService(this)
        val intent = Intent(this, MaxForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, MaxForegroundService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentCommand(intent)
    }

    private fun handleIntentCommand(intent: Intent?) {
        val command = intent?.getStringExtra("EXTRA_COMMAND")
        if (!command.isNullOrBlank()) {
            android.util.Log.i("MainActivity", "handleIntentCommand received: $command")
            val service = foregroundService ?: MaxForegroundService.activeInstance
            if (service != null) {
                service.handleIncomingCommand(command)
            } else {
                android.util.Log.w("MainActivity", "Service not yet bound, queuing command: $command")
                pendingIntentCommand = command
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeInstance = this
        val svc = foregroundService ?: MaxForegroundService.activeInstance
        if (svc != null) {
            if (svc.serviceController.isFullyShutdown()) {
                svc.serviceController.reactivate(svc.voiceEngine.settings.value.outputLanguage)
            }
            if (svc.speechManager.isMuted) {
                svc.setMuted(false)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }
}
