package com.deadarchive.core.data.di

import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.ShowRepositoryImpl
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.repository.DownloadRepositoryImpl
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.LibraryRepositoryImpl
import com.deadarchive.core.data.repository.TodayInHistoryRepository
import com.deadarchive.core.data.sync.DataSyncService
import com.deadarchive.core.data.sync.DataSyncServiceImpl
import com.deadarchive.core.data.debug.WorkManagerDebugUtil
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for data layer dependency injection.
 * 
 * This module provides repository implementations with proper singleton scoping
 * to ensure single instances across the application lifecycle.
 * 
 * Dependency relationships:
 * - ShowRepository -> ShowDao + RecordingDao (from DatabaseModule)
 * - DownloadRepository -> DownloadDao + ShowRepository (circular prevention via lazy injection)
 * - LibraryRepository -> LibraryDao (for library management)
 * 
 * All repositories are scoped as @Singleton to:
 * - Maintain consistent state across ViewModels
 * - Reduce memory allocation overhead
 * - Enable proper reactive stream management
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    /**
     * Binds ShowRepository interface to its implementation.
     * Provides show and recording data access with offline-first caching strategy.
     */
    @Binds
    @Singleton
    abstract fun bindShowRepository(
        showRepositoryImpl: ShowRepositoryImpl
    ): ShowRepository
    
    /**
     * Binds DownloadRepository interface to its implementation.
     * Provides download management with queue operations and file system integration.
     */
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository
    
    /**
     * Binds LibraryRepository interface to its implementation.
     * Provides library management for saved shows and recordings.
     */
    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        libraryRepositoryImpl: LibraryRepositoryImpl
    ): LibraryRepository
    
    /**
     * Binds DataSyncService interface to its implementation.
     * Provides complete catalog download and background sync capabilities.
     */
    @Binds
    @Singleton
    abstract fun bindDataSyncService(
        dataSyncServiceImpl: DataSyncServiceImpl
    ): DataSyncService
}