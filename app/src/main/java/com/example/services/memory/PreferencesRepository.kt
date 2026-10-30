package com.example.services.memory

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.models.AIProviderType
import com.example.models.AssistantPersonality
import com.example.models.OrbColorPreset
import com.example.models.OrbConfig
import com.example.models.RoutingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hasseenaDataStore by preferencesDataStore(name = "hasseena_preferences")

data class HasseenaUserPreferences(
    val memoryEnabled: Boolean = true,
    val responseCacheEnabled: Boolean = true,
    val customInstructions: String = "",
    val personality: AssistantPersonality = AssistantPersonality.CONCISE_FAST,
    val selectedProvider: AIProviderType = AIProviderType.GEMINI,
    val selectedModelId: String = "gemini-3.8-flash",
    val routingMode: RoutingMode = RoutingMode.AUTO_SMART,
    val wakeWordEnabled: Boolean = false,
    val wakePhrase: String = "Hey JARVIS",
    val continuousConversation: Boolean = false,
    val autoSpeakTts: Boolean = true,
    val bargeInEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val orbConfig: OrbConfig = OrbConfig()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val CUSTOM_INSTRUCTIONS = stringPreferencesKey("custom_instructions")
        val PERSONALITY = stringPreferencesKey("personality")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val CONTINUOUS_VOICE = booleanPreferencesKey("continuous_voice")
        val AUTO_SPEAK_TTS = booleanPreferencesKey("auto_speak_tts")
        val BARGE_IN_ENABLED = booleanPreferencesKey("barge_in_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val ORB_COLOR_PRESET = intPreferencesKey("orb_color_preset")
        val ORB_SIZE_DP = floatPreferencesKey("orb_size_dp")
        val ORB_SPEED = floatPreferencesKey("orb_speed")
        val ORB_SHOW_PARTICLES = booleanPreferencesKey("orb_show_particles")
        val ORB_SHOW_RINGS = booleanPreferencesKey("orb_show_rings")
    }

    val preferencesFlow: Flow<HasseenaUserPreferences> = context.hasseenaDataStore.data.map { prefs ->
        val provider = runCatching {
            AIProviderType.valueOf(prefs[Keys.SELECTED_PROVIDER] ?: AIProviderType.GEMINI.name)
        }.getOrDefault(AIProviderType.GEMINI)

        val personality = runCatching {
            AssistantPersonality.valueOf(prefs[Keys.PERSONALITY] ?: AssistantPersonality.CONCISE_FAST.name)
        }.getOrDefault(AssistantPersonality.CONCISE_FAST)

        val routingMode = runCatching {
            RoutingMode.valueOf(prefs[Keys.ROUTING_MODE] ?: RoutingMode.AUTO_SMART.name)
        }.getOrDefault(RoutingMode.AUTO_SMART)

        val colorPresetOrdinal = prefs[Keys.ORB_COLOR_PRESET] ?: 0
        val colorPreset = OrbColorPreset.entries.getOrElse(colorPresetOrdinal) { OrbColorPreset.CYBER_CYAN }

        HasseenaUserPreferences(
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            responseCacheEnabled = prefs[Keys.CACHE_ENABLED] ?: true,
            customInstructions = prefs[Keys.CUSTOM_INSTRUCTIONS] ?: "",
            personality = personality,
            selectedProvider = provider,
            selectedModelId = when (val savedModel = prefs[Keys.SELECTED_MODEL]) {
                null, "gemini-3.5-flash" -> "gemini-3.8-flash"
                else -> savedModel
            },
            routingMode = routingMode,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            wakePhrase = prefs[Keys.WAKE_PHRASE] ?: "Hey JARVIS",
            continuousConversation = prefs[Keys.CONTINUOUS_VOICE] ?: false,
            autoSpeakTts = prefs[Keys.AUTO_SPEAK_TTS] ?: true,
            bargeInEnabled = prefs[Keys.BARGE_IN_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            orbConfig = OrbConfig(
                colorPreset = colorPreset,
                sizeDp = prefs[Keys.ORB_SIZE_DP] ?: 210f,
                animationSpeed = prefs[Keys.ORB_SPEED] ?: 1.0f,
                showParticles = prefs[Keys.ORB_SHOW_PARTICLES] ?: true,
                showRings = prefs[Keys.ORB_SHOW_RINGS] ?: true
            )
        )
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.hasseenaDataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
    }

    suspend fun setResponseCacheEnabled(enabled: Boolean) {
        context.hasseenaDataStore.edit { it[Keys.CACHE_ENABLED] = enabled }
    }

    suspend fun setCustomInstructions(instructions: String) {
        context.hasseenaDataStore.edit { it[Keys.CUSTOM_INSTRUCTIONS] = instructions }
    }

    suspend fun setPersonality(personality: AssistantPersonality) {
        context.hasseenaDataStore.edit { it[Keys.PERSONALITY] = personality.name }
    }

    suspend fun setProviderAndModel(provider: AIProviderType, modelId: String) {
        context.hasseenaDataStore.edit {
            it[Keys.SELECTED_PROVIDER] = provider.name
            it[Keys.SELECTED_MODEL] = modelId
        }
    }

    suspend fun setRoutingMode(mode: RoutingMode) {
        context.hasseenaDataStore.edit { it[Keys.ROUTING_MODE] = mode.name }
    }

    suspend fun setWakeWordConfig(enabled: Boolean, phrase: String) {
        context.hasseenaDataStore.edit {
            it[Keys.WAKE_WORD_ENABLED] = enabled
            it[Keys.WAKE_PHRASE] = phrase.ifBlank { "Hey JARVIS" }
        }
    }

    suspend fun setVoiceSettings(
        continuousConversation: Boolean,
        autoSpeakTts: Boolean,
        bargeInEnabled: Boolean,
        hapticFeedbackEnabled: Boolean
    ) {
        context.hasseenaDataStore.edit {
            it[Keys.CONTINUOUS_VOICE] = continuousConversation
            it[Keys.AUTO_SPEAK_TTS] = autoSpeakTts
            it[Keys.BARGE_IN_ENABLED] = bargeInEnabled
            it[Keys.HAPTIC_ENABLED] = hapticFeedbackEnabled
        }
    }

    suspend fun setOrbConfig(config: OrbConfig) {
        context.hasseenaDataStore.edit {
            it[Keys.ORB_COLOR_PRESET] = config.colorPreset.ordinal
            it[Keys.ORB_SIZE_DP] = config.sizeDp
            it[Keys.ORB_SPEED] = config.animationSpeed
            it[Keys.ORB_SHOW_PARTICLES] = config.showParticles
            it[Keys.ORB_SHOW_RINGS] = config.showRings
        }
    }
}
