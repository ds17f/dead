# V2 Architecture Holistic Improvement Plan

## Current State Analysis

### Strengths of V2 Architecture
- **Service-Oriented Design**: Clean separation of concerns with focused services
- **Reactive State Management**: StateFlow-based data flows
- **Single Source of Truth**: MediaControllerRepository as central playback coordinator
- **Component Abstraction**: Well-defined service interfaces with stub/real implementations
- **Dependency Injection**: Proper Hilt-based DI with scoped services

### Identified Architectural Patterns & Problems

#### 1. **Duplicated MediaController Integration**
**Problem**: Every service reinvents MediaController state observation
- PlaylistServiceImpl: Custom combine() with MediaController flows  
- PlayerServiceImpl: Direct delegation to MediaController StateFlows
- MiniPlayerServiceImpl: Manual metadata transformation
- Each has different approaches to the same core problem

**Current Files Affected:**
- `/v2/core/playlist/src/main/java/com/deadly/v2/core/playlist/service/PlaylistServiceImpl.kt`
- `/v2/core/player/src/main/java/com/deadly/v2/core/player/service/PlayerServiceImpl.kt`
- `/v2/core/miniplayer/src/main/java/com/deadly/v2/core/miniplayer/service/MiniPlayerServiceImpl.kt`
- `/v2/feature/playlist/src/main/java/com/deadly/v2/feature/playlist/screens/main/models/PlaylistViewModel.kt`

#### 2. **Scattered Playback State Logic**
**Problem**: UI responsiveness logic spread across multiple layers
- Button state logic in individual ViewModels
- Loading states managed inconsistently
- Context-aware behavior (play vs pause) duplicated
- No centralized "playback intent" vs "actual state" separation

**Specific Issues Identified:**
- PlaylistActionRow has custom context-aware button logic
- Each ViewModel implements its own MediaController state combination
- Loading states are handled differently in each feature
- Error handling inconsistencies across playback operations

#### 3. **Inconsistent State Composition Patterns**
**Problem**: Each feature reinvents reactive state combination
- PlaylistViewModel: Custom combine() for isCurrentShowAndRecording
- Different error handling approaches per feature
- Inconsistent loading state patterns

## Proposed Architectural Solutions

### 1. **Centralized Playback State Service**
Create `PlaybackStateService` as single source of truth for all playback-related UI state:

```kotlin
@Singleton
class PlaybackStateService @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) {
    // Unified state combining all MediaController flows
    val playbackState: StateFlow<PlaybackUiState> = combine(
        mediaControllerRepository.isPlaying,
        mediaControllerRepository.currentTrack,
        mediaControllerRepository.currentRecordingId,
        mediaControllerRepository.currentShowId,
        mediaControllerRepository.currentPosition,
        mediaControllerRepository.duration
    ) { isPlaying, track, recordingId, showId, position, duration ->
        PlaybackUiState(
            isPlaying = isPlaying,
            currentRecordingId = recordingId,
            currentShowId = showId,
            currentTrack = track,
            position = position,
            duration = duration
        )
    }
    
    // Context-aware button states
    fun getButtonState(context: PlaybackContext): StateFlow<PlayButtonState> {
        return playbackState.map { state ->
            when {
                loadingContexts.contains(context) -> PlayButtonState.Loading
                state.isContextCurrent(context) && state.isPlaying -> PlayButtonState.Playing
                else -> PlayButtonState.Idle
            }
        }.stateIn(serviceScope, SharingStarted.WhileSubscribed(), PlayButtonState.Idle)
    }
    
    // Optimistic state management
    private val loadingContexts = mutableSetOf<PlaybackContext>()
    
    suspend fun setLoadingState(context: PlaybackContext) {
        loadingContexts.add(context)
        // Emit updated state
    }
    
    suspend fun clearLoadingState(context: PlaybackContext) {
        loadingContexts.remove(context)
        // Emit updated state
    }
}

data class PlaybackUiState(
    // What's currently playing
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentShowId: String? = null,
    val currentRecordingId: String? = null,
    val currentTrackId: String? = null,
    
    // Current track details  
    val currentTrack: MediaMetadata? = null,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
    
    // Playback position
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val progressPercentage: Float = 0f,
    
    // Queue information
    val currentTrackIndex: Int = -1,
    val totalTracks: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    
    // Queue tracks (for mini-player, next/prev, etc.)
    val queueTracks: List<QueueTrack> = emptyList(),
    
    // Loading states
    val isLoadingNewContent: Boolean = false,
    val loadingContext: PlaybackContext? = null
) {
    fun isContextCurrent(context: PlaybackContext): Boolean {
        return when (context) {
            is ShowContext -> currentShowId == context.showId
            is RecordingContext -> currentRecordingId == context.recordingId
            is TrackContext -> currentTrackId == context.trackId
        }
    }
}

data class QueueTrack(
    val trackId: String,
    val title: String,
    val duration: String,
    val isCurrentTrack: Boolean
)

sealed class PlaybackContext {
    data class ShowContext(val showId: String) : PlaybackContext()
    data class RecordingContext(val recordingId: String, val showId: String) : PlaybackContext()
    data class TrackContext(val trackId: String, val recordingId: String) : PlaybackContext()
}

enum class PlayButtonState {
    Idle, Loading, Playing, Paused, Error
}
```

