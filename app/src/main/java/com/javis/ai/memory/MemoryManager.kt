package com.javis.ai.memory

import com.javis.ai.memory.entities.AppUsage
import com.javis.ai.memory.entities.Conversation
import com.javis.ai.memory.entities.UserMemory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    private val db: JavisDatabase
) {
    // Conversation history
    fun getRecentConversations(limit: Int = 100): Flow<List<Conversation>> =
        db.conversationDao().getRecentConversations(limit)

    suspend fun saveMessage(role: String, content: String, sessionId: String = "", provider: String = "") {
        db.conversationDao().insert(
            Conversation(role = role, content = content, sessionId = sessionId, provider = provider)
        )
    }

    suspend fun clearConversationHistory() = db.conversationDao().clearAll()

    suspend fun pruneOldConversations(keepDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        db.conversationDao().deleteOlderThan(cutoff)
    }

    // User memory
    suspend fun remember(key: String, value: String, category: String = UserMemory.CAT_GENERAL) {
        db.userMemoryDao().upsert(UserMemory(key = key, value = value, category = category))
    }

    suspend fun recall(key: String): String? = db.userMemoryDao().get(key)?.value

    fun getAllMemories(): Flow<List<UserMemory>> = db.userMemoryDao().getAll()

    fun getMemoriesByCategory(category: String): Flow<List<UserMemory>> =
        db.userMemoryDao().getByCategory(category)

    suspend fun forget(key: String) = db.userMemoryDao().delete(key)

    suspend fun clearAllMemory() = db.userMemoryDao().clearAll()

    // App usage tracking
    suspend fun recordAppLaunch(packageName: String, appName: String) {
        val existing = db.appUsageDao().getByPackage(packageName)
        if (existing == null) {
            db.appUsageDao().upsert(AppUsage(packageName = packageName, appName = appName, launchCount = 1))
        } else {
            db.appUsageDao().incrementLaunch(packageName)
        }
    }

    fun getTopApps(limit: Int = 10): Flow<List<AppUsage>> = db.appUsageDao().getTopApps(limit)

    // Convenience: build context string for AI
    suspend fun buildContextSummary(): String {
        val name = recall("user_name") ?: "unknown"
        val prefs = recall("preferences_summary") ?: ""
        val routines = recall("routines_summary") ?: ""
        return buildString {
            if (name != "unknown") append("User's name: $name. ")
            if (prefs.isNotEmpty()) append("Preferences: $prefs. ")
            if (routines.isNotEmpty()) append("Routines: $routines.")
        }
    }
}

private const val CAT_GENERAL = "general"
