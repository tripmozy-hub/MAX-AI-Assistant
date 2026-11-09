package com.max.assistant.healing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.max.assistant.database.HealthIncidentRecord
import com.max.assistant.database.MaxDatabaseHelper
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class ComponentStatus {
    HEALTHY,
    DEGRADED,
    RECOVERING,
    FAILED
}

enum class MonitoredComponent(val tag: String) {
    SPEECH_RECOGNIZER("SPEECH_RECOGNIZER"),
    MICROPHONE("MICROPHONE"),
    GEMINI_API("GEMINI_API"),
    ACCESSIBILITY("ACCESSIBILITY"),
    SCREEN_VISION("SCREEN_VISION"),
    NETWORK("NETWORK"),
    FOREGROUND_SERVICE("FOREGROUND_SERVICE"),
    DATABASE("DATABASE")
}

data class ComponentHealth(
    val name: String,
    var status: ComponentStatus = ComponentStatus.HEALTHY,
    var failureCount: Int = 0,
    var lastFailureEpoch: Long = 0L,
    var lastRecoveryEpoch: Long = 0L,
    var backoffDelayMs: Long = 1000L
)

class MaxHealthMonitor(private val context: Context) {

    companion object {
        private const val TAG = "MaxHealthMonitor"
        private const val MAX_CONSECUTIVE_RETRIES = 3
        private const val RESET_WINDOW_MS = 60_000L

        const val COMPONENT_SPEECH = "SPEECH_RECOGNIZER"
        const val COMPONENT_MIC = "MICROPHONE"
        const val COMPONENT_GEMINI = "GEMINI_API"
        const val COMPONENT_ACCESSIBILITY = "ACCESSIBILITY"
        const val COMPONENT_VISION = "SCREEN_VISION"
        const val COMPONENT_NETWORK = "NETWORK"
        const val COMPONENT_FOREGROUND = "FOREGROUND_SERVICE"
        const val COMPONENT_DATABASE = "DATABASE"

        @Volatile
        private var instance: MaxHealthMonitor? = null

        fun getInstance(context: Context): MaxHealthMonitor {
            return instance ?: synchronized(this) {
                instance ?: MaxHealthMonitor(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dbHelper = MaxDatabaseHelper.getInstance(context)
    private val componentMap = ConcurrentHashMap<String, ComponentHealth>()

    private val _systemHealthStatus = MutableStateFlow(ComponentStatus.HEALTHY)
    val systemHealthStatus: StateFlow<ComponentStatus> = _systemHealthStatus.asStateFlow()

    init {
        listOf(
            COMPONENT_SPEECH, COMPONENT_MIC, COMPONENT_GEMINI,
            COMPONENT_ACCESSIBILITY, COMPONENT_VISION, COMPONENT_NETWORK,
            COMPONENT_FOREGROUND, COMPONENT_DATABASE
        ).forEach { name ->
            componentMap[name] = ComponentHealth(name)
        }
    }

    // ==================== INCIDENT LOGGING & CLASSIFICATION ====================

    fun reportIncident(
        component: MonitoredComponent,
        errorType: String,
        message: String,
        severity: String = "MEDIUM",
        recoveryLevel: Int = 1
    ): Int {
        return recordIncident(component.tag, errorType, message, severity, recoveryLevel)
    }

    fun reportIncident(
        component: String,
        errorType: String,
        message: String,
        severity: String = "MEDIUM",
        recoveryLevel: Int = 1
    ): Int {
        return recordIncident(component, errorType, message, severity, recoveryLevel)
    }

    fun recordIncident(
        component: String,
        errorType: String,
        message: String,
        severity: String = "MEDIUM",
        recoveryLevel: Int = 1
    ): Int {
        val now = System.currentTimeMillis()
        val comp = componentMap.getOrPut(component) { ComponentHealth(component) }

        if (now - comp.lastFailureEpoch > RESET_WINDOW_MS) {
            comp.failureCount = 0
            comp.backoffDelayMs = 1000L
        }

        comp.failureCount++
        comp.lastFailureEpoch = now
        comp.status = if (comp.failureCount >= MAX_CONSECUTIVE_RETRIES) ComponentStatus.FAILED else ComponentStatus.DEGRADED

        comp.backoffDelayMs = (1000L * (1 shl (comp.failureCount - 1).coerceAtMost(5))).coerceAtMost(30_000L)

        val resolution = when {
            comp.failureCount >= MAX_CONSECUTIVE_RETRIES -> "ACTION_REQUIRED"
            recoveryLevel == 1 -> "RETRYING"
            else -> "ACTION_REQUIRED"
        }

        val incident = HealthIncidentRecord(
            component = component,
            errorType = errorType,
            severity = severity,
            message = message,
            recoveryLevel = recoveryLevel,
            resolution = resolution,
            timestamp = now
        )

        dbHelper.insertHealthIncident(incident)
        Log.w(TAG, "Health Incident [$component][$severity]: $errorType - $message (FailCount=${comp.failureCount}, Level=$recoveryLevel)")

        updateGlobalHealth()
        return recoveryLevel
    }

    fun recordRecovery(component: String) {
        val comp = componentMap[component] ?: return
        comp.status = ComponentStatus.HEALTHY
        comp.failureCount = 0
        comp.backoffDelayMs = 1000L
        comp.lastRecoveryEpoch = System.currentTimeMillis()
        Log.i(TAG, "Component '$component' self-healed and restored to HEALTHY state.")
        updateGlobalHealth()
    }

    fun recordRecovery(component: MonitoredComponent) {
        recordRecovery(component.tag)
    }

    private fun updateGlobalHealth() {
        val statuses = componentMap.values.map { it.status }
        _systemHealthStatus.value = when {
            statuses.any { it == ComponentStatus.FAILED } -> ComponentStatus.FAILED
            statuses.any { it == ComponentStatus.DEGRADED || it == ComponentStatus.RECOVERING } -> ComponentStatus.DEGRADED
            else -> ComponentStatus.HEALTHY
        }
    }

    fun canAttemptAutomaticRecovery(component: String): Boolean {
        val comp = componentMap[component] ?: return true
        val now = System.currentTimeMillis()
        if (now - comp.lastFailureEpoch > RESET_WINDOW_MS) {
            comp.failureCount = 0
            return true
        }
        return comp.failureCount <= MAX_CONSECUTIVE_RETRIES
    }

    fun getBackoffDelayMs(component: String): Long {
        return componentMap[component]?.backoffDelayMs ?: 1000L
    }

    // ==================== SYSTEM CAPABILITY CHECKS ====================

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(network) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    // ==================== LEVEL 2: ASSISTED RECOVERY GUIDANCE ====================

    fun getAssistedRecoveryGuidance(component: String, language: VoiceLanguage): String {
        return when (component) {
            COMPONENT_ACCESSIBILITY -> when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, screen control ke liye MAX Accessibility Service disabled hai. Kripya Accessibility settings mein jakar MAX ko enable karein."
                VoiceLanguage.BENGALI -> "স্যার, স্ক্রিন নিয়ন্ত্রণের জন্য অ্যাক্সেসিবিলিটি সার্ভিস চালু করা প্রয়োজন। অনুগ্রহ করে সেটিংস থেকে MAX অন করুন।"
                else -> "Sir, MAX Accessibility Service is currently disabled. Please enable it in Settings for full device interaction."
            }
            COMPONENT_MIC -> when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, microphone permission blocked hai ya mic busy hai. App settings mein jakar microphone permission check karein."
                VoiceLanguage.BENGALI -> "স্যার, মাইক্রোফোন অনুমতি ব্লক করা আছে। অনুগ্রহ করে সেটিংস থেকে অনুমতি চেক করুন।"
                else -> "Sir, microphone access is blocked or occupied by another app. Please verify permissions in Settings."
            }
            COMPONENT_NETWORK -> when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, internet connection unavailable hai. Main offline mode mein aapki basic commands process kar raha hoon."
                VoiceLanguage.BENGALI -> "স্যার, ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না। অফলাইন মোডে মৌলিক কমান্ড কার্যকর করা হচ্ছে।"
                else -> "Sir, internet is currently unavailable. MAX is operating with local offline heuristics."
            }
            else -> when (language) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> "Sir, $component mein samasya aayi hai. Main restart karke restore karne ki koshish kar raha hoon."
                VoiceLanguage.BENGALI -> "স্যার, $component মডিউলে একটি সমস্যা দেখা দিয়েছে। রিস্টার্ট করে ঠিক করা হচ্ছে।"
                else -> "Sir, an issue occurred in $component. Attempting automated recovery."
            }
        }
    }

