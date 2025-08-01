package com.deadarchive.core.model

/**
 * Installation status for app updates
 */
data class UpdateInstallationState(
    val isInstalling: Boolean = false,
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)