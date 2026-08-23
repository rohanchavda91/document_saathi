package com.rohan.documentsaathi.feature.summarization

/**
 * Represents a specific field extracted from a document by the AI.
 * Example: Key = "Invoice Number", Value = "INV-2024-001"
 */
data class DocumentField(
    val key: String,
    val value: String
)
