package com.deadarchive.core.backup.di

import android.content.Context
import com.deadarchive.core.backup.BackupService
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.settings.api.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        showDao: ShowDao,
        @ApplicationContext context: Context
    ): BackupService {
        return BackupService(showRepository, settingsRepository, showDao, context)
    }
}