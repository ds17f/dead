package com.deadarchive.core.media.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlayerRepository @Inject constructor(
    private val player: ExoPlayer
) {
    
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
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = playbackState
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentTrack.value = mediaItem
        }
        
        override fun onPlayerError(error: PlaybackException) {
            _lastError.value = error
        }
    }
    
    init {
        player.addListener(playerListener)
        
        // Start periodic position updates
        startPositionUpdates()
    }
    
    fun playTrack(url: String, title: String, artist: String? = null) {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(url)
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
        player.play()
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
        // This would typically be done with a coroutine scope
        // For now, we'll rely on UI components to update position
    }
    
    fun updatePosition() {
        _currentPosition.value = player.currentPosition
        _duration.value = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    }
    
    fun release() {
        player.removeListener(playerListener)
    }
}