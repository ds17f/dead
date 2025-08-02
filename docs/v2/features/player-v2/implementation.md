# PlayerV2 Implementation Guide

## Overview

This document provides a comprehensive technical guide to the PlayerV2 implementation, covering the actual code structure, architectural patterns, and development approaches used to build the second V2 feature in the Dead Archive app.

**Implementation Status**: Week 2 Complete - Professional UI with comprehensive stub service integration  
**Architecture Pattern**: V2 UI-first development with service composition  
**File Count**: 4 core files, 1,612 total lines of implementation code

## File Structure & Organization

### Core Implementation Files

```
feature/player/src/main/java/com/deadarchive/feature/player/
├── PlayerV2Screen.kt                 # 1,173 lines - Complete UI implementation
├── PlayerV2ViewModel.kt              # 184 lines - State management & coordination  
├── service/
│   ├── PlayerV2Service.kt            # 217 lines - Service interface definition
│   └── PlayerV2ServiceStub.kt        # 255 lines - Comprehensive stub implementation
└── di/PlayerV2Module.kt              # 24 lines - Hilt dependency injection
```

### Supporting Infrastructure

```
core/model/src/main/java/com/deadarchive/core/model/
├── PlayerV2State.kt                  # Domain state model
├── PlayerV2RepeatMode.kt             # Repeat mode enumeration
├── PlayerV2Track.kt                  # Track domain model
└── PlayerV2Queue.kt                  # Queue domain model

core/settings-api/src/main/java/com/deadarchive/core/settings/api/model/
└── AppSettings.kt                    # Feature flag: usePlayerV2

app/src/main/java/com/deadarchive/app/
└── DeadArchiveNavigation.kt          # Feature flag routing integration
```

## Component Architecture

### 1. PlayerV2Screen.kt - Main UI Implementation

**Architecture**: LazyColumn with scrollable gradient and component composition

#### Key Components Breakdown

```kotlin
@Composable
fun PlayerV2Screen(
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylist: (String?) -> Unit = {},
    viewModel: PlayerV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
)
```

**Primary Components** (lines 315-1172):

1. **PlayerV2TopBar** (315-358)
   - Transparent navigation with Material3 theming
   - Context text display ("Playing from Show")
   - Back button and 3-dot menu actions

2. **PlayerV2CoverArt** (363-393)  
   - Large 450dp section with square aspect ratio
   - Material3 Card with proper elevation
   - Placeholder icon integration

3. **PlayerV2TrackInfoRow** (398-451)
   - Track title, show date, venue display
   - Add to playlist integration
   - Material3 typography hierarchy

4. **PlayerV2ProgressControl** (456-503)
   - Full-width progress slider
   - Time display (current/total)
   - Seek functionality with Material3 styling

5. **PlayerV2EnhancedControls** (508-612)
   - 72dp FAB-style play/pause button
   - 56dp previous/next controls  
   - Shuffle/repeat with smart layout

6. **PlayerV2SecondaryControls** (617-668)
   - Connect, share, queue actions
   - Proper spacing and Material3 integration

7. **PlayerV2MaterialPanels** (673-747)
   - Extended content (venue, lyrics, credits, similar shows)
   - ElevatedCard Material3 design
   - Rich content placeholders

#### Bottom Sheet Implementation

