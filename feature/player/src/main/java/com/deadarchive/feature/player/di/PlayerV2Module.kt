package com.deadarchive.feature.player.di

import com.deadarchive.feature.player.service.PlayerV2Service
import com.deadarchive.feature.player.service.PlayerV2ServiceImpl
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
        playerV2ServiceImpl: PlayerV2ServiceImpl
    ): PlayerV2Service
}