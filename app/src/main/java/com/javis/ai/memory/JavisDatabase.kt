package com.javis.ai.memory

import androidx.room.Database
import androidx.room.RoomDatabase
import com.javis.ai.memory.dao.AppUsageDao
import com.javis.ai.memory.dao.ConversationDao
import com.javis.ai.memory.dao.UserMemoryDao
import com.javis.ai.memory.entities.AppUsage
import com.javis.ai.memory.entities.Conversation
import com.javis.ai.memory.entities.UserMemory

@Database(
    entities = [Conversation::class, UserMemory::class, AppUsage::class],
    version = 1,
    exportSchema = false
)
abstract class JavisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun appUsageDao(): AppUsageDao
}
