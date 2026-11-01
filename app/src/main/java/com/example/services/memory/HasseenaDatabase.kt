package com.example.services.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.models.AgentStep
import com.example.models.AutonomousStepStatus
import com.example.models.AgentTask
import com.example.models.AgentTaskStatus
import com.example.models.CodeDiffPatch
import com.example.models.ErrorCategory
import com.example.models.ErrorSeverity
import com.example.models.RepairState
import com.example.models.ResolutionStatus
import com.example.models.RootCauseAnalysis
import com.example.models.SelfHealingRepairRecord
import com.example.models.SystemErrorRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    fun observeAgentTasks(): Flow<List<AgentTask>>
    suspend fun getAgentTasks(): List<AgentTask>
    suspend fun getAgentTask(taskId: String): AgentTask?
    suspend fun insertOrUpdateAgentTask(task: AgentTask)
    suspend fun insertOrUpdateAgentStep(step: AgentStep, taskId: String)
    suspend fun deleteAgentTask(taskId: String)

    fun observeErrorRecords(): Flow<List<SystemErrorRecord>>
    suspend fun getErrorRecords(): List<SystemErrorRecord>
    suspend fun getErrorRecord(id: String): SystemErrorRecord?
    suspend fun insertOrUpdateErrorRecord(record: SystemErrorRecord)
    suspend fun clearErrorRecords()

    fun observeSelfHealingRepairs(): Flow<List<SelfHealingRepairRecord>>
    suspend fun getSelfHealingRepairs(): List<SelfHealingRepairRecord>
    suspend fun getSelfHealingRepair(id: String): SelfHealingRepairRecord?
    suspend fun insertOrUpdateRepairRecord(repair: SelfHealingRepairRecord)
}

class HasseenaDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "hasseena_ai_assistant.db", null, 1) {

    private val json = Json { ignoreUnknownKeys = true }
    private val messagesState = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val memoryFactsState = MutableStateFlow<List<MemoryFactEntity>>(emptyList())
    private val cacheCountState = MutableStateFlow(0)
    private val automationTasksState = MutableStateFlow<List<AutomationTaskEntity>>(emptyList())
    private val knowledgeItemsState = MutableStateFlow<List<KnowledgeItemEntity>>(emptyList())
    private val agentTasksState = MutableStateFlow<List<AgentTask>>(emptyList())
    private val errorRecordsState = MutableStateFlow<List<SystemErrorRecord>>(emptyList())
    private val selfHealingRepairsState = MutableStateFlow<List<SelfHealingRepairRecord>>(emptyList())

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

        override fun observeAgentTasks(): Flow<List<AgentTask>> = agentTasksState.asStateFlow()

        override suspend fun getAgentTasks(): List<AgentTask> = withContext(Dispatchers.IO) {
            queryAgentTasksInternal()
        }

        override suspend fun getAgentTask(taskId: String): AgentTask? = withContext(Dispatchers.IO) {
            queryAgentTaskByIdInternal(taskId)
        }

