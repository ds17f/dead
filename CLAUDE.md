# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Deadly is a modern Android application for browsing, streaming, and downloading Grateful Dead concert recordings from Archive.org. Built with Kotlin, Jetpack Compose, and clean architecture principles.

## Essential Commands

### Development Workflow

```bash
# Complete development workflow
make run-emulator           # Start emulator + build + install + launch

# Individual steps
make install-quiet         # Build and install without verbose output.  Use this when you need to build
make build                  # Build debug APK
make install               # Install to connected device
make run                   # Install and launch app
make test                  # Run unit tests
make lint                  # Run lint checks
```

### Build & Release

```bash
make release               # Build release APK
make tag-release          # Full release: tests + lint + builds + tag
make tag-release-quick    # Quick release (skip quality checks)
```

### Device Management

```bash
make devices              # Show connected devices
make emulator            # Start Android emulator
make emu-stop           # Stop all emulators
make logs               # Show app logs
```

### Data Collection

```bash
make collect-metadata-test        # Test metadata collection (10 recordings)
make collect-metadata-1977       # Collect 1977 shows (golden year)
make package-datazip             # Package metadata for app deployment
```

### Debug Tools

```bash
# Enable debug mode in app: Settings → Developer Options → Debug Mode
adb logcat -s DEAD_DEBUG_PANEL   # View debug panel output from copy actions
adb logcat -s DEAD_DEBUG_PANEL | grep "PlaylistScreen"  # Filter by screen
```

## Architecture Overview

### Module Structure

**Core Modules:**

- `:core:model` - Data models and domain entities
- `:core:data` - Repository implementations and data access services
- `:core:database` - Room database with entities and DAOs
- `:core:network` - Retrofit networking for Archive.org API
- `:core:media` - Media3/ExoPlayer integration for audio playback
- `:core:design` - UI components, themes, Material3 design system
- `:core:common` - Shared utilities and constants
- `:core:settings` - App settings management with DataStore
- `:core:backup` - Backup and restore functionality

**Feature Modules:**

- `:feature:browse` - Concert browsing and search (includes SearchV2 implementation)
- `:feature:player` - Audio player with playback controls
- `:feature:playlist` - Playlist management and recording selection
- `:feature:downloads` - Download management for offline listening
- `:feature:library` - User favorites and library management

**API Modules:** (for decoupling)

- `:core:settings-api`, `:core:data-api`, `:core:media-api`

### Key Architecture Patterns

**Clean Architecture:** Clear separation between data, domain, and presentation layers

**MVVM with Compose:** ViewModels expose UI state via StateFlow, Compose UI observes state changes

**Dependency Injection:** Hilt provides scoped dependencies across all modules

**Reactive Data Flow:** Repository pattern with Flow-based data streams

**Service-Oriented Architecture:** Large components split into focused services following Single Responsibility Principle:

- **Data operations:** ShowRepository → ShowEnrichmentService, ShowCacheService, ShowCreationService
- **Media operations:** MediaControllerRepository → MediaServiceConnector, PlaybackStateSync, PlaybackCommandProcessor
- **Download operations:** Centralized DownloadService shared across all features
- **Browse operations:** BrowseViewModel → BrowseSearchService, BrowseDataService
- **Library operations:** Unified LibraryService shared across all ViewModels (Browse, Library, Player, Playlist)

### Data Services Architecture

The `:core:data` module implements a service-oriented architecture with the main ShowRepository delegating specialized concerns to focused services:

**ShowEnrichmentService** (`core/data/src/main/java/com/deadarchive/core/data/service/ShowEnrichmentService.kt`)

- **Key Methods:** `enrichShowWithRatings()`, `enrichRecordingWithRating()`, `attachRecordingsToShow()`
- **Dependencies:** RecordingDao, RatingsRepository

**ShowCacheService** (`core/data/src/main/java/com/deadarchive/core/data/service/ShowCacheService.kt`)

