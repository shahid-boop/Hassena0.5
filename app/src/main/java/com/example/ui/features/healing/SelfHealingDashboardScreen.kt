package com.example.ui.features.healing

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.ErrorCategory
import com.example.models.ErrorSeverity
import com.example.models.RepairState
import com.example.models.ResolutionStatus
import com.example.models.SelfHealingRepairRecord
import com.example.models.SystemErrorRecord
import com.example.providers.HasseenaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfHealingDashboardScreen(
    viewModel: HasseenaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current

    val healthScore by viewModel.systemHealthScore.collectAsStateWithLifecycle()
    val errorRecords by viewModel.systemErrorRecords.collectAsStateWithLifecycle()
    val repairs by viewModel.selfHealingRepairs.collectAsStateWithLifecycle()
    val telemetryLogs by viewModel.selfHealingTelemetryLogs.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var isTelemetryExpanded by remember { mutableStateOf(false) }

    val filteredErrors = remember(errorRecords, selectedFilter) {
        when (selectedFilter) {
            "UNRESOLVED" -> errorRecords.filter { !it.resolutionStatus.isResolved }
            "RECOVERABLE" -> errorRecords.filter { it.isTransient || it.resolutionStatus == ResolutionStatus.WAITING_FOR_USER_APPROVAL }
            "PROPOSALS" -> errorRecords.filter { it.repairId != null }
            "RECOVERED" -> errorRecords.filter { it.resolutionStatus == ResolutionStatus.RECOVERED || it.resolutionStatus == ResolutionStatus.AUTO_RECOVERED }
            "FAILED" -> errorRecords.filter { it.resolutionStatus == ResolutionStatus.REPAIR_FAILED || it.resolutionStatus == ResolutionStatus.ROLLED_BACK }
            else -> errorRecords
        }
    }

    val unresolvedCount = remember(errorRecords) { errorRecords.count { !it.resolutionStatus.isResolved } }
    val recoveredCount = remember(errorRecords) { errorRecords.count { it.resolutionStatus.isResolved } }
    val pendingApprovalCount = remember(repairs) { repairs.count { it.state == RepairState.WAITING_FOR_APPROVAL } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "Self-Healing AI Watchdog",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Self-Healing AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Autonomous Watchdog & Patch Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("healing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearResolvedErrors()
                            Toast.makeText(context, "Resolved error history cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("healing_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Resolved Logs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Score Banner Card
            item {
                HealthStatusCard(
                    healthScore = healthScore,
                    unresolvedCount = unresolvedCount,
                    recoveredCount = recoveredCount,
                    pendingApprovalCount = pendingApprovalCount
                )
            }

            // Quick Diagnostic Simulator
            item {
                QuickSimulatorCard(
                    onSimulateTransient = {
                        viewModel.recordManualError(
                            category = ErrorCategory.NETWORK_TRANSIENT,
                            message = "SocketTimeoutException: SSL handshake timed out after 3000ms connecting to api.groq.com",
                            component = "AIService.GroqEngine",
                            operation = "generatePureTextResponse"
                        )
                        Toast.makeText(context, "Transient network timeout simulated", Toast.LENGTH_SHORT).show()
                    },
                    onSimulateSyntax = {
                        viewModel.recordManualError(
                            category = ErrorCategory.COMPILATION_ERROR,
                            message = "Unresolved reference: 'BuildConfig.API_KEY' in AutonomousCodingEngine.kt:42",
                            component = "CodingStudio.Compiler",
                            operation = "compileKotlinSource"
                        )
                        Toast.makeText(context, "Compiler syntax error simulated", Toast.LENGTH_SHORT).show()
                    },
                    onSimulateMissingKey = {
                        viewModel.recordManualError(
                            category = ErrorCategory.MISSING_API_KEY,
                            message = "ApiKeyStore: No valid OpenAI or Gemini API key configured for model request",
                            component = "ApiKeyStore",
                            operation = "fetchCredentials"
                        )
                        Toast.makeText(context, "Missing API key error simulated", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Real-Time Telemetry Watchdog
            item {
                TelemetryWatchdogCard(
                    telemetryLogs = telemetryLogs,
                    isExpanded = isTelemetryExpanded,
                    onToggle = { isTelemetryExpanded = !isTelemetryExpanded }
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "ALL" to "All (${errorRecords.size})",
                        "UNRESOLVED" to "Unresolved ($unresolvedCount)",
                        "RECOVERABLE" to "Recoverable",
                        "PROPOSALS" to "Proposals ($pendingApprovalCount)",
                        "RECOVERED" to "Recovered ($recoveredCount)",
                        "FAILED" to "Failed"
                    )

                    for ((key, label) in filterOptions) {
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
            }

            // Error Items List
            if (filteredErrors.isEmpty()) {
                item {
                    EmptyStateHealingCard(selectedFilter)
                }
            } else {
                items(filteredErrors, key = { it.id }) { errorRecord ->
                    val matchingRepair = repairs.find { it.id == errorRecord.repairId || it.errorId == errorRecord.id }
                    ErrorDetailItemCard(
                        errorRecord = errorRecord,
                        repair = matchingRepair,
                        onDiagnose = { viewModel.diagnoseError(errorRecord) },
                        onProposeFix = {
                            viewModel.proposeCodeRepairForError(
                                errorRecord = errorRecord,
                                targetFile = "app/src/main/java/com/example/services/ai/AIService.kt",
                                sourceCode = "val apiKey = \"\"\n// Initial setup\nfun run() {}",
                                log = errorRecord.message
                            )
                        },
                        onApproveRepair = { repairId ->
                            viewModel.approveAndApplyRepair(repairId)
                            Toast.makeText(context, "Applying patch with auto-rollback protection...", Toast.LENGTH_SHORT).show()
                        },
                        onRejectRepair = { repairId ->
                            viewModel.rejectRepair(repairId)
                            Toast.makeText(context, "Repair proposal rejected", Toast.LENGTH_SHORT).show()
                        },
                        onRollbackRepair = { repairId ->
                            viewModel.rollbackRepair(repairId)
                            Toast.makeText(context, "Reverting to pre-repair safety snapshot...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HealthStatusCard(
    healthScore: Int,
    unresolvedCount: Int,
    recoveredCount: Int,
    pendingApprovalCount: Int
) {
    val scoreColor = when {
        healthScore >= 90 -> Color(0xFF00E676)
        healthScore >= 70 -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }

    val statusLabel = when {
        healthScore >= 90 -> "Optimal Reliability"
        healthScore >= 70 -> "Degraded State"
        else -> "Critical Watch"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141923)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "System Health Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = scoreColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.12f))
                        .border(2.dp, scoreColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$healthScore%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = scoreColor
                        )
                        Text(
                            text = "HEALTH",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricPill(
                    label = "Unresolved",
                    value = unresolvedCount.toString(),
                    color = if (unresolvedCount > 0) Color(0xFFFF5252) else Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Proposals",
                    value = pendingApprovalCount.toString(),
                    color = if (pendingApprovalCount > 0) Color(0xFF00E5FF) else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Recovered",
                    value = recoveredCount.toString(),
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2534))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuickSimulatorCard(
    onSimulateTransient: () -> Unit,
    onSimulateSyntax: () -> Unit,
    onSimulateMissingKey: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161C26)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Self-Healing Diagnostic Simulation",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Simulate errors to test auto-retry backoff, AI diagnosis, minimal patch creation, and rollback safety:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateTransient,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_transient_btn")
                ) {
                    Text("Timeout Retry", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onSimulateSyntax,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_syntax_btn")
                ) {
                    Text("Code Syntax", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onSimulateMissingKey,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_missing_key_btn")
                ) {
                    Text("Missing Key", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun TelemetryWatchdogCard(
    telemetryLogs: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Self-Healing Telemetry",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                }

                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Telemetry",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        telemetryLogs.takeLast(25).forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = if (log.contains("fail", ignoreCase = true) || log.contains("error", ignoreCase = true)) {
                                    Color(0xFFFF5252)
                                } else if (log.contains("recovered", ignoreCase = true) || log.contains("verified", ignoreCase = true)) {
                                    Color(0xFF00E676)
                                } else {
                                    Color(0xFF00E5FF).copy(alpha = 0.85f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDetailItemCard(
    errorRecord: SystemErrorRecord,
    repair: SelfHealingRepairRecord?,
    onDiagnose: () -> Unit,
    onProposeFix: () -> Unit,
    onApproveRepair: (String) -> Unit,
    onRejectRepair: (String) -> Unit,
    onRollbackRepair: (String) -> Unit
) {
    var isStackTraceVisible by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val severityColor = when (errorRecord.severity) {
        ErrorSeverity.LOW -> Color(0xFF00E5FF)
        ErrorSeverity.MEDIUM -> Color(0xFFFFB300)
        ErrorSeverity.HIGH -> Color(0xFFFF7043)
        ErrorSeverity.CRITICAL -> Color(0xFFFF5252)
    }

    val categoryIcon = when (errorRecord.category) {
        ErrorCategory.NETWORK_TRANSIENT -> Icons.Default.NetworkCheck
        ErrorCategory.MISSING_API_KEY -> Icons.Default.Security
        ErrorCategory.COMPILATION_ERROR -> Icons.Default.Code
        ErrorCategory.TEST_FAILURE -> Icons.Default.BugReport
        ErrorCategory.DATABASE_ERROR -> Icons.Default.Storage
        else -> Icons.Default.Warning
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161C26)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (!errorRecord.resolutionStatus.isResolved) severityColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category, Severity, and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorRecord.category.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Severity badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = errorRecord.severity.label.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = severityColor
                        )
                    }

                    // Resolution status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (errorRecord.resolutionStatus.isResolved) Color(0xFF00E676).copy(alpha = 0.15f)
                                else Color(0xFF00E5FF).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = errorRecord.resolutionStatus.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (errorRecord.resolutionStatus.isResolved) Color(0xFF00E676) else Color(0xFF00E5FF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Component and Operation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${errorRecord.affectedComponent} • ${errorRecord.operation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(errorRecord.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sanitized Error Message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Text(
                    text = errorRecord.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White
                )
            }

            // Stack Trace toggle if present
            if (!errorRecord.stackTrace.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clickable { isStackTraceVisible = !isStackTraceVisible }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isStackTraceVisible) "Hide Stack Trace" else "View Sanitized Stack Trace",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF)
                    )
                    Icon(
                        imageVector = if (isStackTraceVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isStackTraceVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = errorRecord.stackTrace.take(800) + if (errorRecord.stackTrace.length > 800) "\n[...truncated...]" else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Recovery Advice
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Suggested Action: ${errorRecord.suggestedRecovery}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Root Cause Analysis Card if present in repair
            repair?.rootCauseAnalysis?.let { rca ->
                Spacer(modifier = Modifier.height(12.dp))
                RootCauseSubCard(rca)
            }

            // Code Diff Patch and Approval Box if present
            repair?.proposedPatch?.let { patch ->
                Spacer(modifier = Modifier.height(12.dp))
                CodeDiffSubCard(
                    repair = repair,
                    patch = patch,
                    onApprove = { onApproveRepair(repair.id) },
                    onReject = { onRejectRepair(repair.id) },
                    onRollback = { onRollbackRepair(repair.id) }
                )
            }

            // Action row if no patch proposed yet
            if (repair?.proposedPatch == null && !errorRecord.resolutionStatus.isResolved) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDiagnose,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("diagnose_btn_${errorRecord.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnose Cause", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }

                    Button(
                        onClick = onProposeFix,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676).copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("propose_fix_btn_${errorRecord.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synthesize Fix", color = Color(0xFF00E676), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RootCauseSubCard(rca: com.example.models.RootCauseAnalysis) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2534))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Root-Cause Diagnosis",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E5FF)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (rca.isConfirmed) Color(0xFF00E676).copy(alpha = 0.15f)
                            else Color(0xFFFFB300).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (rca.isConfirmed) "CONFIRMED FACTUAL" else "WORKING HYPOTHESIS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (rca.isConfirmed) Color(0xFF00E676) else Color(0xFFFFB300)
                    )
                }
            }

            Text(
                text = rca.likelyRootCause,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )

            Text(
                text = "Proposed Fix: ${rca.proposedFixSummary}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.75f)
            )

            Text(
                text = "Impact & Verification: ${rca.estimatedImpact} • ${rca.recommendedVerification}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CodeDiffSubCard(
    repair: SelfHealingRepairRecord,
    patch: com.example.models.CodeDiffPatch,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRollback: () -> Unit
) {
    val stateColor = when (repair.state) {
        RepairState.RECOVERED -> Color(0xFF00E676)
        RepairState.FAILED, RepairState.ROLLED_BACK -> Color(0xFFFF5252)
        RepairState.WAITING_FOR_APPROVAL -> Color(0xFF00E5FF)
        RepairState.REPAIRING, RepairState.VERIFYING -> Color(0xFFFFB300)
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10141D))
            .border(1.dp, stateColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Target File & Diff Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = patch.targetFilePath.takeLast(28),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "+${patch.linesAdded}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E676)
                    )
                    Text(
                        text = "-${patch.linesRemoved}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF5252)
                    )
                }
            }

            // Diff Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = patch.diffPreview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFFE0E0E0)
                )
            }

            // Verification or Error Message
            repair.verificationOutput?.let { vLog ->
                Text(
                    text = vLog,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (repair.state == RepairState.RECOVERED) Color(0xFF00E676) else Color(0xFFFF5252)
                )
            }

            // Approval Controls
            when (repair.state) {
                RepairState.WAITING_FOR_APPROVAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApprove,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("approve_patch_${repair.id}")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Approve & Apply", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reject_patch_${repair.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reject", fontSize = 11.sp)
                        }
                    }
                }
                RepairState.REPAIRING, RepairState.VERIFYING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (repair.state == RepairState.REPAIRING) "Applying patch to source..." else "Running compiler & test verification...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
                RepairState.RECOVERED, RepairState.ROLLED_BACK, RepairState.FAILED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: ${repair.state.label}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = stateColor
                        )

                        if (repair.preRepairSnapshot != null && repair.state != RepairState.ROLLED_BACK) {
                            OutlinedButton(
                                onClick = onRollback,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("rollback_patch_${repair.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rollback", fontSize = 11.sp)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun EmptyStateHealingCard(selectedFilter: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedFilter == "ALL") "All Systems Healthy" else "No Records for '$selectedFilter'",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Autonomous watchdog is actively monitoring network, AI providers, files, database, and autonomous agents.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
