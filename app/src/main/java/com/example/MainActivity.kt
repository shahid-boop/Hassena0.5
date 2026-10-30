package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.TextMuted
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.AIProviderType
import com.example.providers.HasseenaDestination
import com.example.providers.HasseenaViewModel
import com.example.services.android.AndroidControlService
import com.example.ui.features.android_controls.AndroidControlsScreen
import com.example.ui.features.automation.AutomationScreen
import com.example.ui.features.avatar3d.Avatar3DScreen
import com.example.ui.features.chat.ChatStreamScreen
import com.example.ui.features.coding.CodingScreen
import com.example.ui.features.connectors.ControlAndPermissionCenterScreen
import com.example.ui.features.home.HomeVoiceScreen
import com.example.ui.features.knowledge.RagKnowledgeScreen
import com.example.ui.features.providers.AiProvidersScreen
import com.example.ui.features.research.ResearchScreen
import com.example.ui.features.settings.SettingsAndMemoryScreen
import com.example.ui.features.visual_lens.VisualLensScreen
import com.example.ui.features.voice.DedicatedVoiceScreen
import com.example.ui.features.workspace.FileWorkspaceScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.HasseenaTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HasseenaTheme {
                val hasseenaViewModel: HasseenaViewModel = viewModel()
                val windowBrightness by hasseenaViewModel.androidControlService.requestedAppWindowBrightness
                    .collectAsStateWithLifecycle()

                LaunchedEffect(windowBrightness) {
                    val target = windowBrightness
                    if (target != null) {
                        val params = window.attributes
                        params.screenBrightness = target.coerceIn(0.05f, 1.0f)
                        window.attributes = params
                    }
                }

                HasseenaAssistantApp(
                    viewModel = hasseenaViewModel
                )
            }
        }
    }
}

