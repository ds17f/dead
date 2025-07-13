package com.deadarchive.feature.playlist.data

/**
 * Data class representing a user review from Archive.org
 */
data class Review(
    val username: String,
    val rating: Int,
    val stars: Double,
    val reviewText: String,
    val reviewDate: String
)

/**
 * Archive.org API response structure for reviews
 */
data class ArchiveReviewsResponse(
    val reviews: List<ArchiveReview>
)

/**
 * Individual review item from Archive.org API
 */
data class ArchiveReview(
    val stars: Double,
    val reviewtitle: String,
    val reviewbody: String,
    val reviewdate: String?,
    val reviewer: String?
)