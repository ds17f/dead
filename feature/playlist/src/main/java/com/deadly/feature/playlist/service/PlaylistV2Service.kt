package com.deadly.feature.playlist.service

import com.deadly.feature.playlist.model.PlaylistShowViewModel
import com.deadly.feature.playlist.model.PlaylistTrackViewModel
import kotlinx.coroutines.flow.Flow

/**
 * PlaylistV2Service - Clean service interface for playlist functionality
 * 
 * Following V2 architecture patterns and clean architecture principles,
 * this interface returns ViewModel types that represent UI concerns.
 * Real implementation will map domain models to ViewModels, stub provides dummy data.
 */
interface PlaylistV2Service {
    
    /**
     * Load show data for the playlist
     */
    suspend fun loadShow(showId: String?)
    
    /**
     * Get current show information as ViewModel
     */
    suspend fun getCurrentShowInfo(): PlaylistShowViewModel?
    
    /**
     * Get track list for current show as ViewModels
     */
    suspend fun getTrackList(): List<PlaylistTrackViewModel>
    
    /**
     * Play a specific track by index
     */
    suspend fun playTrack(trackIndex: Int)
    
    /**
     * Navigate to the next show chronologically
     */
    suspend fun navigateToNextShow()
    
    /**
     * Navigate to the previous show chronologically  
     */
    suspend fun navigateToPreviousShow()
    
    /**
     * Add current show to library
     */
    suspend fun addToLibrary()
    
    /**
     * Download current show
     */
    suspend fun downloadShow()
    
    /**
     * Share current show
     */
    suspend fun shareShow()
    
    /**
     * Load setlist for current show
     */
    suspend fun loadSetlist()
    
    /**
     * Pause playback
     */
    suspend fun pause()
    
    /**
     * Resume playback
     */
    suspend fun resume()
    
    /**
     * Get current reviews for the loaded show/recording
     */
    suspend fun getCurrentReviews(): List<com.deadly.feature.playlist.Review>
    
    /**
     * Get rating distribution for the current show/recording
     */
    suspend fun getRatingDistribution(): Map<Int, Int>
    
    /**
     * Get recording options for the current show
     */
    suspend fun getRecordingOptions(): com.deadly.feature.playlist.RecordingOptionsV2Result
    
    /**
     * Select a different recording for the current show
     */
    suspend fun selectRecording(recordingId: String)
    
    /**
     * Set a recording as the default for this show
     */
    suspend fun setRecordingAsDefault(recordingId: String)
    
    /**
     * Reset to the recommended recording for this show
     */
    suspend fun resetToRecommended()
}