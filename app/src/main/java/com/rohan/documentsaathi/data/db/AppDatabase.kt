package com.rohan.documentsaathi.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rohan.documentsaathi.data.db.dao.DocumentDao
import com.rohan.documentsaathi.data.db.entity.Document

@Database(
    entities = [Document::class],
    version = 2,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase(){

    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "documentsaathi_db"
    }
}