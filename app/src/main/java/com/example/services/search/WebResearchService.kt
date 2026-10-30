package com.example.services.search

import com.example.models.AIProviderType
import com.example.models.AssistantPersonality
import com.example.models.ResearchMode
import com.example.models.ResearchSource
import com.example.services.ai.AIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLEncoder

class WebResearchService(
    private val aiService: AIService
) {

    private val json = Json { ignoreUnknownKeys = true }

    data class ResearchExecutionResult(
        val query: String,
        val mode: ResearchMode,
        val synthesisText: String,
        val sources: List<ResearchSource>,
        val latencyMs: Long,
        val isError: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Executes real web search queries across Wikipedia, DuckDuckGo Instant Answers, and
     * Wikimedia Commons, then synthesizes findings with grounded source citations.
     * Never fabricates URLs or citations.
     */
    suspend fun executeResearch(
        query: String,
        mode: ResearchMode,
        preferredProvider: AIProviderType,
        onProgressStatus: (String) -> Unit,
        onStreamChunk: suspend (accumulatedText: String) -> Unit
    ): ResearchExecutionResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val trimmedQuery = query.trim()

        onProgressStatus(
            when (mode) {
                ResearchMode.FAST_SEARCH -> "Searching live web sources..."
                ResearchMode.DEEP_RESEARCH -> "Running parallel multi-query deep retrieval..."
                ResearchMode.IMAGE_CONCEPT_SEARCH -> "Searching Wikimedia & encyclopedia visual archives..."
            }
        )

        val retrievedSources = try {
            coroutineScope {
                when (mode) {
                    ResearchMode.FAST_SEARCH -> {
                        val wikiDeferred = async { fetchWikipediaSources(trimmedQuery, limit = 4) }
                        val ddgDeferred = async { fetchDuckDuckGoSources(trimmedQuery) }
                        (wikiDeferred.await() + ddgDeferred.await()).distinctBy { it.url }.take(6)
                    }
                    ResearchMode.DEEP_RESEARCH -> {
                        val subQueries = listOf(
                            trimmedQuery,
                            "$trimmedQuery overview architecture",
                            "$trimmedQuery key facts applications"
                        )
                        val jobs = subQueries.map { sq ->
                            async { fetchWikipediaSources(sq, limit = 4) }
                        } + async { fetchDuckDuckGoSources(trimmedQuery) }
                        jobs.awaitAll().flatten().distinctBy { it.url }.take(10)
                    }
                    ResearchMode.IMAGE_CONCEPT_SEARCH -> {
                        val commonsDeferred = async { fetchWikimediaCommonsSources(trimmedQuery, limit = 6) }
                        val wikiDeferred = async { fetchWikipediaSources(trimmedQuery, limit = 4) }
                        (commonsDeferred.await() + wikiDeferred.await()).distinctBy { it.url }.take(8)
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        if (retrievedSources.isEmpty() && !aiService.isProviderKeyAvailable(AIProviderType.PERPLEXITY)) {
            return@withContext ResearchExecutionResult(
                query = trimmedQuery,
                mode = mode,
                synthesisText = "Could not retrieve live web sources for \"$trimmedQuery\". Please check your internet connection and try again.",
                sources = emptyList(),
                latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L),
                isError = true,
                errorMessage = "No live web sources reachable"
            )
        }

        onProgressStatus("Synthesizing ${retrievedSources.size} verified sources...")

        val groundedPrompt = buildGroundedResearchPrompt(trimmedQuery, mode, retrievedSources)
        val modelId = if (mode == ResearchMode.DEEP_RESEARCH) {
            "gemini-3.1-pro-preview"
        } else {
            "gemini-3.8-flash"
        }

        val streamOutcome = aiService.streamResponse(
            provider = if (aiService.isProviderKeyAvailable(AIProviderType.PERPLEXITY)) AIProviderType.PERPLEXITY else preferredProvider,
            modelId = if (aiService.isProviderKeyAvailable(AIProviderType.PERPLEXITY)) "sonar-pro" else modelId,
            prompt = groundedPrompt,
            history = emptyList(),
            personality = AssistantPersonality.EXECUTIVE_PRO,
            memoryContextPrompt = "",
            imageBitmap = null,
            onChunk = { _, accumulated, _ ->
                onStreamChunk(accumulated)
            }
        )

        val finalSynthesis = if (streamOutcome.isError && retrievedSources.isNotEmpty()) {
            // Even if AI key is missing/rate-limited, display the real retrieved source summaries directly!
            buildString {
                append("### Retrieved Live Web Findings for \"$trimmedQuery\"\n\n")
                retrievedSources.forEachIndexed { idx, src ->
                    append("**[${idx + 1}] ${src.title}** (${src.sourceDomain})\n")
                    append("${src.snippet}\n\n")
                }
            }
        } else {
            streamOutcome.fullText
        }

        return@withContext ResearchExecutionResult(
            query = trimmedQuery,
            mode = mode,
            synthesisText = finalSynthesis,
            sources = retrievedSources,
            latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L),
            isError = streamOutcome.isError && retrievedSources.isEmpty(),
            errorMessage = if (streamOutcome.isError && retrievedSources.isEmpty()) streamOutcome.errorTitle else null
        )
    }

    private fun fetchWikipediaSources(query: String, limit: Int): List<ResearchSource> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&srlimit=$limit&format=json&utf8=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVISAI/2.0 (Android Voice Assistant)")
                .build()

            aiService.sharedHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val bodyStr = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val searchArray = root["query"]?.jsonObject?.get("search")?.jsonArray ?: return emptyList()

                searchArray.mapNotNull { el ->
                    val obj = el.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val rawSnippet = obj["snippet"]?.jsonPrimitive?.content.orEmpty()
                    val cleanSnippet = cleanHtmlEntities(rawSnippet)
                    val articleUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"
                    ResearchSource(
                        title = title,
                        url = articleUrl,
                        snippet = cleanSnippet,
                        sourceDomain = "en.wikipedia.org"
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchDuckDuckGoSources(query: String): List<ResearchSource> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVISAI/2.0 (Android Voice Assistant)")
                .build()

            aiService.sharedHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val bodyStr = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val results = mutableListOf<ResearchSource>()

                val abstractText = root["AbstractText"]?.jsonPrimitive?.content.orEmpty()
                val abstractUrl = root["AbstractURL"]?.jsonPrimitive?.content.orEmpty()
                val heading = root["Heading"]?.jsonPrimitive?.content.orEmpty()

                if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                    results.add(
                        ResearchSource(
                            title = heading.ifBlank { query },
                            url = abstractUrl,
                            snippet = abstractText,
                            sourceDomain = extractDomain(abstractUrl)
                        )
                    )
                }

                val relatedTopics = root["RelatedTopics"]?.jsonArray
                relatedTopics?.take(4)?.forEach { item ->
                    val obj = item.jsonObject
                    val text = obj["Text"]?.jsonPrimitive?.content.orEmpty()
                    val firstUrl = obj["FirstURL"]?.jsonPrimitive?.content.orEmpty()
                    if (text.isNotBlank() && firstUrl.isNotBlank()) {
                        results.add(
                            ResearchSource(
                                title = text.substringBefore(" - ").take(70),
                                url = firstUrl,
                                snippet = text,
                                sourceDomain = extractDomain(firstUrl)
                            )
                        )
                    }
                }
                results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchWikimediaCommonsSources(query: String, limit: Int): List<ResearchSource> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch=$encoded&srlimit=$limit&format=json&utf8=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVISAI/2.0 (Android Voice Assistant)")
                .build()

            aiService.sharedHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val bodyStr = resp.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val searchArray = root["query"]?.jsonObject?.get("search")?.jsonArray ?: return emptyList()

                searchArray.mapNotNull { el ->
                    val obj = el.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val rawSnippet = obj["snippet"]?.jsonPrimitive?.content.orEmpty()
                    val cleanSnippet = cleanHtmlEntities(rawSnippet)
                    val pageUrl = "https://commons.wikimedia.org/wiki/${URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"
                    ResearchSource(
                        title = title.removePrefix("File:"),
                        url = pageUrl,
                        snippet = cleanSnippet.ifBlank { "Wikimedia Commons visual & media reference for $title" },
                        sourceDomain = "commons.wikimedia.org"
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildGroundedResearchPrompt(
        query: String,
        mode: ResearchMode,
        sources: List<ResearchSource>
    ): String {
        return buildString {
            append("Research Query: \"$query\"\n")
            append("Mode: ${mode.title}\n\n")
            append("Verified Retrieved Web Sources:\n")
            sources.forEachIndexed { idx, src ->
                append("[${idx + 1}] Title: ${src.title}\n")
                append("    URL: ${src.url}\n")
                append("    Snippet: ${src.snippet}\n\n")
            }
            append("Instructions:\n")
            if (mode == ResearchMode.FAST_SEARCH) {
                append("Provide a fast, clear, well-structured summary answering \"$query\" grounded in the verified sources above. Cite sources inline using [1], [2], etc. Never invent URLs or fake citations.")
            } else {
                append("Synthesize a comprehensive Deep Research briefing on \"$query\" with:\n")
                append("1. Executive Summary\n")
                append("2. Key Findings & Analysis (citing [1], [2], etc. inline)\n")
                append("3. Technical / Practical Implications\n")
                append("Strictly cite only the verified sources provided above. Never invent citations.")
            }
        }
    }

    private fun cleanHtmlEntities(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractDomain(url: String): String {
        return url.removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .ifBlank { "web" }
    }
}
