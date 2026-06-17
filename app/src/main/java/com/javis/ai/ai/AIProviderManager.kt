package com.javis.ai.ai

import android.util.Log
import com.javis.ai.settings.SettingsManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class ProviderType { GROQ, DEEPSEEK }

@Singleton
class AIProviderManager @Inject constructor(
    private val groqProvider: GroqProvider,
    private val deepSeekProvider: DeepSeekProvider,
    private val settingsManager: SettingsManager
) {
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val maxHistorySize = 20

    suspend fun configure() {
        val settings = settingsManager.settings.first()
        groqProvider.configure(settings.groqApiKey)
        deepSeekProvider.configure(settings.deepSeekApiKey)
    }

    private fun getProviders(): List<AIProvider> {
        val settings = kotlinx.coroutines.runBlocking { settingsManager.settings.first() }
        val primary = when (settings.preferredProvider) {
            ProviderType.GROQ -> groqProvider
            ProviderType.DEEPSEEK -> deepSeekProvider
        }
        val fallback = when (settings.preferredProvider) {
            ProviderType.GROQ -> deepSeekProvider
            ProviderType.DEEPSEEK -> groqProvider
        }
        return listOf(primary, fallback).filter { it.isAvailable }
    }

    suspend fun chat(userMessage: String): AIResult {
        conversationHistory.add(ChatMessage("user", userMessage))
        if (conversationHistory.size > maxHistorySize) {
            conversationHistory.removeAt(0)
        }

        val providers = getProviders()
        if (providers.isEmpty()) {
            return AIResult.Error("No AI provider configured. Please add an API key in Settings.")
        }

        for (provider in providers) {
            Log.d("AIProviderManager", "Trying provider: ${provider.name}")
            val result = provider.chat(conversationHistory.toList(), JAVIS_SYSTEM_PROMPT)
            if (result is AIResult.Success) {
                conversationHistory.add(ChatMessage("assistant", result.response.text))
                return result
            }
            Log.w("AIProviderManager", "Provider ${provider.name} failed, trying next...")
        }

        return AIResult.Error("All AI providers failed. Please check your internet and API keys.")
    }

    suspend fun chatWithContext(userMessage: String, contextNote: String): AIResult {
        val systemPrompt = "$JAVIS_SYSTEM_PROMPT\n\nContext: $contextNote"
        val messages = conversationHistory.toMutableList()
        messages.add(ChatMessage("user", userMessage))

        val providers = getProviders()
        if (providers.isEmpty()) {
            return AIResult.Error("No AI provider configured.")
        }

        for (provider in providers) {
            val result = provider.chat(messages, systemPrompt)
            if (result is AIResult.Success) {
                conversationHistory.add(ChatMessage("user", userMessage))
                conversationHistory.add(ChatMessage("assistant", result.response.text))
                return result
            }
        }
        return AIResult.Error("All providers failed.")
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun getHistory(): List<ChatMessage> = conversationHistory.toList()
}
