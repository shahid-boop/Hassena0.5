package com.example.models

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CrimsonNova
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SolarGold
import kotlinx.serialization.Serializable

enum class OrbState(val label: String, val statusSubtitle: String) {
    IDLE("IDLE", "Tap orb or speak command"),
    LISTENING("LISTENING", "Listening to your voice..."),
    THINKING("THINKING", "Routing & streaming intelligence..."),
    SPEAKING("SPEAKING", "Speaking (Tap or speak to interrupt)"),
    EXECUTING("EXECUTING", "Executing Android device tool..."),
    ERROR("ERROR", "Action requires attention")
}

enum class OrbColorPreset(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    CYBER_CYAN("Cyber Sapphire", CyberCyan, QuantumViolet, ElectricBlue),
    QUANTUM_VIOLET("Quantum Aura", QuantumViolet, CyberCyan, Color(0xFFE040FB)),
    EMERALD_PULSE("Emerald Matrix", EmeraldReady, CyberCyan, Color(0xFF69F0AE)),
    SOLAR_GOLD("Solar Flare", SolarGold, CoralAlert, AmberThinking),
    CRIMSON_NOVA("Crimson Nova", CrimsonNova, QuantumViolet, SolarGold)
}

data class OrbConfig(
    val colorPreset: OrbColorPreset = OrbColorPreset.CYBER_CYAN,
    val sizeDp: Float = 210f,
    val animationSpeed: Float = 1.0f,
    val showParticles: Boolean = true,
    val showRings: Boolean = true
)

enum class AIProviderType(
    val displayName: String,
    val subtitle: String,
    val baseUrl: String
) {
    GEMINI(
        "Google Gemini",
        "Ultra-fast native multimodal & streaming SSE",
        "https://generativelanguage.googleapis.com/"
    ),
    GROQ(
        "Groq LPU",
        "Low-latency LPU inference engine",
        "https://api.groq.com/openai/"
    ),
    OPENAI(
        "OpenAI",
        "GPT-4o / GPT-4o-mini streaming engine",
        "https://api.openai.com/"
    ),
    CLAUDE(
        "Anthropic Claude",
        "Claude 3.7 Sonnet / 3.5 Haiku reasoning",
        "https://api.anthropic.com/"
    ),
    PERPLEXITY(
        "Perplexity Sonar",
        "Real-time web search & grounded citations",
        "https://api.perplexity.ai/"
    ),
    META(
        "Meta Muse Spark",
        "Meta Model API",
        "https://api.meta.ai/v1/"
    ),
    OPENROUTER(
        "OpenRouter",
        "OpenAI-compatible gateway; pricing and free-tier access depend on the selected model",
        "https://openrouter.ai/api/v1/"
    ),
    NVIDIA_NIM(
        "NVIDIA NIM (Free Tier)",
        "Open models through NVIDIA API Catalog; usage limits apply",
        "https://integrate.api.nvidia.com/v1/"
    ),
    MISTRAL(
        "Mistral AI (Experiment Tier)",
        "Open models; free-tier availability and limits depend on account",
        "https://api.mistral.ai/v1/"
    )
}

data class AIModelOption(
    val provider: AIProviderType,
    val modelId: String,
    val displayName: String,
    val badge: String,
    val isFastTier: Boolean,
    val supportsVision: Boolean = false,
    val supportsResearch: Boolean = false
)

