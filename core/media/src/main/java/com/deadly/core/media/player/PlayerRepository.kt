package com.deadly.core.media.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlayerRepository @Inject constructor(
    private val player: ExoPlayer
) {
    
    companion object {
        private const val TAG = "PlayerRepository"
    }
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()
    
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    private val _lastError = MutableStateFlow<PlaybackException?>(null)
    val lastError: StateFlow<PlaybackException?> = _lastError.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: $isPlaying")
            _isPlaying.value = isPlaying
            
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "onPlaybackStateChanged: $playbackState")
            _playbackState.value = playbackState
            
            // Update duration when ready
            if (playbackState == Player.STATE_READY) {
                _duration.value = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                Log.d(TAG, "Duration updated: ${_duration.value}")
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaId}")
            _currentTrack.value = mediaItem
        }
        
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "onPlayerError: ${error.message}", error)
            _lastError.value = error
            stopPositionUpdates()
        }
    }
    
    init {
        Log.d(TAG, "PlayerRepository initialized")
        player.addListener(playerListener)
    }
    
    fun playTrack(url: String, title: String, artist: String? = null) {
        Log.d(TAG, "playTrack: URL=$url, title=$title")
        
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(url)
                .build()
            
            Log.d(TAG, "playTrack: Setting media item and preparing")
            player.setMediaItem(mediaItem)
            player.prepare()
            
            // Wait a moment for the player to be prepared before calling play
            player.playWhenReady = true
            player.play()
            
            Log.d(TAG, "playTrack: Player state after play() call: ${player.playbackState}")
            Log.d(TAG, "playTrack: playWhenReady: ${player.playWhenReady}")
        } catch (e: Exception) {
            Log.e(TAG, "playTrack: Exception", e)
        }
    }
    
    fun playPlaylist(urls: List<String>) {
        val mediaItems = urls.map { url ->
            MediaItem.Builder()
                .setUri(url)
                .setMediaId(url)
                .build()
        }
        
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }
    
    fun play() {
        Log.d(TAG, "play: Calling player.play(), current state: ${player.playbackState}, playWhenReady: ${player.playWhenReady}")
        player.play()
        Log.d(TAG, "play: After play() call, state: ${player.playbackState}, playWhenReady: ${player.playWhenReady}")
    }
    
    fun pause() {
        player.pause()
    }
    
    fun stop() {
        player.stop()
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)
    }
    
    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }
    
    fun skipToPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }
    
    fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }
    
    fun setShuffleMode(shuffleMode: Boolean) {
        player.shuffleModeEnabled = shuffleMode
    }
    
    private fun startPositionUpdates() {
        stopPositionUpdates() // Stop any existing job
        
        positionUpdateJob = coroutineScope.launch {
            while (isActive && player.isPlaying) {
                _currentPosition.value = player.currentPosition
                delay(1000L) // Update every second
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    fun updatePosition() {
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    }
    
    fun release() {
        Log.d(TAG, "release: Releasing player resources")
        stopPositionUpdates()
        player.removeListener(playerListener)
        player.release()
    }
}