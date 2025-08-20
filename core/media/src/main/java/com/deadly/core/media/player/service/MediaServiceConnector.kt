package com.deadly.core.media.player.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.deadly.core.media.service.DeadArchivePlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for MediaController connection lifecycle and service binding.
 * Handles connection establishment, failure recovery, and connection state management.
 */
@UnstableApi
@Singleton
class MediaServiceConnector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "MediaServiceConnector"
        private const val CONNECTION_TIMEOUT_MS = 5000L
    }
    
    // MediaController and connection state
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    // Connection state for debugging and error handling
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Connection callbacks
    private var onConnectionSuccessCallback: ((MediaController) -> Unit)? = null
    private var onConnectionFailedCallback: ((Exception) -> Unit)? = null
    
    /**
     * Get the current MediaController instance
     * @return MediaController if connected, null otherwise
     */
    fun getMediaController(): MediaController? = mediaController
    
    /**
     * Check if currently connected to the service
     */
    fun isConnectedToService(): Boolean = mediaController != null && _isConnected.value
    
    /**
     * Connect to the MediaSessionService using MediaController
     * @param onSuccess Callback when connection succeeds
     * @param onFailure Callback when connection fails
     */
    fun connectToService(
        onSuccess: (MediaController) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "=== ATTEMPTING TO CONNECT TO SERVICE ===")
        Log.d(TAG, "Package name: ${context.packageName}")
        Log.d(TAG, "Service component: ${ComponentName(context, DeadArchivePlaybackService::class.java)}")
        
        // Store callbacks
        onConnectionSuccessCallback = onSuccess
        onConnectionFailedCallback = onFailure
        
        val sessionToken = SessionToken(
            context, 
            ComponentName(context, DeadArchivePlaybackService::class.java)
        )
        Log.d(TAG, "SessionToken created: $sessionToken")
        
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
        Log.d(TAG, "MediaController.Builder.buildAsync() called")
        
        controllerFuture?.addListener({
            Log.d(TAG, "MediaController connection callback triggered")
            try {
                mediaController = controllerFuture?.get()
                Log.d(TAG, "=== MEDIACONTROLLER CONNECTED SUCCESSFULLY ===")
                Log.d(TAG, "Controller available commands: ${mediaController?.availableCommands}")
                Log.d(TAG, "Controller instance: ${mediaController?.javaClass?.simpleName}")
                onControllerConnected()
            } catch (e: Exception) {
                Log.e(TAG, "=== MEDIACONTROLLER CONNECTION FAILED ===", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                onControllerConnectionFailed(e)
            }
        }, MoreExecutors.directExecutor())
        
        Log.d(TAG, "Connection listener added, waiting for callback...")
    }
    
    /**
     * Called when MediaController successfully connects to service
     */
    private fun onControllerConnected() {
        val controller = mediaController ?: return
        
        _isConnected.value = true
        
        Log.d(TAG, "MediaServiceConnector: Connection established successfully")
        Log.d(TAG, "MediaServiceConnector: Controller ready for use")
        
        // Notify success callback
        onConnectionSuccessCallback?.invoke(controller)
    }
    
    /**
     * Called when MediaController connection fails
     */
    private fun onControllerConnectionFailed(error: Exception) {
        Log.e(TAG, "MediaServiceConnector: Connection failed", error)
        _isConnected.value = false
        mediaController = null
        
        // Notify failure callback
        onConnectionFailedCallback?.invoke(error)
    }
    
    /**
     * Disconnect from the service and clean up resources
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from MediaSession service")
        
        mediaController?.let {
            Log.d(TAG, "Releasing MediaController")
            MediaController.releaseFuture(controllerFuture!!)
        }
        
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
        
        // Clear callbacks
        onConnectionSuccessCallback = null
        onConnectionFailedCallback = null
        
        Log.d(TAG, "MediaServiceConnector: Disconnected and cleaned up")
    }
    
    /**
     * Attempt to reconnect to the service
     * Useful for error recovery scenarios
     */
    fun reconnect(
        onSuccess: (MediaController) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Attempting to reconnect to service")
        disconnect()
        connectToService(onSuccess, onFailure)
    }
}