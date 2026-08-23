package com.rohan.documentsaathi.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val documents: List<Document>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val documents: StateFlow<List<Document>> = documentRepository.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Document count flow
    val documentCount: StateFlow<Int> = documentRepository.getDocumentCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                documentRepository.getAllDocuments().collect { documents ->
                    _uiState.value = HomeUiState.Success(documents)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load documents")
            }
        }
    }

    fun searchDocuments(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                documentRepository.searchDocuments(query).collect { documents ->
                    _uiState.value = HomeUiState.Success(documents)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(document)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to delete document")
            }
        }
    }
}