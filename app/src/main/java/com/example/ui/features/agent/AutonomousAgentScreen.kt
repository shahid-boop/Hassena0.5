package com.example.ui.features.agent

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import com.example.models.AgentToolDomain
import com.example.providers.HasseenaDestination
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.AgentStep
import com.example.models.AutonomousStepStatus
import com.example.models.AgentTask
import com.example.models.AgentTaskStatus
import com.example.providers.HasseenaViewModel
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutonomousAgentScreen(
    viewModel: HasseenaViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentTask by viewModel.currentAgentTask.collectAsStateWithLifecycle()
    val logs by viewModel.agentLogs.collectAsStateWithLifecycle()
    val history by viewModel.agentTasksHistory.collectAsStateWithLifecycle()

    var objectiveInput by rememberSaveable { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }
    var expandedLogs by remember { mutableStateOf(false) }
    var expandedStepId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll to top or current step when task updates
    LaunchedEffect(currentTask?.currentStepIndex, currentTask?.status) {
        if (currentTask != null && (currentTask?.currentStepIndex ?: 0) > 0) {
            listState.animateScrollToItem(1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("autonomous_agent_screen"),
        containerColor = ObsidianBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Autonomous Mission Control",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Six-in-One Autonomous AI Agent System",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("agent_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showToolsDialog = true },
                        modifier = Modifier.testTag("agent_tools_catalog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tool Catalog",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("agent_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Task History",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Six-in-One Capabilities Status Banner
            item {
                SixInOneCapabilitiesCard(
                    onSelectCapability = { prompt ->
                        objectiveInput = prompt
                        viewModel.submitAgentObjective(prompt)
                    }
                )
            }

            // 1. Objective Input Card
            item {
                ObjectiveInputCard(
                    objectiveInput = objectiveInput,
                    onObjectiveChange = { objectiveInput = it },
                    isPlanning = currentTask?.status == AgentTaskStatus.PLANNING,
                    onSubmit = {
                        if (objectiveInput.isNotBlank()) {
                            viewModel.submitAgentObjective(objectiveInput)
                        }
                    },
                    onSelectPreset = { preset ->
                        objectiveInput = preset
                        viewModel.submitAgentObjective(preset)
                    }
                )
            }

            // 2. Sensitive Action Permission Banner (if needed)
            if (currentTask?.status == AgentTaskStatus.WAITING_FOR_PERMISSION) {
                val pendingStep = currentTask?.steps?.getOrNull(currentTask?.currentStepIndex ?: -1)
                item {
                    PendingPermissionCard(
                        step = pendingStep,
                        onAuthorize = { viewModel.confirmAgentPendingStep(true) },
                        onDeny = { viewModel.confirmAgentPendingStep(false) }
                    )
                }
            }

            // 3. Current Task Overview & Control Bar
            currentTask?.let { task ->
                item {
                    TaskOverviewCard(
                        task = task,
                        onApprove = { viewModel.approveAgentPlan() },
                        onPause = { viewModel.pauseAgent() },
                        onResume = { viewModel.resumeAgent() },
                        onCancel = { viewModel.cancelAgent() },
                        onRetry = { viewModel.retryAgentStep(task.currentStepIndex + 1) }
                    )
                }

                // 3b. Changed Files Card
                if (task.changedFiles.isNotEmpty()) {
                    item {
                        ChangedFilesCard(
                            files = task.changedFiles,
                            onOpenFileInWorkspace = {
                                viewModel.navigateTo(HasseenaDestination.FILE_WORKSPACE)
                            }
                        )
                    }
                }

                // 4. Step Cards
                if (task.steps.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Execution Steps (${task.steps.size})",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current: Step ${(task.currentStepIndex + 1).coerceAtMost(task.steps.size)} of ${task.steps.size}",
                                color = CyberCyan,
                                fontSize = 12.sp
                            )
                        }
                    }

                    items(task.steps) { step ->
                        val spec = viewModel.agentToolRegistry.getToolSpec(step.toolId)
                        val domainLabel = spec?.domain?.label ?: "Autonomous Tool"
                        StepExecutionCard(
                            step = step,
                            domainLabel = domainLabel,
                            isExpanded = expandedStepId == step.id,
                            onToggleExpand = {
                                expandedStepId = if (expandedStepId == step.id) null else step.id
                            },
                            onRetry = { viewModel.retryAgentStep(step.stepNumber) },
                            onOpenSelfHealing = { viewModel.navigateTo(HasseenaDestination.SELF_HEALING) }
                        )
                    }
                }

                // 5. Final Report Card (when completed)
                if (task.status == AgentTaskStatus.COMPLETED && !task.finalResponse.isNullOrBlank()) {
                    item {
                        ExecutiveReportCard(
                            reportText = task.finalResponse,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(task.finalResponse))
                                Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onSpeak = {
                                viewModel.voiceService.speakImmediate(task.finalResponse.take(500))
                            }
                        )
                    }
                }
            }

            // 6. Live Telemetry Terminal Logs
            item {
                CollapsibleTerminalLogs(
                    logs = logs,
                    isExpanded = expandedLogs,
                    onToggle = { expandedLogs = !expandedLogs }
                )
            }
        }
    }

    // Task History Bottom Sheet
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = ObsidianSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Task History (${history.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showHistorySheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (history.isEmpty()) {
                    Text(
                        text = "No saved autonomous tasks yet. Submit an objective to see it recorded here.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { hTask ->
                            HistoryTaskItem(
                                task = hTask,
                                onSelect = {
                                    viewModel.loadAgentTask(hTask)
                                    showHistorySheet = false
                                },
                                onDelete = { viewModel.deleteAgentTask(hTask.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Tool Catalog Dialog
    if (showToolsDialog) {
        AlertDialog(
            onDismissRequest = { showToolsDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Agent Tool Catalog", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.agentToolRegistry.toolSpecs) { spec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = spec.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = spec.toolId, color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = spec.description, color = TextSecondary, fontSize = 11.sp)
                            if (spec.requiresConfirmation) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "🔒 Requires Explicit User Authorization", color = CoralAlert, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToolsDialog = false }) {
                    Text("Close", color = CyberCyan)
                }
            }
        )
    }
}

@Composable
private fun ObjectiveInputCard(
    objectiveInput: String,
    onObjectiveChange: (String) -> Unit,
    isPlanning: Boolean,
    onSubmit: () -> Unit,
    onSelectPreset: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_objective_input_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Autonomous Objective",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Give Hasseena a complex goal. The agent plans steps, selects tools, and executes them sequentially.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = objectiveInput,
                onValueChange = onObjectiveChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .testTag("agent_objective_text_field"),
                placeholder = {
                    Text(
                        "e.g., Check battery & system telemetry, perform deep web research on topological quantum computing, and compile an executive summary.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberCyan,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = "Tool 1: Refactor Code",
                    onClick = { onSelectPreset("Read app/build.gradle.kts and propose clean dependency refactoring.") }
                )
                PresetChip(
                    label = "Tool 2: Build & Tests",
                    onClick = { onSelectPreset("Run project build check and execute unit test suite.") }
                )
                PresetChip(
                    label = "Tool 3: Quantum Research",
                    onClick = { onSelectPreset("Research latest breakthroughs in topological qubits and prepare a verified summary.") }
                )
                PresetChip(
                    label = "Tool 4: Screen & Telemetry",
                    onClick = { onSelectPreset("Inspect the active screen via Accessibility and read device telemetry.") }
                )
                PresetChip(
                    label = "Tool 5: Knowledge & Memory",
                    onClick = { onSelectPreset("Search local knowledge base for system protocols and save a memory directive.") }
                )
                PresetChip(
                    label = "System Audit & Health",
                    onClick = { onSelectPreset("Check device battery, RAM, free storage, and network status and summarize health.") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSubmit,
                enabled = objectiveInput.isNotBlank() && !isPlanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("agent_plan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.White,
                    disabledContainerColor = CyberCyanDim.copy(alpha = 0.5f),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isPlanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing & Generating Plan...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Plan & Execute Objective", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = ObsidianSurfaceGlass,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PendingPermissionCard(
    step: AgentStep?,
    onAuthorize: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_pending_permission_card"),
        colors = CardDefaults.cardColors(containerColor = CoralAlert.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CoralAlert))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CoralAlert, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explicit Authorization Required",
                    color = CoralAlert,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step ${(step?.stepNumber ?: 0)}: ${step?.title ?: "Sensitive Action"}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tool: ${step?.toolId} with arguments: ${step?.toolArguments}",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("agent_permission_deny_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Deny & Skip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAuthorize,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("agent_permission_authorize_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Authorize Action", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TaskOverviewCard(
    task: AgentTask,
    onApprove: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_task_overview_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.executionPlanOverview,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (task.replanCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚡ Plan adapted dynamically (${task.replanCount} replans applied)",
                    color = AmberThinking,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (task.status) {
                    AgentTaskStatus.WAITING_FOR_APPROVAL, AgentTaskStatus.READY -> {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("agent_approve_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Approve & Start Execution", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AgentTaskStatus.RUNNING -> {
                        Button(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("agent_pause_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberThinking, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("agent_cancel_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AgentTaskStatus.PAUSED -> {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("agent_resume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AgentTaskStatus.FAILED -> {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("agent_retry_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Remaining", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: AgentTaskStatus) {
    val (bgColor, textColor, label) = when (status) {
        AgentTaskStatus.PLANNING -> Triple(AmberThinking.copy(alpha = 0.2f), AmberThinking, "PLANNING")
        AgentTaskStatus.WAITING_FOR_APPROVAL -> Triple(AmberThinking.copy(alpha = 0.2f), AmberThinking, "AWAITING APPROVAL")
        AgentTaskStatus.READY -> Triple(CyberCyan.copy(alpha = 0.2f), CyberCyan, "READY")
        AgentTaskStatus.RUNNING -> Triple(CyberCyan.copy(alpha = 0.2f), CyberCyan, "RUNNING")
        AgentTaskStatus.PAUSED -> Triple(AmberThinking.copy(alpha = 0.2f), AmberThinking, "PAUSED")
        AgentTaskStatus.WAITING_FOR_PERMISSION -> Triple(CoralAlert.copy(alpha = 0.2f), CoralAlert, "PERMISSION NEEDED")
        AgentTaskStatus.COMPLETED -> Triple(EmeraldReady.copy(alpha = 0.2f), EmeraldReady, "COMPLETED")
        AgentTaskStatus.FAILED -> Triple(CoralAlert.copy(alpha = 0.2f), CoralAlert, "FAILED")
        AgentTaskStatus.CANCELLED -> Triple(TextMuted.copy(alpha = 0.2f), TextMuted, "CANCELLED")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepExecutionCard(
    step: AgentStep,
    domainLabel: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRetry: () -> Unit,
    onOpenSelfHealing: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("agent_step_card_${step.stepNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                AutonomousStepStatus.RUNNING -> ObsidianSurfaceGlass
                AutonomousStepStatus.FAILED -> CoralAlert.copy(alpha = 0.08f)
                AutonomousStepStatus.COMPLETED -> ObsidianSurface
                else -> ObsidianSurface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (step.status) {
                    AutonomousStepStatus.RUNNING -> CyberCyan
                    AutonomousStepStatus.COMPLETED -> EmeraldReady.copy(alpha = 0.6f)
                    AutonomousStepStatus.FAILED -> CoralAlert
                    AutonomousStepStatus.WAITING_CONFIRMATION -> AmberThinking
                    else -> BorderSubtle
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Number / Status Icon Indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when (step.status) {
                                AutonomousStepStatus.COMPLETED -> EmeraldReady.copy(alpha = 0.2f)
                                AutonomousStepStatus.RUNNING -> CyberCyan.copy(alpha = 0.2f)
                                AutonomousStepStatus.FAILED -> CoralAlert.copy(alpha = 0.2f)
                                AutonomousStepStatus.WAITING_CONFIRMATION -> AmberThinking.copy(alpha = 0.2f)
                                else -> ObsidianSurfaceGlass
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (step.status) {
                        AutonomousStepStatus.COMPLETED -> Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(16.dp))
                        AutonomousStepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberCyan, strokeWidth = 2.dp)
                        AutonomousStepStatus.FAILED -> Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = CoralAlert, modifier = Modifier.size(16.dp))
                        AutonomousStepStatus.WAITING_CONFIRMATION -> Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AmberThinking, modifier = Modifier.size(14.dp))
                        else -> Text(text = "${step.stepNumber}", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step ${step.stepNumber}: ${step.title}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = step.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Domain chip
                Box(
                    modifier = Modifier
                        .background(CyberCyanDim.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = domainLabel.substringBefore(":"),
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Tool chip
                Box(
                    modifier = Modifier
                        .background(ObsidianSurfaceElevated, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = step.toolId, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Arguments & Detailed Output View
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (step.toolArguments.isNotEmpty()) {
                        Text(text = "Arguments:", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        step.toolArguments.forEach { (k, v) ->
                            Text(
                                text = "  • $k = \"$v\"",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!step.actualResult.isNullOrBlank()) {
                        Text(text = "Output Result:", color = EmeraldReady, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = step.actualResult,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (!step.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Error Details:", color = CoralAlert, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = step.errorMessage, color = CoralAlert, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                            ) {
                                Text("Retry This Step", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onOpenSelfHealing,
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAlert.copy(alpha = 0.2f), contentColor = CoralAlert)
                            ) {
                                Icon(imageVector = Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Self-Healing AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveReportCard(
    reportText: String,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_executive_report_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldReady))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldReady, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Executive Task Report", color = EmeraldReady, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read Report", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = reportText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CollapsibleTerminalLogs(
    logs: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("agent_terminal_logs_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Live Execution Logs (${logs.size})", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(ObsidianBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (logs.isEmpty()) {
                        Text(text = "No logs generated yet.", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        logs.forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("FAILED") || log.contains("Error")) CoralAlert
                                else if (log.contains("COMPLETED") || log.contains("Success")) EmeraldReady
                                else if (log.contains("Starting") || log.contains("approved")) CyberCyan
                                else TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTaskItem(
    task: AgentTask,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${task.steps.size} steps • ${task.status.label}", color = TextSecondary, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Task", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SixInOneCapabilitiesCard(
    onSelectCapability: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_six_in_one_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.35f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Six-in-One Autonomous Capabilities",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(EmeraldReady.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "6/6 LINKED", color = EmeraldReady, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val capabilities = listOf(
                Triple("1. Coding", Icons.Default.Code, "Read, edit, refactor files with safety rollback checkpoints"),
                Triple("2. Build & Test", Icons.Default.Build, "Execute genuine builds, run test suites, parse stack traces"),
                Triple("3. Web & Browser", Icons.Default.Language, "Deep research, documentation retrieval, form automation"),
                Triple("4. Phone Actions", Icons.Default.Smartphone, "Screen reading, clicking, typing, sensors & telephony"),
                Triple("5. Memory Queue", Icons.Default.Storage, "SQLite persistence, RAG knowledge store, task state queue"),
                Triple("6. AI Brain", Icons.Default.Psychology, "Multi-model reasoning, plan decomposition, dynamic replanning")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                capabilities.forEach { (name, icon, desc) ->
                    Surface(
                        onClick = {
                            when {
                                name.contains("Coding") -> onSelectCapability("Read project files, analyze architecture, and propose refactoring with checkpoint.")
                                name.contains("Build") -> onSelectCapability("Run build verification and unit test suite.")
                                name.contains("Web") -> onSelectCapability("Research latest quantum computing breakthroughs with source citations.")
                                name.contains("Phone") -> onSelectCapability("Inspect active screen controls and read device telemetry.")
                                name.contains("Memory") -> onSelectCapability("Search knowledge base for system protocols.")
                                else -> onSelectCapability("Perform a complete system audit and summarize executive status.")
                            }
                        },
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)),
                        modifier = Modifier.width(135.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = desc, color = TextMuted, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangedFilesCard(
    files: List<String>,
    onOpenFileInWorkspace: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_changed_files_card"),
        colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.5f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Modified Project Files (${files.size})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onOpenFileInWorkspace) {
                    Text("Open Workspace", color = CyberCyan, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            files.forEach { file ->
                Text(
                    text = "  • $file",
                    color = EmeraldReady,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
