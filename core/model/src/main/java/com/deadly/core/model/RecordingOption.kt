package com.deadly.core.model

/**
 * Represents a recording option with recommendation information.
 * Used for displaying alternative recordings to users.
 */
data class RecordingOption(
    val recording: Recording,
    val isRecommended: Boolean = false,
    val matchReason: String? = null
)