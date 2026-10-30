package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class CodingCheckpoint(
    val id: String,
    val label: String,
    val timestamp: Long = System.currentTimeMillis(),
    val file: String,
    val language: String,
    val code: String,
    val lineCount: Int,
    val triggerReason: String = "Manual Snapshot"
)

enum class AgentStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    REPAIRED
}

@Serializable
data class AutonomousCodingStep(
    val id: Int,
    val title: String,
    val description: String,
    val status: AgentStepStatus = AgentStepStatus.PENDING,
    val outputSnippet: String? = null,
    val logMessage: String? = null
)

@Serializable
data class AutonomousTaskPlan(
    val id: String,
    val goal: String,
    val language: String,
    val steps: List<AutonomousCodingStep>,
    val currentStepIndex: Int = 0,
    val isExecuting: Boolean = false,
    val isCompleted: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class RepairLoopAttempt(
    val attemptNumber: Int,
    val detectedIssue: String,
    val patchStrategy: String,
    val patchedCode: String,
    val verificationResult: String,
    val success: Boolean
)

@Serializable
data class SelfRepairSession(
    val sessionId: String,
    val targetFile: String,
    val language: String,
    val initialCode: String,
    val maxAttempts: Int = 3,
    val currentAttempt: Int = 0,
    val attempts: List<RepairLoopAttempt> = emptyList(),
    val resolvedCode: String? = null,
    val isRunning: Boolean = false,
    val statusText: String = "Ready for self-repair verification"
)
