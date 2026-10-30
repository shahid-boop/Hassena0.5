package com.example.services.cloud

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

data class CloudMemoryFact(
    val id: String = "",
    val userId: String = "local_user",
    val category: String = "FACT",
    val content: String = "",
    val source: String = "User",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class CloudChatMessage(
    val id: String = "",
    val userId: String = "local_user",
    val role: String = "user",
    val content: String = "",
    val providerName: String = "Direct Input",
    val modelName: String = "User",
    val routeCategory: String = "TEXT",
    val latencyMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

class CloudSyncRepository(context: Context) {

    private val localUserId: String = "local_device_user"

    fun observeCloudMemories(): Flow<List<CloudMemoryFact>> = callbackFlow {
        // Direct local access: Emits empty list for cloud sync as app operates 100% offline-first
        trySend(emptyList())
        awaitClose { }
    }

    suspend fun createCloudMemory(
        category: String,
        content: String,
        source: String = "User"
    ): Result<String> {
        val docId = "local_mem_${UUID.randomUUID().toString().replace("-", "")}"
        return Result.success(docId)
    }

    suspend fun getUserCloudMemories(): Result<List<CloudMemoryFact>> {
        return Result.success(emptyList())
    }

    suspend fun getCloudMemoryById(ownerUid: String, memoryId: String): Result<CloudMemoryFact> {
        val item = CloudMemoryFact(
            id = memoryId,
            userId = localUserId,
            category = "FACT",
            content = "Local Memory",
            source = "User"
        )
        return Result.success(item)
    }

    suspend fun deleteCloudMemory(memoryId: String): Result<Unit> {
        return Result.success(Unit)
    }

    fun observeCloudMessages(): Flow<List<CloudChatMessage>> = callbackFlow {
        trySend(emptyList())
        awaitClose { }
    }

    suspend fun createCloudMessage(
        role: String,
        content: String,
        providerName: String,
        modelName: String,
        routeCategory: String,
        latencyMs: Long
    ): Result<String> {
        val docId = "local_msg_${UUID.randomUUID().toString().replace("-", "")}"
        return Result.success(docId)
    }
}
