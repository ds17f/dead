package com.deadly.v2.core.database.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.deadly.v2.core.database.DeadlyDatabase
import com.deadly.v2.core.model.V2Database
import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.database.dao.ShowSearchDao
import com.deadly.v2.core.database.dao.RecordingDao
import com.deadly.v2.core.database.dao.DataVersionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDeadlyDatabase(@ApplicationContext context: Context): DeadlyDatabase {
        return DeadlyDatabase.create(context)
    }
    
    @Provides
    @V2Database
    fun provideShowDao(database: DeadlyDatabase): ShowDao {
        return database.showDao()
    }
    
    @Provides
    @V2Database
    fun provideShowSearchDao(database: DeadlyDatabase): ShowSearchDao {
        return database.showSearchDao()
    }
    
    @Provides
    @V2Database
    fun provideRecordingDao(database: DeadlyDatabase): RecordingDao {
        return database.recordingDao()
    }
    
    @Provides
    @V2Database
    fun provideDataVersionDao(database: DeadlyDatabase): DataVersionDao {
        return database.dataVersionDao()
    }
    
    // Services are automatically provided by @Singleton @Inject constructor:
    // - DataImportService
}