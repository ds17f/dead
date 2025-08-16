package com.deadarchive.v2.core.database.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.deadarchive.v2.core.database.DeadArchiveV2Database
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseV2Module {
    
    @Provides
    @Singleton
    fun provideDeadArchiveV2Database(@ApplicationContext context: Context): DeadArchiveV2Database {
        return DeadArchiveV2Database.create(context)
    }
    
    @Provides
    fun provideShowV2Dao(database: DeadArchiveV2Database): ShowV2Dao {
        return database.showDao()
    }
    
    @Provides
    fun provideVenueV2Dao(database: DeadArchiveV2Database): VenueV2Dao {
        return database.venueDao()
    }
    
    @Provides
    fun provideDataVersionDao(database: DeadArchiveV2Database): DataVersionDao {
        return database.dataVersionDao()
    }
    
    @Provides
    fun provideSongV2Dao(database: DeadArchiveV2Database): SongV2Dao {
        return database.songDao()
    }
    
    @Provides
    fun provideSetlistV2Dao(database: DeadArchiveV2Database): SetlistV2Dao {
        return database.setlistDao()
    }
    
    @Provides
    fun provideSetlistSongV2Dao(database: DeadArchiveV2Database): SetlistSongV2Dao {
        return database.setlistSongDao()
    }
    
    @Provides
    fun provideRecordingV2Dao(database: DeadArchiveV2Database): RecordingV2Dao {
        return database.recordingDao()
    }
    
    @Provides
    fun provideTrackV2Dao(database: DeadArchiveV2Database): TrackV2Dao {
        return database.trackDao()
    }
    
    @Provides
    fun provideTrackFormatV2Dao(database: DeadArchiveV2Database): TrackFormatV2Dao {
        return database.trackFormatDao()
    }
    
    // Search DAOs
    @Provides
    fun provideSongSearchV2Dao(database: DeadArchiveV2Database): SongSearchV2Dao {
        return database.songSearchDao()
    }
    
    @Provides
    fun provideVenueSearchV2Dao(database: DeadArchiveV2Database): VenueSearchV2Dao {
        return database.venueSearchDao()
    }
    
    @Provides
    fun provideShowSearchV2Dao(database: DeadArchiveV2Database): ShowSearchV2Dao {
        return database.showSearchDao()
    }
    
    @Provides
    fun provideMemberSearchV2Dao(database: DeadArchiveV2Database): MemberSearchV2Dao {
        return database.memberSearchDao()
    }
    
    // Collection DAOs
    @Provides
    fun provideCollectionV2Dao(database: DeadArchiveV2Database): CollectionV2Dao {
        return database.collectionDao()
    }
    
    @Provides
    fun provideCollectionShowV2Dao(database: DeadArchiveV2Database): CollectionShowV2Dao {
        return database.collectionShowDao()
    }
    
    // Services and repositories are automatically provided by @Singleton @Inject constructor
    // AssetManagerV2, DataImportServiceV2, DatabaseManagerV2, ShowV2Repository, SearchServiceV2, SearchRepositoryV2, SearchTableProcessorV2, CollectionProcessorV2 will be available via Hilt
}