- **Key Methods:** `isCacheExpired()`, `getRecordingMetadata()`, `isAudioFile()`
- **Dependencies:** ArchiveApiService

**ShowCreationService** (`core/data/src/main/java/com/deadarchive/core/data/service/ShowCreationService.kt`)

- **Key Methods:** `createAndSaveShowsFromRecordings()`, `normalizeDate()`, `groupRecordingsByShow()`
- **Dependencies:** ShowDao

### Download Architecture

**DownloadService** (`core/data/src/main/java/com/deadarchive/core/data/download/DownloadService.kt`)

- **Singleton service** shared across all ViewModels (Player, Browse, Library)
- **State Flows:** `downloadStates: StateFlow<Map<String, ShowDownloadState>>`, `trackDownloadStates: StateFlow<Map<String, Boolean>>`
- **Key Methods:** `downloadRecording(recording)`, `downloadShow(show)`, `cancelShowDownloads(show)`, `getDownloadState(recording)`
- **Progress Aggregation:** Groups individual track downloads by `recordingId` to calculate `completedTracks/totalTracks`
- **Auto-monitoring:** Observes `DownloadRepository.getAllDownloads()` flow and updates UI states in real-time

### Player Services

**PlayerDataService** (`feature/player/src/main/java/com/deadarchive/feature/player/service/PlayerDataService.kt`)

- **Key Methods:** `loadRecording()`, `getAlternativeRecordings()`, `findNextShowByDate()`, `getBestRecordingForShow()`
- **Dependencies:** ShowRepository, ArchiveApiService

**PlayerPlaylistService** (`feature/player/src/main/java/com/deadarchive/feature/player/service/PlayerPlaylistService.kt`)

- **Key Methods:** `setPlaylist()`, `navigateToTrack()`, `addToPlaylist()`, `playTrackFromPlaylist()`
- **Dependencies:** QueueManager, MediaControllerRepository

**Unified LibraryService Integration**

- **Migration:** PlayerViewModel now uses centralized LibraryService instead of PlayerLibraryService
- **Key Methods:** `isShowInLibrary()`, `addToLibrary()`, `removeFromLibrary()`
- **Benefits:** Consistent library behavior across all features, eliminated service duplication

### Media Services Architecture

The `:core:media` module has been refactored from a monolithic 1,087-line MediaControllerRepository into a service-oriented architecture:

**MediaServiceConnector** (`com.deadarchive.core.media.player.service`)

- **Responsibility:** Connection lifecycle and service binding management (~150 lines)
- **Key Methods:** `connectToService()`, `onControllerConnected()`, `disconnect()`
- **Dependencies:** DeadArchivePlaybackService
- **Impact:** Handles Media3 service connection state and callbacks

**PlaybackStateSync** (`com.deadarchive.core.media.player.service`)

- **Responsibility:** StateFlow synchronization and position updates (~300 lines)
- **Key Methods:** `setupMediaControllerListener()`, `updateCurrentTrackInfo()`, `updateQueueState()`
- **Dependencies:** ShowRepository for rich metadata
- **Impact:** Replaces URL parsing with Recording/Track metadata system

**PlaybackCommandProcessor** (`com.deadarchive.core.media.player.service`)

- **Responsibility:** Command processing and queue operations (~200 lines)
- **Key Methods:** `playTrack()`, `playPlaylist()`, `skipToNext()`, `updateQueueContext()`
- **Dependencies:** LocalFileResolver for offline support
- **Impact:** Handles all playback commands and MediaController synchronization

**MediaControllerRepository** (refactored coordinator ~400 lines)

- **Responsibility:** Facade using service composition
- **Key Methods:** All public methods delegate to appropriate services
- **Dependencies:** All three media services above
- **Impact:** Maintains full backward compatibility with cleaner architecture

This architecture follows Single Responsibility Principle, making the codebase more maintainable and testable by isolating concerns into dedicated services.

