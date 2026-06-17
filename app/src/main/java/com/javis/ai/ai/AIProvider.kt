package com.javis.ai.ai

data class ChatMessage(
    val role: String,
    val content: String
)

data class AIResponse(
    val text: String,
    val provider: String,
    val tokensUsed: Int = 0
)

sealed class AIResult {
    data class Success(val response: AIResponse) : AIResult()
    data class Error(val message: String, val cause: Throwable? = null) : AIResult()
}

interface AIProvider {
    val name: String
    val isAvailable: Boolean
    suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): AIResult
    suspend fun checkAvailability(): Boolean
}

val JAVIS_SYSTEM_PROMPT = """
You are JAVIS, a personal AI assistant. You speak naturally, remember previous conversations, 
adapt to the user's habits, maintain context, and provide concise helpful responses. 
Avoid repetitive phrases. Learn common user routines and preferences.
You are confident, friendly, humorous when appropriate, and never robotic.
When performing device actions, be concise. When chatting, be natural.
Keep responses short unless detail is specifically requested.
Never start a response with "Certainly!", "Of course!", or similar filler phrases.
""".trimIndent()
