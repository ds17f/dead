# Show Rating Implementation Plan

## Overview

We need to add ratings to our app, with the following requirements:
1. Fetch ratings from Archive.org for recordings
2. Map Archive.org recording ratings to our app's Shows
3. For shows with multiple recordings, use a preferred recording (e.g., soundboard)

## Implementation Details

### 1. Rating Model

Create a new Rating class in the model module:

```kotlin
@Serializable
data class Rating(
    val averageRating: Double,  // 0-5 stars, average of all reviews
    val numberOfReviews: Int,   // Total count of reviews
    val reviews: List<Review> = emptyList()  // Optional: store individual reviews
)

@Serializable
data class Review(
    val reviewerId: String,     // Archive.org username
    val reviewerName: String,   // Display name
    val rating: Int,            // 1-5 stars
    val title: String?,         // Review title
    val body: String?,          // Review content
    val date: String?           // Review submission date
)
```

### 2. Update Recording and Show Models

Add rating fields to the Recording model:

```kotlin
data class Recording(
    // existing fields...

    @SerialName("avg_rating")
    val averageRating: Double? = null,

    @SerialName("num_reviews")
    val numberOfReviews: Int? = null,

    val reviews: List<Review> = emptyList(),

    // computed property
    val rating: Rating?
        get() = if (averageRating != null && numberOfReviews != null) {
            Rating(averageRating, numberOfReviews, reviews)
        } else null
)
```

Add rating aggregation to the Show model:

```kotlin
data class Show(
    // existing fields...

    // Rating computed from recordings
    val aggregatedRating: Rating? = null,

    // Computed properties
    val rating: Double?
        get() = when {
            aggregatedRating != null -> aggregatedRating.averageRating
            bestRecording?.averageRating != null -> bestRecording.averageRating
            recordings.isNotEmpty() -> recordings.mapNotNull { it.averageRating }.average()
            else -> null
        }

    val displayRating: String
        get() = rating?.let { String.format("%.1f", it) } ?: "Not Rated"
)
```

### 3. Database Updates

Update the database entities:

```kotlin
// RecordingEntity.kt
data class RecordingEntity(
    // existing fields...

    val averageRating: Double? = null,
    val numberOfReviews: Int? = null,
    val reviewsJson: String? = null  // JSON-serialized reviews
)

// ShowEntity.kt
data class ShowEntity(
    // existing fields...

    val aggregatedRating: Double? = null,
    val aggregatedReviewCount: Int? = null
)
```

### 4. API Updates

Enhance the ArchiveApiService to fetch reviews:

```kotlin
interface ArchiveApiService {
    // existing methods...

    /**
     * Get reviews for a specific recording
     */
    @GET("metadata/{identifier}/reviews")
    suspend fun getRecordingReviews(
        @Path("identifier") identifier: String
    ): Response<ArchiveMetadataResponse>

    /**
     * Search for recordings with specific rating criteria
     */
    @GET("advancedsearch.php")
    suspend fun searchRecordingsByRating(
        @Query("q") query: String = "collection:GratefulDead AND avg_rating:[4 TO 5]",
        @Query("fl") fields: String = "identifier,title,date,venue,coverage,source,avg_rating,num_reviews",
        @Query("rows") rows: Int = 50,
        @Query("start") start: Int = 0,
        @Query("sort") sort: String = "avg_rating desc",
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>
}
```

Update the ArchiveApiClient:

```kotlin
class ArchiveApiClient @Inject constructor(
    private val apiService: ArchiveApiService
) {
    // existing methods...

    /**
     * Get reviews for a recording
     */
    suspend fun getRecordingReviews(
        identifier: String
    ): ApiResult<List<Review>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecordingReviews(identifier)

            if (response.isSuccessful && response.body() != null) {
                val reviews = response.body()!!.reviews?.map { review ->
                    Review(
                        reviewerId = review.reviewer ?: "",
                        reviewerName = review.reviewer ?: "Anonymous",
                        rating = review.stars ?: 0,
                        title = review.title,
                        body = review.body,
                        date = review.reviewDate
                    )
                } ?: emptyList()

                ApiResult.Success(reviews)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}
```

### 5. Mapper Updates

Update the Archive Mapper:

