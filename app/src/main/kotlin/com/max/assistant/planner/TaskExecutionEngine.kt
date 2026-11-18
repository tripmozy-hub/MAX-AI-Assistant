package com.max.assistant.planner

import android.content.Context
import android.util.Log
import com.max.assistant.accessibility.AndroidAccessibilityController
import com.max.assistant.appcontrol.AppInteractionManager
import com.max.assistant.launcher.AppLauncher
import com.max.assistant.protection.GalleryProtectionManager
import com.max.assistant.protection.PaymentProtectionManager
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.delay

sealed class ExecutionStepType {
    data class LaunchApp(val appQuery: String) : ExecutionStepType()
    data class ClickElement(val targetTextOrId: String, val isExact: Boolean = false) : ExecutionStepType()
    data class ClickNthOption(val n: Int) : ExecutionStepType()
    data class TypeText(val text: String) : ExecutionStepType()
    object ScrollDown : ExecutionStepType()
    object ScrollUp : ExecutionStepType()
    object PressBack : ExecutionStepType()
    data class WaitAndVerify(val conditionName: String, val timeoutMs: Long = 2000L) : ExecutionStepType()
}

data class ExecutionStep(
    val id: Int,
    val description: String,
    val action: ExecutionStepType,
    var status: StepStatus = StepStatus.PROPOSED,
    var attempts: Int = 0,
    val maxAttempts: Int = 3
)

data class ExecutionPlan(
    val title: String,
    val steps: MutableList<ExecutionStep>,
    var currentStepIndex: Int = 0,
    var isCancelled: Boolean = false,
    var isDone: Boolean = false
)