### Browse Feature Services Architecture

The `:feature:browse` module implements service-oriented architecture with the BrowseViewModel coordinating between focused services:

### Browse Services

**BrowseSearchService** (`feature/browse/src/main/java/com/deadarchive/feature/browse/service/BrowseSearchService.kt`)
- **Key Methods:** `updateSearchQuery()`, `searchShows()`, `filterByEra()`, `cancelCurrentSearch()`
- **State Flows:** `searchQuery`, `isSearching`
- **Dependencies:** SearchShowsUseCase

**BrowseDataService** (`feature/browse/src/main/java/com/deadarchive/feature/browse/service/BrowseDataService.kt`)
- **Key Methods:** `loadPopularShows()`, `loadRecentShows()`, `loadInitialData()`
- **Dependencies:** SearchShowsUseCase

### Library Services

**LibraryDataService** (`feature/library/src/main/java/com/deadarchive/feature/library/service/LibraryDataService.kt`)
- **Key Methods:** `loadLibraryItems()`, `setSortOption()`, `setDecadeFilter()`
- **State Flows:** `sortOption`, `decadeFilter`
- **Dependencies:** ShowRepository

**Unified LibraryService** (`core/data/src/main/java/com/deadarchive/core/data/service/LibraryService.kt`)
- **Key Methods:** `addToLibrary()`, `removeFromLibrary()`, `clearLibrary()`, `removeShowWithDownloadCleanup()`, `isShowInLibrary()`
- **Dependencies:** LibraryRepository, DownloadService
- **Impact:** Centralized library operations shared across all features with download cleanup support

### UI Component Decomposition

**LibraryScreen** - Reduced from 1,253 to 1,170 lines by extracting focused components:  
**LibraryTopBar** (15 lines), **LibraryEmptyState** (26 lines), **LibraryItemsList** (42 lines)

**Benefits:** Single responsibility, testability, reusability, maintainability, and backward compatibility


### Entry Points

**Application Class:** `DeadArchiveApplication.kt`

- Initializes Hilt DI
- Sets up WorkManager for background downloads
- Initializes ratings database
- Restores last played track on app start

**Main Activity:** `MainActivity.kt`

- Single Activity architecture
- Hosts Compose navigation
- Handles theme mode from settings

**Navigation:** `DeadArchiveNavigation.kt`

- Type-safe Navigation Compose
- Handles splash screen logic
- Routes between feature modules

## Key Technologies

**UI & Architecture:**

- Jetpack Compose with Material3
- Navigation Compose for type-safe navigation
- Hilt for dependency injection
- Room for local database
- DataStore for settings persistence

**Media & Networking:**

- Media3/ExoPlayer for audio playback
- Retrofit with Kotlin Serialization for Archive.org API
- WorkManager for background downloads
- MediaSession for background playback

**Testing:**

- JUnit4 with AndroidX Test
- Fixture-based testing for Archive.org integration
- Hilt testing modules for dependency injection

## Testing Strategy

### Running Tests

```bash
gradle test                # All unit tests
gradle :core:network:test  # Specific module tests
```

### Test Structure

- **Unit Tests:** Individual component testing with mocks
- **Integration Tests:** Multi-component interaction testing
- **Repository Tests:** Data layer testing with mock API responses
- **Fixture-Based Tests:** Archive.org API testing with real response fixtures

### Key Test Locations

- `core/network/src/test/resources/fixtures/` - Archive.org API response fixtures
- `core/*/src/test/` - Unit tests for each core module
- `feature/*/src/test/` - Feature-specific tests

## Common Development Tasks

### Adding New Archive.org Integration

1. Add API models to `core/network/src/main/java/com/deadarchive/core/network/model/`
2. Update `ArchiveApiService.kt` with new endpoints
3. Add repository methods in `core/data/src/main/java/com/deadarchive/core/data/repository/`
4. Create fixtures from real API responses for testing

### Adding New UI Components

