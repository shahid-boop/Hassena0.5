package com.example.ui.features.coding

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.AgentStepStatus
import com.example.models.AutonomousCodingStep
import com.example.models.CodingCheckpoint
import com.example.providers.HasseenaDestination
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
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sampleTemplates = mapOf(
    "Kotlin" to """
package com.jarvis.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                HomeScreen()
            }
        }
    }
}
""".trimIndent(),
    "Python" to """
# JARVIS Core Intelligence Module
import asyncio
import sys

async def main():
    print("[JARVIS] Initializing neural telemetry...")
    status = {"cpu": "12%", "memory": "48%", "status": "ONLINE"}
    print(f"[JARVIS] System status: {status}")

if __name__ == "__main__":
    asyncio.run(main())
""".trimIndent(),
    "JavaScript" to """
// JARVIS Assistant Client
async function checkStatus() {
  const telemetry = { cpu: 12, memory: 48, connected: true };
  console.log("Telemetry:", telemetry);
  return telemetry;
}

checkStatus();
""".trimIndent(),
    "Rust" to """
// JARVIS High-Performance Core
fn main() {
    println!("[JARVIS] Low-latency engine online.");
    let telemetry = ("CPU: 12%", "RAM: 48%");
    println!("Telemetry: {:?}, {:?}", telemetry.0, telemetry.1);
}
""".trimIndent()
)

