package com.deadly.v2.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.deadly.v2.core.database.entities.ShowEntity
import com.deadly.v2.core.database.entities.ShowSearchEntity
import com.deadly.v2.core.database.entities.RecordingEntity
import com.deadly.v2.core.database.entities.DataVersionEntity
import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.database.dao.ShowSearchDao
import com.deadly.v2.core.database.dao.RecordingDao
import com.deadly.v2.core.database.dao.DataVersionDao

@Database(
    entities = [
        ShowEntity::class,
        ShowSearchEntity::class,
        RecordingEntity::class,
        DataVersionEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class DeadlyDatabase : RoomDatabase() {
    
    abstract fun showDao(): ShowDao
    abstract fun showSearchDao(): ShowSearchDao
    abstract fun recordingDao(): RecordingDao
    abstract fun dataVersionDao(): DataVersionDao
    
    companion object {
        const val DATABASE_NAME = "deadly_db"
        
        fun create(context: Context): DeadlyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DeadlyDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // Clean rebuild for V2 simplification
            .build()
        }
    }
}