@Composable
fun HasseenaAssistantApp(
    viewModel: HasseenaViewModel
) {
    val currentDest by viewModel.currentDestination.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialVoiceTranscript.collectAsStateWithLifecycle()
    val voiceError by viewModel.voiceError.collectAsStateWithLifecycle()
    val liveHeadline by viewModel.liveResponseHeadline.collectAsStateWithLifecycle()
    val liveRouteBadge by viewModel.liveRouteBadge.collectAsStateWithLifecycle()
    val lastLatencyMs by viewModel.lastLatencyMs.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsStateWithLifecycle()
    val telemetry by viewModel.deviceTelemetry.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionsList.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val memoryFacts by viewModel.memoryFacts.collectAsStateWithLifecycle()
    val cloudMemories by viewModel.cloudMemories.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val cacheCount by viewModel.cacheCount.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()

    // Visual Lens states
    val capturedBitmap by viewModel.capturedLensBitmap.collectAsStateWithLifecycle()
    val lensAnalysisText by viewModel.lensAnalysisText.collectAsStateWithLifecycle()
    val isLensAnalyzing by viewModel.isLensAnalyzing.collectAsStateWithLifecycle()

    // Research states
    val researchMode by viewModel.researchMode.collectAsStateWithLifecycle()
    val researchStatus by viewModel.researchStatus.collectAsStateWithLifecycle()
    val researchSummary by viewModel.researchSummary.collectAsStateWithLifecycle()
    val researchSources by viewModel.researchSources.collectAsStateWithLifecycle()
    val isResearching by viewModel.isResearching.collectAsStateWithLifecycle()

    // Refresh permissions when returning from Android Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTelemetryAndPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Contextual Microphone permission launcher (activated only when user taps Orb/Mic)
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshTelemetryAndPermissions()
        if (granted) {
            viewModel.voiceService.startListening(bargeInAllowed = preferences.bargeInEnabled)
        }
    }

    // Contextual runtime permission launcher for Permission Center
    val genericPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshTelemetryAndPermissions()
    }

    // Sensitive Action Confirmation Dialog ("Call this number?", "Send this message?", "Delete memory?")
    val confirmation = pendingConfirmation
    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingConfirmation() },
            containerColor = ObsidianSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(text = confirmation.title)
            },
            text = {
                Text(text = confirmation.description)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPendingAction() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("confirm_sensitive_action_button")
                ) {
                    Text(confirmation.confirmButtonText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissPendingConfirmation() },
                    modifier = Modifier.testTag("cancel_sensitive_action_button")
                ) {
                    Text("Cancel", color = CoralAlert)
                }
            }
        )
    }

    // BackHandler: Return to Home when on any sub-screen
    if (currentDest != HasseenaDestination.HOME) {
        BackHandler {
            viewModel.navigateTo(HasseenaDestination.HOME)
        }
    }

    var showMoreHub by remember { mutableStateOf(false) }

    // Primary Bottom Navigation Bar Items (Matching UI Mockup)
    val primaryNavItems = listOf(
        Triple(HasseenaDestination.HOME, "Home", Icons.Default.Home),
        Triple(HasseenaDestination.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
        Triple(HasseenaDestination.VOICE, "Voice", Icons.Default.Mic),
        Triple(HasseenaDestination.FILE_WORKSPACE, "Files", Icons.Default.FolderOpen)
    )

    // Side Navigation Rail Items for Expanded Screens / Foldables / Tablets
    val railNavItems = listOf(
        Triple(HasseenaDestination.HOME, "Home", Icons.Default.Home),
        Triple(HasseenaDestination.AVATAR_3D, "3D Avatar", Icons.Default.GraphicEq),
        Triple(HasseenaDestination.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
        Triple(HasseenaDestination.VOICE, "Voice", Icons.Default.Mic),
        Triple(HasseenaDestination.CODING, "Coding", Icons.Default.Code),
        Triple(HasseenaDestination.FILE_WORKSPACE, "Files", Icons.Default.FolderOpen),
        Triple(HasseenaDestination.RAG_KNOWLEDGE, "Knowledge", Icons.Default.AutoAwesome),
        Triple(HasseenaDestination.RESEARCH, "Research", Icons.Default.TravelExplore),
        Triple(HasseenaDestination.AUTOMATION, "Automation", Icons.Default.PrecisionManufacturing),
        Triple(HasseenaDestination.CONTROL_CENTER, "Controls", Icons.Default.PhoneAndroid),
        Triple(HasseenaDestination.AI_PROVIDERS, "Providers", Icons.Default.Hub),
        Triple(HasseenaDestination.SETTINGS, "Settings", Icons.Default.Settings)
    )

    // More Hub Modal Dialog (Command Center Quick Jump)
    if (showMoreHub) {
        AlertDialog(
            onDismissRequest = { showMoreHub = false },
            containerColor = ObsidianSurfaceElevated,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("JARVIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan, letterSpacing = 1.sp)
                        Text("Command Modules", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    IconButton(onClick = { showMoreHub = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hubModules = listOf(
                        Triple(HasseenaDestination.AVATAR_3D, "3D Female Assistant", "Realistic 3D specialist & dual-monitor workstation"),
                        Triple(HasseenaDestination.CODING, "Coding Studio", "Syntax editing & code intelligence"),
                        Triple(HasseenaDestination.RAG_KNOWLEDGE, "RAG Knowledge", "Document grounding & citations"),
                        Triple(HasseenaDestination.RESEARCH, "Web Research", "Live search & synthesis"),
                        Triple(HasseenaDestination.AUTOMATION, "Automation", "Autonomous scheduled tasks"),
                        Triple(HasseenaDestination.CONTROL_CENTER, "Android Controls", "Phone hardware & actions"),
                        Triple(HasseenaDestination.AI_PROVIDERS, "AI Providers", "API keys & live latency ping"),
                        Triple(HasseenaDestination.SETTINGS, "Settings & Memory", "Preferences & wake phrase"),
                        Triple(HasseenaDestination.VISUAL_LENS, "Visual Lens", "Camera capture & analysis")
                    )

                    hubModules.forEach { (dest, label, desc) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(dest)
                                    showMoreHub = false
                                }
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(CyberCyan.copy(alpha = 0.12f), CircleShape)
                                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (dest) {
                                            HasseenaDestination.AVATAR_3D -> Icons.Default.GraphicEq
                                            HasseenaDestination.CODING -> Icons.Default.Code
                                            HasseenaDestination.RAG_KNOWLEDGE -> Icons.Default.AutoAwesome
                                            HasseenaDestination.RESEARCH -> Icons.Default.TravelExplore
                                            HasseenaDestination.AUTOMATION -> Icons.Default.PrecisionManufacturing
                                            HasseenaDestination.CONTROL_CENTER -> Icons.Default.PhoneAndroid
                                            HasseenaDestination.AI_PROVIDERS -> Icons.Default.Hub
                                            HasseenaDestination.SETTINGS -> Icons.Default.Settings
                                            else -> Icons.Default.CenterFocusStrong
                                        },
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050711), Color(0xFF0A1022), Color(0xFF070A14))))
    ) {
        Hasseena3DBackdrop(modifier = Modifier.fillMaxSize())
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xEE18243D), Color(0xF5091020))))
                            .border(1.dp, Color(0x8846DDFB), RoundedCornerShape(24.dp)),
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        primaryNavItems.forEach { (dest, label, icon) ->
                            val selected = currentDest == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (selected) CyberCyan else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text(text = label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }

                        // More Button
                        NavigationBarItem(
                            selected = showMoreHub,
                            onClick = { showMoreHub = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More",
                                    tint = if (showMoreHub) CyberCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text(text = "More", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberCyan,
                                selectedTextColor = CyberCyan,
                                indicatorColor = CyberCyan.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_more")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    HasseenaSideNavigationRail(
                        currentDestination = currentDest,
                        items = railNavItems,
                        onSelect = { viewModel.navigateTo(it) }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentDest) {
                        HasseenaDestination.HOME -> HomeVoiceScreen(
                            orbState = orbState,
                            orbConfig = preferences.orbConfig,
                            audioLevel = audioLevel,
                            partialTranscript = partialTranscript,
                            voiceError = voiceError,
                            liveHeadline = liveHeadline,
                            liveRouteBadge = liveRouteBadge,
                            lastLatencyMs = lastLatencyMs,
                            isGenerating = isGenerating,
                            isFlashlightOn = isFlashlightOn,
                            telemetry = telemetry,
                            preferences = preferences,
                            onTapOrb = {
                                viewModel.onOrbTapped(
                                    onRequestMicPermission = {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                )
                            },
                            onLongPressOrb = { viewModel.onOrbLongPressed() },
                            onInterrupt = { viewModel.interruptEverything() },
                            onSubmitCommand = { cmd -> viewModel.submitCommandOrPrompt(cmd) },
                            onToggleFlashlight = {
                                val next = !isFlashlightOn
                                viewModel.submitCommandOrPrompt(
                                    if (next) "Turn on flashlight" else "Turn off flashlight"
                                )
                            },
                            onVolumeAdjust = { up ->
                                viewModel.submitCommandOrPrompt(
                                    if (up) "Increase volume" else "Decrease volume"
                                )
                            },
                            onBrightnessPreset = { pct ->
                                viewModel.submitCommandOrPrompt("Set brightness to $pct")
                            },
                            onCopyResponse = { text ->
                                viewModel.androidControlService.copyToClipboard("JARVIS Response", text)
                            },
                            onSpeakResponse = { text ->
                                viewModel.voiceService.speakImmediate(text)
                            },
                            onOpenChatStream = {
                                viewModel.navigateTo(HasseenaDestination.CHAT)
                            },
                            onOpenVoiceAssistant = {
                                viewModel.navigateTo(HasseenaDestination.VOICE)
                            },
                            onOpenCoding = {
                                viewModel.navigateTo(HasseenaDestination.CODING)
                            },
                            onOpenFiles = {
                                viewModel.navigateTo(HasseenaDestination.FILE_WORKSPACE)
                            },
                            onOpenRagKnowledge = {
                                viewModel.navigateTo(HasseenaDestination.RAG_KNOWLEDGE)
                            },
                            onOpenResearch = {
                                viewModel.navigateTo(HasseenaDestination.RESEARCH)
                            },
                            onOpenAutomation = {
                                viewModel.navigateTo(HasseenaDestination.AUTOMATION)
                            },
                            onOpenControls = {
                                viewModel.navigateTo(HasseenaDestination.CONTROL_CENTER)
                            },
                            onOpenProviders = {
                                viewModel.navigateTo(HasseenaDestination.AI_PROVIDERS)
                            },
                            onOpenAvatar3D = {
                                viewModel.navigateTo(HasseenaDestination.AVATAR_3D)
                            },
                            onSelectActiveModel = { provider, modelId ->
                                viewModel.selectActiveModel(provider, modelId)
                            }
                        )

                        HasseenaDestination.VOICE -> DedicatedVoiceScreen(
                            orbState = orbState,
                            orbConfig = preferences.orbConfig,
                            audioLevel = audioLevel,
                            partialTranscript = partialTranscript,
                            liveHeadline = liveHeadline,
                            onTapOrb = {
                                viewModel.onOrbTapped(
                                    onRequestMicPermission = {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                )
                            },
                            onInterrupt = { viewModel.interruptEverything() },
                            onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) },
                            onNavigateToChat = { viewModel.navigateTo(HasseenaDestination.CHAT) },
                            onOpenSettings = { viewModel.navigateTo(HasseenaDestination.SETTINGS) }
                        )

                        HasseenaDestination.CODING -> CodingScreen(
                            onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) },
                            onCodeAssist = { action, code, lang, onResult ->
                                viewModel.codeAssist(action, code, lang, onResult)
                            },
                            onCopyCode = { code ->
                                viewModel.androidControlService.copyToClipboard("JARVIS Code", code)
                            },
                            viewModel = viewModel
                        )

                        HasseenaDestination.RAG_KNOWLEDGE -> {
                            val knowledgeList by viewModel.knowledgeItems.collectAsStateWithLifecycle()
                            RagKnowledgeScreen(
                                knowledgeItems = knowledgeList,
                                onAddKnowledge = { title, type, content, path ->
                                    viewModel.addKnowledgeItem(title, type, content, path)
                                },
                                onDeleteKnowledge = { id -> viewModel.deleteKnowledgeItem(id) },
                                onAskGroundedQuestion = { query, onResult ->
                                    viewModel.askGroundedKnowledgeQuestion(query, onResult)
                                },
                                onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) },
                                onNavigateToResearch = { viewModel.navigateTo(HasseenaDestination.RESEARCH) }
                            )
                        }

                        HasseenaDestination.AUTOMATION -> {
                            val taskList by viewModel.automationTasks.collectAsStateWithLifecycle()
                            AutomationScreen(
                                tasks = taskList,
                                onCreateTask = { title, desc, cat, trig ->
                                    viewModel.createAutomationTask(title, desc, cat, trig)
                                },
                                onRunTask = { id -> viewModel.runAutomationTask(id) },
                                onRetryTask = { id -> viewModel.retryAutomationTask(id) },
                                onCancelTask = { id -> viewModel.cancelAutomationTask(id) },
                                onDeleteTask = { id -> viewModel.deleteAutomationTask(id) },
                                onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) }
                            )
                        }

                        HasseenaDestination.AI_PROVIDERS -> AiProvidersScreen(
                            isProviderConfigured = { p -> viewModel.aiService.isProviderKeyAvailable(p) },
                            onSaveKey = { p, k -> viewModel.saveProviderKey(p, k) },
                            onRemoveKey = { p -> viewModel.removeProviderKey(p) },
                            onTestPing = { p, cb -> viewModel.testProviderPing(p, cb) },
                            onSelectActiveModel = { p, m -> viewModel.selectActiveModel(p, m) },
                            activeProvider = preferences.selectedProvider,
                            activeModelId = preferences.selectedModelId,
                            onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )

                        HasseenaDestination.CHAT -> ChatStreamScreen(
                            messages = messages,
                            isGenerating = isGenerating,
                            onSendPrompt = { prompt -> viewModel.submitCommandOrPrompt(prompt) },
                            onVoiceMicTap = {
                                viewModel.onOrbTapped(
                                    onRequestMicPermission = {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                )
                            },
                            onInterrupt = { viewModel.interruptEverything() },
                            onSpeakMessage = { text -> viewModel.voiceService.speakImmediate(text) },
                            onCopyMessage = { text ->
                                viewModel.androidControlService.copyToClipboard("JARVIS Message", text)
                            },
                            onShareMessage = { text ->
                                viewModel.androidControlService.shareTextContent(text)
                            },
                            onClearHistory = { viewModel.clearAllConversations() },
                            onNavigateBackHome = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )

                        HasseenaDestination.VISUAL_LENS -> VisualLensScreen(
                            capturedBitmap = capturedBitmap,
                            analysisText = lensAnalysisText,
                            isAnalyzing = isLensAnalyzing,
                            onBitmapCaptured = { bmp -> viewModel.setLensBitmap(bmp) },
                            onAnalyzePrompt = { prompt -> viewModel.analyzeVisualLensImage(prompt) },
                            onAnalyzeActiveScreen = { viewModel.analyzeActiveScreenWithAccessibility() },
                            onSpeakText = { text -> viewModel.voiceService.speakImmediate(text) },
                            onNavigateBackHome = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )

                        HasseenaDestination.RESEARCH -> ResearchScreen(
                            selectedMode = researchMode,
                            isResearching = isResearching,
                            researchStatus = researchStatus,
                            researchSummary = researchSummary,
                            researchSources = researchSources,
                            onSelectMode = { mode -> viewModel.setResearchMode(mode) },
                            onExecuteResearch = { query -> viewModel.runWebResearch(query) },
                            onOpenSourceUrl = { url -> viewModel.appLauncherService.openUrl(url) },
                            onSpeakSummary = { text -> viewModel.voiceService.speakImmediate(text) },
                            onNavigateBackHome = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )

                        HasseenaDestination.CONTROL_CENTER -> AndroidControlsScreen(
                            telemetry = telemetry,
                            installedApps = installedApps,
                            isFlashlightOn = isFlashlightOn,
                            onToggleFlashlight = { enable ->
                                viewModel.androidControlService.setFlashlight(enable)
                                viewModel.refreshTelemetryAndPermissions()
                            },
                            onSetBrightness = { pct ->
                                viewModel.androidControlService.setScreenBrightnessPercent(pct)
                                viewModel.refreshTelemetryAndPermissions()
                            },
                            onSetVolume = { pct ->
                                viewModel.androidControlService.adjustVolume(
                                    AndroidControlService.VolumeDirection.SET_PERCENT,
                                    pct
                                )
                                viewModel.refreshTelemetryAndPermissions()
                            },
                            onLaunchApp = { app ->
                                viewModel.appLauncherService.launchPackageDirectly(app.packageName, app.appName)
                            },
                            onMakeCall = { num ->
                                viewModel.androidControlService.executeConfirmedPhoneCall(num)
                            },
                            onSendMessage = { recipient, body ->
                                viewModel.androidControlService.executeConfirmedSmsCompose(recipient, body)
                            },
                            onOpenSearch = { query ->
                                val encoded = runCatching { java.net.URLEncoder.encode(query, "UTF-8") }.getOrDefault(query)
                                viewModel.appLauncherService.openUrl("https://www.google.com/search?q=$encoded")
                            },
                            onOpenSystemSettings = { action ->
                                viewModel.androidControlService.openSystemSettingsScreen(action)
                            },
                            onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )

                        HasseenaDestination.FILE_WORKSPACE -> FileWorkspaceScreen(viewModel)

                        HasseenaDestination.SETTINGS -> SettingsAndMemoryScreen(
                            preferences = preferences,
                            memoryFacts = memoryFacts,
                            cloudMemoryCount = cloudMemories.size,
                            cloudSyncStatus = cloudSyncStatus,
                            cacheCount = cacheCount,
                            isProviderKeyConfigured = { provider ->
                                viewModel.aiService.isProviderKeyAvailable(provider)
                            },
                            onSelectRoutingMode = { mode -> viewModel.updateRoutingMode(mode) },
                            onSelectProviderAndModel = { provider, modelId ->
                                viewModel.updateProviderAndModel(provider, modelId)
                            },
                            onSelectPersonality = { persona -> viewModel.updatePersonality(persona) },
                            onToggleMemory = { enabled -> viewModel.updateMemoryEnabled(enabled) },
                            onToggleCache = { enabled -> viewModel.updateCacheEnabled(enabled) },
                            onUpdateCustomInstructions = { instr ->
                                viewModel.updateCustomInstructions(instr)
                            },
                            onAddMemoryFact = { cat, content -> viewModel.addMemoryFact(cat, content) },
                            onDeleteMemoryFact = { id -> viewModel.deleteMemoryFact(id) },
                            onRequestDeleteAllMemory = { viewModel.requestClearMemoryConfirmation() },
                            onClearConversations = { viewModel.clearAllConversations() },
                            onSyncLocalToCloud = { viewModel.syncLocalMemoriesToCloud() },
                            onUpdateOrbConfig = { cfg -> viewModel.updateOrbConfig(cfg) },
                            onUpdateWakeWord = { enabled, phrase ->
                                viewModel.updateWakeWordConfig(enabled, phrase)
                            },
                            onUpdateVoicePrefs = { cont, tts, barge, hapt ->
                                viewModel.updateVoicePreferences(cont, tts, barge, hapt)
                            },
                            onNavigateBackHome = { viewModel.navigateTo(HasseenaDestination.HOME) },
                            onSaveApiKey = { provider, key ->
                                runCatching {
                                    viewModel.apiKeyStore.save(provider, key)
                                }.onFailure { error ->
                                    android.util.Log.e("ApiKeyManager", "Could not save API key", error)
                                }
                            },
                            onRemoveApiKey = { provider ->
                                runCatching {
                                    viewModel.apiKeyStore.remove(provider)
                                }.onFailure { error ->
                                    android.util.Log.e("ApiKeyManager", "Could not remove API key", error)
                                }
                              },
                            onTestApiKey = { provider, key, onResult ->
                                viewModel.testApiKey(provider, key, onResult)
                            },
                            onSaveVoiceApiKey = { key -> viewModel.voiceApiKeyStore.saveApiKey(key) },
                            onSaveVoiceId = { id -> viewModel.voiceApiKeyStore.saveVoiceId("hasseena_custom", id) },
                            currentVoicePack = viewModel.voiceApiKeyStore.getSelectedVoicePack(),
                            onSaveVoiceIdForPack = { packId, id -> viewModel.voiceApiKeyStore.saveVoiceId(packId, id) },
                            onSelectVoicePack = { packId -> viewModel.voiceApiKeyStore.saveSelectedVoicePack(packId) },
                            onGetVoiceIdForPack = { packId -> viewModel.voiceApiKeyStore.getVoiceId(packId) },
                            onTestElevenLabsVoice = { voiceId, onResult -> viewModel.testElevenLabsVoice(voiceId, onResult) }
                        )

                        HasseenaDestination.AVATAR_3D -> Avatar3DScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(HasseenaDestination.HOME) }
                        )
                    }
                }
            }
        }
    }
}

