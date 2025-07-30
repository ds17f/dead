# LibraryV2 Technical Implementation Guide

## Overview

This document provides a comprehensive technical deep-dive into the LibraryV2 implementation, serving as a reference for replicating the V2 architecture pattern in other features.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        LibraryV2 Architecture                   │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer (Compose)                                            │
│  ├── LibraryV2Screen.kt                                        │
│  ├── ShowListItem / ShowGridItem                               │
│  └── LibraryV2ViewModel (StateFlow integration)                │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                  │
│  ├── LibraryV2Show (Rich domain model)                         │
│  └── Business logic via computed properties                    │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                 │
│  ├── LibraryV2Service (Clean interface)                        │
│  ├── LibraryV2ServiceStub (Stub implementation)                │
│  └── Real-time integration with DownloadV2Service              │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure                                                │
│  ├── Hilt Dependency Injection                                 │
│  ├── Feature Flag Control (@Named qualifiers)                  │
│  └── StateFlow reactive patterns                               │
└─────────────────────────────────────────────────────────────────┘
```

## Domain Model Implementation

### LibraryV2Show Domain Model
**Location**: `core/model/src/main/java/com/deadarchive/core/model/LibraryV2Show.kt`

```kotlin
/**
 * Domain model representing a Show within Library context.
 * Combines core concert data with library-specific metadata.
 * 
 * Key Design Principles:
 * - Composition over inheritance (contains Show, doesn't extend it)
 * - Rich domain model with computed properties
 * - Single source of truth for library state
 * - Immutable data structure with functional updates
 */
data class LibraryV2Show(
    val show: Show,                                    // Core concert data
    val addedToLibraryAt: Long,                       // Library timestamp
    val isPinned: Boolean = false,                    // Pin state
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED // Download state
) {
    // Delegate Show properties for convenient access
    val showId: String get() = show.showId
    val date: String get() = show.date
    val venue: String? get() = show.venue
    val location: String? get() = show.location
    val displayTitle: String get() = show.displayTitle
    val displayLocation: String get() = show.displayLocation
    val displayVenue: String get() = show.displayVenue
    val recordings: List<Recording> get() = show.recordings
    
    // Library-specific computed properties
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1
    val isDownloaded: Boolean get() = downloadStatus == DownloadStatus.COMPLETED
    val libraryAge: Long get() = System.currentTimeMillis() - addedToLibraryAt
}
```

**Key Design Decisions**:
- **Composition over inheritance**: Contains `Show` rather than extending it
- **Computed properties**: Business logic embedded in domain model
- **Delegate properties**: Convenient access to `Show` properties without repetition
- **Immutable design**: All updates create new instances

### Enum Handling Pattern
**Challenge**: Different `DownloadStatus` enums between API and model layers

```kotlin
// Model layer enum (core/model)
enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED }

// API layer enum (core/download-api)  
enum class DownloadStatus { NOT_DOWNLOADED, DOWNLOADING, COMPLETED, FAILED }

// Conversion in ViewModel
private fun convertDownloadStatus(apiStatus: ApiDownloadStatus): ModelDownloadStatus {
    return when (apiStatus) {
        ApiDownloadStatus.NOT_DOWNLOADED -> ModelDownloadStatus.QUEUED
        ApiDownloadStatus.DOWNLOADING -> ModelDownloadStatus.DOWNLOADING
        ApiDownloadStatus.COMPLETED -> ModelDownloadStatus.COMPLETED
        ApiDownloadStatus.FAILED -> ModelDownloadStatus.FAILED
    }
}
```

**Lesson**: Layer-specific enums enable clean boundaries but require explicit conversion

## Service Layer Implementation

### LibraryV2Service Interface
**Location**: `core/library-api/src/main/java/com/deadarchive/core/library/api/LibraryV2Service.kt`

```kotlin
/**
 * Clean service interface for Library V2 operations.
 * 
 * Design Principles:
 * - Returns LibraryV2Show domain models (not Show)
 * - Flow-based reactive APIs
 * - Result types for error handling
 * - Feature-specific operations only
 */
interface LibraryV2Service {
    // Query operations
    fun getLibraryV2Shows(): Flow<List<LibraryV2Show>>
    fun isShowInLibraryV2(showId: String): Flow<Boolean>
    
