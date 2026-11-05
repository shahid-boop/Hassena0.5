package com.example.ui.features.home

import androidx.compose.animation.AnimatedVisibility
import com.example.ui.features.avatar3d.AvatarActivity
import com.example.ui.features.avatar3d.AvatarView
import com.example.ui.features.avatar3d.AvatarViewMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AIModelOption
import com.example.models.AIProviderType
import com.example.models.DeviceTelemetrySnapshot
import com.example.models.OrbConfig
import com.example.models.OrbState
import com.example.models.SupportedModelsCatalog
import com.example.services.memory.HasseenaUserPreferences
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.QuantumVioletLight
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LiquidGlassTokens
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.liquidGlass
import com.example.ui.widgets.orb.HasseenaOrb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeVoiceScreen(
    orbState: OrbState,
    orbConfig: OrbConfig,
    audioLevel: Float,
    partialTranscript: String,
    voiceError: String?,
    liveHeadline: String,
    liveRouteBadge: String,
    lastLatencyMs: Long,
    isGenerating: Boolean,
    isFlashlightOn: Boolean,
    telemetry: DeviceTelemetrySnapshot,
    preferences: HasseenaUserPreferences,
    onTapOrb: () -> Unit,
    onLongPressOrb: () -> Unit,
    onInterrupt: () -> Unit,
    onSubmitCommand: (String) -> Unit,
    onToggleFlashlight: () -> Unit,
    onVolumeAdjust: (Boolean) -> Unit,
    onBrightnessPreset: (Int) -> Unit,
    onCopyResponse: (String) -> Unit,
    onSpeakResponse: (String) -> Unit,
    onOpenChatStream: () -> Unit,
    onOpenVoiceAssistant: () -> Unit = {},
    onOpenCoding: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenRagKnowledge: () -> Unit = {},
    onOpenResearch: () -> Unit = {},
    onOpenAutomation: () -> Unit = {},
    onOpenControls: () -> Unit = {},
    onOpenProviders: () -> Unit = {},
    onOpenAvatar3D: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onOpenHealing: () -> Unit = {},
    onSelectActiveModel: (AIProviderType, String) -> Unit = { _, _ -> }
) {
    var commandText by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var showModelDropdown by remember { mutableStateOf(false) }

    val currentTimeStr = remember {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }
    val currentDateStr = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
    }

    val quickLocalCommands = listOf(
        "Turn on flashlight",
        "Show battery",
        "Open YouTube",
        "Set brightness to 50",
        "Check system status",
        "Search latest AI research",
        "Build daily report"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("home_voice_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. TOP HEADER: LOGO, WEATHER, CLOCK & USER AVATAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(10.dp, CircleShape, spotColor = CyberCyan)
                        .background(Brush.linearGradient(listOf(Color(0x4D00F0FF), Color(0x33A855F7))), CircleShape)
                        .border(1.2.dp, Brush.linearGradient(listOf(CyberCyan, QuantumVioletLight)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Hasseena Logo",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "HASSEENA A1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "AUTONOMOUS AI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Weather & Clock Pill with 3D Liquid Glass
            Box(
                modifier = Modifier
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 6.dp,
                        borderBrush = LiquidGlassTokens.GlassSubtleBorderBrush,
                        surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Weather",
                        tint = SolarGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "24°C Partly Cloudy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$currentTimeStr • $currentDateStr",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. SYSTEM STATUS ROW (CPU, MEMORY, DISK, GPU NEON GAUGES)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // System Status Card (Left 55%) - 3D Liquid Glass
            LiquidGlassCard(
                modifier = Modifier.weight(1.15f),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp,
                borderBrush = LiquidGlassTokens.GlassRimBrush,
                surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "System Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusGaugeItem(label = "CPU", value = "12%", percent = 0.12f, color = CyberCyan)
                        StatusGaugeItem(label = "Memory", value = "${telemetry.batteryPercent}%", percent = telemetry.batteryPercent / 100f, color = QuantumViolet)
                        StatusGaugeItem(label = "Disk", value = "32%", percent = 0.32f, color = SolarGold)
                        StatusGaugeItem(label = "GPU", value = "5%", percent = 0.05f, color = EmeraldReady)
                    }
                }
            }

            // AI Model Card (Right 45%) - 3D Liquid Glass
            LiquidGlassCard(
                modifier = Modifier.weight(0.85f),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp,
                borderBrush = LiquidGlassTokens.GlassSubtleBorderBrush,
                surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush,
                onClick = { showModelDropdown = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "AI Model",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(10.dp),
                                elevation = 2.dp,
                                borderBrush = LiquidGlassTokens.GlassSubtleBorderBrush,
                                surfaceBrush = Brush.verticalGradient(
                                    listOf(Color(0x40162545), Color(0x330B1220))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preferences.selectedModelId.ifEmpty { "gemini-3.8-flash" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .shadow(4.dp, CircleShape, spotColor = EmeraldReady)
                                .background(EmeraldReady, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Connected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldReady
                        )
                    }

                    DropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false },
                        modifier = Modifier
                            .background(Color(0xEB0D1527))
                            .border(1.dp, LiquidGlassTokens.GlassSubtleBorderBrush, RoundedCornerShape(12.dp))
                    ) {
                        SupportedModelsCatalog.models.take(6).forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.displayName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${model.provider.displayName} • ${model.badge}", color = TextMuted, fontSize = 10.sp)
                                    }
                                },
                                onClick = {
                                    onSelectActiveModel(model.provider, model.modelId)
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. QUICK ACTIONS GRID (MATCHING SCREEN 2 OF MOCKUP)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quick Actions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Chat",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    tint = CyberCyan,
                    onClick = onOpenChatStream
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Voice",
                    icon = Icons.Default.Mic,
                    tint = CyberCyan,
                    onClick = onOpenVoiceAssistant
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Files",
                    icon = Icons.Default.FolderOpen,
                    tint = CyberCyan,
                    onClick = onOpenFiles
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "RAG",
                    icon = Icons.Default.AutoAwesome,
                    tint = CyberCyan,
                    onClick = onOpenRagKnowledge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Coding",
                    icon = Icons.Default.Code,
                    tint = QuantumViolet,
                    onClick = onOpenCoding
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Research",
                    icon = Icons.Default.TravelExplore,
                    tint = QuantumViolet,
                    onClick = onOpenResearch
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Automation",
                    icon = Icons.Default.PrecisionManufacturing,
                    tint = QuantumViolet,
                    onClick = onOpenAutomation
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Controls",
                    icon = Icons.Default.PhoneAndroid,
                    tint = QuantumViolet,
                    onClick = onOpenControls
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_autonomous_agent_tile"),
                shape = RoundedCornerShape(16.dp),
                elevation = 6.dp,
                borderBrush = LiquidGlassTokens.GlassRimBrush,
                surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush,
                onClick = { onOpenAgent() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, CircleShape, spotColor = CyberCyan)
                            .background(Brush.linearGradient(listOf(Color(0x4D00F0FF), Color(0x33A855F7))), CircleShape)
                            .border(1.2.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Autonomous Agent Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = CyberCyan)
                                    .background(CyberCyan.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .border(0.8.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "NEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            }
                        }
                        Text(
                            text = "Multi-step planning, tool execution, & real Android telemetry",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Self-Healing AI Watchdog Tile - 3D Liquid Glass
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_self_healing_tile"),
                shape = RoundedCornerShape(16.dp),
                elevation = 6.dp,
                borderBrush = Brush.linearGradient(listOf(Color(0x8000E5FF), Color(0x4000E676))),
                surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush,
                onClick = { onOpenHealing() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, CircleShape, spotColor = Color(0xFF00E5FF))
                            .background(Brush.linearGradient(listOf(Color(0x4D00E5FF), Color(0x3300E676))), CircleShape)
                            .border(1.2.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Self-Healing AI Watchdog",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(6.dp), spotColor = Color(0xFF00E676))
                                    .background(Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .border(0.8.dp, Color(0xFF00E676).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                            }
                        }
                        Text(
                            text = "Automated error scrubbing, root cause diagnosis, & safe patch rollback",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. SIGNATURE JARVIS ARC REACTOR ORB CENTERPIECE
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            HasseenaOrb(
                orbState = orbState,
                orbConfig = orbConfig,
                audioLevel = audioLevel,
                onTapOrb = onTapOrb,
                onLongPressOrb = onLongPressOrb
            )
        }

        // Orb State Pill & Interrupt Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val stateColor = when (orbState) {
                OrbState.IDLE -> CyberCyan
                OrbState.LISTENING -> EmeraldReady
                OrbState.THINKING -> AmberThinking
                OrbState.SPEAKING -> CyberCyan
                OrbState.EXECUTING -> QuantumViolet
                OrbState.ERROR -> CoralAlert
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = stateColor.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${orbState.label} • ${orbState.statusSubtitle}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }

            AnimatedVisibility(visible = orbState == OrbState.SPEAKING || orbState == OrbState.THINKING || isGenerating) {
                Button(
                    onClick = onInterrupt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAlert,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("interrupt_barge_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = "Interrupt JARVIS",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interrupt", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (preferences.continuousConversation || preferences.wakeWordEnabled) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    if (preferences.continuousConversation) append("Continuous Mode ON ")
                    if (preferences.wakeWordEnabled) append("• Wake Phrase: \"${preferences.wakePhrase}\"")
                },
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldReady
            )
        }

        if (!voiceError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CoralAlert.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = voiceError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoralAlert,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3D VIRTUAL SPECIALIST HERO CARD - 3D Liquid Glass
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_3d_avatar_card"),
            shape = RoundedCornerShape(18.dp),
            elevation = 8.dp,
            borderBrush = LiquidGlassTokens.GlassRimBrush,
            surfaceBrush = LiquidGlassTokens.GlassElevatedBrush,
            onClick = { onOpenAvatar3D() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val avatarActivity = when (orbState) {
                        OrbState.SPEAKING -> AvatarActivity.SPEAKING
                        OrbState.LISTENING -> AvatarActivity.LISTENING
                        OrbState.THINKING -> AvatarActivity.THINKING
                        OrbState.EXECUTING -> AvatarActivity.CODING
                        OrbState.ERROR -> AvatarActivity.TASK_ERROR
                        OrbState.IDLE -> AvatarActivity.IDLE
                    }
                    AvatarView(
                        activity = avatarActivity,
                        audioLevel = audioLevel,
                        mode = AvatarViewMode.PIP_BUBBLE,
                        showStatusBadge = false,
                        modifier = Modifier.size(48.dp),
                        onClick = onOpenAvatar3D
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "3D FEMALE ASSISTANT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(4.dp), spotColor = EmeraldReady)
                                    .background(EmeraldReady.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                    .border(0.8.dp, EmeraldReady.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "60 FPS PBR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldReady
                                )
                            }
                        }
                        Text(
                            text = "Realistic adult specialist at dual-monitor workstation with live lip-sync & coding",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Open 3D Assistant",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. LIVE ASSISTANT RESPONSE CARD - 3D Liquid Glass
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_response_card"),
            shape = RoundedCornerShape(18.dp),
            elevation = 8.dp,
            borderBrush = LiquidGlassTokens.GlassRimBrush,
            surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = liveRouteBadge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberCyan
                    )
                    if (lastLatencyMs > 0L) {
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp), spotColor = EmeraldReady)
                                .background(EmeraldReady.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                .border(0.8.dp, EmeraldReady.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${lastLatencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldReady
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (partialTranscript.isNotBlank()) {
                    Text(
                        text = "“$partialTranscript…”",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldReady
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = liveHeadline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open full chat →",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenChatStream() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onSpeakResponse(liveHeadline) },
                            modifier = Modifier.testTag("speak_response_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response aloud",
                                tint = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onCopyResponse(liveHeadline) },
                            modifier = Modifier.testTag("copy_response_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy response text",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. INSTANT COMMAND CHIPS - 3D Liquid Glass Pills
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "INSTANT VOICE & PHONE ACTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickLocalCommands.forEach { cmd ->
                    LiquidGlassPill(
                        text = cmd,
                        leadingDotColor = CyberCyan,
                        onClick = { onSubmitCommand(cmd) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. INPUT BAR - 3D Liquid Glass Dock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_command_input_field"),
                placeholder = {
                    Text(
                        text = "Type or tap mic for Hasseena A1...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val trimmed = commandText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSubmitCommand(trimmed)
                            commandText = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x800F1B33),
                    unfocusedContainerColor = Color(0x660B1220),
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = LiquidGlassTokens.GlassSubtleBorderBrush.let { BorderSubtle },
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Mic Button - 3D Glass Floating Button
            IconButton(
                onClick = onTapOrb,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(10.dp, CircleShape, spotColor = CyberCyan)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CyberCyan, CyberCyanDim)))
                    .border(1.2.dp, Color(0x80FFFFFF), CircleShape)
                    .testTag("home_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak voice command",
                    tint = Color(0xFF030712),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send Button - 3D Glass Floating Button
            IconButton(
                onClick = {
                    val trimmed = commandText.trim()
                    if (trimmed.isNotEmpty()) {
                        onSubmitCommand(trimmed)
                        commandText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, spotColor = QuantumViolet)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0x66162545), Color(0x99111C35))))
                    .border(1.2.dp, LiquidGlassTokens.GlassSubtleBorderBrush, CircleShape)
                    .testTag("home_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Submit command",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatusGaugeItem(
    label: String,
    value: String,
    percent: Float,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, CircleShape, spotColor = color.copy(alpha = 0.5f))
                .background(Color(0x33090F1C), CircleShape)
                .border(0.8.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                val strokeWidth = 3.5.dp.toPx()
                drawCircle(
                    color = color.copy(alpha = 0.15f),
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * percent.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = 4.dp,
        borderBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.5f), Color(0x22FFFFFF))),
        surfaceBrush = LiquidGlassTokens.GlassSurfaceBrush,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape, spotColor = tint)
                    .background(tint.copy(alpha = 0.18f), CircleShape)
                    .border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}