```kotlin
object ArchiveMapper {
    /**
     * Convert Archive search document to Recording domain model
     */
    fun ArchiveSearchResponse.ArchiveDoc.toRecording(): Recording {
        return Recording(
            // existing mappings...

            averageRating = avgRating,
            numberOfReviews = numReviews
        )
    }

    /**
     * Convert metadata response to Recording with reviews
     */
    fun ArchiveMetadataResponse.toRecording(): Recording {
        // existing code...

        // Map reviews if available
        val mappedReviews = reviews?.map { review ->
            Review(
                reviewerId = review.reviewer ?: "",
                reviewerName = review.reviewer ?: "Anonymous",
                rating = review.stars ?: 0,
                title = review.title,
                body = review.body,
                date = review.reviewDate
            )
        } ?: emptyList()

        return Recording(
            // existing fields...

            averageRating = reviews?.mapNotNull { it.stars }?.average(),
            numberOfReviews = reviews?.size ?: 0,
            reviews = mappedReviews
        )
    }

    /**
     * Convert list of recordings to Shows with ratings
     */
    fun List<Recording>.toShows(): List<Show> {
        return this.groupByShowWithFuzzyMatching()
            .map { recordings ->
                val firstRecording = recordings.first()

                // Aggregate ratings from recordings
                val allReviews = recordings.flatMap { it.reviews }
                val avgRating = if (allReviews.isNotEmpty()) {
                    allReviews.map { it.rating }.average()
                } else {
                    recordings.mapNotNull { it.averageRating }.average()
                }

                val reviewCount = allReviews.size.takeIf { it > 0 }
                    ?: recordings.sumOf { it.numberOfReviews ?: 0 }

                val aggregatedRating = if (avgRating > 0 && reviewCount > 0) {
                    Rating(avgRating, reviewCount, allReviews)
                } else null

                Show(
                    // existing fields...

                    aggregatedRating = aggregatedRating
                )
            }
    }
}
```

### 6. Rating Calculation Strategy

When calculating the rating for a show:

1. If possible, use the SBD (soundboard) recording with the highest rating
2. If no SBD available, use the highest-rated recording of any source type
3. If multiple high-quality recordings, use a weighted average favoring:
   - Higher review counts (more reliable rating)
   - SBD > MATRIX > FM > AUD source quality
   - More recent transfers (better audio quality)

```kotlin
fun determineShowRating(recordings: List<Recording>): Rating? {
    if (recordings.isEmpty()) return null

    // First try to use soundboard recordings
    val sbdRecordings = recordings.filter {
        it.cleanSource?.uppercase() == "SBD" && it.averageRating != null
    }

    if (sbdRecordings.isNotEmpty()) {
        // Use the highest-rated SBD with at least 3 reviews
        val bestSbd = sbdRecordings
            .filter { it.numberOfReviews ?: 0 >= 3 }
            .maxByOrNull { it.averageRating ?: 0.0 }

        if (bestSbd != null) {
            return bestSbd.rating
        }
    }

    // Next try any recording with at least 5 reviews
    val wellReviewedRecordings = recordings.filter { it.numberOfReviews ?: 0 >= 5 }
    if (wellReviewedRecordings.isNotEmpty()) {
        val bestRecording = wellReviewedRecordings.maxByOrNull { it.averageRating ?: 0.0 }
        if (bestRecording != null) {
            return bestRecording.rating
        }
    }

    // Fall back to a weighted average of all ratings
    val totalReviews = recordings.sumOf { it.numberOfReviews ?: 0 }
    if (totalReviews > 0) {
        val weightedSum = recordings.sumOf {
            (it.averageRating ?: 0.0) * (it.numberOfReviews ?: 0)
        }
        val avgRating = weightedSum / totalReviews

        return Rating(
            averageRating = avgRating,
            numberOfReviews = totalReviews
        )
    }

    return null
}
```

### 7. UI Considerations

1. Display star ratings in show lists and detail pages
2. Add a filter option to browse by rating
3. Create a "Top Rated Shows" section in the browse screen
4. Consider showing individual recording ratings when multiple recordings exist

## Migration Path

1. Add new fields to models without breaking existing functionality
2. Update the database schema with migration path
3. Enhance API calls to fetch rating data
4. Implement rating calculation logic
5. Update UI to display ratings
6. Add tests for rating-related functionality

This approach allows for a phased implementation while ensuring backward compatibility.