package com.deadly.core.data.repository

import android.content.Context
import com.deadly.core.database.SetlistDao
import com.deadly.core.database.SongDao
import com.deadly.core.database.VenueDao
import com.deadly.core.database.SetlistEntity
import com.deadly.core.database.SongEntity
import com.deadly.core.database.VenueEntity
import com.deadly.core.model.Setlist
import com.deadly.core.model.Song
import com.deadly.core.model.Venue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.util.zip.ZipInputStream

/**
 * Repository for managing setlist, song, and venue data.
 * 
 * This repository handles:
 * - Loading setlist data from consolidated data assets
 * - Caching setlist, song, and venue data in local database
 * - Providing setlist data for shows and performances
 * - Updating setlist data when new data is available
 */
@Singleton
class SetlistRepository @Inject constructor(
    private val setlistDao: SetlistDao,
    private val songDao: SongDao,
    private val venueDao: VenueDao,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SetlistRepository"
        private const val DATA_ZIP_FILE = "data.zip"
        private const val SETLISTS_FILE = "setlists.json"
        private const val SONGS_FILE = "songs.json"
        private const val VENUES_FILE = "venues.json"
        private const val DATA_CACHE_EXPIRY = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    
    /**
     * Initialize setlist data from assets if database is empty.
     */
    suspend fun initializeSetlistDataIfNeeded() {
        try {
            val setlistCount = setlistDao.getSetlistCount()
            val songCount = songDao.getSongCount()
            val venueCount = venueDao.getVenueCount()
            
            if (setlistCount == 0 && songCount == 0 && venueCount == 0) {
                Log.i(TAG, "Database is empty, loading setlist data from assets...")
                loadSetlistDataFromAssets()
            } else {
                Log.i(TAG, "Setlist data already loaded: $setlistCount setlists, $songCount songs, $venueCount venues")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize setlist data: ${e.message}", e)
        }
    }
    
    /**
     * Force refresh setlist data from assets, clearing existing data.
     */
    suspend fun forceRefreshSetlistData() {
        try {
            Log.i(TAG, "Force refreshing setlist data from assets...")
            loadSetlistDataFromAssets()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force refresh setlist data: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Force re-resolve song IDs to names in all setlists.
     */
    suspend fun forceResolveSongIds() {
        try {
            Log.i(TAG, "Force resolving song IDs in setlists...")
            resolveSongIdsInSetlists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force resolve song IDs: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Load and parse setlist data from the consolidated data assets file.
     */
    private suspend fun loadSetlistDataFromAssets() {
        try {
            val dataFiles = extractDataFromAssets()
            parseAndSaveSetlistData(dataFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load setlist data from assets: ${e.message}", e)
        }
    }
    
    /**
     * Extract data files from the consolidated data zip.
     */
    private suspend fun extractDataFromAssets(): Map<String, String> {
        Log.i(TAG, "🗜️ Extracting data from $DATA_ZIP_FILE...")
        val dataFiles = mutableMapOf<String, String>()
        
        context.assets.open(DATA_ZIP_FILE).use { zipStream ->
            ZipInputStream(zipStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        SETLISTS_FILE, SONGS_FILE, VENUES_FILE -> {
                            val content = zip.readBytes().toString(Charsets.UTF_8)
                            dataFiles[entry.name] = content
                            Log.d(TAG, "Extracted ${entry.name}: ${content.length / 1024}KB")
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }
        
        val totalSize = dataFiles.values.sumOf { it.length }
        Log.i(TAG, "✅ Extracted ${dataFiles.size} files (${totalSize / 1024}KB total) from ZIP")
        return dataFiles
    }
    
    /**
     * Parse setlist data JSON files and save to database.
     */
    private suspend fun parseAndSaveSetlistData(dataFiles: Map<String, String>) {
        Log.i(TAG, "Parsing setlist data files...")
        
        try {
            // Parse and save venues first (referenced by setlists)
            dataFiles[VENUES_FILE]?.let { venuesJson ->
                Log.i(TAG, "Processing venues data...")
                parseAndSaveVenues(venuesJson)
            }
            
            // Parse and save songs (referenced by setlists)
            dataFiles[SONGS_FILE]?.let { songsJson ->
                Log.i(TAG, "Processing songs data...")
                parseAndSaveSongs(songsJson)
            }
            
            // Parse and save setlists
            dataFiles[SETLISTS_FILE]?.let { setlistsJson ->
                Log.i(TAG, "Processing setlists data...")
                parseAndSaveSetlists(setlistsJson)
            }
            
            Log.i(TAG, "Successfully loaded setlist data from assets")
            
        } finally {
            // Clean up in-memory JSON data to free memory
            if (dataFiles.isNotEmpty()) {
                val totalSize = dataFiles.values.sumOf { it.length }
                Log.i(TAG, "🧹 Cleaning up JSON data from memory (${totalSize / 1024}KB)")
                // Note: dataFiles is passed as parameter, so original map will be eligible for GC
                // after this method returns. Explicitly clearing here for immediate cleanup.
                (dataFiles as? MutableMap)?.clear()
                
                // Force comprehensive memory cleanup
                forceMemoryCleanup()
            }
        }
    }
    
    /**
     * Parse venues JSON and save to database.
     */
    private suspend fun parseAndSaveVenues(venuesJson: String) {
        val venues = mutableListOf<VenueEntity>()
        val venuesData = JSONObject(venuesJson)
        
        // Get the actual venues data from the wrapper structure
        val actualVenuesData = if (venuesData.has("venues")) {
            venuesData.getJSONObject("venues")
        } else {
            venuesData // fallback to direct structure
        }
        
        var count = 0
        val keys = actualVenuesData.keys()
        while (keys.hasNext()) {
            val venueId = keys.next()
            try {
                val venueObj = actualVenuesData.getJSONObject(venueId)
                val venue = parseVenueFromJson(venueId, venueObj)
                venues.add(VenueEntity.fromVenue(venue))
                count++
                if (count % 50 == 0) {
                    Log.i(TAG, "Processed $count venues...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse venue $venueId: ${e.message}")
            }
        }
        
        venueDao.replaceAllVenues(venues)
        Log.i(TAG, "Successfully loaded ${venues.size} venues")
    }
    
    private fun parseVenueFromJson(venueId: String, venueObj: JSONObject): Venue {
        val aliases = mutableListOf<String>()
        val aliasesArray = venueObj.optJSONArray("aliases")
        if (aliasesArray != null) {
            for (i in 0 until aliasesArray.length()) {
                aliases.add(aliasesArray.getString(i))
            }
        }
        
        return Venue(
            venueId = venueId,
            name = venueObj.getString("name"),
            aliases = aliases,
            normalizedName = venueObj.optString("normalized_name").takeIf { it.isNotEmpty() },
            city = venueObj.optString("city").takeIf { it.isNotEmpty() },
            state = venueObj.optString("state").takeIf { it.isNotEmpty() },
            country = venueObj.optString("country").takeIf { it.isNotEmpty() },
            fullLocation = venueObj.optString("full_location").takeIf { it.isNotEmpty() },
            venueType = venueObj.optString("venue_type").takeIf { it.isNotEmpty() },
            capacity = if (venueObj.has("capacity")) venueObj.optInt("capacity") else null,
            firstShow = venueObj.optString("first_show").takeIf { it.isNotEmpty() },
            lastShow = venueObj.optString("last_show").takeIf { it.isNotEmpty() },
            totalShows = if (venueObj.has("total_shows")) venueObj.optInt("total_shows") else null,
            notes = venueObj.optString("notes").takeIf { it.isNotEmpty() }
        )
    }
    
    /**
     * Parse songs JSON and save to database.
     */
    private suspend fun parseAndSaveSongs(songsJson: String) {
        val songs = mutableListOf<SongEntity>()
        val songsData = JSONObject(songsJson)
        
        // Get the actual songs data from the wrapper structure
        val actualSongsData = if (songsData.has("songs")) {
            songsData.getJSONObject("songs")
        } else {
            songsData // fallback to direct structure
        }
        
        var count = 0
        val keys = actualSongsData.keys()
        while (keys.hasNext()) {
            val songId = keys.next()
            try {
                val songObj = actualSongsData.getJSONObject(songId)
                val song = parseSongFromJson(songId, songObj)
                songs.add(SongEntity.fromSong(song))
                count++
                if (count % 100 == 0) {
                    Log.i(TAG, "Processed $count songs...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse song $songId: ${e.message}")
            }
        }
        
        songDao.replaceAllSongs(songs)
        Log.i(TAG, "Successfully loaded ${songs.size} songs")
    }
    
    private fun parseSongFromJson(songId: String, songObj: JSONObject): Song {
        val aliases = mutableListOf<String>()
        val aliasesArray = songObj.optJSONArray("aliases")
        if (aliasesArray != null) {
            for (i in 0 until aliasesArray.length()) {
                aliases.add(aliasesArray.getString(i))
            }
        }
        
        val variants = mutableListOf<String>()
        val variantsArray = songObj.optJSONArray("variants")
        if (variantsArray != null) {
            for (i in 0 until variantsArray.length()) {
                variants.add(variantsArray.getString(i))
            }
        }
        
        return Song(
            songId = songId,
            name = songObj.getString("name"),
            aliases = aliases,
            variants = variants,
            canonicalName = songObj.optString("canonical_name").takeIf { it.isNotEmpty() },
            category = songObj.optString("category").takeIf { it.isNotEmpty() },
            originalArtist = songObj.optString("original_artist").takeIf { it.isNotEmpty() },
            firstPerformed = songObj.optString("first_performed").takeIf { it.isNotEmpty() },
            lastPerformed = songObj.optString("last_performed").takeIf { it.isNotEmpty() },
            timesPlayed = if (songObj.has("times_played")) songObj.optInt("times_played") else null,
            notes = songObj.optString("notes").takeIf { it.isNotEmpty() }
        )
    }
    
    /**
     * Parse setlists JSON and save to database.
     */
    private suspend fun parseAndSaveSetlists(setlistsJson: String) {
        val setlists = mutableListOf<SetlistEntity>()
        val setlistsData = JSONObject(setlistsJson)
        
        // Get the actual setlists data from the wrapper structure
        val actualSetlistsData = if (setlistsData.has("setlists")) {
            setlistsData.getJSONObject("setlists")
        } else {
            setlistsData // fallback to direct structure
        }
        
        var count = 0
        val keys = actualSetlistsData.keys()
        while (keys.hasNext()) {
            val showId = keys.next()
            try {
                val setlistObj = actualSetlistsData.getJSONObject(showId)
                val setlist = parseSetlistFromJson(showId, setlistObj)
                setlists.add(SetlistEntity.fromSetlist(setlist))
                count++
                if (count % 100 == 0) {
                    Log.i(TAG, "Processed $count setlists...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse setlist $showId: ${e.message}")
            }
        }
        
        setlistDao.replaceAllSetlists(setlists)
        Log.i(TAG, "Successfully loaded ${setlists.size} setlists")
        
        // Post-process to resolve song IDs to actual song names
        Log.i(TAG, "Resolving song IDs to song names...")
        resolveSongIdsInSetlists()
        
        // Debug: Log some sample dates
        if (setlists.isNotEmpty()) {
            Log.d(TAG, "Sample setlist dates: ${setlists.take(5).map { "${it.showId}: ${it.date}" }}")
        }
    }
    
    /**
     * Post-process setlists to resolve song IDs to actual song names.
     */
    private suspend fun resolveSongIdsInSetlists() {
        try {
            Log.i(TAG, "Starting song ID resolution process...")
            
            // Get all setlists from database
            val setlistEntities = setlistDao.getSetlistsWithSongs()
            Log.i(TAG, "Found ${setlistEntities.size} setlists with songs to process")
            
            // Get all songs from database for lookup
            val allSongs = songDao.getAllSongs()
            val songIdToNameMap = allSongs.associate { it.songId to it.name }
            Log.i(TAG, "Created lookup map with ${songIdToNameMap.size} songs")
            
            var resolvedCount = 0
            var unresolvedCount = 0
            val updatedSetlists = mutableListOf<SetlistEntity>()
            
            setlistEntities.forEach { setlistEntity ->
                val setlist = setlistEntity.toSetlist()
                
                // Resolve song IDs to names
                val updatedSongs = setlist.songs.map { song ->
                    val resolvedName = if (song.songId != null && songIdToNameMap.containsKey(song.songId)) {
                        val actualName = songIdToNameMap[song.songId]!!
                        resolvedCount++
                        actualName
                    } else {
                        // Keep the original name if no ID mapping found
                        unresolvedCount++
                        song.songName
                    }
                    
                    song.copy(songName = resolvedName)
                }
                
                // Update the setlist with resolved song names
                val updatedSetlist = setlist.copy(songs = updatedSongs)
                updatedSetlists.add(SetlistEntity.fromSetlist(updatedSetlist))
            }
            
            // Batch update all setlists
            if (updatedSetlists.isNotEmpty()) {
                setlistDao.insertSetlists(updatedSetlists)
                Log.i(TAG, "Updated ${updatedSetlists.size} setlists with resolved song names")
                Log.i(TAG, "Resolved $resolvedCount song IDs, $unresolvedCount unresolved")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve song IDs in setlists: ${e.message}", e)
        }
    }
    
    private fun parseSetlistFromJson(showId: String, setlistObj: JSONObject): Setlist {
        // Parse sets map
        val sets = mutableMapOf<String, List<String>>()
        val setsObj = setlistObj.optJSONObject("sets")
        if (setsObj != null) {
            val setKeys = setsObj.keys()
            while (setKeys.hasNext()) {
                val setName = setKeys.next()
                val setArray = setsObj.getJSONArray(setName)
                val songList = mutableListOf<String>()
                for (i in 0 until setArray.length()) {
                    songList.add(setArray.getString(i))
                }
                sets[setName] = songList
            }
        }
        
        // Parse songs from sets structure (convert sets to songs list)
        val songs = mutableListOf<com.deadly.core.model.SetlistSong>()
        var position = 1
        
        for ((setName, songIds) in sets) {
            for (songId in songIds) {
                songs.add(
                    com.deadly.core.model.SetlistSong(
                        songName = songId, // Will be resolved to actual name after songs are loaded
                        songId = songId,
                        setName = setName,
                        position = position++,
                        isSegue = false,
                        segueType = null
                    )
                )
            }
        }
        
        return Setlist(
            showId = showId,
            date = setlistObj.getString("date"),
            venueId = setlistObj.optString("venueId").takeIf { it.isNotEmpty() },
            venueLine = setlistObj.optString("venue_line").takeIf { it.isNotEmpty() },
            source = setlistObj.getString("source"),
            sets = sets,
            songs = songs,
            rawContent = setlistObj.optString("raw_content").takeIf { it.isNotEmpty() },
            cmuRawContent = setlistObj.optString("cmu_raw_content").takeIf { it.isNotEmpty() },
            cmuVenueLine = setlistObj.optString("cmu_venue_line").takeIf { it.isNotEmpty() }
        )
    }
    
    // Setlist queries
    
    suspend fun getSetlist(showId: String): Setlist? {
        return try {
            setlistDao.getSetlist(showId)?.toSetlist()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setlist for $showId: ${e.message}")
            null
        }
    }
    
    suspend fun getSetlistsByDate(date: String): List<Setlist> {
        return try {
            setlistDao.getSetlistsByDate(date).map { it.toSetlist() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setlists for date $date: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getSetlistsByVenue(venueId: String): List<Setlist> {
        return try {
            setlistDao.getSetlistsByVenue(venueId).map { it.toSetlist() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setlists for venue $venueId: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getSetlistsWithSongs(): List<Setlist> {
        return try {
            setlistDao.getSetlistsWithSongs().map { it.toSetlist() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setlists with songs: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getBestQualitySetlists(): List<Setlist> {
        return try {
            val setlists = setlistDao.getBestQualitySetlists().map { it.toSetlist() }
            enrichSetlistsWithVenueInfo(setlists)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get best quality setlists: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun searchSetlists(query: String): List<Setlist> {
        return try {
            Log.d(TAG, "Searching setlists for query: '$query'")
            val results = setlistDao.searchSetlists(query).map { it.toSetlist() }
            Log.d(TAG, "Search returned ${results.size} results")
            enrichSetlistsWithVenueInfo(results)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search setlists for '$query': ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Find setlists containing a specific song.
     */
    suspend fun searchSetlistsBySong(songName: String): List<Setlist> {
        return try {
            Log.d(TAG, "Searching setlists containing song: '$songName'")
            
            // Search for songs matching the query
            val matchingSongEntities = songDao.searchSongs(songName)
            if (matchingSongEntities.isEmpty()) {
                Log.d(TAG, "No songs found matching '$songName'")
                return emptyList()
            }
            
            // Convert to Song models to access aliases
            val matchingSongs = matchingSongEntities.map { it.toSong() }
            
            // Get all setlists containing any of these songs
            val allSetlists = setlistDao.getSetlistsWithSongs().map { it.toSetlist() }
            val matchingSetlists = mutableListOf<Setlist>()
            
            for (setlist in allSetlists) {
                val songNames = setlist.songs.map { it.songName.lowercase() }
                val hasMatchingSong = matchingSongs.any { song ->
                    songNames.any { songName ->
                        songName.contains(song.name.lowercase()) ||
                        song.aliases.any { alias -> songName.contains(alias.lowercase()) }
                    }
                }
                
                if (hasMatchingSong) {
                    matchingSetlists.add(setlist)
                }
            }
            
            Log.d(TAG, "Found ${matchingSetlists.size} setlists containing '$songName'")
            enrichSetlistsWithVenueInfo(matchingSetlists.sortedByDescending { it.date })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search setlists by song '$songName': ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Find setlists with specific song combinations/segues.
     */
    suspend fun searchSetlistsWithSongCombination(songs: List<String>): List<Setlist> {
        return try {
            Log.d(TAG, "Searching for setlists with song combination: $songs")
            
            if (songs.isEmpty()) return emptyList()
            
            val allSetlists = setlistDao.getSetlistsWithSongs().map { it.toSetlist() }
            val matchingSetlists = mutableListOf<Setlist>()
            
            for (setlist in allSetlists) {
                val songNames = setlist.songs.map { it.songName.lowercase() }
                val hasAllSongs = songs.all { targetSong ->
                    songNames.any { it.contains(targetSong.lowercase()) }
                }
                
                if (hasAllSongs) {
                    matchingSetlists.add(setlist)
                }
            }
            
            Log.d(TAG, "Found ${matchingSetlists.size} setlists with all songs: $songs")
            enrichSetlistsWithVenueInfo(matchingSetlists.sortedByDescending { it.date })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search setlists with song combination: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Find setlists where a song appears in a specific set position.
     */
    suspend fun searchSetlistsBySongPosition(songName: String, position: SongPosition): List<Setlist> {
        return try {
            Log.d(TAG, "Searching for '$songName' in position: $position")
            
            val allSetlists = setlistDao.getSetlistsWithSongs().map { it.toSetlist() }
            val matchingSetlists = mutableListOf<Setlist>()
            
            for (setlist in allSetlists) {
                val matches = when (position) {
                    SongPosition.SHOW_OPENER -> {
                        setlist.songs.firstOrNull()?.songName?.contains(songName, ignoreCase = true) ?: false
                    }
                    SongPosition.SHOW_CLOSER -> {
                        setlist.songs.lastOrNull()?.songName?.contains(songName, ignoreCase = true) ?: false
                    }
                    SongPosition.SET_OPENER -> {
                        setlist.songsBySet.values.any { setSongs ->
                            setSongs.firstOrNull()?.songName?.contains(songName, ignoreCase = true) ?: false
                        }
                    }
                    SongPosition.SET_CLOSER -> {
                        setlist.songsBySet.values.any { setSongs ->
                            setSongs.lastOrNull()?.songName?.contains(songName, ignoreCase = true) ?: false
                        }
                    }
                    SongPosition.ENCORE -> {
                        setlist.songsBySet["encore"]?.any { 
                            it.songName.contains(songName, ignoreCase = true) 
                        } ?: false
                    }
                }
                
                if (matches) {
                    matchingSetlists.add(setlist)
                }
            }
            
            Log.d(TAG, "Found ${matchingSetlists.size} setlists with '$songName' as $position")
            enrichSetlistsWithVenueInfo(matchingSetlists.sortedByDescending { it.date })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search by song position: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get song statistics and performance history.
     */
    suspend fun getSongStatistics(songName: String): SongStatistics? {
        return try {
            Log.d(TAG, "Getting statistics for song: '$songName'")
            
            // Find the song first
            val songEntities = songDao.searchSongs(songName)
            val songs = songEntities.map { it.toSong() }
            val targetSong = songs.firstOrNull { 
                it.name.equals(songName, ignoreCase = true) ||
                it.aliases.any { alias -> alias.equals(songName, ignoreCase = true) }
            } ?: songs.firstOrNull()
            
            if (targetSong == null) {
                Log.d(TAG, "Song '$songName' not found")
                return null
            }
            
            // Get all performances
            val performances = searchSetlistsBySong(targetSong.name)
            
            if (performances.isEmpty()) {
                return SongStatistics(
                    songName = targetSong.name,
                    timesPlayed = 0,
                    firstPlayed = null,
                    lastPlayed = null,
                    averageSetPosition = 0f,
                    mostCommonSet = "Unknown",
                    venues = emptyList(),
                    years = emptyMap()
                )
            }
            
            val venues = performances.mapNotNull { it.venueId }.distinct()
            val years = performances.groupBy { it.date.substring(0, 4) }
                .mapValues { it.value.size }
            
            // Calculate average set position
            var totalPosition = 0
            var positionCount = 0
            
            performances.forEach { setlist ->
                setlist.songs.forEachIndexed { index, song ->
                    if (song.songName.contains(targetSong.name, ignoreCase = true)) {
                        totalPosition += index + 1
                        positionCount++
                    }
                }
            }
            
            val avgPosition = if (positionCount > 0) totalPosition.toFloat() / positionCount else 0f
            
            // Find most common set
            val setOccurrences = mutableMapOf<String, Int>()
            performances.forEach { setlist ->
                setlist.songs.forEach { song ->
                    if (song.songName.contains(targetSong.name, ignoreCase = true)) {
                        val setName = song.setName ?: "unknown"
                        setOccurrences[setName] = setOccurrences.getOrDefault(setName, 0) + 1
                    }
                }
            }
            
            val mostCommonSet = setOccurrences.maxByOrNull { it.value }?.key ?: "Unknown"
            
            SongStatistics(
                songName = targetSong.name,
                timesPlayed = performances.size,
                firstPlayed = performances.minByOrNull { it.date }?.date,
                lastPlayed = performances.maxByOrNull { it.date }?.date,
                averageSetPosition = avgPosition,
                mostCommonSet = mostCommonSet,
                venues = venues,
                years = years
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get song statistics for '$songName': ${e.message}")
            null
        }
    }
    
    /**
     * Find common song segues and transitions.
     */
    suspend fun findSongSegues(songName: String): List<SongSegue> {
        return try {
            Log.d(TAG, "Finding segues for song: '$songName'")
            
            val performances = searchSetlistsBySong(songName)
            val segues = mutableListOf<SongSegue>()
            
            performances.forEach { setlist ->
                val songs = setlist.songs
                songs.forEachIndexed { index, song ->
                    if (song.songName.contains(songName, ignoreCase = true)) {
                        // Check for song that follows
                        if (index < songs.size - 1) {
                            val nextSong = songs[index + 1]
                            segues.add(SongSegue(
                                fromSong = song.songName,
                                toSong = nextSong.songName,
                                date = setlist.date,
                                venue = setlist.displayVenue,
                                isSegue = song.isSegue || nextSong.isSegue,
                                segueType = song.segueType
                            ))
                        }
                        
                        // Check for song that precedes
                        if (index > 0) {
                            val prevSong = songs[index - 1]
                            segues.add(SongSegue(
                                fromSong = prevSong.songName,
                                toSong = song.songName,
                                date = setlist.date,
                                venue = setlist.displayVenue,
                                isSegue = prevSong.isSegue || song.isSegue,
                                segueType = prevSong.segueType
                            ))
                        }
                    }
                }
            }
            
            // Group and count segues
            val segueGroups = segues.groupBy { "${it.fromSong} -> ${it.toSong}" }
            val result = segueGroups.map { (transition, occurrences) ->
                occurrences.first().copy(
                    occurrenceCount = occurrences.size,
                    lastOccurrence = occurrences.maxByOrNull { it.date }?.date
                )
            }.sortedByDescending { it.occurrenceCount }
            
            Log.d(TAG, "Found ${result.size} unique segues for '$songName'")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find segues for '$songName': ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Advanced setlist filtering with multiple criteria.
     */
    suspend fun searchSetlistsAdvanced(criteria: SearchCriteria): List<Setlist> {
        return try {
            Log.d(TAG, "Advanced search with criteria: $criteria")
            
            var results = if (criteria.songs.isNotEmpty()) {
                searchSetlistsBySong(criteria.songs.first())
            } else {
                setlistDao.getSetlistsWithSongs().map { it.toSetlist() }
            }
            
            // Apply date range filter
            if (criteria.startDate != null) {
                results = results.filter { it.date >= criteria.startDate }
            }
            if (criteria.endDate != null) {
                results = results.filter { it.date <= criteria.endDate }
            }
            
            // Apply venue filter
            if (criteria.venue != null) {
                results = results.filter { 
                    it.displayVenue.contains(criteria.venue, ignoreCase = true) 
                }
            }
            
            // Apply minimum songs filter
            if (criteria.minSongs > 0) {
                results = results.filter { it.totalSongs >= criteria.minSongs }
            }
            
            // Apply source filter
            if (criteria.source != null) {
                results = results.filter { it.source == criteria.source }
            }
            
            // Apply additional song filters
            if (criteria.songs.size > 1) {
                val remainingSongs = criteria.songs.drop(1)
                results = results.filter { setlist ->
                    remainingSongs.all { targetSong ->
                        setlist.songs.any { 
                            it.songName.contains(targetSong, ignoreCase = true) 
                        }
                    }
                }
            }
            
            val sortedResults = when (criteria.sortBy) {
                SortOrder.DATE_DESC -> results.sortedByDescending { it.date }
                SortOrder.DATE_ASC -> results.sortedBy { it.date }
                SortOrder.VENUE -> results.sortedBy { it.displayVenue }
                SortOrder.SONG_COUNT -> results.sortedByDescending { it.totalSongs }
            }
            
            Log.d(TAG, "Advanced search returned ${sortedResults.size} results")
            enrichSetlistsWithVenueInfo(sortedResults)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed advanced search: ${e.message}")
            emptyList()
        }
    }
    
    // Song queries
    
    suspend fun getSong(songId: String): Song? {
        return try {
            songDao.getSong(songId)?.toSong()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get song $songId: ${e.message}")
            null
        }
    }
    
    suspend fun getSongs(songIds: List<String>): Map<String, Song> {
        return try {
            songDao.getSongs(songIds).associate { it.songId to it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get songs: ${e.message}")
            emptyMap()
        }
    }
    
    suspend fun searchSongs(query: String): List<Song> {
        return try {
            songDao.searchSongs(query).map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search songs for '$query': ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getMostPlayedSongs(limit: Int = 50): List<Song> {
        return try {
            songDao.getMostPlayedSongs(limit).map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get most played songs: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getOriginalSongs(): List<Song> {
        return try {
            songDao.getOriginalSongs().map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get original songs: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getCoverSongs(): List<Song> {
        return try {
            songDao.getCoverSongs().map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cover songs: ${e.message}")
            emptyList()
        }
    }
    
    // Venue queries
    
    suspend fun getVenue(venueId: String): Venue? {
        return try {
            venueDao.getVenue(venueId)?.toVenue()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get venue $venueId: ${e.message}")
            null
        }
    }
    
    suspend fun getVenues(venueIds: List<String>): Map<String, Venue> {
        return try {
            venueDao.getVenues(venueIds).associate { it.venueId to it.toVenue() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get venues: ${e.message}")
            emptyMap()
        }
    }
    
    suspend fun searchVenues(query: String): List<Venue> {
        return try {
            venueDao.searchVenues(query).map { it.toVenue() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search venues for '$query': ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getMostPopularVenues(limit: Int = 50): List<Venue> {
        return try {
            venueDao.getMostPopularVenues(limit).map { it.toVenue() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get most popular venues: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getUSVenues(): List<Venue> {
        return try {
            venueDao.getUSVenues().map { it.toVenue() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get US venues: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getInternationalVenues(): List<Venue> {
        return try {
            venueDao.getInternationalVenues().map { it.toVenue() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get international venues: ${e.message}")
            emptyList()
        }
    }
    
    // Flow-based reactive queries
    
    fun getSetlistsWithSongsFlow(limit: Int = 100): Flow<List<Setlist>> {
        return setlistDao.getSetlistsWithSongsFlow(limit)
            .map { entities -> entities.map { it.toSetlist() } }
    }
    
    fun getSetlistsByDateFlow(date: String): Flow<List<Setlist>> {
        return setlistDao.getSetlistsByDateFlow(date)
            .map { entities -> entities.map { it.toSetlist() } }
    }
    
    fun getMostPlayedSongsFlow(limit: Int = 50): Flow<List<Song>> {
        return songDao.getMostPlayedSongsFlow(limit)
            .map { entities -> entities.map { it.toSong() } }
    }
    
    fun getMostPopularVenuesFlow(limit: Int = 50): Flow<List<Venue>> {
        return venueDao.getMostPopularVenuesFlow(limit)
            .map { entities -> entities.map { it.toVenue() } }
    }
    
    // Statistics
    
    suspend fun getSetlistStatistics(): Map<String, Any> {
        return try {
            val setlistCount = setlistDao.getSetlistCount()
            val setlistsWithSongs = setlistDao.getSetlistsWithSongsCount()
            val songCount = songDao.getSongCount()
            val venueCount = venueDao.getVenueCount()
            val avgSongCount = setlistDao.getAverageSongCount() ?: 0f
            val sourceStats = setlistDao.getSetlistStatsBySource()
            
            // Debug: Check if we have any 1977 setlists
            val sample1977 = setlistDao.getSetlistsByYear("1977")
            Log.d(TAG, "Found ${sample1977.size} setlists for year 1977")
            if (sample1977.isNotEmpty()) {
                Log.d(TAG, "Sample 1977 setlist: ${sample1977.first().showId}")
            }
            
            mapOf(
                "setlist_count" to setlistCount,
                "setlists_with_songs" to setlistsWithSongs,
                "song_count" to songCount,
                "venue_count" to venueCount,
                "average_song_count" to avgSongCount,
                "source_statistics" to sourceStats,
                "sample_1977_count" to sample1977.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get setlist statistics: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Enrich setlists with venue information by looking up venue names.
     */
    private suspend fun enrichSetlistsWithVenueInfo(setlists: List<Setlist>): List<Setlist> {
        if (setlists.isEmpty()) return setlists
        
        return try {
            // Get all unique venue IDs
            val venueIds = setlists.mapNotNull { it.venueId }.distinct()
            Log.d(TAG, "Enriching ${setlists.size} setlists with ${venueIds.size} unique venues")
            Log.d(TAG, "Sample venue IDs: ${venueIds.take(3)}")
            
            if (venueIds.isEmpty()) {
                Log.w(TAG, "No venue IDs found in setlists")
                return setlists
            }
            
            // Look up all venues in one query
            val venues = getVenues(venueIds)
            Log.d(TAG, "Found ${venues.size} venues from database")
            venues.values.take(2).forEach { venue ->
                Log.d(TAG, "Sample venue: ${venue.venueId} -> ${venue.fullDescription}")
            }
            
            // Enrich setlists with venue information
            val enrichedSetlists = setlists.map { setlist ->
                if (setlist.venueId != null && venues.containsKey(setlist.venueId)) {
                    val venue = venues[setlist.venueId]!!
                    Log.d(TAG, "Enriching ${setlist.showId}: ${setlist.venueId} -> ${venue.fullDescription}")
                    setlist.copy(
                        venueLine = venue.fullDescription
                    )
                } else {
                    Log.d(TAG, "No venue found for ${setlist.showId}: venueId=${setlist.venueId}")
                    setlist
                }
            }
            
            Log.d(TAG, "Enrichment complete. Sample enriched venue: ${enrichedSetlists.firstOrNull()?.displayVenue}")
            enrichedSetlists
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enrich setlists with venue info: ${e.message}", e)
            setlists // Return original setlists if enrichment fails
        }
    }
    
    /**
     * Clean up old setlist data.
     */
    suspend fun cleanupOldSetlistData() {
        try {
            val cutoffTime = System.currentTimeMillis() - DATA_CACHE_EXPIRY
            setlistDao.deleteOldSetlists(cutoffTime)
            songDao.deleteOldSongs(cutoffTime)
            venueDao.deleteOldVenues(cutoffTime)
            Log.i(TAG, "Cleaned up old setlist data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old setlist data: ${e.message}")
        }
    }
    
    /**
     * Force memory cleanup and log memory statistics.
     * Useful after processing large JSON datasets.
     */
    fun forceMemoryCleanup() {
        val runtime = Runtime.getRuntime()
        val usedMemoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        Log.i(TAG, "🧹 Memory before cleanup: ${usedMemoryBefore}MB")
        System.gc()
        
        // Wait a moment for GC to complete
        Thread.sleep(100)
        
        val usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val freed = usedMemoryBefore - usedMemoryAfter
        
        Log.i(TAG, "✅ Memory after cleanup: ${usedMemoryAfter}MB (freed: ${freed}MB)")
    }
}

/**
 * Enum for song position types in setlists.
 */
enum class SongPosition {
    SHOW_OPENER,    // First song of the entire show
    SHOW_CLOSER,    // Last song of the entire show  
    SET_OPENER,     // First song of any set
    SET_CLOSER,     // Last song of any set
    ENCORE          // Song in encore
}

/**
 * Enum for sorting search results.
 */
enum class SortOrder {
    DATE_DESC,      // Most recent first
    DATE_ASC,       // Oldest first
    VENUE,          // Alphabetical by venue
    SONG_COUNT      // Most songs first
}

/**
 * Data class for advanced search criteria.
 */
data class SearchCriteria(
    val songs: List<String> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val venue: String? = null,
    val minSongs: Int = 0,
    val source: String? = null,
    val sortBy: SortOrder = SortOrder.DATE_DESC
)

/**
 * Data class for song performance statistics.
 */
data class SongStatistics(
    val songName: String,
    val timesPlayed: Int,
    val firstPlayed: String?,
    val lastPlayed: String?,
    val averageSetPosition: Float,
    val mostCommonSet: String,
    val venues: List<String>,
    val years: Map<String, Int>
) {
    /**
     * Get performance frequency description.
     */
    val performanceFrequency: String
        get() = when {
            timesPlayed == 0 -> "Never played"
            timesPlayed == 1 -> "Played once"
            timesPlayed < 10 -> "Rarely played ($timesPlayed times)"
            timesPlayed < 50 -> "Occasionally played ($timesPlayed times)"
            timesPlayed < 100 -> "Regularly played ($timesPlayed times)"
            timesPlayed < 200 -> "Frequently played ($timesPlayed times)"
            else -> "Very frequently played ($timesPlayed times)"
        }
    
    /**
     * Get years active as a readable string.
     */
    val yearsActive: String
        get() = if (firstPlayed != null && lastPlayed != null) {
            val firstYear = firstPlayed.substring(0, 4)
            val lastYear = lastPlayed.substring(0, 4)
            if (firstYear == lastYear) firstYear else "$firstYear-$lastYear"
        } else "Unknown"
    
    /**
     * Get most active year.
     */
    val peakYear: String?
        get() = years.maxByOrNull { it.value }?.key
}

/**
 * Data class for song segue information.
 */
data class SongSegue(
    val fromSong: String,
    val toSong: String,
    val date: String,
    val venue: String,
    val isSegue: Boolean = false,
    val segueType: String? = null,
    val occurrenceCount: Int = 1,
    val lastOccurrence: String? = null
) {
    /**
     * Get display string for the segue.
     */
    val displayTransition: String
        get() = if (isSegue) {
            "$fromSong ${segueType ?: ">"} $toSong"
        } else {
            "$fromSong → $toSong"
        }
    
    /**
     * Get frequency description.
     */
    val frequencyDescription: String
        get() = when {
            occurrenceCount == 1 -> "Occurred once"
            occurrenceCount < 5 -> "Rare transition ($occurrenceCount times)"
            occurrenceCount < 10 -> "Occasional transition ($occurrenceCount times)"
            occurrenceCount < 20 -> "Common transition ($occurrenceCount times)"
            else -> "Frequent transition ($occurrenceCount times)"
        }
}