**Benefits**: 
- Single place for all MediaController state logic
- Consistent loading state management
- Reusable context-aware button logic
- Eliminates duplication across services

### 2. **Reactive UI Component System**
Create smart components that automatically handle playback state:

```kotlin
@Composable
fun PlaybackAwareButton(
    context: PlaybackContext,
    onIntent: (PlaybackIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState = LocalPlaybackStateService.current
    val buttonState by playbackState.getButtonState(context).collectAsState()
    
    IconButton(
        onClick = { 
            val intent = when (buttonState) {
                PlayButtonState.Playing -> PlaybackIntent.Pause
                PlayButtonState.Idle -> PlaybackIntent.Play(context)
                PlayButtonState.Loading -> return@IconButton // Ignore clicks while loading
                else -> return@IconButton
            }
            onIntent(intent)
        },
        modifier = modifier
    ) {
        when (buttonState) {
            PlayButtonState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            PlayButtonState.Playing -> Icon(Icons.Filled.Pause, "Pause")
            else -> Icon(Icons.Filled.PlayArrow, "Play")
        }
    }
}

@Composable
fun PlaybackAwareTrackItem(
    track: PlaylistTrackViewModel,
    recordingContext: PlaybackContext.RecordingContext,
    onTrackSelected: (String) -> Unit
) {
    val playbackState = LocalPlaybackStateService.current
    val uiState by playbackState.playbackState.collectAsState()
    
    val isCurrentTrack = uiState.currentTrack?.let { currentTrack ->
        // Track matching logic centralized here
        track.filename == currentTrack.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ||
        currentTrack.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)?.contains(track.filename) == true
    } ?: false
    
    val isPlaying = isCurrentTrack && uiState.isPlaying
    
    // Standard track item UI with automatic state reflection
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrentTrack) Color.Red.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onTrackSelected(track.filename) }
    ) {
        Text(track.title)
        Spacer(Modifier.weight(1f))
        if (isPlaying) {
            Icon(Icons.Filled.VolumeUp, "Playing")
        }
    }
}
```

**Benefits**:
- Automatic UI responsiveness
- Consistent button behavior across app
- Eliminates per-screen state management
- Centralized track matching logic

### 3. **Unified Command Processing Service**
Create `PlaybackCommandService` to handle all playback intents:

