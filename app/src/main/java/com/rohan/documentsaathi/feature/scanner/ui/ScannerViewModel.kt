package com.rohan.documentsaathi.feature.scanner.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.documentsaathi.core.ai.SummarizationManager
import com.rohan.documentsaathi.core.utils.ImageManager
import com.rohan.documentsaathi.core.utils.PdfManager
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import com.rohan.documentsaathi.feature.ocr.manager.OcrManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ScannerUiState{
    data object Idle : ScannerUiState()
    data object CameraReady : ScannerUiState()
    data object ProcessingPhoto : ScannerUiState()
    data class OcrSuccess(val documentId: Long) : ScannerUiState()
    data class OcrError(val message: String): ScannerUiState()

}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val ocrManager: OcrManager,
    private val documentRepository: DocumentRepository,
    private val summarizationManager: SummarizationManager,
    private val pdfManager: PdfManager,
    private val imageManager: ImageManager
) : ViewModel(){
    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState : StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onCameraReady(){
        _uiState.value = ScannerUiState.CameraReady
    }

    fun processPhoto(bitmap:Bitmap){
        viewModelScope.launch {
            _uiState.value = ScannerUiState.ProcessingPhoto
            try{
                val extractedText = withContext(Dispatchers.Default){
                    ocrManager.recognizeText(bitmap)
                }
                
                // Generate PDF from captured photo
                val pdfUri = withContext(Dispatchers.IO) {
                    pdfManager.generatePdfFromBitmap(bitmap)
                }

                // Save image locally for later AI processing
                val imagePath = withContext(Dispatchers.IO) {
                    imageManager.saveBitmap(bitmap)
                }
                
                // Save document to database early
                val document = Document(
                    extractedText = extractedText,
                    detectedLanguage = "en",
                    imageUri = imagePath,
                    structuredDataJson = null, // Will be fetched in detail screen
                    pdfUri = pdfUri
                )
                val documentId = documentRepository.saveDocument(document)
                
                _uiState.value = ScannerUiState.OcrSuccess(documentId)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.OcrError(
                    e.message ?: "Unknown OCR Error"
                )
            }
        }
    }

    fun resetState(){
        _uiState.value = ScannerUiState.Idle
    }
}

