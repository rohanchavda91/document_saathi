package com.rohan.documentsaathi.data.repository

import com.rohan.documentsaathi.data.db.dao.DocumentDao
import com.rohan.documentsaathi.data.db.entity.Document
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {

//    Save a freshly scanned doc
    suspend fun saveDocument(document: Document): Long {
        return documentDao.insertDocument(document)
    }

//    Get all documents
    fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments()
    }

//    Get single document by ID
    suspend fun getDocumentById(documentId: Long): Document?{
        return documentDao.getDocumentById(documentId)
    }

//  Update document (e.g. Adding summary)
    suspend fun updateDocument(document: Document){
        return documentDao.updateDocument(document)
    }

//    Delete document
    suspend fun deleteDocument(document: Document){
        documentDao.deleteDocument(document)
    }

//    Get total count
    fun getDocumentCount(): Flow<Int>{
        return documentDao.getDocumentCount()
    }

//    Search documents
    fun searchDocuments(query: String): Flow<List<Document>> {
        return documentDao.searchDocuments(query)
    }
}