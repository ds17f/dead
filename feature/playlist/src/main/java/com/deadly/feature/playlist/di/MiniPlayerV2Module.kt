package com.deadly.feature.playlist.di

import com.deadly.feature.playlist.service.MiniPlayerV2Service
import com.deadly.feature.playlist.service.MiniPlayerV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * MiniPlayerV2Module - Hilt dependency injection module for V2 mini-player
 * 
 * Provides V2 service implementations following established DI patterns.
 * Currently binds stub implementation for development.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MiniPlayerV2Module {
    
    /**
     * Bind MiniPlayerV2Service to stub implementation
     * 
     * In the future, this can be switched to a real implementation
     * that coordinates with the actual media system.
     */
    @Binds
    @Singleton
    abstract fun bindMiniPlayerV2Service(
        stub: MiniPlayerV2ServiceStub
    ): MiniPlayerV2Service
}