**TrackActionsBottomSheet** (752-854):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackActionsBottomSheet(
    trackTitle: String,
    showDate: String, 
    venue: String,
    onDismiss: () -> Unit,
    // Action handlers
)
```

- LibraryV2-style track card design
- Share, playlist, download, and more options
- Material3 ModalBottomSheet implementation

**QueueBottomSheet** (896-961):
- Drag handle with proper Material3 styling
- Queue header with track count
- LazyColumn list of queue items with current track highlighting

#### Mini-Player Implementation

**PlayerV2MiniPlayer** (1089-1172):
```kotlin
@Composable
private fun PlayerV2MiniPlayer(
    uiState: PlayerV2UiState,
    recordingId: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Key Features**:
- Recording-based color consistency using `getRecordingColorStack()`
- Click-to-expand functionality with smooth scrolling
- Progress bar integration without thumb control
- Proper Material3 Card elevation and styling

#### Recording-Based Gradient System

**Color Generation** (45-99):
```kotlin
private val GradientColors = listOf(DeadGreen, DeadGold, DeadRed, DeadBlue, DeadPurple)

private fun recordingIdToColor(recordingId: String?): Color {
    val hash = recordingId.hashCode()
    val index = kotlin.math.abs(hash) % GradientColors.size
    return GradientColors[index]
}

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
```

**Impact**: Consistent visual identity per recording across all UI components

### 2. PlayerV2ViewModel.kt - State Management

**Architecture**: Clean ViewModel facade coordinating with single service

#### Core Implementation

```kotlin
@HiltViewModel
class PlayerV2ViewModel @Inject constructor(
    private val playerV2Service: PlayerV2Service
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerV2UiState())
    val uiState: StateFlow<PlayerV2UiState> = _uiState.asStateFlow()
    
    // UI command handlers discovered through component needs
    fun onPlayPauseClicked() { /* ... */ }
    fun onPreviousClicked() { /* ... */ }  
    fun onNextClicked() { /* ... */ }
    fun onSeek(position: Float) { /* ... */ }
}
```

#### State Management Pattern

**Service State Observation** (122-135):
```kotlin
private fun observeServiceState() {
    viewModelScope.launch {
        playerV2Service.playerState
            .catch { exception ->
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

**UI State Transformation** (137-152):
```kotlin
private suspend fun updateUiStateFromService(serviceState: PlayerV2State) {
    val trackInfo = playerV2Service.getCurrentTrackInfo()
    val progressInfo = playerV2Service.getProgressInfo()
    
    _uiState.value = _uiState.value.copy(
        isLoading = serviceState.isLoading,
        isPlaying = serviceState.isPlaying,
        trackInfo = trackInfo,
        progressInfo = progressInfo,
        canPlay = playerV2Service.isReady(),
        error = null
    )
}
```

#### UI State Model

```kotlin
data class PlayerV2UiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val trackInfo: TrackDisplayInfo? = null,
    val progressInfo: ProgressDisplayInfo? = null,
    val canPlay: Boolean = false,
    val error: String? = null
)
```

**Design Impact**: Clear separation between service domain state and UI presentation state

### 3. PlayerV2Service.kt - Service Interface

**Architecture**: UI-discovered service interface with comprehensive method coverage

#### Interface Design Philosophy

```kotlin
/**
 * PlayerV2Service - Service interface discovered through UI-first development
 * 
 * Domain Requirements Discovered from UI Components:
 * 
 * From PlayerV2TrackInfo component:
 * - Need current track title with proper display formatting
 * - Need recording name and identification
 * - Need show date and venue information formatted for display
 * 
 * From PlayerV2ProgressBar component:
 * - Need current playback position and total duration
 * - Need formatted time strings (MM:SS format)
 * - Need progress percentage calculation
 * - Need seek/scrub functionality
 * 
 * From PlayerV2Controls component:
 * - Need play/pause state management
 * - Need track navigation (previous/next)
 * - Need control availability states
 */
```

#### Core Service Operations

**Reactive State Management**:
```kotlin
interface PlayerV2Service {
    val playerState: Flow<PlayerV2State>
    
    // Track Operations (discovered from PlayerV2TrackInfo)
    suspend fun loadRecording(recordingId: String)
    suspend fun getCurrentTrackInfo(): TrackDisplayInfo?
    
    // Playback Control (discovered from PlayerV2Controls)
    suspend fun togglePlayPause()
    suspend fun skipToPrevious()
    suspend fun skipToNext()
    
