package com.javis.ai.memory.dao

import androidx.room.*
import com.javis.ai.memory.entities.UserMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: UserMemory)

    @Query("SELECT * FROM user_memory WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): UserMemory?

    @Query("SELECT * FROM user_memory WHERE category = :category")
    fun getByCategory(category: String): Flow<List<UserMemory>>

    @Query("SELECT * FROM user_memory")
    fun getAll(): Flow<List<UserMemory>>

    @Query("DELETE FROM user_memory WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM user_memory")
    suspend fun clearAll()
}
