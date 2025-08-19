package com.deadarchive.v2.app.di

import com.deadarchive.v2.app.splash.service.SplashService
import com.deadarchive.v2.core.database.service.DatabaseManager
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
        databaseManager: DatabaseManager
    ): SplashService {
        return SplashService(databaseManager)
    }
}