package com.deadly.core.download.di

import com.deadly.core.download.api.DownloadV2Service
import com.deadly.core.download.service.DownloadV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for Download V2 stub implementations.
 * 
 * Provides stub services for the stub-first development approach.
 * Uses @Named("stub") qualifier to distinguish from real implementations
 * that will be added later.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadV2StubModule {
    
    /**
     * Binds the minimal logging-only DownloadV2ServiceStub to the DownloadV2Service interface.
     * Uses @Named("stub") qualifier for dependency injection control.
     */
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindDownloadV2ServiceStub(
        impl: DownloadV2ServiceStub
    ): DownloadV2Service
}