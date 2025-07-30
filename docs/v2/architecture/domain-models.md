# V2 Domain Models Guide

## Overview

Domain models are the cornerstone of V2 architecture. They represent business concepts within a specific feature context, combining core data with feature-specific state and behavior. This guide provides comprehensive patterns for designing and implementing V2 domain models.

## Domain Model Philosophy

### **Business-First Design**
V2 domain models capture business concepts, not technical database structures. They represent how users think about the domain, not how data is stored.

```kotlin
// ❌ Technical/Database-focused
data class LibraryEntry(
    val id: Long,
    val show_id: String,
    val added_timestamp: Long,
    val is_pinned: Boolean = false
)

// ✅ Business/Domain-focused  
data class LibraryV2Show(
    val show: Show,                    // Core business entity
    val addedToLibraryAt: Long,       // Business concept: "when added"
    val isPinned: Boolean = false,    // Business concept: "pinned favorite"
    val downloadStatus: DownloadStatus // Business concept: "availability"
) {
    // Business logic as computed properties
    val isRecentlyAdded: Boolean 
        get() = System.currentTimeMillis() - addedToLibraryAt < TimeUnit.DAYS.toMillis(7)
}
```

### **Feature Context Specificity**
Each domain model represents entities within a specific feature's context, including only data and behavior relevant to that feature.

```kotlin
// PlayerV2 context - focused on playback concerns
data class PlayerV2Track(
    val recording: Recording,
    val playbackPosition: Duration,
    val isCurrentlyPlaying: Boolean,
    val playbackQuality: AudioQuality
) {
    val remainingTime: Duration 
        get() = recording.duration - playbackPosition
    val progressPercentage: Float 
        get() = playbackPosition.toMillis() / recording.duration.toMillis().toFloat()
}

// LibraryV2 context - focused on library management  
data class LibraryV2Show(
    val show: Show,
    val addedToLibraryAt: Long,
    val isPinned: Boolean,
    val downloadStatus: DownloadStatus
) {
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1
    val displayAddedDate: String get() = formatDate(addedToLibraryAt)
}
```

## Core Design Patterns

### **1. Composition Over Inheritance**
V2 domain models contain core entities rather than extending them, providing flexibility and clear boundaries.

```kotlin
// ✅ Composition - Flexible and clear
data class BrowseV2Show(
    val show: Show,                    // Contains core entity
    val searchRelevance: Float,        // Feature-specific data
    val isInLibrary: Boolean,          // Cross-feature integration
    val browseContext: BrowseContext   // Feature-specific context
) {
    // Delegate core properties for convenience
    val showId: String get() = show.showId
    val date: String get() = show.date
    val venue: String? get() = show.venue
    
    // Feature-specific computed properties
    val relevanceCategory: RelevanceCategory 
        get() = when {
            searchRelevance > 0.8f -> RelevanceCategory.HIGH
            searchRelevance > 0.5f -> RelevanceCategory.MEDIUM
            else -> RelevanceCategory.LOW
        }
}

// ❌ Inheritance - Rigid and coupled
class BrowseV2Show(
    showId: String,
    date: String,
    // ... all Show properties duplicated
    val searchRelevance: Float  // Only addition
) : Show(showId, date, /* ... */) // Tightly coupled to Show implementation
```

### **2. Computed Properties for Business Logic**
Business logic lives in the domain model as computed properties, ensuring consistency and eliminating duplication.

```kotlin
data class LibraryV2Show(
    val show: Show,
    val addedToLibraryAt: Long,
    val isPinned: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED
) {
    // ✅ Business logic in domain model
    val sortablePinStatus: Int 
        get() = if (isPinned) 0 else 1
    
    val isDownloaded: Boolean 
        get() = downloadStatus == DownloadStatus.COMPLETED
    
    val libraryAge: Duration 
        get() = Duration.ofMillis(System.currentTimeMillis() - addedToLibraryAt)
    
    val displayStatus: String 
        get() = when {
            isPinned && isDownloaded -> "Pinned & Downloaded"
            isPinned -> "Pinned"
            isDownloaded -> "Downloaded"
            else -> "In Library"
        }
    
    // ✅ Sorting logic built into domain model
    companion object {
        val DEFAULT_COMPARATOR = compareBy<LibraryV2Show> { it.sortablePinStatus }
            .thenByDescending { it.addedToLibraryAt }
    }
}

// Usage in UI - no business logic needed
val sortedShows = shows.sortedWith(LibraryV2Show.DEFAULT_COMPARATOR)
```

