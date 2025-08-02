package com.deadarchive.core.data.service

import com.deadarchive.core.model.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global update manager that maintains update state across the entire app.
 * This allows the startup update check to notify UI components about available updates.
 */
@Singleton
class GlobalUpdateManager @Inject constructor() {
    
    companion object {
        private const val TAG = "GlobalUpdateManager"
    }
    
    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()
    
    /**
     * Updates the global update status, typically called from startup update check
     * or manual update checks.
     */
    fun setUpdateStatus(status: UpdateStatus?) {
        android.util.Log.d(TAG, "Setting global update status: ${status?.isUpdateAvailable}")
        _updateStatus.value = status
    }
    
    /**
     * Clears the update status, typically called when user dismisses or installs update.
     */
    fun clearUpdateStatus() {
        android.util.Log.d(TAG, "Clearing global update status")
        _updateStatus.value = null
    }
}