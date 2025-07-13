package com.deadarchive.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [
        ShowEntity::class,
        RecordingEntity::class,
        DownloadEntity::class,
        LibraryEntity::class,
        SyncMetadataEntity::class,
        RecordingRatingEntity::class,
        ShowRatingEntity::class,
        SetlistEntity::class,
        SongEntity::class,
        VenueEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DeadArchiveDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun recordingDao(): RecordingDao
    abstract fun downloadDao(): DownloadDao
    abstract fun libraryDao(): LibraryDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun ratingDao(): RatingDao
    abstract fun setlistDao(): SetlistDao
    abstract fun songDao(): SongDao
    abstract fun venueDao(): VenueDao
    
}