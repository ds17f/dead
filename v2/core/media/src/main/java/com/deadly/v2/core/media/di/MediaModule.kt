package com.deadly.v2.core.media.di

import android.content.Context
import com.deadly.v2.core.media.repository.MediaControllerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Simple Hilt module for V2 media components
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    
    @Provides
    @Singleton
    fun provideMediaControllerRepository(
        @ApplicationContext context: Context
    ): MediaControllerRepository {
        return MediaControllerRepository(context)
    }
}