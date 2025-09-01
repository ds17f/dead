package com.deadly.v2.core.collections.di

import com.deadly.v2.core.api.collections.CollectionsService
import com.deadly.v2.core.collections.CollectionsServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Collections service implementations.
 * 
 * Provides production CollectionsServiceImpl with real data integration.
 * Uses ShowRepository to provide curated collections with actual show data.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CollectionsModule {
    
    /**
     * Binds the production CollectionsServiceImpl to the CollectionsService interface.
     * Provides curated Grateful Dead collections with real show data from database.
     */
    @Binds
    @Singleton
    abstract fun bindCollectionsService(
        impl: CollectionsServiceImpl
    ): CollectionsService
}