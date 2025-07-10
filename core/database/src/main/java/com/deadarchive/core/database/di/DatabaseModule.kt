package com.deadarchive.core.database.di

import android.content.Context
import androidx.room.Room
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.database.SyncMetadataDao
import com.deadarchive.core.database.RatingDao
import com.deadarchive.core.database.SetlistDao
import com.deadarchive.core.database.SongDao
import com.deadarchive.core.database.VenueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides Room database instance for the Dead Archive app
     */
    @Provides
    @Singleton
    fun provideDeadArchiveDatabase(
        @ApplicationContext context: Context
    ): DeadArchiveDatabase {
        return Room.databaseBuilder(
            context,
            DeadArchiveDatabase::class.java,
            "dead_archive_db"
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    /**
     * Provides Show DAO for database operations
     */
    @Provides
    fun provideShowDao(database: DeadArchiveDatabase): ShowDao {
        return database.showDao()
    }
    
    /**
     * Provides Recording DAO for database operations
     */
    @Provides
    fun provideRecordingDao(database: DeadArchiveDatabase): RecordingDao {
        return database.recordingDao()
    }
    
    /**
     * Provides Download DAO for download tracking operations
     */
    @Provides
    fun provideDownloadDao(database: DeadArchiveDatabase): DownloadDao {
        return database.downloadDao()
    }
    
    /**
     * Provides Library DAO for library operations
     */
    @Provides
    fun provideLibraryDao(database: DeadArchiveDatabase): LibraryDao {
        return database.libraryDao()
    }
    
    /**
     * Provides Sync Metadata DAO for sync tracking operations
     */
    @Provides
    fun provideSyncMetadataDao(database: DeadArchiveDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
    
    /**
     * Provides Rating DAO for ratings operations
     */
    @Provides
    fun provideRatingDao(database: DeadArchiveDatabase): RatingDao {
        return database.ratingDao()
    }
    
    /**
     * Provides Setlist DAO for setlist operations
     */
    @Provides
    fun provideSetlistDao(database: DeadArchiveDatabase): SetlistDao {
        return database.setlistDao()
    }
    
    /**
     * Provides Song DAO for song operations
     */
    @Provides
    fun provideSongDao(database: DeadArchiveDatabase): SongDao {
        return database.songDao()
    }
    
    /**
     * Provides Venue DAO for venue operations
     */
    @Provides
    fun provideVenueDao(database: DeadArchiveDatabase): VenueDao {
        return database.venueDao()
    }
}