### **3. Immutable Design with Functional Updates**
V2 domain models are immutable data classes that use copy operations for updates.

```kotlin
data class PlaylistV2Item(
    val recording: Recording,
    val addedToPlaylistAt: Long,
    val playOrder: Int,
    val isCurrentlyPlaying: Boolean = false
) {
    // ✅ Functional update methods
    fun markAsPlaying(): PlaylistV2Item = copy(isCurrentlyPlaying = true)
    fun markAsNotPlaying(): PlaylistV2Item = copy(isCurrentlyPlaying = false)
    fun updatePlayOrder(newOrder: Int): PlaylistV2Item = copy(playOrder = newOrder)
    
    // ✅ Validation in domain model
    fun isValidPlayOrder(): Boolean = playOrder >= 0
    
    // ✅ Business rules as computed properties
    val canMoveUp: Boolean get() = playOrder > 0
    val canMoveDown: Boolean get() = true // Would need playlist context for real implementation
}

// Service updates use functional style
override suspend fun updatePlayOrder(item: PlaylistV2Item, newOrder: Int): Result<PlaylistV2Item> {
    val updatedItem = item.updatePlayOrder(newOrder)
    return if (updatedItem.isValidPlayOrder()) {
        persistItem(updatedItem)
        Result.success(updatedItem)
    } else {
        Result.failure(IllegalArgumentException("Invalid play order: $newOrder"))
    }
}
```

### **4. Cross-Feature Integration**
V2 domain models can integrate data from multiple features while maintaining clear boundaries.

```kotlin
data class LibraryV2Show(
    val show: Show,                           // Core data
    val addedToLibraryAt: Long,              // Library feature
    val isPinned: Boolean = false,           // Library feature
    val downloadStatus: DownloadStatus,       // Download feature integration
    val lastPlayedAt: Long? = null,          // Player feature integration
    val playCount: Int = 0                   // Player feature integration
) {
    // Cross-feature computed properties
    val isRecentlyPlayed: Boolean 
        get() = lastPlayedAt?.let { 
            System.currentTimeMillis() - it < TimeUnit.DAYS.toMillis(7) 
        } ?: false
    
    val isPopular: Boolean 
        get() = playCount > 10
    
    val recommendationScore: Float
        get() = when {
            isPinned -> 1.0f
            isRecentlyPlayed -> 0.8f
            isPopular -> 0.6f
            isDownloaded -> 0.4f
            else -> 0.2f
        }
    
    // Feature-specific views
    val asLibraryItem: LibraryItem 
        get() = LibraryItem(showId, addedToLibraryAt, isPinned)
    val asPlaybackItem: PlaybackItem 
        get() = PlaybackItem(show, lastPlayedAt, playCount)
}
```

## Domain Model Templates

### **Basic Feature Domain Model**
```kotlin
/**
 * Template for simple V2 domain model
 * 
 * @param coreEntity The core business entity
 * @param featureContext Feature-specific timestamp/metadata
 * @param featureState Feature-specific state
 */
data class FeatureV2Item(
    val coreEntity: CoreEntity,               // Core business data
    val featureContext: Long,                 // When/how added to feature
    val featureState: FeatureState = FeatureState.DEFAULT,
    val integrationData: IntegrationData? = null // Optional cross-feature data
) {
    // Delegate core properties
    val id: String get() = coreEntity.id
    val name: String get() = coreEntity.name
    
    // Feature-specific computed properties
    val isActive: Boolean get() = featureState == FeatureState.ACTIVE
    val displayName: String get() = if (isActive) "★ $name" else name
    
    // Cross-feature computed properties  
    val hasIntegrationData: Boolean get() = integrationData != null
    
    // Business logic methods
    fun activate(): FeatureV2Item = copy(featureState = FeatureState.ACTIVE)
    fun deactivate(): FeatureV2Item = copy(featureState = FeatureState.INACTIVE)
}

enum class FeatureState { DEFAULT, ACTIVE, INACTIVE }
```