    // Progress Operations (discovered from PlayerV2ProgressBar)
    suspend fun seekToPosition(position: Float)
    suspend fun getProgressInfo(): ProgressDisplayInfo?
}
```

#### Display Data Models

**TrackDisplayInfo** (147-153):
```kotlin
data class TrackDisplayInfo(
    val trackTitle: String,
    val recordingName: String,
    val showDate: String,
    val venue: String
)
```

**ProgressDisplayInfo** (158-163):
```kotlin
data class ProgressDisplayInfo(
    val currentTime: String,        // Formatted as "MM:SS"
    val totalTime: String,          // Formatted as "MM:SS"  
    val progress: Float             // 0.0 to 1.0
)
```

**Extended Professional UI Support** (169-217):
- `PlayingContextInfo` - Top navigation context
- `PlayerV2ControlState` - Control button states
- `ExtendedTrackInfo` - Professional track display
- `VenueInfo` - Extended content panels
- `CreditsInfo` - Performer and recording details

### 4. PlayerV2ServiceStub.kt - Comprehensive Stub Implementation

**Architecture**: Production-quality stub enabling complete UI development

#### Stub Implementation Philosophy

```kotlin
/**
 * PlayerV2ServiceStub - Stub implementation for UI-first development
 * 
 * This implementation provides mock data and basic state management
 * to enable UI development and testing. Following V2 architecture,
 * this stub lets us build and test the UI before integrating with
 * the full V1 service ecosystem.
 */
@Singleton
class PlayerV2ServiceStub @Inject constructor() : PlayerV2Service
```

#### Rich Mock Data Implementation

**State Management** (35-43):
```kotlin
private val _playerState = MutableStateFlow(createMockPlayerState())
override val playerState: StateFlow<PlayerV2State> = _playerState.asStateFlow()

private var isPlaying = false
private var currentPosition = 0.3f 
private var shuffleEnabled = false
private var repeatMode = PlayerV2RepeatMode.NONE
private var currentRecordingInfo: MockRecordingInfo? = null
```

**Recording Loading** (45-54):
```kotlin
override suspend fun loadRecording(recordingId: String) {
    currentRecordingInfo = createMockRecordingInfo(recordingId)
    val mockState = createMockPlayerState(recordingId)
    _playerState.value = mockState
}
```

**Realistic Interactions** (67-78):
```kotlin
override suspend fun togglePlayPause() {
    isPlaying = !isPlaying
    _playerState.value = _playerState.value.updatePlaybackState(
        if (isPlaying) PlayerV2PlaybackState.PLAYING 
        else PlayerV2PlaybackState.PAUSED
    )
}
```

#### Extended Content Support

**Venue Information** (162-172):
```kotlin
override suspend fun getVenueInfo(): VenueInfo? {
    return VenueInfo(
        name = "Barton Hall",
        description = "Barton Hall at Cornell University...",
        capacity = "8,500",
        notableShows = listOf(
            "May 8, 1977 - The legendary Cornell show",
            "May 7, 1980 - Another classic performance"
        )
    )
}
```

**Lyrics Integration** (174-182):
```kotlin
override suspend fun getLyrics(): String? {
    return """Scarlet begonias tucked into her curls
I knew right away she was not like other girls..."""
}
```

**Impact**: Full UI functionality without waiting for real service integration

### 5. PlayerV2Module.kt - Dependency Injection

**Architecture**: Simple Hilt module binding stub to interface

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerV2Module {
    
    @Binds
    @Singleton
    abstract fun bindPlayerV2Service(
        playerV2ServiceStub: PlayerV2ServiceStub
    ): PlayerV2Service
}
```

**Future Implementation**: Feature flag-based binding for real service integration

## Development Patterns

### 1. UI-First Development Methodology

**Process**:
1. Build UI components with hardcoded data
2. Identify data and operation requirements  
3. Extract service interface methods
4. Implement stub with realistic behavior
5. Connect ViewModel to service interface

