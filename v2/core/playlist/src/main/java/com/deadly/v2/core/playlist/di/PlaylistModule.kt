package com.deadly.v2.core.playlist.di

import com.deadly.v2.core.api.playlist.PlaylistService
import com.deadly.v2.core.playlist.service.PlaylistServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * PlaylistModule - Hilt dependency injection for Playlist
 * 
 * Updated to use real PlaylistServiceImpl with V2 domain architecture.
 * Uses SingletonComponent to access ShowRepository from database layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaylistModule {

    @Binds
    abstract fun bindPlaylistService(
        impl: PlaylistServiceImpl
    ): PlaylistService
}