object SupportedModelsCatalog {
    val models = listOf(
        // Selected OpenRouter models. Configure an OpenRouter API key in Settings.
        AIModelOption(
            provider = AIProviderType.NVIDIA_NIM,
            modelId = "nvidia/nemotron-3.5-lightning-30b-a3b",
            displayName = "NVIDIA Nemotron 3.5 Lightning (NIM)",
            badge = "FREE TRIAL • AGENTIC",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.OPENROUTER,
            modelId = "nvidia/nemotron-3-ultra-550b-a55b",
            displayName = "NVIDIA Nemotron 3 Ultra",
            badge = "PAID • REASONING",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.OPENROUTER,
            modelId = "nvidia/nemotron-3-super-120b-a12b:free",
            displayName = "NVIDIA Nemotron 3 Super",
            badge = "FREE • BALANCED",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.OPENROUTER,
            modelId = "google/gemma-4-31b-it:free",
            displayName = "Google Gemma 4 31B",
            badge = "FREE • GENERAL",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.OPENROUTER,
            modelId = "qwen/qwen3.8-27b:free",
            displayName = "Qwen 3.8 27B",
            badge = "FREE • CODING",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.OPENROUTER,
            modelId = "moonshotai/kimi-k2.6",
            displayName = "Kimi K2.6 (OpenRouter)",
            badge = "PAID • MULTI-PURPOSE",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.NVIDIA_NIM,
            modelId = "meta/llama-3.3-70b-instruct",
            displayName = "Llama 3.3 70B (NVIDIA NIM)",
            badge = "FREE TIER",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.NVIDIA_NIM,
            modelId = "qwen/qwen2.5-coder-32b-instruct",
            displayName = "Qwen 2.5 Coder 32B (NVIDIA NIM)",
            badge = "CODING",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.MISTRAL,
            modelId = "mistral-small-latest",
            displayName = "Mistral Small (Experiment Tier)",
            badge = "FREE TIER",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.MISTRAL,
            modelId = "codestral-latest",
            displayName = "Codestral (Coding)",
            badge = "CODING",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.GEMINI,
            modelId = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            badge = "ULTRA FAST + VISION",
            isFastTier = true,
            supportsVision = true,
            supportsResearch = true
        ),
        AIModelOption(
            provider = AIProviderType.GEMINI,
            modelId = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            badge = "CODING • AGENTIC",
            isFastTier = false,
            supportsVision = true,
            supportsResearch = true
        ),
        AIModelOption(
            provider = AIProviderType.GEMINI,
            modelId = "gemini-3.1-flash-lite-preview",
            displayName = "Gemini 3.1 Flash Lite",
            badge = "ULTRA FAST",
            isFastTier = true,
            supportsVision = true
        ),
        AIModelOption(
            provider = AIProviderType.GEMINI,
            modelId = "gemini-3.1-pro-preview",
            displayName = "Gemini 3.1 Pro",
            badge = "DEEP REASONING",
            isFastTier = false,
            supportsVision = true,
            supportsResearch = true
        ),
        AIModelOption(
            provider = AIProviderType.GROQ,
            modelId = "openai/gpt-oss-20b",
            displayName = "GPT-OSS 20B (Groq)",
            badge = "FAST • REASONING",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.GROQ,
            modelId = "openai/gpt-oss-120b",
            displayName = "GPT-OSS 120B (Groq)",
            badge = "REASONING • CODING",
            isFastTier = false,
            supportsResearch = true
        ),
        AIModelOption(
            provider = AIProviderType.GROQ,
            modelId = "qwen/qwen3.8-27b",
            displayName = "Qwen 3.8 27B (Groq)",
            badge = "CODING • REASONING",
            isFastTier = false
        ),
        AIModelOption(
            provider = AIProviderType.OPENAI,
            modelId = "gpt-4o-mini",
            displayName = "GPT-4o Mini",
            badge = "FAST",
            isFastTier = true,
            supportsVision = true
        ),
        AIModelOption(
            provider = AIProviderType.OPENAI,
            modelId = "gpt-4o",
            displayName = "GPT-4o Omni",
            badge = "REASONING",
            isFastTier = false,
            supportsVision = true
        ),
        AIModelOption(
            provider = AIProviderType.CLAUDE,
            modelId = "claude-3-5-haiku-latest",
            displayName = "Claude 3.5 Haiku",
            badge = "FAST",
            isFastTier = true
        ),
        AIModelOption(
            provider = AIProviderType.CLAUDE,
            modelId = "claude-3-7-sonnet-latest",
            displayName = "Claude 3.7 Sonnet",
            badge = "DEEP REASONING",
            isFastTier = false
        ),
        AIModelOption(
                provider = AIProviderType.META,
                modelId = "muse-spark-1.3",
                displayName = "Meta Muse Spark",
                badge = "META AI",
                isFastTier = false
            ),
                    AIModelOption(
            provider = AIProviderType.PERPLEXITY,
            modelId = "sonar-pro",
            displayName = "Perplexity Sonar Pro",
            badge = "WEB CITATIONS",
            isFastTier = false,
            supportsResearch = true
        )
    )

    fun modelsForProvider(provider: AIProviderType): List<AIModelOption> =
        models.filter { it.provider == provider }
}

enum class RoutingMode(val title: String, val description: String) {
    AUTO_SMART(
        "Smart Router (Auto)",
        "Local engine for commands, Fast model for Q&A, Pro model for deep reasoning, Vision for images"
    ),
    MANUAL_OVERRIDE(
        "Manual Provider/Model",
        "Always use your manually selected AI provider and model after local command checks"
    ),
    LOCAL_FIRST_STRICT(
        "Offline / Local Priority",
        "Prioritize on-device commands, cached answers, and hardware tools"
    )
}

enum class TaskCategory(val label: String) {
    LOCAL_DEVICE_COMMAND("LOCAL ENGINE"),
    CACHED_INSTANT("MEMORY CACHE"),
    SIMPLE_QA("FAST AI"),
    COMPLEX_REASONING("DEEP REASONING"),
    WEB_RESEARCH("WEB RESEARCH"),
    VISION_ANALYSIS("VISUAL LENS")
}

