package com.rohan.documentsaathi.feature.ocr.manager

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OcrManager handles on-device text recognition using ML Kit.
 *
 * Strategy: Dual recognizer approach
 * - Latin recognizer for English text
 * - Devanagari recognizer for Hindi text
 * - Result selection: Choose recognizer output with longer text length
 *   (indicates higher confidence/completeness)
 */
@Singleton
class OcrManager @Inject constructor() {

    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    /**
     * Process bitmap and extract text using dual recognizer strategy.
     *
     * @param bitmap Input image bitmap from camera/gallery
     * @return Extracted text (English or Hindi, whichever is more complete)
     * @throws Exception if ML Kit processing fails
     */
    suspend fun recognizeText(bitmap: Bitmap): String = try {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Run both recognizers in parallel
        val latinTask = latinRecognizer.process(inputImage)
        val devanagariTask = devanagariRecognizer.process(inputImage)

        val latinResult = latinTask.await().text
        val devanagariResult = devanagariTask.await().text

        // Length-based selection: choose the longer result
        // Assumption: longer text = higher confidence/more content detected
        if (latinResult.length >= devanagariResult.length) {
            latinResult
        } else {
            devanagariResult
        }
    } catch (e: Exception) {
        throw OcrException("Text recognition failed: ${e.message}", e)
    }

    /**
     * Process bitmap with debug output (both recognizer results).
     * Useful for testing/viva demonstration.
     *
     * @param bitmap Input image
     * @return Pair of (latinText, devanagariText)
     */
    suspend fun recognizeTextDebug(bitmap: Bitmap): Pair<String, String> = try {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val latinTask = latinRecognizer.process(inputImage)
        val devanagariTask = devanagariRecognizer.process(inputImage)

        val latinResult = latinTask.await().text
        val devanagariResult = devanagariTask.await().text

        Pair(latinResult, devanagariResult)
    } catch (e: Exception) {
        throw OcrException("Debug text recognition failed: ${e.message}", e)
    }

    /**
     * Release resources when OcrManager is no longer needed.
     */
    fun release() {
        try {
            latinRecognizer.close()
            devanagariRecognizer.close()
        } catch (e: Exception) {
            // Log or handle quietly
        }
    }
}

/**
 * Custom exception for OCR operations.
 */
class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)