1. Create in `core/design/src/main/java/com/deadarchive/core/design/component/`
2. Follow existing Material3 theming patterns
3. Add to `IconResources.kt` if new icons are needed
4. Use existing design tokens from `Theme.kt`

### Unified UI Components

**LibraryButton** (`core/design/src/main/java/com/deadarchive/core/design/component/LibraryButton.kt`)

- **Purpose:** Centralized library management component used across all features
- **Actions:** Add to library, remove from library, remove with download cleanup
- **Integration:** Works with unified LibraryService for consistent behavior
- **Features:** Automatic icon state, confirmation dialogs, download cleanup options

### Database Schema Changes

1. Update entities in `core/database/src/main/java/com/deadarchive/core/database/`
2. Create migration in `DeadArchiveDatabase.kt`
3. Update corresponding DAOs
4. Add migration tests

### Service-Oriented ViewModel Development

When creating new ViewModels or refactoring existing ones, follow the established service-oriented architecture pattern:

**Service Architecture Rules:**

1. **Use existing shared services** - `DownloadService` is centralized, don't duplicate it
2. **Create focused feature services** - One responsibility per service (search, library, data loading)
3. **@Singleton @Inject pattern** - All services are Hilt singletons with constructor injection
4. **StateFlow for state** - Services expose reactive state via StateFlow when needed
5. **Callback coordination** - ViewModels coordinate via `onStateChange: (UiState) -> Unit` callbacks

**Service Structure Template:**

```kotlin
@Singleton
class FeatureOperationService @Inject constructor(
    private val dependency: SomeRepository
) {
    companion object {
        private const val TAG = "FeatureOperationService"
    }

    fun performOperation(
        parameters: InputType,
        coroutineScope: CoroutineScope,
        onStateChange: (UiState) -> Unit
    ) {
        coroutineScope.launch {
            // Business logic here
            onStateChange(UiState.Success(result))
        }
    }
}
```

**ViewModel Facade Template:**

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val operationService: FeatureOperationService,
    private val dataService: FeatureDataService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun performOperation(input: InputType) {
        operationService.performOperation(
            parameters = input,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
    }
}
```

**Benefits:** Single responsibility, testability, reusability, maintainability, and backward compatibility through facade pattern

### Build Issues

- **Gradle Wrapper Corrupted:** Use `gradle wrapper --gradle-version=8.14.2` to regenerate
- **Java Version:** Ensure `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64`
- **Build Failures:** Check `build-output.log` for detailed error information

## Archive.org API Integration

**Base URL:** `https://archive.org/` with endpoints for search, metadata, and downloads  
**Response Handling:** Fixture-based testing, custom serializers, error handling, retry mechanisms with exponential backoff

## Data Pipeline

**Sources:** Archive.org, CS.CMU.EDU setlists, GDSets.com  
**Processing:** Venue normalization, song ID mapping, setlist integration  
**Output:** Packaged `data.zip` for app deployment (see `docs/setlist-data-pipeline.md`)

## Known Issues & Troubleshooting

**Build Environment:** JDK 17+, Android SDK API 34, Gradle 8.14.2

**Common Fixes:**
```bash
make deep-clean                              # Clean everything
gradle --stop && gradle wrapper              # Reset gradle  
gradle build --refresh-dependencies          # Fix dependencies
```

**Media Player Issues:** Fixed looping bug, track highlighting, feedback loops, state synchronization  
**Debug:** Check `DEAD_DEBUG_PANEL` logs for track matching details


## Media Playback Architecture

The `:core:media` module implements service-oriented architecture with MediaControllerRepository coordinating three focused services:

**MediaServiceConnector**: Connection lifecycle and service binding management  
**PlaybackStateSync**: StateFlow synchronization with rich Recording/Track metadata  
**PlaybackCommandProcessor**: Command processing and queue operations with offline support  
**MediaControllerRepository**: Facade coordinator (400 lines, down from 1,087)