enum class AssistantPersonality(
    val title: String,
    val subtitle: String,
    val systemInstructionPrompt: String
) {
    CONCISE_FAST(
        "Crisp & Direct",
        "Brief for simple queries, detailed only when asked",
        "You are Hasseena A1, a high-performance, voice-first Android AI assistant. Respond naturally, respectfully, and concisely. Avoid filler introductions like 'Sure, I can help with that.' For simple questions, give a direct 1-2 sentence answer suitable for instant voice playback. Provide deep, structured detail only when explicitly asked."
    ),
    WARM_COMPANION(
        "Warm & Conversational",
        "Empathetic, friendly, natural spoken cadence",
        "You are Hasseena A1, a warm, articulate, and perceptive personal Android AI assistant. Speak naturally with a conversational rhythm that sounds great aloud. Keep answers clear and engaging without robotic repetition."
    ),
    EXECUTIVE_PRO(
        "Executive Strategist",
        "Action-oriented, structured, high-precision",
        "You are Hasseena A1, an executive-grade personal AI assistant. Prioritize actionable conclusions, bulleted clarity, and rapid execution."
    ),
    TECHNICAL_EXPERT(
        "Systems & Engineering",
        "Deep technical accuracy, code-aware, analytical",
        "You are Hasseena A1, a senior technical AI assistant running on Android. Provide precise, technically rigorous answers with clear architecture and root-cause explanations."
    )
}

sealed class PendingConfirmationAction(
    open val title: String,
    open val description: String,
    open val confirmButtonText: String
) {
    data class PlacePhoneCall(
        val contactNameOrNumber: String,
        val phoneNumber: String
    ) : PendingConfirmationAction(
        title = "Call this number?",
        description = "Hasseena A1 is ready to place a phone call to $contactNameOrNumber ($phoneNumber).",
        confirmButtonText = "Place Call"
    )

    data class SendSmsMessage(
        val recipient: String,
        val messageBody: String
    ) : PendingConfirmationAction(
        title = "Send this message?",
        description = "Open messaging app to send to $recipient:\n\"$messageBody\"",
        confirmButtonText = "Compose Message"
    )

    data class ChangeSystemSetting(
        val settingName: String,
        val targetValueDescription: String,
        val settingsAction: String
    ) : PendingConfirmationAction(
        title = "Change this setting?",
        description = "Confirm opening $settingName to apply: $targetValueDescription",
        confirmButtonText = "Open Setting"
    )

    data class AccessibilityClickAction(
        val targetElementLabel: String
    ) : PendingConfirmationAction(
        title = "Perform UI click?",
        description = "Allow Hasseena A1 Accessibility Service to tap control \"$targetElementLabel\" on screen?",
        confirmButtonText = "Confirm Tap"
    )

    data class ClearAllMemory(
        val itemCount: Int
    ) : PendingConfirmationAction(
        title = "Delete all stored memories?",
        description = "This will permanently remove $itemCount stored memory facts and cached responses from local storage.",
        confirmButtonText = "Delete Memory"
    )
}

data class LocalCommandResult(
    val matched: Boolean,
    val toolName: String = "",
    val spokenResponse: String = "",
    val detailSummary: String = "",
    val executionLatencyMs: Long = 0L,
    val requiresConfirmation: Boolean = false,
    val pendingConfirmation: PendingConfirmationAction? = null,
    val isError: Boolean = false,
    val recoveryHint: String? = null
)

@Serializable
data class ResearchSource(
    val title: String,
    val url: String,
    val snippet: String,
    val sourceDomain: String
)

enum class ResearchMode(val title: String, val subtitle: String) {
    FAST_SEARCH("Fast Search", "Query → Live Web Search → Instant Summary"),
    DEEP_RESEARCH("Deep Research", "Multi-Query → Multi-Source Synthesis → Verified Citations"),
    IMAGE_CONCEPT_SEARCH("Visual & Wikimedia Search", "Search Wikimedia Commons & encyclopedia media")
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean
)

data class DeviceTelemetrySnapshot(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTempCelsius: Float,
    val wifiConnected: Boolean,
    val networkTypeLabel: String,
    val musicVolumePercent: Int,
    val ringVolumePercent: Int,
    val screenBrightnessPercent: Int,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val freeStorageGb: Float,
    val deviceModel: String,
    val androidVersion: String,
    val flashlightOn: Boolean
)

data class CapturedNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

data class PermissionStatusItem(
    val id: String,
    val name: String,
    val isGranted: Boolean,
    val whyNeeded: String,
    val manifestPermission: String? = null,
    val isSpecialAccess: Boolean = false,
    val settingsIntentAction: String? = null
)
