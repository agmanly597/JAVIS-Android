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
    val isError: Boolean = false,
    val fromCache: Boolean = false
)

@Singleton
class AgentRouter @Inject constructor(
    private val intentAnalyzer: IntentAnalyzer,
    private val taskPlanner: TaskPlanner,
    private val aiProviderManager: AIProviderManager,
    private val memoryManager: MemoryManager,
    private val cache: SmartResponseCache,
    private val contextEngine: ContextEngine
) {
    suspend fun route(userInput: String): JavisResponse {
        Log.d("AgentRouter", "Routing: $userInput")

        // Fast path: check cache first for chat queries
        val cached = cache.get(userInput)
        if (cached != null) {
            Log.d("AgentRouter", "Returning cached response")
            return JavisResponse(spokenText = cached, fromCache = true)
        }

        val parsed = intentAnalyzer.analyze(userInput)
        Log.d("AgentRouter", "Intent: ${parsed.type}, app=${parsed.appTarget}, contact=${parsed.contactName}")

        val result = taskPlanner.execute(parsed)

        return when {
            result.pendingConfirmation != null -> JavisResponse(
                spokenText = result.spokenResponse,
                pendingAction = result.pendingConfirmation
            )

            result.success && !result.requiresAI -> JavisResponse(spokenText = result.spokenResponse)

            result.requiresAI || !result.success -> {
                val contextSummary = memoryManager.buildContextSummary()
                val liveContext = contextEngine.buildContext()

                val systemContext = buildString {
                    if (contextSummary.isNotEmpty()) append("User memory: $contextSummary\n")
                    append("$liveContext\n")
                    append("You are JAVIS, a smart personal AI assistant on Android. ")
                    append("Give short, direct, actionable responses. ")
                    append("You know the user's current context above — use it naturally.")
                }

                val aiResult = aiProviderManager.chatWithContext(userInput, systemContext)
                when (aiResult) {
                    is AIResult.Success -> {
                        val response = aiResult.response.text
                        cache.put(userInput, response)
                        JavisResponse(spokenText = response)
                    }
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
