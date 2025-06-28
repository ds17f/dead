package com.deadarchive.core.media

import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.deadarchive.core.media.player.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level interface for media playback in the Dead Archive app
 */
@UnstableApi
@Singleton
class MediaPlayer @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    
    val isPlaying: StateFlow<Boolean> = playerRepository.isPlaying
    val currentPosition: StateFlow<Long> = playerRepository.currentPosition
    val duration: StateFlow<Long> = playerRepository.duration
    val playbackState: StateFlow<Int> = playerRepository.playbackState
    val lastError: StateFlow<PlaybackException?> = playerRepository.lastError
    
    /**
     * Play a single track from Archive.org
     * @param url Direct URL to the audio file
     * @param title Track title
     * @param artist Artist name (usually "Grateful Dead")
     */
    fun playTrack(url: String, title: String, artist: String = "Grateful Dead") {
        playerRepository.playTrack(url, title, artist)
    }
    
    /**
     * Play a local asset file
     * @param assetFileName Name of the file in the assets folder
     * @param title Track title
     * @param artist Artist name (usually "Grateful Dead")
     */
    fun playLocalAsset(assetFileName: String, title: String, artist: String = "Grateful Dead") {
        val assetUrl = "asset:///$assetFileName"
        playerRepository.playTrack(assetUrl, title, artist)
    }
    
    /**
     * Play a concert (multiple tracks)
     * @param urls List of track URLs
     */
    fun playConcert(urls: List<String>) {
        playerRepository.playPlaylist(urls)
    }
    
    /**
     * Resume/start playback
     */
    fun play() {
        playerRepository.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        playerRepository.pause()
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        playerRepository.stop()
    }
    
    /**
     * Seek to specific position
     * @param position Position in milliseconds
     */
    fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        playerRepository.skipToNext()
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        playerRepository.skipToPrevious()
    }
    
    /**
     * Set repeat mode
     * @param repeatMode One of Player.REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL
     */
    fun setRepeatMode(repeatMode: Int) {
        playerRepository.setRepeatMode(repeatMode)
    }
    
    /**
     * Enable/disable shuffle mode
     */
    fun setShuffleMode(enabled: Boolean) {
        playerRepository.setShuffleMode(enabled)
    }
    
    /**
     * Update position - should be called periodically by UI
     */
    fun updatePosition() {
        playerRepository.updatePosition()
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