**Example**: PlayerV2ProgressControl component needed time formatting, leading to:
```kotlin
// UI requirement discovered
Text(text = currentTime, ...)

// Service method discovered
suspend fun getProgressInfo(): ProgressDisplayInfo?

// Stub implementation  
ProgressDisplayInfo(
    currentTime = formatTime((currentPosition * 495).toInt()),
    totalTime = formatTime(495),
    progress = currentPosition
)
```

### 2. Component Composition Pattern

**Principle**: Single responsibility components with clear boundaries

```kotlin
// Main screen composition
LazyColumn {
    item { /* Gradient section with all player components */ }
    item { PlayerV2SecondaryControls(...) }
    item { PlayerV2MaterialPanels(...) }
}

// Individual component
@Composable
private fun PlayerV2TrackInfoRow(
    trackTitle: String,
    showDate: String,
    venue: String,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Benefits**: 
- Testable in isolation
- Clear data dependencies
- Reusable across contexts

### 3. Recording-Based Theming Pattern

**Implementation**:
```kotlin
// Consistent color generation
private fun recordingIdToColor(recordingId: String?): Color

// Usage across components
createRecordingGradient(recordingId)  // Main gradient
getRecordingColorStack(recordingId)   // Mini-player colors
```

**Impact**: Visual consistency across all UI components for same recording

### 4. Feature Flag Integration Pattern

**Navigation Routing**:
```kotlin
// DeadArchiveNavigation.kt
playerScreen(
    onNavigateBack = { navController.popBackStack() },
    navController = navController,
    usePlayerV2 = settings.usePlayerV2
)

// PlayerNavigation.kt  
fun NavGraphBuilder.playerScreen(usePlayerV2: Boolean) {
    composable("player/{recordingId}") {
        if (usePlayerV2) {
            PlayerV2Screen(...)
        } else {
            PlayerScreen(...)
        }
    }
}
```

**Settings Integration**:
```kotlin
// AppSettings.kt
data class AppSettings(
    val usePlayerV2: Boolean = false,
    // ...
)

// SettingsScreen.kt
Switch(
    checked = settings.usePlayerV2,
    onCheckedChange = onUpdateUsePlayerV2
)
```

## Integration Points

### 1. V2 Service Ecosystem Integration

**Ready Integrations**:
- ✅ **DownloadV2Service**: Download status and management
- ✅ **LibraryV2Service**: Library operations and state

**Planned Integration Pattern**:
```kotlin
// Future PlayerV2ServiceImpl
@Singleton  
class PlayerV2ServiceImpl @Inject constructor(
    private val mediaV2Service: MediaV2Service,
    private val downloadV2Service: DownloadV2Service,
    private val libraryV2Service: LibraryV2Service,
    private val queueV2Service: QueueV2Service
) : PlayerV2Service {
    // Coordinate V2 services without direct V1 dependencies
}
```

### 2. Navigation System Integration

**Current Implementation**:
- Feature flag-based routing in `DeadArchiveNavigation.kt`
- Recording ID parameter passing
- Back navigation and deep linking support

**Usage Example**:
```kotlin
// From browse/library to player
navController.navigate("player/$recordingId")