### **Complex Multi-Integration Domain Model**
```kotlin
/**
 * Template for complex V2 domain model with multiple integrations
 */
data class ComplexFeatureV2Item(
    val coreEntity: CoreEntity,
    val featureMetadata: FeatureMetadata,
    val crossFeatureData: CrossFeatureData
) {
    // Core entity delegation
    val id: String get() = coreEntity.id
    val createdAt: Long get() = coreEntity.createdAt
    
    // Feature metadata access
    val addedToFeatureAt: Long get() = featureMetadata.addedAt
    val featureConfig: FeatureConfig get() = featureMetadata.config
    
    // Cross-feature integration
    val downloadStatus: DownloadStatus get() = crossFeatureData.downloadStatus
    val lastAccessedAt: Long? get() = crossFeatureData.lastAccessedAt
    
    // Complex computed properties
    val priority: Priority 
        get() = calculatePriority(featureConfig, crossFeatureData)
    
    val displayMetrics: DisplayMetrics
        get() = DisplayMetrics(
            status = if (downloadStatus.isCompleted) "✓" else "○",
            recency = formatRecency(lastAccessedAt),
            priority = priority.displayText
        )
    
    // Business logic methods
    fun updateConfig(newConfig: FeatureConfig): ComplexFeatureV2Item =
        copy(featureMetadata = featureMetadata.copy(config = newConfig))
    
    private fun calculatePriority(config: FeatureConfig, crossData: CrossFeatureData): Priority {
        // Complex business logic for priority calculation
        return when {
            crossData.downloadStatus.isCompleted && config.isPinned -> Priority.HIGH
            crossData.lastAccessedAt.isRecent() -> Priority.MEDIUM
            else -> Priority.LOW
        }
    }
}

data class FeatureMetadata(
    val addedAt: Long,
    val config: FeatureConfig
)

data class CrossFeatureData(
    val downloadStatus: DownloadStatus,
    val lastAccessedAt: Long?
)
```

## Implementation Patterns

### **Factory Methods for Domain Models**
```kotlin
data class LibraryV2Show(/* ... */) {
    
    companion object {
        
        /**
         * Create LibraryV2Show from core Show with current timestamp
         */
        fun fromShow(show: Show): LibraryV2Show = LibraryV2Show(
            show = show,
            addedToLibraryAt = System.currentTimeMillis(),
            isPinned = false,
            downloadStatus = DownloadStatus.QUEUED
        )
        
        /**
         * Create LibraryV2Show with specific metadata
         */
        fun create(
            show: Show, 
            addedAt: Long, 
            isPinned: Boolean = false,
            downloadStatus: DownloadStatus = DownloadStatus.QUEUED
        ): LibraryV2Show = LibraryV2Show(
            show = show,
            addedToLibraryAt = addedAt,
            isPinned = isPinned,
            downloadStatus = downloadStatus
        )
        
        /**
         * Migration from legacy data structures
         */
        fun fromLegacyLibraryEntry(show: Show, entry: LegacyLibraryEntry): LibraryV2Show = 
            LibraryV2Show(
                show = show,
                addedToLibraryAt = entry.timestamp,
                isPinned = entry.isPinned,
                downloadStatus = mapLegacyDownloadStatus(entry.downloadState)
            )
    }
}
```

### **Domain Model Validation**
```kotlin
data class PlaylistV2Item(
    val recording: Recording,
    val playOrder: Int,
    val addedAt: Long
) {
    init {
        require(playOrder >= 0) { "Play order must be non-negative" }
        require(addedAt > 0) { "Added timestamp must be positive" }
    }
    
    /**
     * Domain validation methods
     */
    fun isValid(): Boolean = playOrder >= 0 && addedAt > 0
    
    fun validateForPlayback(): Result<Unit> = when {
        recording.duration <= Duration.ZERO -> 
            Result.failure(IllegalStateException("Recording has no duration"))
        recording.audioUrl.isBlank() -> 
            Result.failure(IllegalStateException("Recording has no audio URL"))
        else -> Result.success(Unit)
    }
    
    /**
     * Business rule validation
     */
    fun canBeReordered(newOrder: Int, playlistSize: Int): Boolean =
        newOrder >= 0 && newOrder < playlistSize
}
```

### **Domain Model Serialization**
```kotlin
@Serializable
data class LibraryV2Show(
    val show: Show,
    @SerialName("added_to_library_at")
    val addedToLibraryAt: Long,
    @SerialName("is_pinned") 
    val isPinned: Boolean = false,
    @SerialName("download_status")
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED
) {
    // Computed properties are not serialized
    @Transient
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1
    
    @Transient
    val isDownloaded: Boolean get() = downloadStatus == DownloadStatus.COMPLETED
}
```

## Testing Domain Models

