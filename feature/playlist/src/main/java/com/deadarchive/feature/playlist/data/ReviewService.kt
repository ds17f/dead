package com.deadarchive.feature.playlist.data

import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveReview
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching review data from Archive.org API
 */
@Singleton
class ReviewService @Inject constructor(
    private val archiveApiService: ArchiveApiService
) {
    
    /**
     * Fetches reviews for a recording from Archive.org
     * @param recordingId The Archive.org identifier for the recording
     * @return ReviewData containing reviews and rating distribution, or error
     */
    suspend fun getRecordingReviews(recordingId: String): Result<ReviewData> {
        return try {
            val response = archiveApiService.getRecordingMetadata(recordingId)
            
            if (response.isSuccessful) {
                val metadata = response.body()
                val archiveReviews = metadata?.metadata?.reviews ?: emptyList()
                
                // Convert Archive.org reviews to our Review model
                val reviews = archiveReviews.mapNotNull { archiveReview ->
                    if (archiveReview.reviewer != null && archiveReview.stars != null) {
                        Review(
                            username = archiveReview.reviewer,
                            rating = archiveReview.stars,
                            stars = archiveReview.stars.toDouble(),
                            reviewText = archiveReview.body ?: "",
                            reviewDate = formatReviewDate(archiveReview.reviewDate)
                        )
                    } else null
                }
                
                // Calculate rating distribution
                val ratingDistribution = reviews
                    .groupBy { it.rating }
                    .mapValues { it.value.size }
                
                Result.success(
                    ReviewData(
                        reviews = reviews,
                        ratingDistribution = ratingDistribution
                    )
                )
            } else {
                Result.failure(Exception("Failed to fetch reviews: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun formatReviewDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        
        return try {
            // Archive.org dates are typically in format: "2023-10-15 14:30:25"
            // Convert to more readable format: "Oct 15, 2023"
            val parts = dateString.split(" ").firstOrNull()?.split("-")
            if (parts != null && parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toIntOrNull()
                val day = parts[2].toIntOrNull()
                
                if (month != null && day != null) {
                    val monthNames = arrayOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    return "${monthNames[month - 1]} $day, $year"
                }
            }
            dateString
        } catch (e: Exception) {
            dateString
        }
    }
}