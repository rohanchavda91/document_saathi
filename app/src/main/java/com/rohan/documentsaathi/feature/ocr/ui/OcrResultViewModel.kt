package com.rohan.documentsaathi.feature.ocr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.documentsaathi.core.ai.SummarizationManager
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import com.rohan.documentsaathi.feature.summarization.DocumentField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrResultViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val summarizationManager: SummarizationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrResultUiState>(OcrResultUiState.Idle)
    val uiState: StateFlow<OcrResultUiState> = _uiState.asStateFlow()
    
    private val _summaryLanguages = MutableStateFlow<List<SummarizationManager.SummaryLanguage>>(emptyList())
    val summaryLanguages: StateFlow<List<SummarizationManager.SummaryLanguage>> = _summaryLanguages.asStateFlow()

    init {
        _summaryLanguages.value = summarizationManager.getSupportedSummaryLanguages()
    }

    fun saveDocument(
        extractedText: String,
        detectedLanguage: String = "ENGLISH"
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = OcrResultUiState.Loading
                val document = Document(
                    extractedText = extractedText,
                    detectedLanguage = detectedLanguage
                )
                val documentId = documentRepository.saveDocument(document)
                _uiState.value = OcrResultUiState.DocumentSaved(documentId)
            } catch (e: Exception) {
                _uiState.value = OcrResultUiState.Error(e.message ?: "Failed to save document")
            }
        }
    }

    fun summarizeText(
        text: String,
        sourceLanguage: String = "ENGLISH",
        targetLanguage: String = "ENGLISH"
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = OcrResultUiState.SummarizationLoading
                val result = summarizationManager.summarizeText(text, sourceLanguage, targetLanguage)
                result.onSuccess { summary ->
                    _uiState.value = OcrResultUiState.SummarizationSuccess(
                        summary = summary,
                        summaryLanguage = targetLanguage
                    )
                }.onFailure { exception ->
                    _uiState.value = OcrResultUiState.Error(
                        exception.message ?: "Summarization failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OcrResultUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun callDeepSeekSummarization(
        extractedText: String,
        language: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = OcrResultUiState.SummarizationLoading
                // Using existing summarization logic for now
                val result = summarizationManager.summarizeText(extractedText, targetLanguage = language)
                result.onSuccess { summary ->
                    // Here we would ideally parse the structured JSON from DeepSeek
                    // For now, we create a placeholder list of fields
                    _uiState.value = OcrResultUiState.SummarizationFieldsSuccess(
                        documentType = "Detected Document",
                        fields = listOf(
                            DocumentField("Full Summary", summary),
                            DocumentField("Language", language)
                        )
                    )
                }.onFailure { exception ->
                    _uiState.value = OcrResultUiState.Error(exception.message ?: "AI Processing failed")
                }
            } catch (e: Exception) {
                _uiState.value = OcrResultUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateDocumentWithSummary(
        documentId: Long,
        summary: String,
        summaryLanguage: String
    ) {
        viewModelScope.launch {
            try {
                val document = documentRepository.getDocumentById(documentId)
                if (document != null) {
                    val updatedDocument = document.copy(
                        isSummarized = true,
                        summary = summary,
                        summaryLanguage = summaryLanguage
                    )
                    documentRepository.updateDocument(updatedDocument)
                }
            } catch (e: Exception) {
                _uiState.value = OcrResultUiState.Error(e.message ?: "Failed to update document")
            }
        }
    }
}

sealed class OcrResultUiState {
    object Idle : OcrResultUiState()
    object Loading : OcrResultUiState()
    object SummarizationLoading : OcrResultUiState()
    data class DocumentSaved(val documentId: Long) : OcrResultUiState()
    data class SummarizationSuccess(
        val summary: String,
        val summaryLanguage: String
    ) : OcrResultUiState()
    data class SummarizationFieldsSuccess(
        val documentType: String,
        val fields: List<DocumentField>
    ) : OcrResultUiState()
    data class Error(val message: String) : OcrResultUiState()
}
