package com.deadarchive.v2.core.database.service

/**
 * Generic progress interface for V2 database operations
 */
data class ProgressV2(
    val phase: String,
    val totalItems: Int,
    val processedItems: Int,
    val currentItem: String,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) (processedItems.toFloat() / totalItems) * 100f else 0f
}