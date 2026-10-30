package com.example.ui.features.workspace

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.os.IBinder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale


import com.example.providers.HasseenaViewModel
import com.example.models.AIProviderType
import com.example.models.AssistantPersonality
import com.example.models.RoutingMode
import org.json.JSONObject
private data class WorkspaceFile(val name: String, val mimeType: String, val uri: Uri? = null, val textContent: String? = null)

/** SAF-based workspace: imports arbitrary user-selected files without broad storage permission and exports ZIPs through SAF. */
@Composable
fun FileWorkspaceScreen(viewModel: HasseenaViewModel) {
    val context = LocalContext.current
    var savedFolderUri by remember { mutableStateOf(WorkspaceSaveLocation.getSavedFolder(context)) }
    val scope = rememberCoroutineScope()
    val files = remember { mutableStateListOf<WorkspaceFile>() }
    var codingPrompt by remember { mutableStateOf("") }
    var isCoding by remember { mutableStateOf(false) }
    var codingStatus by remember { mutableStateOf("AI Coding Agent: apna project prompt likhein.") }

    var projectCheckStatus by remember { mutableStateOf("Project checks abhi nahi chalaye gaye.") }

    var isReviewing by remember { mutableStateOf(false) }
    var autoModeEnabled by remember { mutableStateOf(true) }
    var selfFixStatus by remember { mutableStateOf("AI Review abhi nahi chalaya gaya.") }


    var generatedPreview by remember { mutableStateOf("") }

    var codeName by remember { mutableStateOf("Main.kt") }
    var codeText by remember { mutableStateOf("") }
    var showCodeEditor by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var showPreview by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<WorkspaceFile?>(null) }
    var previewContent by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Files stay on this device until you explicitly export them.") }
    var pendingZip by remember { mutableStateOf(false) }
    var pendingSingleIndex by remember { mutableStateOf(-1) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching {
                val name = queryDisplayName(context, uri)
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                if (files.none { it.uri == uri }) files.add(WorkspaceFile(name, mime, uri = uri))
            }.onFailure { status = "File import nahi ho saki: ${it.message ?: "unknown error"}" }
        }
        if (uris.isNotEmpty()) status = "${uris.size} file(s) workspace mein add hui." 
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
                WorkspaceSaveLocation.rememberFolder(context, uri)
                savedFolderUri = uri
                status = "Save folder yaad rakh liya gaya."
            }.onFailure {
                status = "Folder permission save nahi hui: ${it.message ?: "unknown error"}"
            }
        }
    }

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { destination ->
        if (destination != null) {
            val snapshot = files.toList()
            scope.launch {
                status = "ZIP export ho raha hai…"
                val result = withContext(Dispatchers.IO) { runCatching { writeZip(context, destination, snapshot) } }
                status = result.fold({ "ZIP successfully export ho gayi (${snapshot.size} entries)." }, { "ZIP export fail: ${it.message ?: "unknown error"}" })
            }
        }
    }
    val zipImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        if (source != null) {
            scope.launch {
                status = "ZIP safely extract ho rahi hai…"
                val result = withContext(Dispatchers.IO) {
                    runCatching { extractWorkspaceZip(context, source) }
                }
                result.fold(
                    { extracted ->
                        val existing = files.map { it.name }.toMutableSet()
                        val unique = extracted.map { item ->
                            var name = item.name
                            var n = 1
                            while (!existing.add(name)) {
                                val dot = name.lastIndexOf('.')
                                name = if (dot > 0) name.substring(0, dot) + "-$n" + name.substring(dot)
                                       else "$name-$n"
                                n++
                            }
                            item.copy(name = name)
                        }
                        files.addAll(unique)
                        status = "ZIP se ${unique.size} files extract ho kar workspace mein add hui."
                    },
                    { error -> status = "ZIP import fail: ${error.message ?: "unknown error"}" }
                )
            }
        }
    }

    val singleExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { destination ->
        val index = pendingSingleIndex
        if (destination != null && index in files.indices) {
            val item = files[index]
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { copyWorkspaceItem(context, item, destination) } }
                status = result.fold({ "${item.name} export ho gayi." }, { "Export fail: ${it.message ?: "unknown error"}" })
            }
        }
        pendingSingleIndex = -1
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text(previewFile?.name ?: "File preview") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(previewContent, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(previewContent))
                    status = "Content clipboard mein copy ho gaya."
                }) { Text("Copy content") }
            },
            dismissButton = {
                TextButton(onClick = { showPreview = false }) { Text("Close") }
            }
        )
    }

    if (showCodeEditor) {
        AlertDialog(
            onDismissRequest = { showCodeEditor = false },
            title = { Text("Code / text file add karein") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = codeName, onValueChange = { codeName = it }, label = { Text("Filename, e.g. Main.kt") }, singleLine = true)
                    OutlinedTextField(value = codeText, onValueChange = { codeText = it }, label = { Text("Code ya text") }, minLines = 5, maxLines = 10)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val safeName = sanitizeEntryName(codeName).ifBlank { "generated.txt" }
                    if (codeText.isNotBlank()) {
                        files.add(WorkspaceFile(safeName, "text/plain", textContent = codeText))
                        status = "$safeName ZIP workspace mein add ho gayi."
                        codeText = ""
                        showCodeEditor = false
                    }
                }) { Text("Add file") }
            },
            dismissButton = { TextButton(onClick = { showCodeEditor = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070B17), Color(0xFF10172B))))
            .padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("JARVIS File Workspace", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE8F7FF), fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC111D32)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color(0x8837D9FF), RoundedCornerShape(20.dp))
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "AI CODING AGENT",
                    color = Color(0xFF67E8F9),
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = codingPrompt,
                    onValueChange = { codingPrompt = it },
                    label = { Text("Kis tarah ka project banana hai?") },
                    placeholder = {
                        Text("Misal: Kotlin app, modern UI, SQLite, settings...")
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { autoModeEnabled = !autoModeEnabled },
                    enabled = !isCoding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (autoModeEnabled) "Auto-Mode: ON (tap to turn OFF)" else "Auto-Mode: OFF (tap to turn ON)")
                }
                Button(
                    onClick = {
                        if (!isCoding && codingPrompt.isNotBlank()) {
                            scope.launch {
                                isCoding = true
                                codingStatus = "AI model select ho raha hai..."
                                generatedPreview = ""
                                try {
                                    val systemPrompt = """
You are JARVIS AI Coding Agent.
Create a complete multi-file project matching the user's request.
Return ONLY one valid JSON object, with no Markdown fences or explanations.
Required schema:
{"projectName":"ProjectName","files":[{"path":"app/src/main/Example.kt","content":"complete file contents","mimeType":"text/plain"}]}
Requirements:
- Include all important source files, folders, configuration files, dependencies and README needed for the requested project.
- Every file must contain its complete contents, not a placeholder or a short example.
- Use safe relative file paths. Never use absolute paths or parent-directory traversal.
- Escape JSON strings correctly, including newlines and quotation marks.
- Do not claim that the project was compiled or tested.
- If the complete project cannot fit in one response, return only files you can complete correctly and include a README explaining limitations.
- Limit output to 50 files and keep total source content under 1.5 million characters.
""".trimIndent()

                                    val decision = viewModel.aiModelRouter.routeRequest(
                                        prompt = codingPrompt.trim(),
                                        hasImageAttachment = false,
                                        forceResearch = false,
                                        routingMode = RoutingMode.AUTO_SMART,
                                        preferredProvider = AIProviderType.GEMINI,
                                        preferredModelId = "gemini-3.8-flash",
                                        isProviderConfigured = { provider ->
                                            viewModel.aiService.isProviderKeyAvailable(provider)
                                        }
                                    )

                                    codingStatus = "AI model: ${decision.displayName}. Code generate ho raha hai..."

                                    val outcome = viewModel.aiService.streamResponse(
                                        provider = decision.provider,
                                        modelId = decision.modelId,
                                        prompt = codingPrompt.trim(),
                                        history = emptyList(),
                                        personality = AssistantPersonality.TECHNICAL_EXPERT,
                                        memoryContextPrompt = systemPrompt,
                                        imageBitmap = null,
                                        onChunk = { _, accumulated, _ ->
                                            generatedPreview = accumulated
                                            codingStatus = "AI response aa raha hai... ${accumulated.length} characters"
                                        }
                                    )

                                    if (outcome.isError) {
                                        error(outcome.fullText.ifBlank {
                                            "AI request fail hui. Settings mein API key aur model configuration check karein."
                                        })
                                    }

                                    var rawJson = outcome.fullText.trim()
                                    if (rawJson.startsWith("```")) {
                                        rawJson = rawJson.substringAfter('\n', rawJson)
                                            .removeSuffix("```").trim()
                                    }

                                    val rootObject = JSONObject(rawJson)
                                    val generated = rootObject.optJSONArray("files")
                                        ?: error("AI response mein files array nahi mila.")

                                    if (generated.length() == 0 || generated.length() > 50) {
                                        error("AI ne valid file count nahi diya (1 se 50 files allowed).")
                                    }

                                    val additions = mutableListOf<WorkspaceFile>()
                                    val seen = mutableSetOf<String>()
                                    var totalChars = 0

                                    for (i in 0 until generated.length()) {
                                        val entry = generated.optJSONObject(i)
                                            ?: error("File #${i + 1} ka format ghalat hai.")
                                        val originalPath = entry.optString("path", "").trim()
                                        val path = originalPath.replace('\\', '/')
                                        val segments = path.split('/')

                                        if (path.isBlank() ||
                                            path.startsWith('/') ||
                                            segments.any { it.isBlank() || it == "." || it == ".." } ||
                                            path.contains(':') ||
                                            path.contains('\u0000')
                                        ) {
                                            error("Unsafe ya invalid file path mila: $originalPath")
                                        }

                                        val safePath = sanitizeEntryName(path)
                                        if (safePath.isBlank() || safePath != path) {
                                            error("File path sanitize check pass nahi hua: $originalPath")
                                        }

                                        val content = entry.optString("content", "")
                                        if (content.isEmpty()) {
                                            error("File content khaali hai: $safePath")
                                        }

                                        totalChars += content.length
                                        if (totalChars > 1_500_000) {
                                            error("Generated project size limit se bara hai.")
                                        }
                                        if (!seen.add(safePath)) {
                                            error("AI response mein duplicate path hai: $safePath")
                                        }
                                        if (files.any { it.name == safePath }) {
                                            error("Workspace mein pehle se '$safePath' maujood hai. Existing file ko overwrite nahi kiya.")
                                        }

                                        val mime = entry.optString("mimeType", "text/plain")
                                        additions.add(
                                            WorkspaceFile(
                                                name = safePath,
                                                mimeType = mime,
                                                textContent = content
                                            )
                                        )
                                    }

                                    files.addAll(additions)
                                    if (autoModeEnabled) {
                                        projectCheckStatus = "Auto-Mode: generated files ke static checks..."
                                        val generatedNames = additions.map { it.name }
                                        val findings = mutableListOf<String>()

                                        if (generatedNames.none {
                                                it.substringAfterLast('/').equals("README.md", true) ||
                                                it.substringAfterLast('/').equals("README.txt", true)
                                            }) {
                                            findings.add("README missing")
                                        }
                                        if (generatedNames.none {
                                                it.endsWith("build.gradle", true) ||
                                                it.endsWith("build.gradle.kts", true) ||
                                                it.endsWith("settings.gradle", true) ||
                                                it.endsWith("settings.gradle.kts", true)
                                            }) {
                                            findings.add("Gradle configuration missing")
                                        }

                                        projectCheckStatus =
                                            if (findings.isEmpty()) {
                                                "Static checks: ${generatedNames.size} generated files; README/Gradle config names found. Compilation abhi verify nahi hui."
                                            } else {
                                                "Static checks: ${generatedNames.size} generated files. Warnings: ${findings.joinToString("; ")}. Compilation abhi verify nahi hui."
                                            }

                                        try {
                                            val reviewFiles = additions.filter {
                                                it.textContent != null &&
                                                (it.name.endsWith(".kt", true) ||
                                                 it.name.endsWith(".java", true) ||
                                                 it.name.endsWith(".xml", true) ||
                                                 it.name.endsWith(".gradle", true) ||
                                                 it.name.endsWith(".kts", true) ||
                                                 it.name.endsWith(".json", true) ||
                                                 it.name.endsWith(".md", true) ||
                                                 it.name.endsWith(".txt", true) ||
                                                 it.name.endsWith(".properties", true))
                                            }.take(12)

                                            val reviewChars = reviewFiles.sumOf { it.textContent!!.length }
                                            if (reviewChars > 100_000) {
                                                error("Auto-Review skip: review input 100,000 characters se bara hai.")
                                            }

                                            if (reviewFiles.isNotEmpty()) {
                                                codingStatus = "Auto-Mode: AI code review aur safe fixes chal rahe hain..."
                                                val reviewSource = buildString {
                                                    appendLine("Review these newly generated project files for concrete correctness bugs.")
                                                    reviewFiles.forEach {
                                                        appendLine("\n--- FILE: ${it.name} ---")
                                                        appendLine(it.textContent)
                                                    }
                                                }

                                                val reviewSystemPrompt = """
You are a cautious Android/project code repair agent.
Return ONLY valid JSON:
{"summary":"short explanation","fixes":[{"path":"exact existing relative path","content":"complete corrected file"}]}
Only fix clear bugs in supplied files. Never invent paths, delete files, change unrelated features, or claim a build/test ran.
Each fix must contain the complete file. Maximum 12 fixes and 150000 output characters.
If no safe correction is needed, return {"summary":"No safe changes recommended","fixes":[]}.
""".trimIndent()

                                                val reviewDecision = viewModel.aiModelRouter.routeRequest(
                                                    prompt = reviewSource,
                                                    hasImageAttachment = false,
                                                    forceResearch = false,
                                                    routingMode = RoutingMode.AUTO_SMART,
                                                    preferredProvider = AIProviderType.GEMINI,
                                                    preferredModelId = "gemini-3.8-flash",
                                                    isProviderConfigured = { provider ->
                                                        viewModel.aiService.isProviderKeyAvailable(provider)
                                                    }
                                                )

                                                val reviewOutcome = viewModel.aiService.streamResponse(
                                                    provider = reviewDecision.provider,
                                                    modelId = reviewDecision.modelId,
                                                    prompt = reviewSource,
                                                    history = emptyList(),
                                                    personality = AssistantPersonality.TECHNICAL_EXPERT,
                                                    memoryContextPrompt = reviewSystemPrompt,
                                                    imageBitmap = null,
                                                    onChunk = { _, _, _ -> }
                                                )

                                                if (reviewOutcome.isError) {
                                                    error(reviewOutcome.fullText.ifBlank { "AI review request fail hui." })
                                                }

                                                var reviewJson = reviewOutcome.fullText.trim()
                                                if (reviewJson.startsWith("```")) {
                                                    reviewJson = reviewJson.substringAfter('\n', reviewJson)
                                                        .removeSuffix("```").trim()
                                                }

                                                val reviewRoot = JSONObject(reviewJson)
                                                val fixes = reviewRoot.optJSONArray("fixes")
                                                    ?: error("AI review response mein fixes array nahi mila.")
                                                if (fixes.length() > 12) error("AI fix limit se zyada files hain.")

                                                val allowedPaths = reviewFiles.map { it.name }.toSet()
                                                val seenFixes = mutableSetOf<String>()
                                                var fixedCount = 0
                                                var fixChars = 0

                                                for (i in 0 until fixes.length()) {
                                                    val fix = fixes.optJSONObject(i)
                                                        ?: error("AI fix #${i + 1} ka format ghalat hai.")
                                                    val path = fix.optString("path", "")
                                                    val content = fix.optString("content", "")

                                                    if (path !in allowedPaths || !seenFixes.add(path)) {
                                                        error("AI ne unknown ya duplicate file fix di: $path")
                                                    }
                                                    if (content.isBlank()) error("AI fix khaali hai: $path")
                                                    fixChars += content.length
                                                    if (fixChars > 150_000) error("AI fixes size limit se bari hain.")

                                                    val index = files.indexOfFirst {
                                                        it.name == path && it.textContent != null
                                                    }
                                                    if (index < 0) error("Workspace file nahi mili: $path")
                                                    files[index] = files[index].copy(textContent = content)
                                                    fixedCount++
                                                }

                                                selfFixStatus =
                                                    "${reviewRoot.optString("summary", "Auto-Review complete")}. Auto-fixes applied: $fixedCount."
                                            } else {
                                                selfFixStatus = "Auto-Review skip: supported text/code files nahi milin."
                                            }
                                        } catch (reviewError: Exception) {
                                            selfFixStatus =
                                                "Auto-Review fail/skip: ${reviewError.message ?: "unknown error"}. Generated files phir bhi export ki jayengi."
                                        }
                                    }

                codingStatus = "${additions.size} files Workspace mein add ho gayi hain."
                status = "Tamam files ek ZIP mein pack hongi. Save location select karein."
                zipLauncher.launch("JARVIS-Project-${System.currentTimeMillis()}.zip")
                                } catch (e: Exception) {
                                    codingStatus = "Coding Agent error: ${e.message ?: "response parse nahi hua"}"
                                } finally {
                                    isCoding = false
                                }
                            }
                        }
                    },
                    enabled = !isCoding && codingPrompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCoding) "Generating..." else "Generate Project Files")
                }
                Text(
                    codingStatus,
                    color = Color(0xFFB9D8ED),
                    style = MaterialTheme.typography.bodySmall
                )
                if (generatedPreview.isNotBlank() && isCoding) {
                    Text(
                        "Response preview: ${generatedPreview.takeLast(180).take(180)}",
                        color = Color(0xFF8196AE),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        
Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xAA111D32)),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x5537D9FF), RoundedCornerShape(20.dp))
) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PROJECT CHECKS", color = Color(0xFF67E8F9), fontWeight = FontWeight.Bold)
        Button(
            onClick = {
                val problems = mutableListOf<String>()
                val names = files.map { it.name.replace('\\', '/') }

                if (names.isEmpty()) problems.add("Workspace khaali hai.")
                if (names.size != names.toSet().size) problems.add("Duplicate file paths maujood hain.")

                names.forEach { name ->
                    val parts = name.split('/')
                    if (name.startsWith("/") || name.contains(':') ||
                        parts.any { it.isBlank() || it == "." || it == ".." }) {
                        problems.add("Unsafe path: $name")
                    }
                }

                files.filter { it.textContent != null && it.textContent.isBlank() }
                    .forEach { problems.add("Khaali text file: ${it.name}") }

                if (names.none { it.substringAfterLast('/').equals("README.md", true) ||
                        it.substringAfterLast('/').equals("README.txt", true) }) {
                    problems.add("README file nahi mili (warning).")
                }

                if (names.none { it.endsWith("build.gradle", true) ||
                        it.endsWith("build.gradle.kts", true) ||
                        it.endsWith("settings.gradle", true) ||
                        it.endsWith("settings.gradle.kts", true) }) {
                    problems.add("Gradle build configuration nahi mili (warning).")
                }

                projectCheckStatus = if (problems.isEmpty()) {
                    "Static checks pass: ${names.size} files check hui."
                } else {
                    "Checks complete (${problems.size} finding(s)):\n" +
                        problems.distinct().joinToString("\n") { "• $it" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Project Checks")
        }

        Button(
            onClick = {
                if (!isReviewing) {
                    scope.launch {
                        isReviewing = true
                        selfFixStatus = "Code review ho raha hai..."
                        try {
                            val reviewFiles = files.filter {
                                it.textContent != null &&
                                (it.name.endsWith(".kt", true) ||
                                 it.name.endsWith(".java", true) ||
                                 it.name.endsWith(".xml", true) ||
                                 it.name.endsWith(".gradle", true) ||
                                 it.name.endsWith(".kts", true) ||
                                 it.name.endsWith(".json", true) ||
                                 it.name.endsWith(".md", true) ||
                                 it.name.endsWith(".txt", true) ||
                                 it.name.endsWith(".properties", true))
                            }.take(12)

                            if (reviewFiles.isEmpty()) {
                                error("Review ke liye koi supported text/code file nahi mili.")
                            }

                            val totalChars = reviewFiles.sumOf { it.textContent!!.length }
                            if (totalChars > 100_000) {
                                error("Review limit 100,000 characters hai. Files kam karein.")
                            }

                            val source = buildString {
                                appendLine("Review these existing project files for concrete bugs.")
                                reviewFiles.forEach {
                                    appendLine("\n--- FILE: ${it.name} ---")
                                    appendLine(it.textContent)
                                }
                            }

                            val systemPrompt = """
You are a careful code review and repair agent.
Return ONLY valid JSON:
{"summary":"short explanation","fixes":[{"path":"existing/relative/path","content":"complete corrected file"}]}
Only include files that truly need correction. Each fix must contain the COMPLETE file.
Never invent paths, delete files, change unrelated functionality, or use absolute paths or ../.
Do not claim tests were run. Maximum 12 fixes and 150000 total output characters.
If no safe fix is needed, return {"summary":"No safe changes recommended","fixes":[]}.
""".trimIndent()

                            val decision = viewModel.aiModelRouter.routeRequest(
                                prompt = source,
                                hasImageAttachment = false,
                                forceResearch = false,
                                routingMode = RoutingMode.AUTO_SMART,
                                preferredProvider = AIProviderType.GEMINI,
                                preferredModelId = "gemini-3.8-flash",
                                isProviderConfigured = { provider ->
                                    viewModel.aiService.isProviderKeyAvailable(provider)
                                }
                            )

                            var accumulated = ""
                            val outcome = viewModel.aiService.streamResponse(
                                provider = decision.provider,
                                modelId = decision.modelId,
                                prompt = source,
                                history = emptyList(),
                                personality = AssistantPersonality.TECHNICAL_EXPERT,
                                memoryContextPrompt = systemPrompt,
                                imageBitmap = null,
                                onChunk = { _, text, _ -> accumulated = text }
                            )

                            if (outcome.isError) {
                                error(outcome.fullText.ifBlank { "AI review request fail hui." })
                            }

                            var raw = outcome.fullText.trim()
                            if (raw.startsWith("```")) {
                                raw = raw.substringAfter('\n', raw).removeSuffix("```").trim()
                            }

                            val root = JSONObject(raw)
                            val fixes = root.optJSONArray("fixes")
                                ?: error("AI response mein fixes array nahi mila.")
                            if (fixes.length() > 12) error("AI fix limit se zyada files hain.")

                            val allowed = reviewFiles.map { it.name to it }.toMap()
                            val seen = mutableSetOf<String>()
                            var changed = 0
                            var changedChars = 0

                            for (i in 0 until fixes.length()) {
                                val fix = fixes.optJSONObject(i)
                                    ?: error("Fix #${i + 1} ka format ghalat hai.")
                                val path = fix.optString("path", "")
                                val content = fix.optString("content", "")
                                if (path !in allowed || !seen.add(path)) {
                                    error("Fix unknown ya duplicate file ke liye hai: $path")
                                }
                                if (content.isBlank()) error("Fix content khaali hai: $path")
                                changedChars += content.length
                                if (changedChars > 150_000) error("AI fixes size limit se bari hain.")

                                val index = files.indexOfFirst { it.name == path && it.textContent != null }
                                if (index < 0) error("Workspace file ab maujood nahi: $path")
                                files[index] = files[index].copy(textContent = content)
                                changed++
                            }

                            selfFixStatus =
                                "${root.optString("summary", "Review complete")}. Files update: $changed. Ab build/test chalayein."
                        } catch (e: Exception) {
                            selfFixStatus = "AI Review error: ${e.message ?: "unknown error"}"
                        } finally {
                            isReviewing = false
                        }
                    }
                }
            },
            enabled = !isReviewing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isReviewing) "Reviewing code..." else "AI Review & Self-Fix")
        }
        Text(
            selfFixStatus,
            color = Color(0xFFB9D8ED),
            style = MaterialTheme.typography.bodySmall
        )

        Text(projectCheckStatus, color = Color(0xFFB9D8ED), style = MaterialTheme.typography.bodySmall)
    }
}

