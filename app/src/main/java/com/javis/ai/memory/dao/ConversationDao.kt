package com.javis.ai.memory.dao

import androidx.room.*
import com.javis.ai.memory.entities.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: Conversation): Long

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversations(limit: Int = 100): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSession(sessionId: String): List<Conversation>

    @Query("DELETE FROM conversations WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentForExport(): List<Conversation>
}