        override suspend fun insertOrUpdateAgentTask(task: AgentTask) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val cv = ContentValues().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("userObjective", task.userObjective)
                    put("executionPlanOverview", task.executionPlanOverview)
                    put("status", task.status.name)
                    put("finalResponse", task.finalResponse)
                    put("errorInformation", task.errorInformation)
                    put("replanCount", task.replanCount)
                    put("currentStepIndex", task.currentStepIndex)
                    put("startTime", task.startTime)
                    put("completionTime", task.completionTime)
                    put("logsJson", json.encodeToString(task.logs))
                    put("changedFilesJson", json.encodeToString(task.changedFiles))
                }
                db.insertWithOnConflict("agent_tasks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                for (step in task.steps) {
                    val scv = ContentValues().apply {
                        put("id", step.id)
                        put("taskId", task.id)
                        put("stepNumber", step.stepNumber)
                        put("title", step.title)
                        put("description", step.description)
                        put("toolId", step.toolId)
                        put("toolArgumentsJson", json.encodeToString(step.toolArguments))
                        put("requiresConfirmation", if (step.requiresConfirmation) 1 else 0)
                        put("status", step.status.name)
                        put("actualResult", step.actualResult)
                        put("errorMessage", step.errorMessage)
                        put("startTime", step.startTime)
                        put("completionTime", step.completionTime)
                    }
                    db.insertWithOnConflict("agent_steps", null, scv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            refreshAgentTasksStateInternal()
        }

        override suspend fun insertOrUpdateAgentStep(step: AgentStep, taskId: String) = withContext(Dispatchers.IO) {
            val scv = ContentValues().apply {
                put("id", step.id)
                put("taskId", taskId)
                put("stepNumber", step.stepNumber)
                put("title", step.title)
                put("description", step.description)
                put("toolId", step.toolId)
                put("toolArgumentsJson", json.encodeToString(step.toolArguments))
                put("requiresConfirmation", if (step.requiresConfirmation) 1 else 0)
                put("status", step.status.name)
                put("actualResult", step.actualResult)
                put("errorMessage", step.errorMessage)
                put("startTime", step.startTime)
                put("completionTime", step.completionTime)
            }
            writableDatabase.insertWithOnConflict("agent_steps", null, scv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshAgentTasksStateInternal()
        }

        override suspend fun deleteAgentTask(taskId: String) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete("agent_steps", "taskId = ?", arrayOf(taskId))
                db.delete("agent_tasks", "id = ?", arrayOf(taskId))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            refreshAgentTasksStateInternal()
        }

        override fun observeErrorRecords(): Flow<List<SystemErrorRecord>> = errorRecordsState.asStateFlow()

        override suspend fun getErrorRecords(): List<SystemErrorRecord> = withContext(Dispatchers.IO) {
            queryErrorRecordsInternal()
        }

        override suspend fun getErrorRecord(id: String): SystemErrorRecord? = withContext(Dispatchers.IO) {
            queryErrorRecordByIdInternal(id)
        }

        override suspend fun insertOrUpdateErrorRecord(record: SystemErrorRecord) = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("id", record.id)
                put("category", record.category.name)
                put("message", record.message)
                put("affectedComponent", record.affectedComponent)
                put("operation", record.operation)
                put("timestamp", record.timestamp)
                put("severity", record.severity.name)
                put("stackTrace", record.stackTrace)
                put("isTransient", if (record.isTransient) 1 else 0)
                put("retryCount", record.retryCount)
                put("maxRetries", record.maxRetries)
                put("suggestedRecovery", record.suggestedRecovery)
                put("resolutionStatus", record.resolutionStatus.name)
                put("repairId", record.repairId)
            }
            writableDatabase.insertWithOnConflict("system_error_records", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshErrorRecordsStateInternal()
        }

        override suspend fun clearErrorRecords() = withContext(Dispatchers.IO) {
            writableDatabase.delete("system_error_records", null, null)
            refreshErrorRecordsStateInternal()
        }

        override fun observeSelfHealingRepairs(): Flow<List<SelfHealingRepairRecord>> = selfHealingRepairsState.asStateFlow()

        override suspend fun getSelfHealingRepairs(): List<SelfHealingRepairRecord> = withContext(Dispatchers.IO) {
            querySelfHealingRepairsInternal()
        }

        override suspend fun getSelfHealingRepair(id: String): SelfHealingRepairRecord? = withContext(Dispatchers.IO) {
            querySelfHealingRepairByIdInternal(id)
        }

        override suspend fun insertOrUpdateRepairRecord(repair: SelfHealingRepairRecord) = withContext(Dispatchers.IO) {
            val rootCauseJson = repair.rootCauseAnalysis?.let { json.encodeToString(it) }
            val proposedPatchJson = repair.proposedPatch?.let { json.encodeToString(it) }
            val values = ContentValues().apply {
                put("id", repair.id)
                put("errorId", repair.errorId)
                put("state", repair.state.name)
                put("targetComponent", repair.targetComponent)
                put("targetFile", repair.targetFile)
                put("rootCauseJson", rootCauseJson)
                put("proposedPatchJson", proposedPatchJson)
                put("userApprovalStatus", repair.userApprovalStatus)
                put("preRepairCheckpointId", repair.preRepairCheckpointId)
                put("preRepairSnapshot", repair.preRepairSnapshot)
                put("verificationOutput", repair.verificationOutput)
                put("errorMessage", repair.errorMessage)
                put("createdAt", repair.createdAt)
                put("updatedAt", repair.updatedAt)
            }
            writableDatabase.insertWithOnConflict("self_healing_repairs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshSelfHealingRepairsStateInternal()
        }
    }

    init {
        ensureAutomationAndKnowledgeTables(writableDatabase)
        ensureAgentTables(writableDatabase)
        ensureHealingTables(writableDatabase)
        refreshMessagesStateInternal()
        refreshMemoryFactsStateInternal()
        refreshCacheCountStateInternal()
        refreshAutomationTasksStateInternal()
        refreshKnowledgeItemsStateInternal()
        refreshAgentTasksStateInternal()
        refreshErrorRecordsStateInternal()
        refreshSelfHealingRepairsStateInternal()
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
        ensureAgentTables(db)
        ensureHealingTables(db)
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
        // Safe database migration preserving existing user data, settings, mission history, and memories
        ensureAutomationAndKnowledgeTables(db)
        ensureAgentTables(db)
        ensureHealingTables(db)
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

    private fun ensureAgentTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS agent_tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                userObjective TEXT NOT NULL,
                executionPlanOverview TEXT NOT NULL,
                status TEXT NOT NULL,
                finalResponse TEXT,
                errorInformation TEXT,
                replanCount INTEGER NOT NULL,
                currentStepIndex INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                completionTime INTEGER,
                logsJson TEXT NOT NULL,
                changedFilesJson TEXT
            )
            """.trimIndent()
        )
        try {
            db.execSQL("ALTER TABLE agent_tasks ADD COLUMN changedFilesJson TEXT")
        } catch (_: Exception) {}
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS agent_steps (
                id TEXT PRIMARY KEY,
                taskId TEXT NOT NULL,
                stepNumber INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                toolId TEXT NOT NULL,
                toolArgumentsJson TEXT NOT NULL,
                requiresConfirmation INTEGER NOT NULL,
                status TEXT NOT NULL,
                actualResult TEXT,
                errorMessage TEXT,
                startTime INTEGER,
                completionTime INTEGER
            )
            """.trimIndent()
        )
    }

    private fun refreshAgentTasksStateInternal() {
        agentTasksState.value = queryAgentTasksInternal()
    }

    private fun queryAgentTasksInternal(): List<AgentTask> {
        val tasks = mutableListOf<AgentTask>()
        try {
            readableDatabase.query(
                "agent_tasks",
                null,
                null, null, null, null,
                "startTime DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val taskId = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val objective = cursor.getString(cursor.getColumnIndexOrThrow("userObjective"))
                    val overview = cursor.getString(cursor.getColumnIndexOrThrow("executionPlanOverview"))
                    val statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                    val status = try {
                        AgentTaskStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        AgentTaskStatus.COMPLETED
                    }
                    val finalResponse = if (cursor.isNull(cursor.getColumnIndexOrThrow("finalResponse"))) null else cursor.getString(cursor.getColumnIndexOrThrow("finalResponse"))
                    val errorInfo = if (cursor.isNull(cursor.getColumnIndexOrThrow("errorInformation"))) null else cursor.getString(cursor.getColumnIndexOrThrow("errorInformation"))
                    val replanCount = cursor.getInt(cursor.getColumnIndexOrThrow("replanCount"))
                    val currentStepIndex = cursor.getInt(cursor.getColumnIndexOrThrow("currentStepIndex"))
                    val startTime = cursor.getLong(cursor.getColumnIndexOrThrow("startTime"))
                    val completionTime = if (cursor.isNull(cursor.getColumnIndexOrThrow("completionTime"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("completionTime"))
                    val logsJsonIdx = cursor.getColumnIndex("logsJson")
                    val logsJson = if (logsJsonIdx != -1 && !cursor.isNull(logsJsonIdx)) cursor.getString(logsJsonIdx) else "[]"
                    val logs: List<String> = try {
                        json.decodeFromString(logsJson)
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val changedFilesIdx = cursor.getColumnIndex("changedFilesJson")
                    val changedFiles: List<String> = if (changedFilesIdx != -1 && !cursor.isNull(changedFilesIdx)) {
                        try {
                            json.decodeFromString(cursor.getString(changedFilesIdx))
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } else emptyList()

                    val steps = queryAgentStepsForTaskInternal(taskId)
                    tasks.add(
                        AgentTask(
                            id = taskId,
                            title = title,
                            userObjective = objective,
                            executionPlanOverview = overview,
                            steps = steps,
                            status = status,
                            finalResponse = finalResponse,
                            errorInformation = errorInfo,
                            replanCount = replanCount,
                            currentStepIndex = currentStepIndex,
                            startTime = startTime,
                            completionTime = completionTime,
                            logs = logs,
                            changedFiles = changedFiles
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return tasks
    }

    private fun queryAgentTaskByIdInternal(taskId: String): AgentTask? {
        try {
            readableDatabase.query(
                "agent_tasks",
                null,
                "id = ?", arrayOf(taskId), null, null, null
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val objective = cursor.getString(cursor.getColumnIndexOrThrow("userObjective"))
                    val overview = cursor.getString(cursor.getColumnIndexOrThrow("executionPlanOverview"))
                    val statusStr = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                    val status = try {
                        AgentTaskStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        AgentTaskStatus.COMPLETED
                    }
                    val finalResponse = if (cursor.isNull(cursor.getColumnIndexOrThrow("finalResponse"))) null else cursor.getString(cursor.getColumnIndexOrThrow("finalResponse"))
                    val errorInfo = if (cursor.isNull(cursor.getColumnIndexOrThrow("errorInformation"))) null else cursor.getString(cursor.getColumnIndexOrThrow("errorInformation"))
                    val replanCount = cursor.getInt(cursor.getColumnIndexOrThrow("replanCount"))
                    val currentStepIndex = cursor.getInt(cursor.getColumnIndexOrThrow("currentStepIndex"))
                    val startTime = cursor.getLong(cursor.getColumnIndexOrThrow("startTime"))
                    val completionTime = if (cursor.isNull(cursor.getColumnIndexOrThrow("completionTime"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("completionTime"))
                    val logsJsonIdx = cursor.getColumnIndex("logsJson")
                    val logsJson = if (logsJsonIdx != -1 && !cursor.isNull(logsJsonIdx)) cursor.getString(logsJsonIdx) else "[]"
                    val logs: List<String> = try {
                        json.decodeFromString(logsJson)
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val changedFilesIdx = cursor.getColumnIndex("changedFilesJson")
                    val changedFiles: List<String> = if (changedFilesIdx != -1 && !cursor.isNull(changedFilesIdx)) {
                        try {
                            json.decodeFromString(cursor.getString(changedFilesIdx))
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } else emptyList()

                    val steps = queryAgentStepsForTaskInternal(taskId)
                    return AgentTask(
                        id = taskId,
                        title = title,
                        userObjective = objective,
                        executionPlanOverview = overview,
                        steps = steps,
                        status = status,
                        finalResponse = finalResponse,
                        errorInformation = errorInfo,
                        replanCount = replanCount,
                        currentStepIndex = currentStepIndex,
                        startTime = startTime,
                        completionTime = completionTime,
                        logs = logs,
                        changedFiles = changedFiles
                    )
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun queryAgentStepsForTaskInternal(taskId: String): List<AgentStep> {
        val steps = mutableListOf<AgentStep>()
        try {
            readableDatabase.query(
                "agent_steps",
                arrayOf(
                    "id", "stepNumber", "title", "description",
                    "toolId", "toolArgumentsJson", "requiresConfirmation",
                    "status", "actualResult", "errorMessage",
                    "startTime", "completionTime"
                ),
                "taskId = ?", arrayOf(taskId), null, null,
                "stepNumber ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val stepNumber = cursor.getInt(1)
                    val title = cursor.getString(2)
                    val description = cursor.getString(3)
                    val toolId = cursor.getString(4)
                    val toolArgsJson = cursor.getString(5) ?: "{}"
                    val toolArgs: Map<String, String> = try {
                        json.decodeFromString(toolArgsJson)
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    val requiresConfirmation = cursor.getInt(6) != 0
                    val statusStr = cursor.getString(7)
                    val status = try {
                        AutonomousStepStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        AutonomousStepStatus.PENDING
                    }
                    val actualResult = if (cursor.isNull(8)) null else cursor.getString(8)
                    val errorMessage = if (cursor.isNull(9)) null else cursor.getString(9)
                    val startTime = if (cursor.isNull(10)) null else cursor.getLong(10)
                    val completionTime = if (cursor.isNull(11)) null else cursor.getLong(11)

                    steps.add(
                        AgentStep(
                            id = id,
                            stepNumber = stepNumber,
                            title = title,
                            description = description,
                            toolId = toolId,
                            toolArguments = toolArgs,
                            requiresConfirmation = requiresConfirmation,
                            status = status,
                            actualResult = actualResult,
                            errorMessage = errorMessage,
                            startTime = startTime,
                            completionTime = completionTime
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return steps
    }

    private fun ensureHealingTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS system_error_records (
                id TEXT PRIMARY KEY NOT NULL,
                category TEXT NOT NULL,
                message TEXT NOT NULL,
                affectedComponent TEXT NOT NULL,
                operation TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                severity TEXT NOT NULL,
                stackTrace TEXT,
                isTransient INTEGER NOT NULL,
                retryCount INTEGER NOT NULL,
                maxRetries INTEGER NOT NULL,
                suggestedRecovery TEXT NOT NULL,
                resolutionStatus TEXT NOT NULL,
                repairId TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS self_healing_repairs (
                id TEXT PRIMARY KEY NOT NULL,
                errorId TEXT NOT NULL,
                state TEXT NOT NULL,
                targetComponent TEXT NOT NULL,
                targetFile TEXT,
                rootCauseJson TEXT,
                proposedPatchJson TEXT,
                userApprovalStatus TEXT NOT NULL,
                preRepairCheckpointId TEXT,
                preRepairSnapshot TEXT,
                verificationOutput TEXT,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun refreshErrorRecordsStateInternal() {
        errorRecordsState.value = queryErrorRecordsInternal()
    }

    private fun queryErrorRecordsInternal(): List<SystemErrorRecord> {
        val list = mutableListOf<SystemErrorRecord>()
        try {
            readableDatabase.query(
                "system_error_records",
                arrayOf(
                    "id", "category", "message", "affectedComponent", "operation",
                    "timestamp", "severity", "stackTrace", "isTransient", "retryCount",
                    "maxRetries", "suggestedRecovery", "resolutionStatus", "repairId"
                ),
                null,
                null,
                null,
                null,
                "timestamp DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val catStr = cursor.getString(1)
                    val category = try { ErrorCategory.valueOf(catStr) } catch (_: Exception) { ErrorCategory.RUNTIME_EXCEPTION }
                    val message = cursor.getString(2)
                    val affectedComponent = cursor.getString(3)
                    val operation = cursor.getString(4)
                    val timestamp = cursor.getLong(5)
                    val sevStr = cursor.getString(6)
                    val severity = try { ErrorSeverity.valueOf(sevStr) } catch (_: Exception) { ErrorSeverity.MEDIUM }
                    val stackTrace = if (cursor.isNull(7)) null else cursor.getString(7)
                    val isTransient = cursor.getInt(8) != 0
                    val retryCount = cursor.getInt(9)
                    val maxRetries = cursor.getInt(10)
                    val suggestedRecovery = cursor.getString(11)
                    val resStr = cursor.getString(12)
                    val resolutionStatus = try { ResolutionStatus.valueOf(resStr) } catch (_: Exception) { ResolutionStatus.UNRESOLVED }
                    val repairId = if (cursor.isNull(13)) null else cursor.getString(13)

                    list.add(
                        SystemErrorRecord(
                            id = id,
                            category = category,
                            message = message,
                            affectedComponent = affectedComponent,
                            operation = operation,
                            timestamp = timestamp,
                            severity = severity,
                            stackTrace = stackTrace,
                            isTransient = isTransient,
                            retryCount = retryCount,
                            maxRetries = maxRetries,
                            suggestedRecovery = suggestedRecovery,
                            resolutionStatus = resolutionStatus,
                            repairId = repairId
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun queryErrorRecordByIdInternal(id: String): SystemErrorRecord? {
        try {
            readableDatabase.query(
                "system_error_records",
                arrayOf(
                    "id", "category", "message", "affectedComponent", "operation",
                    "timestamp", "severity", "stackTrace", "isTransient", "retryCount",
                    "maxRetries", "suggestedRecovery", "resolutionStatus", "repairId"
                ),
                "id = ?",
                arrayOf(id),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val catStr = cursor.getString(1)
                    val category = try { ErrorCategory.valueOf(catStr) } catch (_: Exception) { ErrorCategory.RUNTIME_EXCEPTION }
                    val message = cursor.getString(2)
                    val affectedComponent = cursor.getString(3)
                    val operation = cursor.getString(4)
                    val timestamp = cursor.getLong(5)
                    val sevStr = cursor.getString(6)
                    val severity = try { ErrorSeverity.valueOf(sevStr) } catch (_: Exception) { ErrorSeverity.MEDIUM }
                    val stackTrace = if (cursor.isNull(7)) null else cursor.getString(7)
                    val isTransient = cursor.getInt(8) != 0
                    val retryCount = cursor.getInt(9)
                    val maxRetries = cursor.getInt(10)
                    val suggestedRecovery = cursor.getString(11)
                    val resStr = cursor.getString(12)
                    val resolutionStatus = try { ResolutionStatus.valueOf(resStr) } catch (_: Exception) { ResolutionStatus.UNRESOLVED }
                    val repairId = if (cursor.isNull(13)) null else cursor.getString(13)

                    return SystemErrorRecord(
                        id = id,
                        category = category,
                        message = message,
                        affectedComponent = affectedComponent,
                        operation = operation,
                        timestamp = timestamp,
                        severity = severity,
                        stackTrace = stackTrace,
                        isTransient = isTransient,
                        retryCount = retryCount,
                        maxRetries = maxRetries,
                        suggestedRecovery = suggestedRecovery,
                        resolutionStatus = resolutionStatus,
                        repairId = repairId
                    )
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun refreshSelfHealingRepairsStateInternal() {
        selfHealingRepairsState.value = querySelfHealingRepairsInternal()
    }

    private fun querySelfHealingRepairsInternal(): List<SelfHealingRepairRecord> {
        val list = mutableListOf<SelfHealingRepairRecord>()
        try {
            readableDatabase.query(
                "self_healing_repairs",
                arrayOf(
                    "id", "errorId", "state", "targetComponent", "targetFile",
                    "rootCauseJson", "proposedPatchJson", "userApprovalStatus",
                    "preRepairCheckpointId", "preRepairSnapshot", "verificationOutput",
                    "errorMessage", "createdAt", "updatedAt"
                ),
                null,
                null,
                null,
                null,
                "createdAt DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val errorId = cursor.getString(1)
                    val stateStr = cursor.getString(2)
                    val state = try { RepairState.valueOf(stateStr) } catch (_: Exception) { RepairState.DETECTED }
                    val targetComponent = cursor.getString(3)
                    val targetFile = if (cursor.isNull(4)) null else cursor.getString(4)
                    val rootCauseJson = if (cursor.isNull(5)) null else cursor.getString(5)
                    val rootCause: RootCauseAnalysis? = rootCauseJson?.let {
                        try { json.decodeFromString<RootCauseAnalysis>(it) } catch (_: Exception) { null }
                    }
                    val proposedPatchJson = if (cursor.isNull(6)) null else cursor.getString(6)
                    val proposedPatch: CodeDiffPatch? = proposedPatchJson?.let {
                        try { json.decodeFromString<CodeDiffPatch>(it) } catch (_: Exception) { null }
                    }
                    val userApprovalStatus = cursor.getString(7)
                    val preRepairCheckpointId = if (cursor.isNull(8)) null else cursor.getString(8)
                    val preRepairSnapshot = if (cursor.isNull(9)) null else cursor.getString(9)
                    val verificationOutput = if (cursor.isNull(10)) null else cursor.getString(10)
                    val errorMessage = if (cursor.isNull(11)) null else cursor.getString(11)
                    val createdAt = cursor.getLong(12)
                    val updatedAt = cursor.getLong(13)

                    list.add(
                        SelfHealingRepairRecord(
                            id = id,
                            errorId = errorId,
                            state = state,
                            targetComponent = targetComponent,
                            targetFile = targetFile,
                            rootCauseAnalysis = rootCause,
                            proposedPatch = proposedPatch,
                            userApprovalStatus = userApprovalStatus,
                            preRepairCheckpointId = preRepairCheckpointId,
                            preRepairSnapshot = preRepairSnapshot,
                            verificationOutput = verificationOutput,
                            errorMessage = errorMessage,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun querySelfHealingRepairByIdInternal(id: String): SelfHealingRepairRecord? {
        try {
            readableDatabase.query(
                "self_healing_repairs",
                arrayOf(
                    "id", "errorId", "state", "targetComponent", "targetFile",
                    "rootCauseJson", "proposedPatchJson", "userApprovalStatus",
                    "preRepairCheckpointId", "preRepairSnapshot", "verificationOutput",
                    "errorMessage", "createdAt", "updatedAt"
                ),
                "id = ?",
                arrayOf(id),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val errorId = cursor.getString(1)
                    val stateStr = cursor.getString(2)
                    val state = try { RepairState.valueOf(stateStr) } catch (_: Exception) { RepairState.DETECTED }
                    val targetComponent = cursor.getString(3)
                    val targetFile = if (cursor.isNull(4)) null else cursor.getString(4)
                    val rootCauseJson = if (cursor.isNull(5)) null else cursor.getString(5)
                    val rootCause: RootCauseAnalysis? = rootCauseJson?.let {
                        try { json.decodeFromString<RootCauseAnalysis>(it) } catch (_: Exception) { null }
                    }
                    val proposedPatchJson = if (cursor.isNull(6)) null else cursor.getString(6)
                    val proposedPatch: CodeDiffPatch? = proposedPatchJson?.let {
                        try { json.decodeFromString<CodeDiffPatch>(it) } catch (_: Exception) { null }
                    }
                    val userApprovalStatus = cursor.getString(7)
                    val preRepairCheckpointId = if (cursor.isNull(8)) null else cursor.getString(8)
                    val preRepairSnapshot = if (cursor.isNull(9)) null else cursor.getString(9)
                    val verificationOutput = if (cursor.isNull(10)) null else cursor.getString(10)
                    val errorMessage = if (cursor.isNull(11)) null else cursor.getString(11)
                    val createdAt = cursor.getLong(12)
                    val updatedAt = cursor.getLong(13)

                    return SelfHealingRepairRecord(
                        id = id,
                        errorId = errorId,
                        state = state,
                        targetComponent = targetComponent,
                        targetFile = targetFile,
                        rootCauseAnalysis = rootCause,
                        proposedPatch = proposedPatch,
                        userApprovalStatus = userApprovalStatus,
                        preRepairCheckpointId = preRepairCheckpointId,
                        preRepairSnapshot = preRepairSnapshot,
                        verificationOutput = verificationOutput,
                        errorMessage = errorMessage,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                }
            }
        } catch (_: Exception) {
        }
        return null
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
