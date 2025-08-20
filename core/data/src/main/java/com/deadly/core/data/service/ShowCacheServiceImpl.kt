package com.deadly.core.data.service

import com.deadly.core.database.RecordingEntity
import com.deadly.core.network.ArchiveApiService
import com.deadly.core.network.model.ArchiveMetadataResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowCacheServiceImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService
) : ShowCacheService {
    
    override fun isCacheExpired(timestamp: Long): Boolean {
        val expiryTime = timestamp + (ShowCacheService.CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
        return System.currentTimeMillis() > expiryTime
    }
    
    override suspend fun getRecordingMetadata(identifier: String): ArchiveMetadataResponse? {
        return try {
            val response = archiveApiService.getRecordingMetadata(identifier)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isAudioFile(filename: String): Boolean {
        val audioExtensions = setOf("mp3", "flac", "ogg", "m4a", "wav", "aac", "wma")
        val extension = filename.lowercase().substringAfterLast(".", "")
        return extension in audioExtensions
    }
    
    override fun shouldRefreshCache(
        cachedEntity: RecordingEntity?,
        hasEmptyTracks: Boolean
    ): Boolean {
        return when {
            cachedEntity == null -> true
            isCacheExpired(cachedEntity.cachedTimestamp) -> true
            hasEmptyTracks -> true
            else -> false
        }
    }
}