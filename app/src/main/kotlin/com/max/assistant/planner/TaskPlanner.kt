package com.max.assistant.planner

import android.util.Log
import com.max.assistant.ai.BrainDecision
import com.max.assistant.tts.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StepStatus {
    PROPOSED,
    AWAITING_CONFIRMATION,
    EXECUTING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class PlannedStep(
    val stepIndex: Int,
    val description: String,
    val decision: BrainDecision,
    var status: StepStatus = StepStatus.PROPOSED,
    var resultMessage: String = "",
    val requiresConfirmation: Boolean = false
)

data class MultiStepPlan(
    val planId: String,
    val goal: String,
    val steps: List<PlannedStep>,
    var currentStepIndex: Int = 0,
    var isCompleted: Boolean = false
)

class TaskPlanner {

    companion object {
        private const val TAG = "TaskPlanner"
    }

    private val _activePlan = MutableStateFlow<MultiStepPlan?>(null)
    val activePlan: StateFlow<MultiStepPlan?> = _activePlan.asStateFlow()

    fun createPlan(goal: String, steps: List<PlannedStep>): MultiStepPlan {
        val plan = MultiStepPlan(
            planId = "plan_${System.currentTimeMillis()}",
            goal = goal,
            steps = steps,
            currentStepIndex = 0
        )
        _activePlan.value = plan
        Log.i(TAG, "Created Multi-Step Plan for goal: '$goal' with ${steps.size} steps.")
        return plan
    }

    fun getNextStep(): PlannedStep? {
        val plan = _activePlan.value ?: return null
        if (plan.currentStepIndex < plan.steps.size) {
            return plan.steps[plan.currentStepIndex]
        }
        plan.isCompleted = true
        return null
    }

    fun markStepExecuting(stepIndex: Int) {
        val plan = _activePlan.value ?: return
        val step = plan.steps.getOrNull(stepIndex) ?: return
        step.status = StepStatus.EXECUTING
    }

    fun markStepSuccess(stepIndex: Int, message: String) {
        val plan = _activePlan.value ?: return
        val step = plan.steps.getOrNull(stepIndex) ?: return
        step.status = StepStatus.SUCCEEDED
        step.resultMessage = message
        plan.currentStepIndex++
        if (plan.currentStepIndex >= plan.steps.size) {
            plan.isCompleted = true
            Log.i(TAG, "Multi-Step Plan '${plan.goal}' completed successfully.")
        }
    }

    fun markStepFailed(stepIndex: Int, error: String) {
        val plan = _activePlan.value ?: return
        val step = plan.steps.getOrNull(stepIndex) ?: return
        step.status = StepStatus.FAILED
        step.resultMessage = error
        // Mark remaining steps as cancelled
        for (i in (stepIndex + 1) until plan.steps.size) {
            plan.steps[i].status = StepStatus.CANCELLED
        }
        plan.isCompleted = true
        Log.w(TAG, "Multi-Step Plan '${plan.goal}' halted at step $stepIndex: $error")
    }

    fun cancelActivePlan(reason: String = "User cancelled plan"): String {
        val plan = _activePlan.value ?: return ""
        for (step in plan.steps) {
            if (step.status == StepStatus.PROPOSED || step.status == StepStatus.AWAITING_CONFIRMATION || step.status == StepStatus.EXECUTING) {
                step.status = StepStatus.CANCELLED
                step.resultMessage = reason
            }
        }
        plan.isCompleted = true
        _activePlan.value = null
        Log.i(TAG, "Cancelled multi-step plan: '${plan.goal}'")
        return "Plan '${plan.goal}' was cancelled."
    }

    fun clear() {
        _activePlan.value = null
    }
}
