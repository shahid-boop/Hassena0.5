package com.example.models

import kotlinx.serialization.Serializable

@Serializable
enum class AgentTaskStatus(val label: String, val isTerminal: Boolean) {
    PLANNING("Planning Execution", false),
    WAITING_FOR_APPROVAL("Awaiting Approval", false),
    READY("Ready to Execute", false),
    RUNNING("Executing Steps", false),
    PAUSED("Paused", false),
    WAITING_FOR_PERMISSION("Permission Needed", false),
    FAILED("Failed", true),
    COMPLETED("Completed", true),
    CANCELLED("Cancelled", true);

    val isActive: Boolean get() = this == RUNNING || this == PLANNING || this == READY
}

@Serializable
enum class AutonomousStepStatus(val label: String) {
    PENDING("Queued"),
    RUNNING("In Progress"),
    WAITING_CONFIRMATION("Needs Confirmation"),
    COMPLETED("Success"),
    FAILED("Failed"),
    SKIPPED("Skipped"),
    CANCELLED("Cancelled")
}

@Serializable
data class AgentStep(
    val id: String,
    val stepNumber: Int,
    val title: String,
    val description: String,
    val toolId: String,
    val toolArguments: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val status: AutonomousStepStatus = AutonomousStepStatus.PENDING,
    val actualResult: String? = null,
    val errorMessage: String? = null,
    val startTime: Long? = null,
    val completionTime: Long? = null
)

@Serializable
data class AgentTask(
    val id: String,
    val title: String,
    val userObjective: String,
    val executionPlanOverview: String,
    val steps: List<AgentStep> = emptyList(),
    val status: AgentTaskStatus = AgentTaskStatus.PLANNING,
    val finalResponse: String? = null,
    val errorInformation: String? = null,
    val replanCount: Int = 0,
    val currentStepIndex: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val completionTime: Long? = null,
    val logs: List<String> = emptyList(),
    val changedFiles: List<String> = emptyList()
)

@Serializable
enum class AgentToolDomain(val label: String, val toolNumber: Int) {
    AI_CODING("Tool 1: AI Coding & Files", 1),
    BUILD_TEST("Tool 2: Build & Test Executor", 2),
    BROWSER_WEB("Tool 3: Browser & Web", 3),
    PHONE_ACTIONS("Tool 4: Phone Actions", 4),
    PERSISTENT_MEMORY("Tool 5: Memory & Queue", 5),
    AI_BRAIN("Tool 6: AI Brain & Planner", 6)
}

data class AgentToolSpec(
    val toolId: String,
    val name: String,
    val description: String,
    val domain: AgentToolDomain = AgentToolDomain.AI_BRAIN,
    val requiredArguments: List<String> = emptyList(),
    val optionalArguments: List<String> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val requiredPermission: String? = null,
    val exampleUsage: String = ""
)

data class AgentToolResult(
    val success: Boolean,
    val output: String,
    val metadata: Map<String, String> = emptyMap(),
    val error: String? = null,
    val modifiedFiles: List<String> = emptyList()
)

