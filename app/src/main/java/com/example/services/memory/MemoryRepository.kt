package com.example.services.memory

import kotlinx.coroutines.flow.Flow
import java.util.Locale

class MemoryRepository(private val dao: HasseenaDao) {

    val messagesFlow: Flow<List<ChatMessageEntity>> = dao.observeMessages()
    val memoryFactsFlow: Flow<List<MemoryFactEntity>> = dao.observeMemoryFacts()
    val cacheCountFlow: Flow<Int> = dao.observeCacheCount()
    val automationTasksFlow: Flow<List<AutomationTaskEntity>> = dao.observeAutomationTasks()
    val knowledgeItemsFlow: Flow<List<KnowledgeItemEntity>> = dao.observeKnowledgeItems()

    suspend fun getAutomationTasks(): List<AutomationTaskEntity> = dao.getAutomationTasks()
    suspend fun addAutomationTask(task: AutomationTaskEntity): Long = dao.insertAutomationTask(task)
    suspend fun updateAutomationTaskStatus(id: Long, status: String, progress: Int, resultLog: String = "") {
        dao.updateAutomationTaskStatus(id, status, progress, resultLog)
    }
    suspend fun deleteAutomationTask(id: Long) = dao.deleteAutomationTaskById(id)

    suspend fun getKnowledgeItems(): List<KnowledgeItemEntity> = dao.getKnowledgeItems()
    suspend fun addKnowledgeItem(item: KnowledgeItemEntity): Long = dao.insertKnowledgeItem(item)
    suspend fun searchKnowledge(query: String): List<KnowledgeItemEntity> = dao.searchKnowledge(query)
    suspend fun deleteKnowledgeItem(id: Long) = dao.deleteKnowledgeItemById(id)

    suspend fun addMessage(message: ChatMessageEntity): Long {
        return dao.insertMessage(message)
    }

    suspend fun updateStreamingMessage(
        id: Long,
        content: String,
        isStreaming: Boolean,
        latencyMs: Long,
        isError: Boolean = false,
        recoveryHint: String? = null,
        citations: String? = null
    ) {
        dao.updateMessageContent(
            id = id,
            content = content,
            isStreaming = isStreaming,
            latencyMs = latencyMs,
            isError = isError,
            recoveryHint = recoveryHint,
            citations = citations
        )
    }

    suspend fun getRecentConversationContext(limit: Int = 8): List<ChatMessageEntity> {
        return dao.getRecentMessages(limit).reversed()
    }

    suspend fun clearConversations() {
        dao.clearAllMessages()
    }

    suspend fun addMemoryFact(category: String, content: String, source: String = "User") {
        val trimmed = content.trim()
        if (trimmed.isNotEmpty()) {
            dao.insertMemoryFact(
                MemoryFactEntity(
                    category = category.uppercase(Locale.US),
                    content = trimmed,
                    source = source
                )
            )
        }
    }

    suspend fun deleteMemoryFact(id: Long) {
        dao.deleteMemoryFactById(id)
    }

    suspend fun clearAllMemoryAndCache() {
        dao.clearAllMemoryFacts()
        dao.clearResponseCache()
    }

    suspend fun getMemoryContextPrompt(memoryEnabled: Boolean, customInstructions: String): String {
        if (!memoryEnabled && customInstructions.isBlank()) return ""
        val sb = StringBuilder()
        if (customInstructions.isNotBlank()) {
            sb.append("\n[USER-DEFINED INSTRUCTIONS]\n")
            sb.append(customInstructions.trim()).append("\n")
        }
        if (memoryEnabled) {
            val facts = dao.getActiveMemoryFacts()
            if (facts.isNotEmpty()) {
                sb.append("\n[PERSONAL MEMORY CONTEXT]\n")
                facts.forEach { fact ->
                    sb.append("- (${fact.category}) ${fact.content}\n")
                }
            }
        }
        return sb.toString()
    }

    /**
     * Automatically detects and saves explicit user memory statements such as:
     * "Remember that I prefer...", "My name is...", "Always answer in..."
     */
    suspend fun extractAndSaveImplicitMemoryIfApplicable(
        userText: String,
        memoryEnabled: Boolean
    ): String? {
        if (!memoryEnabled) return null
        val lower = userText.trim().lowercase(Locale.US)
        return when {
            lower.startsWith("remember that ") -> {
                val fact = userText.trim().substring("remember that ".length).trim()
                if (fact.length >= 4) {
                    addMemoryFact("FACT", fact, "Voice/Chat Command")
                    fact
                } else null
            }
            lower.startsWith("remember ") && lower.length > 14 -> {
                val fact = userText.trim().substring("remember ".length).trim()
                addMemoryFact("FACT", fact, "Voice/Chat Command")
                fact
            }
            lower.startsWith("my name is ") -> {
                val fact = "User's name is " + userText.trim().substring("my name is ".length).trim()
                addMemoryFact("PREFERENCE", fact, "Auto-Memory")
                fact
            }
            lower.startsWith("i prefer ") -> {
                val fact = "User prefers " + userText.trim().substring("i prefer ".length).trim()
                addMemoryFact("PREFERENCE", fact, "Auto-Memory")
                fact
            }
            else -> null
        }
    }

    private fun normalizeQuery(raw: String): String {
        return raw.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
    }

    suspend fun lookupCachedResponse(query: String, maxAgeMs: Long = 1000L * 60 * 60 * 12): ResponseCacheEntity? {
        val norm = normalizeQuery(query)
        if (norm.length < 4) return null
        val cached = dao.getCachedResponse(norm) ?: return null
        return if (System.currentTimeMillis() - cached.timestamp <= maxAgeMs) cached else null
    }

    suspend fun cacheResponse(query: String, responseText: String, providerName: String, modelName: String) {
        val norm = normalizeQuery(query)
        if (norm.length >= 4 && responseText.length in 10..4000) {
            dao.putCachedResponse(
                ResponseCacheEntity(
                    normalizedQuery = norm,
                    responseText = responseText,
                    providerName = providerName,
                    modelName = modelName
                )
            )
        }
    }

    suspend fun clearCache() {
        dao.clearResponseCache()
    }
}
