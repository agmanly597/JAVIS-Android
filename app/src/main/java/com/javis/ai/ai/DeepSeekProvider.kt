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
class DeepSeekProvider @Inject constructor() : AIProvider {

    override val name = "DeepSeek"
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

    override suspend fun checkAvailability(): Boolean = try {
        val result = chat(listOf(ChatMessage("user", "Hi")), "Say only: ok")
        result is AIResult.Success
    } catch (e: Exception) { false }

    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): AIResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = mutableListOf<Map<String, String>>()
                payload.add(mapOf("role" to "system", "content" to systemPrompt))
                messages.forEach { payload.add(mapOf("role" to it.role, "content" to it.content)) }

                val requestMap = mapOf(
                    "model" to "deepseek-chat",
                    "messages" to payload,
                    "max_tokens" to 512,
                    "temperature" to 0.7
                )

                val json = gson.toJson(requestMap)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.deepseek.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext AIResult.Error("DeepSeek API error: ${response.code}")
                }

                val responseBody = response.body?.string() ?: ""
                val parsed = gson.fromJson(responseBody, Map::class.java)

                @Suppress("UNCHECKED_CAST")
                val choices = parsed["choices"] as? List<Map<String, Any>>
                val message = choices?.firstOrNull()?.get("message") as? Map<String, String>
                val text = message?.get("content")?.trim()
                    ?: return@withContext AIResult.Error("Empty response from DeepSeek")

                AIResult.Success(AIResponse(text = text, provider = name))
            } catch (e: Exception) {
                Log.e("DeepSeekProvider", "Chat error", e)
                AIResult.Error(e.message ?: "Unknown error", e)
            }
        }
}
