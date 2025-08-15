package com.deadarchive.core.database.v2.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.deadarchive.core.database.v2.DeadArchiveV2Database
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.SongV2Dao
import com.deadarchive.core.database.v2.dao.SetlistV2Dao
import com.deadarchive.core.database.v2.dao.SetlistSongV2Dao
import com.deadarchive.core.database.v2.dao.RecordingV2Dao
import com.deadarchive.core.database.v2.dao.TrackV2Dao
import com.deadarchive.core.database.v2.dao.TrackFormatV2Dao
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
    
    // Services and repositories are automatically provided by @Singleton @Inject constructor
    // AssetManagerV2, DataImportServiceV2, DatabaseManagerV2, ShowV2Repository will be available via Hilt
}