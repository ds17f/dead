package com.deadly.core.media.player

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges QueueManager and MediaControllerRepository to provide queue state flows
 * without circular dependencies. This class exposes queue state from QueueManager
 * in a format compatible with existing UI components.
 */
@Singleton
class QueueStateManager @Inject constructor(
    private val queueManager: QueueManager,
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    companion object {
        private const val TAG = "QueueStateManager"
    }
    
    /**
     * Queue URLs from QueueManager
     */
    val queueUrls: Flow<List<String>> = queueManager.getCurrentQueue()
        .map { queueItems ->
            Log.d(TAG, "QueueManager queue updated: ${queueItems.size} items")
            queueItems.map { it.mediaId }
        }
    
    /**
     * Queue metadata (URL to title pairs) from QueueManager
     */
    val queueMetadata: Flow<List<Pair<String, String>>> = queueManager.getCurrentQueue()
        .map { queueItems ->
            queueItems.map { queueItem ->
                Pair(queueItem.mediaId, queueItem.title)
            }
        }
    
    /**
     * Current queue index from MediaController
     * FIXED: Removed currentTrackUrl dependency to prevent feedback loops
     * Added distinctUntilChanged to prevent duplicate emissions
     */
    val queueIndex: Flow<Int> = queueManager.getCurrentQueue()
        .map { queueItems ->
            val controller = mediaControllerRepository.getMediaController()
            val rawIndex = controller?.currentMediaItemIndex ?: 0
            
            // Validate index is within bounds
            val currentIndex = if (queueItems.isNotEmpty()) {
                rawIndex.coerceIn(0, queueItems.size - 1)
            } else {
                0
            }
            
            // Reduced logging to prevent spam - only log warnings and changes
            if (rawIndex != currentIndex) {
                Log.w(TAG, "Queue index out of bounds: $rawIndex, corrected to: $currentIndex (queue size: ${queueItems.size})")
            }
            
            currentIndex
        }
        .distinctUntilChanged()
    
    /**
     * Has next track in queue
     */
    val hasNext: Flow<Boolean> = combine(
        queueManager.getCurrentQueue(),
        queueIndex
    ) { queueItems, index ->
        val hasNext = index < queueItems.size - 1
        Log.d(TAG, "Has next: $hasNext (index $index of ${queueItems.size})")
        hasNext
    }
    
    /**
     * Has previous track in queue
     */
    val hasPrevious: Flow<Boolean> = queueIndex.map { index ->
        val hasPrevious = index > 0
        Log.d(TAG, "Has previous: $hasPrevious (index $index)")
        hasPrevious
    }
    
    /**
     * Current recording from QueueManager
     */
    val currentRecording = queueManager.currentRecording
}