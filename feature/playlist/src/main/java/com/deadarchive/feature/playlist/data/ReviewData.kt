package com.deadarchive.feature.playlist.data

/**
 * Container for review data fetched from Archive.org API
 */
data class ReviewData(
    val reviews: List<Review>,
    val ratingDistribution: Map<Int, Int>
)