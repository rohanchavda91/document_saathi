package com.rohan.documentsaathi.data.db.entity

import android.net.Uri
import android.view.inputmethod.ExtractedText
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.intellij.lang.annotations.Language
import java.util.Date

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val extractedText: String,
    val detectedLanguage: String, // "en", "hi"
    val createdAt: Long=System.currentTimeMillis(),
    val imageUri: String?=null, // location or path to the cached image
    val isSummarized: Boolean = false,
    val summary: String? = null,
    val summaryLanguage: String?=null, // Language in which the summary will be given
    val structuredDataJson: String? = null, // JSON string containing extracted fields
    val pdfUri: String? = null // Path to the permanent PDF file
)