package com.deadly.core.media.service

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
import com.deadly.core.media.player.PlayerNotificationManager
import com.deadly.core.media.player.MediaControllerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    
    @Inject
    lateinit var mediaControllerRepository: MediaControllerRepository
    
    private var mediaSession: MediaSession? = null
    
    // Note: Queue management is handled natively by Media3/ExoPlayer
    // No custom queue tracking needed - ExoPlayer maintains the playlist state
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "dead_archive_playback",
                "Deadly Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        createNotificationChannel()
        initializePlayer()
        createMediaSession()
        setupNotifications()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: $intent")
        super.onStartCommand(intent, flags, startId)
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
                
                // Add delay to allow CurrentTrackInfo to be updated by PlaybackStateSync
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500) // Wait for CurrentTrackInfo to be updated
                    updateMediaMetadata(mediaItem)
                    updateNotification()
                }
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
            .build()
        
        Log.d(TAG, "MediaSession created successfully")
    }
    
    /**
     * Setup notification management
     * Media3 MediaSessionService handles notifications automatically
     */
    private fun setupNotifications() {
        Log.d(TAG, "Setting up notifications")
        val session = mediaSession ?: return
        notificationManager.initialize(this, session)
    }
    
    /**
     * Update notification with current playback state
     * Media3 handles this automatically based on MediaSession state
     */
    private fun updateNotification() {
        Log.d(TAG, "Notification will be updated automatically by Media3")
    }
    
    /**
     * Update MediaMetadata for current track using rich CurrentTrackInfo
     */
    private fun updateMediaMetadata(mediaItem: MediaItem?) {
        mediaItem?.let { item ->
            // Get rich track info from MediaControllerRepository
            val currentTrackInfo = mediaControllerRepository.currentTrackInfo.value
            
            if (currentTrackInfo != null) {
                Log.d(TAG, "Using CurrentTrackInfo for rich metadata: ${currentTrackInfo.songTitle}")
                
                // Use rich metadata from CurrentTrackInfo
                val metadata = MediaMetadata.Builder()
                    .setTitle(currentTrackInfo.displayTitle)  // Proper track name
                    .setArtist(currentTrackInfo.displayArtist) // Date + venue + location
                    .setAlbumTitle(currentTrackInfo.albumTitle) // Show date + venue
                    .setDisplayTitle(currentTrackInfo.displayTitle)
                    .build()
                
                // Update the MediaSession's metadata
                mediaSession?.let { session ->
                    // Set the metadata directly on the player's current MediaItem
                    val currentIndex = player.currentMediaItemIndex
                    if (currentIndex >= 0) {
                        val updatedItem = item.buildUpon()
                            .setMediaMetadata(metadata)
                            .build()
                        
                        // Replace the current MediaItem with updated metadata
                        player.replaceMediaItem(currentIndex, updatedItem)
                    }
                    
                    Log.d(TAG, "Updated rich metadata - Title: ${metadata.title}, Artist: ${metadata.artist}")
                }
            } else {
                Log.d(TAG, "No CurrentTrackInfo available, using basic metadata")
                
                // Fallback to basic metadata
                val metadata = MediaMetadata.Builder()
                    .setTitle(item.mediaMetadata.title ?: "Unknown Track")
                    .setArtist(item.mediaMetadata.artist ?: "Grateful Dead")
                    .setAlbumTitle(item.mediaMetadata.albumTitle ?: "Deadly")
                    .setDisplayTitle(item.mediaMetadata.displayTitle ?: item.mediaMetadata.title)
                    .build()
                
                mediaSession?.let { session ->
                    // Set the metadata directly on the player's current MediaItem
                    val currentIndex = player.currentMediaItemIndex
                    if (currentIndex >= 0) {
                        val updatedItem = item.buildUpon()
                            .setMediaMetadata(metadata)
                            .build()
                        
                        // Replace the current MediaItem with updated metadata
                        player.replaceMediaItem(currentIndex, updatedItem)
                    }
                    
                    Log.d(TAG, "Updated basic metadata for: ${metadata.title}")
                }
            }
        }
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
            
            // Ensure the service is in foreground when media is loaded
            if (mediaItems.isNotEmpty()) {
                Log.d(TAG, "Starting foreground service for media playback")
                try {
                    // Create a basic notification for foreground service
                    val notification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.app.Notification.Builder(this@DeadArchivePlaybackService, "dead_archive_playback")
                            .setContentTitle("Deadly")
                            .setContentText("Loading media...")
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        android.app.Notification.Builder(this@DeadArchivePlaybackService)
                            .setContentTitle("Deadly")
                            .setContentText("Loading media...")
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .build()
                    }
                    
                    startForeground(NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground service", e)
                }
            }
            
            Log.d(TAG, "Returning items to MediaSession for native playlist management")
            
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
            
            // Note: Queue management handled natively by Media3/ExoPlayer
            // No custom queue tracking needed - Media3 will add to its native playlist
            
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