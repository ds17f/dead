package com.deadly.core.settings

/**
 * UI state for settings screen operations
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isDraggingFormats: Boolean = false
)