Text("PDF • Images • Videos • Documents • Source code • ZIP", color = Color(0xFF9FB4CD), style = MaterialTheme.typography.bodyMedium)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xAA111D32)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x5537D9FF), RoundedCornerShape(20.dp))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { zipImportLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Archive, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import ZIP and extract files")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(6.dp)); Text("Import files")
                    }
                    OutlinedButton(onClick = { showCodeEditor = true }, modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add code")
                    }
                }
                                OutlinedButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (savedFolderUri == null) "Choose save folder" else "Change save folder")
                }
                Text(
                    if (savedFolderUri == null) "Save location: choose per export"
                    else "Save location: remembered folder",
                    color = Color(0xFF9FB4CD),
                    style = MaterialTheme.typography.labelSmall
                )
                Button(
                    onClick = {
                        if (files.isEmpty()) {
                            status = "Pehle kam az kam ek file add karein."
                        } else {
                            val fileName = "JARVIS-Project-${System.currentTimeMillis()}.zip"
                            val folder = savedFolderUri
                            if (folder == null) {
                                pendingZip = true
                                zipLauncher.launch(fileName)
                            } else {
                                val snapshot = files.toList()
                                scope.launch {
                                    status = "ZIP remembered folder mein save ho rahi hai…"
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val destination = WorkspaceSaveLocation.createFile(
                                                context, folder, "application/zip", fileName
                                            )
                                            writeZip(context, destination, snapshot)
                                        }
                                    }
                                    status = result.fold(
                                        { "ZIP successfully save ho gayi (${snapshot.size} entries)." },
                                        { "ZIP save fail: ${it.message ?: "unknown error"}" }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Archive, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download all files as ZIP")
                }
                OutlinedButton(onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}" )).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        status = "Display over other apps permission allow karein, phir Start Floating Orb dabayein."
                    } else {
                        ContextCompat.startForegroundService(context, Intent(context, FloatingOrbService::class.java).setAction(FloatingOrbService.ACTION_START))
                        status = "Floating Orb start command bhej di gayi."
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Start Floating Orb")
                }
                OutlinedButton(onClick = {
                    context.startService(Intent(context, FloatingOrbService::class.java).setAction(FloatingOrbService.ACTION_STOP))
                    status = "Floating Orb stop command bhej di gayi."
                }, modifier = Modifier.fillMaxWidth()) { Text("Stop Floating Orb") }
            }
        }
        Text(status, color = Color(0xFF7DE7C7), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Workspace files", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp)); Text("${files.size}", color = Color(0xFF55DFFF))
        }
        if (files.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Abhi koi file add nahi hui. Import files ya Add code use karein.", color = Color(0xFF91A6BF))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                itemsIndexed(files, key = { index, item -> "${item.name}-$index" }) { index, item ->
                    Surface(color = Color(0xCC111A2D), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Default.FileOpen, null, tint = Color(0xFF55DFFF), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(item.mimeType, color = Color(0xFF91A6BF), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                            TextButton(onClick = {
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                runCatching { readWorkspacePreview(context, item) }
                                            }
                                            result.fold(
                                                { content ->
                                                    previewFile = item
                                                    previewContent = content
                                                    showPreview = true
                                                },
                                                { error -> status = "Preview nahi khul saka: ${error.message ?: "unknown error"}" }
                                            )
                                        }
                                    }) { Text("View") }
                              TextButton(onClick = {
                                                                        pendingSingleIndex = index
                                    val folder = savedFolderUri
                                    if (folder == null) {
                                        singleExportLauncher.launch(
                                            item.name.substringAfterLast('/').ifBlank { "exported-file.bin" }
                                        )
                                    } else {
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    val destination = WorkspaceSaveLocation.createFile(
                                                        context, folder, item.mimeType,
                                                        item.name.substringAfterLast('/').ifBlank { "exported-file.bin" }
                                                    )
                                                    copyWorkspaceItem(context, item, destination)
                                                }
                                            }
                                            status = result.fold(
                                                { "${item.name} remembered folder mein save ho gayi." },
                                                { "Export fail: ${it.message ?: "unknown error"}" }
                                            )
                                        }
                                        pendingSingleIndex = -1
                                    }
                                }) { Text("Save") }
                            androidx.compose.material3.IconButton(onClick = { files.removeAt(index); status = "${item.name} remove kar di gayi." }) {
                                androidx.compose.material3.Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF7E91))
                            }
                        }
                    }
                }
            }
        }
        Text("Security: Android file picker use hota hai; broad storage permission ki zaroorat nahi. ZIP import/extraction is version mein enabled nahi—sirf safe file import aur ZIP export hai.", color = Color(0xFF8196AE), style = MaterialTheme.typography.labelSmall)
    }
}


