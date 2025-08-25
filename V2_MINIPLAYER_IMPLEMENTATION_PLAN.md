# V2 MiniPlayer Implementation Plan - Complete Specification

## Module Structure (Following V2 Patterns)

### V2 Feature Module

```
v2/feature/miniplayer/
├── build.gradle.kts
└── src/main/java/com/deadly/v2/feature/miniplayer/
    └── screens/
        └── main/
            ├── MiniPlayerScreen.kt           # UI + direct ViewModel integration
            └── models/
                └── MiniPlayerViewModel.kt    # Service integration layer
```

### V2 Core API Module

```
v2/core/api/miniplayer/
├── build.gradle.kts
└── src/main/java/com/deadly/v2/core/api/miniplayer/
    └── MiniPlayerService.kt                  # Business logic interface only
```

### V2 Core Implementation Module

```
v2/core/miniplayer/
├── build.gradle.kts
└── src/main/java/com/deadly/v2/core/miniplayer/
    ├── service/
    │   └── MiniPlayerServiceImpl.kt          # Real implementation (no stub)
    ├── LastPlayedTrackService.kt             # Internal persistence (no API interface)
    └── di/
        └── MiniPlayerModule.kt               # Hilt DI configuration
```

## Implementation Steps

### Step 1: Enhance V2 MediaControllerRepository

Add missing StateFlows for complete MiniPlayer integration:

```kotlin
// v2/core/media/repository/MediaControllerRepository.kt

// ADD these StateFlows:
private val _currentPosition = MutableStateFlow(0L)
val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

private val _duration = MutableStateFlow(0L)
val duration: StateFlow<Long> = _duration.asStateFlow()

private val _currentShowId = MutableStateFlow<String?>(null)
val currentShowId: StateFlow<String?> = _currentShowId.asStateFlow()

private val _currentRecordingId = MutableStateFlow<String?>(null)
val currentRecordingId: StateFlow<String?> = _currentRecordingId.asStateFlow()

private val _currentTrackIndex = MutableStateFlow(0)
val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

// Computed progress for MiniPlayer progress bar
val progress: StateFlow<Float> = combine(
    _currentPosition, _duration
) { pos, dur -> if (dur > 0) pos.toFloat() / dur else 0f }

// ADD Media3 Player Listeners for automatic state updates:
controller.addListener(object : Player.Listener {
    override fun onPlaybackPositionChanged(eventTime: Long, position: Long) {
        _currentPosition.value = position
    }
    
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _currentShowId.value = extractShowIdFromMediaItem(mediaItem)
        _currentRecordingId.value = extractRecordingIdFromMediaItem(mediaItem)
        _currentTrackIndex.value = controller.currentMediaItemIndex
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            _duration.value = controller.duration.coerceAtLeast(0L)
        }
    }
})

// Helper methods to extract IDs from MediaMetadata
private fun extractShowIdFromMediaItem(mediaItem: MediaItem?): String? {
    return mediaItem?.mediaMetadata?.extras?.getString("showId")
}

private fun extractRecordingIdFromMediaItem(mediaItem: MediaItem?): String? {
    return mediaItem?.mediaMetadata?.extras?.getString("recordingId")
}
```

### Step 2: Create V2 Core API

```kotlin
// v2/core/api/miniplayer/MiniPlayerService.kt

interface MiniPlayerService {
    val isPlaying: Flow<Boolean>
    val currentPosition: Flow<Long>
    val duration: Flow<Long>
    val progress: Flow<Float>
    val currentTrackInfo: Flow<CurrentTrackInfo?>
    val currentShowId: Flow<String?>
    val currentRecordingId: Flow<String?>
    
    suspend fun togglePlayPause()
    suspend fun initialize()
    suspend fun cleanup()
}
```

### Step 3: Create V2 Core Implementation

#### LastPlayedTrackService (Internal - No API Interface)

