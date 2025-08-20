package com.deadly.feature.playlist.di

import com.deadly.feature.playlist.service.PlaylistV2Service
import com.deadly.feature.playlist.service.PlaylistV2ServiceStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * PlaylistV2Module - Hilt dependency injection for PlaylistV2
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaylistV2Module {

    @Binds
    abstract fun bindPlaylistV2Service(
        playlistV2ServiceStub: PlaylistV2ServiceStub
    ): PlaylistV2Service
}