**Key Components:** QueueManager, QueueStateManager, CurrentTrackInfo, LastPlayedTrackService, PlaybackEventTracker, DeadArchivePlaybackService

**Playback Flow:** User selection → QueueManager metadata loading → MediaId creation → LocalFileResolver → PlaybackStateSync matching → UI highlighting → ExoPlayer playback

**Rich Metadata System:** Replaces URL parsing with proper track metadata ("Scarlet Begonias" by "May 8, 1977 - Barton Hall")

**Track Highlighting:** Stable `recordingId_filename` MediaId format supporting both streaming and downloaded content

**Critical Fixes:** Media player looping, track highlighting, state synchronization, library timestamp preservation, navigation interference, feedback loop prevention

## V2 Architecture (Feature Implementations)

The app implements V2 architecture across major features with UI-first development patterns, comprehensive service abstraction, and feature flag safety.

### V2 Implementation Overview

**Core V2 Principles:**
- **UI-First Development**: Build components first, then extract service requirements
- **Service Abstraction**: Clean service contracts with comprehensive stub implementations
- **Feature Flag Safety**: Risk-free deployment with settings toggles
- **Visual Parity**: Exact V1 visual match while maintaining V2 architectural benefits

### PlayerV2

**Status**: Complete - Professional UI with comprehensive stub implementation  
**Feature Flag**: `usePlayerV2: Boolean` in AppSettings

**Key Innovations:**
- **Recording-Based Visual Identity**: Consistent color scheme per recording across app
- **Scrolling Gradient System**: LazyColumn-integrated gradients for natural transitions
- **Component Architecture**: 8 focused components (TopBar, CoverArt, Controls, etc.)
- **Mini-Player Integration**: Scroll-based visibility with color consistency

**Service Interface:**
```kotlin
interface PlayerV2Service {
    val playerState: Flow<PlayerV2State>
    suspend fun getCurrentTrackInfo(): TrackDisplayInfo?
    suspend fun togglePlayPause()
    suspend fun seekToPosition(position: Float)
}
```

### SearchV2

**Status**: Complete - Professional search UI with transparent design  
**Feature Flag**: `useSearchV2: Boolean` in AppSettings

**UI Innovations:**
- **Transparent Search Interface**: Invisible input with Color.Transparent styling
- **Smart Text Positioning**: 8dp offset for precise arrow alignment  
- **Enhanced Icons**: 28dp size for improved visibility and touch targets

**Component Architecture:**
```kotlin
SearchV2Screen(TopBar, SearchBox, BrowseSection, DiscoverSection)
SearchResultsV2Screen(TopBar, RecentSearches, Suggestions, Results)
```

### PlaylistV2

**Status**: Track Section Complete - Visual parity with V1 achieved  
**Feature Flag**: `usePlaylistV2: Boolean` in AppSettings

**Visual Parity Achievements:**
- **Track Header**: Exact V1 format `"Tracks (${tracks.size})"`
- **Track Items**: Simple Row layout with V1 color coding
- **Music Note Logic**: `isCurrentTrack && isPlaying` matching V1 exactly

**V2 Benefits Preserved:**
- Rich PlaylistTrackViewModel data patterns
- Component isolation and LazyListScope integration
- Service boundaries and callback patterns

### MiniPlayerV2

**Status**: Complete - Visual parity with V1 EnrichedMiniPlayer  
**Feature Flag**: `useMiniPlayerV2: Boolean` in AppSettings

**Exact V1 Replication:**
- **Layout**: 88dp height, 8dp elevation, 12dp rounded corners
- **Typography**: Material3 bodyMedium/bodySmall with exact color scheme
- **ScrollingText**: Identical 8-second animation with pauses
- **3-Line Format**: Track name, date, venue/location

**Global Integration:**
```kotlin
// MainAppScreen.kt
if (settings.useMiniPlayerV2) {
    MiniPlayerV2Container(onTapToExpand = onNavigateToPlayer)
} else {
    MiniPlayerContainer(onTapToExpand = onNavigateToPlayer)
}
```