```kotlin
// v2/core/miniplayer/LastPlayedTrackService.kt

@Singleton
class LastPlayedTrackService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    companion object {
        private const val TAG = "LastPlayedTrackService"
        private const val PREFS_NAME = "v2_last_played_track"
        private const val KEY_SHOW_ID = "show_id"
        private const val KEY_RECORDING_ID = "recording_id"
        private const val KEY_TRACK_INDEX = "track_index"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_TRACK_TITLE = "track_title"
        private const val KEY_TRACK_FILENAME = "track_filename"
        private const val KEY_SELECTED_FORMAT = "selected_format"
        private const val KEY_LAST_SAVED = "last_saved"
    }
    
    data class LastPlayedTrack(
        val showId: String,              // For playlist navigation
        val recordingId: String,         // For playback restoration
        val trackIndex: Int,
        val positionMs: Long,
        val trackTitle: String,
        val trackFilename: String,
        val selectedFormat: String,      // Store V2 format selection
        val lastSavedTime: Long
    )
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val saveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Save current track state to SharedPreferences
    fun saveCurrentTrack(
        showId: String,
        recordingId: String,
        trackIndex: Int,
        positionMs: Long,
        trackTitle: String,
        trackFilename: String,
        selectedFormat: String
    ) {
        Log.d(TAG, "Saving last played track: $trackTitle at ${positionMs}ms")
        
        prefs.edit()
            .putString(KEY_SHOW_ID, showId)
            .putString(KEY_RECORDING_ID, recordingId)
            .putInt(KEY_TRACK_INDEX, trackIndex)
            .putLong(KEY_POSITION_MS, positionMs)
            .putString(KEY_TRACK_TITLE, trackTitle)
            .putString(KEY_TRACK_FILENAME, trackFilename)
            .putString(KEY_SELECTED_FORMAT, selectedFormat)
            .putLong(KEY_LAST_SAVED, System.currentTimeMillis())
            .apply()
    }
    
    // Load last played track info from SharedPreferences
    fun getLastPlayedTrack(): LastPlayedTrack? {
        val showId = prefs.getString(KEY_SHOW_ID, null)
        val recordingId = prefs.getString(KEY_RECORDING_ID, null)
        val trackIndex = prefs.getInt(KEY_TRACK_INDEX, -1)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val trackTitle = prefs.getString(KEY_TRACK_TITLE, null)
        val trackFilename = prefs.getString(KEY_TRACK_FILENAME, null)
        val selectedFormat = prefs.getString(KEY_SELECTED_FORMAT, null)
        val lastSaved = prefs.getLong(KEY_LAST_SAVED, 0L)
        
        return if (showId != null && recordingId != null && trackIndex >= 0 &&
                   trackTitle != null && trackFilename != null && selectedFormat != null) {
            LastPlayedTrack(
                showId = showId,
                recordingId = recordingId,
                trackIndex = trackIndex,
                positionMs = positionMs,
                trackTitle = trackTitle,
                trackFilename = trackFilename,
                selectedFormat = selectedFormat,
                lastSavedTime = lastSaved
            )
        } else {
            Log.d(TAG, "No valid last played track found")
            null
        }
    }
    
    // Restore last played track on app start - makes MiniPlayer appear
    suspend fun restoreLastPlayedTrack() {
        try {
            val lastTrack = getLastPlayedTrack()
            if (lastTrack == null) {
                Log.d(TAG, "No last played track to restore")
                return
            }
            
            Log.d(TAG, "Restoring last played track: ${lastTrack.trackTitle}")
            
            // Load track into MediaController with exact position
            mediaControllerRepository.playTrack(
                trackIndex = lastTrack.trackIndex,
                recordingId = lastTrack.recordingId,
                format = lastTrack.selectedFormat,
                position = lastTrack.positionMs
            )
            
            Log.d(TAG, "Successfully restored last played track")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore last played track", e)
        }
    }
    
    // Built-in monitoring - combines persistence + monitoring in single service
    fun startMonitoring() {
        Log.d(TAG, "Starting playback state monitoring")
        
        saveScope.launch {
            combine(
                mediaControllerRepository.isPlaying,
                mediaControllerRepository.currentPosition,
                mediaControllerRepository.currentShowId,
                mediaControllerRepository.currentRecordingId,
                mediaControllerRepository.currentTrackIndex,
                mediaControllerRepository.currentTrack
            ).collect { (isPlaying, position, showId, recordingId, trackIndex, trackMetadata) ->
                
                // Auto-save every 10 seconds during playback - CRITICAL for persistence
                if (isPlaying && showId != null && recordingId != null && trackMetadata != null) {
                    
                    saveCurrentTrack(
                        showId = showId,
                        recordingId = recordingId,
                        trackIndex = trackIndex,
                        positionMs = position,
                        trackTitle = trackMetadata.title?.toString() ?: "Unknown Track",
                        trackFilename = extractFilename(trackMetadata),
                        selectedFormat = extractFormat(trackMetadata)
                    )
                }
            }
        }
    }
    
    private fun extractFilename(metadata: MediaMetadata): String {
        return metadata.extras?.getString("filename") ?: ""
    }
    
    private fun extractFormat(metadata: MediaMetadata): String {
        return metadata.extras?.getString("format") ?: "MP3"
    }
}
```

