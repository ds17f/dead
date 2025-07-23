package com.deadarchive.feature.player.service

import android.util.Log
import com.deadarchive.core.data.api.repository.ShowRepository
import com.deadarchive.core.data.repository.RatingsRepository
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.util.VenueUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDataServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val ratingsRepository: RatingsRepository
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
            Log.d(TAG, "getBestRecordingForShow: Getting best recording for show ${show.showId}")
            
            // First try user's preferred recording
            val preferredRecordingId = show.bestRecordingId
            if (preferredRecordingId != null) {
                val preferredRecording = show.recordings.find { it.identifier == preferredRecordingId }
                if (preferredRecording != null) {
                    Log.d(TAG, "getBestRecordingForShow: Found preferred recording: ${preferredRecording.identifier}")
                    return preferredRecording
                }
            }
            
            // Fall back to first recording
            val firstRecording = show.recordings.firstOrNull()
            if (firstRecording != null) {
                Log.d(TAG, "getBestRecordingForShow: Using first recording: ${firstRecording.identifier}")
            } else {
                Log.w(TAG, "getBestRecordingForShow: No recordings found for show ${show.showId}")
            }
            
            firstRecording
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingForShow: Error getting best recording", e)
            null
        }
    }
}