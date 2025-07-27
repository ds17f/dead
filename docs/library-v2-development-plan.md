# Library V2 Development Plan - Minimal Stub-First Approach

## Strategy Overview

Build Library V2 using **minimal logging-only stubs** first, implement the complete UI that talks to these stubs, then gradually evolve the stubs into more functional versions. Finally, implement real services and use feature flags to control which implementation is active.

## Development Phases

### Phase 1: Minimal Logging Stubs + UI Integration
### Phase 2: Evolve Stubs with Basic Functionality  
### Phase 3: Implement Real Services
### Phase 4: Feature Flag Control Between Stub/Real

---

## Phase 1: Minimal Logging Stubs + UI Integration

### Goal
Create the simplest possible stub implementations that just log method calls, then build UI that integrates with them. This establishes the architecture and integration patterns.

### Tasks

#### 1.1 API Interface Definition
**Location**: `core/library-api/src/main/java/com/deadarchive/core/library/api/LibraryV2Service.kt`

```kotlin
interface LibraryV2Service {
    fun getLibraryShows(): Flow<List<Show>>
    suspend fun addShowToLibrary(showId: String): Result<Unit>
    suspend fun removeShowFromLibrary(showId: String): Result<Unit>
    suspend fun clearLibrary(): Result<Unit>
    fun isShowInLibrary(showId: String): Flow<Boolean>
    suspend fun getLibraryStats(): LibraryStats
}

data class LibraryStats(
    val totalShows: Int,
    val totalDownloaded: Int,
    val totalStorageUsed: Long
)
```

**Location**: `core/download-api/src/main/java/com/deadarchive/core/download/api/DownloadV2Service.kt`

```kotlin
interface DownloadV2Service {
    suspend fun downloadShow(show: Show): Result<Unit>
    suspend fun cancelShowDownloads(show: Show): Result<Unit>
    fun getDownloadStatus(show: Show): Flow<DownloadStatus>
    fun getDownloadProgress(show: Show): Flow<DownloadProgress>
    fun hasDownloads(show: Show): Flow<Boolean>
}

enum class DownloadStatus { NOT_DOWNLOADED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadProgress(
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus
)
```

#### 1.2 Minimal Logging-Only Stubs
**Location**: `core/library/src/main/java/com/deadarchive/core/library/service/LibraryV2ServiceStub.kt`

```kotlin
@Singleton
class LibraryV2ServiceStub @Inject constructor() : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceStub"
    }
    
    override fun getLibraryShows(): Flow<List<Show>> {
        Log.d(TAG, "STUB: getLibraryShows() called")
        return flowOf(emptyList()) // Return empty list, just log the call
    }
    
    override suspend fun addShowToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: addShowToLibrary(showId='$showId') called")
        return Result.success(Unit) // Just log and return success
    }
    
    override suspend fun removeShowFromLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: removeShowFromLibrary(showId='$showId') called")
        return Result.success(Unit)
    }
    
    override suspend fun clearLibrary(): Result<Unit> {
        Log.d(TAG, "STUB: clearLibrary() called")
        return Result.success(Unit)
    }
    
    override fun isShowInLibrary(showId: String): Flow<Boolean> {
        Log.d(TAG, "STUB: isShowInLibrary(showId='$showId') called")
        return flowOf(false) // Always return false, just log
    }
    
    override suspend fun getLibraryStats(): LibraryStats {
        Log.d(TAG, "STUB: getLibraryStats() called")
        return LibraryStats(totalShows = 0, totalDownloaded = 0, totalStorageUsed = 0L)
    }
}
```

**Location**: `core/download/src/main/java/com/deadarchive/core/download/service/DownloadV2ServiceStub.kt`

```kotlin
@Singleton
class DownloadV2ServiceStub @Inject constructor() : DownloadV2Service {
    
    companion object {
        private const val TAG = "DownloadV2ServiceStub"
    }
    
    override suspend fun downloadShow(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: downloadShow(showId='${show.showId}') called")
        return Result.success(Unit) // Just log, return success
    }
    
    override suspend fun cancelShowDownloads(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: cancelShowDownloads(showId='${show.showId}') called")
        return Result.success(Unit)
    }
    
    override fun getDownloadStatus(show: Show): Flow<DownloadStatus> {
        Log.d(TAG, "STUB: getDownloadStatus(showId='${show.showId}') called")
        return flowOf(DownloadStatus.NOT_DOWNLOADED) // Always not downloaded
    }
    
    override fun getDownloadProgress(show: Show): Flow<DownloadProgress> {
        Log.d(TAG, "STUB: getDownloadProgress(showId='${show.showId}') called")
        return flowOf(DownloadProgress(0.0f, 0L, 0L, DownloadStatus.NOT_DOWNLOADED))
    }
    
    override fun hasDownloads(show: Show): Flow<Boolean> {
        Log.d(TAG, "STUB: hasDownloads(showId='${show.showId}') called")
        return flowOf(false) // Always return false
    }
}
```