    // Library management
    suspend fun addShowToLibraryV2(showId: String): Result<Unit>
    suspend fun removeShowFromLibraryV2(showId: String): Result<Unit>
    suspend fun clearLibraryV2(): Result<Unit>
    
    // Pin management  
    suspend fun pinShowV2(showId: String): Result<Unit>
    suspend fun unpinShowV2(showId: String): Result<Unit>
    fun isShowPinnedV2(showId: String): Flow<Boolean>
    
    // Statistics
    suspend fun getLibraryV2Stats(): LibraryV2Stats
    suspend fun populateTestDataV2(): Result<Unit>
}
```

**Key Design Decisions**:
- **Domain model returns**: Returns `LibraryV2Show` instead of `Show`
- **Reactive APIs**: Flow-based for real-time UI updates
- **Error handling**: `Result<T>` for operations that can fail
- **Feature namespace**: All operations use "V2" suffix for clear separation

### LibraryV2ServiceStub Implementation
**Location**: `core/library/src/main/java/com/deadarchive/core/library/service/LibraryV2ServiceStub.kt`

```kotlin
/**
 * Stub implementation for Library V2 service.
 * 
 * Evolution:
 * Phase 1: Logging-only stubs
 * Phase 2: In-memory state (current)
 * Phase 3: Production-ready behavior
 */
@Singleton
class LibraryV2ServiceStub @Inject constructor() : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceStub"
    }
    
    // In-memory state for realistic behavior
    private val libraryShowIds = MutableStateFlow<Set<String>>(emptySet())
    private val pinnedShowIds = MutableStateFlow<Set<String>>(emptySet())
    
    // Sample data for immediate UI development
    private val sampleShows = listOf(
        Show(
            showId = "gd1977-05-08.sbd.miller.110987.sbeok.flac16",
            date = "1977-05-08",
            venue = "Barton Hall, Cornell University", 
            city = "Ithaca",
            state = "NY",
            recordings = emptyList()
        ),
        Show(
            showId = "gd1972-05-04.sbd.miller.29303.sbeok.flac16",
            date = "1972-05-04",
            venue = "Olympia Theatre",
            city = "Paris", 
            state = "France",
            recordings = emptyList()
        )
        // More sample shows...
    )
    
    override fun getLibraryV2Shows(): Flow<List<LibraryV2Show>> {
        Log.d(TAG, "STUB: getLibraryV2Shows() called")
        
        return combine(libraryShowIds, pinnedShowIds) { libraryIds, pinnedIds ->
            val shows = sampleShows.filter { it.showId in libraryIds }
                .map { show ->
                    LibraryV2Show(
                        show = show,
                        addedToLibraryAt = System.currentTimeMillis() - Random.nextLong(0, TimeUnit.DAYS.toMillis(365)),
                        isPinned = show.showId in pinnedIds,
                        downloadStatus = DownloadStatus.QUEUED // Will be overridden by real-time integration
                    )
                }
            
            // Sort pinned items first, then by date descending
            shows.sortedWith(
                compareBy<LibraryV2Show> { it.sortablePinStatus }
                    .thenByDescending { it.addedToLibraryAt }
            )
        }.onEach { shows ->
            Log.d(TAG, "STUB: returning ${shows.size} shows (${shows.count { it.isPinned }} pinned)")
        }
    }
    
    override suspend fun addShowToLibraryV2(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: addShowToLibraryV2(showId='$showId')")
        
        return if (libraryShowIds.value.contains(showId)) {
            Log.w(TAG, "STUB: show already in library")
            Result.failure(Exception("Show already in library"))
        } else {
            libraryShowIds.value = libraryShowIds.value + showId
            Log.d(TAG, "STUB: added show, library now has ${libraryShowIds.value.size} shows")
            Result.success(Unit)
        }
    }
    
    override suspend fun pinShowV2(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: pinShowV2(showId='$showId')")
        
        return if (!libraryShowIds.value.contains(showId)) {
            Log.w(TAG, "STUB: cannot pin show not in library")
            Result.failure(Exception("Show not in library"))
        } else {
            pinnedShowIds.value = pinnedShowIds.value + showId
            Log.d(TAG, "STUB: pinned show, ${pinnedShowIds.value.size} shows now pinned")
            Result.success(Unit)
        }
    }
    
    // Test data population for development
    override suspend fun populateTestDataV2(): Result<Unit> {
        Log.d(TAG, "STUB: populateTestDataV2() - adding sample shows")
        
        val testShowIds = sampleShows.take(5).map { it.showId }.toSet()
        libraryShowIds.value = testShowIds
        
        // Pin first two shows
        pinnedShowIds.value = testShowIds.take(2).toSet()
        
        Log.d(TAG, "STUB: populated ${testShowIds.size} test shows, ${pinnedShowIds.value.size} pinned")
        return Result.success(Unit)
    }
}
```

**Key Implementation Details**:
- **Progressive enhancement**: Started as logging-only, evolved to stateful
- **Realistic behavior**: In-memory state provides realistic UI behavior
- **Clear logging**: Every operation logged for debugging
- **Test data support**: `populateTestDataV2()` enables immediate UI development

## ViewModel Implementation

### LibraryV2ViewModel
**Location**: `feature/library/src/main/java/com/deadarchive/feature/library/LibraryV2ViewModel.kt`

```kotlin
/**
 * ViewModel for Library V2 screen.
 * 
 * Key Responsibilities:
 * - Coordinate between LibraryV2Service and DownloadV2Service
 * - Provide reactive UI state via StateFlow
 * - Handle real-time download status integration
 * - Manage UI actions and state updates
 */
