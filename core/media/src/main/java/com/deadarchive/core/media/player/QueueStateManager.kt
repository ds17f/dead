package com.deadarchive.core.media.player

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
     */
    val queueIndex: Flow<Int> = combine(
        queueManager.getCurrentQueue(),
        mediaControllerRepository.currentTrackUrl
    ) { queueItems, currentUrl ->
        val controller = mediaControllerRepository.getMediaController()
        val currentIndex = controller?.currentMediaItemIndex ?: 0
        Log.d(TAG, "Queue index updated: $currentIndex of ${queueItems.size} items")
        currentIndex
    }
    
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