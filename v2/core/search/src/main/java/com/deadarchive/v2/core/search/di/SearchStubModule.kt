package com.deadarchive.v2.core.search.di

import com.deadarchive.v2.core.api.search.SearchService
import com.deadarchive.v2.core.search.SearchServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for Search stub implementations.
 * 
 * Provides stub services for the V2 architecture stub-first development approach.
 * Uses @Named("stub") qualifier to distinguish from real implementations
 * that will be added later.
 * 
 * Following the established pattern from LibraryV2StubModule and DownloadV2StubModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchStubModule {
    
    /**
     * Binds the comprehensive SearchServiceStub to the SearchService interface.
     * Uses @Named("stub") qualifier for dependency injection control.
     */
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindSearchServiceStub(
        impl: SearchServiceStub
    ): SearchService
}