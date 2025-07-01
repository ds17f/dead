package com.deadarchive.core.media

import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.deadarchive.core.media.player.MediaControllerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level interface for media playback in the Dead Archive app
 * Now uses service-based MediaControllerRepository for proper background playback
 */
@UnstableApi
@Singleton
class MediaPlayer @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    val currentPosition: StateFlow<Long> = mediaControllerRepository.currentPosition
    val duration: StateFlow<Long> = mediaControllerRepository.duration
    val playbackState: StateFlow<Int> = mediaControllerRepository.playbackState
    val lastError: StateFlow<PlaybackException?> = mediaControllerRepository.lastError
    
    /**
     * Play a single track from Archive.org
     * @param url Direct URL to the audio file
     * @param title Track title
     * @param artist Artist name (usually "Grateful Dead")
     */
    fun playTrack(url: String, title: String, artist: String = "Grateful Dead") {
        mediaControllerRepository.playTrack(url, title, artist)
    }
    
    /**
     * Play a local asset file
     * @param assetFileName Name of the file in the assets folder
     * @param title Track title
     * @param artist Artist name (usually "Grateful Dead")
     */
    fun playLocalAsset(assetFileName: String, title: String, artist: String = "Grateful Dead") {
        val assetUrl = "asset:///$assetFileName"
        mediaControllerRepository.playTrack(assetUrl, title, artist)
    }
    
    /**
     * Play a concert (multiple tracks)
     * @param urls List of track URLs
     */
    fun playConcert(urls: List<String>) {
        mediaControllerRepository.playPlaylist(urls)
    }
    
    /**
     * Resume/start playback
     */
    fun play() {
        mediaControllerRepository.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        mediaControllerRepository.pause()
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        mediaControllerRepository.stop()
    }
    
    /**
     * Seek to specific position
     * @param position Position in milliseconds
     */
    fun seekTo(position: Long) {
        mediaControllerRepository.seekTo(position)
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        mediaControllerRepository.skipToNext()
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        mediaControllerRepository.skipToPrevious()
    }
    
    /**
     * Set repeat mode
     * @param repeatMode One of Player.REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL
     */
    fun setRepeatMode(repeatMode: Int) {
        mediaControllerRepository.setRepeatMode(repeatMode)
    }
    
    /**
     * Enable/disable shuffle mode
     */
    fun setShuffleMode(enabled: Boolean) {
        mediaControllerRepository.setShuffleMode(enabled)
    }
    
    /**
     * Update position - should be called periodically by UI
     */
    fun updatePosition() {
        mediaControllerRepository.updatePosition()
    }
    
    /**
     * Check if player has next track
     */
    fun hasNext(): Boolean {
        return playbackState.value != Player.STATE_ENDED
    }
    
    /**
     * Check if player has previous track
     */
    fun hasPrevious(): Boolean {
        return currentPosition.value > 0
    }
}