package com.deadly.core.data.service

import com.deadly.core.network.model.ArchiveMetadataResponse

/**
 * Service responsible for cache management and API interactions
 * for show and recording metadata.
 */
interface ShowCacheService {
    
    /**
     * Checks if a cache entry has expired based on the configured cache expiry time.
     */
    fun isCacheExpired(timestamp: Long): Boolean
    
    /**
     * Fetches recording metadata from Archive.org API.
     */
    suspend fun getRecordingMetadata(identifier: String): ArchiveMetadataResponse?
    
    /**
     * Determines if a file is an audio file based on its extension.
     */
    fun isAudioFile(filename: String): Boolean
    
    /**
     * Determines if a cached recording should be refreshed based on
     * cache expiry and content completeness (e.g., missing tracks).
     */
    fun shouldRefreshCache(
        cachedEntity: com.deadly.core.database.RecordingEntity?,
        hasEmptyTracks: Boolean = false
    ): Boolean
    
    companion object {
        const val CACHE_EXPIRY_HOURS = 24
    }
}