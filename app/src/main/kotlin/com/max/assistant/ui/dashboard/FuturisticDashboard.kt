package com.max.assistant.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import com.max.assistant.hud.ActivityCategory
import com.max.assistant.hud.ActivityEvent
import com.max.assistant.hud.ActiveTaskState
import com.max.assistant.hud.EventStatus
import com.max.assistant.hud.ScreenVisionUiState
import com.max.assistant.hud.SystemTelemetryInfo
import com.max.assistant.speech.AssistantState
import com.max.assistant.tts.VoiceLanguage
import com.max.assistant.tts.VoiceSettings
import com.max.assistant.ui.settings.VoiceSettingsDialog
import com.max.assistant.workspace.*

/**
 * Modern, Simple, Attractive, and Smooth AI Assistant Dashboard.
 * Optimized for Samsung Galaxy S25 Ultra AMOLED display with smooth fluid physics,
 * glassmorphic floating surfaces, and clean typography.
 */
@Composable
fun FuturisticDashboard(
    assistantState: AssistantState,
    rmsLevel: Float,
    liveTranscript: String,
    isMuted: Boolean,
    isSpeaking: Boolean = false,
    voiceSettings: VoiceSettings = VoiceSettings(),
    showPinDialog: Boolean,
    pendingAction: com.max.assistant.action.PendingAction? = null,
    diagnosticInfo: com.max.assistant.service.SystemDiagnosticInfo? = null,
    recentHistory: List<com.max.assistant.ai.ChatMessage> = emptyList(),
    activityEvents: List<ActivityEvent> = emptyList(),
    activeTaskState: ActiveTaskState? = null,
    screenVisionState: ScreenVisionUiState = ScreenVisionUiState(),
    systemTelemetry: SystemTelemetryInfo = SystemTelemetryInfo(),
    workspaceState: WorkspaceUiState = WorkspaceUiState(),
    onToggleWorkspace: () -> Unit = {},
    onCloseWorkspace: () -> Unit = {},
    onToggleCompactCore: () -> Unit = {},
    onCreateWorkspaceTab: (String) -> Unit = {},
    onSwitchWorkspaceTab: (String) -> Unit = {},
    onCloseWorkspaceTab: (String) -> Unit = {},
    onRenameWorkspaceTab: (String, String) -> Unit = { _, _ -> },
    onSetWorkspaceLayoutMode: (String, WorkspaceLayoutMode) -> Unit = { _, _ -> },
    onCloseWorkspacePanel: (String, String) -> Unit = { _, _ -> },
    onMinimizeWorkspacePanel: (String, String) -> Unit = { _, _ -> },
    onMaximizeWorkspacePanel: (String, String) -> Unit = { _, _ -> },
    onRestoreWorkspacePanel: (String, String) -> Unit = { _, _ -> },
    onUpdateWorkspacePanelPosition: (String, String, Float, Float) -> Unit = { _, _, _, _ -> },
    onPlayWorkspaceVideo: (VideoItem) -> Unit = {},
    onSummarizeWorkspaceVideo: (VideoItem) -> Unit = {},
    onSearchWorkspaceVideos: (String) -> Unit = {},
    onSpeakWorkspaceSummary: (String) -> Unit = {},
    onToggleMute: () -> Unit,
    onStopMax: () -> Unit,
    onVerifyPin: (String) -> Unit,
    onDismissPin: () -> Unit,
    onCoreTapped: () -> Unit,
    onSendText: (String) -> Unit = {},
    onConfirmAction: () -> Unit = {},
    onCancelAction: () -> Unit = {},
    onCancelTask: () -> Unit = {},
    onTriggerScreenVision: () -> Unit = {},
    onInputLanguageChange: (VoiceLanguage) -> Unit = {},
    onOutputLanguageChange: (VoiceLanguage) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onTestVoice: () -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onToggleShortWakeWord: (Boolean) -> Unit = {},
    onToggleMuteListeningBeep: (Boolean) -> Unit = {}
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // 0 = Modern Fluid Orb, 1 = JARVIS HUD (Default), 2 = Holographic Core, 3 = Cyber Silver Helmet
    var visualizerFaceMode by remember { mutableStateOf(1) }

    // Interactive scale animation when tapping center orb
    val orbInteractionSource = remember { MutableInteractionSource() }
    val isOrbPressed by orbInteractionSource.collectIsPressedAsState()
    val orbScale by animateFloatAsState(
        targetValue = if (isOrbPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "orb_tap_scale"
    )

    // Gentle pulsing glow indicator for the status LED
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val statusPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_alpha"
    )

    // Human-readable status mapping and theme colors
    val (statusText, statusColor) = when (assistantState) {
        AssistantState.STOPPED -> Pair("MAX Stopped", Color(0xFF8899AA))
        AssistantState.MUTED -> Pair("Microphone Muted", Color(0xFFFF9900))
        AssistantState.AWAITING_PIN -> Pair("Locked (PIN 1212)", Color(0xFFFFCC00))
        AssistantState.LISTENING_FOR_COMMAND -> Pair("Listening...", Color(0xFF00E5FF))
        AssistantState.THINKING, AssistantState.PROCESSING -> Pair("Thinking...", Color(0xFFBD00FF))
        AssistantState.EXECUTING_COMMAND -> Pair("Executing...", Color(0xFF00E5FF))
        AssistantState.VERIFYING -> Pair("Verifying Outcome...", Color(0xFF00E5FF))
        AssistantState.RECOVERING -> Pair("Self-Healing...", Color(0xFFFF9900))
        AssistantState.SCREEN_SHARING -> Pair("Screen Vision Active", Color(0xFFFF1744))
        AssistantState.COMPLETED -> Pair("Ready", Color(0xFF00E676))
        AssistantState.FAILED -> Pair("Attention Needed", Color(0xFFFF5252))
        AssistantState.SPEAKING -> Pair("Speaking...", Color(0xFF40C4FF))
        else -> Pair("Ready", Color(0xFF00E676))
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(400),
        label = "status_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Smooth AMOLED Aurora Ambient Canvas
        ModernAmbientBackground(state = assistantState)

        // 2. Main Dashboard Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ==================== TOP BAR & HUD HEADER ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branding & Status Indicator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MAX",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "// OS",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    // Glassmorphic Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0C1420).copy(alpha = 0.85f))
                            .border(1.dp, animatedStatusColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(animatedStatusColor.copy(alpha = statusPulseAlpha))
                        )
                        Text(
                            text = statusText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                // Action Pill Buttons: Language, Face Mode, History, Settings, Diagnostics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Language Indicator Pill
                    val langBadge = when (voiceSettings.outputLanguage) {
                        VoiceLanguage.HINGLISH -> "HING"
                        VoiceLanguage.HINDI -> "HI"
                        VoiceLanguage.BENGALI -> "BN"
                        VoiceLanguage.ENGLISH -> "EN"
                        else -> "AUTO"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = langBadge,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Face Switcher Button (Fluid Orb <-> JARVIS HUD <-> Holographic Core <-> Cyber Helmet)
                    IconButton(
                        onClick = { visualizerFaceMode = (visualizerFaceMode + 1) % 4 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Switch Visualizer Face",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Holographic Multi-Window Workspace Button
                    IconButton(
                        onClick = onToggleWorkspace,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (workspaceState.isVisible) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, if (workspaceState.isVisible) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Holographic Workspace",
                            tint = if (workspaceState.isVisible) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Conversation Audit History Button
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Conversation History",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice Settings",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Diagnostics Button
                    IconButton(
                        onClick = { showDiagnosticDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131A26).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "System Diagnostics",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ==================== HUD TELEMETRY BAR ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E17).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery & Power
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (systemTelemetry.isCharging) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery",
                        tint = if (systemTelemetry.batteryPercent > 20) Color(0xFF00E5FF) else Color(0xFFFF5252),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${systemTelemetry.batteryPercent}%${if (systemTelemetry.isCharging) "⚡" else ""}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Network
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Network",
                        tint = if (systemTelemetry.isOnline) Color(0xFF00E676) else Color(0xFFFF9900),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = systemTelemetry.networkType,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Screen Vision Active Pill
                if (screenVisionState.isActive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                        Text(
                            text = "VISION ON",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Current Time
                Text(
                    text = systemTelemetry.currentTime,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ==================== CENTER SECTION: AI ORB ====================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(orbScale)
                        .clickable(
                            interactionSource = orbInteractionSource,
                            indication = null
                        ) { onCoreTapped() },
                    contentAlignment = Alignment.Center
                ) {
                    when (visualizerFaceMode) {
                        0 -> ModernFluidAiOrb(
                            rmsLevel = rmsLevel,
                            state = assistantState
                        )
                        1 -> JarvisHudVisualizer(
                            rmsLevel = rmsLevel,
                            state = assistantState
                        )
                        2 -> HolographicCoreVisualizer(
                            rmsLevel = rmsLevel,
                            state = assistantState
                        )
                        else -> CyberHelmetVisualizer(
                            rmsLevel = rmsLevel,
                            state = assistantState
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Helpful friendly status hint below the orb
                Text(
                    text = when (assistantState) {
                        AssistantState.LISTENING_FOR_COMMAND -> "Listening to your voice, Sir..."
                        AssistantState.THINKING, AssistantState.PROCESSING -> "Processing your request, Sir..."
                        AssistantState.EXECUTING_COMMAND -> "Executing action on device..."
                        AssistantState.VERIFYING -> "Verifying action outcome on screen..."
                        AssistantState.RECOVERING -> "Performing self-healing recovery..."
                        AssistantState.SCREEN_SHARING -> "Screen vision is active (tap to stop)"
                        AssistantState.SPEAKING -> "Tap orb or button to stop speaking"
                        AssistantState.MUTED -> "Microphone is muted. Tap UNMUTE below."
                        AssistantState.STOPPED -> "MAX is stopped. Tap or say wake word."
                        else -> "Tap orb or say 'Hey MAX'"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }

            // ==================== INTERACTIVE CARD / TRANSCRIPT ====================
            // Action Confirmation Card (For calls/messages requiring confirmation)
            if (pendingAction != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F1826).copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (pendingAction) {
                                is com.max.assistant.action.PendingAction.MakeCall -> "Confirm Call to ${pendingAction.target}?"
                                is com.max.assistant.action.PendingAction.SendMessage -> "Confirm Message to ${pendingAction.recipient}?"
                                else -> "Confirm this action, Sir?"
                            },
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelAction,
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.7f))
                            ) {
                                Text("Cancel", color = Color(0xFFFF7777), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = onConfirmAction,
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("Confirm", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==================== SCREEN VISION HUD PANEL ====================
            if (screenVisionState.isActive || assistantState == AssistantState.SCREEN_SHARING) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0A131F).copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = "Vision", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "SCREEN VISION // ${screenVisionState.appName.uppercase()}",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = screenVisionState.captureStatus,
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (screenVisionState.maxInterpretation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = screenVisionState.maxInterpretation,
                                color = Color(0xFFF1F5F9),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (screenVisionState.suggestedActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (action in screenVisionState.suggestedActions) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .clickable { onSendText(action) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = action,
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== LIVE ACTIVE TASK PANEL ====================
            if (activeTaskState != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0B1420).copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Task", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Text(
                                    text = activeTaskState.taskName.uppercase(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (activeTaskState.isCancellable) {
                                Text(
                                    text = "CANCEL",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                        .clickable { onCancelTask() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeTaskState.currentStep} ${if (activeTaskState.toolUsed != null) "• ${activeTaskState.toolUsed}" else ""}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (activeTaskState.progressPercent ?: 50) / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = Color(0xFF1E293B),
                        )
                    }
                }
            }

            // Live Speech Transcript or Recent Assistant Message
            val displayText = when {
                liveTranscript.isNotBlank() -> liveTranscript
                recentHistory.isNotEmpty() -> recentHistory.last().text
                else -> ""
            }

            AnimatedVisibility(
                visible = displayText.isNotBlank(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF111722).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (liveTranscript.isNotBlank()) Icons.Default.Mic else Icons.Default.Chat,
                            contentDescription = "Message",
                            tint = if (liveTranscript.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayText,
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Suggestion Chips (Available when idle for quick 1-tap actions)
            if (assistantState == AssistantState.IDLE || assistantState == AssistantState.COMPLETED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = when (voiceSettings.outputLanguage) {
                        VoiceLanguage.HINGLISH -> listOf(
                            "📚 Study Notes" to "make notes on Indian history",
                            "🎯 Revision Quiz" to "start my revision test",
                            "👁️ Screen Vision" to "kya tum ye dekh rahe ho",
                            "⏰ Set Reminder" to "remind me to check notes tomorrow at 10 am",
                            "🎵 Gaana chalao" to "gana chalao",
                            "📞 Call lagao" to "call lagao",
                            "⏱️ 5 min timer" to "5 minute ka timer lagao",
                            "⛅ Mausam kaisa hai" to "mausam kaisa hai",
                            "❤️ Mood khrb hai" to "mood khrb hai",
                            "⚡ Flashlight on" to "flashlight on karo",
                            "🛑 Standby" to "standby mode"
                        )
                        VoiceLanguage.HINDI -> listOf(
                            "📚 अध्ययन नोट्स" to "भारतीय इतिहास पर नोट्स बनाओ",
                            "🎯 रिवीजन क्विज़" to "क्विज़ शुरू करो",
                            "👁️ स्क्रीन विज़न" to "स्क्रीन देखो",
                            "⏰ रिमाइंडर" to "कल सुबह 10 बजे का रिमाइंडर लगाओ",
                            "📞 कॉल करो" to "कॉल करो",
                            "⏱️ ५ मिनट टाइमर" to "5 मिनट का टाइमर लगाओ",
                            "🎵 गाना चलाओ" to "गाना चलाओ",
                            "🧭 दिशा-निर्देश" to "रास्ता बताओ",
                            "⛅ मौसम" to "मौसम कैसा है",
                            "🛑 स्टैंडबाई" to "स्टैंडबाई मोड"
                        )
                        VoiceLanguage.BENGALI -> listOf(
                            "📚 স্টাডি নোটস" to "ইতিহাসের উপর নোট তৈরি করো",
                            "🎯 রিভিশন কুইজ" to "কুইজ শুরু করো",
                            "👁️ স্ক্রিন ভিশন" to "স্ক্রিন দেখো",
                            "⏰ রিমাইন্ডার" to "আমাকে মনে করিয়ে দাও",
                            "📞 কল করো" to "কল করো",
                            "🎵 গান চালাও" to "গান চালাও",
                            "🧭 রাস্তা দেখাও" to "কাছের হাসপাতালের রাস্তা দেখাও",
                            "🛑 স্ট্যান্ডবাই" to "স্ট্যান্ডবাই"
                        )
                        else -> listOf(
                            "📚 Study Notes" to "make notes on Indian history",
                            "🎯 Revision Quiz" to "start my revision test",
                            "👁️ Screen Vision" to "explain this question on screen",
                            "⏰ Set Reminder" to "remind me to check notes tomorrow at 10 am",
                            "📞 Call" to "call",
                            "⏱️ 5 min timer" to "set timer for 5 minutes",
                            "🎵 Play music" to "play music",
                            "🧭 Directions" to "directions to nearby hospital",
                            "⛅ Weather" to "what is the weather today",
                            "🛑 Standby" to "standby mode"
                        )
                    }
                    for ((label, command) in suggestions) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131A26).copy(alpha = 0.75f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .clickable { onSendText(command) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            // Stop Speaking Quick Pill (When MAX voice is active)
            if (isSpeaking) {
                Button(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Speaking, Sir", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // ==================== BOTTOM FLOATING CONTROL DOCK ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Mute / Unmute Button Pill
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFFF9900).copy(alpha = 0.2f) else Color(0xFF131A26).copy(alpha = 0.9f))
                        .border(1.dp, if (isMuted) Color(0xFFFF9900) else Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) Color(0xFFFF9900) else Color(0xFF00E5FF),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // 2. Screen Vision Quick Trigger Button Pill
                IconButton(
                    onClick = onTriggerScreenVision,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (screenVisionState.isActive) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF131A26).copy(alpha = 0.9f))
                        .border(1.dp, if (screenVisionState.isActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Screen Vision",
                        tint = if (screenVisionState.isActive) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // 2. Modern Rounded Text Input Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = "Ask MAX anything, Sir...",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131A26).copy(alpha = 0.95f),
                        unfocusedContainerColor = Color(0xFF131A26).copy(alpha = 0.85f),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendText(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                // 3. Stop MAX / Power Button Pill
                IconButton(
                    onClick = onStopMax,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (assistantState == AssistantState.STOPPED) Color(0xFF1E293B) else Color(0xFF3B1219).copy(alpha = 0.8f)
                        )
                        .border(
                            1.dp,
                            if (assistantState == AssistantState.STOPPED) Color(0xFF00E5FF) else Color(0xFFFF4D4D).copy(alpha = 0.6f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Stop MAX",
                        tint = if (assistantState == AssistantState.STOPPED) Color(0xFF00E5FF) else Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ==================== DIALOGS ====================

        // Voice Settings Dialog
        if (showSettingsDialog) {
            VoiceSettingsDialog(
                settings = voiceSettings,
                isMuted = isMuted,
                isSpeaking = isSpeaking,
                onDismiss = { showSettingsDialog = false },
                onInputLanguageChange = onInputLanguageChange,
                onOutputLanguageChange = onOutputLanguageChange,
                onPitchChange = onPitchChange,
                onSpeedChange = onSpeedChange,
                onTestVoice = onTestVoice,
                onStopSpeaking = onStopSpeaking,
                onToggleMute = onToggleMute,
                onToggleShortWakeWord = onToggleShortWakeWord,
                onToggleMuteListeningBeep = onToggleMuteListeningBeep
            )
        }

        // System Diagnostics Dialog
        if (showDiagnosticDialog) {
            DiagnosticDialog(
                diagnosticInfo = diagnosticInfo,
                systemTelemetry = systemTelemetry,
                onDismiss = { showDiagnosticDialog = false }
            )
        }

        // Conversation Audit History Dialog
        if (showHistoryDialog) {
            HistoryDialog(
                history = recentHistory,
                activityEvents = activityEvents,
                onDismiss = { showHistoryDialog = false }
            )
        }

        // Security PIN Dialog (PIN: 1212)
        if (showPinDialog || assistantState == AssistantState.AWAITING_PIN) {
            Dialog(onDismissRequest = onDismissPin) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0E1420),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Security Verification",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your security PIN (1212) to activate MAX, Sir.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // PIN Input Display (4 clean dots)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 4) {
                                val isFilled = pinInput.length > i
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFilled) Color(0xFF00E5FF) else Color(0xFF1E293B)
                                        )
                                        .border(
                                            1.dp,
                                            if (isFilled) Color(0xFF00E5FF) else Color(0xFF475569),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 4) {
                                    pinInput = it
                                    pinError = false
                                    if (it == "1212") {
                                        onVerifyPin("1212")
                                        pinInput = ""
                                    }
                                }
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            ),
                            placeholder = {
                                Text(
                                    "Enter PIN",
                                    color = Color(0xFF475569),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinError) {
                            Text(
                                "Incorrect PIN, Sir.",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismissPin,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text("Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if (pinInput == "1212") {
                                        onVerifyPin(pinInput)
                                        pinInput = ""
                                    } else {
                                        pinError = true
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("Unlock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 10. Holographic Multi-Window Workspace Overlay
        AnimatedVisibility(
            visible = workspaceState.isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 4 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { it / 4 }
        ) {
            com.max.assistant.workspace.HolographicWorkspaceView(
                workspaceState = workspaceState,
                onCloseWorkspace = onCloseWorkspace,
                onToggleCompactCore = onToggleCompactCore,
                onCreateTab = onCreateWorkspaceTab,
                onSwitchTab = onSwitchWorkspaceTab,
                onCloseTab = onCloseWorkspaceTab,
                onRenameTab = onRenameWorkspaceTab,
                onSetLayoutMode = onSetWorkspaceLayoutMode,
                onClosePanel = onCloseWorkspacePanel,
                onMinimizePanel = onMinimizeWorkspacePanel,
                onMaximizePanel = onMaximizeWorkspacePanel,
                onRestorePanel = onRestoreWorkspacePanel,
                onUpdatePosition = onUpdateWorkspacePanelPosition,
                onPlayVideo = onPlayWorkspaceVideo,
                onSummarizeVideo = onSummarizeWorkspaceVideo,
                onSearchVideos = onSearchWorkspaceVideos,
                onSpeakSummary = onSpeakWorkspaceSummary
            )
        }
    }
}

@Composable
fun DiagnosticDialog(
    diagnosticInfo: com.max.assistant.service.SystemDiagnosticInfo?,
    systemTelemetry: SystemTelemetryInfo = SystemTelemetryInfo(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0C1420),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "System Diagnostics",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "MAX Subsystem Telemetry // ${systemTelemetry.appVersion}",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                val info = diagnosticInfo ?: com.max.assistant.service.SystemDiagnosticInfo(
                    aiConnectionStatus = systemTelemetry.aiProviderStatus,
                    micPermissionGranted = systemTelemetry.micActive,
                    speechRecognitionAvailable = true,
                    ttsEngineStatus = "Ready",
                    selectedLanguage = "AUTO",
                    accessibilityEnabled = true,
                    commandExecutorStatus = "Ready"
                )

                DiagnosticRow(label = "AI Intelligence", value = systemTelemetry.aiProviderStatus, isGood = true)
                DiagnosticRow(label = "Power Subsystem", value = "${systemTelemetry.batteryPercent}% (${if (systemTelemetry.isCharging) "Charging" else "Discharging"})", isGood = systemTelemetry.batteryPercent > 15)
                DiagnosticRow(label = "Network Status", value = "${systemTelemetry.networkType} (${if (systemTelemetry.isOnline) "Connected" else "Offline"})", isGood = systemTelemetry.isOnline)
                DiagnosticRow(label = "Bluetooth Radio", value = if (systemTelemetry.bluetoothEnabled) "Active" else "Standby", isGood = true)
                DiagnosticRow(label = "Mic & Wake Word", value = if (systemTelemetry.micActive) "Active (Listening)" else "Muted", isGood = systemTelemetry.micActive)
                DiagnosticRow(label = "Accessibility Guard", value = if (info.accessibilityEnabled) "Enabled (Full Automation)" else "Standby (Intents)", isGood = info.accessibilityEnabled)
                DiagnosticRow(label = "Background Engine", value = if (systemTelemetry.serviceRunning) "Online (Foreground Service)" else "Stopped", isGood = systemTelemetry.serviceRunning)
                DiagnosticRow(label = "App Release", value = "${systemTelemetry.appVersion} (Production Signed)", isGood = true)

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Close Diagnostics", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = value,
            color = if (isGood) Color(0xFF00E676) else Color(0xFFFFB74D),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun HistoryDialog(
    history: List<com.max.assistant.ai.ChatMessage>,
    activityEvents: List<ActivityEvent> = emptyList(),
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0C1420),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Chat else Icons.Default.List,
                            contentDescription = "History",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedTab == 0) "Conversation History" else "Live Activity Log",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Dual Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131D2E))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Conversations",
                            color = if (selectedTab == 0) Color.Black else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Activity Log (${activityEvents.size})",
                            color = if (selectedTab == 1) Color.Black else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Chat history
                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No conversation entries logged yet, Sir.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history) { msg ->
                                val isUser = msg.role == com.max.assistant.ai.MessageRole.USER
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isUser) Color(0xFF1E293B) else Color(0xFF131A26))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = if (isUser) "You:" else "MAX:",
                                        color = if (isUser) Color(0xFF00E5FF) else Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(text = msg.text, color = Color(0xFFF1F5F9), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Chronological Activity Log (Phase 5)
                    if (activityEvents.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No activity events logged yet, Sir.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activityEvents) { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F1724))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val catColor = when (ev.category) {
                                        ActivityCategory.VOICE -> Color(0xFF00E5FF)
                                        ActivityCategory.REASONING -> Color(0xFFBD00FF)
                                        ActivityCategory.EXECUTION -> Color(0xFF00E676)
                                        ActivityCategory.VISION -> Color(0xFF00F5D4)
                                        ActivityCategory.SECURITY -> Color(0xFFFF9900)
                                        ActivityCategory.SYSTEM -> Color(0xFF94A3B8)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                            .padding(top = 4.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = ev.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = ev.timestamp,
                                                color = Color(0xFF64748B),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (ev.detail.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = ev.detail,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07090E, widthDp = 390, heightDp = 844)
@Composable
fun FuturisticDashboardPreview() {
    FuturisticDashboard(
        assistantState = AssistantState.IDLE,
        rmsLevel = 0.2f,
        liveTranscript = "At your service, Sir.",
        isMuted = false,
        showPinDialog = false,
        onToggleMute = {},
        onStopMax = {},
        onVerifyPin = {},
        onDismissPin = {},
        onCoreTapped = {}
    )
}
