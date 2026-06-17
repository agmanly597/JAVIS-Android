package com.javis.ai.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqProvider @Inject constructor() : AIProvider {

    override val name = "Groq (Llama 3)"
    private var apiKey: String = ""
    private var _isAvailable = false
    override val isAvailable get() = _isAvailable && apiKey.isNotEmpty()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun configure(apiKey: String) {
        this.apiKey = apiKey
        _isAvailable = apiKey.isNotEmpty()
    }

    override suspend fun checkAvailability(): Boolean {
        return try {
            val result = chat(
                listOf(ChatMessage("user", "Hi")),
                "Say only: ok"
            )
            result is AIResult.Success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): AIResult =
        withContext(Dispatchers.IO) {
            try {
                val groqMessages = mutableListOf<GroqMessage>()
                groqMessages.add(GroqMessage("system", systemPrompt))
                messages.forEach { groqMessages.add(GroqMessage(it.role, it.content)) }

                val requestBody = GroqRequest(
                    model = "llama3-8b-8192",
                    messages = groqMessages,
                    maxTokens = 512,
                    temperature = 0.7f,
                    stream = false
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext AIResult.Error("Groq API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val groqResponse = gson.fromJson(responseBody, GroqResponse::class.java)
                val text = groqResponse.choices?.firstOrNull()?.message?.content?.trim()
                    ?: return@withContext AIResult.Error("Empty response from Groq")

                AIResult.Success(
                    AIResponse(
                        text = text,
                        provider = name,
                        tokensUsed = groqResponse.usage?.totalTokens ?: 0
                    )
                )
            } catch (e: Exception) {
                Log.e("GroqProvider", "Chat error", e)
                AIResult.Error(e.message ?: "Unknown error", e)
            }
        }

    private data class GroqMessage(val role: String, val content: String)
    private data class GroqRequest(
        val model: String,
        val messages: List<GroqMessage>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Float,
        val stream: Boolean
    )
    private data class GroqResponse(
        val choices: List<Choice>?,
        val usage: Usage?
    )
    private data class Choice(val message: GroqMessage?)
    private data class Usage(@SerializedName("total_tokens") val totalTokens: Int)
}