// PlayerV2Screen receives recordingId and loads recording
LaunchedEffect(recordingId) {
    if (recordingId != null) {
        viewModel.loadRecording(recordingId)
    }
}
```

### 3. Debug System Integration

**Implementation**:
```kotlin
// Debug panel integration when settings.showDebugInfo enabled
if (settings.showDebugInfo && debugData != null) {
    DebugActivator(
        isVisible = true,
        onClick = { showDebugPanel = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    )
}
```

**Debug Data Collection**:
```kotlin
@Composable
private fun collectPlayerV2DebugData(
    uiState: PlayerV2UiState,
    recordingId: String?
): DebugData {
    // Ready for future debug information population
}
```

## Testing Approach

### 1. Component Testing Strategy

**UI Component Testing**:
```kotlin
// Test individual components with mock data
@Test
fun playerV2TrackInfoRow_displaysCorrectInformation() {
    composeTestRule.setContent {
        PlayerV2TrackInfoRow(
            trackTitle = "Test Track",
            showDate = "Test Date", 
            venue = "Test Venue",
            onAddToPlaylist = {}
        )
    }
    
    composeTestRule.onNodeWithText("Test Track").assertIsDisplayed()
}
```

### 2. Service Testing Strategy

**Stub Service Testing**:
```kotlin
@Test
fun playerV2ServiceStub_togglePlayPause_updatesState() = runTest {
    val service = PlayerV2ServiceStub()
    
    service.togglePlayPause()
    
    val state = service.playerState.first()
    assertEquals(PlayerV2PlaybackState.PLAYING, state.playbackState)
}
```

### 3. Integration Testing Strategy

**ViewModel Testing**:
```kotlin
@Test
fun playerV2ViewModel_loadRecording_updatesUiState() = runTest {
    val mockService = MockPlayerV2Service()
    val viewModel = PlayerV2ViewModel(mockService)
    
    viewModel.loadRecording("test-recording")
    
    val uiState = viewModel.uiState.first()
    assertNotNull(uiState.trackInfo)
}
```

## Performance Characteristics

### 1. Scroll Performance

**LazyColumn Architecture**:
- Efficient scrolling with large content
- Gradient scrolls naturally with content
- Mini-player visibility based on scroll offset

### 2. State Management Performance

**StateFlow Efficiency**:
```kotlin
// Single StateFlow source with proper scoping
private val _uiState = MutableStateFlow(PlayerV2UiState())
val uiState: StateFlow<PlayerV2UiState> = _uiState.asStateFlow()

// Reactive updates without manual subscription management
private fun observeServiceState() {
    viewModelScope.launch {
        playerV2Service.playerState.collect { serviceState ->
            updateUiStateFromService(serviceState)
        }
    }
}
```

### 3. Memory Management

**Component Lifecycle**:
- Composable components automatically disposed
- ViewModel scoped to navigation lifecycle
- Service state properly cleaned up in `onCleared()`

## Error Handling

### 1. Service Error Handling

```kotlin
override suspend fun togglePlayPause() {
    viewModelScope.launch {
        try {
            playerV2Service.togglePlayPause()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle play/pause", e)
        }
    }
}
```

### 2. State Error Handling

```kotlin
private fun observeServiceState() {
    viewModelScope.launch {
        playerV2Service.playerState
            .catch { exception ->
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

## Next Steps for Production

### 1. V2 Service Wrapper Creation

**Required Services**:
- `MediaV2Service` - Wrap MediaControllerRepository
- `QueueV2Service` - Wrap QueueManager  
- `PlayerV2ServiceImpl` - Coordinate V2 services

### 2. Feature Flag Enhancement

```kotlin
// Future dependency injection with feature flag
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerV2Module {
    
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindPlayerV2ServiceStub(stub: PlayerV2ServiceStub): PlayerV2Service
    
    @Binds
    @Singleton  
    @Named("real")
    abstract fun bindPlayerV2ServiceImpl(impl: PlayerV2ServiceImpl): PlayerV2Service
    
    // Feature flag-based provider
}
```

### 3. Real Media Integration

- Connect to existing MediaControllerRepository through MediaV2Service
- Integrate with QueueManager through QueueV2Service
- Maintain V2 service isolation principles

---

**Implementation Status**: Week 2 Complete - Professional UI with comprehensive stub  
**Next Phase**: V2 Service Integration (Week 3)  
**Total Lines**: 1,612 lines of implementation code  
**Architecture**: Clean V2 patterns with service composition

This implementation demonstrates the power of UI-first V2 development, delivering a world-class music player interface while maintaining clean architectural boundaries and safe deployment practices.