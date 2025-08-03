package com.deadarchive.core.search.di

import com.deadarchive.core.search.api.SearchV2Service
import com.deadarchive.core.search.service.SearchV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for SearchV2 stub implementations.
 * 
 * Provides stub services for the V2 architecture stub-first development approach.
 * Uses @Named("stub") qualifier to distinguish from real implementations
 * that will be added later.
 * 
 * Following the established pattern from LibraryV2StubModule and DownloadV2StubModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchV2StubModule {
    
    /**
     * Binds the comprehensive SearchV2ServiceStub to the SearchV2Service interface.
     * Uses @Named("stub") qualifier for dependency injection control.
     */
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindSearchV2ServiceStub(
        impl: SearchV2ServiceStub
    ): SearchV2Service
}