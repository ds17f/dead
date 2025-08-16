package com.deadarchive.v2.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.deadarchive.v2.core.database.entities.ShowV2Entity
import com.deadarchive.v2.core.database.entities.VenueV2Entity
import com.deadarchive.v2.core.database.entities.DataVersionEntity
import com.deadarchive.v2.core.database.entities.SongV2Entity
import com.deadarchive.v2.core.database.entities.SetlistV2Entity
import com.deadarchive.v2.core.database.entities.SetlistSongV2Entity
import com.deadarchive.v2.core.database.entities.RecordingV2Entity
import com.deadarchive.v2.core.database.entities.TrackV2Entity
import com.deadarchive.v2.core.database.entities.TrackFormatV2Entity
import com.deadarchive.v2.core.database.entities.SongSearchV2Entity
import com.deadarchive.v2.core.database.entities.VenueSearchV2Entity
import com.deadarchive.v2.core.database.entities.ShowSearchV2Entity
import com.deadarchive.v2.core.database.entities.MemberSearchV2Entity
import com.deadarchive.v2.core.database.entities.CollectionV2Entity
import com.deadarchive.v2.core.database.entities.CollectionShowV2Entity
import com.deadarchive.v2.core.database.dao.ShowV2Dao
import com.deadarchive.v2.core.database.dao.VenueV2Dao
import com.deadarchive.v2.core.database.dao.DataVersionDao
import com.deadarchive.v2.core.database.dao.SongV2Dao
import com.deadarchive.v2.core.database.dao.SetlistV2Dao
import com.deadarchive.v2.core.database.dao.SetlistSongV2Dao
import com.deadarchive.v2.core.database.dao.RecordingV2Dao
import com.deadarchive.v2.core.database.dao.TrackV2Dao
import com.deadarchive.v2.core.database.dao.TrackFormatV2Dao
import com.deadarchive.v2.core.database.dao.SongSearchV2Dao
import com.deadarchive.v2.core.database.dao.VenueSearchV2Dao
import com.deadarchive.v2.core.database.dao.ShowSearchV2Dao
import com.deadarchive.v2.core.database.dao.MemberSearchV2Dao
import com.deadarchive.v2.core.database.dao.CollectionV2Dao
import com.deadarchive.v2.core.database.dao.CollectionShowV2Dao

@Database(
    entities = [
        ShowV2Entity::class,
        VenueV2Entity::class,
        DataVersionEntity::class,
        SongV2Entity::class,
        SetlistV2Entity::class,
        SetlistSongV2Entity::class,
        RecordingV2Entity::class,
        TrackV2Entity::class,
        TrackFormatV2Entity::class,
        SongSearchV2Entity::class,
        VenueSearchV2Entity::class,
        ShowSearchV2Entity::class,
        MemberSearchV2Entity::class,
        CollectionV2Entity::class,
        CollectionShowV2Entity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DeadArchiveV2Database : RoomDatabase() {
    
    abstract fun showDao(): ShowV2Dao
    abstract fun venueDao(): VenueV2Dao
    abstract fun dataVersionDao(): DataVersionDao
    abstract fun songDao(): SongV2Dao
    abstract fun setlistDao(): SetlistV2Dao
    abstract fun setlistSongDao(): SetlistSongV2Dao
    abstract fun recordingDao(): RecordingV2Dao
    abstract fun trackDao(): TrackV2Dao
    abstract fun trackFormatDao(): TrackFormatV2Dao
    abstract fun songSearchDao(): SongSearchV2Dao
    abstract fun venueSearchDao(): VenueSearchV2Dao
    abstract fun showSearchDao(): ShowSearchV2Dao
    abstract fun memberSearchDao(): MemberSearchV2Dao
    abstract fun collectionDao(): CollectionV2Dao
    abstract fun collectionShowDao(): CollectionShowV2Dao
    
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