@HiltViewModel
class LibraryV2ViewModel @Inject constructor(
    private val libraryV2Service: LibraryV2Service,
    private val downloadV2Service: DownloadV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryV2ViewModel"
    }
    
    // UI State Management
    private val _libraryV2UiState = MutableStateFlow<LibraryV2UiState>(LibraryV2UiState.Loading)
    val libraryV2UiState: StateFlow<LibraryV2UiState> = _libraryV2UiState.asStateFlow()
    
    // Real-time download integration
    val libraryShows: StateFlow<List<LibraryV2Show>> = libraryV2Service.getLibraryV2Shows()
        .flatMapLatest { libraryShows ->
            if (libraryShows.isEmpty()) {
                flowOf(emptyList<LibraryV2Show>())
            } else {
                // For each show, combine with its real download status
                val showFlows = libraryShows.map { libraryShow ->
                    combine(
                        flowOf(libraryShow),
                        downloadV2Service.getDownloadStatus(libraryShow.show)
                    ) { show, downloadApiStatus ->
                        // Convert API DownloadStatus to model DownloadStatus
                        val modelDownloadStatus = when (downloadApiStatus) {
                            ApiDownloadStatus.NOT_DOWNLOADED -> ModelDownloadStatus.QUEUED
                            ApiDownloadStatus.DOWNLOADING -> ModelDownloadStatus.DOWNLOADING
                            ApiDownloadStatus.COMPLETED -> ModelDownloadStatus.COMPLETED
                            ApiDownloadStatus.FAILED -> ModelDownloadStatus.FAILED
                        }
                        show.copy(downloadStatus = modelDownloadStatus)
                    }
                }
                combine(showFlows) { showArray -> showArray.toList() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        Log.d(TAG, "LibraryV2ViewModel initialized")
        
        // Update UI state based on library shows
        viewModelScope.launch {
            libraryShows.collect { shows ->
                _libraryV2UiState.value = if (shows.isEmpty()) {
                    LibraryV2UiState.Empty
                } else {
                    LibraryV2UiState.Success(shows)
                }
            }
        }
    }
    
    // UI Actions
    fun addToLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "addToLibrary: $showId")
            
            libraryV2Service.addShowToLibraryV2(showId)
                .onSuccess { Log.d(TAG, "Successfully added $showId to library") }
                .onFailure { Log.e(TAG, "Failed to add $showId to library: ${it.message}") }
        }
    }
    
    fun removeFromLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "removeFromLibrary: $showId")
            
            libraryV2Service.removeShowFromLibraryV2(showId)
                .onSuccess { Log.d(TAG, "Successfully removed $showId from library") }
                .onFailure { Log.e(TAG, "Failed to remove $showId from library: ${it.message}") }
        }
    }
    
    fun togglePin(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "togglePin: ${show.showId}, currently pinned: ${show.isPinned}")
            
            val result = if (show.isPinned) {
                libraryV2Service.unpinShowV2(show.showId)
            } else {
                libraryV2Service.pinShowV2(show.showId)
            }
            
            result
                .onSuccess { Log.d(TAG, "Successfully toggled pin for ${show.showId}") }
                .onFailure { Log.e(TAG, "Failed to toggle pin for ${show.showId}: ${it.message}") }
        }
    }
    
    fun downloadShow(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "downloadShow: ${show.showId}")
            
            downloadV2Service.downloadShow(show.show)
                .onSuccess { Log.d(TAG, "Successfully started download for ${show.showId}") }
                .onFailure { Log.e(TAG, "Failed to start download for ${show.showId}: ${it.message}") }
        }
    }
    
    fun populateTestData() {
        viewModelScope.launch {
            Log.d(TAG, "populateTestData")
            
            libraryV2Service.populateTestDataV2()
                .onSuccess { Log.d(TAG, "Successfully populated test data") }
                .onFailure { Log.e(TAG, "Failed to populate test data: ${it.message}") }
        }
    }
}

