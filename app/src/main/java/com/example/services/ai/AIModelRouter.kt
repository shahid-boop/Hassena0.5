package com.example.services.ai

import com.example.models.AIModelOption
import com.example.models.AIProviderType
import com.example.models.RoutingMode
import com.example.models.SupportedModelsCatalog
import com.example.models.TaskCategory
import java.util.Locale

/** Task-aware router. It prefers configured keys and catalog-declared capabilities; no keys are embedded here. */
class AIModelRouter {

    data class RouteDecision(
        val category: TaskCategory,
        val provider: AIProviderType,
        val modelId: String,
        val displayName: String,
        val reason: String
    )

    fun routeRequest(
        prompt: String,
        hasImageAttachment: Boolean,
        forceResearch: Boolean,
        routingMode: RoutingMode,
        preferredProvider: AIProviderType,
        preferredModelId: String,
        isProviderConfigured: (AIProviderType) -> Boolean
    ): RouteDecision {
        val lower = prompt.trim().lowercase(Locale.ROOT)
        val configuredModels = SupportedModelsCatalog.models.filter { isProviderConfigured(it.provider) }

        // Visual inputs must go to a catalog entry explicitly marked vision-capable.
        if (hasImageAttachment) {
            val vision = SupportedModelsCatalog.models.firstOrNull {
                it.supportsVision && it.provider == preferredProvider && isProviderConfigured(it.provider)
            } ?: SupportedModelsCatalog.models.firstOrNull {
                it.supportsVision && isProviderConfigured(it.provider)
            }
            return vision?.let {
                decision(TaskCategory.VISION_ANALYSIS, it, "Image attachment routed to a configured vision-capable model")
            } ?: RouteDecision(
                TaskCategory.VISION_ANALYSIS, AIProviderType.GEMINI, "gemini-3.8-flash",
                "Gemini 3.8 Flash (API key required)",
                "No configured vision-capable provider found; add a supported provider API key in Settings"
            )
        }

        val research = forceResearch || listOf(
            "research ", "search the web for ", "search for ", "latest news", "cite sources", "deep research",
            "current price", "today's", "today’s", "latest version"
        ).any { lower.contains(it) }
        if (research) {
            val model = SupportedModelsCatalog.models.firstOrNull {
                it.provider == AIProviderType.PERPLEXITY && it.supportsResearch && isProviderConfigured(it.provider)
            } ?: SupportedModelsCatalog.models.firstOrNull {
                it.supportsResearch && isProviderConfigured(it.provider)
            }
            return model?.let { decision(TaskCategory.WEB_RESEARCH, it, "Research task routed to a configured research-capable model") }
                ?: RouteDecision(TaskCategory.WEB_RESEARCH, AIProviderType.PERPLEXITY, "sonar-pro", "Perplexity Sonar Pro (API key required)", "No configured research-capable provider found; add a provider key in Settings")
        }

        if (routingMode == RoutingMode.MANUAL_OVERRIDE) {
            val manual = SupportedModelsCatalog.models.firstOrNull {
                it.provider == preferredProvider && it.modelId == preferredModelId && isProviderConfigured(it.provider)
            }
            if (manual != null) {
                val category = if (manual.isFastTier) TaskCategory.SIMPLE_QA else TaskCategory.COMPLEX_REASONING
                return decision(category, manual, "Manual provider/model override is active")
            }
        }

        val complex = prompt.length > 180 || listOf(
            "explain in detail", "step by step", "write code", "generate code", "create an app", "architecture",
            "compare and contrast", "analyze", "analyse", "prove", "algorithm", "debug", "refactor", "build a project",
            "zip project", "review this code", "reasoning"
        ).any { lower.contains(it) }

        if (routingMode == RoutingMode.LOCAL_FIRST_STRICT && !complex) {
            // The ViewModel still performs local command/cache checks before calling this router.
            val fast = configuredModels.firstOrNull { it.provider == preferredProvider && it.isFastTier }
                ?: configuredModels.firstOrNull { it.isFastTier }
            if (fast != null) return decision(TaskCategory.SIMPLE_QA, fast, "Local-first checks completed; using configured fast model as cloud fallback")
        }

        val preferred = SupportedModelsCatalog.models.firstOrNull {
            it.provider == preferredProvider && it.isFastTier != complex && isProviderConfigured(it.provider)
        }
        val chosen = preferred ?: configuredModels.firstOrNull { it.isFastTier != complex }
            ?: configuredModels.firstOrNull { it.isFastTier == complex }

        if (chosen != null) {
            return decision(
                if (complex) TaskCategory.COMPLEX_REASONING else TaskCategory.SIMPLE_QA,
                chosen,
                if (complex) "Complex coding/reasoning task routed to a configured reasoning-tier model" else "Simple conversation routed to a configured fast model"
            )
        }

        // Keep a deterministic fallback so the service can return its normal missing-key guidance.
        return if (complex) RouteDecision(
            TaskCategory.COMPLEX_REASONING, AIProviderType.GEMINI, "gemini-3.1-pro-preview",
            "Gemini 3.1 Pro (API key required)", "No configured provider key found; configure a key in Settings"
        ) else RouteDecision(
            TaskCategory.SIMPLE_QA, AIProviderType.GEMINI, "gemini-3.8-flash",
            "Gemini 3.8 Flash (API key required)", "No configured provider key found; configure a key in Settings"
        )
    }

    private fun decision(category: TaskCategory, model: AIModelOption, reason: String) = RouteDecision(
        category = category,
        provider = model.provider,
        modelId = model.modelId,
        displayName = model.displayName,
        reason = reason
    )
}
