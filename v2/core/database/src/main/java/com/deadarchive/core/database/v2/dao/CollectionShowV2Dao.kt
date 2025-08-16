package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.CollectionShowV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionShowV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collectionShows: List<CollectionShowV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collectionShow: CollectionShowV2Entity)
    
    @Query("DELETE FROM collection_show_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM collection_show_v2 WHERE collectionId = :collectionId ORDER BY orderIndex, showId")
    suspend fun getShowsInCollection(collectionId: String): List<CollectionShowV2Entity>
    
    @Query("SELECT * FROM collection_show_v2 WHERE collectionId = :collectionId ORDER BY orderIndex, showId")
    fun getShowsInCollectionFlow(collectionId: String): Flow<List<CollectionShowV2Entity>>
    
    @Query("SELECT * FROM collection_show_v2 WHERE showId = :showId ORDER BY collectionId")
    suspend fun getCollectionsForShow(showId: String): List<CollectionShowV2Entity>
    
    @Query("SELECT showId FROM collection_show_v2 WHERE collectionId = :collectionId ORDER BY orderIndex, showId")
    suspend fun getShowIdsInCollection(collectionId: String): List<String>
    
    @Query("SELECT collectionId FROM collection_show_v2 WHERE showId = :showId")
    suspend fun getCollectionIdsForShow(showId: String): List<String>
    
    @Query("SELECT COUNT(*) FROM collection_show_v2 WHERE collectionId = :collectionId")
    suspend fun getShowCountInCollection(collectionId: String): Int
    
    @Query("SELECT COUNT(DISTINCT collectionId) FROM collection_show_v2 WHERE showId = :showId")
    suspend fun getCollectionCountForShow(showId: String): Int
    
    @Query("DELETE FROM collection_show_v2 WHERE collectionId = :collectionId")
    suspend fun deleteCollectionShows(collectionId: String)
    
    @Query("DELETE FROM collection_show_v2 WHERE showId = :showId")
    suspend fun deleteShowFromCollections(showId: String)
    
    @Query("DELETE FROM collection_show_v2 WHERE collectionId = :collectionId AND showId = :showId")
    suspend fun removeShowFromCollection(collectionId: String, showId: String)
    
    @Query("SELECT COUNT(*) FROM collection_show_v2")
    suspend fun getTotalRelationshipCount(): Int
    
    @Query("""
        SELECT cs.*, c.name as collectionName 
        FROM collection_show_v2 cs 
        JOIN collection_v2 c ON cs.collectionId = c.collectionId 
        WHERE cs.showId = :showId 
        ORDER BY c.name
    """)
    suspend fun getCollectionInfoForShow(showId: String): List<CollectionShowWithName>
}

data class CollectionShowWithName(
    val id: String,
    val collectionId: String,
    val showId: String,
    val orderIndex: Int,
    val collectionName: String
)