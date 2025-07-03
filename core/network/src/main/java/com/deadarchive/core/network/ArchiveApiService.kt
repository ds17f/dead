package com.deadarchive.core.network

import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Archive.org API service interface for accessing Grateful Dead concert recordings
 * 
 * Base URL: https://archive.org/
 * API Documentation: https://archive.org/help/aboutapi.php
 */
interface ArchiveApiService {
    
    /**
     * Search for Grateful Dead recordings using the advanced search API
     * 
     * @param query Search query string (e.g., "Grateful Dead 1977")
     * @param collection Filter by collection (default: GratefulDead)
     * @param mediaType Filter by media type (default: etree for recordings)
     * @param fields Fields to return in response
     * @param rows Number of results to return (max 10000)
     * @param start Starting index for pagination
     * @param sort Sort field and direction (e.g., "date desc")
     * @param output Output format (json, xml, csv)
     * @return Search response with concert listings
     */
    @GET("advancedsearch.php")
    suspend fun searchRecordings(
        @Query("q") query: String = "collection:GratefulDead",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,creator,year,source,taper,transferer,lineage,description,setlist,uploader,addeddate,publicdate",
        @Query("rows") rows: Int = 50,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "date desc",
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
    
    /**
     * Search recordings by date range
     * 
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @param fields Fields to return in response
     * @param rows Number of results to return
     * @param start Starting index for pagination
     * @return Search response with recordings in date range
     */
    @GET("advancedsearch.php")
    suspend fun searchRecordingsByDateRange(
        @Query("q") query: String = "collection:GratefulDead",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,creator,source",
        @Query("rows") rows: Int = 100,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "date asc",
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
    
    /**
     * Search recordings by venue
     * 
     * @param venue Venue name (e.g., "Fillmore West")
     * @param fields Fields to return in response
     * @param rows Number of results to return
     * @param start Starting index for pagination
     * @return Search response with recordings at specified venue
     */
    @GET("advancedsearch.php")
    suspend fun searchRecordingsByVenue(
        @Query("q") query: String = "collection:GratefulDead",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,source",
        @Query("rows") rows: Int = 100,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "date desc",
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
    
    /**
     * Get detailed metadata for a specific concert recording
     * 
     * @param identifier Archive.org item identifier
     * @return Metadata response with files, full metadata, and reviews
     */
    @GET("metadata/{identifier}")
    suspend fun getRecordingMetadata(
        @Path("identifier") identifier: String
    ): Response<ArchiveMetadataResponse>
    
    /**
     * Get files list for a specific concert recording
     * 
     * @param identifier Archive.org item identifier
     * @return Metadata response focused on files
     */
    @GET("metadata/{identifier}/files")
    suspend fun getRecordingFiles(
        @Path("identifier") identifier: String
    ): Response<ArchiveMetadataResponse>
    
    /**
     * Search for popular/highly-rated recordings
     * 
     * @param minRating Minimum average rating (1.0 to 5.0)
     * @param minReviews Minimum number of reviews
     * @param year Optional year filter
     * @param rows Number of results to return
     * @return Search response with popular recordings
     */
    @GET("advancedsearch.php")
    suspend fun getPopularRecordings(
        @Query("q") query: String = "collection:GratefulDead",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,source,downloads",
        @Query("rows") rows: Int = 50,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "downloads desc",
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
    
    /**
     * Get recently added recordings
     * 
     * @param days Number of days to look back (default 30)
     * @param rows Number of results to return
     * @return Search response with recently added recordings
     */
    @GET("advancedsearch.php")
    suspend fun getRecentRecordings(
        @Query("q") query: String = "collection:GratefulDead",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,source,addeddate,uploader",
        @Query("rows") rows: Int = 50,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "addeddate desc", 
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
}