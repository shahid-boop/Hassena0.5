package com.example.services.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.models.AgentToolResult
import com.example.models.AgentToolSpec
import com.example.models.AIProviderType
import com.example.models.ResearchMode
import com.example.services.ai.AIService
import com.example.services.android.AndroidControlService
import com.example.services.android.AppLauncherService
import com.example.services.memory.MemoryRepository
import com.example.services.notifications.HasseenaNotificationListenerService
import com.example.services.search.WebResearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentToolRegistry(
    private val context: Context,
    private val androidControlService: AndroidControlService,
    private val appLauncherService: AppLauncherService,
    private val webResearchService: WebResearchService,
    private val memoryRepository: MemoryRepository,
    private val aiService: AIService
) {

    val toolSpecs: List<AgentToolSpec> = listOf(
        AgentToolSpec(
            toolId = "web_search",
            name = "Web Search",
            description = "Searches the live web using Wikipedia and DuckDuckGo for fast facts, current events, and references.",
            requiredArguments = listOf("query"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "web_search(query = \"Mars rover discoveries 2024\")"
        ),
        AgentToolSpec(
            toolId = "deep_research",
            name = "Deep Research",
            description = "Performs deep parallel multi-query web research and synthesizes comprehensive findings with source citations.",
            requiredArguments = listOf("query"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "deep_research(query = \"Quantum computing topological qubits progress\")"
        ),
        AgentToolSpec(
            toolId = "search_knowledge",
            name = "Search Knowledge Base",
            description = "Searches local indexed knowledge documents, guidelines, notes, and imported files in the SQLite database.",
            requiredArguments = listOf("query"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "search_knowledge(query = \"System Architecture guidelines\")"
        ),
        AgentToolSpec(
            toolId = "save_memory",
            name = "Save Memory Fact",
            description = "Stores a permanent fact, preference, or directive in Hasseena's long-term memory store.",
            requiredArguments = listOf("category", "content"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "save_memory(category = \"PREFERENCE\", content = \"Prefers detailed technical summaries\")"
        ),
        AgentToolSpec(
            toolId = "get_device_telemetry",
            name = "Device Telemetry",
            description = "Reads real Android device telemetry: battery %, charging status, RAM, free storage, Wi-Fi/Cellular connectivity, volume, and brightness.",
            requiredArguments = emptyList(),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "get_device_telemetry()"
        ),
        AgentToolSpec(
            toolId = "set_flashlight",
            name = "Flashlight Torch",
            description = "Turns the camera hardware flashlight on or off.",
            requiredArguments = listOf("enabled"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "set_flashlight(enabled = \"true\")"
        ),
        AgentToolSpec(
            toolId = "set_volume",
            name = "Set Media Volume",
            description = "Sets or adjusts media stream volume. Specify percent (0-100) or direction (UP, DOWN, MUTE, MAX).",
            requiredArguments = emptyList(),
            optionalArguments = listOf("percent", "direction"),
            requiresConfirmation = false,
            exampleUsage = "set_volume(percent = \"65\")"
        ),
        AgentToolSpec(
            toolId = "set_brightness",
            name = "Set Screen Brightness",
            description = "Adjusts screen brightness percentage (5 to 100).",
            requiredArguments = listOf("percent"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "set_brightness(percent = \"75\")"
        ),
        AgentToolSpec(
            toolId = "launch_app",
            name = "Launch Application",
            description = "Launches an installed Android app by name or system utility alias (e.g. 'YouTube', 'Settings', 'Camera', 'Clock').",
            requiredArguments = listOf("app_name"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "launch_app(app_name = \"YouTube\")"
        ),
        AgentToolSpec(
            toolId = "open_url",
            name = "Open Web URL",
            description = "Opens a web link in the default Android web browser.",
            requiredArguments = listOf("url"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "open_url(url = \"https://en.wikipedia.org\")"
        ),
        AgentToolSpec(
            toolId = "list_installed_apps",
            name = "List Installed Apps",
            description = "Queries the package manager for installed launcher applications, optionally filtering by keyword.",
            requiredArguments = emptyList(),
            optionalArguments = listOf("filter"),
            requiresConfirmation = false,
            exampleUsage = "list_installed_apps(filter = \"music\")"
        ),
        AgentToolSpec(
            toolId = "read_active_notifications",
            name = "Read Notifications",
            description = "Reads active notifications captured by Hasseena's Notification Listener Service.",
            requiredArguments = emptyList(),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "read_active_notifications()"
        ),
        AgentToolSpec(
            toolId = "set_timer_alarm",
            name = "Set Timer or Alarm",
            description = "Sets a countdown timer (in seconds) or scheduled alarm (hour 0-23, minute 0-59).",
            requiredArguments = listOf("type"), // "timer" or "alarm"
            optionalArguments = listOf("seconds", "hour", "minute", "label"),
            requiresConfirmation = false,
            exampleUsage = "set_timer_alarm(type = \"timer\", seconds = \"300\", label = \"Pasta\")"
        ),
        AgentToolSpec(
            toolId = "place_phone_call",
            name = "Place Phone Call",
            description = "Initiates an outgoing phone call to a phone number or contact. SENSITIVE: Requires explicit user approval.",
            requiredArguments = listOf("phone_number"),
            optionalArguments = emptyList(),
            requiresConfirmation = true,
            requiredPermission = Manifest.permission.CALL_PHONE,
            exampleUsage = "place_phone_call(phone_number = \"+15551234567\")"
        ),
        AgentToolSpec(
            toolId = "compose_sms",
            name = "Compose SMS Message",
            description = "Prepares an SMS text message to a recipient with body text. SENSITIVE: Requires explicit user approval.",
            requiredArguments = listOf("recipient", "message"),
            optionalArguments = emptyList(),
            requiresConfirmation = true,
            exampleUsage = "compose_sms(recipient = \"555-0199\", message = \"Meeting moved to 3pm\")"
        ),
        AgentToolSpec(
            toolId = "synthesize_report",
            name = "Synthesize Final Report",
            description = "Analyzes all previously collected step data and produces a comprehensive executive answer or summary.",
            requiredArguments = listOf("objective", "data_summary"),
            optionalArguments = emptyList(),
            requiresConfirmation = false,
            exampleUsage = "synthesize_report(objective = \"...\", data_summary = \"...\")"
        )
    )

    fun getToolSpec(toolId: String): AgentToolSpec? {
        return toolSpecs.firstOrNull { it.toolId.equals(toolId, ignoreCase = true) }
    }

    suspend fun executeTool(
        toolId: String,
        arguments: Map<String, String>,
        onProgress: (String) -> Unit
    ): AgentToolResult = withContext(Dispatchers.IO) {
        val spec = getToolSpec(toolId) ?: return@withContext AgentToolResult(
            success = false,
            output = "Error: Tool '$toolId' is not registered in the Autonomous Agent catalog.",
            error = "Unknown tool identifier"
        )

        // Validate required arguments
        for (req in spec.requiredArguments) {
            val value = arguments[req]
            if (value.isNullOrBlank()) {
                return@withContext AgentToolResult(
                    success = false,
                    output = "Error: Missing required argument '$req' for tool '${spec.name}'.",
                    error = "Missing argument: $req"
                )
            }
        }

        try {
            when (toolId.lowercase()) {
                "web_search" -> {
                    val query = arguments["query"].orEmpty()
                    onProgress("Executing web search: \"$query\"...")
                    val result = webResearchService.executeResearch(
                        query = query,
                        mode = ResearchMode.FAST_SEARCH,
                        preferredProvider = AIProviderType.GEMINI,
                        onProgressStatus = { onProgress(it) },
                        onStreamChunk = {}
                    )
                    if (result.isError) {
                        AgentToolResult(
                            success = false,
                            output = result.errorMessage ?: "Search failed.",
                            error = result.errorMessage
                        )
                    } else {
                        val sourcesText = if (result.sources.isNotEmpty()) {
                            "\nSources:\n" + result.sources.joinToString("\n") { "- ${it.title} (${it.url})" }
                        } else ""
                        AgentToolResult(
                            success = true,
                            output = result.synthesisText + sourcesText,
                            metadata = mapOf("query" to query, "source_count" to result.sources.size.toString())
                        )
                    }
                }

                "deep_research" -> {
                    val query = arguments["query"].orEmpty()
                    onProgress("Executing deep multi-query research: \"$query\"...")
                    val result = webResearchService.executeResearch(
                        query = query,
                        mode = ResearchMode.DEEP_RESEARCH,
                        preferredProvider = AIProviderType.GEMINI,
                        onProgressStatus = { onProgress(it) },
                        onStreamChunk = {}
                    )
                    if (result.isError) {
                        AgentToolResult(
                            success = false,
                            output = result.errorMessage ?: "Deep research failed.",
                            error = result.errorMessage
                        )
                    } else {
                        val sourcesText = if (result.sources.isNotEmpty()) {
                            "\nVerified Sources:\n" + result.sources.joinToString("\n") { "- ${it.title} (${it.url})" }
                        } else ""
                        AgentToolResult(
                            success = true,
                            output = result.synthesisText + sourcesText,
                            metadata = mapOf("query" to query, "source_count" to result.sources.size.toString())
                        )
                    }
                }

                "search_knowledge" -> {
                    val query = arguments["query"].orEmpty()
                    onProgress("Searching local SQLite knowledge base for \"$query\"...")
                    val items = memoryRepository.searchKnowledge(query)
                    if (items.isEmpty()) {
                        AgentToolResult(
                            success = true,
                            output = "No matching knowledge entries found in local database for query '$query'.",
                            metadata = mapOf("count" to "0")
                        )
                    } else {
                        val text = items.joinToString("\n\n") { item ->
                            "Document: ${item.title} (${item.type})\nSnippet: ${item.contentSnippet}\nContent: ${item.fullText.take(400)}"
                        }
                        AgentToolResult(
                            success = true,
                            output = "Found ${items.size} knowledge items:\n\n$text",
                            metadata = mapOf("count" to items.size.toString())
                        )
                    }
                }

                "save_memory" -> {
                    val category = arguments["category"] ?: "FACT"
                    val content = arguments["content"].orEmpty()
                    onProgress("Saving memory fact...")
                    memoryRepository.addMemoryFact(category, content, source = "Autonomous Agent")
                    AgentToolResult(
                        success = true,
                        output = "Successfully saved memory item in category [$category]: \"$content\"",
                        metadata = mapOf("category" to category)
                    )
                }

                "get_device_telemetry" -> {
                    onProgress("Reading real-time device sensors and system state...")
                    val telemetry = androidControlService.getDeviceTelemetry()
                    val output = buildString {
                        appendLine("Device Model: ${telemetry.deviceModel}")
                        appendLine("OS Version: ${telemetry.androidVersion}")
                        appendLine("Battery Level: ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "On Battery"}, Temp: ${telemetry.batteryTempCelsius}°C)")
                        appendLine("Network: ${telemetry.networkTypeLabel} (Wi-Fi: ${telemetry.wifiConnected})")
                        appendLine("RAM: ${telemetry.availableRamMb} MB available / ${telemetry.totalRamMb} MB total")
                        appendLine("Storage: ${telemetry.freeStorageGb} GB free")
                        appendLine("Media Volume: ${telemetry.musicVolumePercent}%, Screen Brightness: ${telemetry.screenBrightnessPercent}%")
                        appendLine("Flashlight: ${if (telemetry.flashlightOn) "ON" else "OFF"}")
                    }
                    AgentToolResult(
                        success = true,
                        output = output,
                        metadata = mapOf(
                            "battery" to "${telemetry.batteryPercent}%",
                            "network" to telemetry.networkTypeLabel
                        )
                    )
                }

                "set_flashlight" -> {
                    val enable = arguments["enabled"]?.trim()?.equals("true", ignoreCase = true)
                        ?: arguments["enabled"]?.trim()?.equals("1")
                        ?: false
                    onProgress("Setting flashlight to ${if (enable) "ON" else "OFF"}...")
                    val outcome = androidControlService.setFlashlight(enable)
                    AgentToolResult(
                        success = outcome.success,
                        output = outcome.detailText,
                        error = if (!outcome.success) outcome.detailText else null
                    )
                }

                "set_volume" -> {
                    val pct = arguments["percent"]?.toIntOrNull()
                    val dirStr = arguments["direction"]?.uppercase()
                    val direction = when (dirStr) {
                        "UP" -> AndroidControlService.VolumeDirection.UP
                        "DOWN" -> AndroidControlService.VolumeDirection.DOWN
                        "MUTE" -> AndroidControlService.VolumeDirection.MUTE
                        "MAX" -> AndroidControlService.VolumeDirection.MAX
                        else -> AndroidControlService.VolumeDirection.SET_PERCENT
                    }
                    onProgress("Adjusting media volume...")
                    val outcome = androidControlService.adjustVolume(direction, pct)
                    AgentToolResult(
                        success = outcome.success,
                        output = outcome.detailText,
                        error = if (!outcome.success) outcome.detailText else null
                    )
                }

                "set_brightness" -> {
                    val pct = arguments["percent"]?.toIntOrNull() ?: 50
                    onProgress("Setting display brightness to $pct%...")
                    val outcome = androidControlService.setScreenBrightnessPercent(pct)
                    AgentToolResult(
                        success = outcome.success,
                        output = outcome.detailText,
                        error = if (!outcome.success) outcome.detailText else null
                    )
                }

                "launch_app" -> {
                    val appName = arguments["app_name"].orEmpty()
                    onProgress("Launching application \"$appName\"...")
                    val outcome = appLauncherService.launchAppByNameOrAlias(appName)
                    AgentToolResult(
                        success = outcome.launched,
                        output = outcome.detailMessage,
                        error = if (!outcome.launched) outcome.detailMessage else null,
                        metadata = if (outcome.packageName != null) mapOf("package" to outcome.packageName) else emptyMap()
                    )
                }

                "open_url" -> {
                    var url = arguments["url"].orEmpty().trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    onProgress("Opening URL in browser...")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        AgentToolResult(
                            success = true,
                            output = "Successfully launched browser intent for $url",
                            metadata = mapOf("url" to url)
                        )
                    } catch (e: Exception) {
                        AgentToolResult(
                            success = false,
                            output = "Could not open URL: ${e.localizedMessage}",
                            error = e.localizedMessage
                        )
                    }
                }

                "list_installed_apps" -> {
                    val filter = arguments["filter"]?.trim()?.lowercase().orEmpty()
                    onProgress("Querying installed applications...")
                    val apps = appLauncherService.refreshInstalledApps()
                    val filtered = if (filter.isNotBlank()) {
                        apps.filter { it.appName.lowercase().contains(filter) || it.packageName.lowercase().contains(filter) }
                    } else {
                        apps
                    }
                    val formatted = filtered.take(40).joinToString("\n") { "- ${it.appName} (${it.packageName})" }
                    AgentToolResult(
                        success = true,
                        output = "Found ${filtered.size} apps (showing top ${filtered.take(40).size}):\n$formatted",
                        metadata = mapOf("count" to filtered.size.toString())
                    )
                }

                "read_active_notifications" -> {
                    onProgress("Checking active system notifications...")
                    val notifs = HasseenaNotificationListenerService.notifications.value
                    if (notifs.isEmpty()) {
                        val isEnabled = HasseenaNotificationListenerService.isNotificationAccessEnabled(context)
                        val msg = if (!isEnabled) {
                            "Notification listener permission is not enabled. Grant notification access in Android Settings to read active notifications."
                        } else {
                            "No active notifications found in the status bar."
                        }
                        AgentToolResult(
                            success = true,
                            output = msg,
                            metadata = mapOf("enabled" to isEnabled.toString())
                        )
                    } else {
                        val list = notifs.joinToString("\n") { n ->
                            "• [${n.appName}] ${n.title}: ${n.text}"
                        }
                        AgentToolResult(
                            success = true,
                            output = "Active notifications (${notifs.size}):\n$list",
                            metadata = mapOf("count" to notifs.size.toString())
                        )
                    }
                }

                "set_timer_alarm" -> {
                    val type = arguments["type"]?.lowercase() ?: "timer"
                    val label = arguments["label"] ?: "Hasseena Agent Alert"
                    if (type == "alarm") {
                        val hour = arguments["hour"]?.toIntOrNull() ?: 8
                        val minute = arguments["minute"]?.toIntOrNull() ?: 0
                        onProgress("Setting alarm for $hour:${minute.toString().padStart(2, '0')}...")
                        val outcome = androidControlService.setAlarm(hour, minute, label)
                        AgentToolResult(
                            success = outcome.success,
                            output = outcome.detailText,
                            error = if (!outcome.success) outcome.detailText else null
                        )
                    } else {
                        val seconds = arguments["seconds"]?.toIntOrNull() ?: 60
                        onProgress("Setting countdown timer for $seconds seconds...")
                        val outcome = androidControlService.setTimer(seconds, label)
                        AgentToolResult(
                            success = outcome.success,
                            output = outcome.detailText,
                            error = if (!outcome.success) outcome.detailText else null
                        )
                    }
                }

                "place_phone_call" -> {
                    val phone = arguments["phone_number"].orEmpty()
                    onProgress("Executing confirmed phone call to $phone...")
                    val outcome = androidControlService.executeConfirmedPhoneCall(phone)
                    AgentToolResult(
                        success = outcome.success,
                        output = outcome.detailText,
                        error = if (!outcome.success) outcome.detailText else null
                    )
                }

                "compose_sms" -> {
                    val recipient = arguments["recipient"].orEmpty()
                    val message = arguments["message"].orEmpty()
                    onProgress("Preparing SMS to $recipient...")
                    val outcome = androidControlService.executeConfirmedSmsCompose(recipient, message)
                    AgentToolResult(
                        success = outcome.success,
                        output = outcome.detailText,
                        error = if (!outcome.success) outcome.detailText else null
                    )
                }

                "synthesize_report" -> {
                    val objective = arguments["objective"].orEmpty()
                    val summaryData = arguments["data_summary"].orEmpty()
                    onProgress("Synthesizing final executive intelligence report...")
                    val prompt = """
                        You are the Hasseena A1 Autonomous Agent.
                        The user gave you this objective:
                        "$objective"

                        Here is the verified data and evidence collected from executing the autonomous steps:
                        $summaryData

                        Provide a clear, authoritative, and complete final report addressing the user's objective.
                        Include verified facts, key takeaways, and explicit next steps where relevant.
                        Do not include fake tool brackets or speculative statements.
                    """.trimIndent()

                    val report = aiService.generatePureTextResponse(
                        provider = AIProviderType.GEMINI,
                        modelId = "gemini-2.5-flash",
                        systemPrompt = "You are Hasseena A1's Autonomous Intelligence Agent. Synthesize findings into structured executive reports with clear headings.",
                        userPrompt = prompt
                    )
                    AgentToolResult(
                        success = !report.isError,
                        output = report.fullText,
                        error = if (report.isError) report.errorTitle else null,
                        metadata = mapOf("objective" to objective)
                    )
                }

                else -> {
                    AgentToolResult(
                        success = false,
                        output = "Tool '$toolId' is not implemented.",
                        error = "Unimplemented tool"
                    )
                }
            }
        } catch (e: Exception) {
            AgentToolResult(
                success = false,
                output = "Exception during tool execution: ${e.localizedMessage}",
                error = e.localizedMessage ?: "Unknown error"
            )
        }
    }
}
