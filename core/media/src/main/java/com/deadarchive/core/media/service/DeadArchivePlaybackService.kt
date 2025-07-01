package com.deadarchive.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.deadarchive.core.media.player.PlayerNotificationManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Enhanced MediaSessionService that manages ExoPlayer lifecycle, playlist queue,
 * and external media controls for background playback support.
 */
@UnstableApi
@AndroidEntryPoint
class DeadArchivePlaybackService : MediaSessionService() {
    
    companion object {
        private const val TAG = "DeadArchivePlaybackService"
        
        // Custom session commands
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
        private const val CUSTOM_COMMAND_TOGGLE_REPEAT = "TOGGLE_REPEAT"
        
        // Service lifecycle constants
        private const val NOTIFICATION_ID = 1001
    }
    
    @Inject
    lateinit var player: ExoPlayer
    
    @Inject
    lateinit var notificationManager: PlayerNotificationManager
    
    private var mediaSession: MediaSession? = null
    
    // Queue and state management
    private val currentQueue = mutableListOf<MediaItem>()
    private var currentQueueIndex = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        initializePlayer()
        createMediaSession()
        setupNotifications()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: $intent")
        return START_STICKY // Keep service running for background playback
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "MediaController connecting: ${controllerInfo.packageName}")
        return mediaSession
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        notificationManager.release()
        super.onDestroy()
    }
    
    /**
     * Initialize ExoPlayer with proper configuration for music playback
     */
    private fun initializePlayer() {
        Log.d(TAG, "Initializing ExoPlayer")
        
        // Configure audio attributes for music playback
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        
        player.setAudioAttributes(audioAttributes, true)
        
        // Add player listener for state changes
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed: $playbackState")
                updateNotification()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "Is playing changed: $isPlaying")
                updateNotification()
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "Media item transition: ${mediaItem?.mediaId}")
                updateMediaMetadata(mediaItem)
                updateNotification()
            }
        })
    }
    
    /**
     * Create MediaSession with comprehensive callback support
     */
    private fun createMediaSession() {
        Log.d(TAG, "Creating MediaSession")
        
        val sessionCallback = DeadArchiveSessionCallback()
        
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .setSessionActivity(createSessionActivityPendingIntent())
            .setCustomLayout(createCustomCommandButtons())
            .build()
        
        Log.d(TAG, "MediaSession created successfully")
    }
    
    /**
     * Setup notification management
     */
    private fun setupNotifications() {
        Log.d(TAG, "Setting up notifications")
        val session = mediaSession ?: return
        notificationManager.initialize(this, session)
    }
    
    /**
     * Update notification with current playback state
     */
    private fun updateNotification() {
        if (player.isPlaying) {
            notificationManager.showNotification(this)
        }
    }
    
    /**
     * Update MediaMetadata for current track
     */
    private fun updateMediaMetadata(mediaItem: MediaItem?) {
        mediaItem?.let { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.mediaMetadata.title ?: "Unknown Track")
                .setArtist(item.mediaMetadata.artist ?: "Grateful Dead")
                .setAlbumTitle(item.mediaMetadata.albumTitle ?: "Dead Archive")
                .setDisplayTitle(item.mediaMetadata.displayTitle ?: item.mediaMetadata.title)
                .build()
            
            // Update the MediaSession's metadata
            mediaSession?.let { session ->
                // Create updated MediaItem with enhanced metadata
                val updatedItem = item.buildUpon()
                    .setMediaMetadata(metadata)
                    .build()
                
                Log.d(TAG, "Updated metadata for: ${metadata.title}")
            }
        }
    }
    
    /**
     * Create custom command buttons for extended controls
     */
    private fun createCustomCommandButtons(): ImmutableList<CommandButton> {
        val shuffleButton = CommandButton.Builder()
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE, Bundle()))
            .setIconResId(android.R.drawable.ic_menu_sort_alphabetically)
            .build()
        
        val repeatButton = CommandButton.Builder()
            .setDisplayName("Repeat")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_TOGGLE_REPEAT, Bundle()))
            .setIconResId(android.R.drawable.ic_menu_revert)
            .build()
        
        return ImmutableList.of(shuffleButton, repeatButton)
    }
    
    /**
     * Create PendingIntent for returning to the main activity
     */
    private fun createSessionActivityPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    /**
     * MediaSession callback implementation for handling external control commands
     */
    private inner class DeadArchiveSessionCallback : MediaSession.Callback {
        
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            Log.d(TAG, "=== NEW CONTROLLER CONNECTION ===")
            Log.d(TAG, "Package name: ${controller.packageName}")
            Log.d(TAG, "Controller UID: ${controller.uid}")
            Log.d(TAG, "Connection request from: ${if (controller.packageName == packageName) "UI/APP" else "SYSTEM"}")
            
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .build()
        }
        
        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            Log.d(TAG, "Controller post-connect: ${controller.packageName}")
            super.onPostConnect(session, controller)
        }
        
        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            Log.d(TAG, "Controller disconnected: ${controller.packageName}")
            super.onDisconnected(session, controller)
        }
        
        // Note: Individual playback control callbacks are not needed in Media3
        // The MediaSession automatically handles standard playback commands
        // and routes them to the underlying Player (ExoPlayer in our case)
        
        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            Log.d(TAG, "=== SET MEDIA ITEMS COMMAND RECEIVED ===")
            Log.d(TAG, "From controller: ${controller.packageName}")
            Log.d(TAG, "Items count: ${mediaItems.size}")
            Log.d(TAG, "Start index: $startIndex")
            Log.d(TAG, "Start position: $startPositionMs")
            
            mediaItems.forEachIndexed { index, item ->
                Log.d(TAG, "Item $index: ${item.mediaId} - ${item.mediaMetadata.title}")
            }
            
            // Update internal queue
            currentQueue.clear()
            currentQueue.addAll(mediaItems)
            currentQueueIndex = startIndex
            
            Log.d(TAG, "Internal queue updated, returning items to MediaSession")
            
            // Return the media items with start position
            val result = MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            return Futures.immediateFuture(result)
        }
        
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            Log.d(TAG, "onAddMediaItems: ${mediaItems.size} items")
            
            // Add to internal queue
            currentQueue.addAll(mediaItems)
            
            // Return the media items to be added
            return Futures.immediateFuture(mediaItems)
        }
        
        // Custom commands
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Log.d(TAG, "onCustomCommand: ${customCommand.customAction}")
            
            when (customCommand.customAction) {
                CUSTOM_COMMAND_TOGGLE_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    Log.d(TAG, "Shuffle toggled: ${player.shuffleModeEnabled}")
                }
                CUSTOM_COMMAND_TOGGLE_REPEAT -> {
                    val newRepeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = newRepeatMode
                    Log.d(TAG, "Repeat mode changed to: $newRepeatMode")
                }
                else -> {
                    Log.w(TAG, "Unknown custom command: ${customCommand.customAction}")
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            }
            
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}