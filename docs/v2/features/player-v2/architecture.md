# PlayerV2 Architecture Reference

## Overview

This document provides a comprehensive architectural reference for PlayerV2, documenting the component hierarchy, state flows, and technical implementation patterns that make up the V2 player system.

**Architecture Type**: V2 UI-first with service composition  
**Design Pattern**: Clean Architecture with domain-driven service interfaces  
**State Management**: Reactive StateFlow with single source of truth

## Component Hierarchy

### 1. Top-Level Architecture

```
PlayerV2Screen
├── PlayerV2ViewModel (State Coordination)
│   └── PlayerV2Service (Domain Interface)
│       └── PlayerV2ServiceStub (Mock Implementation)
├── UI Components (Presentation Layer)
│   ├── PlayerV2TopBar
│   ├── PlayerV2CoverArt  
│   ├── PlayerV2TrackInfoRow
│   ├── PlayerV2ProgressControl
│   ├── PlayerV2EnhancedControls
│   ├── PlayerV2SecondaryControls
│   └── PlayerV2MaterialPanels
├── Modal Components (Interaction Layer)
│   ├── TrackActionsBottomSheet
│   ├── ConnectBottomSheet
│   └── QueueBottomSheet
└── Overlay Components (Context Layer)
    ├── PlayerV2MiniPlayer
    └── DebugActivator (when enabled)
```

### 2. Detailed Component Breakdown

#### Core Screen Components

```kotlin
// Main Container (LazyColumn Architecture)
PlayerV2Screen(
    recordingId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToPlaylist: (String?) -> Unit,
    viewModel: PlayerV2ViewModel,
    settingsViewModel: SettingsViewModel
) {
    
    // Scrollable Content Items
    LazyColumn {
        item {
            // Gradient Container
            Box(modifier = Modifier.background(recordingGradient)) {
                Column {
                    PlayerV2TopBar(...)           // Navigation & context
                    PlayerV2CoverArt(...)         // Large album display
                    PlayerV2TrackInfoRow(...)     // Metadata & actions
                    PlayerV2ProgressControl(...)  // Seek & time display
                    PlayerV2EnhancedControls(...) // Media controls
                }
            }
        }
        item { PlayerV2SecondaryControls(...) }  // Share, queue, connect
        item { PlayerV2MaterialPanels(...) }     // Extended content
    }
    
    // Modal Overlays (Conditional)
    if (showTrackActions) TrackActionsBottomSheet(...)
    if (showConnect) ConnectBottomSheet(...)
    if (showQueue) QueueBottomSheet(...)
    
    // Context Overlays (Conditional)  
    if (showMiniPlayer) PlayerV2MiniPlayer(...)
    if (debugEnabled) DebugActivator(...)
}
```

#### Component Responsibilities

| Component | Lines | Responsibility | Input | Output |
|-----------|-------|----------------|--------|---------|
| **PlayerV2TopBar** | 43 | Navigation & context display | `contextText`, navigation callbacks | User navigation actions |
| **PlayerV2CoverArt** | 30 | Album artwork display | `modifier` | Visual album representation |
| **PlayerV2TrackInfoRow** | 53 | Track metadata & playlist actions | `trackTitle`, `showDate`, `venue`, `onAddToPlaylist` | Add to playlist action |
| **PlayerV2ProgressControl** | 47 | Seek control & time display | `currentTime`, `totalTime`, `progress`, `onSeek` | Seek position changes |
| **PlayerV2EnhancedControls** | 104 | Primary media controls | Play state, shuffle, repeat, control callbacks | Media control actions |
| **PlayerV2SecondaryControls** | 51 | Secondary actions | Action callbacks | Share, queue, connect actions |
| **PlayerV2MaterialPanels** | 74 | Extended content display | Static content | User content consumption |

#### Modal Component Architecture

```kotlin
// Bottom Sheet Pattern
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetComponent(
    onDismiss: () -> Unit,
    // Component-specific parameters
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Component-specific content
        Column(modifier = Modifier.padding(16.dp)) {
            // Content implementation
        }
    }
}

// Applied to:
TrackActionsBottomSheet    // Track metadata + action buttons
ConnectBottomSheet         // Connection features (coming soon)
QueueBottomSheet          // Queue display with drag handle
```

## State Flow Architecture

### 1. Data Flow Diagram

