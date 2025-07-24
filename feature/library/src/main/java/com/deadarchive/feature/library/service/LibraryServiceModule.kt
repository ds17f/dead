package com.deadarchive.feature.library.service

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing Library feature services.
 * Services are automatically provided by their @Singleton @Inject constructors,
 * but this module can be used for any custom binding if needed in the future.
 */
@Module
@InstallIn(SingletonComponent::class)
object LibraryServiceModule {
    
    // All Library services are automatically provided by Hilt via their @Inject constructors:
    // - LibraryDataService
    // - LibraryDownloadService  
    // - LibraryManagementService
    
    // This module is reserved for any custom bindings that may be needed in the future,
    // such as interface implementations or complex factory patterns.
}