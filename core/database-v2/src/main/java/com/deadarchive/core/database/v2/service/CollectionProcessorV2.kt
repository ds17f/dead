package com.deadarchive.core.database.v2.service

import android.util.Log
import com.deadarchive.core.database.v2.dao.CollectionV2Dao
import com.deadarchive.core.database.v2.dao.CollectionShowV2Dao
import com.deadarchive.core.database.v2.entities.CollectionV2Entity
import com.deadarchive.core.database.v2.entities.CollectionShowV2Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CollectionJson(
    val collections: List<CollectionItemJson>
)

@Serializable
data class CollectionItemJson(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val show_ids: List<String>,
    val total_shows: Int,
    val aliases: List<String> = emptyList()
)

@Singleton
class CollectionProcessorV2 @Inject constructor(
    private val collectionDao: CollectionV2Dao,
    private val collectionShowDao: CollectionShowV2Dao
) {
    
    companion object {
        private const val TAG = "CollectionProcessorV2"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Process collections.json and save to database
     */
    suspend fun processCollectionsJson(collectionsJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting collections JSON processing...")
            
            val collectionsJson = json.decodeFromString<CollectionJson>(collectionsJsonContent)
            Log.d(TAG, "Parsed ${collectionsJson.collections.size} collections from JSON")
            
            val collections = mutableListOf<CollectionV2Entity>()
            val collectionShows = mutableListOf<CollectionShowV2Entity>()
            
            collectionsJson.collections.forEach { collectionItem ->
                // Create collection entity
                val collection = CollectionV2Entity(
                    collectionId = collectionItem.id,
                    name = collectionItem.name,
                    description = collectionItem.description,
                    tags = collectionItem.tags,
                    totalShows = collectionItem.total_shows,
                    aliases = collectionItem.aliases
                )
                collections.add(collection)
                
                // Create collection-show relationships
                collectionItem.show_ids.forEachIndexed { index, showId ->
                    val relationshipId = "${collectionItem.id}_${showId}"
                    val collectionShow = CollectionShowV2Entity(
                        id = relationshipId,
                        collectionId = collectionItem.id,
                        showId = showId,
                        orderIndex = index
                    )
                    collectionShows.add(collectionShow)
                }
                
                Log.d(TAG, "Processed collection '${collectionItem.name}' with ${collectionItem.show_ids.size} shows")
            }
            
            // Clear existing collections
            Log.d(TAG, "Clearing existing collection data...")
            collectionShowDao.deleteAll()
            collectionDao.deleteAll()
            
            // Insert new collections
            Log.d(TAG, "Inserting ${collections.size} collections...")
            collectionDao.insertAll(collections)
            
            Log.d(TAG, "Inserting ${collectionShows.size} collection-show relationships...")
            collectionShowDao.insertAll(collectionShows)
            
            Log.i(TAG, "Collections processing completed successfully")
            Log.i(TAG, "Imported ${collections.size} collections with ${collectionShows.size} show relationships")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing collections JSON", e)
            false
        }
    }
    
    /**
     * Get current collection statistics
     */
    suspend fun getCollectionStats(): CollectionStats = withContext(Dispatchers.IO) {
        try {
            val collectionCount = collectionDao.getCollectionCount()
            val relationshipCount = collectionShowDao.getTotalRelationshipCount()
            val totalShowsInCollections = collectionDao.getTotalShowsInCollections()
            
            CollectionStats(
                collectionCount = collectionCount,
                relationshipCount = relationshipCount,
                totalShowsInCollections = totalShowsInCollections
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting collection stats", e)
            CollectionStats(0, 0, 0)
        }
    }
}

data class CollectionStats(
    val collectionCount: Int,
    val relationshipCount: Int,
    val totalShowsInCollections: Int
)