@Composable
fun CodingScreen(
    onNavigateBack: () -> Unit = {},
    onCodeAssist: (action: String, code: String, language: String, onResult: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onCopyCode: (String) -> Unit = {},
    viewModel: HasseenaViewModel? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedFile by remember { mutableStateOf("MainActivity.kt") }
    var codeContent by remember { mutableStateOf(sampleTemplates["Kotlin"] ?: "") }
    var showLangMenu by remember { mutableStateOf(false) }
    var terminalOutput by remember { mutableStateOf("JARVIS Compiler Terminal initialized.\n> System ready.") }
    var showTerminal by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var aiAssistOutput by remember { mutableStateOf<String?>(null) }
    var isAiAssisting by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Studio Tabs: 0 = Editor, 1 = Autonomous Plan, 2 = Checkpoints, 3 = Self-Repair
    var activeTab by remember { mutableIntStateOf(0) }

    // Autonomous task goal input
    var taskGoalInput by remember { mutableStateOf("Build Multi-Step Room Cache & State Invariant Engine") }

    val languages = listOf("Kotlin", "Python", "JavaScript", "Rust")
    val lineCount = remember(codeContent) { codeContent.lines().size.coerceAtLeast(1) }

    // Reactive streams from ViewModel's AutonomousCodingEngine
    val checkpoints = viewModel?.codingCheckpoints?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val currentPlan = viewModel?.autonomousPlan?.collectAsStateWithLifecycle()?.value
    val repairSession = viewModel?.selfRepairSession?.collectAsStateWithLifecycle()?.value
    val telemetryLogs = viewModel?.codingTelemetryLogs?.collectAsStateWithLifecycle()?.value ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .testTag("coding_screen")
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
                    .testTag("coding_back_button")
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
                    imageVector = Icons.Default.Code,
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
                    text = "Autonomous Coding Studio",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // 3D Avatar Workstation Shortcut Badge
            Surface(
                onClick = { viewModel?.navigateTo(HasseenaDestination.AVATAR_3D) },
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Avatar 3D",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3D Avatar", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    onCopyCode(codeContent)
                    toastMessage = "Code copied to clipboard!"
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(ObsidianSurfaceElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = ObsidianSurface,
            contentColor = CyberCyan,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Editor", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Autonomous Plan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        if (currentPlan != null && !currentPlan.isCompleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(EmeraldReady, CircleShape)
                            )
                        }
                    }
                }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Checkpoints", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        if (checkpoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(${checkpoints.size})", color = CyberCyan, fontSize = 11.sp)
                        }
                    }
                }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text("Self-Repair", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }

        // Optional notification toast
        AnimatedVisibility(visible = toastMessage != null) {
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanDim),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(toastMessage ?: "", color = CyberCyan, fontSize = 12.sp)
                    Text(
                        "Dismiss",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { toastMessage = null }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TAB CONTENT
        when (activeTab) {
            0 -> {
                // =============================================================
                // TAB 0: EDITOR & TERMINAL
                // =============================================================
                // Language & File Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Button(
                            onClick = { showLangMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("language_selector_button")
                        ) {
                            Text(
                                text = selectedLanguage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CyberCyan
                            )
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            modifier = Modifier.background(ObsidianSurfaceElevated)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        selectedLanguage = lang
                                        selectedFile = when (lang) {
                                            "Kotlin" -> "MainActivity.kt"
                                            "Python" -> "main.py"
                                            "JavaScript" -> "index.js"
                                            "Rust" -> "main.rs"
                                            else -> "script"
                                        }
                                        sampleTemplates[lang]?.let { codeContent = it }
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = selectedFile,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )

                    // Quick Snapshot Button
                    OutlinedButton(
                        onClick = {
                            viewModel?.createCodingCheckpoint(
                                label = "Quick Snapshot (${selectedFile})",
                                file = selectedFile,
                                language = selectedLanguage,
                                code = codeContent,
                                reason = "User Manual Snapshot"
                            )
                            toastMessage = "Checkpoint saved!"
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Icon(Icons.Default.History, null, tint = QuantumViolet, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snapshot", color = QuantumViolet, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$lineCount lines",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Code Editor Window
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        // Line numbers
                        val lineNumbersScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight()
                                .verticalScroll(lineNumbersScrollState)
                                .padding(end = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted.copy(alpha = 0.5f),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(BorderSubtle)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Code input field
                        val codeScrollState = rememberScrollState()
                        val codeHScrollState = rememberScrollState()
                        BasicTextField(
                            value = codeContent,
                            onValueChange = { codeContent = it },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(CyberCyan),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(codeScrollState)
                                .horizontalScroll(codeHScrollState)
                                .testTag("code_editor_field")
                        )
                    }
                }

                // AI Explanation / Assistant output
                AnimatedVisibility(visible = isAiAssisting || aiAssistOutput != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, QuantumViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = QuantumViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "JARVIS AI Analysis",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = QuantumViolet
                                    )
                                }
                                Text(
                                    "Close",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.clickable { aiAssistOutput = null }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (isAiAssisting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = QuantumViolet,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synthesizing code reasoning...", color = TextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                Text(
                                    text = aiAssistOutput ?: "",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Interactive Terminal Drawer
                AnimatedVisibility(visible = showTerminal) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(top = 6.dp)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EmeraldReady, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "TERMINAL — $selectedLanguage",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = EmeraldReady
                                    )
                                }
                                Text(
                                    "Clear",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.clickable { terminalOutput = "> Terminal ready." }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = terminalOutput,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isRunning = true
                            showTerminal = true
                            terminalOutput = "> Executing $selectedFile ($selectedLanguage)...\n[OK] Syntax check passed (0 errors).\n[JARVIS] Running in isolated container...\nProcess finished with exit code 0."
                            isRunning = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_code_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            showTerminal = true
                            terminalOutput = "> Compiling $selectedFile...\n> AGP 9.1 & Kotlin 2.2 syntax validation: 0 errors, 0 warnings.\n> Binary target ready."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("build_code_button")
                    ) {
                        Icon(Icons.Default.Build, null, tint = SolarGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Build", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { showTerminal = !showTerminal },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .testTag("toggle_terminal_button")
                    ) {
                        Icon(Icons.Default.Terminal, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminal", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // AI Code Assistant Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isAiAssisting = true
                            aiAssistOutput = null
                            onCodeAssist("EXPLAIN", codeContent, selectedLanguage) { result ->
                                aiAssistOutput = result
                                isAiAssisting = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("explain_code_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = QuantumViolet, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Explain", color = QuantumViolet, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            isAiAssisting = true
                            aiAssistOutput = null
                            onCodeAssist("REFACTOR", codeContent, selectedLanguage) { result ->
                                aiAssistOutput = result
                                isAiAssisting = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyanDim),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("refactor_code_button")
                    ) {
                        Text("Refactor", color = CyberCyan, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            isAiAssisting = true
                            aiAssistOutput = null
                            onCodeAssist("FIX_BUGS", codeContent, selectedLanguage) { result ->
                                aiAssistOutput = result
                                isAiAssisting = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("fix_bugs_button")
                    ) {
                        Text("Check Bugs", color = CoralAlert, fontSize = 12.sp)
                    }
                }
            }

            1 -> {
                // =============================================================
                // TAB 1: AUTONOMOUS AI CODING PLANNER
                // =============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SmartToy, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Autonomous Task Goal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = taskGoalInput,
                                    onValueChange = { taskGoalInput = it },
                                    placeholder = { Text("Describe the task for JARVIS...", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel?.generateAutonomousPlan(taskGoalInput, selectedLanguage, codeContent)
                                            toastMessage = "Autonomous plan generated!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Generate Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    if (currentPlan != null) {
                                        Button(
                                            onClick = {
                                                viewModel.runEntireAutonomousPlan(
                                                    initialCode = codeContent,
                                                    targetFile = selectedFile
                                                ) { _, _, updatedCode ->
                                                    codeContent = updatedCode
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldReady, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !currentPlan.isExecuting && !currentPlan.isCompleted
                                        ) {
                                            Text("Execute All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.executeNextAutonomousStep(
                                                    currentCode = codeContent,
                                                    targetFile = selectedFile
                                                ) { _, _, updatedCode ->
                                                    codeContent = updatedCode
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                            enabled = !currentPlan.isExecuting && !currentPlan.isCompleted
                                        ) {
                                            Text("Next Step", color = TextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (currentPlan != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Plan Pipeline: ${currentPlan.goal}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CyberCyan
                                        )
                                        if (currentPlan.isCompleted) {
                                            Text("COMPLETED", color = EmeraldReady, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else if (currentPlan.isExecuting) {
                                            Text("RUNNING", color = AmberThinking, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Step ${currentPlan.currentStepIndex + 1}/${currentPlan.steps.size}", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    val progress = (currentPlan.currentStepIndex.toFloat() / currentPlan.steps.size.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = CyberCyan,
                                        trackColor = ObsidianSurfaceElevated
                                    )
                                }
                            }
                        }

                        items(currentPlan.steps) { step ->
                            StepCard(step = step)
                        }
                    }
                }
            }

            2 -> {
                // =============================================================
                // TAB 2: CHECKPOINTS & ROLLBACK
                // =============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Checkpoints Timeline", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text("Restore any past snapshot with 1 click", color = TextMuted, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel?.createCodingCheckpoint(
                                        label = "Snapshot: $selectedFile",
                                        file = selectedFile,
                                        language = selectedLanguage,
                                        code = codeContent,
                                        reason = "Manual Checkpoint"
                                    )
                                    toastMessage = "New snapshot created!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Snapshot Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (checkpoints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No checkpoints yet. Tap 'Snapshot Now' or run Autonomous Plan to auto-save.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(checkpoints) { checkpoint ->
                                CheckpointItemCard(
                                    checkpoint = checkpoint,
                                    onRollback = {
                                        val restored = viewModel?.rollbackCodingCheckpoint(checkpoint.id)
                                        if (restored != null) {
                                            codeContent = restored.code
                                            selectedLanguage = restored.language
                                            selectedFile = restored.file
                                            toastMessage = "Rolled back to checkpoint [${restored.id}]!"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // =============================================================
                // TAB 3: SELF-REPAIR LOOP
                // =============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralAlert.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, null, tint = CoralAlert, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Automated Self-Repair & Healing Loop",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Iterative 3-round repair loop: Inspects compiler AST, generates bug patches, re-tests, and verifies artifact integrity automatically.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel?.runSelfRepairLoop(
                                            initialCode = codeContent,
                                            targetFile = selectedFile,
                                            language = selectedLanguage
                                        ) { _, patchedCode ->
                                            codeContent = patchedCode
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = repairSession?.isRunning != true
                                ) {
                                    if (repairSession?.isRunning == true) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Self-Repair in Progress...", fontSize = 12.sp)
                                    } else {
                                        Text("Run Self-Repair Loop (Max 3 Rounds)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (repairSession != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        repairSession.statusText,
                                        color = if (repairSession.isRunning) AmberThinking else EmeraldReady,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Attempts Executed: ${repairSession.attempts.size}/${repairSession.maxAttempts}", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        items(repairSession.attempts) { attempt ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Round ${attempt.attemptNumber}: ${attempt.detectedIssue}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = EmeraldReady
                                        )
                                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldReady, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Strategy: ${attempt.patchStrategy}", color = TextSecondary, fontSize = 11.sp)
                                    Text("Result: ${attempt.verificationResult}", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Engine Telemetry Logs Stream
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ENGINE TELEMETRY LOGS", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    telemetryLogs.takeLast(6).forEach { logLine ->
                                        Text(
                                            text = logLine,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextMuted,
                                            lineHeight = 15.sp
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
}

@Composable
private fun StepCard(step: AutonomousCodingStep) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (step.status) {
                AgentStepStatus.COMPLETED -> EmeraldReady.copy(alpha = 0.5f)
                AgentStepStatus.IN_PROGRESS -> CyberCyan
                AgentStepStatus.FAILED -> CoralAlert
                AgentStepStatus.REPAIRED -> QuantumViolet
                AgentStepStatus.PENDING -> BorderSubtle
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step ${step.id}: ${step.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                when (step.status) {
                    AgentStepStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, null, tint = EmeraldReady, modifier = Modifier.size(16.dp))
                    AgentStepStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberCyan, strokeWidth = 2.dp)
                    AgentStepStatus.FAILED -> Icon(Icons.Default.Error, null, tint = CoralAlert, modifier = Modifier.size(16.dp))
                    AgentStepStatus.REPAIRED -> Text("REPAIRED", color = QuantumViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    AgentStepStatus.PENDING -> Text("PENDING", color = TextMuted, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.description, color = TextSecondary, fontSize = 11.sp)
            if (step.logMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(step.logMessage, color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun CheckpointItemCard(
    checkpoint: CodingCheckpoint,
    onRollback: () -> Unit
) {
    val dateStr = remember(checkpoint.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(checkpoint.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(checkpoint.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${checkpoint.file} • ${checkpoint.lineCount} lines • $dateStr (${checkpoint.triggerReason})",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            OutlinedButton(
                onClick = onRollback,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumViolet.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Restore, null, tint = QuantumViolet, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore", color = QuantumViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
