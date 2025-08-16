package com.deadarchive.v2.core.database.service

import android.util.Log
import com.deadarchive.v2.core.database.dao.SongSearchV2Dao
import com.deadarchive.v2.core.database.dao.VenueSearchV2Dao
import com.deadarchive.v2.core.database.dao.ShowSearchV2Dao
import com.deadarchive.v2.core.database.dao.MemberSearchV2Dao
import com.deadarchive.v2.core.database.entities.SongSearchV2Entity
import com.deadarchive.v2.core.database.entities.VenueSearchV2Entity
import com.deadarchive.v2.core.database.entities.ShowSearchV2Entity
import com.deadarchive.v2.core.database.entities.MemberSearchV2Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double
import kotlinx.serialization.json.boolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchTableProcessorV2 @Inject constructor(
    private val songSearchDao: SongSearchV2Dao,
    private val venueSearchDao: VenueSearchV2Dao,
    private val showSearchDao: ShowSearchV2Dao,
    private val memberSearchDao: MemberSearchV2Dao
) {
    
    companion object {
        private const val TAG = "SearchTableProcessorV2"
        
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    
    suspend fun processSongsJson(songsJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing songs JSON data...")
            val songsData = json.parseToJsonElement(songsJsonContent).jsonObject
            val songEntities = mutableListOf<SongSearchV2Entity>()
            
            songsData.forEach { (songKey, songData) ->
                val songObj = songData.jsonObject
                val songName = songObj["name"]?.jsonPrimitive?.content ?: ""
                val shows = songObj["shows"]?.jsonArray ?: return@forEach
                
                shows.forEach { showElement ->
                    val show = showElement.jsonObject
                    val showId = show["show_id"]?.jsonPrimitive?.content ?: return@forEach
                    
                    val entity = SongSearchV2Entity(
                        id = "${songKey}_${showId}",
                        songKey = songKey,
                        songName = songName,
                        showId = showId,
                        date = show["date"]?.jsonPrimitive?.content ?: "",
                        venue = show["venue"]?.jsonPrimitive?.content ?: "",
                        location = show["location"]?.jsonPrimitive?.content ?: "",
                        setName = show["set"]?.jsonPrimitive?.content,
                        position = show["position"]?.jsonPrimitive?.int,
                        segueIntoNext = show["segue_into_next"]?.jsonPrimitive?.boolean ?: false,
                        rating = show["rating"]?.jsonPrimitive?.double ?: 0.0,
                        rawRating = show["raw_rating"]?.jsonPrimitive?.double ?: 0.0
                    )
                    songEntities.add(entity)
                }
            }
            
            songSearchDao.deleteAll()
            songSearchDao.insertAll(songEntities)
            Log.d(TAG, "Successfully processed ${songEntities.size} song search entries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing songs JSON", e)
            false
        }
    }
    
    suspend fun processVenuesJson(venuesJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing venues JSON data...")
            val venuesData = json.parseToJsonElement(venuesJsonContent).jsonObject
            val venueEntities = mutableListOf<VenueSearchV2Entity>()
            
            venuesData.forEach { (venueKey, venueData) ->
                val venueObj = venueData.jsonObject
                val venueName = venueObj["name"]?.jsonPrimitive?.content ?: ""
                val location = venueObj["location"]?.jsonPrimitive?.content ?: ""
                val city = venueObj["city"]?.jsonPrimitive?.content ?: ""
                val state = venueObj["state"]?.jsonPrimitive?.content ?: ""
                val country = venueObj["country"]?.jsonPrimitive?.content ?: ""
                val totalShows = venueObj["total_shows"]?.jsonPrimitive?.int ?: 0
                val firstShow = venueObj["first_show"]?.jsonPrimitive?.content ?: ""
                val lastShow = venueObj["last_show"]?.jsonPrimitive?.content ?: ""
                val shows = venueObj["shows"]?.jsonArray ?: return@forEach
                
                shows.forEach { showElement ->
                    val show = showElement.jsonObject
                    val showId = show["show_id"]?.jsonPrimitive?.content ?: return@forEach
                    
                    val entity = VenueSearchV2Entity(
                        id = "${venueKey}_${showId}",
                        venueKey = venueKey,
                        venueName = venueName,
                        location = location,
                        city = city,
                        state = state,
                        country = country,
                        showId = showId,
                        date = show["date"]?.jsonPrimitive?.content ?: "",
                        rating = show["rating"]?.jsonPrimitive?.double ?: 0.0,
                        rawRating = show["raw_rating"]?.jsonPrimitive?.double ?: 0.0,
                        recordingCount = show["recording_count"]?.jsonPrimitive?.int ?: 0,
                        totalShows = totalShows,
                        firstShow = firstShow,
                        lastShow = lastShow
                    )
                    venueEntities.add(entity)
                }
            }
            
            venueSearchDao.deleteAll()
            venueSearchDao.insertAll(venueEntities)
            Log.d(TAG, "Successfully processed ${venueEntities.size} venue search entries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing venues JSON", e)
            false
        }
    }
    
    suspend fun processShowsIndexJson(showsJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing shows index JSON data...")
            val showsData = json.parseToJsonElement(showsJsonContent).jsonObject
            val showEntities = mutableListOf<ShowSearchV2Entity>()
            
            showsData.forEach { (showId, showData) ->
                val show = showData.jsonObject
                
                val collectionsArray = show["collections"]?.jsonArray
                val collections = collectionsArray?.map { it.jsonPrimitive.content } ?: emptyList()
                
                val entity = ShowSearchV2Entity(
                    showId = showId,
                    date = show["date"]?.jsonPrimitive?.content ?: "",
                    venue = show["venue"]?.jsonPrimitive?.content ?: "",
                    location = show["location"]?.jsonPrimitive?.content ?: "",
                    city = show["city"]?.jsonPrimitive?.content ?: "",
                    state = show["state"]?.jsonPrimitive?.content ?: "",
                    country = show["country"]?.jsonPrimitive?.content ?: "",
                    band = show["band"]?.jsonPrimitive?.content ?: "",
                    year = show["year"]?.jsonPrimitive?.int ?: 0,
                    month = show["month"]?.jsonPrimitive?.int ?: 0,
                    day = show["day"]?.jsonPrimitive?.int ?: 0,
                    rating = show["rating"]?.jsonPrimitive?.double ?: 0.0,
                    rawRating = show["raw_rating"]?.jsonPrimitive?.double ?: 0.0,
                    recordingCount = show["recording_count"]?.jsonPrimitive?.int ?: 0,
                    songCount = show["song_count"]?.jsonPrimitive?.int ?: 0,
                    hasSetlist = show["has_setlist"]?.jsonPrimitive?.boolean ?: false,
                    collections = collections,
                    searchText = show["search_text"]?.jsonPrimitive?.content ?: ""
                )
                showEntities.add(entity)
            }
            
            showSearchDao.deleteAll()
            showSearchDao.insertAll(showEntities)
            Log.d(TAG, "Successfully processed ${showEntities.size} show search entries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing shows index JSON", e)
            false
        }
    }
    
    suspend fun processMembersJson(membersJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing members JSON data...")
            val membersData = json.parseToJsonElement(membersJsonContent).jsonObject
            val memberEntities = mutableListOf<MemberSearchV2Entity>()
            
            membersData.forEach { (memberKey, memberData) ->
                val memberObj = memberData.jsonObject
                val memberName = memberObj["name"]?.jsonPrimitive?.content ?: ""
                val totalShows = memberObj["total_shows"]?.jsonPrimitive?.int ?: 0
                val firstShow = memberObj["first_show"]?.jsonPrimitive?.content ?: ""
                val lastShow = memberObj["last_show"]?.jsonPrimitive?.content ?: ""
                val primaryInstruments = memberObj["primary_instruments"]?.jsonArray
                    ?.joinToString(", ") { it.jsonPrimitive.content } ?: ""
                val shows = memberObj["shows"]?.jsonArray ?: return@forEach
                
                shows.forEach { showElement ->
                    val show = showElement.jsonObject
                    val showId = show["show_id"]?.jsonPrimitive?.content ?: return@forEach
                    
                    val entity = MemberSearchV2Entity(
                        id = "${memberKey}_${showId}",
                        memberKey = memberKey,
                        memberName = memberName,
                        showId = showId,
                        date = show["date"]?.jsonPrimitive?.content ?: "",
                        venue = show["venue"]?.jsonPrimitive?.content ?: "",
                        location = show["location"]?.jsonPrimitive?.content ?: "",
                        instruments = show["instruments"]?.jsonPrimitive?.content ?: "",
                        rating = show["rating"]?.jsonPrimitive?.double ?: 0.0,
                        totalShows = totalShows,
                        firstShow = firstShow,
                        lastShow = lastShow,
                        primaryInstruments = primaryInstruments
                    )
                    memberEntities.add(entity)
                }
            }
            
            memberSearchDao.deleteAll()
            memberSearchDao.insertAll(memberEntities)
            Log.d(TAG, "Successfully processed ${memberEntities.size} member search entries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing members JSON", e)
            false
        }
    }
    
    suspend fun processAllSearchTables(
        songsJson: String?,
        venuesJson: String?,
        showsIndexJson: String?,
        membersJson: String?
    ): Boolean {
        var allSuccessful = true
        
        songsJson?.let { 
            if (!processSongsJson(it)) allSuccessful = false 
        }
        
        venuesJson?.let { 
            if (!processVenuesJson(it)) allSuccessful = false 
        }
        
        showsIndexJson?.let { 
            if (!processShowsIndexJson(it)) allSuccessful = false 
        }
        
        membersJson?.let { 
            if (!processMembersJson(it)) allSuccessful = false 
        }
        
        return allSuccessful
    }
}