private fun sanitizeEntryName(raw: String): String = raw.replace('\\', '/')
    .split('/')
    .filter { it.isNotBlank() && it != "." && it != ".." }
    .map { segment -> segment.filter { it >= ' ' && it != ':' && it != '\u0000' } }
    .filter { it.isNotBlank() }
    .joinToString("/")

private fun queryDisplayName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "imported-file"
}

private suspend fun extractWorkspaceZip(context: Context, source: Uri): List<WorkspaceFile> {
    val resolver = context.contentResolver
    val input = resolver.openInputStream(source) ?: error("ZIP file open nahi ho saki")
    val root = File(context.cacheDir, "workspace_zip_${System.currentTimeMillis()}").apply {
        if (!mkdirs()) error("Temporary extraction folder create nahi hua")
    }
    val result = mutableListOf<WorkspaceFile>()
    var totalBytes = 0L
    var entryCount = 0
    try {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                entryCount++
                require(entryCount <= 500) { "ZIP mein 500 se zyada files hain" }

                val raw = entry.name
                require(!raw.startsWith("/") && !raw.startsWith("\\") &&
                    !Regex("^[A-Za-z]:").containsMatchIn(raw) &&
                    !raw.contains('\u0000')) { "Unsafe ZIP path reject kiya gaya" }

                val safe = sanitizeEntryName(raw)
                require(safe.isNotBlank()) { "ZIP mein invalid file path hai" }
                val outFile = File(root, safe).canonicalFile
                require(outFile.path.startsWith(root.canonicalPath + File.separator)) {
                    "Unsafe ZIP path reject kiya gaya"
                }
                val parent = outFile.parentFile
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    error("Extraction folder create nahi hua")
                }

                var entryBytes = 0L
                BufferedOutputStream(outFile.outputStream()).use { out ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        require(entryBytes <= 20L * 1024 * 1024) {
                            "Ek file 20 MB limit se bari hai"
                        }
                        require(totalBytes <= 50L * 1024 * 1024) {
                            "ZIP ka extracted data 50 MB limit se zyada hai"
                        }
                        out.write(buffer, 0, count)
                    }
                }
                val mime = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(outFile.extension.lowercase(Locale.ROOT))
                    ?: "application/octet-stream"
                result.add(WorkspaceFile(safe, mime, uri = Uri.fromFile(outFile)))
                zip.closeEntry()
            }
        }
        require(result.isNotEmpty()) { "ZIP mein extract karne ke liye koi file nahi mili" }
        return result
    } catch (e: Exception) {
        root.deleteRecursively()
        throw e
    }
}