### **Unit Testing Computed Properties**
```kotlin
class LibraryV2ShowTest {
    
    private val sampleShow = Show(
        showId = "test-show",
        date = "1977-05-08",
        venue = "Test Venue"
    )
    
    @Test
    fun `sortablePinStatus returns correct values for pinned and unpinned shows`() {
        val unpinnedShow = LibraryV2Show(
            show = sampleShow,
            addedToLibraryAt = System.currentTimeMillis(),
            isPinned = false
        )
        val pinnedShow = unpinnedShow.copy(isPinned = true)
        
        assertEquals(1, unpinnedShow.sortablePinStatus)
        assertEquals(0, pinnedShow.sortablePinStatus)
    }
    
    @Test
    fun `isDownloaded reflects download status correctly`() {
        val downloadedShow = LibraryV2Show(
            show = sampleShow,
            addedToLibraryAt = System.currentTimeMillis(),
            downloadStatus = DownloadStatus.COMPLETED
        )
        val notDownloadedShow = downloadedShow.copy(downloadStatus = DownloadStatus.QUEUED)
        
        assertTrue(downloadedShow.isDownloaded)
        assertFalse(notDownloadedShow.isDownloaded)
    }
    
    @Test
    fun `displayStatus shows correct combinations`() {
        val baseShow = LibraryV2Show(
            show = sampleShow,
            addedToLibraryAt = System.currentTimeMillis()
        )
        
        assertEquals("In Library", baseShow.displayStatus)
        assertEquals("Pinned", baseShow.copy(isPinned = true).displayStatus)
        assertEquals("Downloaded", baseShow.copy(downloadStatus = DownloadStatus.COMPLETED).displayStatus)
        assertEquals("Pinned & Downloaded", 
            baseShow.copy(isPinned = true, downloadStatus = DownloadStatus.COMPLETED).displayStatus)
    }
}
```

### **Integration Testing with Services**
```kotlin
class LibraryV2ServiceIntegrationTest {
    
    @Test
    fun `domain model integrates correctly with service operations`() = runTest {
        val service = LibraryV2ServiceStub()
        val testShow = LibraryV2Show.fromShow(sampleShow)
        
        // Test domain model creation
        val addResult = service.addShowToLibraryV2(testShow.showId)
        assertTrue(addResult.isSuccess)
        
        // Test domain model retrieval and properties
        val libraryShows = service.getLibraryV2Shows().first()
        val retrievedShow = libraryShows.first { it.showId == testShow.showId }
        
        assertFalse(retrievedShow.isPinned)
        assertEquals(DownloadStatus.QUEUED, retrievedShow.downloadStatus)
        assertTrue(retrievedShow.addedToLibraryAt > 0)
        
        // Test domain model updates
        val pinResult = service.pinShowV2(retrievedShow.showId)
        assertTrue(pinResult.isSuccess)
        
        val updatedShows = service.getLibraryV2Shows().first()
        val pinnedShow = updatedShows.first { it.showId == testShow.showId }
        assertTrue(pinnedShow.isPinned)
        assertEquals(0, pinnedShow.sortablePinStatus) // Computed property works
    }
}
```

## Best Practices

### **✅ Do**
- **Start with business concepts**: Model how users think about the domain
- **Use composition**: Contain core entities rather than extending them
- **Add computed properties**: Put business logic in domain models
- **Keep models immutable**: Use data classes with copy operations
- **Provide factory methods**: Make domain model creation convenient
- **Include validation**: Validate business rules in domain models
- **Test thoroughly**: Unit test all computed properties and business logic

### **❌ Don't**
- **Mirror database structure**: Domain models are not database entities
- **Include infrastructure concerns**: Keep persistence logic out of domain models
- **Make models mutable**: Avoid var properties and mutable collections
- **Add UI-specific logic**: Domain models should be UI-agnostic
- **Create god objects**: Keep domain models focused on their feature context
- **Skip validation**: Always validate business rules and constraints

### **🔄 Migration Patterns**
When migrating existing features to V2 domain models:

1. **Create domain model alongside existing model**
2. **Add factory methods for conversion between models**
3. **Implement V2 services that return domain models**
4. **Update UI components to use domain models**
5. **Migrate business logic from services to domain models**
6. **Remove old models once migration is complete**

---

V2 domain models are the foundation of clean, maintainable features. By following these patterns and principles, you can create rich domain models that encapsulate business logic, improve code organization, and provide a solid foundation for complex feature development.