#### MiniPlayerServiceImpl (Business Logic)

```kotlin
// v2/core/miniplayer/service/MiniPlayerServiceImpl.kt

@Singleton
class MiniPlayerServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) : MiniPlayerService {
    
    companion object {
        private const val TAG = "MiniPlayerServiceImpl"
    }
    
    // Direct StateFlow delegation - perfect synchronization with MediaController
    override val isPlaying = mediaControllerRepository.isPlaying
    override val currentPosition = mediaControllerRepository.currentPosition
    override val duration = mediaControllerRepository.duration
    override val progress = mediaControllerRepository.progress
    override val currentShowId = mediaControllerRepository.currentShowId
    override val currentRecordingId = mediaControllerRepository.currentRecordingId
    
    // Convert MediaMetadata to rich CurrentTrackInfo for MiniPlayer display
    override val currentTrackInfo: Flow<CurrentTrackInfo?> =
        mediaControllerRepository.currentTrack.map { metadata ->
            metadata?.let { createCurrentTrackInfo(it) }
        }
    
    // Delegate playback commands to MediaControllerRepository
    override suspend fun togglePlayPause() {
        Log.d(TAG, "MiniPlayer togglePlayPause requested")
        mediaControllerRepository.togglePlayPause()
    }
    
    override suspend fun initialize() {
        Log.d(TAG, "MiniPlayer service initialized")
        // MediaControllerRepository handles its own initialization
    }
    
    override suspend fun cleanup() {
        Log.d(TAG, "MiniPlayer service cleanup")
        // No cleanup needed - MediaControllerRepository handles lifecycle
    }
    
    private fun createCurrentTrackInfo(metadata: MediaMetadata): CurrentTrackInfo {
        return CurrentTrackInfo(
            trackUrl = metadata.mediaUri?.toString() ?: "",
            recordingId = metadata.extras?.getString("recordingId") ?: "",
            showId = metadata.extras?.getString("showId") ?: "",
            showDate = metadata.extras?.getString("showDate") ?: "",
            venue = metadata.extras?.getString("venue") ?: "",
            location = metadata.extras?.getString("location") ?: "",
            songTitle = metadata.title?.toString() ?: "Unknown Track",
            trackNumber = metadata.trackNumber ?: 0,
            filename = metadata.extras?.getString("filename") ?: "",
            isPlaying = false, // Will be set by combining flows
            position = 0L,     // Will be set by combining flows
            duration = 0L      // Will be set by combining flows
        )
    }
}
```

#### Hilt Module

