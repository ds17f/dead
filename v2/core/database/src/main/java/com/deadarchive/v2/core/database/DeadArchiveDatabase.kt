package com.deadarchive.v2.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.deadarchive.v2.core.database.entities.ShowEntity
import com.deadarchive.v2.core.database.entities.ShowSearchEntity
import com.deadarchive.v2.core.database.entities.RecordingEntity
import com.deadarchive.v2.core.database.entities.DataVersionEntity
import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.ShowSearchDao
import com.deadarchive.v2.core.database.dao.RecordingDao
import com.deadarchive.v2.core.database.dao.DataVersionDao

@Database(
    entities = [
        ShowEntity::class,
        ShowSearchEntity::class,
        RecordingEntity::class,
        DataVersionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class DeadArchiveDatabase : RoomDatabase() {
    
    abstract fun showDao(): ShowDao
    abstract fun showSearchDao(): ShowSearchDao
    abstract fun recordingDao(): RecordingDao
    abstract fun dataVersionDao(): DataVersionDao
    
    companion object {
        const val DATABASE_NAME = "dead_archive_v2_database"
        
        fun create(context: Context): DeadArchiveDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DeadArchiveDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // Clean rebuild for V2 simplification
            .build()
        }
    }
}