package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.ShowFtsEntity

@Dao
interface ShowFtsDao {
    
    @Insert
    suspend fun insert(ftsEntity: ShowFtsEntity)
    
    @Insert
    suspend fun insertAll(ftsEntities: List<ShowFtsEntity>)
    
    @Query("DELETE FROM shows_fts")
    suspend fun deleteAll()
    
    /**
     * Awesome Bar - Full-text search across all show data
     * Searches: date, venue, location, songs
     */
    @Query("SELECT rowid FROM shows_fts WHERE shows_fts MATCH :query")
    suspend fun searchShows(query: String): List<Long>
    
    /**
     * Prefix search for autocomplete
     */
    @Query("SELECT rowid FROM shows_fts WHERE shows_fts MATCH :query || '*' LIMIT :limit")
    suspend fun searchShowsPrefix(query: String, limit: Int = 10): List<Long>
    
    /**
     * Phrase search for exact matches
     */
    @Query("SELECT rowid FROM shows_fts WHERE shows_fts MATCH '\"' || :phrase || '\"'")
    suspend fun searchShowsPhrase(phrase: String): List<Long>
    
    /**
     * Boolean search for complex queries
     * Examples: "Dark Star AND 1977", "Cornell OR Barton Hall"
     */
    @Query("SELECT rowid FROM shows_fts WHERE shows_fts MATCH :booleanQuery")
    suspend fun searchShowsBoolean(booleanQuery: String): List<Long>
}