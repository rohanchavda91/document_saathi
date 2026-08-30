package com.rohan.documentsaathi.feature.document.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.documentsaathi.core.ai.SummarizationManager
import com.rohan.documentsaathi.core.utils.ImageManager
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val summarizationManager: SummarizationManager,
    private val imageManager: ImageManager
): ViewModel(){

    private val _documentState = MutableStateFlow<Document?>(null)
    val documentState: StateFlow<Document?> = _documentState.asStateFlow()

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val document = documentRepository.getDocumentById(documentId)
            _documentState.value = document
            
            if (document != null) {
                if (document.structuredDataJson == null) {
                    fetchStructuredData(document)
                }
                if (document.summary == null) {
                    fetchSummary(document)
                }
            }
        }
    }

    private fun fetchStructuredData(document: Document) {
        viewModelScope.launch {
            val bitmap = document.imageUri?.let { imageManager.loadBitmap(it) }
            if (bitmap != null) {
                val result = summarizationManager.extractDocumentInfo(bitmap, document.extractedText)
                result.onSuccess { json ->
                    val currentDoc = _documentState.value ?: document
                    val updatedDoc = currentDoc.copy(structuredDataJson = json)
                    documentRepository.updateDocument(updatedDoc)
                    _documentState.value = updatedDoc
                }
            }
        }
    }

    private fun fetchSummary(document: Document) {
        viewModelScope.launch {
            val result = summarizationManager.summarizeText(document.extractedText)
            result.onSuccess { summary ->
                val currentDoc = _documentState.value ?: document
                val updatedDoc = currentDoc.copy(summary = summary, isSummarized = true)
                documentRepository.updateDocument(updatedDoc)
                _documentState.value = updatedDoc
            }
        }
    }

    suspend fun getDocumentById(documentId: Long): Document?{
        return documentRepository.getDocumentById(documentId)
    }

    fun deleteDocument(documentId: Long){
        viewModelScope.launch {
            val document = documentRepository.getDocumentById(documentId)
            if (document!=null){
                documentRepository.deleteDocument(document)
            }
        }
    }
}