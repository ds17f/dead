package com.deadarchive.core.backup.di

import com.deadarchive.core.backup.BackupService
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.settings.api.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    
    @Provides
    @Singleton
    fun provideBackupService(
        showRepository: ShowRepository,
        settingsRepository: SettingsRepository,
        libraryDao: LibraryDao
    ): BackupService {
        return BackupService(showRepository, settingsRepository, libraryDao)
    }
}