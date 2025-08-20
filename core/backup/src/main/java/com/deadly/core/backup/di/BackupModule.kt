package com.deadly.core.backup.di

import android.content.Context
import com.deadly.core.backup.BackupService
import com.deadly.core.data.repository.ShowRepository
import com.deadly.core.database.ShowDao
import com.deadly.core.settings.api.SettingsRepository
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