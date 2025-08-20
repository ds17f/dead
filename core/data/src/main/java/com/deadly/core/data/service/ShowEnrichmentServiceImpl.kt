package com.deadly.core.data.service

import com.deadly.core.database.RecordingDao
import com.deadly.core.database.ShowEntity
import com.deadly.core.data.repository.RatingsRepository
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowEnrichmentServiceImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val ratingsRepository: RatingsRepository
) : ShowEnrichmentService {
    
    override suspend fun enrichShowWithRatings(
        showEntity: ShowEntity,
        userPreferences: Map<String, String>
    ): Show {
        // Get recordings for this show
        val recordings = attachRecordingsToShow(showEntity.showId)
        
        // Get show rating
        val showRating = ratingsRepository.getShowRatingByDateVenue(
            showEntity.date, showEntity.venue ?: ""
        )
        
        // Apply user preferences to determine best recording
        val finalBestRecordingId = applyUserPreferences(
            recordings = recordings,
            showId = showEntity.showId,
            userPreferences = userPreferences,
            ratingBasedBestRecordingId = showRating?.bestRecordingId
        )
        
        return showEntity.toShow(recordings).copy(
            rating = showRating?.rating,
            rawRating = showRating?.rawRating,
            ratingConfidence = showRating?.confidence,
            totalHighRatings = showRating?.totalHighRatings,
            totalLowRatings = showRating?.totalLowRatings,
            bestRecordingId = finalBestRecordingId
        )
    }
    
    override suspend fun enrichRecordingWithRating(recording: Recording): Recording {
        val recordingRating = ratingsRepository.getRecordingRating(recording.identifier)
        return recording.copy(
            rating = recordingRating?.rating,
            rawRating = recordingRating?.rawRating,
            ratingConfidence = recordingRating?.confidence,
            reviewCount = recordingRating?.reviewCount,
            sourceType = recordingRating?.sourceType,
            ratingDistribution = recordingRating?.ratingDistribution,
            highRatings = recordingRating?.highRatings,
            lowRatings = recordingRating?.lowRatings
        )
    }
    
    override suspend fun attachRecordingsToShow(showId: String): List<Recording> {
        return recordingDao.getRecordingsByConcertId(showId).map { recordingEntity ->
            val recording = recordingEntity.toRecording()
            enrichRecordingWithRating(recording)
        }
    }
    
    override fun applyUserPreferences(
        recordings: List<Recording>,
        showId: String,
        userPreferences: Map<String, String>,
        ratingBasedBestRecordingId: String?
    ): String? {
        // Check for user recording preference first (takes priority)
        val preferredRecordingId = userPreferences[showId]
        return if (preferredRecordingId != null && recordings.any { it.identifier == preferredRecordingId }) {
            preferredRecordingId
        } else {
            ratingBasedBestRecordingId
        }
    }
}