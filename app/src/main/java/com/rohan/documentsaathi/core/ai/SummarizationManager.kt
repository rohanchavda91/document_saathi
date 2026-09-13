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
            Log.d(TAG, "Extracting structured data using fast AI extraction")
            
            // Downscale bitmap to max 800px to keep payload size tiny (~80KB) for ultra-fast API response
            val downscaled = downscaleBitmap(bitmap, 800)
            val base64Image = encodeImageToBase64(downscaled)
            
            val prompt = """
                Task: Analyze the document OCR text and image.
                Identify the document type (e.g. PAN Card, Aadhar Card, Driving License, Passport, Credit/Debit Card, Resume, Marksheet, Invoice/Receipt, Document).
                
                OCR Text:
                $ocrText
                
                Rules for JSON Output:
                1. Always include key "document_type" with a clean name (e.g., "PAN Card", "Aadhar Card", "Driving License", "Debit Card", "Resume", "Receipt").
                2. For PAN Card: include "pan_number", "holder_name", "father_name", "dob".
                3. For Aadhar Card: include "aadhar_number", "holder_name", "dob", "gender", "address".
                4. For Driving License: include "dl_number", "holder_name", "dob", "validity", "address".
                5. For Cards: include "card_number", "card_holder", "expiry_date".
                6. For other documents: include "title", "name", "id_number", "date", "additional_info".
                7. Output ONLY a valid JSON object. Do NOT include markdown code blocks or introductory text.
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
                
                val cleanedJson = sanitizeJson(extraction)
                Result.success(cleanedJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in VLM extraction", e)
            Result.failure(e)
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (maxDimension / aspectRatio).toInt()
        } else {
            (maxDimension * aspectRatio).toInt() to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun sanitizeJson(input: String): String {
        var text = input.trim()
        if (text.startsWith("```")) {
            text = text.replace("^```[a-zA-Z]*".toRegex(), "").removeSuffix("```").trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1).trim()
        } else {
            text
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
