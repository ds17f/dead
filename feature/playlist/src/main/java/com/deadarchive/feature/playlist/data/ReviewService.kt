package com.deadarchive.feature.playlist.data

/**
 * Service for fetching review data from Archive.org API
 * TODO: Implement actual HTTP client integration
 */
class ReviewService {
    
    /**
     * Fetches reviews for a recording from Archive.org
     * @param recordingId The Archive.org identifier for the recording
     * @return List of reviews or empty list if none available
     */
    suspend fun getRecordingReviews(recordingId: String): Result<List<Review>> {
        return try {
            // TODO: Implement actual API call to https://archive.org/metadata/{recordingId}/reviews
            // For now, return empty list to avoid errors
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}