```
User Interaction
       ↓
PlayerV2ViewModel Commands
       ↓
PlayerV2Service Operations
       ↓
PlayerV2State Updates
       ↓
StateFlow Emissions
       ↓
UI State Transformation
       ↓
Composable Recomposition
       ↓
Updated UI Display
```

### 2. State Management Layers

#### Layer 1: Domain State (PlayerV2Service)

```kotlin
interface PlayerV2Service {
    val playerState: Flow<PlayerV2State>
    
    // State modifying operations
    suspend fun loadRecording(recordingId: String)
    suspend fun togglePlayPause()
    suspend fun skipToPrevious() 
    suspend fun skipToNext()
    suspend fun seekToPosition(position: Float)
}

// Domain state model
data class PlayerV2State(
    val isLoading: Boolean = false,
    val playbackState: PlayerV2PlaybackState = PlayerV2PlaybackState.IDLE,
    val currentTrack: PlayerV2Track? = null,
    val queue: PlayerV2Queue? = null
) {
    val isPlaying: Boolean get() = playbackState == PlayerV2PlaybackState.PLAYING
    
    fun updatePlaybackState(newState: PlayerV2PlaybackState): PlayerV2State {
        return copy(playbackState = newState)
    }
}
```

#### Layer 2: UI State Transformation (PlayerV2ViewModel)

```kotlin
class PlayerV2ViewModel @Inject constructor(
    private val playerV2Service: PlayerV2Service
) : ViewModel() {
    
    // UI-specific state
    private val _uiState = MutableStateFlow(PlayerV2UiState())
    val uiState: StateFlow<PlayerV2UiState> = _uiState.asStateFlow()
    
    // State transformation pipeline
    private fun observeServiceState() {
        viewModelScope.launch {
            playerV2Service.playerState
                .catch { exception -> handleError(exception) }
                .collect { serviceState -> 
                    transformToUiState(serviceState)
                }
        }
    }
    
    private suspend fun transformToUiState(serviceState: PlayerV2State) {
        // Transform domain state to UI-specific requirements
        val trackInfo = playerV2Service.getCurrentTrackInfo()
        val progressInfo = playerV2Service.getProgressInfo()
        
        _uiState.value = PlayerV2UiState(
            isLoading = serviceState.isLoading,
            isPlaying = serviceState.isPlaying,
            trackInfo = trackInfo,          // UI-formatted data
            progressInfo = progressInfo,    // UI-formatted data
            canPlay = playerV2Service.isReady(),
            error = null
        )
    }
}

// UI state model (presentation-focused)
data class PlayerV2UiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val trackInfo: TrackDisplayInfo? = null,
    val progressInfo: ProgressDisplayInfo? = null,
    val canPlay: Boolean = false,
    val error: String? = null
)
```

#### Layer 3: UI Consumption (Composables)

```kotlin
@Composable
fun PlayerV2Screen(viewModel: PlayerV2ViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI components consume transformed state
    PlayerV2TrackInfoRow(
        trackTitle = uiState.trackInfo?.trackTitle ?: "Scarlet Begonias",
        showDate = uiState.trackInfo?.showDate ?: "May 8, 1977",
        venue = uiState.trackInfo?.venue ?: "Barton Hall, Cornell University",
        onAddToPlaylist = { /* TODO */ }
    )
    
    PlayerV2ProgressControl(
        currentTime = uiState.progressInfo?.currentTime ?: "2:34",
        totalTime = uiState.progressInfo?.totalTime ?: "8:15", 
        progress = uiState.progressInfo?.progress ?: 0.31f,
        onSeek = viewModel::onSeek
    )
    
    PlayerV2EnhancedControls(
        isPlaying = uiState.isPlaying,
        onPlayPause = viewModel::onPlayPauseClicked,
        onPrevious = viewModel::onPreviousClicked,
        onNext = viewModel::onNextClicked
    )
}
```

### 3. Command Flow Pattern

#### User Action → Service Command Flow

```kotlin
// User clicks play button
PlayerV2EnhancedControls(
    onPlayPause = viewModel::onPlayPauseClicked  // UI callback
)

// ViewModel processes command
fun onPlayPauseClicked() {
    viewModelScope.launch {
        try {
            playerV2Service.togglePlayPause()      // Service command
        } catch (e: Exception) {
            handleError(e)                         // Error handling
        }
    }
}

// Service updates state
override suspend fun togglePlayPause() {
    isPlaying = !isPlaying
    _playerState.value = _playerState.value.updatePlaybackState(
        if (isPlaying) PlayerV2PlaybackState.PLAYING 
        else PlayerV2PlaybackState.PAUSED
    )
}

// StateFlow emits update
playerState: StateFlow<PlayerV2State>

// ViewModel transforms and emits UI update  
_uiState.value = _uiState.value.copy(isPlaying = serviceState.isPlaying)

// UI recomposes with new state
```