class TaskExecutionEngine(
    private val context: Context,
    val accessibility: AndroidAccessibilityController = AndroidAccessibilityController(context),
    val appInteraction: AppInteractionManager = AppInteractionManager(context),
    val appLauncher: AppLauncher = AppLauncher(context)
) {

    companion object {
        private const val TAG = "TaskExecutionEngine"
    }

    private var activePlan: ExecutionPlan? = null

    fun isPlanActive(): Boolean = activePlan != null && !activePlan!!.isDone && !activePlan!!.isCancelled

    fun getActivePlanProgress(): String? {
        val plan = activePlan ?: return null
        return "Executing plan '${plan.title}': step ${plan.currentStepIndex + 1} of ${plan.steps.size} (${plan.steps.getOrNull(plan.currentStepIndex)?.description ?: "in progress"})."
    }

    fun cancelActivePlan(): String {
        val plan = activePlan
        return if (plan != null && isPlanActive()) {
            plan.isCancelled = true
            plan.isDone = true
            activePlan = null
            "Active task '${plan.title}' has been cancelled, Sir."
        } else {
            "There is no active multi-step task running right now, Sir."
        }
    }

    suspend fun executePlan(
        plan: ExecutionPlan,
        onProgressUpdate: ((String) -> Unit)? = null
    ): Pair<Boolean, String> {
        activePlan = plan
        Log.i(TAG, "Starting multi-step execution plan: '${plan.title}' with ${plan.steps.size} steps.")

        for (i in plan.steps.indices) {
            if (plan.isCancelled) {
                return Pair(false, "Task '${plan.title}' was cancelled by user.")
            }

            plan.currentStepIndex = i
            val step = plan.steps[i]
            step.status = StepStatus.EXECUTING
            onProgressUpdate?.invoke(step.description)

            var stepSucceeded = false
            while (step.attempts < step.maxAttempts && !stepSucceeded && !plan.isCancelled) {
                step.attempts++
                Log.d(TAG, "Executing Step ${step.id}: ${step.description} (Attempt ${step.attempts}/${step.maxAttempts})")

                stepSucceeded = executeSingleStep(step.action)
                if (!stepSucceeded) {
                    delay(800L) // Wait before retry
                }
            }

            if (!stepSucceeded) {
                step.status = StepStatus.FAILED
                plan.isDone = true
                Log.w(TAG, "Step ${step.id} failed after ${step.attempts} attempts: ${step.description}")
                return Pair(false, "Could not complete step: ${step.description}. The target element was not responsive.")
            } else {
                step.status = StepStatus.SUCCEEDED
                delay(600L) // Small settling delay between steps
            }
        }

        plan.isDone = true
        Log.i(TAG, "Execution plan '${plan.title}' completed successfully!")
        return Pair(true, "Task '${plan.title}' completed successfully, Sir.")
    }

    private suspend fun executeSingleStep(action: ExecutionStepType): Boolean {
        return when (action) {
            is ExecutionStepType.LaunchApp -> {
                if (GalleryProtectionManager.shouldBlockVoiceLaunch(action.appQuery)) {
                    Log.w(TAG, "Blocked gallery launch step in execution plan.")
                    return false
                }
                if (PaymentProtectionManager.isPaymentRequest(action.appQuery)) {
                    Log.w(TAG, "Blocked payment launch step in execution plan: ${action.appQuery}")
                    return false
                }
                val res = appLauncher.launchAppByVoice(action.appQuery)
                delay(1200L)
                res !is com.max.assistant.launcher.LaunchResult.NotFound &&
                        res !is com.max.assistant.launcher.LaunchResult.Blocked
            }
            is ExecutionStepType.ClickElement -> {
                if (PaymentProtectionManager.isPaymentAction(action.targetTextOrId)) {
                    Log.w(TAG, "Blocked payment element click in execution plan: ${action.targetTextOrId}")
                    return false
                }
                accessibility.clickByText(action.targetTextOrId, action.isExact) ||
                        accessibility.clickByDescription(action.targetTextOrId, action.isExact) ||
                        accessibility.clickByViewId(action.targetTextOrId)
            }
            is ExecutionStepType.ClickNthOption -> {
                accessibility.clickNthOption(action.n)
            }
            is ExecutionStepType.TypeText -> {
                accessibility.typeText(action.text)
            }
            is ExecutionStepType.ScrollDown -> {
                accessibility.scrollForward()
            }
            is ExecutionStepType.ScrollUp -> {
                accessibility.scrollBackward()
            }
            is ExecutionStepType.PressBack -> {
                accessibility.pressBack()
            }
            is ExecutionStepType.WaitAndVerify -> {
                delay(action.timeoutMs)
                !accessibility.detectLoading()
            }
        }
    }

    /**
     * Translates natural language compound tasks into multi-step execution plans.
     */
    fun createPlanForQuery(query: String): ExecutionPlan? {
        val lower = query.lowercase().trim()

        // Privacy check
        if (GalleryProtectionManager.shouldBlockVoiceLaunch(query)) {
            return null
        }

        // Financial Payment check
        if (PaymentProtectionManager.isPaymentRequest(query)) {
            Log.w(TAG, "Rejected execution plan for payment request: $query")
            return null
        }

        // 1. YouTube multi-step task: "open youtube search for ... and play"
        if (lower.contains("youtube") && (lower.contains("search") || lower.contains("play") || lower.contains("chalao") || lower.contains("chala do") || lower.contains("bajao") || lower.contains("song") || lower.contains("gaana"))) {
            val searchTerm = query.replace(Regex("(?i)^(.*?(search for|play|find|kholo|dhoondo|khojo))\\s+"), "")
                .replace(Regex("(?i)(and play.*|video.*|on youtube.*|chalao.*|chala do.*|bajao.*)"), "")
                .trim()
            val cleanSearch = if (searchTerm.isNotBlank()) searchTerm else "Bengali travel documentary"

            return ExecutionPlan(
                title = "Search & Play YouTube video",
                steps = mutableListOf(
                    ExecutionStep(1, "Open YouTube application", ExecutionStepType.LaunchApp("youtube")),
                    ExecutionStep(2, "Click search button", ExecutionStepType.ClickElement("Search")),
                    ExecutionStep(3, "Enter search query '$cleanSearch'", ExecutionStepType.TypeText(cleanSearch)),
                    ExecutionStep(4, "Wait for video results", ExecutionStepType.WaitAndVerify("Search results loaded", 1500L)),
                    ExecutionStep(5, "Play first video result", ExecutionStepType.ClickNthOption(2))
                )
            )
        }

        // 2. Chrome multi-step search & open: "open chrome search for ... and open first result"
        if (lower.contains("chrome") && (lower.contains("search") || lower.contains("open") || lower.contains("kholo") || lower.contains("khojo"))) {
            val searchTerm = query.replace(Regex("(?i)^(.*?(search for|search|dhoondo|khojo))\\s+"), "")
                .replace(Regex("(?i)(and open.*|first result.*|in chrome.*)"), "")
                .trim()
            val cleanSearch = if (searchTerm.isNotBlank()) searchTerm else "Darjeeling hotels"

            return ExecutionPlan(
                title = "Search in Chrome and open first result",
                steps = mutableListOf(
                    ExecutionStep(1, "Open Chrome browser", ExecutionStepType.LaunchApp("chrome")),
                    ExecutionStep(2, "Type search query '$cleanSearch'", ExecutionStepType.TypeText(cleanSearch)),
                    ExecutionStep(3, "Wait for search results", ExecutionStepType.WaitAndVerify("Google search loaded", 2000L)),
                    ExecutionStep(4, "Click first organic search result", ExecutionStepType.ClickNthOption(2))
                )
            )
        }

        // 3. Settings Bluetooth multi-step
        if (lower.contains("bluetooth") && (lower.contains("turn on") || lower.contains("enable") || lower.contains("chalu") || lower.contains("on karo"))) {
            return ExecutionPlan(
                title = "Enable Bluetooth in Settings",
                steps = mutableListOf(
                    ExecutionStep(1, "Open Bluetooth settings", ExecutionStepType.LaunchApp("settings")),
                    ExecutionStep(2, "Click Bluetooth menu", ExecutionStepType.ClickElement("Bluetooth")),
                    ExecutionStep(3, "Toggle Bluetooth switch on", ExecutionStepType.ClickElement("Off"))
                )
            )
        }

        // 4. Settings Wi-Fi multi-step
        if ((lower.contains("wifi") || lower.contains("wi-fi")) && (lower.contains("turn on") || lower.contains("enable") || lower.contains("chalu") || lower.contains("on karo"))) {
            return ExecutionPlan(
                title = "Enable Wi-Fi in Settings",
                steps = mutableListOf(
                    ExecutionStep(1, "Open Wi-Fi settings", ExecutionStepType.LaunchApp("settings")),
                    ExecutionStep(2, "Click Wi-Fi menu", ExecutionStepType.ClickElement("Wi-Fi")),
                    ExecutionStep(3, "Toggle Wi-Fi switch on", ExecutionStepType.ClickElement("Off"))
                )
            )
        }

        // 5. WhatsApp multi-step: "open whatsapp search for X and open chat"
        if (lower.contains("whatsapp") && (lower.contains("chat") || lower.contains("open") || lower.contains("kholo"))) {
            val contact = query.replace(Regex("(?i)^(.*?(chat of|chat|kholo|open))\\s+"), "")
                .replace(Regex("(?i)(ka chat.*|ki chat.*|on whatsapp.*)"), "")
                .trim()
            val cleanContact = if (contact.isNotBlank()) contact else "Rahul"

            return ExecutionPlan(
                title = "Open WhatsApp chat with $cleanContact",
                steps = mutableListOf(
                    ExecutionStep(1, "Open WhatsApp application", ExecutionStepType.LaunchApp("whatsapp")),
                    ExecutionStep(2, "Click search contact", ExecutionStepType.ClickElement("Search")),
                    ExecutionStep(3, "Type contact name '$cleanContact'", ExecutionStepType.TypeText(cleanContact)),
                    ExecutionStep(4, "Wait for contact match", ExecutionStepType.WaitAndVerify("Chat match loaded", 1000L)),
                    ExecutionStep(5, "Open contact chat", ExecutionStepType.ClickElement(cleanContact, isExact = false))
                )
            )
        }

        return null
    }
}