    fun getHealthSummary(language: VoiceLanguage): String {
        val health = _systemHealthStatus.value
        val healthyCount = componentMap.values.count { it.status == ComponentStatus.HEALTHY }
        val totalCount = componentMap.size

        return when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                when (health) {
                    ComponentStatus.HEALTHY -> "Sir, MAX ke sabhi $totalCount components bilkul healthy aur active hain. Network aur database normal respond kar rahe hain."
                    ComponentStatus.DEGRADED -> "Sir, system $healthyCount/$totalCount healthy hai. Kuch components retry backoff par hain par core services active hain."
                    ComponentStatus.FAILED -> "Sir, attention needed. Ek ya zyada components fail hue hain. Main recovery protocol run kar raha hoon."
                    else -> "Sir, system normal chal raha hai."
                }
            }
            VoiceLanguage.BENGALI -> {
                "স্যার, সিস্টেম অবস্থা: $health ($healthyCount/$totalCount উপাদান কার্যকর)।"
            }
            else -> {
                when (health) {
                    ComponentStatus.HEALTHY -> "Sir, all $totalCount subsystem components are fully healthy and operational. Network, speech, and database are running cleanly."
                    ComponentStatus.DEGRADED -> "Sir, system is degraded ($healthyCount/$totalCount healthy). Automatic self-healing recovery is actively stabilizing components."
                    ComponentStatus.FAILED -> "Sir, attention is required. Critical component failures detected. Self-healing safe recovery initiated."
                    else -> "Sir, system status is $health."
                }
            }
        }
    }

    fun runSelfDiagnostics(language: VoiceLanguage): String {
        val net = isNetworkAvailable()
        val battery = isBatteryOptimizationIgnored()
        val report = generateDiagnosticReport()
        Log.i(TAG, "Self diagnostics executed:\n$report")

        return when (language) {
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> {
                "Sir, self-diagnostics complete. Network connection: ${if (net) "Active" else "Offline"}. Battery optimization: ${if (battery) "Unrestricted" else "Restricted"}. Overall status: ${_systemHealthStatus.value.name}."
            }
            VoiceLanguage.BENGALI -> {
                "স্যার, ডায়াগনস্টিক সম্পূর্ণ। নেটওয়ার্ক: ${if (net) "সক্রিয়" else "অফলাইন"}। সামগ্রিক অবস্থা: ${_systemHealthStatus.value.name}।"
            }
            else -> {
                "Sir, self-diagnostics completed. Internet: ${if (net) "Online" else "Offline"}. Battery optimization: ${if (battery) "Unrestricted" else "Restricted"}. Overall health: ${_systemHealthStatus.value.name}."
            }
        }
    }

    // ==================== LEVEL 3: DEVELOPER DIAGNOSTICS REPORT ====================

    fun generateDiagnosticReport(): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sb.append("=== MAX AI ASSISTANT DIAGNOSTIC REPORT ===\n")
        sb.append("Generated at: ${sdf.format(Date())}\n")
        sb.append("Overall Health: ${_systemHealthStatus.value.name}\n")
        sb.append("Network Available: ${isNetworkAvailable()}\n")
        sb.append("Battery Optimization Ignored: ${isBatteryOptimizationIgnored()}\n\n")

        sb.append("-- Component Health Matrix --\n")
        componentMap.values.forEach { c ->
            sb.append("- ${c.name.padEnd(20)}: ${c.status.name.padEnd(10)} (Failures: ${c.failureCount}, Backoff: ${c.backoffDelayMs}ms)\n")
        }

        sb.append("\n-- Recent Incidents (Last 10) --\n")
        val incidents = dbHelper.getRecentIncidents(10)
        if (incidents.isEmpty()) {
            sb.append("No recorded incidents. System running cleanly.\n")
        } else {
            incidents.forEach { inc ->
                val timeStr = sdf.format(Date(inc.timestamp))
                sb.append("[$timeStr] [${inc.severity}] ${inc.component} | ${inc.errorType}: ${inc.message} (Level ${inc.recoveryLevel} -> ${inc.resolution})\n")
            }
        }

        return sb.toString().trim()
    }
}
