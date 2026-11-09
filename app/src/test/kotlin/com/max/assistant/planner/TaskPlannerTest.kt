package com.max.assistant.planner

import com.max.assistant.ai.BrainDecision
import com.max.assistant.tts.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {

    private val planner = TaskPlanner()

    @Test
    fun testCreateAndCompletePlan() {
        val step1 = PlannedStep(
            stepIndex = 0,
            description = "Search nearby pharmacy",
            decision = BrainDecision.SearchNearby("pharmacy", VoiceLanguage.ENGLISH)
        )
        val step2 = PlannedStep(
            stepIndex = 1,
            description = "Open directions to pharmacy",
            decision = BrainDecision.OpenDirections("pharmacy", VoiceLanguage.ENGLISH)
        )

        val plan = planner.createPlan("Find pharmacy and navigate", listOf(step1, step2))
        assertNotNull(plan)
        assertEquals(2, plan.steps.size)
        assertEquals(0, plan.currentStepIndex)
        assertEquals(StepStatus.PROPOSED, plan.steps[0].status)

        // Step 1 execution
        val next1 = planner.getNextStep()
        assertNotNull(next1)
        assertEquals(0, next1?.stepIndex)

        planner.markStepExecuting(0)
        assertEquals(StepStatus.EXECUTING, plan.steps[0].status)

        planner.markStepSuccess(0, "Found nearby pharmacy")
        assertEquals(StepStatus.SUCCEEDED, plan.steps[0].status)
        assertEquals(1, plan.currentStepIndex)

        // Step 2 execution
        val next2 = planner.getNextStep()
        assertNotNull(next2)
        assertEquals(1, next2?.stepIndex)

        planner.markStepSuccess(1, "Navigation opened")
        assertEquals(StepStatus.SUCCEEDED, plan.steps[1].status)
        assertTrue(plan.isCompleted)
        assertNull(planner.getNextStep())
    }

    @Test
    fun testPlanFailureHaltsSubsequentSteps() {
        val step1 = PlannedStep(0, "Step 1", BrainDecision.QueryBattery)
        val step2 = PlannedStep(1, "Step 2", BrainDecision.QueryNetwork)
        val step3 = PlannedStep(2, "Step 3", BrainDecision.CheckUpdates)

        val plan = planner.createPlan("Multi-task pipeline", listOf(step1, step2, step3))

        planner.markStepFailed(0, "Simulated device error")
        assertEquals(StepStatus.FAILED, plan.steps[0].status)
        assertEquals(StepStatus.CANCELLED, plan.steps[1].status)
        assertEquals(StepStatus.CANCELLED, plan.steps[2].status)
        assertTrue(plan.isCompleted)
    }

    @Test
    fun testCancelActivePlan() {
        val step1 = PlannedStep(0, "Step 1", BrainDecision.QueryBattery)
        val step2 = PlannedStep(1, "Step 2", BrainDecision.QueryNetwork)

        val plan = planner.createPlan("Cancelable plan", listOf(step1, step2))
        planner.markStepExecuting(0)

        val cancelMsg = planner.cancelActivePlan("User cancelled")
        assertTrue(cancelMsg.contains("Cancelable plan"))
        assertEquals(StepStatus.CANCELLED, plan.steps[0].status)
        assertEquals(StepStatus.CANCELLED, plan.steps[1].status)
        assertTrue(plan.isCompleted)
        assertNull(planner.activePlan.value)
    }
}
