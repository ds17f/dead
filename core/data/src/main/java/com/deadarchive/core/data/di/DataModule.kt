package com.deadarchive.core.data.di

import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.data.repository.ConcertRepositoryImpl
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.repository.DownloadRepositoryImpl
import com.deadarchive.core.data.repository.FavoriteRepository
import com.deadarchive.core.data.repository.FavoriteRepositoryImpl
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
 * - ConcertRepository -> ConcertDao (from DatabaseModule)
 * - DownloadRepository -> DownloadDao + ConcertRepository (circular prevention via lazy injection)
 * - FavoriteRepository -> FavoriteDao + ConcertRepository (for enhanced features)
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
     * Binds ConcertRepository interface to its implementation.
     * Provides concert data access with offline-first caching strategy.
     */
    @Binds
    @Singleton
    abstract fun bindConcertRepository(
        concertRepositoryImpl: ConcertRepositoryImpl
    ): ConcertRepository
    
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
     * Binds FavoriteRepository interface to its implementation.
     * Provides favorites management with enhanced batch operations and integration features.
     */
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        favoriteRepositoryImpl: FavoriteRepositoryImpl
    ): FavoriteRepository
}