#### 1.3 Hilt Module for Stubs
**Location**: `core/library/src/main/java/com/deadarchive/core/library/di/LibraryV2StubModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryV2StubModule {
    
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindLibraryV2ServiceStub(
        impl: LibraryV2ServiceStub
    ): LibraryV2Service
}
```

#### 1.4 LibraryV2ViewModel with Stub Integration
**Location**: `feature/library/src/main/java/com/deadarchive/feature/library/LibraryV2ViewModel.kt`

```kotlin
@HiltViewModel
class LibraryV2ViewModel @Inject constructor(
    @Named("stub") private val libraryV2Service: LibraryV2Service,
    @Named("stub") private val downloadV2Service: DownloadV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryV2ViewModel"
    }
    
    private val _uiState = MutableStateFlow<LibraryV2UiState>(LibraryV2UiState.Loading)
    val uiState: StateFlow<LibraryV2UiState> = _uiState.asStateFlow()
    
    private val _libraryStats = MutableStateFlow<LibraryStats?>(null)
    val libraryStats: StateFlow<LibraryStats?> = _libraryStats.asStateFlow()
    
    init {
        Log.d(TAG, "LibraryV2ViewModel initialized with STUB services")
        loadLibrary()
        loadLibraryStats()
    }
    
    private fun loadLibrary() {
        viewModelScope.launch {
            Log.d(TAG, "Loading library via stub service...")
            try {
                libraryV2Service.getLibraryShows()
                    .collect { shows ->
                        Log.d(TAG, "Received ${shows.size} shows from stub service")
                        _uiState.value = LibraryV2UiState.Success(shows)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading library from stub", e)
                _uiState.value = LibraryV2UiState.Error(e.message ?: "Failed to load")
            }
        }
    }
    
    private fun loadLibraryStats() {
        viewModelScope.launch {
            try {
                val stats = libraryV2Service.getLibraryStats()
                _libraryStats.value = stats
                Log.d(TAG, "Loaded stats from stub: $stats")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load stats from stub", e)
            }
        }
    }
    
    // All actions just call stubs and log
    fun addToLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: addToLibrary('$showId') -> calling stub")
            libraryV2Service.addShowToLibrary(showId)
                .onSuccess { Log.d(TAG, "ViewModel: addToLibrary succeeded") }
                .onFailure { Log.e(TAG, "ViewModel: addToLibrary failed: ${it.message}") }
        }
    }
    
    fun removeFromLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: removeFromLibrary('$showId') -> calling stub")
            libraryV2Service.removeShowFromLibrary(showId)
        }
    }
    
    fun downloadShow(show: Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: downloadShow('${show.showId}') -> calling stub")
            downloadV2Service.downloadShow(show)
        }
    }
    
    fun getDownloadStatus(show: Show): Flow<DownloadStatus> {
        Log.d(TAG, "ViewModel: getDownloadStatus('${show.showId}') -> calling stub")
        return downloadV2Service.getDownloadStatus(show)
    }
    
    fun retry() {
        Log.d(TAG, "ViewModel: retry() -> reloading with stubs")
        loadLibrary()
        loadLibraryStats()
    }
}

sealed class LibraryV2UiState {
    object Loading : LibraryV2UiState()
    data class Success(val shows: List<Show>) : LibraryV2UiState()
    data class Error(val message: String) : LibraryV2UiState()
}
```

#### 1.5 LibraryV2Screen with Stub Integration
**Location**: `feature/library/src/main/java/com/deadarchive/feature/library/LibraryV2Screen.kt`

