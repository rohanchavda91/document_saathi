package com.rohan.documentsaathi.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Converts a bitmap into an A4 PDF and saves it to internal storage.
     * Returns the absolute path of the generated file.
     */
    fun generatePdfFromBitmap(bitmap: Bitmap): String? {
        val pdfDocument = PdfDocument()
        
        // Standard A4 size in pixels at 72 DPI (595x842)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Calculate scaling to fit image within A4 while keeping aspect ratio
        val maxWidth = 595f - 40f // Margin of 20 on each side
        val maxHeight = 842f - 40f
        
        val scale = Math.min(maxWidth / bitmap.width, maxHeight / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        
        val left = (595f - scaledWidth) / 2f
        val top = (842f - scaledHeight) / 2f

        canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), Paint())

        pdfDocument.finishPage(page)

        // Save to internal storage
        val directory = File(context.filesDir, "documents")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "DOC_${System.currentTimeMillis()}.pdf"
        val file = File(directory, fileName)

        return try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            file.absolutePath
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