```kotlin
sealed class PlaybackIntent {
    object Pause : PlaybackIntent()
    data class Play(val context: PlaybackContext) : PlaybackIntent()
    data class PlayTrack(val trackId: String, val context: PlaybackContext.RecordingContext) : PlaybackIntent()
    data class PlayAll(val recordingId: String, val showContext: PlaybackContext.ShowContext) : PlaybackIntent()
}

@Singleton  
class PlaybackCommandService @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val playbackStateService: PlaybackStateService,
    private val archiveService: ArchiveService
) {
    suspend fun processIntent(intent: PlaybackIntent): Result<Unit> {
        // Immediate optimistic UI update
        playbackStateService.setLoadingState(intent.context)
        
        return try {
            when (intent) {
                is PlaybackIntent.Pause -> {
                    mediaControllerRepository.pause()
                    playbackStateService.clearLoadingState(intent.context)
                    Result.success(Unit)
                }
                
                is PlaybackIntent.Play -> {
                    when (intent.context) {
                        is PlaybackContext.RecordingContext -> {
                            // Load recording and play
                            val tracks = archiveService.getRecordingTracks(intent.context.recordingId)
                            mediaControllerRepository.playAll(
                                recordingId = intent.context.recordingId,
                                tracks = tracks,
                                showId = intent.context.showId
                            )
                        }
                        // Handle other contexts...
                    }
                    playbackStateService.clearLoadingState(intent.context)
                    Result.success(Unit)
                }
                
                is PlaybackIntent.PlayTrack -> {
                    val track = archiveService.getTrack(intent.trackId, intent.context.recordingId)
                    mediaControllerRepository.playTrack(track, intent.context.recordingId)
                    playbackStateService.clearLoadingState(intent.context)
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            // Revert optimistic state + return error
            playbackStateService.clearLoadingState(intent.context)
            Result.failure(e)
        }
    }
}
```

**Benefits**:
- Centralized loading state management
- Consistent error handling
- Eliminates command processing duplication
- Clear separation of intent vs execution

### 4. **Domain-Driven Service Composition**
Refactor existing services to use shared foundations:

