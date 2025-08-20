package com.deadly.core.data.di

import com.deadly.core.data.api.repository.ShowRepository
import com.deadly.core.data.repository.ShowRepositoryImpl
import com.deadly.core.data.repository.DownloadRepository
import com.deadly.core.data.repository.DownloadRepositoryImpl
import com.deadly.core.data.repository.LibraryRepository
import com.deadly.core.data.repository.LibraryRepositoryImpl
import com.deadly.core.data.sync.DataSyncService
import com.deadly.core.data.sync.DataSyncServiceImpl
import com.deadly.core.data.service.ShowEnrichmentService
import com.deadly.core.data.service.ShowEnrichmentServiceImpl
import com.deadly.core.data.service.ShowCacheService
import com.deadly.core.data.service.ShowCacheServiceImpl
import com.deadly.core.data.service.ShowCreationService
import com.deadly.core.data.service.ShowCreationServiceImpl
import com.deadly.core.data.service.UpdateService
import com.deadly.core.data.service.UpdateServiceImpl
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
    
    /**
     * Binds ShowEnrichmentService interface to its implementation.
     * Provides rating and recording attachment services for shows.
     */
    @Binds
    @Singleton
    abstract fun bindShowEnrichmentService(
        showEnrichmentServiceImpl: ShowEnrichmentServiceImpl
    ): ShowEnrichmentService
    
    /**
     * Binds ShowCacheService interface to its implementation.
     * Provides caching and API interaction services.
     */
    @Binds
    @Singleton
    abstract fun bindShowCacheService(
        showCacheServiceImpl: ShowCacheServiceImpl
    ): ShowCacheService
    
    /**
     * Binds ShowCreationService interface to its implementation.
     * Provides show creation and normalization services.
     */
    @Binds
    @Singleton
    abstract fun bindShowCreationService(
        showCreationServiceImpl: ShowCreationServiceImpl
    ): ShowCreationService
    
    /**
     * Binds UpdateService interface to its implementation.
     * Provides app update checking, downloading, and installation services.
     */
    @Binds
    @Singleton
    abstract fun bindUpdateService(
        updateServiceImpl: UpdateServiceImpl
    ): UpdateService
}