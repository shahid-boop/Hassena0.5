package com.example.ui.features.avatar3d

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.providers.HasseenaViewModel
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Avatar3DScreen(
    viewModel: HasseenaViewModel,
    onNavigateBack: () -> Unit
) {
    val voiceService = viewModel.voiceService
    val isListening by voiceService.isListening.collectAsStateWithLifecycle()
    val isSpeaking by voiceService.isSpeaking.collectAsStateWithLifecycle()
    val audioLevel by voiceService.audioLevel.collectAsStateWithLifecycle()
    val currentViseme by voiceService.lipSyncEngine.currentViseme.collectAsStateWithLifecycle()
    val voiceError by voiceService.voiceError.collectAsStateWithLifecycle()
    val partialTranscript by voiceService.partialTranscript.collectAsStateWithLifecycle()

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val liveHeadline by viewModel.liveResponseHeadline.collectAsStateWithLifecycle()

    var cameraPreset by remember { mutableStateOf(CameraPreset.WORKSTATION) }
    var avatarStyle by remember { mutableStateOf(AvatarStyle.EXECUTIVE_NAVY) }
    var isPaused by remember { mutableStateOf(false) }
    var userPromptText by remember { mutableStateOf("") }
    var showCustomizationSheet by remember { mutableStateOf(false) }
    var customTaskTitle by remember { mutableStateOf("Ready for Next Task") }
    var customTaskProgress by remember { mutableStateOf(0.0f) }
    var activeCodeSnippet by remember {
        mutableStateOf(
            """
            // HASASENA A1 NEURAL CORE
            class AutonomousTaskExecutor {
                suspend fun dispatch(action: AndroidAction): Result {
                    val context = resolveDeviceContext()
                    val telemetry = deviceTelemetrySnapshot()
                    return aiModelRouter.dispatch(action, telemetry)
                }
            }
            """.trimIndent()
        )
    }

    // Determine current activity state based on real system events
    val currentActivity by remember(isListening, isSpeaking, isGenerating, voiceError) {
        derivedStateOf {
            when {
                voiceError != null -> AvatarActivity.TASK_ERROR
                isListening -> AvatarActivity.LISTENING
                isSpeaking -> AvatarActivity.SPEAKING
                isGenerating -> AvatarActivity.CODING
                else -> AvatarActivity.IDLE
            }
        }
    }

    // Sync task progress and title from system state
    LaunchedEffect(currentActivity, liveHeadline) {
        if (liveHeadline.isNotBlank()) {
            customTaskTitle = liveHeadline
        }
        customTaskProgress = when (currentActivity) {
            AvatarActivity.CODING, AvatarActivity.SPEAKING -> 0.75f
            AvatarActivity.LISTENING -> 0.35f
            AvatarActivity.TASK_ERROR -> 1.0f
            else -> 0.0f
        }
    }

    val workstationState = remember(
        currentActivity,
        customTaskTitle,
        customTaskProgress,
        activeCodeSnippet,
        cameraPreset,
        avatarStyle,
        isPaused,
        audioLevel,
        currentViseme
    ) {
        AvatarWorkstationState(
            activity = currentActivity,
            currentTaskTitle = customTaskTitle,
            currentTaskProgress = customTaskProgress,
            activeCodeSnippet = activeCodeSnippet,
            cameraPreset = cameraPreset,
            avatarStyle = avatarStyle,
            isAnimationPaused = isPaused,
            audioLevel = audioLevel,
            isReady = true,
            viseme = currentViseme
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("avatar_3d_screen"),
        containerColor = Color(0xFF050914),
        topBar = {
            Surface(
                color = Color(0xDD070F1F),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("avatar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "HASASENA A1 • 3D ASSISTANT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "REALISTIC 3D SPECIALIST & WORKSTATION",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isPaused = !isPaused },
                                modifier = Modifier.testTag("avatar_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume Animation" else "Pause Animation (Battery Saver)",
                                    tint = if (isPaused) SolarGold else CyberCyan
                                )
                            }
                            IconButton(
                                onClick = { showCustomizationSheet = true },
                                modifier = Modifier.testTag("avatar_style_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Customize Avatar Style",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    // Camera preset selector chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        items(CameraPreset.values()) { preset ->
                            FilterChip(
                                selected = (cameraPreset == preset),
                                onClick = { cameraPreset = preset },
                                label = { Text(preset.label, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    selectedLeadingIconColor = CyberCyan,
                                    containerColor = ObsidianSurface,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = (cameraPreset == preset),
                                    borderColor = BorderSubtle,
                                    selectedBorderColor = CyberCyan
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Genuine WebGL 3D Avatar & Workstation
            AvatarView(
                state = workstationState,
                mode = AvatarViewMode.WORKSTATION,
                onAvatarReady = {},
                modifier = Modifier.fillMaxSize()
            )

            // 2. HUD Overlays & Task Progress Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (currentActivity != AvatarActivity.IDLE) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCC091325),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentActivity.displayLabel.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentActivity == AvatarActivity.TASK_ERROR) CoralAlert else CyberCyan
                                    )
                                    if (isSpeaking) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CyberCyan.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = "VISEME [${currentViseme.phoneme.uppercase()}]",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${(customTaskProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = EmeraldReady
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { customTaskProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (currentActivity == AvatarActivity.TASK_ERROR) CoralAlert else CyberCyan,
                                trackColor = Color(0xFF14243B)
                            )
                        }
                    }
                }

                // Error Banner
                AnimatedVisibility(
                    visible = !voiceError.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralAlert.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = voiceError ?: "",
                            fontSize = 12.sp,
                            color = CoralAlert,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Live Partial Speech Transcript
                AnimatedVisibility(
                    visible = partialTranscript.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldReady.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = "“$partialTranscript…”",
                            fontSize = 13.sp,
                            color = EmeraldReady,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // 3. Bottom Interactive Interaction Bar
            Surface(
                color = Color(0xE6081020),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Task Action Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    activeCodeSnippet = """
                                        // Auto-generated Task: Memory Repository Sync
                                        suspend fun syncAllUserMemories(userId: String) {
                                            val query = firestore.collection("users").document(userId)
                                            val local = memoryDao.getAllFacts()
                                            query.set(local).await()
                                        }
                                    """.trimIndent()
                                    customTaskTitle = "Writing Cloud Sync Pipeline"
                                    viewModel.submitCommandOrPrompt("Write a Kotlin coroutine function to sync user memories with Firestore")
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Assign Coding Task", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    customTaskTitle = "Synthesizing Android 16 APIs"
                                    viewModel.submitCommandOrPrompt("Research latest Android 16 features and best practices")
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        tint = EmeraldReady,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Web Research", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ObsidianSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable {
                                    customTaskTitle = "Analyzing Local Knowledge Base"
                                    viewModel.submitCommandOrPrompt("Summarize all documents in my RAG knowledge base")
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SolarGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Review Knowledge", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }

                    // Text Input & Mic Control Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Microphone / Barge-In Interrupt Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeaking) CoralAlert
                                    else if (isListening) EmeraldReady
                                    else CyberCyan
                                )
                                .clickable {
                                    if (isSpeaking) {
                                        voiceService.stopSpeakingImmediately()
                                    } else if (isListening) {
                                        voiceService.stopListening()
                                    } else {
                                        voiceService.startListening(bargeInAllowed = true)
                                    }
                                }
                                .testTag("avatar_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isSpeaking) "Interrupt Speech" else "Speak with Avatar",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = userPromptText,
                            onValueChange = { userPromptText = it },
                            placeholder = { Text("Command JARVIS...", fontSize = 13.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = CyberCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("avatar_text_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (userPromptText.isNotBlank()) {
                                    val text = userPromptText.trim()
                                    userPromptText = ""
                                    viewModel.submitCommandOrPrompt(text)
                                }
                            },
                            enabled = userPromptText.isNotBlank(),
                            modifier = Modifier.testTag("avatar_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (userPromptText.isNotBlank()) CyberCyan else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Customization & Style Bottom Sheet
    if (showCustomizationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizationSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0A1224),
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderSubtle) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "CUSTOMIZE 3D VIRTUAL ASSISTANT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
                Text(
                    text = "Professional Adult Female Executive (23-28 yrs) • PBR Rendering",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Appearance & Wardrobe Palette", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarStyle.values().forEach { style ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (avatarStyle == style) Color(0xFF13223D) else ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (avatarStyle == style) CyberCyan else BorderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { avatarStyle = style }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(style.neonHex)))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(style.label, fontSize = 14.sp, color = TextPrimary)
                                }
                                if (avatarStyle == style) {
                                    Text("ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Battery & Performance Optimizer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isPaused) "Rendering is paused. GPU power consumption minimized." else "Running 60 FPS full PBR lighting and continuous skeletal animation.",
                    fontSize = 12.sp,
                    color = if (isPaused) SolarGold else EmeraldReady
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
