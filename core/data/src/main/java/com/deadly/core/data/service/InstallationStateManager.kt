package com.deadly.core.data.service

import com.deadly.core.model.UpdateInstallationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for installation state that can be shared between
 * UpdateServiceImpl and UpdateInstallReceiver.
 */
@Singleton
class InstallationStateManager @Inject constructor() {
    
    private val _installationState = MutableStateFlow(UpdateInstallationState())
    val installationState: StateFlow<UpdateInstallationState> = _installationState.asStateFlow()
    
    fun updateState(state: UpdateInstallationState) {
        _installationState.value = state
    }
    
    fun reset() {
        _installationState.value = UpdateInstallationState()
    }
}