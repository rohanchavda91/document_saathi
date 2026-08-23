package com.rohan.documentsaathi.feature.document.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
): ViewModel(){
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