```kotlin
@Composable
fun LibraryV2Screen(
    viewModel: LibraryV2ViewModel = hiltViewModel(),
    onNavigateToShow: (String) -> Unit = {},
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Stub Development Banner
        StubDevelopmentBanner(
            modifier = Modifier.fillMaxWidth()
        )
        
        // Stats Display (will show 0s from stub)
        libraryStats?.let { stats ->
            StatsCard(
                stats = stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        // Main Content
        when (val state = uiState) {
            is LibraryV2UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading library via stubs...")
                    }
                }
            }
            
            is LibraryV2UiState.Success -> {
                if (state.shows.isEmpty()) {
                    // Empty state (expected with minimal stubs)
                    EmptyLibraryContent(
                        onTestStubs = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show list (won't happen with minimal stubs)
                    LibraryShowsList(
                        shows = state.shows,
                        viewModel = viewModel,
                        onNavigateToShow = onNavigateToShow,
                        onNavigateToPlayer = onNavigateToPlayer
                    )
                }
            }
            
            is LibraryV2UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StubDevelopmentBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Library V2 - Minimal Stub Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Services are logging-only stubs • Check logcat for calls",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent(
    onTestStubs: LibraryV2ViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Library is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Stubs return empty data. Test the integration by tapping buttons below.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test buttons to verify stub integration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTestStubs.addToLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Add to Library (Check Logs)")
            }
            
            OutlinedButton(
                onClick = { onTestStubs.removeFromLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Remove from Library (Check Logs)")
            }
            
            OutlinedButton(
                onClick = { 
                    onTestStubs.downloadShow(
                        Show(
                            showId = "test-show-1",
                            date = "1977-05-08",
                            venue = "Test Venue",
                            city = "Test City",
                            recordings = emptyList(),
                            isInLibrary = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Download Show (Check Logs)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "💡 All actions only log to console. No actual data changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}
```

### Checkpoint 1: Minimal Stubs + UI Integration Complete
**Success Criteria**:
- [ ] LibraryV2Screen loads and displays empty state
- [ ] All buttons trigger ViewModel methods
- [ ] ViewModel calls stub services
- [ ] Stub services log all method calls clearly
- [ ] No crashes, clean integration
- [ ] Logcat shows complete call chain: UI → ViewModel → Service

**Test**: 
1. Enable Library V2 feature flag
2. Navigate to Library V2 screen
3. Tap all test buttons
4. Verify logcat shows: `LibraryV2ViewModel: addToLibrary('test-show-1') -> calling stub` followed by `LibraryV2ServiceStub: STUB: addShowToLibrary(showId='test-show-1') called`

---

## Phase 2: Evolve Stubs with Basic Functionality

### Goal
Add minimal functionality to stubs so they can provide realistic behavior for UI development.

### Tasks

#### 2.1 Enhanced LibraryV2ServiceStub with In-Memory State
```kotlin
@Singleton
class LibraryV2ServiceStub @Inject constructor() : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceStub"
    }
    
    // Add minimal in-memory state
    private val libraryShowIds = MutableStateFlow<Set<String>>(emptySet())
    private val sampleShows = listOf(
        Show("1977-05-08_cornell", "1977-05-08", "Barton Hall", "Ithaca, NY", emptyList(), false),
        Show("1972-05-04_olympia", "1972-05-04", "Olympia Theatre", "Paris, France", emptyList(), false)
    )
    
    override fun getLibraryShows(): Flow<List<Show>> {
        Log.d(TAG, "STUB: getLibraryShows() called")
        return libraryShowIds.map { ids ->
            val shows = sampleShows.filter { it.showId in ids }
            Log.d(TAG, "STUB: returning ${shows.size} shows from library")
            shows
        }
    }
    
    override suspend fun addShowToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: addShowToLibrary(showId='$showId') called")
        
        return if (libraryShowIds.value.contains(showId)) {
            Log.d(TAG, "STUB: show already in library")
            Result.failure(Exception("Show already in library"))
        } else {
            libraryShowIds.value = libraryShowIds.value + showId
            Log.d(TAG, "STUB: added show to library, now has ${libraryShowIds.value.size} shows")
            Result.success(Unit)
        }
    }
    
    override suspend fun removeShowFromLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: removeShowFromLibrary(showId='$showId') called")
        libraryShowIds.value = libraryShowIds.value - showId
        Log.d(TAG, "STUB: removed show from library, now has ${libraryShowIds.value.size} shows")
        return Result.success(Unit)
    }
    
    override suspend fun getLibraryStats(): LibraryStats {
        Log.d(TAG, "STUB: getLibraryStats() called")
        val count = libraryShowIds.value.size
        return LibraryStats(
            totalShows = count,
            totalDownloaded = count / 2, // Simulate some downloads
            totalStorageUsed = count * 150_000_000L // ~150MB per show
        ).also {
            Log.d(TAG, "STUB: returning stats: $it")
        }
    }
}
```