private fun openWorkspaceInput(context: Context, item: WorkspaceFile): java.io.InputStream {
    if (item.textContent != null) return item.textContent.byteInputStream(Charsets.UTF_8)
    val uri = item.uri ?: error("Missing source URI")
    if (uri.scheme == "file") {
        return File(uri.path ?: error("Invalid local file path")).inputStream()
    }
    return context.contentResolver.openInputStream(uri) ?: error("Source file unavailable")
}

private fun readWorkspacePreview(context: Context, item: WorkspaceFile): String {
    val extension = item.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val textExtensions = setOf(
        "txt", "md", "json", "xml", "html", "htm", "css", "js", "ts",
        "kt", "kts", "java", "py", "sh", "gradle", "properties", "yml",
        "yaml", "csv", "log", "sql", "toml", "pro", "gitignore"
    )
    require(item.mimeType.startsWith("text/") || extension in textExtensions) {
        "Is file type ka text preview supported nahi."
    }

    val limit = 1024 * 1024
    val bytes = ByteArrayOutputStream()
    openWorkspaceInput(context, item).use { input ->
        val buffer = ByteArray(8192)
        while (bytes.size() <= limit) {
            val allowed = minOf(buffer.size, limit + 1 - bytes.size())
            val count = input.read(buffer, 0, allowed)
            if (count < 0) break
            bytes.write(buffer, 0, count)
        }
    }
    val data = bytes.toByteArray()
    val truncated = data.size > limit
    val content = String(data, 0, minOf(data.size, limit), Charsets.UTF_8)
    return if (truncated) content + "\n\n[Preview 1 MB tak limited hai]" else content
}

