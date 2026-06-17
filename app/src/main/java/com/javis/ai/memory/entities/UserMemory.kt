package com.javis.ai.memory.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class UserMemory(
    @PrimaryKey
    val key: String,
    val value: String,
    val category: String = "general",
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CAT_PREFERENCE = "preference"
        const val CAT_PERSONAL = "personal"
        const val CAT_ROUTINE = "routine"
        const val CAT_CONTACT = "contact"
        const val CAT_WEBSITE = "website"
    }
}
