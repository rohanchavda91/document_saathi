package com.rohan.documentsaathi.data.db.dao

import androidx.room.*
import com.rohan.documentsaathi.data.db.entity.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

//    Inserting a new doc
    @Insert
    suspend fun insertDocument(document: Document): Long

//    Getting all doc ordered by creation date by newest first
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

//    Get an individual doc by its id
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: Long): Document?

//    Update document this will be used for adding summary laterly
    @Update
    suspend fun updateDocument(document: Document)

//    Delete a document
    @Delete
    suspend fun deleteDocument(document: Document)

//    Get total count of documents
    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

//    Search documents by text content
    @Query("SELECT * FROM documents WHERE extractedText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchDocuments(query: String): Flow<List<Document>>
}