/** Shared ambient sci-fi backdrop used behind every authenticated destination. */
@Composable
private fun Hasseena3DBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Soft cyan/violet volumetric glows.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3320DFFF), Color.Transparent),
                center = Offset(w * 0.12f, h * 0.18f),
                radius = w * 0.58f
            ), radius = w * 0.58f, center = Offset(w * 0.12f, h * 0.18f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2D8B5CFF), Color.Transparent),
                center = Offset(w * 0.94f, h * 0.68f),
                radius = w * 0.62f
            ), radius = w * 0.62f, center = Offset(w * 0.94f, h * 0.68f)
        )
        // Fine perspective grid, intentionally subtle so content remains readable.
        val gridColor = Color(0x1429C8EF)
        val gap = 34.dp.toPx()
        var x = 0f
        while (x <= w) {
            drawLine(gridColor, Offset(x, h * 0.60f), Offset(x, h), strokeWidth = 0.7.dp.toPx())
            x += gap
        }
        var y = h * 0.62f
        while (y <= h) {
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.7.dp.toPx())
            y += gap
        }
        // Holographic orbit rings in the upper field.
        val center = Offset(w * 0.83f, h * 0.13f)
        listOf(54f, 72f, 91f).forEachIndexed { index, radiusDp ->
            drawCircle(
                color = if (index == 1) Color(0x2445DDF5) else Color(0x1B9C76FF),
                radius = radiusDp.dp.toPx(),
                center = center,
                style = Stroke(width = if (index == 1) 1.2.dp.toPx() else 0.7.dp.toPx())
            )
        }
        drawCircle(Color(0x6685F3FF), radius = 2.2.dp.toPx(), center = center)
    }
}

