package com.example.ui.features.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AIProviderType
import com.example.models.SupportedModelsCatalog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AiProvidersScreen(
    isProviderConfigured: (AIProviderType) -> Boolean,
    onSaveKey: (AIProviderType, String) -> Unit = { _, _ -> },
    onRemoveKey: (AIProviderType) -> Unit = {},
    onTestPing: (AIProviderType, (Boolean, Long, String) -> Unit) -> Unit = { _, _ -> },
    onSelectActiveModel: (AIProviderType, String) -> Unit = { _, _ -> },
    activeProvider: AIProviderType = AIProviderType.GEMINI,
    activeModelId: String = "gemini-3.8-flash",
    onNavigateBack: () -> Unit = {}
) {
    val providers = remember {
        listOf(
            AIProviderType.OPENAI,
            AIProviderType.GEMINI,
            AIProviderType.CLAUDE,
            AIProviderType.GROQ,
            AIProviderType.PERPLEXITY,
            AIProviderType.MISTRAL,
            AIProviderType.OPENROUTER,
            AIProviderType.NVIDIA_NIM
        )
    }

    var selectedProviderForEdit by remember { mutableStateOf<AIProviderType?>(null) }
    var keyInputValue by remember { mutableStateOf("") }
    val pingStatusMap = remember { mutableStateMapOf<AIProviderType, String>() }
    var isTestingPing by remember { mutableStateOf<AIProviderType?>(null) }

    // Dialog for entering or modifying an API Key
    if (selectedProviderForEdit != null) {
        val p = selectedProviderForEdit!!
        AlertDialog(
            onDismissRequest = { selectedProviderForEdit = null },
            containerColor = ObsidianSurfaceElevated,
            title = {
                Text(
                    text = "Configure ${p.displayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your ${p.displayName} API Key. Keys are encrypted via Android Keystore AES-GCM and stored locally in private storage.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = keyInputValue,
                        onValueChange = { keyInputValue = it },
                        label = { Text("API Key") },
                        placeholder = { Text("e.g. sk-...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInputValue.isNotBlank()) {
                            onSaveKey(p, keyInputValue)
                            selectedProviderForEdit = null
                            keyInputValue = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedProviderForEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .testTag("ai_providers_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .testTag("providers_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JARVIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AI Providers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Button(
                onClick = {
                    selectedProviderForEdit = AIProviderType.OPENAI
                    keyInputValue = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Notice Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyanDim.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Multi-Model Intelligent Routing. Provider keys are encrypted locally using Android Keystore AES-256.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Providers List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(providers) { provider ->
                val isConfigured = isProviderConfigured(provider)
                val isCurrentActive = provider == activeProvider
                val pingStatus = pingStatusMap[provider]
                val availableModels = remember(provider) {
                    SupportedModelsCatalog.models.filter { it.provider == provider }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isCurrentActive) CyberCyan else BorderSubtle,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Provider logo / avatar indicator
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(ObsidianSurfaceElevated, CircleShape)
                                    .border(
                                        1.dp,
                                        if (isConfigured) EmeraldReady.copy(alpha = 0.5f) else BorderSubtle,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.displayName.take(2).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConfigured) EmeraldReady else TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = provider.subtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }

                            // Connection status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isConfigured) EmeraldReady.copy(alpha = 0.15f) else CoralAlert.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isConfigured) EmeraldReady else CoralAlert,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isConfigured) "Connected" else "Not Connected",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isConfigured) EmeraldReady else CoralAlert
                                    )
                                }
                            }
                        }

                        // Available models chips
                        if (availableModels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableModels.take(2).forEach { model ->
                                    val isModelActive = isCurrentActive && model.modelId == activeModelId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isModelActive) CyberCyan.copy(alpha = 0.2f) else ObsidianSurfaceGlass)
                                            .border(
                                                1.dp,
                                                if (isModelActive) CyberCyan else BorderSubtle,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                onSelectActiveModel(provider, model.modelId)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${model.displayName} ${if (isModelActive) "✓" else ""}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isModelActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isModelActive) CyberCyan else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Ping result display
                        if (pingStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pingStatus,
                                fontSize = 11.sp,
                                color = if (pingStatus.contains("ms")) EmeraldReady else SolarGold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Ping Test, Edit Key, Clear Key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTestingPing = provider
                                    pingStatusMap[provider] = "Testing connection..."
                                    onTestPing(provider) { success, latency, msg ->
                                        pingStatusMap[provider] = if (success) {
                                            "Latency: ${latency}ms • Status: 200 OK"
                                        } else {
                                            "Check failed: $msg"
                                        }
                                        isTestingPing = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Speed, null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ping Test", fontSize = 11.sp, color = CyberCyan)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    selectedProviderForEdit = provider
                                    keyInputValue = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianSurfaceElevated,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Key, null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Key", fontSize = 11.sp)
                            }

                            if (isConfigured) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        onRemoveKey(provider)
                                        pingStatusMap.remove(provider)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove key", tint = TextMuted, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
