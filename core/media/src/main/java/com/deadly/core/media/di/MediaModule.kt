package com.deadly.core.media.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.deadly.core.data.repository.DownloadRepository
import com.deadly.core.media.player.LocalFileResolver
import com.deadly.core.media.player.MediaControllerRepository
import com.deadly.core.media.player.service.MediaServiceConnector
import com.deadly.core.media.player.service.PlaybackCommandProcessor
import com.deadly.core.media.player.service.PlaybackStateSync
import com.deadly.core.media.player.PlaybackEventTracker
import com.deadly.core.media.player.PlaybackHistorySessionManager
import com.deadly.core.media.player.PlaybackResumeService
import com.deadly.core.media.player.LastPlayedTrackService
import com.deadly.core.media.player.LastPlayedTrackMonitor
import com.deadly.core.media.player.QueueManager
import com.deadly.core.media.player.QueueStateManager
import com.deadly.core.data.repository.PlaybackHistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    
    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("DeadArchive/1.0 (Android; Grateful Dead Concert Archive App)")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
    }
    
    @Provides
    @Singleton
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        httpDataSourceFactory: DefaultHttpDataSource.Factory
    ): DefaultDataSource.Factory {
        return DefaultDataSource.Factory(context, httpDataSourceFactory)
    }
    
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        dataSourceFactory: DefaultDataSource.Factory
    ): ExoPlayer {
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
    
    /**
     * Provides LocalFileResolver for offline playback support.
     */
    @Provides
    @Singleton
    fun provideLocalFileResolver(
        downloadRepository: DownloadRepository
    ): LocalFileResolver {
        return LocalFileResolver(downloadRepository)
    }
    
    /**
     * Provides MediaServiceConnector for MediaController connection lifecycle
     */
    @Provides
    @Singleton
    fun provideMediaServiceConnector(
        @ApplicationContext context: Context
    ): MediaServiceConnector {
        return MediaServiceConnector(context)
    }
    
    /**
     * Provides PlaybackStateSync for StateFlow synchronization
     */
    @Provides
    @Singleton
    fun providePlaybackStateSync(
        showRepository: com.deadly.core.data.api.repository.ShowRepository
    ): PlaybackStateSync {
        return PlaybackStateSync(showRepository)
    }
    
    /**
     * Provides PlaybackCommandProcessor for command handling
     */
    @Provides
    @Singleton
    fun providePlaybackCommandProcessor(
        localFileResolver: LocalFileResolver
    ): PlaybackCommandProcessor {
        return PlaybackCommandProcessor(localFileResolver)
    }
    
    /**
     * Provides refactored MediaControllerRepository using service composition.
     * Maintains identical interface for backward compatibility while using focused services.
     */
    @Provides
    @Singleton
    fun provideMediaControllerRepository(
        @ApplicationContext context: Context,
        mediaServiceConnector: MediaServiceConnector,
        playbackStateSync: PlaybackStateSync,
        playbackCommandProcessor: PlaybackCommandProcessor
    ): MediaControllerRepository {
        return MediaControllerRepository(
            context, 
            mediaServiceConnector, 
            playbackStateSync, 
            playbackCommandProcessor
        )
    }
    
    /**
     * Provides QueueManager for simplified queue operations.
     * Uses MediaControllerRepository as the single source of truth.
     */
    @Provides
    @Singleton
    fun provideQueueManager(
        mediaControllerRepository: MediaControllerRepository,
        localFileResolver: LocalFileResolver
    ): QueueManager {
        return QueueManager(mediaControllerRepository, localFileResolver)
    }
    
    /**
     * Provides QueueStateManager for exposing queue state flows.
     * Bridges QueueManager and MediaControllerRepository without circular dependencies.
     */
    @Provides
    @Singleton
    fun provideQueueStateManager(
        queueManager: QueueManager,
        mediaControllerRepository: MediaControllerRepository
    ): QueueStateManager {
        val queueStateManager = QueueStateManager(queueManager, mediaControllerRepository)
        
        // Wire the flows to avoid circular dependencies
        mediaControllerRepository.setQueueStateManager(queueStateManager)
        
        return queueStateManager
    }
    
    /**
     * Provides PlaybackEventTracker for Media3 event monitoring.
     * Used for playback history tracking and debugging.
     * The tracker automatically connects when MediaController becomes available.
     */
    @Provides
    @Singleton
    fun providePlaybackEventTracker(
        mediaControllerRepository: MediaControllerRepository
    ): PlaybackEventTracker {
        return PlaybackEventTracker(mediaControllerRepository)
        // No manual connection needed - tracker monitors connection state automatically
    }
    
    /**
     * Provides PlaybackHistorySessionManager for intelligent playback history tracking.
     * Coordinates between Media3 events and persistent history storage.
     */
    @Provides
    @Singleton
    fun providePlaybackHistorySessionManager(
        playbackEventTracker: PlaybackEventTracker,
        playbackHistoryRepository: PlaybackHistoryRepository,
        mediaControllerRepository: MediaControllerRepository
    ): PlaybackHistorySessionManager {
        val sessionManager = PlaybackHistorySessionManager(
            playbackEventTracker,
            playbackHistoryRepository,
            mediaControllerRepository
        )
        
        // Start tracking automatically
        sessionManager.startTracking()
        
        return sessionManager
    }
    
    /**
     * Provides PlaybackResumeService for restoring interrupted playback sessions.
     * Allows users to continue where they left off when the app restarts.
     */
    @Provides
    @Singleton
    fun providePlaybackResumeService(
        playbackHistoryRepository: PlaybackHistoryRepository,
        showRepository: com.deadly.core.data.api.repository.ShowRepository,
        queueManager: QueueManager,
        mediaControllerRepository: MediaControllerRepository
    ): PlaybackResumeService {
        return PlaybackResumeService(
            playbackHistoryRepository,
            showRepository,
            queueManager,
            mediaControllerRepository
        )
    }
    
    /**
     * Provides LastPlayedTrackService for simple last track persistence.
     * Works like Spotify - just saves and restores the last track and position.
     */
    @Provides
    @Singleton
    fun provideLastPlayedTrackService(
        @ApplicationContext context: Context,
        showRepository: com.deadly.core.data.api.repository.ShowRepository,
        queueManager: QueueManager,
        mediaControllerRepository: MediaControllerRepository
    ): LastPlayedTrackService {
        return LastPlayedTrackService(context, showRepository, queueManager, mediaControllerRepository)
    }
    
    /**
     * Provides LastPlayedTrackMonitor for continuously saving current playback state.
     * Automatically starts monitoring when created.
     */
    @Provides
    @Singleton
    fun provideLastPlayedTrackMonitor(
        mediaControllerRepository: MediaControllerRepository,
        lastPlayedTrackService: LastPlayedTrackService
    ): LastPlayedTrackMonitor {
        val monitor = LastPlayedTrackMonitor(mediaControllerRepository, lastPlayedTrackService)
        // Start monitoring automatically
        monitor.startMonitoring()
        return monitor
    }
}