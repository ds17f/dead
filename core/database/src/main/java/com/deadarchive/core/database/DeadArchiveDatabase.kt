package com.deadarchive.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [ConcertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DeadArchiveDatabase : RoomDatabase() {
    abstract fun concertDao(): ConcertDao
    
    companion object {
        @Volatile
        private var INSTANCE: DeadArchiveDatabase? = null
        
        fun getDatabase(context: Context): DeadArchiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeadArchiveDatabase::class.java,
                    "dead_archive_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}