package com.deadly.v2.core.home.di

import com.deadly.v2.core.api.home.HomeService
import com.deadly.v2.core.home.HomeServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Home service implementations.
 * 
 * Currently provides stub implementation for UI-first development.
 * Will be updated to provide real implementation once underlying services are complete.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    
    /**
     * Binds the stub HomeServiceStub to the HomeService interface.
     * Provides comprehensive mock data for immediate UI development.
     */
    @Binds
    @Singleton
    abstract fun bindHomeService(
        impl: HomeServiceStub
    ): HomeService
}