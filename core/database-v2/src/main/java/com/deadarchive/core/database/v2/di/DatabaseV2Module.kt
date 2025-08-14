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
    
    // Services and repositories are automatically provided by @Singleton @Inject constructor
    // AssetManagerV2, DataImportServiceV2, DatabaseManagerV2, ShowV2Repository will be available via Hilt
}