private fun copyWorkspaceItem(context: Context, item: WorkspaceFile, destination: Uri) {
    val output = context.contentResolver.openOutputStream(destination, "w")
        ?: error("Destination stream unavailable")
    output.use { out -> openWorkspaceInput(context, item).use { input -> input.copyTo(out) } }
}

private fun writeZip(context: Context, destination: Uri, items: List<WorkspaceFile>) {
    val output = context.contentResolver.openOutputStream(destination, "w") ?: error("ZIP destination unavailable")
    val usedNames = mutableSetOf<String>()
    ZipOutputStream(BufferedOutputStream(output)).use { zip ->
        items.forEachIndexed { index, item ->
            var safeName = sanitizeEntryName(item.name).ifBlank { "file-$index.bin" }
            val parent = safeName.substringBeforeLast('/', "")
            val leaf = safeName.substringAfterLast('/')
            val base = leaf.substringBeforeLast('.', leaf)
            val ext = leaf.substringAfterLast('.', "").let { if (it == leaf) "" else ".$it" }
            var suffix = 1
            while (!usedNames.add(safeName)) {
                val nextLeaf = "$base-${suffix++}$ext"
                safeName = if (parent.isBlank()) nextLeaf else "$parent/$nextLeaf"
            }
            zip.putNextEntry(ZipEntry(safeName))
            openWorkspaceInput(context, item).use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

/** User-enabled floating overlay. Requires the explicit Android "display over other apps" grant. */
class FloatingOrbService : Service() {
    private var windowManager: WindowManager? = null
    private var orbView: FloatingOrbView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { removeOrb(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        createChannel()
        val openIntent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, com.example.MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 101, openIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Floating Orb")
            .setContentText("Tap the floating Orb to open JARVIS")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        if (orbView == null) showOrb()
        return START_STICKY
    }

    private fun createChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "JARVIS Floating Orb", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun showOrb() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val type = if (VERSION.SDK_INT >= VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(76, 76, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, android.graphics.PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 24; y = 220
        }
        val view = FloatingOrbView(this) { openAssistant() }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { v.tag = floatArrayOf(event.rawX, event.rawY, lp.x.toFloat(), lp.y.toFloat()); true }
                MotionEvent.ACTION_MOVE -> {
                    val start = v.tag as? FloatArray
                    if (start != null) { lp.x = (start[2] + event.rawX - start[0]).toInt(); lp.y = (start[3] + event.rawY - start[1]).toInt(); runCatching { windowManager?.updateViewLayout(v, lp) } }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val start = v.tag as? FloatArray
                    if (start != null && kotlin.math.abs(event.rawX - start[0]) < 10 && kotlin.math.abs(event.rawY - start[1]) < 10) openAssistant()
                    true
                }
                else -> true
            }
        }
        params = lp; orbView = view
        runCatching { windowManager?.addView(view, lp) }.onFailure { orbView = null; stopSelf() }
    }

    private fun openAssistant() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, com.example.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun removeOrb() {
        orbView?.let { view -> runCatching { windowManager?.removeView(view) } }
        orbView = null; params = null; windowManager = null
    }
    override fun onDestroy() { removeOrb(); super.onDestroy() }

    companion object {
        const val ACTION_START = "com.example.workspace.FLOATING_ORB_START"
        const val ACTION_STOP = "com.example.workspace.FLOATING_ORB_STOP"
        private const val CHANNEL_ID = "hasseena_floating_orb"
        private const val NOTIFICATION_ID = 4071
    }
}

private class FloatingOrbView(context: Context, private val onTap: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f; val cy = height / 2f; val r = minOf(width, height) * 0.39f
        paint.shader = RadialGradient(cx, cy, r * 1.9f, intArrayOf(0x9937DFFF.toInt(), 0x443F62FF, 0x003F62FF), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.9f, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f; paint.color = 0xFF57E7FF.toInt(); canvas.drawCircle(cx, cy, r * 1.28f, paint)
        paint.style = Paint.Style.FILL; paint.shader = RadialGradient(cx - r * .25f, cy - r * .3f, r * 1.5f, intArrayOf(0xFFDBFFFF.toInt(), 0xFF2CBFEF.toInt(), 0xFF3522A7.toInt()), null, Shader.TileMode.CLAMP); canvas.drawCircle(cx, cy, r, paint); paint.shader = null
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.3f; paint.color = 0xFFB8F8FF.toInt(); canvas.drawArc(cx-r*1.48f, cy-r*.65f, cx+r*1.48f, cy+r*.65f, -22f, 220f, false, paint); paint.style = Paint.Style.FILL
    }
}
