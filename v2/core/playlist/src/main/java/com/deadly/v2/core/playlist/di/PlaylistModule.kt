package com.deadly.v2.core.playlist.di

import com.deadly.v2.core.api.playlist.PlaylistService
import com.deadly.v2.core.playlist.service.PlaylistServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * PlaylistModule - Hilt dependency injection for Playlist
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaylistModule {

    @Binds
    abstract fun bindPlaylistService(
        playlistServiceStub: PlaylistServiceStub
    ): PlaylistService
}