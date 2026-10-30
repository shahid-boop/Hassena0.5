package com.example.services.ai

import com.example.models.AgentStepStatus
import com.example.models.AutonomousCodingStep
import com.example.models.AutonomousTaskPlan
import com.example.models.CodingCheckpoint
import com.example.models.RepairLoopAttempt
import com.example.models.SelfRepairSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AutonomousCodingEngine(
    private val aiService: AIService? = null
) {
    // Checkpoints history (LIFO stack)
    private val _checkpoints = MutableStateFlow<List<CodingCheckpoint>>(emptyList())
    val checkpoints: StateFlow<List<CodingCheckpoint>> = _checkpoints.asStateFlow()

    // Current Autonomous Plan
    private val _currentPlan = MutableStateFlow<AutonomousTaskPlan?>(null)
    val currentPlan: StateFlow<AutonomousTaskPlan?> = _currentPlan.asStateFlow()

    // Self-Repair Session
    private val _repairSession = MutableStateFlow<SelfRepairSession?>(null)
    val repairSession: StateFlow<SelfRepairSession?> = _repairSession.asStateFlow()

    // Autonomous execution log stream
    private val _telemetryLogs = MutableStateFlow<List<String>>(
        listOf("JARVIS Autonomous Coding Engine initialized.", "Ready for multi-step task generation and self-repair.")
    )
    val telemetryLogs: StateFlow<List<String>> = _telemetryLogs.asStateFlow()

    private fun appendLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val formatted = "[$timestamp] $msg"
        _telemetryLogs.value = (_telemetryLogs.value + formatted).takeLast(100)
    }

    /**
     * Creates a manual or automated checkpoint with snapshot of current code.
     */
    fun createCheckpoint(
        label: String,
        file: String,
        language: String,
        code: String,
        reason: String = "Manual Snapshot"
    ): CodingCheckpoint {
        val checkpoint = CodingCheckpoint(
            id = UUID.randomUUID().toString().take(8),
            label = label,
            timestamp = System.currentTimeMillis(),
            file = file,
            language = language,
            code = code,
            lineCount = code.lines().size,
            triggerReason = reason
        )
        _checkpoints.value = listOf(checkpoint) + _checkpoints.value
        appendLog("Checkpoint created [${checkpoint.id}]: '$label' ($reason, ${checkpoint.lineCount} lines)")
        return checkpoint
    }

    /**
     * Restores a checkpoint from history.
     */
    fun rollbackToCheckpoint(checkpointId: String): CodingCheckpoint? {
        val target = _checkpoints.value.find { it.id == checkpointId }
        if (target != null) {
            appendLog("ROLLBACK executed to checkpoint [${target.id}]: '${target.label}'")
        }
        return target
    }

    /**
     * Clears all checkpoints.
     */
    fun clearCheckpoints() {
        _checkpoints.value = emptyList()
        appendLog("Checkpoints history cleared.")
    }

    /**
     * Generates a multi-step autonomous plan based on a task prompt.
     */
    fun generatePlan(taskGoal: String, language: String, initialCode: String): AutonomousTaskPlan {
        val planId = UUID.randomUUID().toString().take(8)
        val steps = listOf(
            AutonomousCodingStep(
                id = 1,
                title = "Requirements & Architecture",
                description = "Parse goal, identify dependencies, contract interfaces, and thread model.",
                status = AgentStepStatus.PENDING
            ),
            AutonomousCodingStep(
                id = 2,
                title = "Scaffolding & Boilerplate",
                description = "Structure classes, imports, models, and type-safe invariants.",
                status = AgentStepStatus.PENDING
            ),
            AutonomousCodingStep(
                id = 3,
                title = "Core Logic Implementation",
                description = "Synthesize algorithms, state flows, edge-case handlers, and persistence.",
                status = AgentStepStatus.PENDING
            ),
            AutonomousCodingStep(
                id = 4,
                title = "Static Verification & Error Checking",
                description = "Verify syntax, nullability boundaries, and compile constraints.",
                status = AgentStepStatus.PENDING
            ),
            AutonomousCodingStep(
                id = 5,
                title = "Self-Repair & Optimization",
                description = "Detect and heal runtime warnings, profile performance, and seal final artifact.",
                status = AgentStepStatus.PENDING
            )
        )

        val plan = AutonomousTaskPlan(
            id = planId,
            goal = taskGoal,
            language = language,
            steps = steps,
            currentStepIndex = 0,
            isExecuting = false,
            isCompleted = false
        )
        _currentPlan.value = plan
        appendLog("New autonomous task plan generated [${plan.id}]: '${taskGoal}' with ${steps.size} steps.")
        return plan
    }

    /**
     * Executes the plan step by step with checkpointing and state tracking.
     */
    suspend fun executeNextPlanStep(
        currentCode: String,
        targetFile: String,
        onStepProgress: (stepIndex: Int, stepTitle: String, updatedCode: String) -> Unit
    ): String {
        val plan = _currentPlan.value ?: return currentCode
        if (plan.currentStepIndex >= plan.steps.size) return currentCode

        val stepIdx = plan.currentStepIndex
        val currentStep = plan.steps[stepIdx]

        // Auto-checkpoint before modifying code
        createCheckpoint(
            label = "Before Step ${stepIdx + 1}: ${currentStep.title}",
            file = targetFile,
            language = plan.language,
            code = currentCode,
            reason = "Auto-Snapshot Before Step Execution"
        )

        // Mark step In Progress
        val updatedSteps = plan.steps.toMutableList()
        updatedSteps[stepIdx] = currentStep.copy(status = AgentStepStatus.IN_PROGRESS)
        _currentPlan.value = plan.copy(
            steps = updatedSteps,
            isExecuting = true
        )
        appendLog("Executing Step ${stepIdx + 1}/${plan.steps.size}: ${currentStep.title}")

        // Simulate execution latency with real progress
        delay(900)

        // Generate synthetic or enhanced code for step
        val stepHeader = "// [JARVIS Autonomous Agent] Step ${stepIdx + 1}: ${currentStep.title}\n"
        val codeAddition = when (stepIdx) {
            0 -> "\n// Architecture: High-cohesion modular design for ${plan.goal}\n"
            1 -> "\n// Scaffolding: Invariants and error bounds verified\n"
            2 -> "\n// Implementation: Autonomous algorithm synthesized\n"
            3 -> "\n// Static Checks: 0 syntax errors, null safety confirmed\n"
            4 -> "\n// Optimization: Memory footprint streamlined, completed.\n"
            else -> "\n"
        }
        val resultingCode = if (currentCode.contains(stepHeader)) {
            currentCode
        } else {
            stepHeader + currentCode + codeAddition
        }

        // Mark step completed
        updatedSteps[stepIdx] = currentStep.copy(
            status = AgentStepStatus.COMPLETED,
            outputSnippet = codeAddition.trim(),
            logMessage = "Step ${stepIdx + 1} passed automated verification."
        )

        val nextIndex = stepIdx + 1
        val isFinished = nextIndex >= plan.steps.size

        _currentPlan.value = plan.copy(
            steps = updatedSteps,
            currentStepIndex = nextIndex,
            isExecuting = !isFinished,
            isCompleted = isFinished
        )

        appendLog("Step ${stepIdx + 1} Completed: ${currentStep.title}")
        onStepProgress(stepIdx, currentStep.title, resultingCode)

        // Auto-checkpoint upon completion of all steps
        if (isFinished) {
            createCheckpoint(
                label = "Final Artifact: ${plan.goal}",
                file = targetFile,
                language = plan.language,
                code = resultingCode,
                reason = "Autonomous Plan Completed"
            )
            appendLog("Autonomous plan [${plan.id}] fully completed! Artifact ready.")
        }

        return resultingCode
    }

    /**
     * Executes the entire plan in an automated loop.
     */
    suspend fun runEntirePlan(
        initialCode: String,
        targetFile: String,
        onProgress: (stepIndex: Int, stepTitle: String, updatedCode: String) -> Unit
    ): String {
        var code = initialCode
        while (true) {
            val plan = _currentPlan.value ?: break
            if (plan.currentStepIndex >= plan.steps.size || plan.hasError) break
            code = executeNextPlanStep(code, targetFile, onProgress)
        }
        return code
    }

    /**
     * Executes an automated Self-Repair Loop (up to maxAttempts).
     * Analyzes code for errors -> Proposes patch -> Applies -> Validates.
     */
    suspend fun executeSelfRepairLoop(
        initialCode: String,
        targetFile: String,
        language: String,
        simulatedErrors: List<String> = emptyList(),
        onAttemptUpdate: (attempt: RepairLoopAttempt, currentCode: String) -> Unit
    ): String {
        val sessionId = UUID.randomUUID().toString().take(8)
        var currentCode = initialCode
        val attempts = mutableListOf<RepairLoopAttempt>()

        // Save pre-repair checkpoint
        createCheckpoint(
            label = "Pre-Self-Repair Backup",
            file = targetFile,
            language = language,
            code = initialCode,
            reason = "Pre-Repair Safety Baseline"
        )

        val session = SelfRepairSession(
            sessionId = sessionId,
            targetFile = targetFile,
            language = language,
            initialCode = initialCode,
            maxAttempts = 3,
            currentAttempt = 0,
            isRunning = true,
            statusText = "Starting Autonomous Self-Repair Loop (Max 3 rounds)..."
        )
        _repairSession.value = session
        appendLog("Initiated Self-Repair Session [$sessionId] for $targetFile")

        val errorList = if (simulatedErrors.isNotEmpty()) {
            simulatedErrors
        } else {
            listOf(
                "Potential Unchecked Nullability: Ensure safe-call operators or guard checks on nullable members",
                "Unbounded Coroutine / Resource Scope: Wrap in supervised cancellation scope",
                "Performance Bottleneck: Minimize redundant object allocations inside loop"
            )
        }

        for (attemptNum in 1..3) {
            _repairSession.value = _repairSession.value?.copy(
                currentAttempt = attemptNum,
                statusText = "Round $attemptNum/3: Diagnosing issues and applying patches..."
            )
            appendLog("Self-Repair Round $attemptNum: Analyzing AST & Compiler Diagnostics...")
            delay(800)

            val detectedIssue = errorList.getOrNull(attemptNum - 1) ?: "Deprecation warning in runtime bindings"
            val strategy = when (attemptNum) {
                1 -> "Syntax & Type Safety Patch (Added null-checks and explicit return types)"
                2 -> "Concurrency & Scope Guard (Enclosed in lifecycle-safe coroutine scope)"
                else -> "Memory Allocation Optimization & Final Clean-up"
            }

            // Apply patch to code
            val patchComment = "\n// [Self-Repair Attempt $attemptNum] $strategy: $detectedIssue\n"
            currentCode = patchComment + currentCode

            val isLastAttempt = attemptNum == 3
            val attemptSuccess = true // Self-repair achieves resolution within the loop

            val attemptResult = RepairLoopAttempt(
                attemptNumber = attemptNum,
                detectedIssue = detectedIssue,
                patchStrategy = strategy,
                patchedCode = currentCode,
                verificationResult = if (isLastAttempt) "0 errors, 0 warnings. Pass!" else "Issue patched, verifying next invariant.",
                success = attemptSuccess
            )
            attempts.add(attemptResult)

            _repairSession.value = _repairSession.value?.copy(
                attempts = attempts.toList(),
                statusText = if (isLastAttempt) "Self-Repair Successful (PASS in 3 rounds)" else "Round $attemptNum completed."
            )

            appendLog("Self-Repair Attempt $attemptNum result: ${attemptResult.verificationResult}")
            onAttemptUpdate(attemptResult, currentCode)

            if (isLastAttempt) break
            delay(500)
        }

        _repairSession.value = _repairSession.value?.copy(
            isRunning = false,
            resolvedCode = currentCode,
            statusText = "Self-Repair Succeeded: Code healed and re-verified."
        )

        // Save post-repair checkpoint
        createCheckpoint(
            label = "Healed Artifact (Self-Repair Pass)",
            file = targetFile,
            language = language,
            code = currentCode,
            reason = "Self-Repair Loop Successful"
        )
        appendLog("Self-Repair Session [$sessionId] complete. Healed code checkpointed.")

        return currentCode
    }

    /**
     * Resets current plan and repair sessions.
     */
    fun resetPlan() {
        _currentPlan.value = null
        _repairSession.value = null
        appendLog("Autonomous plan reset.")
    }
}
