package com.rohan.documentsaathi.core.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.rohan.documentsaathi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummarizationManager @Inject constructor() {
    
    private val TAG = "SummarizationManager"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val API_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val MODEL_NAME = "google/diffusiongemma-26b-a4b-it"

    suspend fun extractDocumentInfo(bitmap: Bitmap, ocrText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting structured data using PaliGemma (VLM)")
            
            // Convert bitmap to Base64
            val base64Image = encodeImageToBase64(bitmap)
            
            val prompt = """
                Task: You are an expert document analyzer. 
                Identify the document type (Aadhar, PAN, License, etc.) and extract all details.
                
                Input OCR Text for reference: $ocrText
                
                Return a JSON object with:
                - "document_type": (e.g. Aadhar Card, PAN Card, Driving License)
                - "id_number": (The main ID number)
                - "holder_name": (Name of person)
                - "dob": (Date of Birth if found)
                - "address": (Full address if found)
                
                Return ONLY the JSON. No other text.
            """.trimIndent()
            
            val requestBody = mapOf(
                "model" to MODEL_NAME,
                "messages" to listOf(
                    mapOf(
                        "role" to "user", 
                        "content" to listOf(
                            mapOf("type" to "text", "text" to prompt),
                            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image"))
                        )
                    )
                ),
                "max_tokens" to 1024,
                "temperature" to 0.2,
                "top_p" to 0.7
            )

            val jsonBody = gson.toJson(requestBody)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d(TAG, "VLM API Response: ${responseBody}")
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "PaliGemma API Error: ${response.code} - $responseBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }

                if (responseBody == null) return@withContext Result.failure(Exception("Empty response"))

                val apiResponse = gson.fromJson(responseBody, NvidiaResponse::class.java)
                val extraction = apiResponse.choices?.getOrNull(0)?.message?.content
                    ?: return@withContext Result.failure(Exception("Parse error"))
                
                val cleaned = extraction.trim().removePrefix("```json").removeSuffix("```").trim()
                Result.success(cleaned)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in VLM extraction", e)
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress image to JPEG and reduce size to stay within API limits
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun summarizeText(
        text: String,
        sourceLanguage: String = "ENGLISH",
        targetLanguage: String = "ENGLISH"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Summarizing text using PaliGemma. Target: $targetLanguage")
            
            val prompt = """
                Summarize the following document in $targetLanguage.
                Text: $text
                
                Keep it concise (150 words). Provide only the summary.
            """.trimIndent()
            
            val requestBody = mapOf(
                "model" to MODEL_NAME,
                "messages" to listOf(
                    mapOf(
                        "role" to "user", 
                        "content" to listOf(
                            mapOf("type" to "text", "text" to prompt)
                        )
                    )
                ),
                "max_tokens" to 1024,
                "temperature" to 0.5
            )

            val jsonBody = gson.toJson(requestBody)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d(TAG, "Full API Response: ${responseBody}")
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "PaliGemma API Error: ${response.code} - $responseBody")
                    return@withContext Result.failure(Exception("API Error: ${response.code}"))
                }

                val apiResponse = gson.fromJson(responseBody, NvidiaResponse::class.java)
                val summary = apiResponse.choices?.getOrNull(0)?.message?.content
                    ?: "Failed to parse summary"
                
                Result.success(summary.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary", e)
            Result.failure(e)
        }
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
