package com.deadarchive.v2.app.di

import com.deadarchive.v2.app.splash.service.SplashV2Service
import com.deadarchive.v2.core.database.service.DatabaseManagerV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppV2Module {
    
    @Provides
    @Singleton
    fun provideSplashV2Service(
        v2DatabaseManager: DatabaseManagerV2
    ): SplashV2Service {
        return SplashV2Service(v2DatabaseManager)
    }
}