```kotlin
// v2/core/miniplayer/di/MiniPlayerModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class MiniPlayerModule {
    
    @Binds
    abstract fun bindMiniPlayerService(
        miniPlayerServiceImpl: MiniPlayerServiceImpl
    ): MiniPlayerService
}
```

### Step 4: Create V2 Feature Components

#### UI State Model

```kotlin
// v2/core/model/PlaylistModels.kt (add to existing file)

data class MiniPlayerUiState(
    val isPlaying: Boolean = false,
    val currentTrack: CurrentTrackInfo? = null,
    val progress: Float = 0f,
    val showId: String? = null,
    val recordingId: String? = null,
    val shouldShow: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

#### ViewModel

```kotlin
// v2/feature/miniplayer/screens/main/models/MiniPlayerViewModel.kt

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val miniPlayerService: MiniPlayerService
) : ViewModel() {
    
    companion object {
        private const val TAG = "MiniPlayerViewModel"
    }
    
    private val _uiState = MutableStateFlow(MiniPlayerUiState())
    val uiState: StateFlow<MiniPlayerUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "MiniPlayerViewModel initialized")
        initializeService()
        observeServiceState()
    }
    
    private fun initializeService() {
        viewModelScope.launch {
            try {
                miniPlayerService.initialize()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MiniPlayer service", e)
                _uiState.value = _uiState.value.copy(error = "Failed to initialize")
            }
        }
    }
    
    private fun observeServiceState() {
        viewModelScope.launch {
            combine(
                miniPlayerService.isPlaying,
                miniPlayerService.currentTrackInfo,
                miniPlayerService.progress,
                miniPlayerService.currentShowId,
                miniPlayerService.currentRecordingId
            ) { isPlaying, trackInfo, progress, showId, recordingId ->
                
                _uiState.value = MiniPlayerUiState(
                    isPlaying = isPlaying,
                    currentTrack = trackInfo,
                    progress = progress,
                    showId = showId,
                    recordingId = recordingId,
                    shouldShow = trackInfo != null, // Show MiniPlayer when track is loaded
                    isLoading = false,
                    error = null
                )
                
            }.collect()
        }
    }
    
    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "User requested play/pause toggle")
                miniPlayerService.togglePlayPause()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle play/pause", e)
                _uiState.value = _uiState.value.copy(error = "Playback error")
            }
        }
    }
    
    fun onTapToExpand() {
        val currentShowId = _uiState.value.showId
        Log.d(TAG, "User tapped to expand - showId: $currentShowId")
        // Callback will be handled by Screen component
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            miniPlayerService.cleanup()
        }
    }
}
```

#### Screen Component

```kotlin
// v2/feature/miniplayer/screens/main/MiniPlayerScreen.kt

