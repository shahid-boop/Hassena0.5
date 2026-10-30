package com.example.services.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ChatMessageEntity(
    val id: Long = 0,
    val role: String, // "user", "assistant", "tool"
    val content: String,
    val providerName: String = "Local Engine",
    val modelName: String = "On-Device",
    val routeCategory: String = "LOCAL ENGINE",
    val latencyMs: Long = 0L,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val recoveryHint: String? = null,
    val citationsSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryFactEntity(
    val id: Long = 0,
    val category: String, // "PREFERENCE", "FACT", "INSTRUCTION", "HABIT"
    val content: String,
    val source: String = "User / Conversation",
    val timestamp: Long = System.currentTimeMillis()
)

data class ResponseCacheEntity(
    val normalizedQuery: String,
    val responseText: String,
    val providerName: String,
    val modelName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AutomationTaskEntity(
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String = "SYSTEM",
    val status: String = "QUEUED", // "RUNNING", "QUEUED", "COMPLETED", "FAILED", "CANCELLED"
    val progress: Int = 0,
    val triggerType: String = "MANUAL", // "SCHEDULED", "MANUAL", "BATTERY", "SYSTEM"
    val lastRunTimestamp: Long = System.currentTimeMillis(),
    val resultLog: String = "",
    val retryCount: Int = 0
)

data class KnowledgeItemEntity(
    val id: Long = 0,
    val title: String,
    val type: String = "DOCUMENT", // "DOCUMENT", "WEB_LINK", "NOTE"
    val contentSnippet: String,
    val fullText: String,
    val sourcePath: String = "",
    val chunkCount: Int = 1,
    val wordCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

interface HasseenaDao {
    fun observeMessages(): Flow<List<ChatMessageEntity>>
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>
    suspend fun insertMessage(message: ChatMessageEntity): Long
    suspend fun updateMessageContent(
        id: Long,
        content: String,
        isStreaming: Boolean,
        latencyMs: Long,
        isError: Boolean = false,
        recoveryHint: String? = null,
        citations: String? = null
    )
    suspend fun clearAllMessages()

    fun observeMemoryFacts(): Flow<List<MemoryFactEntity>>
    suspend fun getActiveMemoryFacts(): List<MemoryFactEntity>
    suspend fun insertMemoryFact(fact: MemoryFactEntity): Long
    suspend fun deleteMemoryFactById(id: Long)
    suspend fun clearAllMemoryFacts()

    suspend fun getCachedResponse(query: String): ResponseCacheEntity?
    suspend fun putCachedResponse(entry: ResponseCacheEntity)
    fun observeCacheCount(): Flow<Int>
    suspend fun clearResponseCache()

    fun observeAutomationTasks(): Flow<List<AutomationTaskEntity>>
    suspend fun getAutomationTasks(): List<AutomationTaskEntity>
    suspend fun insertAutomationTask(task: AutomationTaskEntity): Long
    suspend fun updateAutomationTaskStatus(id: Long, status: String, progress: Int, resultLog: String)
    suspend fun deleteAutomationTaskById(id: Long)

    fun observeKnowledgeItems(): Flow<List<KnowledgeItemEntity>>
    suspend fun getKnowledgeItems(): List<KnowledgeItemEntity>
    suspend fun insertKnowledgeItem(item: KnowledgeItemEntity): Long
    suspend fun searchKnowledge(query: String): List<KnowledgeItemEntity>
    suspend fun deleteKnowledgeItemById(id: Long)
}

class HasseenaDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "hasseena_ai_assistant.db", null, 1) {

    private val messagesState = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val memoryFactsState = MutableStateFlow<List<MemoryFactEntity>>(emptyList())
    private val cacheCountState = MutableStateFlow(0)
    private val automationTasksState = MutableStateFlow<List<AutomationTaskEntity>>(emptyList())
    private val knowledgeItemsState = MutableStateFlow<List<KnowledgeItemEntity>>(emptyList())

    private val daoImpl = object : HasseenaDao {
        override fun observeMessages(): Flow<List<ChatMessageEntity>> = messagesState.asStateFlow()

        override suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity> =
            withContext(Dispatchers.IO) {
                queryRecentMessagesInternal(limit)
            }

        override suspend fun insertMessage(message: ChatMessageEntity): Long =
            withContext(Dispatchers.IO) {
                val values = ContentValues().apply {
                    if (message.id > 0L) put("id", message.id)
                    put("role", message.role)
                    put("content", message.content)
                    put("providerName", message.providerName)
                    put("modelName", message.modelName)
                    put("routeCategory", message.routeCategory)
                    put("latencyMs", message.latencyMs)
                    put("isStreaming", if (message.isStreaming) 1 else 0)
                    put("isError", if (message.isError) 1 else 0)
                    put("recoveryHint", message.recoveryHint)
                    put("citationsSummary", message.citationsSummary)
                    put("timestamp", message.timestamp)
                }
                val id = writableDatabase.insertWithOnConflict(
                    "chat_messages",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                refreshMessagesStateInternal()
                id
            }

        override suspend fun updateMessageContent(
            id: Long,
            content: String,
            isStreaming: Boolean,
            latencyMs: Long,
            isError: Boolean,
            recoveryHint: String?,
            citations: String?
        ) = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("content", content)
                put("isStreaming", if (isStreaming) 1 else 0)
                put("latencyMs", latencyMs)
                put("isError", if (isError) 1 else 0)
                put("recoveryHint", recoveryHint)
                put("citationsSummary", citations)
            }
            writableDatabase.update(
                "chat_messages",
                values,
                "id = ?",
                arrayOf(id.toString())
            )
            refreshMessagesStateInternal()
        }

        override suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
            writableDatabase.delete("chat_messages", null, null)
            refreshMessagesStateInternal()
        }

        override fun observeMemoryFacts(): Flow<List<MemoryFactEntity>> =
            memoryFactsState.asStateFlow()

        override suspend fun getActiveMemoryFacts(): List<MemoryFactEntity> =
            withContext(Dispatchers.IO) {
                queryMemoryFactsInternal(limit = 25)
            }

        override suspend fun insertMemoryFact(fact: MemoryFactEntity): Long =
            withContext(Dispatchers.IO) {
                val values = ContentValues().apply {
                    if (fact.id > 0L) put("id", fact.id)
                    put("category", fact.category)
                    put("content", fact.content)
                    put("source", fact.source)
                    put("timestamp", fact.timestamp)
                }
                val id = writableDatabase.insertWithOnConflict(
                    "memory_facts",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                refreshMemoryFactsStateInternal()
                id
            }

        override suspend fun deleteMemoryFactById(id: Long) = withContext(Dispatchers.IO) {
            writableDatabase.delete("memory_facts", "id = ?", arrayOf(id.toString()))
            refreshMemoryFactsStateInternal()
        }

        override suspend fun clearAllMemoryFacts() = withContext(Dispatchers.IO) {
            writableDatabase.delete("memory_facts", null, null)
            refreshMemoryFactsStateInternal()
        }

        override suspend fun getCachedResponse(query: String): ResponseCacheEntity? =
            withContext(Dispatchers.IO) {
                readableDatabase.query(
                    "response_cache",
                    arrayOf("normalizedQuery", "responseText", "providerName", "modelName", "timestamp"),
                    "normalizedQuery = ?",
                    arrayOf(query),
                    null,
                    null,
                    null,
                    "1"
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        ResponseCacheEntity(
                            normalizedQuery = cursor.getString(0),
                            responseText = cursor.getString(1),
                            providerName = cursor.getString(2),
                            modelName = cursor.getString(3),
                            timestamp = cursor.getLong(4)
                        )
                    } else {
                        null
                    }
                }
            }

        override suspend fun putCachedResponse(entry: ResponseCacheEntity) =
            withContext(Dispatchers.IO) {
                val values = ContentValues().apply {
                    put("normalizedQuery", entry.normalizedQuery)
                    put("responseText", entry.responseText)
                    put("providerName", entry.providerName)
                    put("modelName", entry.modelName)
                    put("timestamp", entry.timestamp)
                }
                writableDatabase.insertWithOnConflict(
                    "response_cache",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                refreshCacheCountStateInternal()
            }

        override fun observeCacheCount(): Flow<Int> = cacheCountState.asStateFlow()

        override suspend fun clearResponseCache() = withContext(Dispatchers.IO) {
            writableDatabase.delete("response_cache", null, null)
            refreshCacheCountStateInternal()
        }

        override fun observeAutomationTasks(): Flow<List<AutomationTaskEntity>> = automationTasksState.asStateFlow()

        override suspend fun getAutomationTasks(): List<AutomationTaskEntity> = withContext(Dispatchers.IO) {
            queryAutomationTasksInternal()
        }

        override suspend fun insertAutomationTask(task: AutomationTaskEntity): Long = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                if (task.id > 0L) put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("status", task.status)
                put("progress", task.progress)
                put("triggerType", task.triggerType)
                put("lastRunTimestamp", task.lastRunTimestamp)
                put("resultLog", task.resultLog)
                put("retryCount", task.retryCount)
            }
            val id = writableDatabase.insertWithOnConflict("automation_tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshAutomationTasksStateInternal()
            id
        }

        override suspend fun updateAutomationTaskStatus(id: Long, status: String, progress: Int, resultLog: String) = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("status", status)
                put("progress", progress)
                put("lastRunTimestamp", System.currentTimeMillis())
                if (resultLog.isNotEmpty()) put("resultLog", resultLog)
            }
            writableDatabase.update("automation_tasks", values, "id = ?", arrayOf(id.toString()))
            refreshAutomationTasksStateInternal()
        }

        override suspend fun deleteAutomationTaskById(id: Long) = withContext(Dispatchers.IO) {
            writableDatabase.delete("automation_tasks", "id = ?", arrayOf(id.toString()))
            refreshAutomationTasksStateInternal()
        }

        override fun observeKnowledgeItems(): Flow<List<KnowledgeItemEntity>> = knowledgeItemsState.asStateFlow()

        override suspend fun getKnowledgeItems(): List<KnowledgeItemEntity> = withContext(Dispatchers.IO) {
            queryKnowledgeItemsInternal()
        }

        override suspend fun insertKnowledgeItem(item: KnowledgeItemEntity): Long = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                if (item.id > 0L) put("id", item.id)
                put("title", item.title)
                put("type", item.type)
                put("contentSnippet", item.contentSnippet)
                put("fullText", item.fullText)
                put("sourcePath", item.sourcePath)
                put("chunkCount", item.chunkCount)
                put("wordCount", item.wordCount)
                put("timestamp", item.timestamp)
            }
            val id = writableDatabase.insertWithOnConflict("knowledge_items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshKnowledgeItemsStateInternal()
            id
        }

        override suspend fun searchKnowledge(query: String): List<KnowledgeItemEntity> = withContext(Dispatchers.IO) {
            val normalized = query.trim().lowercase()
            queryKnowledgeItemsInternal().filter {
                it.title.lowercase().contains(normalized) ||
                it.contentSnippet.lowercase().contains(normalized) ||
                it.fullText.lowercase().contains(normalized)
            }
        }

        override suspend fun deleteKnowledgeItemById(id: Long) = withContext(Dispatchers.IO) {
            writableDatabase.delete("knowledge_items", "id = ?", arrayOf(id.toString()))
            refreshKnowledgeItemsStateInternal()
        }
    }

    init {
        ensureAutomationAndKnowledgeTables(writableDatabase)
        refreshMessagesStateInternal()
        refreshMemoryFactsStateInternal()
        refreshCacheCountStateInternal()
        refreshAutomationTasksStateInternal()
        refreshKnowledgeItemsStateInternal()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                providerName TEXT NOT NULL,
                modelName TEXT NOT NULL,
                routeCategory TEXT NOT NULL,
                latencyMs INTEGER NOT NULL,
                isStreaming INTEGER NOT NULL,
                isError INTEGER NOT NULL,
                recoveryHint TEXT,
                citationsSummary TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS memory_facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                content TEXT NOT NULL,
                source TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS response_cache (
                normalizedQuery TEXT PRIMARY KEY NOT NULL,
                responseText TEXT NOT NULL,
                providerName TEXT NOT NULL,
                modelName TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        ensureAutomationAndKnowledgeTables(db)
    }

    private fun ensureAutomationAndKnowledgeTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS automation_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                status TEXT NOT NULL,
                progress INTEGER NOT NULL,
                triggerType TEXT NOT NULL,
                lastRunTimestamp INTEGER NOT NULL,
                resultLog TEXT NOT NULL,
                retryCount INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS knowledge_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                contentSnippet TEXT NOT NULL,
                fullText TEXT NOT NULL,
                sourcePath TEXT NOT NULL,
                chunkCount INTEGER NOT NULL,
                wordCount INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Seed initial Automation Tasks if empty
        val taskCount = db.rawQuery("SELECT COUNT(*) FROM automation_tasks", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        if (taskCount == 0) {
            val tasks = listOf(
                AutomationTaskEntity(title = "Backup Photos", description = "Sync camera roll and workspace captures to safe offline archive", category = "BACKUP", status = "RUNNING", progress = 65, triggerType = "SCHEDULED", resultLog = "Syncing batch 18/24..."),
                AutomationTaskEntity(title = "Send Daily Report", description = "Generate morning intelligence summary with calendar and weather", category = "NOTIFICATION", status = "QUEUED", progress = 0, triggerType = "SCHEDULED", resultLog = "Scheduled for 08:00 AM"),
                AutomationTaskEntity(title = "Website Monitoring", description = "Ping configured endpoints and verify API uptime", category = "WEB", status = "COMPLETED", progress = 100, triggerType = "SYSTEM", resultLog = "Checked endpoints: 200 OK, latency 42ms"),
                AutomationTaskEntity(title = "Auto Reply (Email)", description = "Draft intelligent contextual responses to incoming alerts", category = "COMMUNICATION", status = "FAILED", progress = 0, triggerType = "MANUAL", resultLog = "Requires explicit user confirmation before sending", retryCount = 1),
                AutomationTaskEntity(title = "System Cleanup", description = "Clear temporary response caches and orphaned image buffers", category = "SYSTEM", status = "COMPLETED", progress = 100, triggerType = "MANUAL", resultLog = "Freed 420 MB cache storage")
            )
            for (t in tasks) {
                val cv = ContentValues().apply {
                    put("title", t.title)
                    put("description", t.description)
                    put("category", t.category)
                    put("status", t.status)
                    put("progress", t.progress)
                    put("triggerType", t.triggerType)
                    put("lastRunTimestamp", t.lastRunTimestamp)
                    put("resultLog", t.resultLog)
                    put("retryCount", t.retryCount)
                }
                db.insert("automation_tasks", null, cv)
            }
        }

        // Seed initial Knowledge items if empty
        val itemCount = db.rawQuery("SELECT COUNT(*) FROM knowledge_items", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        if (itemCount == 0) {
            val docs = listOf(
                KnowledgeItemEntity(title = "JARVIS Core Directives.md", type = "DOCUMENT", contentSnippet = "Autonomous native AI assistant operating protocol, security constraints, and safety guidelines.", fullText = "JARVIS operates as a high-performance native assistant on Android. Core rules: least-privilege permissions, local credential protection, grounded knowledge answers with citations, and zero covert operations.", chunkCount = 4, wordCount = 120),
                KnowledgeItemEntity(title = "Android Architecture & Compose.pdf", type = "DOCUMENT", contentSnippet = "M3 design tokens, state hoisting, coroutine flows, and background services spec.", fullText = "Jetpack Compose architecture guide with Material 3 styling, reactive ViewModel state hoisting, Room SQLite data persistence, and foreground voice services.", chunkCount = 8, wordCount = 350),
                KnowledgeItemEntity(title = "Neural Routing & Model Fallbacks.pdf", type = "DOCUMENT", contentSnippet = "Tiered model routing strategies based on latency, complexity, and capabilities.", fullText = "Smart router directs simple QA to fast tier models like Gemini 3.5 Flash-Lite or Groq, coding tasks to Codestral/Qwen, and deep reasoning to GPT-4o or Gemini 3.1 Pro.", chunkCount = 6, wordCount = 280),
                KnowledgeItemEntity(title = "Device Automation Protocols.txt", type = "DOCUMENT", contentSnippet = "Permitted intents, accessibility navigation, and sensitive action confirmations.", fullText = "Phone control directives require explicit confirmation dialogs for phone calls, outgoing SMS, and global setting modifications.", chunkCount = 3, wordCount = 140),
                KnowledgeItemEntity(title = "Project Manifest & Roadmap.docx", type = "DOCUMENT", contentSnippet = "Release milestones, supported APIs, and test coverage targets for JARVIS Assistant.", fullText = "Roadmap covers multi-model routing, offline fallback, SAF workspace, real-time research synthesis, and local RAG retrieval with citations.", chunkCount = 5, wordCount = 210),
                KnowledgeItemEntity(title = "Audio Visualizer Specifications.md", type = "DOCUMENT", contentSnippet = "Arc reactor rendering math, canvas drawing loops, and FFT frequency bands.", fullText = "Arc reactor orb utilizes multi-ring concentric Canvas paths with sine wave displacement and neon red alpha gradients.", chunkCount = 4, wordCount = 180),
                KnowledgeItemEntity(title = "RAG Indexing & Chunking Strategy.md", type = "DOCUMENT", contentSnippet = "Document parsing, sliding window chunking, and source citation formatting.", fullText = "RAG queries scan indexed text chunks, calculate relevance scores, and format responses with explicit source badges.", chunkCount = 5, wordCount = 230),
                KnowledgeItemEntity(title = "Security & Encryption Whitepaper.pdf", type = "DOCUMENT", contentSnippet = "Android Keystore AES-256 GCM encryption for user-supplied provider API keys.", fullText = "API keys are encrypted in-memory and persisted inside private SharedPreferences using Android Keystore master keys.", chunkCount = 7, wordCount = 320),
                KnowledgeItemEntity(title = "Coding Studio Toolchain.md", type = "DOCUMENT", contentSnippet = "Syntax definitions, JetBrains Mono font metrics, and code formatting rules.", fullText = "Coding studio provides multi-language code generation, syntax validation, line numbering, and terminal console logs.", chunkCount = 4, wordCount = 190),
                KnowledgeItemEntity(title = "Voice Synthesis & STT Pipelines.txt", type = "DOCUMENT", contentSnippet = "Android SpeechRecognizer, ElevenLabs streaming, and Gemini TTS endpoints.", fullText = "Voice pipeline manages speech-to-text recognition events, partial transcripts, speech synthesis, and barge-in interruption.", chunkCount = 5, wordCount = 240),
                KnowledgeItemEntity(title = "System Telemetry & Health Monitoring.md", type = "DOCUMENT", contentSnippet = "Battery, thermal state, RAM memory allocation, and storage metrics.", fullText = "Device telemetry monitors battery temperature, free RAM, network connection state, and storage availability.", chunkCount = 3, wordCount = 160),
                KnowledgeItemEntity(title = "Backup & Safe Storage Rules.txt", type = "DOCUMENT", contentSnippet = "SAF storage scopes, ZIP extraction safeguards, and traversal protection.", fullText = "Zip inspection enforces canonical path verification against path traversal attacks and checks entry size limits.", chunkCount = 4, wordCount = 175)
            )
            for (d in docs) {
                val cv = ContentValues().apply {
                    put("title", d.title)
                    put("type", d.type)
                    put("contentSnippet", d.contentSnippet)
                    put("fullText", d.fullText)
                    put("sourcePath", d.sourcePath)
                    put("chunkCount", d.chunkCount)
                    put("wordCount", d.wordCount)
                    put("timestamp", d.timestamp)
                }
                db.insert("knowledge_items", null, cv)
            }

            val webLinks = listOf(
                KnowledgeItemEntity(title = "Android Developers Documentation", type = "WEB_LINK", contentSnippet = "Official developer reference for Android APIs, Jetpack Compose, and system services.", fullText = "https://developer.android.com/reference - Official guide for Android development.", sourcePath = "https://developer.android.com", chunkCount = 2, wordCount = 50),
                KnowledgeItemEntity(title = "Google Gemini API Portal", type = "WEB_LINK", contentSnippet = "Developer documentation for Gemini models, multimodal inputs, and streaming SSE.", fullText = "https://ai.google.dev - Gemini API documentation and SDK guides.", sourcePath = "https://ai.google.dev", chunkCount = 2, wordCount = 50),
                KnowledgeItemEntity(title = "Kotlin Language Reference", type = "WEB_LINK", contentSnippet = "Kotlin coroutines, Flow, serialization, and language specifications.", fullText = "https://kotlinlang.org/docs - Complete Kotlin reference documentation.", sourcePath = "https://kotlinlang.org", chunkCount = 2, wordCount = 50),
                KnowledgeItemEntity(title = "OpenAI Developer Platform", type = "WEB_LINK", contentSnippet = "API reference for GPT-4o, embeddings, and chat completions.", fullText = "https://platform.openai.com/docs - OpenAI API developer docs.", sourcePath = "https://platform.openai.com", chunkCount = 2, wordCount = 50),
                KnowledgeItemEntity(title = "Material Design 3 Guidelines", type = "WEB_LINK", contentSnippet = "Design specifications for components, typography, color roles, and elevation.", fullText = "https://m3.material.io - Material Design 3 guidelines and components.", sourcePath = "https://m3.material.io", chunkCount = 2, wordCount = 50)
            )
            for (w in webLinks) {
                val cv = ContentValues().apply {
                    put("title", w.title)
                    put("type", w.type)
                    put("contentSnippet", w.contentSnippet)
                    put("fullText", w.fullText)
                    put("sourcePath", w.sourcePath)
                    put("chunkCount", w.chunkCount)
                    put("wordCount", w.wordCount)
                    put("timestamp", w.timestamp)
                }
                db.insert("knowledge_items", null, cv)
            }

            val notes = listOf(
                KnowledgeItemEntity(title = "Quick Voice Command Shortcuts", type = "NOTE", contentSnippet = "Common voice commands for flashlight, brightness, volume, call, and SMS.", fullText = "Voice shortcuts: 'Turn on flashlight', 'Set brightness to 80', 'Mute volume', 'Call contact', 'Open YouTube', 'Explain quantum physics'.", chunkCount = 1, wordCount = 25),
                KnowledgeItemEntity(title = "Personal Daily Schedule", type = "NOTE", contentSnippet = "Routine agenda: 08:00 Morning briefing, 10:00 Standup, 18:00 Workout.", fullText = "Schedule: 08:00 Morning briefing & weather, 10:00 Team standup, 13:00 Lunch, 18:00 Workout, 21:00 Evening review.", chunkCount = 1, wordCount = 20),
                KnowledgeItemEntity(title = "Favorite AI Models Priority", type = "NOTE", contentSnippet = "Priority ordering: Gemini 3.8 Flash for coding, GPT-4o for reasoning, Flash-Lite for fast QA.", fullText = "Model preference: Gemini 3.8 Flash (speed/coding), GPT-4o (complex reasoning), Claude 3.5 Sonnet (nuanced writing).", chunkCount = 1, wordCount = 22),
                KnowledgeItemEntity(title = "Meeting Notes: System Architecture", type = "NOTE", contentSnippet = "Discussion points on edge-to-edge UI, zero-trust security rules, and Robolectric JVM testing.", fullText = "Discussed: edge-to-edge system insets handling, zero-trust Firestore rules, AES-GCM keystore encryption, and Robolectric test runners.", chunkCount = 2, wordCount = 30),
                KnowledgeItemEntity(title = "Contact Emergency Info", type = "NOTE", contentSnippet = "Emergency contacts and ICE numbers for assistant auto-dialing.", fullText = "Emergency contact: Home 555-0199, Work 555-0144. Fast dial command: 'Call emergency'.", chunkCount = 1, wordCount = 15),
                KnowledgeItemEntity(title = "Workout & Hydration Goals", type = "NOTE", contentSnippet = "Daily target: 3 liters water, 30 min cardio, 10k steps.", fullText = "Daily targets: 3 liters of water, 10,000 steps, 30 min cardio workout.", chunkCount = 1, wordCount = 15),
                KnowledgeItemEntity(title = "Server Deployment Checklist", type = "NOTE", contentSnippet = "Pre-flight checks: tests passing, signing configuration verified, secrets configured in .env.", fullText = "Checklist: Run unit tests, verify AGP build, check ProGuard rules, confirm keystore variables, test APK.", chunkCount = 1, wordCount = 20),
                KnowledgeItemEntity(title = "Home Automation Ideas", type = "NOTE", contentSnippet = "Ideas for smart home intents: morning alarm triggering briefing, battery full alert.", fullText = "Automation ideas: When battery reaches 100%, speak 'Battery full'. When morning alarm fires, run Daily Report task.", chunkCount = 1, wordCount = 22)
            )
            for (n in notes) {
                val cv = ContentValues().apply {
                    put("title", n.title)
                    put("type", n.type)
                    put("contentSnippet", n.contentSnippet)
                    put("fullText", n.fullText)
                    put("sourcePath", n.sourcePath)
                    put("chunkCount", n.chunkCount)
                    put("wordCount", n.wordCount)
                    put("timestamp", n.timestamp)
                }
                db.insert("knowledge_items", null, cv)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        db.execSQL("DROP TABLE IF EXISTS memory_facts")
        db.execSQL("DROP TABLE IF EXISTS response_cache")
        db.execSQL("DROP TABLE IF EXISTS automation_tasks")
        db.execSQL("DROP TABLE IF EXISTS knowledge_items")
        onCreate(db)
    }

    fun hasseenaDao(): HasseenaDao = daoImpl

    private fun refreshMessagesStateInternal() {
        val list = mutableListOf<ChatMessageEntity>()
        readableDatabase.query(
            "chat_messages",
            arrayOf(
                "id", "role", "content", "providerName", "modelName",
                "routeCategory", "latencyMs", "isStreaming", "isError",
                "recoveryHint", "citationsSummary", "timestamp"
            ),
            null,
            null,
            null,
            null,
            "timestamp ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ChatMessageEntity(
                        id = cursor.getLong(0),
                        role = cursor.getString(1),
                        content = cursor.getString(2),
                        providerName = cursor.getString(3),
                        modelName = cursor.getString(4),
                        routeCategory = cursor.getString(5),
                        latencyMs = cursor.getLong(6),
                        isStreaming = cursor.getInt(7) != 0,
                        isError = cursor.getInt(8) != 0,
                        recoveryHint = if (cursor.isNull(9)) null else cursor.getString(9),
                        citationsSummary = if (cursor.isNull(10)) null else cursor.getString(10),
                        timestamp = cursor.getLong(11)
                    )
                )
            }
        }
        messagesState.value = list
    }

    private fun queryRecentMessagesInternal(limit: Int): List<ChatMessageEntity> {
        val list = mutableListOf<ChatMessageEntity>()
        readableDatabase.query(
            "chat_messages",
            arrayOf(
                "id", "role", "content", "providerName", "modelName",
                "routeCategory", "latencyMs", "isStreaming", "isError",
                "recoveryHint", "citationsSummary", "timestamp"
            ),
            null,
            null,
            null,
            null,
            "timestamp DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ChatMessageEntity(
                        id = cursor.getLong(0),
                        role = cursor.getString(1),
                        content = cursor.getString(2),
                        providerName = cursor.getString(3),
                        modelName = cursor.getString(4),
                        routeCategory = cursor.getString(5),
                        latencyMs = cursor.getLong(6),
                        isStreaming = cursor.getInt(7) != 0,
                        isError = cursor.getInt(8) != 0,
                        recoveryHint = if (cursor.isNull(9)) null else cursor.getString(9),
                        citationsSummary = if (cursor.isNull(10)) null else cursor.getString(10),
                        timestamp = cursor.getLong(11)
                    )
                )
            }
        }
        return list
    }

    private fun refreshMemoryFactsStateInternal() {
        memoryFactsState.value = queryMemoryFactsInternal(limit = null)
    }

    private fun queryMemoryFactsInternal(limit: Int?): List<MemoryFactEntity> {
        val list = mutableListOf<MemoryFactEntity>()
        readableDatabase.query(
            "memory_facts",
            arrayOf("id", "category", "content", "source", "timestamp"),
            null,
            null,
            null,
            null,
            "timestamp DESC",
            limit?.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    MemoryFactEntity(
                        id = cursor.getLong(0),
                        category = cursor.getString(1),
                        content = cursor.getString(2),
                        source = cursor.getString(3),
                        timestamp = cursor.getLong(4)
                    )
                )
            }
        }
        return list
    }

    private fun refreshCacheCountStateInternal() {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM response_cache", null).use { cursor ->
            cacheCountState.value = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun refreshAutomationTasksStateInternal() {
        automationTasksState.value = queryAutomationTasksInternal()
    }

    private fun queryAutomationTasksInternal(): List<AutomationTaskEntity> {
        val list = mutableListOf<AutomationTaskEntity>()
        readableDatabase.query(
            "automation_tasks",
            arrayOf("id", "title", "description", "category", "status", "progress", "triggerType", "lastRunTimestamp", "resultLog", "retryCount"),
            null,
            null,
            null,
            null,
            "lastRunTimestamp DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    AutomationTaskEntity(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        description = cursor.getString(2),
                        category = cursor.getString(3),
                        status = cursor.getString(4),
                        progress = cursor.getInt(5),
                        triggerType = cursor.getString(6),
                        lastRunTimestamp = cursor.getLong(7),
                        resultLog = cursor.getString(8),
                        retryCount = cursor.getInt(9)
                    )
                )
            }
        }
        return list
    }

    private fun refreshKnowledgeItemsStateInternal() {
        knowledgeItemsState.value = queryKnowledgeItemsInternal()
    }

    private fun queryKnowledgeItemsInternal(): List<KnowledgeItemEntity> {
        val list = mutableListOf<KnowledgeItemEntity>()
        readableDatabase.query(
            "knowledge_items",
            arrayOf("id", "title", "type", "contentSnippet", "fullText", "sourcePath", "chunkCount", "wordCount", "timestamp"),
            null,
            null,
            null,
            null,
            "timestamp DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    KnowledgeItemEntity(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        type = cursor.getString(2),
                        contentSnippet = cursor.getString(3),
                        fullText = cursor.getString(4),
                        sourcePath = cursor.getString(5),
                        chunkCount = cursor.getInt(6),
                        wordCount = cursor.getInt(7),
                        timestamp = cursor.getLong(8)
                    )
                )
            }
        }
        return list
    }

    companion object {
        @Volatile
        private var INSTANCE: HasseenaDatabase? = null

        fun getInstance(context: Context): HasseenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: HasseenaDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
