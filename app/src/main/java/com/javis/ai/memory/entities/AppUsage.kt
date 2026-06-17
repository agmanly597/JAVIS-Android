package com.javis.ai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val launchCount: Int = 0,
    val lastLaunched: Long = System.currentTimeMillis()
)
