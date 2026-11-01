package com.example.ui.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.models.AIProviderType
import com.example.models.AssistantPersonality
import com.example.models.OrbColorPreset
import com.example.models.OrbConfig
import com.example.models.RoutingMode
import com.example.models.SupportedModelsCatalog
import com.example.services.memory.HasseenaUserPreferences
import com.example.services.memory.MemoryFactEntity
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndMemoryScreen(
    preferences: HasseenaUserPreferences,
    memoryFacts: List<MemoryFactEntity>,
    cloudMemoryCount: Int,
    cloudSyncStatus: String?,
    cacheCount: Int,
    isProviderKeyConfigured: (AIProviderType) -> Boolean,
    onSelectRoutingMode: (RoutingMode) -> Unit,
    onSelectProviderAndModel: (AIProviderType, String) -> Unit,
    onSelectPersonality: (AssistantPersonality) -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onToggleCache: (Boolean) -> Unit,
    onUpdateCustomInstructions: (String) -> Unit,
    onAddMemoryFact: (String, String) -> Unit,
    onDeleteMemoryFact: (Long) -> Unit,
    onRequestDeleteAllMemory: () -> Unit,
    onClearConversations: () -> Unit,
    onSyncLocalToCloud: () -> Unit,
    onUpdateOrbConfig: (OrbConfig) -> Unit,
    onUpdateWakeWord: (Boolean, String) -> Unit,
    onUpdateVoicePrefs: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onNavigateBackHome: () -> Unit,
    onSaveApiKey: (AIProviderType, String) -> Unit,
    onRemoveApiKey: (AIProviderType) -> Unit,
    onTestApiKey: (AIProviderType, String, (String) -> Unit) -> Unit,
    onSaveVoiceApiKey: (String) -> Unit = {},
    onSaveVoiceId: (String) -> Unit = {},
    currentVoicePack: String = "hasseena_custom",
    onSaveVoiceIdForPack: (String, String) -> Unit = { _, _ -> },
    onSelectVoicePack: (String) -> Unit = {},
    onGetVoiceIdForPack: (String) -> String = { "" },
    onTestElevenLabsVoice: (String, (Boolean, String) -> Unit) -> Unit = { _, result -> result(false, "Voice test is not connected.") }
) {
    BackHandler { onNavigateBackHome() }

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0=AI & Router, 1=Memory, 2=Orb & Voice
    var newFactText by rememberSaveable { mutableStateOf("") }
    var customInstructionsDraft by rememberSaveable(preferences.customInstructions) {
        mutableStateOf(preferences.customInstructions)
    }
    var wakePhraseDraft by rememberSaveable(preferences.wakePhrase) {
        mutableStateOf(preferences.wakePhrase)
    }
    var voiceApiKeyDraft by rememberSaveable { mutableStateOf("") }
    var voiceIdDraft by rememberSaveable(currentVoicePack) { mutableStateOf(onGetVoiceIdForPack(currentVoicePack)) }
    var selectedVoicePack by rememberSaveable(currentVoicePack) { mutableStateOf(currentVoicePack) }
    var voiceSetupStatus by rememberSaveable { mutableStateOf("") }
    val voicePacks = listOf(
        Triple("charmion", "Charmion", "Soft • mature • husky"),
        Triple("hasseena_custom", "JARVIS Custom", "Dark romance • intimate"),
        Triple("tiffany", "Tiffany", "Bold • gritty • confident"),
        Triple("aurora", "Aurora", "Warm • natural • clear"),
        Triple("nyx", "Nyx", "Low • mysterious • calm")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "AI Providers, Memory & Orb Studio",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(
            text = "Configure multi-provider routing, local memory, wake phrase & Orb physics",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-navigation tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple(0, "AI & Router", Icons.Default.SmartToy),
                Triple(1, "Memory", Icons.Default.Memory),
                Triple(2, "Orb & Voice", Icons.Default.Palette)
            )
            tabs.forEach { (idx, title, icon) ->
                val selected = activeTab == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) CyberCyan else BorderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { activeTab = idx }
                        .testTag("settings_tab_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) CyberCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) CyberCyan else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            // =================================================================
            // TAB 0: AI PROVIDERS & SMART MODEL ROUTING
            // =================================================================
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Smart Router Mode
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SMART MODEL ROUTING ENGINE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RoutingMode.entries.forEach { mode ->
                                    val isSelected = preferences.routingMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) CyberCyan.copy(alpha = 0.14f) else ObsidianSurfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) CyberCyan else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectRoutingMode(mode) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = mode.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (isSelected) CyberCyan else TextPrimary
                                            )
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Unified AIProvider Selection
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI PROVIDERS & MODELS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                AIProviderType.entries.forEach { provider ->
                                    val selectedProvider = preferences.selectedProvider == provider
                                    val keyReady = isProviderKeyConfigured(provider)
                                    val providerModels = SupportedModelsCatalog.modelsForProvider(provider)

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selectedProvider) ObsidianSurfaceElevated else ObsidianSurface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedProvider) CyberCyan else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                val defaultModel = providerModels.first().modelId
                                                onSelectProviderAndModel(provider, defaultModel)
                                            }
                                            .testTag("provider_card_${provider.name.lowercase()}")
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = provider.displayName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selectedProvider) CyberCyan else TextPrimary
                                                    )
                                                    Text(
                                                        text = provider.subtitle,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = TextSecondary
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (keyReady) EmeraldReady.copy(alpha = 0.16f) else AmberThinking.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = if (keyReady) "KEY READY" else "CONFIGURE IN SECRETS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (keyReady) EmeraldReady else AmberThinking,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                providerModels.forEach { modelOpt ->
                                                    val isCurrentModel = selectedProvider && preferences.selectedModelId == modelOpt.modelId
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (isCurrentModel) CyberCyan else ObsidianSurface,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            if (isCurrentModel) CyberCyan else BorderSubtle
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable {
                                                                onSelectProviderAndModel(provider, modelOpt.modelId)
                                                            }
                                                    ) {
                                                        Text(
                                                            text = "${modelOpt.displayName} (${modelOpt.badge})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isCurrentModel) Color.Black else TextSecondary,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // API KEY MANAGER
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API KEY MANAGER",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a provider key to use it instead of the app's fallback key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AIProviderType.entries.forEach { provider ->
                        var draftKey by rememberSaveable(provider.name) {
                            mutableStateOf("")
                        }
                        var showKey by rememberSaveable(provider.name) {
                            mutableStateOf(false)
                        }
                        var testStatus by rememberSaveable(provider.name) {
                            mutableStateOf("Not tested")
                        }
                        var isTesting by rememberSaveable(provider.name) {
                            mutableStateOf(false)
                        }

                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = draftKey,
                            onValueChange = { draftKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("API key") },
                            placeholder = { Text("Paste ${provider.displayName} key") },
                            visualTransformation = if (showKey)
                                androidx.compose.ui.text.input.VisualTransformation.None
                            else
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                androidx.compose.material3.TextButton(
                                    onClick = { showKey = !showKey }
                                ) {
                                    Text(if (showKey) "HIDE" else "SHOW")
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (draftKey.isNotBlank()) {
                                        onSaveApiKey(provider, draftKey.trim())
                                        draftKey = ""
                                    }
                                },
                                enabled = draftKey.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SAVE KEY")
                            }
                            OutlinedButton(
                                onClick = {
                                    onRemoveApiKey(provider)
                                    testStatus = "Saved key removed"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("REMOVE")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (draftKey.isNotBlank() && !isTesting) {
                                    isTesting = true
                                    testStatus = "Testing..."
                                    onTestApiKey(provider, draftKey.trim()) { result ->
                                        testStatus = result
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = draftKey.isNotBlank() && !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isTesting) "TESTING..." else "TEST KEY")
                        }
                        Text(
                            text = testStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                testStatus == "Connected" -> CyberCyan
                                testStatus.startsWith("Invalid") -> AmberThinking
                                testStatus.startsWith("Network error") -> AmberThinking
                                else -> TextSecondary
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        // Security Warning Notice per android-secret-management
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberThinking.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AmberThinking)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(id = R.string.security_prototype_warning),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 1: HASSEENA MEMORY & PERSONALITY CENTER
            // =================================================================
            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // On-Device Local Memory & Persistence Card (Zero login required)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_storage_memory_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = "On-Device Storage Active",
                                            tint = EmeraldReady,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ON-DEVICE PERSISTENCE & MEMORY",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyberCyan
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = EmeraldReady.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "${memoryFacts.size} SAVED LOCALLY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldReady,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Autonomous Offline-First Memory Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "100% private to your device. All facts, chat logs, and preferences are stored locally in SQLite/Room with zero account, login, or signup requirements.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onClearConversations,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("clear_conversations_button")
                                    ) {
                                        Text("Clear Chat History", color = TextSecondary)
                                    }

                                    OutlinedButton(
                                        onClick = onRequestDeleteAllMemory,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("delete_all_memory_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Clear All Memories",
                                            tint = CoralAlert,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear Memory", color = CoralAlert)
                                    }
                                }
                            }
                        }
                    }

                    // Memory & Cache Toggles
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Personal Memory Context", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Remember facts, preferences, and instructions across sessions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.memoryEnabled,
                                        onCheckedChange = onToggleMemory,
                                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan),
                                        modifier = Modifier.testTag("memory_toggle_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sub-Millisecond Response Cache ($cacheCount cached)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Instant local answers for repeated questions", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.responseCacheEnabled,
                                        onCheckedChange = onToggleCache,
                                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldReady)
                                    )
                                }
                            }
                        }
                    }

                    // Personality Selector
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ASSISTANT PERSONALITY", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                AssistantPersonality.entries.forEach { persona ->
                                    val selected = preferences.personality == persona
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) QuantumViolet.copy(alpha = 0.22f) else ObsidianSurfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) CyberCyan else BorderSubtle
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectPersonality(persona) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(persona.title, style = MaterialTheme.typography.titleMedium, color = if (selected) CyberCyan else TextPrimary)
                                            Text(persona.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Custom User Instructions
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("USER-DEFINED INSTRUCTIONS", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customInstructionsDraft,
                                    onValueChange = {
                                        customInstructionsDraft = it
                                        onUpdateCustomInstructions(it)
                                    },
                                    placeholder = { Text("e.g. Call me Sahil. Prefer concise bullet points...", color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Add & View Memory Facts
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("STORED MEMORY FACTS (${memoryFacts.size})", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newFactText,
                                        onValueChange = { newFactText = it },
                                        placeholder = { Text("Add a memory fact...", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("add_memory_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (newFactText.isNotBlank()) {
                                                onAddMemoryFact("PREFERENCE", newFactText)
                                                newFactText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                        modifier = Modifier.testTag("add_memory_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add memory")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onRequestDeleteAllMemory,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("delete_all_memory_button")
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralAlert, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete Memory", color = CoralAlert)
                                    }
                                    OutlinedButton(
                                        onClick = onClearConversations,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("clear_conversations_button")
                                    ) {
                                        Text("Clear Chats", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    items(memoryFacts, key = { it.id }) { fact ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${fact.category} • ${fact.source}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = fact.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(onClick = { onDeleteMemoryFact(fact.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete fact", tint = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 2: ORB CUSTOMIZATION & VOICE / WAKE-WORD STUDIO
            // =================================================================
            2 -> {
                val orbCfg = preferences.orbConfig
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Orb Color, Size, and Speed
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("HASSEENA ORB STUDIO", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                Spacer(modifier = Modifier.height(10.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OrbColorPreset.entries.forEach { preset ->
                                        val selected = orbCfg.colorPreset == preset
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (selected) preset.primaryColor.copy(alpha = 0.25f) else ObsidianSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selected) preset.primaryColor else BorderSubtle
                                            ),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    onUpdateOrbConfig(orbCfg.copy(colorPreset = preset))
                                                }
                                        ) {
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) preset.primaryColor else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Orb Size", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text("${orbCfg.sizeDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                }
                                Slider(
                                    value = orbCfg.sizeDp,
                                    onValueChange = { onUpdateOrbConfig(orbCfg.copy(sizeDp = it)) },
                                    valueRange = 150f..270f,
                                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Animation Speed", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text("${(orbCfg.animationSpeed * 10).roundToInt() / 10f}x", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                }
                                Slider(
                                    value = orbCfg.animationSpeed,
                                    onValueChange = { onUpdateOrbConfig(orbCfg.copy(animationSpeed = it)) },
                                    valueRange = 0.5f..2.2f,
                                    colors = SliderDefaults.colors(thumbColor = QuantumViolet, activeTrackColor = QuantumViolet)
                                )
                            }
                        }
                    }

                    // Voice-First & Barge-In Settings
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("VOICE-FIRST & BARGE-IN ENGINE", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sentence-Streaming TTS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Begin speaking as soon as the first sentence streams in", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.autoSpeakTts,
                                        onCheckedChange = {
                                            onUpdateVoicePrefs(
                                                preferences.continuousConversation,
                                                it,
                                                preferences.bargeInEnabled,
                                                preferences.hapticFeedbackEnabled
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Barge-In Interruption", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Stop TTS immediately when you tap the orb or start speaking", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.bargeInEnabled,
                                        onCheckedChange = {
                                            onUpdateVoicePrefs(
                                                preferences.continuousConversation,
                                                preferences.autoSpeakTts,
                                                it,
                                                preferences.hapticFeedbackEnabled
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Continuous Voice Conversation", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Automatically resume listening after JARVIS finishes speaking", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.continuousConversation,
                                        onCheckedChange = {
                                            onUpdateVoicePrefs(
                                                it,
                                                preferences.autoSpeakTts,
                                                preferences.bargeInEnabled,
                                                preferences.hapticFeedbackEnabled
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ElevenLabs online voice configuration. API key is encrypted by Android Keystore.
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.75f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("DARK ROMANTIC VOICE ENGINE", style = MaterialTheme.typography.labelMedium, color = QuantumViolet)
                                Text("Deep • Husky • Warm • Cinematic", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Choose one of five voice packs. Each pack uses its own ElevenLabs Voice ID; IDs must be entered from your ElevenLabs account. Android TTS remains the fallback when online voice is unavailable.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    voicePacks.chunked(2).forEach { packRow ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            packRow.forEach { pack ->
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedVoicePack = pack.first
                                                        voiceIdDraft = onGetVoiceIdForPack(pack.first)
                                                        onSelectVoicePack(pack.first)
                                                        voiceSetupStatus = "Selected ${pack.second}. Enter its ElevenLabs Voice ID below and save."
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (selectedVoicePack == pack.first) Color.White else TextPrimary,
                                                        containerColor = if (selectedVoicePack == pack.first) QuantumViolet.copy(alpha = 0.75f) else Color.Transparent
                                                    )
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(pack.second, style = MaterialTheme.typography.labelLarge)
                                                        Text(pack.third, style = MaterialTheme.typography.labelSmall, color = if (selectedVoicePack == pack.first) Color.White else TextSecondary)
                                                    }
                                                }
                                            }
                                            if (packRow.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = voiceApiKeyDraft,
                                    onValueChange = { voiceApiKeyDraft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("ElevenLabs API key") },
                                    placeholder = { Text("Paste key here (stored encrypted on device)") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QuantumViolet, focusedLabelColor = QuantumViolet)
                                )
                                OutlinedTextField(
                                    value = voiceIdDraft,
                                    onValueChange = { voiceIdDraft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("${voicePacks.firstOrNull { it.first == selectedVoicePack }?.second ?: "Hasseena Custom"} Voice ID") },
                                    placeholder = { Text("Paste this pack's Voice ID from ElevenLabs") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, focusedLabelColor = CyberCyan)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        try {
                                            if (voiceApiKeyDraft.isNotBlank()) onSaveVoiceApiKey(voiceApiKeyDraft.trim())
                                            if (voiceIdDraft.isNotBlank()) {
                                                onSaveVoiceIdForPack(selectedVoicePack, voiceIdDraft.trim())
                                                if (selectedVoicePack == "hasseena_custom") onSaveVoiceId(voiceIdDraft.trim())
                                            }
                                            onSelectVoicePack(selectedVoicePack)
                                            voiceApiKeyDraft = ""
                                            voiceSetupStatus = if (voiceIdDraft.isNotBlank()) "Saved ${voicePacks.firstOrNull { it.first == selectedVoicePack }?.second} Voice ID. This pack is now selected." else "API key saved. Add a Voice ID for the selected pack to enable its voice."
                                        } catch (e: Exception) {
                                            voiceSetupStatus = "Could not save settings: ${e.localizedMessage ?: "unknown error"}"
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = QuantumViolet)) {
                                        Text("Save Voice Settings", color = Color.White)
                                    }
                                    OutlinedButton(onClick = {
                                        voiceApiKeyDraft = ""
                                        voiceIdDraft = ""
                                        voiceSetupStatus = "Fields cleared. Saved Voice IDs are not deleted."
                                    }) { Text("Clear Fields") }
                                }
                                OutlinedButton(
                                    onClick = {
                                        onTestElevenLabsVoice(voiceIdDraft.trim()) { success, message ->
                                            voiceSetupStatus = message
                                        }
                                    },
                                    enabled = voiceIdDraft.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Test Selected ElevenLabs Voice")
                                }
                                if (voiceSetupStatus.isNotBlank()) {
                                    Text(voiceSetupStatus, style = MaterialTheme.typography.bodySmall, color = if (voiceSetupStatus.startsWith("Could not") || voiceSetupStatus.contains("failed", ignoreCase = true) || voiceSetupStatus.contains("first", ignoreCase = true)) CoralAlert else EmeraldReady)
                                }
                            }
                        }
                    }

                    // Wake-Word Configuration
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Wake-Phrase Assistant Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Battery-conscious foreground wake phrase detection (never records covertly)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = preferences.wakeWordEnabled,
                                        onCheckedChange = { onUpdateWakeWord(it, wakePhraseDraft) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = wakePhraseDraft,
                                    onValueChange = {
                                        wakePhraseDraft = it
                                        onUpdateWakeWord(preferences.wakeWordEnabled, it)
                                    },
                                    label = { Text("Configurable Wake Phrase", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
