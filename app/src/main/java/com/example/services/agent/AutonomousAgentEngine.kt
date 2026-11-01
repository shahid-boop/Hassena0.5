package com.example.services.agent

import com.example.models.AgentStep
import com.example.models.AutonomousStepStatus
import com.example.models.AgentTask
import com.example.models.AgentTaskStatus
import com.example.models.AIProviderType
import com.example.services.ai.AIService
import com.example.services.memory.MemoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class AutonomousAgentEngine(
    private val aiService: AIService,
    private val toolRegistry: AgentToolRegistry,
    private val memoryRepository: MemoryRepository,
    private val scope: CoroutineScope,
    var selfHealingManager: com.example.services.healing.SelfHealingManager? = null
) {

    private val _currentTask = MutableStateFlow<AgentTask?>(null)
    val currentTask: StateFlow<AgentTask?> = _currentTask.asStateFlow()

    private val _liveLogs = MutableStateFlow<List<String>>(emptyList())
    val liveLogs: StateFlow<List<String>> = _liveLogs.asStateFlow()

    private var executionJob: Job? = null
    private var pendingConfirmationDeferred: CompletableDeferred<Boolean>? = null
    private var isPaused: Boolean = false

    private val json = Json { ignoreUnknownKeys = true }

    fun addLog(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $entry"
        val updated = (_liveLogs.value + formatted).takeLast(100)
        _liveLogs.value = updated

        val task = _currentTask.value
        if (task != null) {
            val updatedTask = task.copy(logs = (_currentTask.value?.logs ?: emptyList()) + formatted)
            _currentTask.value = updatedTask
        }
    }

    /**
     * Step 1: Takes user objective, analyzes requirements, and generates a structured execution plan.
     */
    fun startPlanning(objective: String) {
        val trimmed = objective.trim()
        if (trimmed.isBlank()) return

        executionJob?.cancel()
        _liveLogs.value = emptyList()
        isPaused = false

        val taskId = "task_${UUID.randomUUID().toString().take(8)}"
        val initialTask = AgentTask(
            id = taskId,
            title = "Analyzing: \"${trimmed.take(30)}...\"",
            userObjective = trimmed,
            executionPlanOverview = "Generating multi-step autonomous plan...",
            status = AgentTaskStatus.PLANNING,
            startTime = System.currentTimeMillis()
        )
        _currentTask.value = initialTask
        addLog("Received objective: \"$trimmed\"")
        addLog("Analyzing requirements & consulting tool registry...")

        scope.launch {
            try {
                memoryRepository.saveAgentTask(initialTask)
                val plannedTask = generatePlanForObjective(taskId, trimmed)
                _currentTask.value = plannedTask
                memoryRepository.saveAgentTask(plannedTask)
                addLog("Execution plan generated (${plannedTask.steps.size} steps). Waiting for user approval.")
            } catch (e: Exception) {
                addLog("Planning error: ${e.localizedMessage}. Generating safe fallback plan...")
                val fallback = generateFallbackPlan(taskId, trimmed)
                _currentTask.value = fallback
                memoryRepository.saveAgentTask(fallback)
                addLog("Fallback plan prepared (${fallback.steps.size} steps). Awaiting approval.")
            }
        }
    }

    private suspend fun generatePlanForObjective(taskId: String, objective: String): AgentTask = withContext(Dispatchers.IO) {
        val toolsSchema = toolRegistry.toolSpecs.joinToString("\n") { spec ->
            "- Tool ID: \"${spec.toolId}\" | Domain: ${spec.domain.label} | Name: ${spec.name}\n" +
            "  Description: ${spec.description}\n" +
            "  Required Args: ${spec.requiredArguments.joinToString(", ")}\n" +
            "  Optional Args: ${spec.optionalArguments.joinToString(", ")}\n" +
            "  Confirmation Needed: ${spec.requiresConfirmation}\n" +
            "  Example: ${spec.exampleUsage}"
        }

        val prompt = """
            You are the Six-in-One Mission Control Planner for Hasseena A1 Autonomous AI Agent.
            The user wants to accomplish this objective:
            "$objective"

            You have 6 integrated capability domains available in the tool registry:
            1. Tool 1: AI Coding & Project Editing (read_project_file, write_project_file, list_project_files, propose_code_refactor, rollback_file_checkpoint)
            2. Tool 2: Real Build & Test Executor (run_build_check, run_unit_tests, parse_compiler_log)
            3. Tool 3: Internet & Browser Automation (web_search, deep_research, fetch_web_page, interact_web_form)
            4. Tool 4: Android Phone Actions (accessibility_read_screen, accessibility_click, accessibility_input_text, accessibility_global_action, launch_app, get_device_telemetry, set_flashlight, set_volume, set_brightness, set_timer_alarm, place_phone_call, compose_sms)
            5. Tool 5: Persistent Memory & Task Queue (search_knowledge, save_memory, manage_mission_queue)
            6. Tool 6: AI Brain & Model API (synthesize_report)

            AVAILABLE REGISTERED TOOLS:
            $toolsSchema

            RULES:
            1. Create a sequential, realistic step-by-step plan (between 2 to 6 steps, maximum 10).
            2. Every step MUST use one of the registered Tool IDs above. Never invent tool IDs.
            3. Each step must have concrete string arguments.
            4. Mark requiresConfirmation as true for code edits, file writes, web forms, phone calls, or SMS.
            5. Final step should synthesize or present findings with synthesize_report if research, telemetry, or code diffs were gathered.
            6. Output ONLY a valid JSON object matching this schema without markdown codeblocks or explanation:
            {
              "title": "Short descriptive title (max 6 words)",
              "overview": "Clear 1-2 sentence summary of how the agent will accomplish this objective",
              "steps": [
                {
                  "stepNumber": 1,
                  "title": "Step title",
                  "description": "What this step performs",
                  "toolId": "tool_id_from_list",
                  "arguments": {"argKey": "argValue"},
                  "requiresConfirmation": false
                }
              ]
            }
        """.trimIndent()

        val response = aiService.generatePureTextResponse(
            provider = AIProviderType.GEMINI,
            modelId = "gemini-2.5-flash",
            systemPrompt = "You are an autonomous AI agent task planning engine. You output valid JSON only.",
            userPrompt = prompt
        )

        val cleanJson = response.fullText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsed = json.parseToJsonElement(cleanJson).jsonObject
        val title = parsed["title"]?.jsonPrimitive?.content ?: "Autonomous Mission"
        val overview = parsed["overview"]?.jsonPrimitive?.content ?: "Autonomous plan execution"
        val stepsJson = parsed["steps"]?.jsonArray ?: throw IllegalArgumentException("No steps array found in plan")

        val steps = stepsJson.mapIndexed { index, element ->
            val sObj = element.jsonObject
            val stepNumber = sObj["stepNumber"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)
            val stepTitle = sObj["title"]?.jsonPrimitive?.content ?: "Step $stepNumber"
            val desc = sObj["description"]?.jsonPrimitive?.content ?: ""
            val toolId = sObj["toolId"]?.jsonPrimitive?.content ?: "synthesize_report"
            val confirmation = sObj["requiresConfirmation"]?.jsonPrimitive?.content?.toBoolean() ?: false

            val argsMap = mutableMapOf<String, String>()
            sObj["arguments"]?.jsonObject?.forEach { (k, v) ->
                argsMap[k] = v.jsonPrimitive.content
            }

            AgentStep(
                id = "${taskId}_step_$stepNumber",
                stepNumber = stepNumber,
                title = stepTitle,
                description = desc,
                toolId = toolId,
                toolArguments = argsMap,
                requiresConfirmation = confirmation || toolRegistry.getToolSpec(toolId)?.requiresConfirmation == true,
                status = AutonomousStepStatus.PENDING
            )
        }

        AgentTask(
            id = taskId,
            title = title,
            userObjective = objective,
            executionPlanOverview = overview,
            steps = steps.take(10),
            status = AgentTaskStatus.WAITING_FOR_APPROVAL,
            startTime = System.currentTimeMillis()
        )
    }

    private fun generateFallbackPlan(taskId: String, objective: String): AgentTask {
        val lower = objective.lowercase()
        val steps = mutableListOf<AgentStep>()

        if (lower.contains("code") || lower.contains("file") || lower.contains("refactor") || lower.contains("bug") || lower.contains("edit") || lower.contains("class")) {
            val targetFile = if (lower.contains("manifest")) "app/src/main/AndroidManifest.xml"
                else if (lower.contains("gradle")) "app/build.gradle.kts"
                else "app/src/main/java/com/example/MainActivity.kt"

            steps.add(
                AgentStep(
                    id = "${taskId}_step_1",
                    stepNumber = 1,
                    title = "Read Target Project File",
                    description = "Inspect source code and structural invariants of $targetFile.",
                    toolId = "read_project_file",
                    toolArguments = mapOf("file_path" to targetFile),
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_2",
                    stepNumber = 2,
                    title = "Propose Code Refactoring",
                    description = "Generate architectural improvement and diff patch for approval.",
                    toolId = "propose_code_refactor",
                    toolArguments = mapOf("file_path" to targetFile, "goal" to objective),
                    requiresConfirmation = true,
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_3",
                    stepNumber = 3,
                    title = "Verify Build & Manifest",
                    description = "Run real build checks to verify integrity.",
                    toolId = "run_build_check",
                    toolArguments = mapOf("component" to "app"),
                    status = AutonomousStepStatus.PENDING
                )
            )
        } else if (lower.contains("build") || lower.contains("test") || lower.contains("compile") || lower.contains("verify")) {
            steps.add(
                AgentStep(
                    id = "${taskId}_step_1",
                    stepNumber = 1,
                    title = "Run Build Verification",
                    description = "Analyze Gradle build scripts, manifest, and compilation readiness.",
                    toolId = "run_build_check",
                    toolArguments = mapOf("component" to "app"),
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_2",
                    stepNumber = 2,
                    title = "Execute Unit Test Suite",
                    description = "Run available test suites and verify assertions.",
                    toolId = "run_unit_tests",
                    toolArguments = mapOf("test_target" to "ExampleRobolectricTest"),
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_3",
                    stepNumber = 3,
                    title = "Synthesize Build & Test Report",
                    description = "Compile verified test diagnostics into executive status.",
                    toolId = "synthesize_report",
                    toolArguments = mapOf("objective" to objective, "data_summary" to "Build and Test Verification"),
                    status = AutonomousStepStatus.PENDING
                )
            )
        } else if (lower.contains("screen") || lower.contains("accessibility") || lower.contains("click") || lower.contains("tap")) {
            steps.add(
                AgentStep(
                    id = "${taskId}_step_1",
                    stepNumber = 1,
                    title = "Inspect Active Screen",
                    description = "Read visible text elements and interactive buttons on screen.",
                    toolId = "accessibility_read_screen",
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_2",
                    stepNumber = 2,
                    title = "Read Device Telemetry",
                    description = "Capture battery, RAM, network, and system state.",
                    toolId = "get_device_telemetry",
                    status = AutonomousStepStatus.PENDING
                )
            )
            steps.add(
                AgentStep(
                    id = "${taskId}_step_3",
                    stepNumber = 3,
                    title = "Synthesize Screen Findings",
                    description = "Summarize screen state and actionable controls.",
                    toolId = "synthesize_report",
                    toolArguments = mapOf("objective" to objective, "data_summary" to "Screen and Device telemetry"),
                    status = AutonomousStepStatus.PENDING
                )
            )
        } else {
            if (lower.contains("battery") || lower.contains("ram") || lower.contains("storage") || lower.contains("device") || lower.contains("telemetry") || lower.contains("system")) {
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_1",
                        stepNumber = 1,
                        title = "Read Device Telemetry",
                        description = "Collect real Android device status including battery, RAM, network, and storage.",
                        toolId = "get_device_telemetry",
                        status = AutonomousStepStatus.PENDING
                    )
                )
            }

            if (lower.contains("flashlight") || lower.contains("torch")) {
                val enable = !lower.contains("off")
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_${steps.size + 1}",
                        stepNumber = steps.size + 1,
                        title = if (enable) "Turn On Flashlight" else "Turn Off Flashlight",
                        description = "Control hardware camera torch.",
                        toolId = "set_flashlight",
                        toolArguments = mapOf("enabled" to enable.toString()),
                        status = AutonomousStepStatus.PENDING
                    )
                )
            }

            if (lower.contains("volume") || lower.contains("mute") || lower.contains("sound")) {
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_${steps.size + 1}",
                        stepNumber = steps.size + 1,
                        title = "Adjust Media Volume",
                        description = "Configure media sound volume level.",
                        toolId = "set_volume",
                        toolArguments = mapOf("percent" to "60"),
                        status = AutonomousStepStatus.PENDING
                    )
                )
            }

            if (lower.contains("brightness") || lower.contains("display")) {
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_${steps.size + 1}",
                        stepNumber = steps.size + 1,
                        title = "Adjust Brightness",
                        description = "Adjust screen brightness percent.",
                        toolId = "set_brightness",
                        toolArguments = mapOf("percent" to "70"),
                        status = AutonomousStepStatus.PENDING
                    )
                )
            }

            if (lower.contains("open") || lower.contains("launch")) {
                val appTarget = objective.substringAfter("open", "").substringAfter("launch", "").trim().take(20)
                if (appTarget.isNotBlank()) {
                    steps.add(
                        AgentStep(
                            id = "${taskId}_step_${steps.size + 1}",
                            stepNumber = steps.size + 1,
                            title = "Launch Application: $appTarget",
                            description = "Open requested app or settings via Android package manager.",
                            toolId = "launch_app",
                            toolArguments = mapOf("app_name" to appTarget),
                            status = AutonomousStepStatus.PENDING
                        )
                    )
                }
            }

            if (steps.isEmpty() || lower.contains("research") || lower.contains("search") || lower.contains("find") || lower.contains("explain") || lower.contains("how")) {
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_1",
                        stepNumber = 1,
                        title = "Deep Web Research",
                        description = "Search Wikipedia and web knowledge for factual data.",
                        toolId = "web_search",
                        toolArguments = mapOf("query" to objective),
                        status = AutonomousStepStatus.PENDING
                    )
                )
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_2",
                        stepNumber = 2,
                        title = "Search Knowledge Base",
                        description = "Check local saved documents and notes for relevant context.",
                        toolId = "search_knowledge",
                        toolArguments = mapOf("query" to objective),
                        status = AutonomousStepStatus.PENDING
                    )
                )
                steps.add(
                    AgentStep(
                        id = "${taskId}_step_3",
                        stepNumber = 3,
                        title = "Synthesize Findings",
                        description = "Compile verified research into an executive answer.",
                        toolId = "synthesize_report",
                        toolArguments = mapOf("objective" to objective, "data_summary" to "Web and Knowledge results"),
                        status = AutonomousStepStatus.PENDING
                    )
                )
            }
        }

        return AgentTask(
            id = taskId,
            title = "Task: ${objective.take(30)}",
            userObjective = objective,
            executionPlanOverview = "Multi-step plan with ${steps.size} steps tailored to objective.",
            steps = steps,
            status = AgentTaskStatus.WAITING_FOR_APPROVAL,
            startTime = System.currentTimeMillis()
        )
    }

    /**
     * Step 2: User approves the plan. Execution begins.
     */
    fun approveAndExecute() {
        val task = _currentTask.value ?: return
        if (task.status != AgentTaskStatus.WAITING_FOR_APPROVAL && task.status != AgentTaskStatus.PAUSED && task.status != AgentTaskStatus.READY) return

        isPaused = false
        val readyTask = task.copy(status = AgentTaskStatus.RUNNING)
        _currentTask.value = readyTask
        addLog("Plan approved by user. Commencing autonomous execution loop...")

        executionJob?.cancel()
        executionJob = scope.launch {
            runExecutionLoop(readyTask)
        }
    }

    private suspend fun runExecutionLoop(startingTask: AgentTask) {
        var current = startingTask
        val collectedResults = mutableListOf<String>()

        memoryRepository.saveAgentTask(current)

        for (i in current.currentStepIndex until current.steps.size) {
            if (!scope.isActive || isPaused) {
                if (isPaused) {
                    addLog("Execution paused by user.")
                    val pausedTask = current.copy(status = AgentTaskStatus.PAUSED)
                    _currentTask.value = pausedTask
                    memoryRepository.saveAgentTask(pausedTask)
                }
                return
            }

            var step = current.steps[i]
            addLog("Starting Step ${step.stepNumber}/${current.steps.size}: ${step.title}")

            // Check if step requires confirmation
            if (step.requiresConfirmation) {
                addLog("Step ${step.stepNumber} is marked sensitive (${step.toolId}). Requesting explicit user authorization...")
                val updatedSteps = current.steps.toMutableList()
                step = step.copy(status = AutonomousStepStatus.WAITING_CONFIRMATION)
                updatedSteps[i] = step
                current = current.copy(
                    steps = updatedSteps,
                    currentStepIndex = i,
                    status = AgentTaskStatus.WAITING_FOR_PERMISSION
                )
                _currentTask.value = current
                memoryRepository.saveAgentTask(current)

                val deferred = CompletableDeferred<Boolean>()
                pendingConfirmationDeferred = deferred
                val approved = deferred.await()
                pendingConfirmationDeferred = null

                if (!approved) {
                    addLog("User denied permission for Step ${step.stepNumber}. Skipping step.")
                    val skippedSteps = current.steps.toMutableList()
                    skippedSteps[i] = step.copy(
                        status = AutonomousStepStatus.SKIPPED,
                        actualResult = "Skipped: Permission denied by user."
                    )
                    current = current.copy(
                        steps = skippedSteps,
                        currentStepIndex = i + 1,
                        status = AgentTaskStatus.RUNNING
                    )
                    _currentTask.value = current
                    memoryRepository.saveAgentTask(current)
                    continue
                } else {
                    addLog("User authorized Step ${step.stepNumber}. Proceeding...")
                }
            }

            // Mark Step as Running
            val runningSteps = current.steps.toMutableList()
            step = step.copy(status = AutonomousStepStatus.RUNNING, startTime = System.currentTimeMillis())
            runningSteps[i] = step
            current = current.copy(steps = runningSteps, currentStepIndex = i, status = AgentTaskStatus.RUNNING)
            _currentTask.value = current
            memoryRepository.saveAgentTask(current)

            // Inject previously collected results if tool is synthesize_report
            val effectiveArgs = step.toolArguments.toMutableMap()
            if (step.toolId.equals("synthesize_report", ignoreCase = true)) {
                effectiveArgs["objective"] = current.userObjective
                effectiveArgs["data_summary"] = collectedResults.joinToString("\n\n")
            }

            // Execute tool in real environment
            val outcome = toolRegistry.executeTool(step.toolId, effectiveArgs) { progress ->
                addLog("  ↳ $progress")
            }

            if (outcome.success) {
                addLog("Step ${step.stepNumber} COMPLETED.")
                val completedSteps = current.steps.toMutableList()
                val completedStep = step.copy(
                    status = AutonomousStepStatus.COMPLETED,
                    actualResult = outcome.output,
                    completionTime = System.currentTimeMillis()
                )
                completedSteps[i] = completedStep
                collectedResults.add("Step ${completedStep.stepNumber} (${completedStep.title}):\n${outcome.output}")

                val updatedFiles = if (outcome.modifiedFiles.isNotEmpty()) {
                    (current.changedFiles + outcome.modifiedFiles).distinct()
                } else {
                    current.changedFiles
                }

                current = current.copy(
                    steps = completedSteps,
                    currentStepIndex = i + 1,
                    changedFiles = updatedFiles
                )
                _currentTask.value = current
                memoryRepository.saveAgentTask(current)
            } else {
                addLog("Step ${step.stepNumber} FAILED: ${outcome.error}")
                val failedSteps = current.steps.toMutableList()
                val failedStep = step.copy(
                    status = AutonomousStepStatus.FAILED,
                    errorMessage = outcome.error,
                    actualResult = outcome.output,
                    completionTime = System.currentTimeMillis()
                )
                failedSteps[i] = failedStep
                current = current.copy(steps = failedSteps)
                _currentTask.value = current
                memoryRepository.saveAgentTask(current)

                // Report failure to Self-Healing AI watchdog
                selfHealingManager?.handleAgentStepFailure(failedStep, current, RuntimeException(outcome.error ?: "Step execution failed"))

                // Check replanning
                if (current.replanCount < 3) {
                    addLog("Initiating autonomous replanning (attempt ${current.replanCount + 1}/3)...")
                    val replanned = attemptReplan(current, failedStep, outcome.error ?: "Operation failed")
                    if (replanned != null) {
                        current = replanned
                        _currentTask.value = current
                        memoryRepository.saveAgentTask(current)
                        addLog("Plan revised. Resuming execution...")
                        continue
                    }
                }

                // If replan cannot fix or exhausted
                addLog("Autonomous task halted due to unrecoverable step failure.")
                val terminatedTask = current.copy(
                    status = AgentTaskStatus.FAILED,
                    errorInformation = "Step ${failedStep.stepNumber} failed: ${outcome.error}",
                    completionTime = System.currentTimeMillis()
                )
                _currentTask.value = terminatedTask
                memoryRepository.saveAgentTask(terminatedTask)
                return
            }
        }

        // All steps executed! Generate final response if needed
        addLog("All steps executed successfully. Finalizing executive summary...")
        val finalResponseText = if (collectedResults.isNotEmpty()) {
            collectedResults.lastOrNull { it.contains("Synthesize") || it.contains("Report") }
                ?: collectedResults.joinToString("\n\n---\n\n")
        } else {
            "Task executed successfully with ${current.steps.size} verified steps."
        }

        val completedTask = current.copy(
            status = AgentTaskStatus.COMPLETED,
            finalResponse = finalResponseText,
            completionTime = System.currentTimeMillis()
        )
        _currentTask.value = completedTask
        memoryRepository.saveAgentTask(completedTask)
        addLog("TASK COMPLETED.")
    }

    private suspend fun attemptReplan(
        task: AgentTask,
        failedStep: AgentStep,
        errorReason: String
    ): AgentTask? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                The autonomous agent was executing the following task:
                Objective: "${task.userObjective}"
                Step ${failedStep.stepNumber} failed with error: "$errorReason"
                Tool attempted: "${failedStep.toolId}" with args: ${failedStep.toolArguments}

                Revise the remaining plan to bypass this failure using alternative tools or queries.
                Output ONLY a JSON array of replacement steps for the remaining work:
                [
                  {
                    "stepNumber": ${failedStep.stepNumber},
                    "title": "Alternative Step Title",
                    "description": "How this bypasses the error",
                    "toolId": "valid_tool_id",
                    "arguments": {"key": "val"},
                    "requiresConfirmation": false
                  }
                ]
            """.trimIndent()

            val response = aiService.generatePureTextResponse(
                provider = AIProviderType.GEMINI,
                modelId = "gemini-2.5-flash",
                systemPrompt = "You are an autonomous AI replanner. You return only JSON arrays of steps.",
                userPrompt = prompt
            )

            val clean = response.fullText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsedArray = json.parseToJsonElement(clean).jsonArray

            val newSteps = parsedArray.mapIndexed { idx, el ->
                val o = el.jsonObject
                val num = failedStep.stepNumber + idx
                val title = o["title"]?.jsonPrimitive?.content ?: "Revised Step $num"
                val desc = o["description"]?.jsonPrimitive?.content ?: ""
                val toolId = o["toolId"]?.jsonPrimitive?.content ?: "synthesize_report"
                val conf = o["requiresConfirmation"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val argsMap = mutableMapOf<String, String>()
                o["arguments"]?.jsonObject?.forEach { (k, v) ->
                    argsMap[k] = v.jsonPrimitive.content
                }
                AgentStep(
                    id = "${task.id}_replan_${num}",
                    stepNumber = num,
                    title = title,
                    description = desc,
                    toolId = toolId,
                    toolArguments = argsMap,
                    requiresConfirmation = conf || toolRegistry.getToolSpec(toolId)?.requiresConfirmation == true,
                    status = AutonomousStepStatus.PENDING
                )
            }

            val completedPriorSteps = task.steps.take(failedStep.stepNumber - 1)
            val combinedSteps = (completedPriorSteps + newSteps).take(10)

            task.copy(
                steps = combinedSteps,
                replanCount = task.replanCount + 1,
                currentStepIndex = failedStep.stepNumber - 1,
                status = AgentTaskStatus.RUNNING
            )
        } catch (_: Exception) {
            null
        }
    }

    fun confirmPendingStep(approved: Boolean) {
        pendingConfirmationDeferred?.complete(approved)
    }

    fun pause() {
        isPaused = true
        executionJob?.cancel()
        val current = _currentTask.value ?: return
        val paused = current.copy(status = AgentTaskStatus.PAUSED)
        _currentTask.value = paused
        scope.launch { memoryRepository.saveAgentTask(paused) }
        addLog("Task paused.")
    }

    fun resume() {
        val current = _currentTask.value ?: return
        if (current.status == AgentTaskStatus.PAUSED) {
            approveAndExecute()
        }
    }

    fun cancel() {
        executionJob?.cancel()
        pendingConfirmationDeferred?.complete(false)
        val current = _currentTask.value ?: return
        val cancelled = current.copy(
            status = AgentTaskStatus.CANCELLED,
            completionTime = System.currentTimeMillis()
        )
        _currentTask.value = cancelled
        scope.launch { memoryRepository.saveAgentTask(cancelled) }
        addLog("Task cancelled by user.")
    }

    fun retryStep(stepNumber: Int) {
        val current = _currentTask.value ?: return
        val updatedSteps = current.steps.map {
            if (it.stepNumber >= stepNumber) {
                it.copy(status = AutonomousStepStatus.PENDING, actualResult = null, errorMessage = null)
            } else it
        }
        val reset = current.copy(
            steps = updatedSteps,
            currentStepIndex = stepNumber - 1,
            status = AgentTaskStatus.READY,
            errorInformation = null
        )
        _currentTask.value = reset
        scope.launch { memoryRepository.saveAgentTask(reset) }
        approveAndExecute()
    }

    fun loadTask(task: AgentTask) {
        executionJob?.cancel()
        _currentTask.value = task
        _liveLogs.value = task.logs
    }
}
