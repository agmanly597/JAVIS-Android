package com.javis.ai.domain

import android.util.Log
import com.javis.ai.ai.AIProviderManager
import com.javis.ai.ai.AIResult
import com.javis.ai.memory.MemoryManager
import javax.inject.Inject
import javax.inject.Singleton

data class JavisResponse(
    val spokenText: String,
    val pendingAction: PendingAction? = null,
    val isError: Boolean = false
)

@Singleton
class AgentRouter @Inject constructor(
    private val intentAnalyzer: IntentAnalyzer,
    private val taskPlanner: TaskPlanner,
    private val aiProviderManager: AIProviderManager,
    private val memoryManager: MemoryManager
) {
    suspend fun route(userInput: String): JavisResponse {
        Log.d("AgentRouter", "Routing: $userInput")

        val parsed = intentAnalyzer.analyze(userInput)
        Log.d("AgentRouter", "Intent: ${parsed.type}, app=${parsed.appTarget}, contact=${parsed.contactName}")

        val result = taskPlanner.execute(parsed)

        return when {
            result.pendingConfirmation != null -> {
                JavisResponse(
                    spokenText = result.spokenResponse,
                    pendingAction = result.pendingConfirmation
                )
            }
            result.success && !result.requiresAI -> {
                JavisResponse(spokenText = result.spokenResponse)
            }
            result.requiresAI || !result.success -> {
                val contextSummary = memoryManager.buildContextSummary()
                val aiInput = if (contextSummary.isNotEmpty() && parsed.type == IntentType.CHAT_AI) {
                    userInput
                } else {
                    userInput
                }
                val aiResult = if (contextSummary.isNotEmpty()) {
                    aiProviderManager.chatWithContext(aiInput, contextSummary)
                } else {
                    aiProviderManager.chat(aiInput)
                }
                when (aiResult) {
                    is AIResult.Success -> JavisResponse(spokenText = aiResult.response.text)
                    is AIResult.Error -> JavisResponse(
                        spokenText = "Sorry, I had trouble with that. ${aiResult.message}",
                        isError = true
                    )
                }
            }
            else -> JavisResponse(spokenText = result.spokenResponse)
        }
    }

    fun confirmPendingAction(action: PendingAction) {
        taskPlanner.confirmAction(action)
    }
}