## Recording-Based Theming System

### 1. Color Generation Architecture

```kotlin
// Grateful Dead color palette
private val GradientColors = listOf(
    DeadGreen,   // Forest green
    DeadGold,    // Golden yellow
    DeadRed,     // Crimson red  
    DeadBlue,    // Royal blue
    DeadPurple   // Blue violet
)

// Hash-based selection for consistency
private fun recordingIdToColor(recordingId: String?): Color {
    if (recordingId.isNullOrEmpty()) return DeadRed
    
    val hash = recordingId.hashCode()
    val index = kotlin.math.abs(hash) % GradientColors.size
    return GradientColors[index]
}

// Color stack generation for UI components
@Composable
private fun getRecordingColorStack(recordingId: String?): List<Color> {
    val baseColor = recordingIdToColor(recordingId)
    val background = MaterialTheme.colorScheme.background
    
    return listOf(
        lerp(background, baseColor, 0.8f),  // Strong blend
        lerp(background, baseColor, 0.4f),  // Medium blend
        lerp(background, baseColor, 0.1f),  // Faint blend
        background,                         // Background
        background                          // Background
    )
}
```

### 2. Gradient Application Pattern

```kotlin
// Main screen gradient (scrollable content)
@Composable
private fun createRecordingGradient(recordingId: String?): Brush {
    val colors = getRecordingColorStack(recordingId)
    
    return Brush.verticalGradient(
        0f to colors[0],      // Strong color at top
        0.3f to colors[1],    // Medium color at 30%
        0.6f to colors[2],    // Faint color at 60%
        0.8f to colors[3],    // Background at 80%
        1f to colors[4]       // Full background at bottom
    )
}

// Usage in LazyColumn item
item {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(createRecordingGradient(recordingId))
    ) {
        // Player components
    }
}
```

### 3. Mini-Player Color Consistency

```kotlin
@Composable
private fun PlayerV2MiniPlayer(recordingId: String?) {
    val colors = getRecordingColorStack(recordingId)
    val backgroundColor = colors[1] // Medium color for consistency
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        // Mini-player content
    }
}
```

## Mini-Player Architecture

### 1. Scroll-Based Visibility System

```kotlin
// Scroll state monitoring
val scrollState = rememberLazyListState()

// Visibility calculation
val showMiniPlayer by remember {
    derivedStateOf {
        scrollState.firstVisibleItemIndex > 0 || 
        (scrollState.firstVisibleItemIndex == 0 && 
         scrollState.firstVisibleItemScrollOffset > 1200)
    }
}

// Conditional rendering
if (showMiniPlayer && uiState.trackInfo != null) {
    PlayerV2MiniPlayer(
        uiState = uiState,
        recordingId = recordingId,
        onPlayPause = viewModel::onPlayPauseClicked,
        onTapToExpand = {
            coroutineScope.launch {
                scrollState.animateScrollToItem(0)
            }
        }
    )
}
```

### 2. Mini-Player Component Structure

```kotlin
@Composable
private fun PlayerV2MiniPlayer(
    uiState: PlayerV2UiState,
    recordingId: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.height(72.dp)) {
        Column {
            // Main content row
            Row {
                // Track info (expandable click area)
                Column(modifier = Modifier.clickable { onTapToExpand() }) {
                    Text(text = uiState.trackInfo?.trackTitle ?: "Unknown Track")
                    Text(text = uiState.trackInfo?.showDate ?: "Unknown Date")
                }
                
                // Play/pause button (separate interaction)
                IconButton(onClick = onPlayPause) {
                    Icon(
                        painter = if (uiState.isPlaying) 
                            IconResources.PlayerControls.Pause()
                        else 
                            IconResources.PlayerControls.Play()
                    )
                }
            }
            
            // Progress bar (no thumb, read-only)
            LinearProgressIndicator(
                progress = uiState.progressInfo?.progress ?: 0f
            )
        }
    }
}
```

## Feature Flag Integration Architecture

### 1. Settings-Based Routing