### V2 Development Patterns

**UI-First Pattern:**
```kotlin
// 1. Build UI component
PlayerV2ProgressControl(progress = 0.31f, onSeek = { position -> })
// 2. Extract service need
suspend fun seekToPosition(position: Float)
// 3. Implement in stub
override suspend fun seekToPosition(position: Float) { /* mock */ }
```

**Service Architecture:**
- Clean service contracts with Flow-based state
- Comprehensive stub implementations with realistic mock data
- Hilt @Singleton injection with proper scoping
- Future-ready for V1 service integration

**Feature Flag Integration:**
- Settings toggles in Developer Options
- Navigation routing with conditional rendering
- Safe A/B testing between V1 and V2 implementations

### Success Metrics

**Architecture Quality:**
- 83% code reduction (PlayerV2: 184 lines vs V1's 1,099 lines)
- Single service dependencies vs V1's 8+ injected services
- Component-based architecture enabling focused testing

**Visual Compliance:**
- Exact visual parity across all V2 implementations
- Zero user experience differences from V1
- Consistent recording-based visual identity system

**Development Experience:**
- Immediate UI development with comprehensive stubs
- Risk-free deployment via feature flags
- Clean service boundaries for future real implementations

## V2 Service Architecture Evolution (Phase 1.0+)

**Foundation First Approach:** Systematic elimination of code duplication and threading issues across V2 services through direct MediaControllerRepository delegation pattern.

### Direct Delegation Architecture

**Core Pattern:** Services depend directly on MediaControllerRepository and MediaControllerStateUtil instead of thin abstraction layers.

```kotlin
@Singleton
class ServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,  // Raw state & commands
    private val mediaControllerStateUtil: MediaControllerStateUtil     // Rich state objects
) : Service {
    
    // Direct delegation - no wrapper layers
    override val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    override val playbackStatus: StateFlow<PlaybackStatus> = mediaControllerRepository.playbackStatus
    override val currentTrackInfo: StateFlow<CurrentTrackInfo?> = 
        mediaControllerStateUtil.createCurrentTrackInfoStateFlow(serviceScope)
    override val queueInfo: StateFlow<QueueInfo> = 
        mediaControllerStateUtil.createQueueInfoStateFlow(serviceScope)
        
    override suspend fun togglePlayPause() = mediaControllerRepository.togglePlayPause()
}
```

### Unified State Models

**PlaybackStatus:** Replaces fragmented position/duration/progress StateFlows with single unified model
**QueueInfo:** Rich queue state with computed properties (hasNext, hasPrevious, queueProgress) for navigation decisions  
**CurrentTrackInfo:** Comprehensive track metadata from MediaControllerStateUtil

### Service Implementation Status

**✅ MiniPlayerService (Phase 1.0):** Complete direct delegation implementation
- Eliminated PlaybackStateService thin wrapper layer
- Clean Hilt DI with no circular dependencies
- QueueInfo integration for navigation logic

**🔄 PlayerService (Phase 1.1):** Pending - apply same direct delegation pattern
**🔄 PlaylistService (Phase 1.2):** Pending - apply same direct delegation pattern

### Architecture Benefits

**Eliminated Complexity:**
- No more PlaybackStateService thin wrappers (was just ceremonial delegation)
- No circular dependency issues or Hilt binding conflicts
- Simplified dependency graph with clear ownership

**Maintained Benefits:**
- Unified reactive state models (PlaybackStatus, QueueInfo, CurrentTrackInfo)
- MediaControllerRepository as single source of truth
- MediaControllerStateUtil for rich state object creation

**Pattern Consistency:**
- Information architecture: services observe rich state and make business decisions
- Same pattern for CurrentTrackInfo and QueueInfo creation
- Direct command delegation with zero abstraction overhead

