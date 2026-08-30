package com.rohan.documentsaathi.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveBitmap(bitmap: Bitmap): String? {
        val directory = File(context.filesDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val file = File(directory, fileName)

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
}
