package com.max.assistant.hud

import com.max.assistant.speech.AssistantState
import java.util.UUID

/**
 * Category of real-time activity events performed by MAX.
 */
enum class ActivityCategory {
    VOICE,
    REASONING,
    EXECUTION,
    VISION,
    SYSTEM,
    SECURITY
}

/**
 * Status indicator for an activity event.
 */
enum class EventStatus {
    RUNNING,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Chronological real-time activity event logged by MAX during operation.
 */
data class ActivityEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val title: String,
    val detail: String = "",
    val category: ActivityCategory = ActivityCategory.SYSTEM,
    val status: EventStatus = EventStatus.SUCCESS
)

/**
 * Real-time state of an ongoing automated task or command execution.
 */
data class ActiveTaskState(
    val taskId: String = UUID.randomUUID().toString(),
    val taskName: String,
    val recognizedCommand: String,
    val reasoningSummary: String,
    val targetApp: String? = null,
    val currentStep: String,
    val progressPercent: Int? = null,
    val toolUsed: String? = null,
    val isCancellable: Boolean = true
)

/**
 * Live multimodal Screen Vision telemetry and analysis state.
 */
data class ScreenVisionUiState(
    val isActive: Boolean = false,
    val captureStatus: String = "Standby",
    val appName: String = "Home Screen",
    val extractedTextSummary: String = "",
    val identifiedElementsCount: Int = 0,
    val identifiedElements: List<String> = emptyList(),
    val maxInterpretation: String = "",
    val suggestedActions: List<String> = emptyList(),
    val lastExecutedAction: String? = null
)

/**
 * Real-time hardware, network, and subsystem telemetry for the JARVIS HUD.
 */
data class SystemTelemetryInfo(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "Wi-Fi",
    val isOnline: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val micActive: Boolean = true,
    val serviceRunning: Boolean = true,
    val aiProviderStatus: String = "Connected",
    val currentTime: String = "12:00 PM",
    val appVersion: String = "v2.1.0"
)
