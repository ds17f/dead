package com.deadarchive.core.model

/**
 * Domain model representing the Player Queue state.
 * Manages queue items, current position, and queue metadata.
 * 
 * Key Design Principles:
 * - Rich domain model with computed properties
 * - Single source of truth for queue state
 * - Immutable data structure with functional updates
 * - Cross-recording queue support
 */
data class PlayerV2Queue(
    val items: List<PlayerV2QueueItem> = emptyList(),     // Queue items
    val currentIndex: Int = 0,                           // Current queue position
    val title: String? = null,                           // Queue title/description
    val createdAt: Long = System.currentTimeMillis(),    // When queue was created
    val shuffle: Boolean = false,                        // Shuffle mode state
    val repeatMode: RepeatMode = RepeatMode.NONE,        // Repeat mode
    val originalOrder: List<Int> = emptyList()           // Original indices for shuffle
) {
    // Queue state computed properties
    val isEmpty: Boolean 
        get() = items.isEmpty()
    
    val size: Int 
        get() = items.size
    
    val isValidIndex: Boolean 
        get() = currentIndex in items.indices
    
    val currentItem: PlayerV2QueueItem? 
        get() = items.getOrNull(currentIndex)
    
    val hasNext: Boolean 
        get() = when (repeatMode) {
            RepeatMode.ALL -> size > 1
            RepeatMode.ONE -> false
            RepeatMode.NONE -> currentIndex < items.size - 1
        }
    
    val hasPrevious: Boolean 
        get() = when (repeatMode) {
            RepeatMode.ALL -> size > 1
            RepeatMode.ONE -> false
            RepeatMode.NONE -> currentIndex > 0
        }
    
    val nextItem: PlayerV2QueueItem?
        get() = when {
            !hasNext -> null
            repeatMode == RepeatMode.ALL && currentIndex == items.size - 1 -> items.firstOrNull()
            else -> items.getOrNull(currentIndex + 1)
        }
    
    val previousItem: PlayerV2QueueItem?
        get() = when {
            !hasPrevious -> null
            repeatMode == RepeatMode.ALL && currentIndex == 0 -> items.lastOrNull()
            else -> items.getOrNull(currentIndex - 1)
        }
    
    val upcomingItems: List<PlayerV2QueueItem>
        get() = if (currentIndex < items.size - 1) {
            items.subList(currentIndex + 1, items.size)
        } else emptyList()
    
    val remainingItems: Int
        get() = maxOf(0, items.size - currentIndex - 1)
    
    val isShuffled: Boolean
        get() = shuffle && originalOrder.isNotEmpty()
    
    // Queue analysis properties
    val totalRecordings: Int
        get() = items.distinctBy { it.recordingId }.size
    
    val totalTracks: Int
        get() = items.size
    
    val recordingSummary: List<QueueRecordingSummary>
        get() = items.groupBy { it.recordingId }
            .map { (recordingId, tracks) ->
                QueueRecordingSummary(
                    recordingId = recordingId,
                    title = tracks.first().recordingTitle,
                    trackCount = tracks.size,
                    firstTrackIndex = items.indexOfFirst { it.recordingId == recordingId }
                )
            }
    
    val displayTitle: String
        get() = title ?: when {
            isEmpty -> "Empty Queue"
            totalRecordings == 1 -> items.first().recordingTitle ?: "Recording Queue"
            else -> "Mixed Queue ($totalRecordings recordings)"
        }
    
    // Navigation methods
    fun getNextIndex(): Int? {
        return when {
            !hasNext -> null
            repeatMode == RepeatMode.ALL && currentIndex == items.size - 1 -> 0
            else -> currentIndex + 1
        }
    }
    
    fun getPreviousIndex(): Int? {
        return when {
            !hasPrevious -> null
            repeatMode == RepeatMode.ALL && currentIndex == 0 -> items.size - 1
            else -> currentIndex - 1
        }
    }
    
    // Functional update methods
    fun updateCurrentIndex(index: Int): PlayerV2Queue {
        val validIndex = index.coerceIn(0, items.size - 1)
        return copy(currentIndex = validIndex)
    }
    
    fun addItem(item: PlayerV2QueueItem): PlayerV2Queue =
        copy(items = items + item)
    
    fun addItems(newItems: List<PlayerV2QueueItem>): PlayerV2Queue =
        copy(items = items + newItems)
    
    fun removeItem(index: Int): PlayerV2Queue {
        if (index !in items.indices) return this
        
        val newItems = items.toMutableList().apply { removeAt(index) }
        val newCurrentIndex = when {
            newItems.isEmpty() -> 0
            index < currentIndex -> currentIndex - 1
            index == currentIndex && currentIndex >= newItems.size -> maxOf(0, newItems.size - 1)
            else -> currentIndex
        }
        
        return copy(
            items = newItems,
            currentIndex = newCurrentIndex
        )
    }
    
    fun moveItem(fromIndex: Int, toIndex: Int): PlayerV2Queue {
        if (fromIndex !in items.indices || toIndex !in items.indices) return this
        
        val newItems = items.toMutableList()
        val item = newItems.removeAt(fromIndex)
        newItems.add(toIndex, item)
        
        // Update current index if needed
        val newCurrentIndex = when {
            fromIndex == currentIndex -> toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        
        return copy(
            items = newItems,
            currentIndex = newCurrentIndex
        )
    }
    
    fun clear(): PlayerV2Queue =
        copy(
            items = emptyList(),
            currentIndex = 0,
            title = null,
            originalOrder = emptyList()
        )
    
    fun updateTitle(newTitle: String?): PlayerV2Queue =
        copy(title = newTitle)
    
    fun toggleShuffle(): PlayerV2Queue {
        return if (shuffle) {
            // Turn off shuffle - restore original order
            if (originalOrder.isNotEmpty() && originalOrder.size == items.size) {
                val originalItems = originalOrder.map { items[it] }
                val newCurrentIndex = originalOrder.indexOf(currentIndex)
                copy(
                    items = originalItems,
                    currentIndex = newCurrentIndex.takeIf { it >= 0 } ?: 0,
                    shuffle = false,
                    originalOrder = emptyList()
                )
            } else {
                copy(shuffle = false, originalOrder = emptyList())
            }
        } else {
            // Turn on shuffle - save original order and shuffle
            val currentItem = items.getOrNull(currentIndex)
            val originalOrder = items.indices.toList()
            val shuffledIndices = originalOrder.shuffled()
            val shuffledItems = shuffledIndices.map { items[it] }
            val newCurrentIndex = currentItem?.let { item ->
                shuffledItems.indexOfFirst { it.mediaId == item.mediaId }
            } ?: 0
            
            copy(
                items = shuffledItems,
                currentIndex = newCurrentIndex,
                shuffle = true,
                originalOrder = originalOrder
            )
        }
    }
    
    fun updateRepeatMode(mode: RepeatMode): PlayerV2Queue =
        copy(repeatMode = mode)
    
    companion object {
        /**
         * Create queue from PlayerV2Recording
         */
        fun fromRecording(recording: PlayerV2Recording): PlayerV2Queue {
            val items = recording.playerTracks.mapIndexed { index, playerTrack ->
                PlayerV2QueueItem.fromPlayerTrack(
                    playerTrack = playerTrack,
                    recordingId = recording.recordingId,
                    recordingTitle = recording.displayTitle,
                    queueIndex = index
                )
            }
            
            return PlayerV2Queue(
                items = items,
                currentIndex = recording.currentTrackIndex,
                title = recording.displayTitle
            )
        }
        
        /**
         * Create empty queue
         */
        fun empty(): PlayerV2Queue = PlayerV2Queue()
        
        /**
         * Create queue from mixed tracks
         */
        fun fromMixedTracks(
            tracks: List<PlayerV2QueueItem>,
            title: String? = null
        ): PlayerV2Queue = PlayerV2Queue(
            items = tracks,
            title = title
        )
    }
}

/**
 * Individual item in the player queue
 */
data class PlayerV2QueueItem(
    val mediaId: String,                    // Unique identifier for media item
    val trackTitle: String,                 // Track display title
    val recordingId: String,                // Parent recording ID
    val recordingTitle: String?,            // Parent recording title
    val duration: kotlin.time.Duration,     // Track duration
    val audioUrl: String?,                  // Audio file URL
    val queueIndex: Int,                    // Position in queue
    val addedAt: Long                       // When added to queue
) {
    val displayTitle: String
        get() = trackTitle
    
    val displaySubtitle: String?
        get() = recordingTitle
    
    val hasAudio: Boolean
        get() = !audioUrl.isNullOrBlank()
    
    companion object {
        fun fromPlayerTrack(
            playerTrack: PlayerV2Track,
            recordingId: String,
            recordingTitle: String?,
            queueIndex: Int
        ): PlayerV2QueueItem = PlayerV2QueueItem(
            mediaId = "${recordingId}_${playerTrack.filename}",
            trackTitle = playerTrack.displayTitle,
            recordingId = recordingId,
            recordingTitle = recordingTitle,
            duration = playerTrack.duration,
            audioUrl = playerTrack.audioFile?.downloadUrl,
            queueIndex = queueIndex,
            addedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Summary of a recording's tracks in the queue
 */
data class QueueRecordingSummary(
    val recordingId: String,
    val title: String?,
    val trackCount: Int,
    val firstTrackIndex: Int
)

/**
 * Repeat modes for queue playback
 */
enum class RepeatMode {
    NONE,     // No repeat
    ONE,      // Repeat current track
    ALL       // Repeat entire queue
}