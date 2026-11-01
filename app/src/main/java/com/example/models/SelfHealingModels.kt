package com.example.models

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorSeverity(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4)
}

@Serializable
enum class ErrorCategory(val displayName: String, val isTransientByDefault: Boolean) {
    NETWORK_TRANSIENT("Network / Transient Timeout", true),
    AI_PROVIDER_ERROR("AI Provider API Error", false),
    MISSING_API_KEY("Missing API Credentials", false),
    COMPILATION_ERROR("Code Compilation Error", false),
    TEST_FAILURE("Test Suite Assertion Failure", false),
    FILE_IO_ERROR("File System I/O Error", false),
    AGENT_TASK_ERROR("Autonomous Agent Step Failure", false),
    DATABASE_ERROR("SQLite Database Operation Error", false),
    RUNTIME_EXCEPTION("Unhandled Runtime Exception", false)
}

@Serializable
enum class ResolutionStatus(val label: String, val isResolved: Boolean) {
    UNRESOLVED("Unresolved", false),
    ANALYZING("Analyzing Cause", false),
    AUTO_RECOVERED("Auto-Recovered", true),
    REPAIR_PROPOSED("Repair Proposed", false),
    WAITING_FOR_USER_APPROVAL("Waiting for User Approval", false),
    REPAIRING("Applying Patch", false),
    VERIFYING("Verifying Outcome", false),
    RECOVERED("Successfully Recovered", true),
    REPAIR_FAILED("Repair Failed", false),
    ROLLED_BACK("Rolled Back", false),
    IGNORED("Ignored", true)
}

@Serializable
enum class RepairState(val label: String, val isTerminal: Boolean) {
    DETECTED("Error Detected", false),
    ANALYZING("Root-Cause Analysis In Progress", false),
    ANALYSIS_COMPLETE("Analysis Complete", false),
    REPAIR_PROPOSED("Repair Patch Proposed", false),
    WAITING_FOR_APPROVAL("Awaiting User Authorization", false),
    REPAIRING("Applying Patch to Source", false),
    VERIFYING("Verifying via Build / Tests", false),
    RECOVERED("Recovered & Verified", true),
    FAILED("Repair Verification Failed", true),
    ROLLED_BACK("Safely Rolled Back", true),
    CANCELLED("Repair Cancelled by User", true)
}

@Serializable
data class SystemErrorRecord(
    val id: String,
    val category: ErrorCategory,
    val message: String,
    val affectedComponent: String,
    val operation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: ErrorSeverity = ErrorSeverity.MEDIUM,
    val stackTrace: String? = null,
    val isTransient: Boolean = false,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val suggestedRecovery: String = "Inspect error details and run root-cause diagnosis.",
    val resolutionStatus: ResolutionStatus = ResolutionStatus.UNRESOLVED,
    val repairId: String? = null
)

@Serializable
data class RootCauseAnalysis(
    val analysisId: String,
    val errorId: String,
    val identifiedCategory: ErrorCategory,
    val likelyRootCause: String,
    val isConfirmed: Boolean, // True if verified factual cause, false if hypothesis
    val isRecoverable: Boolean,
    val proposedFixSummary: String,
    val estimatedImpact: String,
    val recommendedVerification: String,
    val aiConfidence: Float = 0.85f
)

@Serializable
data class CodeDiffPatch(
    val targetFilePath: String,
    val originalSnippet: String,
    val replacementSnippet: String,
    val diffPreview: String,
    val linesAdded: Int = 0,
    val linesRemoved: Int = 0,
    val fullPatchedContent: String? = null
)

@Serializable
data class SelfHealingRepairRecord(
    val id: String,
    val errorId: String,
    val state: RepairState = RepairState.DETECTED,
    val targetComponent: String,
    val targetFile: String? = null,
    val rootCauseAnalysis: RootCauseAnalysis? = null,
    val proposedPatch: CodeDiffPatch? = null,
    val userApprovalStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    val preRepairCheckpointId: String? = null,
    val preRepairSnapshot: String? = null,
    val verificationOutput: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
