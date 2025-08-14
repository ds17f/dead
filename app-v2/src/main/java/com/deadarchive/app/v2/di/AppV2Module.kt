package com.deadarchive.app.v2.di

import com.deadarchive.app.v2.splash.service.SplashV2Service
import com.deadarchive.core.database.v2.service.DatabaseManagerV2
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