package com.deadly.core.library.di

import com.deadly.core.library.api.LibraryV2Service
import com.deadly.core.library.service.LibraryV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for Library V2 stub implementations.
 * 
 * Provides stub services for the stub-first development approach.
 * Uses @Named("stub") qualifier to distinguish from real implementations
 * that will be added later.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryV2StubModule {
    
    /**
     * Binds the minimal logging-only LibraryV2ServiceStub to the LibraryV2Service interface.
     * Uses @Named("stub") qualifier for dependency injection control.
     */
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindLibraryV2ServiceStub(
        impl: LibraryV2ServiceStub
    ): LibraryV2Service
}