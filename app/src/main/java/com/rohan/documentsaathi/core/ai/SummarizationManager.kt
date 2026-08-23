package com.rohan.documentsaathi.core.ai

import android.util.Log
import com.google.gson.Gson
import com.rohan.documentsaathi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummarizationManager @Inject constructor() {
    
    private val TAG = "SummarizationManager"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val API_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val MODEL_NAME = "meta/llama-3.1-8b-instruct"

    suspend fun summarizeText(
        text: String,
        sourceLanguage: String = "ENGLISH",
        targetLanguage: String = "ENGLISH"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Summarizing text using PaliGemma via Nvidia. Target: $targetLanguage")
            
            val prompt = buildPrompt(text, sourceLanguage, targetLanguage)
            
            val requestBody = mapOf(
                "model" to MODEL_NAME,
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                ),
                "temperature" to 0.5,
                "top_p" to 0.7,
                "max_tokens" to 1024,
                "stream" to false
            )

            val jsonBody = gson.toJson(requestBody)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Nvidia API Error: ${response.code} - $responseBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }

                if (responseBody == null) {
                    return@withContext Result.failure(Exception("Empty response from server"))
                }

                val apiResponse = gson.fromJson(responseBody, NvidiaResponse::class.java)
                val summary = apiResponse.choices?.getOrNull(0)?.message?.content
                    ?: "Failed to parse summary from response"
                
                Log.d(TAG, "Summary generated successfully")
                Result.success(summary.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary", e)
            Result.failure(e)
        }
    }
    
    private fun buildPrompt(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        return """
            You are a professional document summarizer. 
            
            Please summarize the following text in $targetLanguage language.
            The text is originally in $sourceLanguage.
            
            Keep the summary concise (150-200 words) but comprehensive.
            Focus on key points and important information.
            
            Text to summarize:
            ---
            $text
            ---
            
            Provide only the summary content, without any preamble, bold headers, or explanation.
        """.trimIndent()
    }
    
    fun getSupportedSummaryLanguages(): List<SummaryLanguage> {
        return listOf(
            SummaryLanguage("ENGLISH", "English"),
            SummaryLanguage("HINDI", "हिंदी"),
            SummaryLanguage("GUJARATI", "ગુજરાતી")
        )
    }
    
    data class SummaryLanguage(val code: String, val displayName: String)

    // Response models for Gson
    private data class NvidiaResponse(
        val choices: List<Choice>?
    )

    private data class Choice(
        val message: Message?
    )

    private data class Message(
        val content: String?
    )
}
