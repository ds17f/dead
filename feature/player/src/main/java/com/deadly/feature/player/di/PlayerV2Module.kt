package com.deadly.feature.player.di

import com.deadly.feature.player.service.PlayerV2Service
import com.deadly.feature.player.service.PlayerV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for PlayerV2 dependency injection
 * Following V2 architecture pattern of service abstraction
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerV2Module {
    
    @Binds
    @Singleton
    abstract fun bindPlayerV2Service(
        playerV2ServiceStub: PlayerV2ServiceStub
    ): PlayerV2Service
}