```kotlin
// App-level navigation routing
@Composable
fun DeadArchiveNavigation(settings: AppSettings) {
    NavHost {
        playerScreen(
            onNavigateBack = { navController.popBackStack() },
            navController = navController,
            usePlayerV2 = settings.usePlayerV2  // Feature flag
        )
    }
}

// Feature module routing
fun NavGraphBuilder.playerScreen(usePlayerV2: Boolean) {
    composable("player/{recordingId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId")
        
        if (usePlayerV2) {
            PlayerV2Screen(recordingId = recordingId, ...)
        } else {
            PlayerScreen(recordingId = recordingId, ...)
        }
    }
}
```

### 2. Settings UI Integration

```kotlin
// Settings screen toggle
@Composable  
fun SettingsScreen() {
    val settings by viewModel.settings.collectAsState()
    
    SettingsSection(title = "Experimental Features") {
        SettingsItem(
            title = "PlayerV2",
            description = "New music player interface",
            trailing = {
                Switch(
                    checked = settings.usePlayerV2,
                    onCheckedChange = { viewModel.updateUsePlayerV2(it) }
                )
            }
        )
    }
}
```

### 3. Dependency Injection Architecture

```kotlin
// Current implementation (stub-only)
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerV2Module {
    
    @Binds
    @Singleton
    abstract fun bindPlayerV2Service(
        playerV2ServiceStub: PlayerV2ServiceStub
    ): PlayerV2Service
}

// Future implementation (feature flag-based)
@Module
@InstallIn(SingletonComponent::class)
class PlayerV2Module {
    
    @Provides
    @Singleton
    fun providePlayerV2Service(
        @Named("stub") stub: PlayerV2ServiceStub,
        @Named("real") real: PlayerV2ServiceImpl,
        settingsRepository: SettingsRepository
    ): PlayerV2Service {
        return if (settingsRepository.getSettings().usePlayerV2Real) {
            real
        } else {
            stub
        }
    }
}
```

## Debug System Integration

### 1. Debug Activation Pattern

```kotlin
// Conditional debug UI based on settings
@Composable
fun PlayerV2Screen(settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.settings.collectAsState()
    
    // Debug data collection (only when enabled)
    val debugData = if (settings.showDebugInfo) {
        collectPlayerV2DebugData(uiState, recordingId)
    } else {
        null
    }
    
    // Debug activator (conditional rendering)
    if (settings.showDebugInfo && debugData != null) {
        DebugActivator(
            isVisible = true,
            onClick = { showDebugPanel = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
```

### 2. Debug Data Structure

```kotlin
@Composable
private fun collectPlayerV2DebugData(
    uiState: PlayerV2UiState,
    recordingId: String?
): DebugData {
    return DebugData(
        screenName = "PlayerV2Screen",
        sections = listOf(
            DebugSection(
                title = "PlayerV2 State",
                items = listOf(
                    DebugItem.KeyValue("Recording ID", recordingId ?: "None"),
                    DebugItem.KeyValue("Is Playing", uiState.isPlaying.toString()),
                    DebugItem.KeyValue("Is Loading", uiState.isLoading.toString()),
                    DebugItem.KeyValue("Track Title", uiState.trackInfo?.trackTitle ?: "None"),
                    DebugItem.KeyValue("Progress", "${(uiState.progressInfo?.progress ?: 0f) * 100}%")
                )
            ),
            DebugSection(
                title = "Service Status", 
                items = listOf(
                    DebugItem.KeyValue("Service Type", "PlayerV2ServiceStub"),
                    DebugItem.KeyValue("Ready State", "Mock Ready"),
                    DebugItem.KeyValue("Error State", uiState.error ?: "None")
                )
            )
        )
    )
}
```

## Performance Optimization Patterns

### 1. LazyColumn Performance

```kotlin
// Efficient scrolling with item spacing
LazyColumn(
    state = scrollState,
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(0.dp)
) {
    // Large gradient item (expensive background)
    item {
        Box(modifier = Modifier.background(createRecordingGradient(recordingId))) {
            // Component composition
        }
    }
    
    // Lightweight secondary items
    item { PlayerV2SecondaryControls(...) }
    item { PlayerV2MaterialPanels(...) }
}
```

### 2. State Flow Performance

