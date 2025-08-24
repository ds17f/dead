package com.deadly.v2.core.media.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Simple MediaSessionService for V2 playback
 * 
 * Handles basic playback with queue management keyed by (recordingId, format).
 * Uses standard MediaSession commands - no custom serialization.
 */
@UnstableApi
class DeadlyMediaSessionService : MediaSessionService() {
    
    companion object {
        private const val TAG = "DeadlyMediaSessionService"
    }
    
    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    
    // Simple queue state tracking
    private var currentRecordingId: String? = null
    private var currentFormat: String? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")
        
        // Initialize ExoPlayer with audio attributes
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Add player listeners
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateString = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "Playback state: $stateString")
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
            }
        })
        
        // Create MediaSession
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("DeadlySession")
            .setCallback(MediaSessionCallback())
            .build()
        
        Log.d(TAG, "Service initialized successfully")
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "Client requesting session: ${controllerInfo.packageName}")
        return mediaSession
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy()")
        mediaSession?.run {
            release()
            mediaSession = null
        }
        exoPlayer.release()
        super.onDestroy()
    }
    
    
    
    
    /**
     * MediaSession callback for standard commands
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        // MediaSession.Callback methods will handle player commands automatically
        // No need to override unless we need custom behavior
    }
}