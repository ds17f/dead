package com.deadarchive.feature.player.service

import android.util.Log
import com.deadarchive.core.media.player.QueueManager
import com.deadarchive.core.media.player.MediaControllerRepositoryRefactored
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerPlaylistServiceImpl @Inject constructor(
    private val queueManager: QueueManager,
    private val mediaControllerRepository: MediaControllerRepositoryRefactored
) : PlayerPlaylistService {
    
    companion object {
        private const val TAG = "PlayerPlaylistService"
    }
    
    private val _currentPlaylist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    override val currentPlaylist: StateFlow<List<PlaylistItem>> = _currentPlaylist.asStateFlow()
    
    private val _playlistTitle = MutableStateFlow<String?>(null)
    override val playlistTitle: StateFlow<String?> = _playlistTitle.asStateFlow()
    
    override fun setPlaylist(playlist: List<PlaylistItem>, title: String?) {
        Log.d(TAG, "setPlaylist: Setting playlist with ${playlist.size} items, title: $title")
        _playlistTitle.value = title
        _currentPlaylist.value = playlist
        
        // Log formats to verify filtering was applied
        playlist.take(3).forEach { item ->
            Log.d(TAG, "setPlaylist: Item - ${item.track.displayTitle} (${item.track.audioFile?.format})")
        }
    }
    
    override fun getCurrentPlaylist(): List<PlaylistItem> {
        return _currentPlaylist.value
    }
    
    override suspend fun navigateToTrack(playlistIndex: Int) {
        val playlist = _currentPlaylist.value
        Log.d(TAG, "navigateToTrack: Navigating to playlist index $playlistIndex")
        Log.d(TAG, "navigateToTrack: Current playlist size: ${playlist.size}")
        
        if (playlistIndex in playlist.indices) {
            val playlistItem = playlist[playlistIndex]
            Log.d(TAG, "navigateToTrack: Selected item - ${playlistItem.track.displayTitle}")
            
            try {
                playTrackFromPlaylist(playlistItem)
            } catch (e: Exception) {
                Log.e(TAG, "navigateToTrack: Error playing track", e)
            }
        } else {
            Log.w(TAG, "navigateToTrack: Invalid playlist index $playlistIndex for playlist size ${playlist.size}")
        }
    }
    
    override fun addToPlaylist(playlistItem: PlaylistItem) {
        val currentPlaylist = _currentPlaylist.value.toMutableList()
        currentPlaylist.add(playlistItem)
        _currentPlaylist.value = currentPlaylist
        
        Log.d(TAG, "addToPlaylist: Added '${playlistItem.track.displayTitle}' to playlist")
        Log.d(TAG, "addToPlaylist: New playlist size: ${currentPlaylist.size}")
    }
    
    override fun removeFromPlaylist(playlistIndex: Int) {
        val currentPlaylist = _currentPlaylist.value.toMutableList()
        
        if (playlistIndex in currentPlaylist.indices) {
            val removedItem = currentPlaylist.removeAt(playlistIndex)
            _currentPlaylist.value = currentPlaylist
            
            Log.d(TAG, "removeFromPlaylist: Removed '${removedItem.track.displayTitle}' from playlist at index $playlistIndex")
            Log.d(TAG, "removeFromPlaylist: New playlist size: ${currentPlaylist.size}")
        } else {
            Log.w(TAG, "removeFromPlaylist: Invalid index $playlistIndex for playlist size ${currentPlaylist.size}")
        }
    }
    
    override fun clearPlaylist() {
        Log.d(TAG, "clearPlaylist: Clearing playlist (current size: ${_currentPlaylist.value.size})")
        _currentPlaylist.value = emptyList()
        _playlistTitle.value = null
    }
    
    override suspend fun playTrackFromPlaylist(playlistItem: PlaylistItem) {
        val track = playlistItem.track
        Log.d(TAG, "playTrackFromPlaylist: Playing track '${track.displayTitle}' from concert '${playlistItem.concertIdentifier}'")
        
        try {
            val audioFile = track.audioFile
            val downloadUrl = audioFile?.downloadUrl
            
            if (downloadUrl != null) {
                Log.d(TAG, "playTrackFromPlaylist: Playing track with URL: $downloadUrl")
                mediaControllerRepository.playTrack(
                    url = downloadUrl,
                    title = track.displayTitle,
                    artist = _playlistTitle.value ?: "Unknown Artist"
                )
                Log.d(TAG, "playTrackFromPlaylist: Successfully started playback")
            } else {
                Log.w(TAG, "playTrackFromPlaylist: No download URL available for track ${track.displayTitle}")
                throw IllegalStateException("Audio file not available for this track")
            }
        } catch (e: Exception) {
            Log.e(TAG, "playTrackFromPlaylist: Error playing track from playlist", e)
            throw e
        }
    }
    
    override fun updateQueueContext(playlist: List<PlaylistItem>, forceUpdate: Boolean) {
        // Legacy method - queue operations should go through QueueManager instead
        // This method was already marked as legacy in the original PlayerViewModel
        Log.d(TAG, "updateQueueContext: Legacy method called - queue operations should use QueueManager")
    }
}