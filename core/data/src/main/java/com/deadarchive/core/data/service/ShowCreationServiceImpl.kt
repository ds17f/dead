package com.deadarchive.core.data.service

import android.util.Log
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.util.VenueUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowCreationServiceImpl @Inject constructor(
    private val showDao: ShowDao
) : ShowCreationService {
    
    companion object {
        private const val TAG = "ShowCreationService"
    }
    
    override suspend fun createAndSaveShowsFromRecordings(recordings: List<Recording>): List<Show> {
        Log.d(TAG, "🔧 createAndSaveShowsFromRecordings: Processing ${recordings.size} recordings")
        Log.d(TAG, "🔧 Recording identifiers: ${recordings.map { it.identifier }}")
        
        // Group recordings by show ID
        val groupedRecordings = groupRecordingsByShow(recordings)
        
        Log.d(TAG, "🔧 Grouped recordings into ${groupedRecordings.size} potential shows:")
        groupedRecordings.forEach { (showId, recordingGroup) ->
            Log.d(TAG, "🔧   Show '$showId': ${recordingGroup.size} recordings [${recordingGroup.map { it.identifier }}]")
        }
        
        val shows = mutableListOf<Show>()
        val newShowEntities = mutableListOf<ShowEntity>()
        val failedShows = mutableListOf<String>()
        
        for ((_, recordingGroup) in groupedRecordings) {
            try {
                val firstRecording = recordingGroup.first()
                val normalizedDate = normalizeDate(firstRecording.concertDate)
                
                // Validate we can create a proper show
                if (normalizedDate.isBlank()) {
                    failedShows.add("Invalid date for recordings: ${recordingGroup.map { it.identifier }}")
                    continue
                }
                
                val normalizedVenue = VenueUtil.normalizeVenue(firstRecording.concertVenue)
                val showId = "${normalizedDate}_${normalizedVenue}"
                
                // Debug: Show venue normalization in action
                if (firstRecording.concertVenue != normalizedVenue) {
                    Log.d(TAG, "Venue normalized: '${firstRecording.concertVenue}' → '$normalizedVenue'")
                }
                
                // Check if ShowEntity already exists
                val showExists = showDao.showExists(showId)
                Log.d(TAG, "🔧 Checking if show '$showId' exists in database: $showExists")
                
                if (showExists) {
                    Log.d(TAG, "🔧 SKIPPING show creation for '$showId' - already exists in database")
                } else {
                    Log.d(TAG, "🔧 WILL CREATE new show entity for '$showId'")
                }
                
                if (!showExists) {
                    // Create new ShowEntity - use original venue name for display
                    val showEntity = ShowEntity(
                        showId = showId, // Uses normalized venue for deduplication
                        date = normalizedDate,
                        venue = firstRecording.concertVenue, // Keep original readable venue name
                        location = firstRecording.concertLocation,
                        year = normalizedDate.take(4),
                        setlistRaw = null,
                        setsJson = null,
                        addedToLibraryAt = null, // Will be updated when added to library
                        cachedTimestamp = System.currentTimeMillis()
                    )
                    newShowEntities.add(showEntity)
                    Log.d(TAG, "🔧 Added new ShowEntity to creation list: $showId with ${recordingGroup.size} recordings")
                    Log.d(TAG, "🔧   ShowEntity details: date='$normalizedDate', venue='${firstRecording.concertVenue}', location='${firstRecording.concertLocation}'")
                }
                
                // Create Show object - use ORIGINAL venue name for display, normalized only for showId  
                val isInLibrary = false // Shows created from recordings are not initially in library
                val show = Show(
                    date = normalizedDate,
                    venue = firstRecording.concertVenue, // Keep original readable venue name
                    location = firstRecording.concertLocation,
                    year = normalizedDate.take(4),
                    recordings = recordingGroup,
                    isInLibrary = isInLibrary
                )
                shows.add(show)
                
            } catch (e: Exception) {
                failedShows.add("Failed to create show from recordings ${recordingGroup.map { it.identifier }}: ${e.message}")
                Log.e(TAG, "Failed to create show from recordings: ${e.message}")
            }
        }
        
        // Save new ShowEntity records
        if (newShowEntities.isNotEmpty()) {
            Log.d(TAG, "🔧 About to save ${newShowEntities.size} new ShowEntity records to database:")
            newShowEntities.forEach { entity ->
                Log.d(TAG, "🔧   Saving ShowEntity: showId='${entity.showId}', date='${entity.date}', venue='${entity.venue}'")
            }
            try {
                showDao.insertShows(newShowEntities)
                Log.d(TAG, "🔧 ✅ Successfully saved ${newShowEntities.size} new ShowEntity records to database")
                
                // Verify what we actually saved
                newShowEntities.forEach { entity ->
                    val exists = showDao.showExists(entity.showId)
                    Log.d(TAG, "🔧 Post-save verification: '${entity.showId}' exists in database: $exists")
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔧 ❌ CRITICAL: Failed to save ShowEntity records: ${e.message}")
                Log.e(TAG, "🔧 Exception details:", e)
                failedShows.add("Database save failed: ${e.message}")
            }
        } else {
            Log.d(TAG, "🔧 No new ShowEntity records to save - all shows already exist")
        }
        
        // Log any failures
        if (failedShows.isNotEmpty()) {
            Log.w(TAG, "Failed to create ${failedShows.size} shows:")
            failedShows.forEach { failure ->
                Log.w(TAG, "  - $failure")
            }
        }
        
        Log.d(TAG, "🔧 ✅ Successfully created ${shows.size} shows from ${recordings.size} recordings")
        Log.d(TAG, "🔧 Final show summary:")
        shows.forEach { show ->
            Log.d(TAG, "🔧   Show: showId='${show.showId}', date='${show.date}', venue='${show.venue}', ${show.recordings.size} recordings")
        }
        return shows.sortedByDescending { it.date }
    }
    
    override fun normalizeDate(date: String?): String {
        if (date.isNullOrBlank()) return ""
        return if (date.contains("T")) {
            date.substringBefore("T")
        } else {
            date
        }
    }
    
    override fun groupRecordingsByShow(recordings: List<Recording>): Map<String, List<Recording>> {
        return recordings.groupBy { recording ->
            val normalizedDate = normalizeDate(recording.concertDate)
            val normalizedVenue = VenueUtil.normalizeVenue(recording.concertVenue)
            val showId = "${normalizedDate}_${normalizedVenue}"
            Log.d(TAG, "🔧 Recording ${recording.identifier}: date='${recording.concertDate}' → '$normalizedDate', venue='${recording.concertVenue}' → '$normalizedVenue', showId='$showId'")
            showId
        }
    }
    
    override suspend fun createShowEntities(groupedRecordings: Map<String, List<Recording>>): List<ShowEntity> {
        val showEntities = mutableListOf<ShowEntity>()
        
        for ((showId, recordingGroup) in groupedRecordings) {
            try {
                val firstRecording = recordingGroup.first()
                val normalizedDate = normalizeDate(firstRecording.concertDate)
                
                if (normalizedDate.isNotBlank()) {
                    val showEntity = ShowEntity(
                        showId = showId,
                        date = normalizedDate,
                        venue = firstRecording.concertVenue,
                        location = firstRecording.concertLocation,
                        year = normalizedDate.take(4),
                        setlistRaw = null,
                        setsJson = null,
                        addedToLibraryAt = null,
                        cachedTimestamp = System.currentTimeMillis()
                    )
                    showEntities.add(showEntity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ShowEntity for showId $showId: ${e.message}")
            }
        }
        
        return showEntities
    }
}