#### 2.2 Enhanced DownloadV2ServiceStub with Simulated Progress
```kotlin
@Singleton
class DownloadV2ServiceStub @Inject constructor() : DownloadV2Service {
    
    companion object {
        private const val TAG = "DownloadV2ServiceStub"
    }
    
    // Simple download simulation
    private val downloadedShows = MutableStateFlow<Set<String>>(emptySet())
    private val downloadingShows = MutableStateFlow<Set<String>>(emptySet())
    
    override suspend fun downloadShow(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: downloadShow(showId='${show.showId}') called")
        
        if (downloadedShows.value.contains(show.showId)) {
            Log.d(TAG, "STUB: show already downloaded")
            return Result.success(Unit)
        }
        
        // Simulate download process
        downloadingShows.value = downloadingShows.value + show.showId
        Log.d(TAG, "STUB: starting simulated download...")
        
        // Simulate download completion after delay
        CoroutineScope(Dispatchers.IO).launch {
            delay(3000) // 3 second simulated download
            downloadingShows.value = downloadingShows.value - show.showId
            downloadedShows.value = downloadedShows.value + show.showId
            Log.d(TAG, "STUB: completed simulated download for ${show.showId}")
        }
        
        return Result.success(Unit)
    }
    
    override fun getDownloadStatus(show: Show): Flow<DownloadStatus> {
        Log.d(TAG, "STUB: getDownloadStatus(showId='${show.showId}') called")
        return combine(downloadedShows, downloadingShows) { downloaded, downloading ->
            when {
                downloaded.contains(show.showId) -> DownloadStatus.COMPLETED
                downloading.contains(show.showId) -> DownloadStatus.DOWNLOADING
                else -> DownloadStatus.NOT_DOWNLOADED
            }.also { status ->
                Log.d(TAG, "STUB: status for ${show.showId} = $status")
            }
        }
    }
    
    override fun hasDownloads(show: Show): Flow<Boolean> {
        Log.d(TAG, "STUB: hasDownloads(showId='${show.showId}') called")
        return downloadedShows.map { it.contains(show.showId) }
    }
}
```

#### 2.3 Update UI to Work with Enhanced Stubs
```kotlin
@Composable
private fun EmptyLibraryContent(
    onTestStubs: LibraryV2ViewModel,
    modifier: Modifier = Modifier
) {
    // Update to add sample shows that exist in stubs
    Column(/* ... */) {
        Text("Library is Empty (Enhanced Stubs)")
        Text("Stubs now maintain state and can show realistic behavior.")
        
        Button(
            onClick = { onTestStubs.addToLibrary("1977-05-08_cornell") }
        ) {
            Text("Add Cornell '77 (Will Appear)")
        }
        
        Button(
            onClick = { onTestStubs.addToLibrary("1972-05-04_olympia") }
        ) {
            Text("Add Olympia '72 (Will Appear)")
        }
    }
}
```

### Checkpoint 2: Enhanced Stubs Complete
**Success Criteria**:
- [ ] Library shows appear when added via stubs
- [ ] Library stats update correctly
- [ ] Download simulation works with state changes
- [ ] UI updates reactively based on stub state
- [ ] All logging still clearly shows stub behavior

---

## Phase 3: Implement Real Services

### Goal
Create real implementations of services that can be swapped in via feature flags.

### Tasks

#### 3.1 Real LibraryV2ServiceImpl
```kotlin
@Singleton
class LibraryV2ServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val libraryV2Repository: LibraryV2Repository,
    private val downloadV2Service: DownloadV2Service
) : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceImpl"
    }
    
    override fun getLibraryShows(): Flow<List<Show>> {
        Log.d(TAG, "REAL: getLibraryShows() called")
        return libraryV2Repository.getLibraryShows()
            .onEach { shows ->
                Log.d(TAG, "REAL: loaded ${shows.size} shows from database")
            }
    }
    
    override suspend fun addShowToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "REAL: addShowToLibrary(showId='$showId') called")
        return try {
            val success = libraryV2Repository.addShowToLibrary(showId)
            if (success) {
                Log.d(TAG, "REAL: successfully added show to database")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to add show to library"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "REAL: error adding show to library", e)
            Result.failure(e)
        }
    }
    
    // ... other real implementations
}
```

