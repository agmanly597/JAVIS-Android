package com.javis.ai.domain

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

data class CacheEntry(
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * In-memory LRU cache for AI responses.
 * Skips caching for time-sensitive, personalized, or action-based queries.
 * Max 60 entries, 6-hour TTL — reduces AI API calls, speeds up repeat queries.
 */
@Singleton
class SmartResponseCache @Inject constructor() {

    private val TTL_MS = 6 * 60 * 60 * 1000L  // 6 hours
    private val MAX_SIZE = 60

    private val cache = object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>?) = size > MAX_SIZE
    }

    // These patterns should never be cached (time-sensitive or action-based)
    private val noCachePatterns = listOf(
        "what time", "what's the time", "current time",
        "today", "yesterday", "tomorrow", "this week",
        "my name", "call", "message", "send", "open",
        "alarm", "timer", "remind", "notification",
        "weather", "news", "latest", "update",
        "battery", "wifi", "bluetooth", "volume",
        "take photo", "screenshot", "navigate"
    )

    fun get(input: String): String? {
        val key = normalize(input)
        if (shouldSkip(key)) return null
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
            cache.remove(key)
            return null
        }
        Log.d("SmartCache", "Cache HIT for: ${key.take(40)}")
        return entry.response
    }

    fun put(input: String, response: String) {
        val key = normalize(input)
        if (shouldSkip(key)) return
        if (response.length < 10 || response.length > 2000) return
        cache[key] = CacheEntry(response)
        Log.d("SmartCache", "Cached response for: ${key.take(40)}")
    }

    fun clear() {
        cache.clear()
        Log.d("SmartCache", "Cache cleared")
    }

    fun size() = cache.size

    private fun normalize(input: String) = input
        .lowercase()
        .trim()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .replace(Regex("\\s+"), " ")

    private fun shouldSkip(normalizedKey: String) =
        noCachePatterns.any { normalizedKey.contains(it) }
}
