package com.javis.ai.memory.dao

import androidx.room.*
import com.javis.ai.memory.entities.AppUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: AppUsage)

    @Query("SELECT * FROM app_usage ORDER BY launchCount DESC LIMIT :limit")
    fun getTopApps(limit: Int = 10): Flow<List<AppUsage>>

    @Query("SELECT * FROM app_usage WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppUsage?

    @Query("UPDATE app_usage SET launchCount = launchCount + 1, lastLaunched = :time WHERE packageName = :pkg")
    suspend fun incrementLaunch(pkg: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_usage")
    suspend fun clearAll()
}
