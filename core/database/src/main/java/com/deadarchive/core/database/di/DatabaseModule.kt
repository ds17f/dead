package com.deadarchive.core.database.di

import android.content.Context
import androidx.room.Room
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.database.SyncMetadataDao
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
     * Provides Concert DAO for database operations
     */
    @Provides
    fun provideConcertDao(database: DeadArchiveDatabase): ConcertDao {
        return database.concertDao()
    }
    
    /**
     * Provides Download DAO for download tracking operations
     */
    @Provides
    fun provideDownloadDao(database: DeadArchiveDatabase): DownloadDao {
        return database.downloadDao()
    }
    
    /**
     * Provides Favorite DAO for favorites operations
     */
    @Provides
    fun provideFavoriteDao(database: DeadArchiveDatabase): FavoriteDao {
        return database.favoriteDao()
    }
    
    /**
     * Provides Sync Metadata DAO for sync tracking operations
     */
    @Provides
    fun provideSyncMetadataDao(database: DeadArchiveDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}