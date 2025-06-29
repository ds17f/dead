package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.mapper.ArchiveMapper
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

interface ConcertRepository {
    fun searchConcerts(query: String): Flow<List<Concert>>
    suspend fun getConcertById(id: String): Concert?
    fun getFavoriteConcerts(): Flow<List<Concert>>
    fun getAllCachedConcerts(): Flow<List<Concert>>
    
    // Streaming URL generation methods
    suspend fun getConcertMetadata(identifier: String): ArchiveMetadataResponse?
    suspend fun getStreamingUrl(identifier: String, filename: String): String?
    suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>>
    suspend fun getPreferredStreamingUrl(identifier: String): String?
    suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String?
}

@Singleton
class ConcertRepositoryImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService,
    private val concertDao: ConcertDao,
    private val favoriteDao: FavoriteDao
) : ConcertRepository {
    
    companion object {
        private const val CACHE_EXPIRY_HOURS = 24
    }
    
    /**
     * Search concerts using local-only precise matching strategy
     * Provides fast, consistent results from the complete local catalog
     */
    override fun searchConcerts(query: String): Flow<List<Concert>> = flow {
        val searchResults = performPreciseLocalSearch(query.trim())
        
        val concerts = searchResults.map { entity ->
            val isFavorite = favoriteDao.isConcertFavorite(entity.id)
            entity.toConcert().copy(isFavorite = isFavorite)
        }
        
        emit(concerts)
    }
    
    /**
     * Perform precise local search with field-specific matching
     * No API fallbacks - returns exactly what matches or empty results
     */
    private suspend fun performPreciseLocalSearch(query: String): List<ConcertEntity> {
        if (query.isBlank()) {
            // Return recent concerts for empty query
            return concertDao.getRecentConcerts(100)
        }
        
        return when {
            // Exact date patterns: 1977, 1977-05, 1977-05-08
            query.matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")) -> {
                searchByDatePattern(query)
            }
            
            // Year range: 1970s, 1980s
            query.matches(Regex("\\d{4}s")) -> {
                val decade = query.substring(0, 4).toInt()
                concertDao.searchConcertsByYearRange(decade, decade + 9)
            }
            
            // Venue-specific searches: Winterland, MSG, etc.
            isVenueQuery(query) -> {
                concertDao.searchConcertsByVenue(query)
            }
            
            // Location searches: Berkeley, Boston, NYC
            isCityQuery(query) -> {
                concertDao.searchConcertsByLocation(query)
            }
            
            // General text search across all fields
            else -> {
                concertDao.searchConcertsGeneral(query)
            }
        }
    }
    
    /**
     * Search by precise date patterns with no fallback
     */
    private suspend fun searchByDatePattern(dateQuery: String): List<ConcertEntity> {
        return when {
            // Full date: 1977-05-08
            dateQuery.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                concertDao.searchConcertsByExactDate(dateQuery)
            }
            
            // Year-month: 1977-05
            dateQuery.matches(Regex("\\d{4}-\\d{2}")) -> {
                concertDao.searchConcertsByYearMonth(dateQuery)
            }
            
            // Year only: 1977
            dateQuery.matches(Regex("\\d{4}")) -> {
                concertDao.searchConcertsByYear(dateQuery)
            }
            
            else -> emptyList()
        }
    }
    
    /**
     * Check if query is likely a venue search
     */
    private fun isVenueQuery(query: String): Boolean {
        val venueKeywords = setOf(
            "winterland", "fillmore", "garden", "arena", "coliseum", "stadium", 
            "theater", "theatre", "hall", "center", "dome", "pavilion", "forum"
        )
        return venueKeywords.any { keyword -> 
            query.contains(keyword, ignoreCase = true) 
        }
    }
    
    /**
     * Check if query is likely a city/location search
     */
    private fun isCityQuery(query: String): Boolean {
        val cityKeywords = setOf(
            "berkeley", "san francisco", "boston", "chicago", "detroit", 
            "philadelphia", "new york", "nyc", "buffalo", "atlanta"
        )
        return cityKeywords.any { city -> 
            query.contains(city, ignoreCase = true) 
        }
    }
    
    /**
     * Get concert by ID with cache-first strategy
     */
    override suspend fun getConcertById(id: String): Concert? {
        return try {
            // Check local cache first
            val cachedConcert = concertDao.getConcertById(id)
            if (cachedConcert != null && !isCacheExpired(cachedConcert.cachedTimestamp)) {
                val isFavorite = favoriteDao.isConcertFavorite(id)
                return cachedConcert.toConcert().copy(isFavorite = isFavorite)
            }
            
            // Fetch from API if not cached or expired
            val metadata = getConcertMetadata(id)
            metadata?.let { 
                val concert = ArchiveMapper.run { it.toConcert() }
                
                // Cache the result
                val isFavorite = favoriteDao.isConcertFavorite(id)
                val entity = ConcertEntity.fromConcert(concert, isFavorite)
                concertDao.insertConcert(entity)
                
                concert.copy(isFavorite = isFavorite)
            }
        } catch (e: Exception) {
            // Return cached version if available, even if expired
            concertDao.getConcertById(id)?.let { entity ->
                val isFavorite = favoriteDao.isConcertFavorite(id)
                entity.toConcert().copy(isFavorite = isFavorite)
            }
        }
    }
    
    /**
     * Get favorite concerts with real-time updates
     */
    override fun getFavoriteConcerts(): Flow<List<Concert>> {
        return concertDao.getFavoriteConcerts().map { entities ->
            entities.map { it.toConcert().copy(isFavorite = true) }
        }
    }
    
    /**
     * Get all cached concerts with favorite status
     */
    override fun getAllCachedConcerts(): Flow<List<Concert>> {
        return concertDao.getAllConcerts().map { entities ->
            entities.map { entity ->
                entity.toConcert().copy(isFavorite = entity.isFavorite)
            }
        }
    }
    
    /**
     * Cache concerts in local database
     */
    private suspend fun cacheConcerts(concerts: List<Concert>) {
        try {
            println("DEBUG: cacheConcerts called with ${concerts.size} concerts")
            
            val entities = concerts.map { concert ->
                println("DEBUG: Processing concert: ${concert.identifier} - ${concert.title}")
                val isFavorite = favoriteDao.isConcertFavorite(concert.identifier)
                println("DEBUG: isFavorite for ${concert.identifier}: $isFavorite")
                val entity = ConcertEntity.fromConcert(concert, isFavorite)
                println("DEBUG: Created entity with id: ${entity.id}, title: ${entity.title}")
                entity
            }
            
            println("DEBUG: About to insert ${entities.size} entities into database")
            concertDao.insertConcerts(entities)
            println("DEBUG: Successfully inserted entities into database")
            
            // Verify insertion by checking if we can find them
            val insertedCount = entities.map { entity ->
                concertDao.concertExists(entity.id)
            }.sum()
            println("DEBUG: Verification - ${insertedCount} out of ${entities.size} concerts exist in database")
            
            // Clean up old cached concerts (keep favorites)
            cleanupOldCache()
            println("DEBUG: Cache cleanup completed")
            
        } catch (e: Exception) {
            println("ERROR: cacheConcerts failed with exception: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Clean up old cached concerts
     */
    private suspend fun cleanupOldCache() {
        try {
            val cutoffTimestamp = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            println("DEBUG: Cleaning up cache with cutoff timestamp: $cutoffTimestamp")
            concertDao.cleanupOldCachedConcerts(cutoffTimestamp)
            println("DEBUG: Cache cleanup completed successfully")
        } catch (e: Exception) {
            println("ERROR: Cache cleanup failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        val expiryTime = timestamp + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000)
        return System.currentTimeMillis() > expiryTime
    }
    
    /**
     * Fetch metadata for a concert from Archive.org
     */
    override suspend fun getConcertMetadata(identifier: String): ArchiveMetadataResponse? {
        return try {
            val response = archiveApiService.getConcertMetadata(identifier)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: HttpException) {
            null
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            // JSON parsing errors, etc.
            null
        }
    }
    
    /**
     * Generate a direct streaming URL for a specific file
     * 
     * @param identifier Archive.org concert identifier
     * @param filename Name of the audio file
     * @return Direct streaming URL or null if metadata unavailable
     */
    override suspend fun getStreamingUrl(identifier: String, filename: String): String? {
        val metadata = getConcertMetadata(identifier) ?: return null
        
        // Get server and directory info from metadata
        val server = metadata.server ?: metadata.workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directory = metadata.directory ?: "0"
        
        // URL encode the filename to handle spaces and special characters
        val encodedFilename = URLEncoder.encode(filename, "UTF-8")
        
        // Construct the direct streaming URL
        // Note: directory already includes the full path like "/1/items/identifier"
        val streamingUrl = "https://$server$directory/$encodedFilename"
        println("DEBUG: Generated URL: server=$server, directory=$directory, filename=$filename -> $streamingUrl")
        return streamingUrl
    }
    
    /**
     * Get streaming URLs for all audio tracks in a concert
     * 
     * @param identifier Archive.org concert identifier
     * @return List of pairs (AudioFile, StreamingURL)
     */
    override suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>> {
        val metadata = getConcertMetadata(identifier) ?: return emptyList()
        
        // Get server and directory info
        val server = metadata.server ?: metadata.workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directory = metadata.directory ?: "0"
        
        // Filter for audio files and generate URLs
        return metadata.files
            .filter { file -> isAudioFile(file.format) }
            .mapNotNull { file ->
                try {
                    val audioFile = AudioFile(
                        filename = file.name,
                        format = file.format,
                        sizeBytes = file.size,
                        durationSeconds = file.length,
                        bitrate = file.bitrate,
                        sampleRate = file.sampleRate,
                        md5Hash = file.md5,
                        sha1Hash = file.sha1,
                        crc32Hash = file.crc32
                    )
                    
                    val encodedFilename = URLEncoder.encode(file.name, "UTF-8")
                    val streamingUrl = "https://$server$directory/$encodedFilename"
                    
                    audioFile to streamingUrl
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { (audioFile, _) -> 
                // Sort by track number if available, otherwise by filename
                audioFile.filename
            }
    }
    
    /**
     * Get the preferred streaming URL for a concert (usually the best quality MP3)
     */
    override suspend fun getPreferredStreamingUrl(identifier: String): String? {
        val trackUrls = getTrackStreamingUrls(identifier)
        
        // Prefer MP3 files for streaming (better compatibility)
        val mp3Tracks = trackUrls.filter { (audioFile, _) -> 
            audioFile.format.lowercase().contains("mp3")
        }
        
        return if (mp3Tracks.isNotEmpty()) {
            // Get the first MP3 track (usually track 01)
            mp3Tracks.first().second
        } else {
            // Fallback to first available audio file
            trackUrls.firstOrNull()?.second
        }
    }
    
    /**
     * Get streaming URL for a specific track by title or track number
     */
    override suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String? {
        val trackUrls = getTrackStreamingUrls(identifier)
        
        // Try to find by filename match
        return trackUrls.find { (audioFile, _) -> 
            audioFile.filename.contains(trackQuery, ignoreCase = true)
        }?.second
    }
    
    private fun isAudioFile(format: String): Boolean {
        val audioFormats = setOf(
            "flac", "mp3", "vbr mp3", "ogg vorbis", "wav", "aiff", "ape", "wv"
        )
        return format.lowercase() in audioFormats
    }
}