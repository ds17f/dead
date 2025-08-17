package com.deadarchive.v2.core.database.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.deadarchive.v2.core.database.DeadArchiveDatabase
import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.ShowFtsDao
import com.deadarchive.v2.core.database.dao.RecordingDao
import com.deadarchive.v2.core.database.dao.DataVersionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDeadArchiveDatabase(@ApplicationContext context: Context): DeadArchiveDatabase {
        return DeadArchiveDatabase.create(context)
    }
    
    @Provides
    fun provideShowDao(database: DeadArchiveDatabase): ShowDao {
        return database.showDao()
    }
    
    @Provides
    fun provideShowFtsDao(database: DeadArchiveDatabase): ShowFtsDao {
        return database.showFtsDao()
    }
    
    @Provides
    fun provideRecordingDao(database: DeadArchiveDatabase): RecordingDao {
        return database.recordingDao()
    }
    
    @Provides
    fun provideDataVersionDao(database: DeadArchiveDatabase): DataVersionDao {
        return database.dataVersionDao()
    }
    
    // Services are automatically provided by @Singleton @Inject constructor:
    // - DataImportService
    // - AwesomeBarService
}