package com.deadly.feature.player.service

import com.deadly.core.model.PlaylistItem
import com.deadly.core.model.Track
import com.deadly.core.model.Recording
import kotlinx.coroutines.flow.StateFlow

/**
 * Service responsible for playlist management and queue operations.
 * Handles playlist state, adding/removing items, and playlist navigation.
 */
interface PlayerPlaylistService {
    
    /**
     * Current playlist items
     */
    val currentPlaylist: StateFlow<List<PlaylistItem>>
    
    /**
     * Current playlist title
     */
    val playlistTitle: StateFlow<String?>
    
    /**
     * Set the current playlist
     * @param playlist List of playlist items
     * @param title Optional playlist title
     */
    fun setPlaylist(playlist: List<PlaylistItem>, title: String? = null)
    
    /**
     * Get the current playlist
     * @return Current playlist items
     */
    fun getCurrentPlaylist(): List<PlaylistItem>
    
    /**
     * Navigate to a specific track in the playlist
     * @param playlistIndex Index of the track in the playlist
     */
    suspend fun navigateToTrack(playlistIndex: Int)
    
    /**
     * Add an item to the current playlist
     * @param playlistItem Item to add
     */
    fun addToPlaylist(playlistItem: PlaylistItem)
    
    /**
     * Remove an item from the playlist
     * @param playlistIndex Index of item to remove
     */
    fun removeFromPlaylist(playlistIndex: Int)
    
    /**
     * Clear the current playlist
     */
    fun clearPlaylist()
    
    /**
     * Play a track from the playlist
     * @param playlistItem The playlist item to play
     */
    suspend fun playTrackFromPlaylist(playlistItem: PlaylistItem)
    
    /**
     * Update the queue context with current playlist
     * @param playlist Current playlist
     * @param forceUpdate Whether to force update even if same
     */
    fun updateQueueContext(playlist: List<PlaylistItem>, forceUpdate: Boolean = false)
}