```kotlin
// Single StateFlow source with proper scoping
class PlayerV2ViewModel @Inject constructor(
    private val playerV2Service: PlayerV2Service
) : ViewModel() {
    
    // Efficient state management
    private val _uiState = MutableStateFlow(PlayerV2UiState())
    val uiState: StateFlow<PlayerV2UiState> = _uiState.asStateFlow()
    
    // Scoped collection with error handling
    private fun observeServiceState() {
        viewModelScope.launch {
            playerV2Service.playerState
                .catch { exception -> handleError(exception) }
                .collect { serviceState -> updateUiStateFromService(serviceState) }
        }
    }
    
    // Cleanup on ViewModel disposal
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            playerV2Service.cleanup()
        }
    }
}
```

### 3. Gradient Performance Optimization

```kotlin
// Color generation cached per recording
@Composable
private fun getRecordingColorStack(recordingId: String?): List<Color> {
    return remember(recordingId) {
        val baseColor = recordingIdToColor(recordingId)
        val background = MaterialTheme.colorScheme.background
        
        listOf(
            lerp(background, baseColor, 0.8f),
            lerp(background, baseColor, 0.4f),
            lerp(background, baseColor, 0.1f),
            background,
            background
        )
    }
}

// Gradient brush generation cached
@Composable
private fun createRecordingGradient(recordingId: String?): Brush {
    val colors = getRecordingColorStack(recordingId)
    
    return remember(recordingId) {
        Brush.verticalGradient(
            0f to colors[0],
            0.3f to colors[1], 
            0.6f to colors[2],
            0.8f to colors[3],
            1f to colors[4]
        )
    }
}
```

## Error Handling Architecture

### 1. Service-Level Error Handling

```kotlin
// PlayerV2ViewModel command error handling
fun onPlayPauseClicked() {
    viewModelScope.launch {
        try {
            playerV2Service.togglePlayPause()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle play/pause", e)
            _uiState.value = _uiState.value.copy(
                error = "Playback error: ${e.message}"
            )
        }
    }
}
```

### 2. State Flow Error Handling

```kotlin
// Reactive state observation with error recovery
private fun observeServiceState() {
    viewModelScope.launch {
        playerV2Service.playerState
            .catch { exception ->
                Log.e(TAG, "Error observing player state", exception)
                _uiState.value = _uiState.value.copy(
                    error = "Playback error: ${exception.message}"
                )
            }
            .collect { serviceState ->
                updateUiStateFromService(serviceState)
            }
    }
}
```

### 3. UI Error Display

```kotlin
// Error state in UI
@Composable
fun PlayerV2Screen() {
    val uiState by viewModel.uiState.collectAsState()
    
    // Error handling in UI
    uiState.error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Show snackbar or error dialog
            Log.e("PlayerV2Screen", "UI Error: $errorMessage")
        }
    }
}
```

## Future Integration Architecture

### 1. V2 Service Wrapper Pattern

```kotlin
// Future MediaV2Service integration
interface MediaV2Service {
    val playbackState: Flow<MediaPlaybackState>
    suspend fun play()
    suspend fun pause() 
    suspend fun seekTo(position: Long)
}

// Future QueueV2Service integration  
interface QueueV2Service {
    val currentQueue: Flow<List<Track>>
    val currentIndex: Flow<Int>
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun setQueue(tracks: List<Track>)
}

// PlayerV2ServiceImpl coordination
class PlayerV2ServiceImpl @Inject constructor(
    private val mediaV2Service: MediaV2Service,
    private val queueV2Service: QueueV2Service,
    private val downloadV2Service: DownloadV2Service,
    private val libraryV2Service: LibraryV2Service
) : PlayerV2Service {
    
    override suspend fun togglePlayPause() {
        if (isPlaying) {
            mediaV2Service.pause()
        } else {
            mediaV2Service.play()
        }
    }
}
```

### 2. Production Deployment Architecture

```kotlin
// Feature flag-based service binding
@Module
@InstallIn(SingletonComponent::class)  
class PlayerV2ProductionModule {
    
    @Provides
    @Singleton
    fun providePlayerV2Service(
        settingsRepository: SettingsRepository,
        stub: PlayerV2ServiceStub,
        impl: PlayerV2ServiceImpl
    ): PlayerV2Service {
        return if (settingsRepository.getUsePlayerV2Real()) {
            impl
        } else {
            stub
        }
    }
}
```

---

**Architecture Status**: Week 2 Complete - UI and Stub Service Architecture  
**Next Phase**: V2 Service Wrapper Integration  
**Design Pattern**: Clean Architecture with V2 Service Composition  
**Performance**: Optimized for smooth scrolling and responsive interactions

This architecture successfully demonstrates the V2 pattern principles while delivering a professional music player experience with maintainable, testable, and scalable code structure.