#### 3.2 Feature Flag Control
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryV2ServiceModule {
    
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
        impl: LibraryV2ServiceImpl
    ): LibraryV2Service
}

// In ViewModel
@HiltViewModel
class LibraryV2ViewModel @Inject constructor(
    @Named("stub") private val libraryV2ServiceStub: LibraryV2Service,
    @Named("real") private val libraryV2ServiceReal: LibraryV2Service,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val libraryV2Service: LibraryV2Service
        get() = if (useRealImplementation) libraryV2ServiceReal else libraryV2ServiceStub
    
    private var useRealImplementation = false
    
    init {
        // Read feature flag
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                useRealImplementation = settings.useLibraryV2RealImplementation
                Log.d(TAG, "Using ${if (useRealImplementation) "REAL" else "STUB"} implementation")
                loadLibrary() // Reload with new implementation
            }
        }
    }
}
```

### Checkpoint 3: Real Implementation with Feature Flag
**Success Criteria**:
- [ ] Real services work identically to enhanced stubs
- [ ] Feature flag switches between stub and real implementations
- [ ] UI behavior is identical regardless of implementation
- [ ] Can A/B test stub vs real performance and behavior
- [ ] Clear logging shows which implementation is active

---

## Phase 4: Feature Flag Control Between Stub/Real

### Goal
Create clean feature flag system for testing and gradual rollout.

### Tasks

#### 4.1 Settings for Implementation Control
```kotlin
// Add to AppSettings
data class AppSettings(
    // ... existing settings
    val useLibraryV2: Boolean = false,
    val useLibraryV2RealImplementation: Boolean = false
)

// Settings screen
@Composable
fun DeveloperOptionsSection() {
    // ... existing options
    
    SwitchPreference(
        title = "Use Library V2",
        summary = "Enable Library V2 preview",
        checked = settings.useLibraryV2,
        onCheckedChange = onUpdateUseLibraryV2
    )
    
    if (settings.useLibraryV2) {
        SwitchPreference(
            title = "Use Real Implementation",
            summary = "Switch from stubs to real services",
            checked = settings.useLibraryV2RealImplementation,
            onCheckedChange = onUpdateUseLibraryV2RealImplementation
        )
    }
}
```

#### 4.2 Implementation Factory Pattern
```kotlin
@Singleton
class LibraryV2ServiceFactory @Inject constructor(
    @Named("stub") private val stubService: LibraryV2Service,
    @Named("real") private val realService: LibraryV2Service,
    private val settingsRepository: SettingsRepository
) {
    
    fun getService(): Flow<LibraryV2Service> {
        return settingsRepository.getSettings().map { settings ->
            if (settings.useLibraryV2RealImplementation) {
                Log.d("LibraryV2Factory", "Providing REAL implementation")
                realService
            } else {
                Log.d("LibraryV2Factory", "Providing STUB implementation")
                stubService
            }
        }
    }
}
```

### Final Checkpoint: Complete Stub-to-Real System
**Success Criteria**:
- [ ] Can switch between stub and real implementations instantly
- [ ] UI works identically with both implementations
- [ ] Stubs provide predictable behavior for testing
- [ ] Real implementations provide production functionality
- [ ] Clear logging shows which implementation is active
- [ ] Feature flags allow safe gradual rollout

## Benefits of This Approach

### Development Benefits
1. **Immediate Integration**: UI development starts immediately with minimal stubs
2. **Clear Architecture**: Stub implementation defines exact service contracts
3. **Incremental Complexity**: Add functionality gradually as needed
4. **Risk Mitigation**: Working system at every stage

### Testing Benefits
1. **Predictable Stubs**: Logging-only stubs provide deterministic behavior
2. **Easy Debugging**: Clear logging shows exact call chains
3. **A/B Testing**: Can compare stub vs real behavior easily
4. **Performance Baseline**: Establish performance expectations with stubs

### Architecture Benefits
1. **Interface Validation**: Stubs prove API design is workable
2. **Clean Separation**: Clear boundary between stub and real implementations
3. **Feature Flag Safety**: Instant rollback capability
4. **Template for Other Features**: Pattern can be replicated for other areas

This approach maximizes learning speed while minimizing risk, giving us a working system at every stage of development.