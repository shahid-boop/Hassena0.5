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
}
