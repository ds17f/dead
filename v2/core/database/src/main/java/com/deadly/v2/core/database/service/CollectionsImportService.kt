package com.deadly.v2.core.database.service

import android.util.Log
import com.deadly.v2.core.model.V2Database
import com.deadly.v2.core.database.dao.CollectionsDao
import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.database.entities.DeadCollectionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for importing collections from collections.json into database
 * 
 * Converts JSON collections with show_selector patterns into DeadCollectionEntity
 * records with resolved show IDs for database storage.
 */
@Singleton
class CollectionsImportService @Inject constructor(
    @V2Database private val collectionsDao: CollectionsDao,
    @V2Database private val showDao: ShowDao
) {
    
    companion object {
        private const val TAG = "CollectionsImportService"
        private const val COLLECTIONS_FILE = "collections.json"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
    @Serializable
    data class CollectionImportData(
        val id: String,
        val name: String,
        val description: String,
        val tags: List<String> = emptyList(),
        @SerialName("show_selector")
        val showSelector: ShowSelectorData? = null
    )
    
    @Serializable
    data class ShowSelectorData(
        val dates: List<String> = emptyList(),
        val ranges: List<DateRangeData> = emptyList(),
        @SerialName("show_ids") 
        val showIds: List<String> = emptyList(),
        val venues: List<String> = emptyList(),
        val years: List<Int> = emptyList()
    )
    
    @Serializable 
    data class DateRangeData(
        val start: String,
        val end: String
    )
    
    /**
     * Import collections from JSON file in extracted data directory
     */
    suspend fun importCollectionsFromFile(extractedDataDirectory: File): CollectionsImportResult {
        return try {
            val collectionsFile = File(extractedDataDirectory, COLLECTIONS_FILE)
            
            if (!collectionsFile.exists()) {
                Log.w(TAG, "Collections file not found: ${collectionsFile.absolutePath}")
                return CollectionsImportResult.Success(0, "No collections.json found")
            }
            
            Log.i(TAG, "Reading collections from: ${collectionsFile.absolutePath}")
            val jsonContent = collectionsFile.readText()
            
            val collections = json.decodeFromString<List<CollectionImportData>>(jsonContent)
            Log.i(TAG, "Parsed ${collections.size} collections from JSON")
            
            // Convert to entities with resolved show IDs
            val entities = collections.map { collection ->
                convertToEntity(collection)
            }
            
            // Clear existing collections and insert new ones
            collectionsDao.deleteAllCollections()
            collectionsDao.insertCollections(entities)
            
            Log.i(TAG, "Successfully imported ${entities.size} collections")
            CollectionsImportResult.Success(entities.size, "Collections imported successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import collections", e)
            CollectionsImportResult.Error("Failed to import collections: ${e.message}")
        }
    }
    
    /**
     * Convert JSON import data to database entity with resolved show IDs
     */
    private suspend fun convertToEntity(collection: CollectionImportData): DeadCollectionEntity {
        val currentTime = System.currentTimeMillis()
        
        // Resolve show IDs from show_selector patterns
        val resolvedShowIds = resolveShowIds(collection.showSelector)
        
        // Convert to JSON for storage
        val tagsJson = json.encodeToString(ListSerializer(String.serializer()), collection.tags)
        val showIdsJson = json.encodeToString(ListSerializer(String.serializer()), resolvedShowIds)
        
        return DeadCollectionEntity(
            id = collection.id,
            name = collection.name,
            description = collection.description,
            tagsJson = tagsJson,
            showIdsJson = showIdsJson,
            totalShows = resolvedShowIds.size,
            primaryTag = collection.tags.firstOrNull(),
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
    
    /**
     * Resolve show_selector patterns into actual show IDs from database
     */
    private suspend fun resolveShowIds(showSelector: ShowSelectorData?): List<String> {
        if (showSelector == null) {
            return emptyList()
        }
        
        val resolvedIds = mutableSetOf<String>()
        
        try {
            // Add explicit show IDs
            resolvedIds.addAll(showSelector.showIds)
            
            // Add shows by specific dates
            showSelector.dates.forEach { date ->
                val shows = showDao.getShowsByDate(date)
                resolvedIds.addAll(shows.map { it.showId })
            }
            
            // Add shows by date ranges
            showSelector.ranges.forEach { range ->
                val shows = showDao.getShowsInDateRange(range.start, range.end)
                resolvedIds.addAll(shows.map { it.showId })
            }
            
            // Add shows by venue names
            showSelector.venues.forEach { venue ->
                val shows = showDao.getShowsByVenue(venue)
                resolvedIds.addAll(shows.map { it.showId })
            }
            
            // Add shows by years
            showSelector.years.forEach { year ->
                val shows = showDao.getShowsByYear(year)
                resolvedIds.addAll(shows.map { it.showId })
            }
            
            Log.d(TAG, "Resolved ${resolvedIds.size} show IDs from show_selector patterns")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve some show IDs from show_selector", e)
        }
        
        return resolvedIds.toList().sorted()
    }
}

/**
 * Result of collections import operation
 */
sealed class CollectionsImportResult {
    data class Success(val importedCount: Int, val message: String) : CollectionsImportResult()
    data class Error(val error: String) : CollectionsImportResult()
}