@Composable
private fun HasseenaBottomNavigationBar(
    currentDestination: HasseenaDestination,
    items: List<Triple<HasseenaDestination, String, ImageVector>>,
    onSelect: (HasseenaDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEE18243D), Color(0xF5091020))))
            .border(1.dp, Color(0x8846DDFB), RoundedCornerShape(24.dp)),
        containerColor = Color.Transparent,
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        items.forEach { (dest, label, icon) ->
            val selected = currentDestination == dest
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    // Raised holographic icon tile; preserves the original Material icon semantics.
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = if (selected) 12.dp else 5.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = if (selected) CyberCyan else Color(0xFF415B88),
                                spotColor = if (selected) CyberCyan else Color(0xFF415B88)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    if (selected) listOf(Color(0xFF9AF7FF), Color(0xFF37BDEB), Color(0xFF6158D9))
                                    else listOf(Color(0xFF263954), Color(0xFF111C31), Color(0xFF202A47))
                                )
                            )
                            .border(
                                width = if (selected) 1.4.dp else 0.8.dp,
                                color = if (selected) Color(0xFFD2FCFF) else Color(0x664BDCF5),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color(0xFF071525) else Color(0xFF9CEBFA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(text = label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_${dest.route}")
            )
        }
    }
}

@Composable
private fun HasseenaSideNavigationRail(
    currentDestination: HasseenaDestination,
    items: List<Triple<HasseenaDestination, String, ImageVector>>,
    onSelect: (HasseenaDestination) -> Unit
) {
    NavigationRail(
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        items.forEach { (dest, label, icon) ->
            val selected = currentDestination == dest
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = if (selected) 12.dp else 5.dp,
                                shape = RoundedCornerShape(15.dp),
                                ambientColor = if (selected) CyberCyan else Color(0xFF415B88),
                                spotColor = if (selected) CyberCyan else Color(0xFF415B88)
                            )
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    if (selected) listOf(Color(0xFF9AF7FF), Color(0xFF37BDEB), Color(0xFF6158D9))
                                    else listOf(Color(0xFF263954), Color(0xFF111C31), Color(0xFF202A47))
                                )
                            )
                            .border(
                                width = if (selected) 1.4.dp else 0.8.dp,
                                color = if (selected) Color(0xFFD2FCFF) else Color(0x664BDCF5),
                                shape = RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color(0xFF071525) else Color(0xFF9CEBFA),
                            modifier = Modifier.size(23.dp)
                        )
                    }
                },
                label = {
                    Text(text = label)
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("rail_${dest.route}")
            )
        }
    }
}
