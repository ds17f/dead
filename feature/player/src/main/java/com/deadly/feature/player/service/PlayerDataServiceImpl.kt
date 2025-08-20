package com.deadly.feature.player.service

import android.util.Log
import com.deadly.core.data.api.repository.ShowRepository
import com.deadly.core.data.repository.RatingsRepository
import com.deadly.core.data.service.RecordingSelectionService
import com.deadly.core.model.Recording
import com.deadly.core.model.Show
import com.deadly.core.model.util.VenueUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDataServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val ratingsRepository: RatingsRepository,
    private val recordingSelectionService: RecordingSelectionService
) : PlayerDataService {
    
    companion object {
        private const val TAG = "PlayerDataService"
    }
    
    override suspend fun loadRecording(recordingId: String, formatPreferences: List<String>): Recording? {
        Log.d(TAG, "loadRecording: Loading recording with ID: $recordingId")
        Log.d(TAG, "loadRecording: Using format preferences: $formatPreferences")
        
        return try {
            val recording = showRepository.getRecordingByIdWithFormatFilter(recordingId, formatPreferences)
            Log.d(TAG, "loadRecording: Repository returned recording: ${recording != null}")
            
            if (recording != null) {
                Log.d(TAG, "loadRecording: Found recording: ${recording.identifier} - ${recording.title}")
                Log.d(TAG, "loadRecording: Filtered tracks count: ${recording.tracks.size}")
                recording.tracks.forEachIndexed { index, track ->
                    Log.d(TAG, "loadRecording: Track $index - title: ${track.displayTitle}, format: ${track.audioFile?.format}")
                }
            }
            
            recording
        } catch (e: Exception) {
            Log.e(TAG, "loadRecording: Error loading recording $recordingId", e)
            null
        }
    }
    
    override fun generateShowId(recording: Recording): String {
        val normalizedDate = recording.concertDate?.substringBefore("T") ?: ""
        val normalizedVenue = VenueUtil.normalizeVenue(recording.concertVenue)
        return "${normalizedDate}_${normalizedVenue}"
    }
    
    override suspend fun getAlternativeRecordings(currentRecording: Recording): List<Recording> {
        return try {
            val showId = generateShowId(currentRecording)
            Log.d(TAG, "getAlternativeRecordings: Looking for alternatives to recording ${currentRecording.identifier} in show $showId")
            
            val recordings = showRepository.getRecordingsByShowId(showId)
            Log.d(TAG, "getAlternativeRecordings: Found ${recordings.size} recordings for show $showId")
            
            // Filter out the current recording
            val alternatives = recordings.filter { it.identifier != currentRecording.identifier }
            Log.d(TAG, "getAlternativeRecordings: Found ${alternatives.size} alternative recordings")
            
            alternatives
        } catch (e: Exception) {
            Log.e(TAG, "getAlternativeRecordings: Error getting alternatives", e)
            emptyList()
        }
    }
    
    override suspend fun findNextShowByDate(currentRecording: Recording): Show? {
        return try {
            val currentDate = currentRecording.concertDate?.substringBefore("T") ?: ""
            Log.d(TAG, "findNextShowByDate: Looking for next show after $currentDate")
            
            showRepository.getNextShowByDate(currentDate)
        } catch (e: Exception) {
            Log.e(TAG, "findNextShowByDate: Error finding next show", e)
            null
        }
    }
    
    override suspend fun findPreviousShowByDate(currentRecording: Recording): Show? {
        return try {
            val currentDate = currentRecording.concertDate?.substringBefore("T") ?: ""
            Log.d(TAG, "findPreviousShowByDate: Looking for previous show before $currentDate")
            
            showRepository.getPreviousShowByDate(currentDate)
        } catch (e: Exception) {
            Log.e(TAG, "findPreviousShowByDate: Error finding previous show", e)
            null
        }
    }
    
    override suspend fun getBestRecordingForShow(show: Show): Recording? {
        return try {
            Log.d(TAG, "getBestRecordingForShow: Getting best recording for show ${show.showId} using centralized service")
            
            val bestRecording = recordingSelectionService.getBestRecording(show)
            if (bestRecording != null) {
                Log.d(TAG, "getBestRecordingForShow: Selected best recording: ${bestRecording.identifier}")
            } else {
                Log.w(TAG, "getBestRecordingForShow: No best recording found for show ${show.showId}")
            }
            
            bestRecording
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingForShow: Error getting best recording", e)
            null
        }
    }
    
    override suspend fun getBestRecordingByShowId(showId: String): Recording? {
        return try {
            Log.d(TAG, "getBestRecordingByShowId: Getting best recording for showId: $showId using centralized service")
            
            // Get show data for the showId 
            val show = showRepository.getShowById(showId)
            if (show == null) {
                Log.w(TAG, "getBestRecordingByShowId: No show found for showId $showId")
                return null
            }
            
            Log.d(TAG, "getBestRecordingByShowId: Found show with ${show.recordings.size} recordings")
            
            val bestRecording = recordingSelectionService.getBestRecording(show)
            if (bestRecording != null) {
                Log.d(TAG, "getBestRecordingByShowId: Selected best recording: ${bestRecording.identifier}")
            } else {
                Log.w(TAG, "getBestRecordingByShowId: No best recording found for show $showId")
            }
            
            bestRecording
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingByShowId: Error getting recordings for show $showId", e)
            null
        }
    }
}