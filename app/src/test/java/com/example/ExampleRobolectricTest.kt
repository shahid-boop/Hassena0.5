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
@Config(sdk = [34])
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
}
