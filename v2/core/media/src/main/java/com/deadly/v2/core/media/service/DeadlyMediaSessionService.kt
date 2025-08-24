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
import com.deadly.core.model.Track

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
     * Set queue for a recording with format
     * Returns true if queue was replaced, false if same recording/format
     */
    fun setQueueForRecording(recordingId: String, format: String, tracks: List<Track>): Boolean {
        Log.d(TAG, "setQueueForRecording: $recordingId ($format) with ${tracks.size} tracks")
        
        val isCurrentQueue = currentRecordingId == recordingId && currentFormat == format
        
        if (!isCurrentQueue) {
            Log.d(TAG, "Different recording/format - replacing queue")
            
            // Convert tracks to MediaItems
            val mediaItems = tracks.map { track -> createMediaItem(track, recordingId) }
            
            // Set queue on ExoPlayer
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            
            // Update current queue tracking
            currentRecordingId = recordingId
            currentFormat = format
            
            return true // Queue was replaced
        } else {
            Log.d(TAG, "Same recording/format - keeping existing queue")
            return false // Same queue
        }
    }
    
    /**
     * Play specific track index in current queue
     */
    fun playTrackIndex(trackIndex: Int) {
        Log.d(TAG, "playTrackIndex: $trackIndex")
        
        if (trackIndex >= 0 && trackIndex < exoPlayer.mediaItemCount) {
            exoPlayer.seekTo(trackIndex, 0)
            exoPlayer.playWhenReady = true
        } else {
            Log.w(TAG, "Invalid track index: $trackIndex (queue size: ${exoPlayer.mediaItemCount})")
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause - currently playing: ${exoPlayer.isPlaying}")
        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
    }
    
    /**
     * Get test tracks for a recording - simple hardcoded for now
     */
    fun getTestTracks(recordingId: String): List<Track> {
        return listOf(
            Track(
                filename = "gd77-05-08eaton-d3t01.mp3",
                title = "Jack Straw",
                trackNumber = "1", 
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t01.mp3"
            ),
            Track(
                filename = "gd77-05-08eaton-d3t02.mp3",
                title = "Scarlet Begonias", 
                trackNumber = "2",
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t02.mp3"
            ),
            Track(
                filename = "gd77-05-08eaton-d3t03.mp3",
                title = "Fire on the Mountain", 
                trackNumber = "3",
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t03.mp3"
            )
        )
    }
    
    /**
     * Create MediaItem from Track
     */
    private fun createMediaItem(track: Track, recordingId: String): MediaItem {
        val uri = track.streamingUrl ?: "https://archive.org/download/$recordingId/${track.filename}"
        
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.displayTitle)
                    .setArtist("Grateful Dead")
                    .setAlbumTitle(recordingId) // TODO: Better show info
                    .build()
            )
            .build()
    }
    
    /**
     * MediaSession callback for standard commands
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        // MediaSession.Callback methods will handle player commands automatically
        // No need to override unless we need custom behavior
    }
}