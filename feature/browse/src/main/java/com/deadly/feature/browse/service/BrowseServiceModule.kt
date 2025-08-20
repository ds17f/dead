package com.deadly.feature.browse.service

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Browse feature services.
 * Services are automatically provided by their @Singleton @Inject constructors,
 * but this module can be used for any custom binding if needed in the future.
 */
@Module
@InstallIn(SingletonComponent::class)
object BrowseServiceModule {
    
    // All Browse services are automatically provided by Hilt via their @Inject constructors:
    // - BrowseSearchService
    // - BrowseLibraryService  
    // - BrowseDownloadService
    // - BrowseDataService
    
    // This module is reserved for any custom bindings that may be needed in the future,
    // such as interface implementations or complex factory patterns.
}