package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.models.AIProviderType
import com.example.models.PendingConfirmationAction
import com.example.models.RoutingMode
import com.example.models.SupportedModelsCatalog
import com.example.models.TaskCategory
import com.example.services.ai.AIModelRouter
import com.example.services.ai.LocalCommandClassifier
import com.example.services.android.AndroidControlService
import com.example.services.android.AppLauncherService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@ConscryptMode(ConscryptMode.Mode.OFF)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ExampleRobolectricTest {

    @Test
    fun `read app name from context matches HASASENA A1`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HASASENA A1", appName)
    }

    @Test
    fun `local command classifier handles battery and brightness and confirmation without cloud AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val classifier = LocalCommandClassifier(context, control)

        // 1. Battery status command
        val batteryRes = classifier.tryExecuteLocalCommand("Show battery")
        assertTrue(batteryRes.matched)
        assertTrue(batteryRes.spokenResponse.contains("Battery is at"))

        // 2. Brightness command
        val brightRes = classifier.tryExecuteLocalCommand("Set brightness to 50")
        assertTrue(brightRes.matched)
        assertTrue(brightRes.spokenResponse.contains("50 percent"))

        // 3. Phone call requires confirmation
        val callRes = classifier.tryExecuteLocalCommand("Call 5551234567")
        assertTrue(callRes.matched)
        assertTrue(callRes.requiresConfirmation)
        assertTrue(callRes.pendingConfirmation is PendingConfirmationAction.PlacePhoneCall)

        // 4. Complex natural language question does not match local command engine
        val aiRes = classifier.tryExecuteLocalCommand("Explain how quantum entanglement works in detail")
        assertFalse(aiRes.matched)
    }

    @Test
    fun `catalog has unique provider model pairs and current requested model IDs`() {
        val models = SupportedModelsCatalog.models
        assertEquals(models.size, models.map { it.provider to it.modelId }.toSet().size)

        val kimi = models.single { it.displayName.startsWith("Kimi K2.6") }
        assertEquals(AIProviderType.OPENROUTER, kimi.provider)
        assertEquals("moonshotai/kimi-k2.6", kimi.modelId)
        assertTrue(kimi.badge.contains("PAID"))

        val nemotron = models.single { it.displayName.startsWith("NVIDIA Nemotron 3.5 Lightning") }
        assertEquals(AIProviderType.NVIDIA_NIM, nemotron.provider)
        assertEquals("nvidia/nemotron-3.5-lightning-30b-a3b", nemotron.modelId)

        assertTrue(models.any { it.provider == AIProviderType.GEMINI && it.modelId == "gemini-3.8-flash" })
        assertTrue(models.any { it.provider == AIProviderType.GROQ && it.modelId == "openai/gpt-oss-120b" })
        assertTrue(models.any { it.provider == AIProviderType.GROQ && it.modelId == "openai/gpt-oss-20b" })
        assertTrue(models.any { it.provider == AIProviderType.GROQ && it.modelId == "qwen/qwen3.8-27b" })
    }

    @Test
    fun `smart model router selects appropriate model tier`() {
        val router = AIModelRouter()

        // Simple QA -> Fast tier
        val simpleRoute = router.routeRequest(
            prompt = "What is the capital of France?",
            hasImageAttachment = false,
            forceResearch = false,
            routingMode = RoutingMode.AUTO_SMART,
            preferredProvider = AIProviderType.GEMINI,
            preferredModelId = "gemini-3.5-flash-lite",
            isProviderConfigured = { true }
        )
        assertEquals(TaskCategory.SIMPLE_QA, simpleRoute.category)
        assertEquals("gemini-3.5-flash-lite", simpleRoute.modelId)

        // Complex Reasoning -> Pro tier
        val complexRoute = router.routeRequest(
            prompt = "Compare and contrast distributed consensus algorithms Raft and Paxos step by step",
            hasImageAttachment = false,
            forceResearch = false,
            routingMode = RoutingMode.AUTO_SMART,
            preferredProvider = AIProviderType.GEMINI,
            preferredModelId = "gemini-3.8-flash",
            isProviderConfigured = { true }
        )
        assertEquals(TaskCategory.COMPLEX_REASONING, complexRoute.category)
        assertEquals("gemini-3.8-flash", complexRoute.modelId)
    }

    @Test
    fun `cloud sync repository operates completely offline without firebase`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.services.cloud.CloudSyncRepository(context)
        val createResult = repo.createCloudMemory("FACT", "Autonomous offline intelligence", "Robolectric")
        assertTrue(createResult.isSuccess)
        val memories = repo.getUserCloudMemories().getOrThrow()
        assertTrue(memories.isEmpty())
    }

    @Test
    fun `autonomous agent tool registry contains all required tools and enforces sensitive permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService
        )

        val toolIds = registry.toolSpecs.map { it.toolId }.toSet()
        val expectedTools = setOf(
            "web_search", "deep_research", "search_knowledge", "save_memory",
            "get_device_telemetry", "set_flashlight", "set_volume", "set_brightness",
            "launch_app", "open_url", "list_installed_apps", "read_active_notifications",
            "set_timer_alarm", "place_phone_call", "compose_sms", "synthesize_report"
        )
        assertTrue(toolIds.containsAll(expectedTools))

        // Check sensitive actions require confirmation
        val callTool = registry.getToolSpec("place_phone_call")
        assertTrue(callTool != null && callTool.requiresConfirmation)

        val smsTool = registry.getToolSpec("compose_sms")
        assertTrue(smsTool != null && smsTool.requiresConfirmation)

        val teleTool = registry.getToolSpec("get_device_telemetry")
        assertTrue(teleTool != null && !teleTool.requiresConfirmation)
    }

    @Test
    fun `autonomous agent real tool execution returns real telemetry and handles controls`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService
        )

        // Test telemetry execution
        val teleRes = registry.executeTool("get_device_telemetry", emptyMap()) {}
        assertTrue(teleRes.success)
        assertTrue(teleRes.output.contains("Battery Level:"))
        assertTrue(teleRes.output.contains("RAM:"))

        // Test volume execution
        val volRes = registry.executeTool("set_volume", mapOf("percent" to "70")) {}
        assertTrue(volRes.success)

        // Test brightness execution
        val brightRes = registry.executeTool("set_brightness", mapOf("percent" to "80")) {}
        assertTrue(brightRes.success)

        // Test knowledge save and search
        val saveRes = registry.executeTool(
            "save_memory",
            mapOf("category" to "PREFERENCE", "content" to "Autonomous agent test memory item")
        ) {}
        assertTrue(saveRes.success)

        val searchRes = registry.executeTool("search_knowledge", mapOf("query" to "Autonomous")) {}
        assertTrue(searchRes.success)
    }

    @Test
    fun `autonomous agent task persistence in SQLite database`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())

        val step1 = com.example.models.AgentStep(
            id = "test_task_step_1",
            stepNumber = 1,
            title = "Read Telemetry",
            description = "Read battery and storage",
            toolId = "get_device_telemetry",
            status = com.example.models.AutonomousStepStatus.COMPLETED,
            actualResult = "Battery: 100%"
        )
        val task = com.example.models.AgentTask(
            id = "test_task_001",
            title = "Test System Health Audit",
            userObjective = "Check system telemetry",
            executionPlanOverview = "Read telemetry step by step",
            steps = listOf(step1),
            status = com.example.models.AgentTaskStatus.COMPLETED,
            finalResponse = "Audit complete. All systems nominal."
        )

        memory.saveAgentTask(task)

        val retrieved = memory.getAgentTask("test_task_001")
        assertTrue(retrieved != null)
        assertEquals("test_task_001", retrieved?.id)
        assertEquals("Test System Health Audit", retrieved?.title)
        assertEquals(1, retrieved?.steps?.size)
        assertEquals("Read Telemetry", retrieved?.steps?.first()?.title)
        assertEquals(com.example.models.AutonomousStepStatus.COMPLETED, retrieved?.steps?.first()?.status)

        // Delete task
        memory.deleteAgentTask("test_task_001")
        val afterDelete = memory.getAgentTask("test_task_001")
        assertTrue(afterDelete == null)
    }

    @Test
    fun `self-healing credential scrubber redacts sensitive API keys and tokens`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val manager = com.example.services.healing.SelfHealingManager(context, memory)

        val rawText = "Failed calling endpoint with key sk-abcdef1234567890abcdef1234567890 and Google AIzaSyD1234567890abcdefghijklmnopqrstuvw"
        val scrubbed = manager.scrubSensitiveData(rawText)

        assertFalse(scrubbed.contains("sk-abcdef1234567890abcdef1234567890"))
        assertFalse(scrubbed.contains("AIzaSyD1234567890abcdefghijklmnopqrstuvw"))
        assertTrue(scrubbed.contains("[REDACTED_SECRET]"))
    }

    @Test
    fun `self-healing executeWithRetry bounded retries and recovery`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val manager = com.example.services.healing.SelfHealingManager(context, memory)

        var attempts = 0
        val result = manager.executeWithRetry<String>(
            maxRetries = 3,
            initialDelayMs = 10L,
            backoffFactor = 1.5,
            component = "TestService",
            operation = "FetchData"
        ) { attempt ->
            attempts = attempt
            if (attempt < 2) {
                throw java.net.SocketTimeoutException("Simulated temporary timeout")
            }
            "Success on attempt $attempt"
        }

        assertTrue(result.isSuccess)
        assertEquals(2, attempts)
        assertEquals("Success on attempt 2", result.getOrNull())
    }

    @Test
    fun `self-healing error recording and SQLite persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val manager = com.example.services.healing.SelfHealingManager(context, memory)

        val record = manager.recordError(
            category = com.example.models.ErrorCategory.COMPILATION_ERROR,
            message = "Unresolved symbol 'foo' with key sk-123456789012345678901234",
            affectedComponent = "CompilerService",
            operation = "BuildArtifact",
            severity = com.example.models.ErrorSeverity.HIGH
        )

        val retrieved = memory.getErrorRecord(record.id)
        assertTrue(retrieved != null)
        assertEquals(record.id, retrieved?.id)
        assertEquals(com.example.models.ErrorCategory.COMPILATION_ERROR, retrieved?.category)
        assertFalse(retrieved?.message?.contains("sk-123456789012345678901234") == true)
        assertTrue(retrieved?.message?.contains("[REDACTED_SECRET]") == true)

        // Clear resolved errors test
        manager.clearResolvedErrors()
        // Unresolved remains
        val stillExists = memory.getErrorRecord(record.id)
        assertTrue(stillExists != null)
    }

    @Test
    fun `self-healing patch approval verification and automatic rollback`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val manager = com.example.services.healing.SelfHealingManager(context, memory)

        val error = manager.recordError(
            category = com.example.models.ErrorCategory.COMPILATION_ERROR,
            message = "Syntax error in TargetClass.kt",
            affectedComponent = "CodingStudio",
            operation = "Compile"
        )

        val initialCode = "val apiKey = \"\"\nfun run() = true"
        val proposedRepair = manager.proposeCodeRepair(
            errorRecord = error,
            targetFile = "TargetClass.kt",
            sourceCode = initialCode
        )

        assertEquals(com.example.models.RepairState.WAITING_FOR_APPROVAL, proposedRepair.state)
        assertEquals("PENDING", proposedRepair.userApprovalStatus)
        assertTrue(proposedRepair.proposedPatch != null)

        // Scenario 1: Verification FAILS -> Auto-Rollback to snapshot
        val failedRepair = manager.approveAndApplyPatch(
            repairId = proposedRepair.id,
            fileWriter = { _, _ -> true },
            verifier = { _ -> Pair(false, "Compilation error: syntax invalid") }
        )

        assertEquals(com.example.models.RepairState.ROLLED_BACK, failedRepair.state)
        assertTrue(failedRepair.verificationOutput?.contains("Verification Failed") == true)

        // Scenario 2: Successful verification -> RECOVERED
        val error2 = manager.recordError(
            category = com.example.models.ErrorCategory.MISSING_API_KEY,
            message = "Missing key for Gemini",
            affectedComponent = "AIService",
            operation = "Inference"
        )
        val repair2 = manager.proposeCodeRepair(
            errorRecord = error2,
            targetFile = "Service.kt",
            sourceCode = "val apiKey = \"\""
        )
        val successRepair = manager.approveAndApplyPatch(
            repairId = repair2.id,
            fileWriter = { _, _ -> true },
            verifier = { _ -> Pair(true, "All 42 tests passed.") }
        )
        assertEquals(com.example.models.RepairState.RECOVERED, successRepair.state)
    }

    @Test
    fun `six-in-one agent tool registry covers all 6 domains with proper specifications`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService
        )

        val specs = registry.toolSpecs

        // Verify each domain has registered tools
        val codingTools = specs.filter { it.domain == com.example.models.AgentToolDomain.AI_CODING }
        val buildTools = specs.filter { it.domain == com.example.models.AgentToolDomain.BUILD_TEST }
        val webTools = specs.filter { it.domain == com.example.models.AgentToolDomain.BROWSER_WEB }
        val phoneTools = specs.filter { it.domain == com.example.models.AgentToolDomain.PHONE_ACTIONS }
        val memoryTools = specs.filter { it.domain == com.example.models.AgentToolDomain.PERSISTENT_MEMORY }
        val brainTools = specs.filter { it.domain == com.example.models.AgentToolDomain.AI_BRAIN }

        assertTrue("Tool 1: Coding tools count >= 4", codingTools.size >= 4)
        assertTrue("Tool 2: Build tools count >= 3", buildTools.size >= 3)
        assertTrue("Tool 3: Web tools count >= 4", webTools.size >= 4)
        assertTrue("Tool 4: Phone tools count >= 8", phoneTools.size >= 8)
        assertTrue("Tool 5: Memory tools count >= 3", memoryTools.size >= 3)
        assertTrue("Tool 6: Brain tools count >= 1", brainTools.size >= 1)

        // Verify write_project_file requires confirmation
        val writeSpec = registry.getToolSpec("write_project_file")
        assertTrue(writeSpec != null && writeSpec.requiresConfirmation)

        // Verify rollback_file_checkpoint requires confirmation
        val rollbackSpec = registry.getToolSpec("rollback_file_checkpoint")
        assertTrue(rollbackSpec != null && rollbackSpec.requiresConfirmation)
    }

    @Test
    fun `tool 1 - coding file operations create checkpoints and track modified files`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val codingEngine = com.example.services.ai.AutonomousCodingEngine(aiService)

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService,
            codingEngine = codingEngine
        )

        // Test writing a project file
        val testFileName = "test_script.kt"
        java.io.File(context.filesDir, testFileName).delete()
        codingEngine.clearCheckpoints()

        val initialContent = "fun hello() = println(\"Hasseena A1\")"
        val writeRes = registry.executeTool(
            "write_project_file",
            mapOf("file_path" to testFileName, "content" to initialContent, "reason" to "Initial write")
        ) {}

        assertTrue(writeRes.success)
        assertTrue(writeRes.modifiedFiles.isNotEmpty())

        // Test reading the written file
        val readRes = registry.executeTool("read_project_file", mapOf("file_path" to testFileName)) {}
        assertTrue(readRes.success)
        assertTrue(readRes.output.contains("Hasseena A1"))

        // Modify file and verify checkpoint creation
        val updatedContent = "fun hello() = println(\"Hasseena A1 Mission Control\")"
        val updateRes = registry.executeTool(
            "write_project_file",
            mapOf("file_path" to testFileName, "content" to updatedContent, "reason" to "Update text")
        ) {}
        assertTrue(updateRes.success)

        val checkpoints = codingEngine.checkpoints.value
        assertTrue("Checkpoints should be created", checkpoints.isNotEmpty())
        assertEquals("test_script.kt", java.io.File(checkpoints.first().file).name)
        assertTrue(checkpoints.first().code.contains("Hasseena A1"))

        // Test rollback
        val rollbackRes = registry.executeTool(
            "rollback_file_checkpoint",
            mapOf("checkpoint_id" to checkpoints.first().id)
        ) {}
        assertTrue(rollbackRes.success)

        val readAfterRollback = registry.executeTool("read_project_file", mapOf("file_path" to testFileName)) {}
        assertTrue(readAfterRollback.output.contains("fun hello() = println(\"Hasseena A1\")"))

        // Test move_project_file
        val destFileName = "test_script_renamed.kt"
        java.io.File(context.filesDir, destFileName).delete()
        val moveRes = registry.executeTool(
            "move_project_file",
            mapOf("source_path" to testFileName, "destination_path" to destFileName)
        ) {}
        assertTrue(moveRes.success)
        assertTrue(java.io.File(context.filesDir, destFileName).exists())

        // Test analyze_code_diagnostics
        val diagRes = registry.executeTool("analyze_code_diagnostics", mapOf("file_path" to destFileName)) {}
        assertTrue(diagRes.success)
        assertTrue(diagRes.output.contains("Diagnostics"))
    }

    @Test
    fun `tool 2 - real build and test executor validates project integrity and parses errors`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())
        val healing = com.example.services.healing.SelfHealingManager(context, memory, aiService)

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService,
            selfHealingManager = healing
        )

        // 1. Run build verification check
        val buildRes = registry.executeTool("run_build_check", mapOf("component" to "app")) {}
        assertTrue(buildRes.success)
        assertTrue(buildRes.output.contains("HASSEENA BUILD VERIFICATION REPORT"))

        // 2. Run unit test suite
        val testRes = registry.executeTool("run_unit_tests", mapOf("test_target" to "ExampleRobolectricTest")) {}
        assertTrue(testRes.success)
        assertTrue(testRes.output.contains("HASSEENA TEST EXECUTOR RESULTS"))
        assertTrue(testRes.output.contains("VERIFIED"))

        // 3. Parse compiler log with errors -> auto-routes to Self-Healing
        val compilerLog = "e: /app/src/main/Test.kt: (42, 12): Unresolved reference: BuildConfig.API_KEY"
        val parseRes = registry.executeTool("parse_compiler_log", mapOf("log_text" to compilerLog)) {}
        assertTrue(parseRes.success)
        assertTrue(parseRes.output.contains("Unresolved reference"))

        // 4. Verify APK artifact tool
        val apkRes = registry.executeTool("verify_apk_artifact", mapOf("build_type" to "debug")) {}
        assertTrue(apkRes.output.contains("APK") || apkRes.output.contains("com.aistudio.applet.ztvtvv"))

        // Verify error was dispatched to Self-Healing memory store
        val recordedErrors = memory.getErrorRecords()
        assertTrue(recordedErrors.any { it.message.contains("Unresolved reference") || it.affectedComponent.contains("BuildExecutor") })
    }

    @Test
    fun `tool 4 & 5 - phone accessibility and persistent queue management`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = AppLauncherService(context)
        val control = AndroidControlService(context, launcher)
        val keyStore = com.example.services.ai.ApiKeyStore(context)
        val aiService = com.example.services.ai.AIService(control, keyStore)
        val research = com.example.services.search.WebResearchService(aiService)
        val db = com.example.services.memory.HasseenaDatabase.getInstance(context)
        val memory = com.example.services.memory.MemoryRepository(db.hasseenaDao())

        val registry = com.example.services.agent.AgentToolRegistry(
            context = context,
            androidControlService = control,
            appLauncherService = launcher,
            webResearchService = research,
            memoryRepository = memory,
            aiService = aiService
        )

        // Test manage_mission_queue
        val queueRes = registry.executeTool("manage_mission_queue", mapOf("action" to "LIST")) {}
        assertTrue(queueRes.success)
        assertTrue(queueRes.output.contains("Persistent Mission Queue"))

        // Test accessibility tools gracefully report service status when inactive
        val screenRes = registry.executeTool("accessibility_read_screen", emptyMap()) {}
        assertFalse(screenRes.success)
        assertTrue(screenRes.output.contains("Accessibility Service is not active"))

        val clickRes = registry.executeTool("accessibility_click", mapOf("label" to "Next")) {}
        assertFalse(clickRes.success)

        val inputRes = registry.executeTool("accessibility_input_text", mapOf("label" to "Search", "text" to "Query")) {}
        assertFalse(inputRes.success)
    }
}