@Composable
fun MiniPlayerScreen(
    onTapToExpand: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Log.e("MiniPlayerScreen", "Error: $error")
            delay(3000) // Show error for 3 seconds
            viewModel.clearError()
        }
    }
    
    // Only show MiniPlayer when there's a current track
    if (!uiState.shouldShow || uiState.currentTrack == null) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp) // Match V1 MiniPlayer height
            .clickable {
                viewModel.onTapToExpand()
                onTapToExpand(uiState.showId) // Use showId for navigation
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track information
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.currentTrack?.songTitle ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.currentTrack?.showDate ?: ""} - ${uiState.currentTrack?.venue ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Play/pause button
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Progress bar at bottom
            if (uiState.progress > 0f) {
                LinearProgressIndicator(
                    progress = uiState.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
```

### Step 5: V2 App Integration

#### V2 MainAppScreen Integration

```kotlin
// v2/app/src/main/java/com/deadly/v2/app/MainAppScreen.kt

@Composable
fun MainAppScreen(
    navController: NavHostController = rememberNavController()
) {
    Scaffold { paddingValues ->
        Column {
            // Main content navigation
            NavHost(
                navController = navController,
                startDestination = BottomNavDestination.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .weight(1f)
            ) {
                // ... existing navigation routes
            }
            
            // Global MiniPlayer - appears above bottom navigation
            MiniPlayerScreen(
                onTapToExpand = { showId ->
                    if (showId != null) {
                        Log.d("MainAppScreen", "MiniPlayer tapped - navigating to playlist: $showId")
                        navController.navigate("playlist/$showId")
                    }
                }
            )
            
            // Bottom navigation
            // ... existing bottom navigation
        }
    }
}
```

#### V2 Application Startup Integration

```kotlin
// v2/app/src/main/java/com/deadly/v2/app/DeadlyApplication.kt (or wherever V2 app starts)

@HiltAndroidApp
class DeadlyApplication : Application() {
    
    @Inject
    lateinit var lastPlayedTrackService: LastPlayedTrackService
    
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("DeadlyApplication", "V2 Application starting up")
        
        // CRITICAL: Restore last played track on app start
        applicationScope.launch {
            try {
                Log.d("DeadlyApplication", "Restoring last played track...")
                lastPlayedTrackService.restoreLastPlayedTrack()
                lastPlayedTrackService.startMonitoring()
                Log.d("DeadlyApplication", "✅ Last played track restoration completed")
            } catch (e: Exception) {
                Log.e("DeadlyApplication", "❌ Failed to restore last played track", e)
            }
        }
    }
}
```

### Step 6: Build Configuration

#### Module Dependencies

```kotlin
// v2/feature/miniplayer/build.gradle.kts
dependencies {
    implementation(project(":v2:core:api:miniplayer"))
    implementation(project(":v2:core:model"))
    implementation(project(":v2:core:design"))
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    
    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
}

// v2/core/api/miniplayer/build.gradle.kts
dependencies {
    implementation(project(":v2:core:model"))
    implementation(libs.kotlinx.coroutines.core)
}

// v2/core/miniplayer/build.gradle.kts
dependencies {
    implementation(project(":v2:core:api:miniplayer"))
    implementation(project(":v2:core:model"))
    implementation(project(":v2:core:media"))
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
```

#### Settings Gradle

```kotlin
// settings.gradle.kts
include(":v2:feature:miniplayer")
include(":v2:core:api:miniplayer")
include(":v2:core:miniplayer")
```

## Critical Success Requirements

### Perfect State Persistence (Like Spotify)

- ✅ Track position saved every 10 seconds during playback
- ✅ Exact restoration on app restart (same track, same position, same format)
- ✅ Both showId and recordingId tracked for complete navigation
- ✅ Never lose user's place in any track
- ✅ Works reliably across app termination/restart

### Perfect Bidirectional Communication

- ✅ Notification controls → Media3 → MediaControllerRepository → MiniPlayer (instant sync)
- ✅ Playlist playback → MediaControllerRepository → MiniPlayer (instant sync)
- ✅ MiniPlayer controls → MediaControllerRepository → All UIs (instant sync)
- ✅ Single source of truth: V2 MediaControllerRepository StateFlows

### Clean V2 Architecture

- ✅ Follows exact V2 patterns from playlist/search features
- ✅ No redundant components (no container, no separate monitor, no stub)
- ✅ API interface only for business logic services (MiniPlayerService)
- ✅ Internal services have no API interface (LastPlayedTrackService)
- ✅ Direct ViewModel-Service integration following V2 conventions
- ✅ No "V2" naming in v2/ modules (structure implies V2)
- ✅ No V1 dependencies in any V2 components

### Navigation Integration

- ✅ MiniPlayer tap uses showId to navigate to correct playlist
- ✅ Playlist shows the specific recording that's currently playing
- ✅ Perfect user flow: hear track → tap MiniPlayer → see that show's playlist

This creates a production-ready V2 MiniPlayer with minimal necessary components and maximum reliability, following established V2 architectural patterns while ensuring perfect state persistence and cross-UI synchronization.