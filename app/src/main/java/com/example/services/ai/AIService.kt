package com.example.services.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.models.AIProviderType
import com.example.models.AssistantPersonality
import com.example.services.android.AndroidControlService
import com.example.services.memory.ChatMessageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float? = 0.6f,
    val topP: Float? = 0.92f,
    val topK: Int? = 40
)

class AIService(
    private val controlService: AndroidControlService,
    private val apiKeyStore: ApiKeyStore? = null
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Omit absent optional Gemini fields instead of serializing JSON nulls.
        explicitNulls = false
    }

    // Shared high-performance OkHttpClient with HTTP/2 & keep-alive connection reuse
    val sharedHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(6, 5, TimeUnit.MINUTES))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun isProviderKeyAvailable(provider: AIProviderType): Boolean {
        val key = getApiKeyForProvider(provider)
        return key.isNotBlank() && !key.startsWith("MY_")
    }

    fun getApiKeyForProvider(provider: AIProviderType): String {
        val savedKey = runCatching { apiKeyStore?.get(provider).orEmpty() }
            .getOrDefault("")
            .trim()

        if (savedKey.isNotBlank() && !savedKey.startsWith("MY_")) {
            return savedKey
        }

        val rawKey: String = when (provider) {
            AIProviderType.GEMINI -> runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
            AIProviderType.GROQ -> runCatching { BuildConfig.GROQ_API_KEY }.getOrDefault("")
            AIProviderType.OPENAI -> runCatching { BuildConfig.OPENAI_API_KEY }.getOrDefault("")
            AIProviderType.CLAUDE -> runCatching { BuildConfig.ANTHROPIC_API_KEY }.getOrDefault("")
            AIProviderType.PERPLEXITY -> runCatching { BuildConfig.PERPLEXITY_API_KEY }.getOrDefault("")
            AIProviderType.META -> ""
            AIProviderType.OPENROUTER -> ""
            AIProviderType.NVIDIA_NIM -> ""
            AIProviderType.MISTRAL -> ""
        }.trim()
        return if (rawKey.startsWith("MY_")) "" else rawKey
    }


    suspend fun testApiKey(provider: AIProviderType, key: String): String =
        withContext(Dispatchers.IO) {
            if (key.isBlank() || key.startsWith("MY_")) {
                return@withContext "Missing API key"
            }

            try {
                val request = when (provider) {
                    AIProviderType.GEMINI -> {
                        val encodedKey = java.net.URLEncoder.encode(key, "UTF-8")
                        Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$encodedKey")
                            .get()
                            .build()
                    }

                    AIProviderType.GROQ ->
                        Request.Builder()
                            .url("https://api.groq.com/openai/v1/models")
                            .header("Authorization", "Bearer $key")
                            .get()
                            .build()

                    AIProviderType.OPENAI ->
                        Request.Builder()
                            .url("https://api.openai.com/v1/models")
                            .header("Authorization", "Bearer $key")
                            .get()
                            .build()

                    AIProviderType.CLAUDE ->
                        Request.Builder()
                            .url("https://api.anthropic.com/v1/models")
                            .header("x-api-key", key)
                            .header("anthropic-version", "2023-06-01")
                            .get()
                            .build()

                    AIProviderType.OPENROUTER ->
                        Request.Builder()
                            .url("https://openrouter.ai/api/v1/models")
                            .header("Authorization", "Bearer $key")
                            .get()
                            .build()

                    AIProviderType.NVIDIA_NIM ->
                        Request.Builder()
                            .url("https://integrate.api.nvidia.com/v1/models")
                            .header("Authorization", "Bearer $key")
                            .get()
                            .build()

                    AIProviderType.MISTRAL ->
                        Request.Builder()
                            .url("https://api.mistral.ai/v1/models")
                            .header("Authorization", "Bearer $key")
                            .get()
                            .build()

                    AIProviderType.META -> {
                        val body = """{"model":"muse-spark-1.3","messages":[{"role":"user","content":"Reply OK"}]}"""
                            .toRequestBody("application/json".toMediaType())
                        Request.Builder()
                            .url("https://api.meta.ai/v1/chat/completions")
                            .header("Authorization", "Bearer $key")
                            .post(body)
                            .build()
                    }
                    AIProviderType.PERPLEXITY -> {
                        val body = """{"model":"sonar","messages":[{"role":"user","content":"Reply OK"}],"max_tokens":1}"""
                            .toRequestBody("application/json".toMediaType())
                        Request.Builder()
                            .url("https://api.perplexity.ai/chat/completions")
                            .header("Authorization", "Bearer $key")
                            .post(body)
                            .build()
                    }
                }

                sharedHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> "Connected"
                        response.code == 401 || response.code == 403 ->
                            "Invalid key or access denied (HTTP ${response.code})"
                        response.code == 429 ->
                            "Key recognized, but rate limit or quota reached (HTTP 429)"
                        else -> "Provider returned HTTP ${response.code}"
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                "Network error: check your internet connection"
            } catch (e: Exception) {
                "Test failed: ${e.javaClass.simpleName}"
            }
        }

    data class StreamCompletionOutcome(
        val fullText: String,
        val firstTokenLatencyMs: Long,
        val totalLatencyMs: Long,
        val executedTools: List<String> = emptyList(),
        val isError: Boolean = false,
        val errorTitle: String? = null,
        val recoveryAction: String? = null
    )

    suspend fun generatePureTextResponse(
        provider: AIProviderType,
        modelId: String,
        systemPrompt: String,
        userPrompt: String
    ): StreamCompletionOutcome = withContext(Dispatchers.IO) {
        streamResponse(
            provider = provider,
            modelId = modelId,
            prompt = userPrompt,
            history = emptyList(),
            personality = AssistantPersonality.TECHNICAL_EXPERT,
            memoryContextPrompt = "",
            imageBitmap = null,
            executeToolDirectives = false,
            customSystemPrompt = systemPrompt,
            onChunk = { _, _, _ -> }
        )
    }

    suspend fun streamResponse(
        provider: AIProviderType,
        modelId: String,
        prompt: String,
        history: List<ChatMessageEntity>,
        personality: AssistantPersonality,
        memoryContextPrompt: String,
        imageBitmap: Bitmap? = null,
        executeToolDirectives: Boolean = true,
        customSystemPrompt: String? = null,
        onChunk: suspend (deltaText: String, accumulatedText: String, firstTokenLatencyMs: Long) -> Unit
    ): StreamCompletionOutcome = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        var firstTokenMs = -1L

        // Respect the provider/model selected by the user. Silently switching to
        // Gemini here can send a model ID to the wrong API and hide a missing key.
        // Automatic fallback belongs in AIModelRouter, where the selected route is known.
        val effectiveProvider = provider
        val effectiveModel = modelId
        val apiKey = getApiKeyForProvider(effectiveProvider)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext StreamCompletionOutcome(
                fullText = "API Key is not configured for ${effectiveProvider.displayName}. Please add this provider's API key in JARVIS Settings → API Key Manager.",
                firstTokenLatencyMs = 0L,
                totalLatencyMs = System.currentTimeMillis() - startMs,
                isError = true,
                errorTitle = "Missing ${effectiveProvider.displayName} API Key",
                recoveryAction = "Open JARVIS Settings → API Key Manager and save the selected provider key. Local device commands may still work offline where permissions allow."
            )
        }

        val systemInstructionText = customSystemPrompt ?: buildSystemPrompt(personality, memoryContextPrompt, includeToolDirectives = executeToolDirectives)

        var lastException: Exception? = null
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            try {
                val accumulated = StringBuilder()
                val chunkCallback: suspend (String) -> Unit = { delta ->
                    if (delta.isNotEmpty()) {
                        if (firstTokenMs < 0L) {
                            firstTokenMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                        }
                        accumulated.append(delta)
                        val cleanSoFar = stripToolTags(accumulated.toString())
                        onChunk(delta, cleanSoFar, firstTokenMs)
                    }
                }

                when (effectiveProvider) {
                    AIProviderType.GEMINI -> streamGeminiSse(
                        apiKey = apiKey,
                        modelId = effectiveModel,
                        prompt = prompt,
                        history = history,
                        systemPrompt = systemInstructionText,
                        imageBitmap = imageBitmap,
                        onDelta = chunkCallback
                    )
                    AIProviderType.GROQ, AIProviderType.OPENAI, AIProviderType.PERPLEXITY, AIProviderType.META, AIProviderType.OPENROUTER, AIProviderType.NVIDIA_NIM, AIProviderType.MISTRAL -> streamOpenAiCompatibleSse(
                        provider = effectiveProvider,
                        apiKey = apiKey,
                        modelId = effectiveModel,
                        prompt = prompt,
                        history = history,
                        systemPrompt = systemInstructionText,
                        onDelta = chunkCallback
                    )
                    AIProviderType.CLAUDE -> streamClaudeSse(
                        apiKey = apiKey,
                        modelId = effectiveModel,
                        prompt = prompt,
                        history = history,
                        systemPrompt = systemInstructionText,
                        onDelta = chunkCallback
                    )
                }

                val rawFull = accumulated.toString().trim()
                val executedTools = if (executeToolDirectives) executeEmbeddedToolDirectives(rawFull) else emptyList()
                val cleanFinalText = if (executeToolDirectives) {
                    stripToolTags(rawFull).ifBlank {
                        if (executedTools.isNotEmpty()) executedTools.joinToString(" ") else "Done."
                    }
                } else {
                    rawFull
                }
                val totalMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)

                return@withContext StreamCompletionOutcome(
                    fullText = cleanFinalText,
                    firstTokenLatencyMs = if (firstTokenMs > 0) firstTokenMs else totalMs,
                    totalLatencyMs = totalMs,
                    executedTools = executedTools,
                    isError = false
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                lastException = e
                val isTransient = e is SocketTimeoutException || (e.message?.contains("503") == true) || (e.message?.contains("502") == true)
                if (attempt < maxAttempts && isTransient) {
                    delay(180L * attempt)
                    continue
                } else {
                    break
                }
            }
        }

        val mappedError = classifyException(lastException, effectiveProvider)
        return@withContext StreamCompletionOutcome(
            fullText = mappedError.userMessage,
            firstTokenLatencyMs = 0L,
            totalLatencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L),
            isError = true,
            errorTitle = mappedError.title,
            recoveryAction = mappedError.recoveryHint
        )
    }

    private suspend fun streamGeminiSse(
        apiKey: String,
        modelId: String,
        prompt: String,
        history: List<ChatMessageEntity>,
        systemPrompt: String,
        imageBitmap: Bitmap?,
        onDelta: suspend (String) -> Unit
    ) {
        val contents = mutableListOf<GeminiContent>()

        if (imageBitmap == null) {
            history.takeLast(6).forEach { msg ->
                if (msg.content.isNotBlank() && !msg.isError) {
                    val geminiRole = if (msg.role == "user") "user" else "model"
                    contents.add(
                        GeminiContent(
                            role = geminiRole,
                            parts = listOf(GeminiPart(text = msg.content.take(1200)))
                        )
                    )
                }
            }
        }

        val currentParts = mutableListOf<GeminiPart>()
        currentParts.add(GeminiPart(text = prompt))
        if (imageBitmap != null) {
            currentParts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = bitmapToBase64Jpeg(imageBitmap)
                    )
                )
            )
        }
        contents.add(GeminiContent(role = "user", parts = currentParts))

        val requestPayload = GenerateContentRequest(
            contents = contents,
            // Gemini 3.x models no longer accept the legacy temperature/topP/topK fields.
            generationConfig = if (modelId.startsWith("gemini-3.")) null else GeminiGenerationConfig(temperature = 0.6f),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        val requestBodyString = json.encodeToString(GenerateContentRequest.serializer(), requestPayload)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:streamGenerateContent?alt=sse&key=$apiKey"

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBodyString.toRequestBody("application/json".toMediaType()))
            .header("Accept", "text/event-stream")
            .build()

        val call = sharedHttpClient.newCall(httpRequest)
        val response = call.execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                val errBody = resp.body?.string()?.take(300) ?: "HTTP ${resp.code}"
                throw IOException("HTTP ${resp.code}: $errBody")
            }

            val body = resp.body ?: throw IOException("Empty streaming response body")
            body.byteStream().bufferedReader().use { reader ->
                var line: String? = null
                while (currentCoroutineContext().isActive && reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (!rawLine.startsWith("data:")) continue
                    val jsonPayload = rawLine.removePrefix("data:").trim()
                    if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

                    val chunkObj = runCatching { json.parseToJsonElement(jsonPayload).jsonObject }.getOrNull() ?: continue
                    val parts = chunkObj["candidates"]?.jsonArray
                        ?.getOrNull(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray

                    val textDelta = parts?.mapNotNull { partEl ->
                        partEl.jsonObject["text"]?.jsonPrimitive?.content
                    }?.joinToString("")

                    if (!textDelta.isNullOrEmpty()) {
                        onDelta(textDelta)
                    }
                }
            }
        }
    }

    private suspend fun streamOpenAiCompatibleSse(
        provider: AIProviderType,
        apiKey: String,
        modelId: String,
        prompt: String,
        history: List<ChatMessageEntity>,
        systemPrompt: String,
        onDelta: suspend (String) -> Unit
    ) {
        val endpoint = when (provider) {
            AIProviderType.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AIProviderType.OPENAI -> "https://api.openai.com/v1/chat/completions"
            AIProviderType.META -> "https://api.meta.ai/v1/chat/completions"
            AIProviderType.PERPLEXITY -> "https://api.perplexity.ai/chat/completions"
            AIProviderType.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AIProviderType.NVIDIA_NIM -> "https://integrate.api.nvidia.com/v1/chat/completions"
            AIProviderType.MISTRAL -> "https://api.mistral.ai/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }

        val payload = buildJsonObject {
            put("model", modelId)
            put("stream", true)
            put("temperature", 0.6)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                history.takeLast(6).forEach { msg ->
                    if (msg.content.isNotBlank() && !msg.isError) {
                        add(buildJsonObject {
                            put("role", if (msg.role == "user") "user" else "assistant")
                            put("content", msg.content.take(1000))
                        })
                    }
                }
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            }
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        sharedHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${resp.body?.string()?.take(250)}")
            }
            val body = resp.body ?: throw IOException("Empty stream")
            body.byteStream().bufferedReader().use { reader ->
                var line: String? = null
                while (currentCoroutineContext().isActive && reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (!trimmed.startsWith("data:")) continue
                    val dataStr = trimmed.removePrefix("data:").trim()
                    if (dataStr == "[DONE]" || dataStr.isEmpty()) continue
                    val obj = runCatching { json.parseToJsonElement(dataStr).jsonObject }.getOrNull() ?: continue
                    val delta = obj["choices"]?.jsonArray
                        ?.getOrNull(0)?.jsonObject
                        ?.get("delta")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.content
                    if (!delta.isNullOrEmpty()) {
                        onDelta(delta)
                    }
                }
            }
        }
    }

    private suspend fun streamClaudeSse(
        apiKey: String,
        modelId: String,
        prompt: String,
        history: List<ChatMessageEntity>,
        systemPrompt: String,
        onDelta: suspend (String) -> Unit
    ) {
        val payload = buildJsonObject {
            put("model", modelId)
            put("max_tokens", 1024)
            put("stream", true)
            put("system", systemPrompt)
            putJsonArray("messages") {
                history.takeLast(6).forEach { msg ->
                    if (msg.content.isNotBlank() && !msg.isError) {
                        add(buildJsonObject {
                            put("role", if (msg.role == "user") "user" else "assistant")
                            put("content", msg.content.take(1000))
                        })
                    }
                }
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            }
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        sharedHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${resp.body?.string()?.take(250)}")
            }
            val body = resp.body ?: throw IOException("Empty stream")
            body.byteStream().bufferedReader().use { reader ->
                var line: String? = null
                while (currentCoroutineContext().isActive && reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: continue
                    if (!trimmed.startsWith("data:")) continue
                    val dataStr = trimmed.removePrefix("data:").trim()
                    if (dataStr.isEmpty()) continue
                    val obj = runCatching { json.parseToJsonElement(dataStr).jsonObject }.getOrNull() ?: continue
                    val deltaText = obj["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                    if (!deltaText.isNullOrEmpty()) {
                        onDelta(deltaText)
                    }
                }
            }
        }
    }

    private fun buildSystemPrompt(
        personality: AssistantPersonality,
        memoryContextPrompt: String,
        includeToolDirectives: Boolean = true
    ): String {
        val telemetry = controlService.getDeviceTelemetry()
        return buildString {
            append(personality.systemInstructionPrompt)
            append("\n\n[LIVE ANDROID DEVICE TELEMETRY]\n")
            append("- Device: ${telemetry.deviceModel} (${telemetry.androidVersion})\n")
            append("- Battery: ${telemetry.batteryPercent}% (Charging: ${telemetry.isCharging})\n")
            append("- Network: ${telemetry.networkTypeLabel}\n")
            append("- Media Volume: ${telemetry.musicVolumePercent}% | Brightness: ${telemetry.screenBrightnessPercent}%\n")
            if (includeToolDirectives) {
                append("\n[REAL ANDROID TOOL TAGS]\n")
                append("If the user's request includes performing a supported hardware action alongside a question, you may append one of these tags at the very end of your response so JARVIS executes it immediately:\n")
                append("[TOOL:FLASHLIGHT_ON], [TOOL:FLASHLIGHT_OFF], [TOOL:VOLUME_UP], [TOOL:VOLUME_DOWN], [TOOL:OPEN_APP:AppName], [TOOL:BRIGHTNESS:50]\n")
            }
            if (memoryContextPrompt.isNotBlank()) {
                append(memoryContextPrompt)
            }
        }
    }

    private fun executeEmbeddedToolDirectives(rawResponse: String): List<String> {
        val executed = mutableListOf<String>()
        val regex = Regex("\\[TOOL:([A-Z_]+)(?::([^\\]]+))?\\]")
        regex.findAll(rawResponse).forEach { match ->
            val cmd = match.groupValues[1]
            val arg = match.groupValues.getOrNull(2)?.trim().orEmpty()
            when (cmd) {
                "FLASHLIGHT_ON" -> {
                    val res = controlService.setFlashlight(true)
                    executed.add(res.spokenText)
                }
                "FLASHLIGHT_OFF" -> {
                    val res = controlService.setFlashlight(false)
                    executed.add(res.spokenText)
                }
                "VOLUME_UP" -> {
                    val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.UP)
                    executed.add(res.spokenText)
                }
                "VOLUME_DOWN" -> {
                    val res = controlService.adjustVolume(AndroidControlService.VolumeDirection.DOWN)
                    executed.add(res.spokenText)
                }
                "BRIGHTNESS" -> {
                    val pct = arg.toIntOrNull() ?: 50
                    val res = controlService.setScreenBrightnessPercent(pct)
                    executed.add(res.spokenText)
                }
                "OPEN_APP" -> {
                    if (arg.isNotBlank()) {
                        val res = controlService.appLauncherService.launchAppByNameOrAlias(arg)
                        executed.add(res.spokenResponse)
                    }
                }
            }
        }
        return executed
    }

    private fun stripToolTags(text: String): String {
        return text.replace(Regex("\\[TOOL:[^\\]]*\\]"), "").trim()
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private data class MappedError(
        val title: String,
        val userMessage: String,
        val recoveryHint: String
    )

    private fun classifyException(e: Exception?, provider: AIProviderType): MappedError {
        val msg = e?.message ?: "Unknown error"
        return when {
            e is UnknownHostException -> MappedError(
                title = "No Internet Connection",
                userMessage = "No internet connection detected. Local device commands (flashlight, volume, battery, opening apps) remain available offline.",
                recoveryHint = "Check Wi-Fi or cellular data, or use on-device voice commands."
            )
            e is SocketTimeoutException -> MappedError(
                title = "Request Timed Out",
                userMessage = "${provider.displayName} took too long to respond.",
                recoveryHint = "Tap Retry or switch to Gemini 3.8 Flash or Gemini 3.5 Flash-Lite in Settings."
            )
            msg.contains("401") || msg.contains("403") || msg.contains("API_KEY_INVALID", ignoreCase = true) -> MappedError(
                title = "Invalid or Unauthorized API Key",
                userMessage = "${provider.displayName} rejected the API key (HTTP 401/403).",
                recoveryHint = "Verify the saved key in JARVIS Settings → API Key Manager, then test the key again or switch providers in Settings."
            )
            msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> MappedError(
                title = "Rate Limit Reached",
                userMessage = "${provider.displayName} rate limit or quota reached (HTTP 429).",
                recoveryHint = "Wait a few seconds or switch to another AI model in Settings."
            )
            else -> MappedError(
                title = "${provider.displayName} API Error",
                userMessage = "Could not complete cloud AI request: ${msg.take(140)}",
                recoveryHint = "Check network status or try another provider in Settings."
            )
        }
    }
}
