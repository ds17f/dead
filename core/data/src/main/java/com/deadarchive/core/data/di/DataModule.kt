package com.deadarchive.core.data.di

import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.data.repository.ConcertRepositoryImpl
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.repository.DownloadRepositoryImpl
import com.deadarchive.core.data.repository.FavoriteRepository
import com.deadarchive.core.data.repository.FavoriteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindConcertRepository(
        concertRepositoryImpl: ConcertRepositoryImpl
    ): ConcertRepository
    
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository
    
    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        favoriteRepositoryImpl: FavoriteRepositoryImpl
    ): FavoriteRepository
}