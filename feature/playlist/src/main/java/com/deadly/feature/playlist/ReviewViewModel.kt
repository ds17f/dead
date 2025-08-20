package com.deadly.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.feature.playlist.data.Review
import com.deadly.feature.playlist.data.ReviewData
import com.deadly.feature.playlist.data.ReviewService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing review data and loading states
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewService: ReviewService
) : ViewModel() {
    
    private val _reviewState = MutableStateFlow(ReviewState())
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()
    
    fun loadReviews(recordingId: String) {
        viewModelScope.launch {
            _reviewState.value = _reviewState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            reviewService.getRecordingReviews(recordingId)
                .onSuccess { reviewData ->
                    _reviewState.value = _reviewState.value.copy(
                        isLoading = false,
                        reviews = reviewData.reviews,
                        ratingDistribution = reviewData.ratingDistribution,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _reviewState.value = _reviewState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load reviews"
                    )
                }
        }
    }
    
    fun clearReviews() {
        _reviewState.value = ReviewState()
    }
}

/**
 * State for review data
 */
data class ReviewState(
    val isLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val ratingDistribution: Map<Int, Int> = emptyMap(),
    val errorMessage: String? = null
)