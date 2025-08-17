package com.deadarchive.v2.core.database.service

import android.util.Log
import com.deadarchive.v2.core.database.dao.DataVersionDao
import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.ShowFtsDao
import com.deadarchive.v2.core.database.dao.RecordingDao
import com.deadarchive.v2.core.database.entities.DataVersionEntity
import com.deadarchive.v2.core.database.entities.ShowEntity
import com.deadarchive.v2.core.database.entities.ShowFtsEntity
import com.deadarchive.v2.core.database.entities.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ShowImportData(
    @SerialName("show_id") val showId: String,
    val band: String,
    val venue: String,
    @SerialName("location_raw") val locationRaw: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = "USA",
    val date: String,
    val url: String? = null,
    @SerialName("setlist_status") val setlistStatus: String? = null,
    val setlist: List<SetImportData>? = null,
    val recordings: List<RecordingImportData>? = null
)

@Serializable
data class SetImportData(
    @SerialName("set_name") val setName: String,
    val songs: List<SongImportData>
)

@Serializable
data class SongImportData(
    val name: String,
    val position: Int? = null,
    @SerialName("segue_into_next") val segueIntoNext: Boolean = false
)

@Serializable
data class RecordingImportData(
    val identifier: String,
    @SerialName("source_type") val sourceType: String? = null,
    val rating: Double = 0.0,
    @SerialName("raw_rating") val rawRating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    val confidence: Double = 0.0,
    @SerialName("high_ratings") val highRatings: Int = 0,
    @SerialName("low_ratings") val lowRatings: Int = 0
)

@Singleton
class DataImportService @Inject constructor(
    private val showDao: ShowDao,
    private val showFtsDao: ShowFtsDao,
    private val recordingDao: RecordingDao,
    private val dataVersionDao: DataVersionDao
) {
    
    companion object {
        private const val TAG = "DataImportService"
    }
    
    /**
     * Import shows from JSON file - simplified for 3-entity structure
     */
    suspend fun importShowsFromJson(
        jsonFile: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): ImportResult = withContext(Dispatchers.IO) {
        
        Log.i(TAG, "Starting simplified import from: ${jsonFile.name}")
        
        try {
            // Parse JSON
            val jsonContent = jsonFile.readText()
            val shows = Json.decodeFromString<List<ShowImportData>>(jsonContent)
            
            Log.i(TAG, "Parsed ${shows.size} shows from JSON")
            
            var importedShows = 0
            var importedRecordings = 0
            
            // Clear existing data
            showFtsDao.deleteAll()
            recordingDao.deleteAllRecordings()
            showDao.deleteAll()
            
            // Import shows and recordings
            shows.forEachIndexed { index, showData ->
                try {
                    // Create ShowEntity with denormalized venue data
                    val showEntity = createShowEntity(showData)
                    showDao.insert(showEntity)
                    importedShows++
                    
                    // Create FTS entry for search
                    val ftsEntity = createFtsEntity(showData)
                    showFtsDao.insert(ftsEntity)
                    
                    // Import recordings for this show
                    showData.recordings?.forEach { recordingData ->
                        val recordingEntity = createRecordingEntity(recordingData, showData.showId)
                        recordingDao.insertRecording(recordingEntity)
                        importedRecordings++
                    }
                    
                    onProgress(index + 1, shows.size)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing show: ${showData.showId}", e)
                }
            }
            
            // Update data version
            val currentTime = System.currentTimeMillis()
            dataVersionDao.insertOrUpdate(
                DataVersionEntity(
                    id = 1,
                    dataVersion = "2.0.0",
                    packageName = "Dead Archive Metadata",
                    versionType = "release", 
                    description = "Simplified V2 database import",
                    importedAt = currentTime,
                    gitCommit = null,
                    gitTag = null,
                    buildTimestamp = null,
                    totalShows = importedShows,
                    totalVenues = 0,
                    totalFiles = importedRecordings,
                    totalSizeBytes = 0L
                )
            )
            
            Log.i(TAG, "Import completed: $importedShows shows, $importedRecordings recordings")
            
            ImportResult(
                success = true,
                importedShows = importedShows,
                importedRecordings = importedRecordings,
                message = "Successfully imported $importedShows shows and $importedRecordings recordings"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportResult(
                success = false,
                importedShows = 0,
                importedRecordings = 0,
                message = "Import failed: ${e.message}"
            )
        }
    }
    
    private fun createShowEntity(showData: ShowImportData): ShowEntity {
        // Parse date components
        val dateParts = showData.date.split("-")
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt()
        val yearMonth = "${year}-${dateParts[1]}"
        
        // Create setlist JSON and flattened song list  
        val setlistRaw = showData.setlist?.let { sets ->
            Json.encodeToString(sets)
        }
        
        val songList = showData.setlist?.flatMap { set ->
            set.songs.map { it.name }
        }?.joinToString(",")
        
        val currentTime = System.currentTimeMillis()
        
        return ShowEntity(
            showId = showData.showId,
            date = showData.date,
            year = year,
            month = month,
            yearMonth = yearMonth,
            band = showData.band,
            url = showData.url,
            venueName = showData.venue, // Denormalized venue data
            city = showData.city,
            state = showData.state,
            country = showData.country ?: "USA",
            locationRaw = showData.locationRaw,
            setlistStatus = showData.setlistStatus,
            setlistRaw = setlistRaw,
            songList = songList,
            showSequence = 1,
            recordingCount = showData.recordings?.size ?: 0,
            bestRecordingId = showData.recordings?.maxByOrNull { it.rating }?.identifier,
            averageRating = showData.recordings?.map { it.rawRating }?.average()?.toFloat(),
            totalReviews = showData.recordings?.sumOf { it.reviewCount } ?: 0,
            isInLibrary = false,
            libraryAddedAt = null,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
    
    private fun createFtsEntity(showData: ShowImportData): ShowFtsEntity {
        // Combine all searchable text for FTS
        val searchableText = buildList {
            add(showData.date)
            add(showData.venue)
            showData.city?.let { add(it) }
            showData.state?.let { add(it) }
            showData.locationRaw?.let { add(it) }
            showData.setlist?.flatMap { set ->
                set.songs.map { it.name }
            }?.let { addAll(it) }
        }.joinToString(" ")
        
        return ShowFtsEntity(searchText = searchableText)
    }
    
    private fun createRecordingEntity(recordingData: RecordingImportData, showId: String): RecordingEntity {
        return RecordingEntity(
            identifier = recordingData.identifier,
            showId = showId,
            sourceType = recordingData.sourceType,
            rating = recordingData.rating,
            rawRating = recordingData.rawRating,
            reviewCount = recordingData.reviewCount,
            confidence = recordingData.confidence,
            highRatings = recordingData.highRatings,
            lowRatings = recordingData.lowRatings,
            collectionTimestamp = System.currentTimeMillis()
        )
    }
}

data class ImportResult(
    val success: Boolean,
    val importedShows: Int,
    val importedRecordings: Int,
    val message: String
)