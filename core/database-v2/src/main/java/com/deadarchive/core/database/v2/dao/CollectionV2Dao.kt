package com.deadarchive.core.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.core.database.v2.entities.CollectionV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionV2Entity)
    
    @Query("DELETE FROM collection_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM collection_v2 ORDER BY name")
    suspend fun getAllCollections(): List<CollectionV2Entity>
    
    @Query("SELECT * FROM collection_v2 ORDER BY name")
    fun getAllCollectionsFlow(): Flow<List<CollectionV2Entity>>
    
    @Query("SELECT * FROM collection_v2 WHERE collectionId = :collectionId")
    suspend fun getCollection(collectionId: String): CollectionV2Entity?
    
    @Query("SELECT * FROM collection_v2 WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name")
    suspend fun searchCollections(query: String): List<CollectionV2Entity>
    
    @Query("SELECT * FROM collection_v2 WHERE tags LIKE '%' || :tag || '%' ORDER BY name")
    suspend fun getCollectionsByTag(tag: String): List<CollectionV2Entity>
    
    @Query("SELECT * FROM collection_v2 WHERE totalShows BETWEEN :minShows AND :maxShows ORDER BY totalShows DESC")
    suspend fun getCollectionsByShowCount(minShows: Int, maxShows: Int): List<CollectionV2Entity>
    
    @Query("SELECT DISTINCT tags FROM collection_v2")
    suspend fun getAllTags(): List<String>
    
    @Query("SELECT COUNT(*) FROM collection_v2")
    suspend fun getCollectionCount(): Int
    
    @Query("SELECT SUM(totalShows) FROM collection_v2")
    suspend fun getTotalShowsInCollections(): Int
    
    @Query("SELECT * FROM collection_v2 ORDER BY totalShows DESC LIMIT :limit")
    suspend fun getLargestCollections(limit: Int = 10): List<CollectionV2Entity>
    
    @Query("SELECT * FROM collection_v2 WHERE tags LIKE '%era%' ORDER BY name")
    suspend fun getEraCollections(): List<CollectionV2Entity>
}