# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Dead Archive is a modern Android application for browsing, streaming, and downloading Grateful Dead concert recordings from Archive.org. Built with Kotlin, Jetpack Compose, and clean architecture principles.

## Essential Commands

### Development Workflow
```bash
# Complete development workflow
make run-emulator           # Start emulator + build + install + launch

# Individual steps
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
- `:feature:browse` - Concert browsing and search
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

**Unified LibraryService** (`core/data/src/main/java/com/deadarchive/core/data/service/LibraryService.kt`)
- **Key Methods:** `addToLibrary()`, `removeFromLibrary()`, `removeShowWithDownloadCleanup()`, `isShowInLibrary()`
- **Dependencies:** LibraryRepository, DownloadService
- **Impact:** Centralized library operations shared across all features with download cleanup support

**BrowseDataService** (`feature/browse/src/main/java/com/deadarchive/feature/browse/service/BrowseDataService.kt`)
- **Key Methods:** `loadPopularShows()`, `loadRecentShows()`, `loadInitialData()`
- **Dependencies:** SearchShowsUseCase

### Library Services

**LibraryDataService** (`feature/library/src/main/java/com/deadarchive/feature/library/service/LibraryDataService.kt`)
- **Key Methods:** `loadLibraryItems()`, `setSortOption()`, `setDecadeFilter()`
- **State Flows:** `sortOption`, `decadeFilter`
- **Dependencies:** ShowRepository

**Unified LibraryService** (`core/data/src/main/java/com/deadarchive/core/data/service/LibraryService.kt`)
- **Key Methods:** `addToLibrary()`, `removeFromLibrary()`, `clearLibrary()`, `removeShowWithDownloadCleanup()`
- **Dependencies:** LibraryRepository, DownloadService
- **Impact:** Replaces feature-specific library management services with centralized implementation
- **Download Integration:** Supports removing shows with optional download cleanup

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

**Benefits of this pattern:**
- **Single Responsibility** - Each service has one clear purpose
- **Testability** - Services can be unit tested in isolation  
- **Reusability** - Services can be shared across ViewModels
- **Maintainability** - Smaller, focused components are easier to modify
- **Backward Compatibility** - Facade pattern preserves existing interfaces

### Build Issues
- **Gradle Wrapper Corrupted:** Use `gradle wrapper --gradle-version=8.14.2` to regenerate
- **Java Version:** Ensure `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64`
- **Build Failures:** Check `build-output.log` for detailed error information

## Archive.org API Integration

### Base Configuration
- **Base URL:** `https://archive.org/`
- **Search:** `/advancedsearch.php`
- **Metadata:** `/metadata/{identifier}`  
- **Downloads:** `/download/{identifier}`

### Response Handling
- Uses fixture-based testing with real API responses
- Custom serializers for flexible string/number parsing
- Error handling for network timeouts and malformed responses
- Retry mechanisms with exponential backoff

## Data Pipeline

The app includes comprehensive data processing for setlist integration:

**Sources:** Archive.org, CS.CMU.EDU setlists, GDSets.com
**Processing:** Venue normalization, song ID mapping, setlist integration
**Output:** Packaged `data.zip` for app deployment

See `docs/setlist-data-pipeline.md` for complete pipeline documentation.

## Known Issues & Troubleshooting

### Gradle Issues
- Wrapper JAR may be corrupted - regenerate with system gradle
- Multiple daemon conflicts - use `gradle --stop` to reset

### Build Environment
- Requires JDK 17+ 
- Android SDK API 34
- Gradle 8.14.2

### Common Fixes
```bash
# Clean everything
make deep-clean

# Reset gradle
gradle --stop && gradle wrapper

# Fix dependencies  
gradle build --refresh-dependencies
```

### Media Player Issues
- **Looping Bug:** Fixed by preventing MediaItem conflicts between QueueManager and PlaybackCommandProcessor
- **Track Highlighting:** Uses MediaId-based matching instead of URL comparison for downloaded content
- **Feedback Loops:** QueueStateManager flows use distinctUntilChanged to prevent duplicate emissions
- **Debug Logging:** Check `DEAD_DEBUG_PANEL` logs for detailed track matching information

### Library Service Migration
- **Service Duplication:** All features now use unified LibraryService instead of feature-specific services
- **Download Integration:** Library removal supports optional download cleanup via DownloadService
- **Consistent Behavior:** LibraryButton component provides unified library actions across all screens

## Media Playback Architecture

### Service-Oriented Media Architecture

The `:core:media` module implements a service-oriented architecture with MediaControllerRepository coordinating three focused services:

**MediaServiceConnector** (`com.deadarchive.core.media.player.service`)
- **Responsibility:** MediaController connection lifecycle and service binding
- **Key Methods:** `connectToService()`, `onControllerConnected()`, `disconnect()`
- **Dependencies:** DeadArchivePlaybackService
- **Impact:** Manages Media3 service connection state and handles connection callbacks

**PlaybackStateSync** (`com.deadarchive.core.media.player.service`)
- **Responsibility:** StateFlow synchronization and position updates
- **Key Methods:** `setupMediaControllerListener()`, `updateStateFromController()`, `updateCurrentTrackInfo()`
- **Dependencies:** ShowRepository for track metadata enrichment
- **Impact:** Provides reactive state flows with rich Recording/Track metadata and stable MediaId tracking
- **State Flows:** `currentTrackUrl`, `currentTrackMediaId`, `currentTrackInfo`, `playbackState`

**PlaybackCommandProcessor** (`com.deadarchive.core.media.player.service`)
- **Responsibility:** Command processing and queue operations
- **Key Methods:** `playTrack()`, `playPlaylist()`, `skipToNext()`, `updateQueueContext()`
- **Dependencies:** LocalFileResolver for offline playback support
- **Impact:** Handles all playback commands and MediaController synchronization

**MediaControllerRepository** (`com.deadarchive.core.media.player`)
- **Responsibility:** Facade coordinator using service composition (~400 lines, down from 1,087)
- **Key Methods:** All public methods delegate to appropriate services, `updatePlaybackStateSyncOnly()`
- **Dependencies:** MediaServiceConnector, PlaybackStateSync, PlaybackCommandProcessor
- **Impact:** Maintains identical public interface while providing cleaner internal architecture
- **Anti-Pattern Prevention:** `updatePlaybackStateSyncOnly()` prevents MediaItem conflicts between services

### Key Components
- **QueueManager:** Manages playback queue and track progression with Recording metadata
- **QueueStateManager:** Queue index and navigation state with feedback loop prevention
- **CurrentTrackInfo:** Rich metadata system with concert date, venue, track titles
- **LastPlayedTrackService:** Spotify-like resume functionality
- **PlaybackEventTracker:** Media3 event monitoring for history tracking
- **DeadArchivePlaybackService:** Background MediaSessionService with rich notifications

### Playback Flow
1. User selects track → QueueManager loads Recording with full metadata
2. QueueManager creates MediaItems with stable `recordingId_filename` MediaId format
3. LocalFileResolver resolves download URLs to `file://` paths for offline content
4. PlaybackStateSync matches tracks using smart URL/filename matching for rich metadata
5. PlayerViewModel syncs UI highlighting using MediaId matching against loaded tracks
6. DeadArchivePlaybackService displays proper track names and show info in notifications
7. ExoPlayer handles actual audio streaming/playback with Media3 integration
8. PlaybackEventTracker logs history for recommendations

### Rich Metadata System
- **Before:** URL parsing like `gd1975.07.23.studio.bershaw.t01-BosBlues1.mp3`
- **After:** Proper metadata like "Scarlet Begonias" by "May 8, 1977 - Barton Hall"
- **Benefits:** Professional music app experience, MiniPlayer visibility, accurate notifications

### Track Highlighting System
- **Stable MediaId Format:** Uses `recordingId_filename` pattern (e.g., `gd1977-05-08_d1t01.mp3`) for reliable track identification
- **Unified Streaming/Downloaded Support:** Same MediaId works for both streaming and downloaded content
- **Smart URL Matching:** PlaybackStateSync handles both `https://` and `file://` URLs for track metadata lookup
- **Cross-Component Synchronization:** Consistent highlighting across PlayerViewModel, MiniPlayer, and notifications

### Critical Bug Fixes
- **Media Player Looping:** Fixed MediaItem conflicts between QueueManager and PlaybackCommandProcessor
- **Downloaded Track Highlighting:** Resolved with stable MediaId format and smart URL matching in PlaybackStateSync
- **Media State Synchronization:** Fixed PlaybackStateSync track lookup to work with downloaded files
- **Library Timestamp Preservation:** Downloading existing library shows preserves original "added" date
- **Navigation Interference:** Disabled aggressive auto-loading that overwrote user show selections
- **Feedback Loop Prevention:** Added distinctUntilChanged to QueueStateManager flows

The media system supports background playback, queue management, offline file resolution, automatic resume of last played track on app restart, and robust track highlighting for both streaming and downloaded content.