```kotlin
@Singleton
class PlaylistServiceImpl @Inject constructor(
    // Core domain services
    private val showRepository: ShowRepository,
    private val archiveService: ArchiveService,
    // Shared infrastructure services  
    private val playbackStateService: PlaybackStateService,
    private val playbackCommandService: PlaybackCommandService,
    @Named("PlaylistApplicationScope") private val coroutineScope: CoroutineScope
) : PlaylistService {
    
    private var currentShow: Show? = null
    private var currentRecordingId: String? = null
    
    // Domain state (show data, tracks, etc.)
    private val playlistDomainState = MutableStateFlow(PlaylistDomainState())
    
    // Combined UI state using shared infrastructure
    override val uiState: StateFlow<PlaylistUiState> = combine(
        playlistDomainState,
        playbackStateService.playbackState
    ) { domain, playback ->
        PlaylistUiState(
            // Domain data
            showData = domain.showData,
            trackData = domain.trackData,
            // Playback state (automatically managed)
            isPlaying = playback.isPlaying,
            isCurrentShowAndRecording = playback.isContextCurrent(
                PlaybackContext.RecordingContext(
                    recordingId = domain.showData?.currentRecordingId ?: "",
                    showId = domain.showData?.showId ?: ""
                )
            )
        )
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), PlaylistUiState())
    
    // Simplified playback operations
    override suspend fun togglePlayback() {
        val showData = playlistDomainState.value.showData ?: return
        val context = PlaybackContext.RecordingContext(
            recordingId = showData.currentRecordingId ?: return,
            showId = showData.showId
        )
        
        val playbackState = playbackStateService.playbackState.value
        val intent = if (playbackState.isContextCurrent(context) && playbackState.isPlaying) {
            PlaybackIntent.Pause
        } else {
            PlaybackIntent.Play(context)
        }
        
        playbackCommandService.processIntent(intent)
    }
    
    // Focus on domain logic
    override suspend fun loadShow(showId: String, recordingId: String?) {
        val show = showRepository.getShowById(showId) ?: return
        currentShow = show
        currentRecordingId = recordingId ?: show.bestRecordingId
        
        // Update domain state
        playlistDomainState.value = PlaylistDomainState(
            showData = convertShowToViewModel(show),
            isLoading = false
        )
        
        // Load tracks
        loadTrackList()
    }
}

data class PlaylistDomainState(
    val showData: PlaylistShowViewModel? = null,
    val trackData: List<PlaylistTrackViewModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## Implementation Strategy

### Phase 1: Infrastructure Services (Week 1-2)
1. **Create PlaybackStateService**: Central MediaController state aggregation
   - Location: `/v2/core/media/src/main/java/com/deadly/v2/core/media/service/PlaybackStateService.kt`
   - Dependencies: MediaControllerRepository
   - Deliverable: Single source of truth for all playback UI state

2. **Create PlaybackCommandService**: Unified command processing with optimistic UI
   - Location: `/v2/core/media/src/main/java/com/deadly/v2/core/media/service/PlaybackCommandService.kt`
   - Dependencies: MediaControllerRepository, PlaybackStateService, ArchiveService
   - Deliverable: Centralized command processing with loading states

3. **Create PlaybackContext models**: Standardized context information
   - Location: `/v2/core/model/src/main/java/com/deadly/v2/core/model/PlaybackModels.kt`
   - Deliverable: Type-safe playback context system

### Phase 2: Component System (Week 2-3)
1. **Create PlaybackAwareButton**: Smart button component
   - Location: `/v2/core/design/src/main/java/com/deadly/v2/core/design/component/PlaybackAwareButton.kt`
   - Replace existing custom button logic in PlaylistActionRow

2. **Create PlaybackAwareTrackList**: Self-managing track list components
   - Location: `/v2/core/design/src/main/java/com/deadly/v2/core/design/component/PlaybackAwareTrackItem.kt`
   - Centralize track highlighting and state logic

3. **Standardize loading/error states**: Consistent visual patterns
   - Update existing components to use shared loading/error patterns

### Phase 3: Service Refactoring (Week 3-4)
1. **Refactor PlaylistServiceImpl**: Use shared infrastructure services
   - Remove custom MediaController state combination
   - Use PlaybackCommandService for playback operations
   - Focus on domain logic (show loading, track management)

2. **Refactor PlayerServiceImpl**: Eliminate MediaController duplication  
   - Use PlaybackStateService for UI state
   - Delegate commands to PlaybackCommandService

3. **Refactor MiniPlayerServiceImpl**: Use shared state composition
   - Remove custom metadata transformation
   - Use PlaybackStateService for consistent state

### Phase 4: Advanced Features (Week 4+)
1. **Implement preloading system**: Background URL resolution
   - Pre-fetch track URLs for likely-to-be-played content
   - Cache management for better performance

2. **Add offline-first patterns**: Graceful degradation
   - Handle network failures gracefully
   - Prefer cached content when available

3. **Create analytics service**: Centralized user interaction tracking
   - Track user playback patterns
   - Performance metrics for optimization

## Expected Outcomes

### Code Quality Improvements
- **50% reduction** in MediaController integration code across services
- **Consistent** loading states and error handling patterns
- **Centralized** playback logic eliminates duplication
- **Testable** architecture through clear service boundaries

### User Experience Improvements
- **Immediate** UI feedback on all playback interactions (no more "dead" buttons)
- **Consistent** behavior across all playback contexts (playlist, player, mini-player)
- **Reliable** error recovery with clear user feedback
- **Performance** improvements through optimized state management

### Developer Experience Improvements
- **Simpler** feature development using shared playback components
- **Less duplication** when adding new playback contexts or features
- **Clear patterns** for state management and UI responsiveness
- **Easier debugging** through centralized logging and state tracking

### Migration Strategy
- **Backward Compatible**: New services work alongside existing code
- **Incremental**: Can migrate one feature at a time
- **Feature Flag Safe**: Changes can be toggled during development
- **Low Risk**: Existing functionality preserved during migration

## Success Metrics
- **Responsiveness**: All UI interactions provide feedback within 100ms
- **Code Reduction**: 50% reduction in MediaController integration code
- **Error Handling**: Consistent error recovery across all playback contexts
- **Performance**: Improved app responsiveness through optimized state flows
- **Maintainability**: New playback features require minimal boilerplate

---

**Document Status**: Draft for Implementation Planning  
**Last Updated**: December 2024  
**Next Review**: After Phase 1 Implementation