sealed class LibraryV2UiState {
    object Loading : LibraryV2UiState()
    object Empty : LibraryV2UiState()
    data class Success(val shows: List<LibraryV2Show>) : LibraryV2UiState()
    data class Error(val message: String) : LibraryV2UiState()
}
```

**Key Implementation Details**:
- **Real-time integration**: Complex Flow composition for download status
- **State management**: Clean StateFlow patterns for UI updates
- **Error handling**: Proper Result handling with logging
- **Service coordination**: Manages multiple services transparently

## UI Implementation

### LibraryV2Screen
**Location**: `feature/library/src/main/java/com/deadarchive/feature/library/LibraryV2Screen.kt`

Key UI components designed around the `LibraryV2Show` domain model:

```kotlin
@Composable
private fun ShowListItem(
    show: LibraryV2Show,
    onNavigateToShow: (String) -> Unit,
    onLongPress: (LibraryV2Show) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavigateToShow(show.showId) },
                onLongClick = { onLongPress(show) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Line 1: Date with pin and download indicators
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin indicator
                if (show.isPinned) {
                    Icon(
                        painter = IconResources.Content.PushPin(),
                        contentDescription = "Pinned",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                // Download indicator
                if (show.downloadStatus == DownloadStatus.COMPLETED) {
                    Icon(
                        painter = IconResources.Status.CheckCircle(),
                        contentDescription = "Downloaded",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Text(
                    text = show.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Line 2: Venue
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Line 3: City, State
            Text(
                text = show.displayLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShowGridItem(
    show: LibraryV2Show,
    onNavigateToShow: (String) -> Unit,
    onLongPress: (LibraryV2Show) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = { onNavigateToShow(show.showId) },
                onLongClick = { onLongPress(show) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Album cover - full square aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Perfect square for album art
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Line 1: Date with icons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin indicator
                if (show.isPinned) {
                    Icon(
                        painter = IconResources.Content.PushPin(),
                        contentDescription = "Pinned",
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                
                // Download indicator
                if (show.downloadStatus == DownloadStatus.COMPLETED) {
                    Icon(
                        painter = IconResources.Status.CheckCircle(),
                        contentDescription = "Downloaded",
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                
                Text(
                    text = show.date,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Line 2: Venue
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Line 3: City, State
            Text(
                text = show.displayLocation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

**Key UI Design Decisions**:
- **Domain model integration**: Components work directly with `LibraryV2Show`
- **Consistent data access**: Both list and grid items use same domain model properties
- **Visual indicators**: Pin and download status displayed using domain model state
- **Layout flexibility**: Proper album cover aspect ratios for grid view

## Dependency Injection Setup

### Hilt Module Configuration
**Location**: `core/library/src/main/java/com/deadarchive/core/library/di/LibraryV2Module.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryV2Module {
    
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindLibraryV2ServiceStub(
        impl: LibraryV2ServiceStub
    ): LibraryV2Service
    
    @Binds  
    @Singleton
    @Named("real")
    abstract fun bindLibraryV2ServiceReal(
        impl: LibraryV2ServiceImpl // Future implementation
    ): LibraryV2Service
}
```

**Feature Flag Integration**:
```kotlin
// In ViewModel
@HiltViewModel
class LibraryV2ViewModel @Inject constructor(
    @Named("stub") private val libraryV2Service: LibraryV2Service,
    // Future: Use settings to choose implementation
) : ViewModel()
```

## Testing Strategy

### Domain Model Testing
```kotlin
class LibraryV2ShowTest {
    
    @Test
    fun `sortablePinStatus returns correct values`() {
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
        
        assertTrue(downloadedShow.isDownloaded)
    }
}
```

### Service Testing
```kotlin
class LibraryV2ServiceStubTest {
    
    private lateinit var service: LibraryV2ServiceStub
    
    @Before
    fun setup() {
        service = LibraryV2ServiceStub()
    }
    
    @Test
    fun `addShowToLibraryV2 updates library shows`() = runTest {
        // Given
        val testShowId = "test-show-id"
        
        // When
        val result = service.addShowToLibraryV2(testShowId)
        
        // Then
        assertTrue(result.isSuccess)
        
        val libraryShows = service.getLibraryV2Shows().first()
        assertTrue(libraryShows.any { it.showId == testShowId })
    }
}
```

### ViewModel Testing
```kotlin
class LibraryV2ViewModelTest {
    
    @Mock private lateinit var libraryService: LibraryV2Service
    @Mock private lateinit var downloadService: DownloadV2Service
    
    private lateinit var viewModel: LibraryV2ViewModel
    
    @Test
    fun `library shows combine with download status correctly`() = runTest {
        // Given
        val testShow = LibraryV2Show(/* ... */)
        whenever(libraryService.getLibraryV2Shows()).thenReturn(flowOf(listOf(testShow)))
        whenever(downloadService.getDownloadStatus(any())).thenReturn(flowOf(ApiDownloadStatus.COMPLETED))
        
        // When
        viewModel = LibraryV2ViewModel(libraryService, downloadService)
        
        // Then
        val libraryShows = viewModel.libraryShows.first()
        assertEquals(ModelDownloadStatus.COMPLETED, libraryShows.first().downloadStatus)
    }
}
```

## Performance Optimizations

### StateFlow Configuration
```kotlin
// Proper StateFlow scoping for memory efficiency
val libraryShows: StateFlow<List<LibraryV2Show>> = libraryV2Service.getLibraryV2Shows()
    .flatMapLatest { /* complex Flow composition */ }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Stop after 5s of no subscribers
        initialValue = emptyList()
    )
```

### Flow Composition Optimization
```kotlin
// Efficient real-time integration
libraryV2Service.getLibraryV2Shows()
    .flatMapLatest { libraryShows ->
        if (libraryShows.isEmpty()) {
            flowOf(emptyList<LibraryV2Show>()) // Short-circuit for empty case
        } else {
            // Only create download flows for shows that exist
            val showFlows = libraryShows.map { libraryShow ->
                combine(flowOf(libraryShow), downloadV2Service.getDownloadStatus(libraryShow.show)) { show, status ->
                    show.copy(downloadStatus = convertStatus(status))
                }
            }
            combine(showFlows) { showArray -> showArray.toList() }
        }
    }
```

## Migration Considerations

### Future Database Integration
When implementing the real service:

```kotlin
@Singleton
class LibraryV2ServiceImpl @Inject constructor(
    private val libraryV2Dao: LibraryV2Dao,
    private val showRepository: ShowRepository
) : LibraryV2Service {
    
    override fun getLibraryV2Shows(): Flow<List<LibraryV2Show>> {
        return libraryV2Dao.getAllLibraryItemsWithShows()
            .map { entities ->
                entities.map { entity ->
                    LibraryV2Show(
                        show = showRepository.getShow(entity.showId),
                        addedToLibraryAt = entity.addedToLibraryAt,
                        isPinned = entity.isPinned,
                        downloadStatus = DownloadStatus.QUEUED // Will be updated by real-time integration
                    )
                }
            }
    }
}
```

### Backward Compatibility
- Keep both stub and real implementations available
- Use feature flags for gradual rollout
- Maintain identical interfaces between implementations
- Plan data migration strategy for existing users

---

This implementation guide provides the complete technical foundation for replicating the LibraryV2 architecture pattern in other features. The combination of domain-driven design, service composition, and stub-first development creates a robust foundation for complex feature development.