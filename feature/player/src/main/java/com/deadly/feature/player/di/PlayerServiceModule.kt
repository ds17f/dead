package com.deadly.feature.player.di

import com.deadly.feature.player.service.PlayerDataService
import com.deadly.feature.player.service.PlayerDataServiceImpl
import com.deadly.feature.player.service.PlayerPlaylistService
import com.deadly.feature.player.service.PlayerPlaylistServiceImpl
import com.deadly.feature.player.service.PlayerLibraryService
import com.deadly.feature.player.service.PlayerLibraryServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for player service dependency injection.
 * 
 * This module provides service implementations for the player feature,
 * following the service-oriented architecture pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerServiceModule {
    
    /**
     * Binds PlayerDataService interface to its implementation.
     * Provides recording data loading and management services.
     */
    @Binds
    @Singleton
    abstract fun bindPlayerDataService(
        playerDataServiceImpl: PlayerDataServiceImpl
    ): PlayerDataService
    
    /**
     * Binds PlayerPlaylistService interface to its implementation.
     * Provides playlist management and queue operations.
     */
    @Binds
    @Singleton
    abstract fun bindPlayerPlaylistService(
        playerPlaylistServiceImpl: PlayerPlaylistServiceImpl
    ): PlayerPlaylistService
    
    
    /**
     * Binds PlayerLibraryService interface to its implementation.
     * Provides library integration and status tracking.
     */
    @Binds
    @Singleton
    abstract fun bindPlayerLibraryService(
        playerLibraryServiceImpl: PlayerLibraryServiceImpl
    ): PlayerLibraryService
}