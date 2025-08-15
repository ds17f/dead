package com.deadarchive.core.database.v2

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.deadarchive.core.database.v2.entities.ShowV2Entity
import com.deadarchive.core.database.v2.entities.VenueV2Entity
import com.deadarchive.core.database.v2.entities.DataVersionEntity
import com.deadarchive.core.database.v2.entities.SongV2Entity
import com.deadarchive.core.database.v2.entities.SetlistV2Entity
import com.deadarchive.core.database.v2.entities.SetlistSongV2Entity
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.SongV2Dao
import com.deadarchive.core.database.v2.dao.SetlistV2Dao
import com.deadarchive.core.database.v2.dao.SetlistSongV2Dao

@Database(
    entities = [
        ShowV2Entity::class,
        VenueV2Entity::class,
        DataVersionEntity::class,
        SongV2Entity::class,
        SetlistV2Entity::class,
        SetlistSongV2Entity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DeadArchiveV2Database : RoomDatabase() {
    
    abstract fun showDao(): ShowV2Dao
    abstract fun venueDao(): VenueV2Dao
    abstract fun dataVersionDao(): DataVersionDao
    abstract fun songDao(): SongV2Dao
    abstract fun setlistDao(): SetlistV2Dao
    abstract fun setlistSongDao(): SetlistSongV2Dao
    
    companion object {
        const val DATABASE_NAME = "dead_archive_v2_database"
        
        fun create(context: Context): DeadArchiveV2Database {
            return Room.databaseBuilder(
                context.applicationContext,
                DeadArchiveV2Database::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For now, since it's V2 and we can re-import
            .build()
        }
    }
}