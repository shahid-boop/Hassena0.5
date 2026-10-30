package com.example.ui.features.avatar3d

import com.example.services.voice.PhonemeLipSyncEngine
import com.example.services.voice.VisemeData

enum class AvatarActivity(val displayLabel: String) {
    IDLE("Idle / Standby"),
    LISTENING("Listening to You"),
    THINKING("Thinking / Solving"),
    SPEAKING("Speaking"),
    CODING("Writing Code"),
    RESEARCHING("Web Deep Research"),
    DOCUMENT_REVIEW("Reviewing Knowledge"),
    TASK_COMPLETE("Task Completed"),
    TASK_ERROR("Execution Warning")
}

enum class CameraPreset(val id: String, val label: String) {
    WORKSTATION("WORKSTATION", "Workstation"),
    CLOSEUP_PORTRAIT("CLOSEUP_PORTRAIT", "Portrait Face"),
    DESK_ANGLE("DESK_ANGLE", "Desk Angle"),
    CINEMATIC_ORBIT("CINEMATIC_ORBIT", "Cinematic Orbit")
}

enum class AvatarStyle(
    val label: String,
    val hairHex: String,
    val blazerHex: String,
    val neonHex: String
) {
    EXECUTIVE_NAVY("Executive Navy", "#2b1c14", "#182236", "#00f0ff"),
    CYBER_EMERALD("Cyber Emerald", "#1a120b", "#0d2b22", "#00ff88"),
    QUANTUM_PURPLE("Quantum Violet", "#3b2042", "#241338", "#c752ff"),
    SOLAR_GOLD("Solar Executive", "#422818", "#2b2114", "#ffb300")
}

data class AvatarWorkstationState(
    val activity: AvatarActivity = AvatarActivity.IDLE,
    val currentTaskTitle: String = "Ready for Command",
    val currentTaskProgress: Float = 0.0f,
    val activeCodeSnippet: String = "",
    val cameraPreset: CameraPreset = CameraPreset.WORKSTATION,
    val avatarStyle: AvatarStyle = AvatarStyle.EXECUTIVE_NAVY,
    val isAnimationPaused: Boolean = false,
    val audioLevel: Float = 0.0f,
    val isReady: Boolean = false,
    val viseme: VisemeData = PhonemeLipSyncEngine.VISEME_SIL
)

enum class AvatarViewMode {
    WORKSTATION,
    PORTRAIT,
    CARD,
    PIP_BUBBLE
}

