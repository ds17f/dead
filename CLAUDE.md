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
  - **Browse operations:** BrowseViewModel → BrowseSearchService, BrowseLibraryService, BrowseDownloadService, BrowseDataService
  - **Library operations:** LibraryViewModel → LibraryDataService, LibraryDownloadService, LibraryManagementService

### Data Services Architecture

The `:core:data` module implements a service-oriented architecture with the main ShowRepository delegating specialized concerns to focused services:

**ShowEnrichmentService** (`com.deadarchive.core.data.service`)
- **Responsibility:** Rating and recording enrichment
- **Key Methods:** `enrichShowWithRatings()`, `enrichRecordingWithRating()`, `attachRecordingsToShow()`
- **Dependencies:** RecordingDao, RatingsRepository
- **Impact:** Handles all rating lookups and user preference application

**ShowCacheService** (`com.deadarchive.core.data.service`)
- **Responsibility:** Cache management and API interaction
- **Key Methods:** `isCacheExpired()`, `getRecordingMetadata()`, `isAudioFile()`
- **Dependencies:** ArchiveApiService
- **Impact:** Centralizes cache validation and Archive.org API calls

**ShowCreationService** (`com.deadarchive.core.data.service`)
- **Responsibility:** Show creation and normalization
- **Key Methods:** `createAndSaveShowsFromRecordings()`, `normalizeDate()`, `groupRecordingsByShow()`
- **Dependencies:** ShowDao
- **Impact:** Handles complex show creation workflow from recordings

### Player Services Architecture

The `:feature:player` module implements service-oriented architecture with PlayerViewModel delegating specialized concerns to focused services:

**PlayerDataService** (`com.deadarchive.feature.player.service`)
- **Responsibility:** Recording data loading and management
- **Key Methods:** `loadRecording()`, `getAlternativeRecordings()`, `findNextShowByDate()`, `getBestRecordingForShow()`
- **Dependencies:** ShowRepository, ArchiveApiService
- **Impact:** Handles all recording data operations and navigation logic

**PlayerPlaylistService** (`com.deadarchive.feature.player.service`)
- **Responsibility:** Playlist management and queue operations
- **Key Methods:** `setPlaylist()`, `navigateToTrack()`, `addToPlaylist()`, `playTrackFromPlaylist()`
- **Dependencies:** QueueManager, MediaControllerRepository
- **Impact:** Manages playlist state and track playback operations

**PlayerDownloadService** (`com.deadarchive.feature.player.service`)
- **Responsibility:** Download state management and monitoring
- **Key Methods:** `downloadRecording()`, `getRecordingDownloadState()`, `isTrackDownloaded()`
- **Dependencies:** DownloadRepository
- **Impact:** Provides reactive download state monitoring and control

**PlayerLibraryService** (`com.deadarchive.feature.player.service`)
- **Responsibility:** Library integration and status tracking
- **Key Methods:** `checkLibraryStatus()`, `addToLibrary()`, `removeFromLibrary()`
- **Dependencies:** LibraryRepository, ShowRepository
- **Impact:** Manages show library operations and status updates

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

**BrowseSearchService** (`com.deadarchive.feature.browse.service`)
- **Responsibility:** Search functionality and era filtering (~185 lines)
- **Key Methods:** `updateSearchQuery()`, `searchShows()`, `filterByEra()`, `cancelCurrentSearch()`
- **Dependencies:** SearchShowsUseCase for data operations
- **State Management:** Internal search query and loading state with StateFlow
- **Impact:** Handles all search operations with proper cancellation and debouncing

**BrowseLibraryService** (`com.deadarchive.feature.browse.service`)
- **Responsibility:** Library operations with local UI updates (~40 lines)
- **Key Methods:** `toggleLibrary()` with optimistic UI updates
- **Dependencies:** LibraryRepository for persistence
- **Pattern:** Callback-based UI state updates for immediate user feedback
- **Impact:** Clean separation of library logic from UI concerns

**BrowseDownloadService** (`com.deadarchive.feature.browse.service`)
- **Responsibility:** Download management and state monitoring (~220 lines)
- **Key Methods:** `downloadShow()`, `downloadRecording()`, `cancelShowDownloads()`, `startDownloadStateMonitoring()`
- **Dependencies:** DownloadRepository, WorkManager for background downloads
- **State Management:** Download states map and confirmation dialogs via StateFlow
- **Impact:** Comprehensive download handling with real-time progress tracking

**BrowseDataService** (`com.deadarchive.feature.browse.service`)
- **Responsibility:** Data loading operations (~120 lines)
- **Key Methods:** `loadPopularShows()`, `loadRecentShows()`, `loadInitialData()`
- **Dependencies:** SearchShowsUseCase for various data queries
- **Pattern:** Coroutine-based data loading with proper cancellation
- **Impact:** Separates data loading logic from search operations

**BrowseViewModel** (refactored coordinator ~100 lines, reduced from 486 lines)
- **Responsibility:** UI coordination using service composition
- **Key Methods:** All public methods delegate to appropriate services
- **Dependencies:** All four browse services above
- **Pattern:** Facade pattern with callback-based service coordination
- **Impact:** 80% reduction in size while maintaining full backward compatibility

This service extraction follows the successful MediaControllerRepository pattern, transforming a monolithic ViewModel into a clean coordination layer.

### Library Feature Services Architecture

The `:feature:library` module implements service-oriented architecture with the LibraryViewModel coordinating between focused services:

**LibraryDataService** (`com.deadarchive.feature.library.service`)
- **Responsibility:** Data loading and filtering operations (~120 lines)
- **Key Methods:** `loadLibraryItems()`, `setSortOption()`, `setDecadeFilter()`
- **Dependencies:** ShowRepository for library data access
- **State Management:** Sort options and decade filters via StateFlow with reactive updates
- **Impact:** Complex reactive data loading with combine() for filtering, sorting, and legacy format conversion

**LibraryDownloadService** (`com.deadarchive.feature.library.service`)
- **Responsibility:** Download management and state monitoring (~220 lines)
- **Key Methods:** `downloadShow()`, `downloadRecording()`, `cancelShowDownloads()`, `startDownloadStateMonitoring()`
- **Dependencies:** DownloadRepository for download operations
- **Pattern:** Consistent with BrowseDownloadService for code reusability
- **Impact:** Comprehensive download handling with optimistic UI updates and confirmation dialogs

**LibraryManagementService** (`com.deadarchive.feature.library.service`)
- **Responsibility:** Library operations (add, remove, clear) (~60 lines)
- **Key Methods:** `removeFromLibrary()`, `removeShowFromLibrary()`, `clearLibrary()`
- **Dependencies:** LibraryRepository for persistence
- **Pattern:** Simple coroutine-based operations with error handling
- **Impact:** Clean separation of library management logic from UI concerns

**LibraryViewModel** (refactored coordinator ~120 lines, reduced from 407 lines)
- **Responsibility:** UI coordination using service composition
- **Key Methods:** All public methods delegate to appropriate services
- **Dependencies:** LibraryDataService, LibraryDownloadService, LibraryManagementService
- **Pattern:** Facade pattern with callback-based service coordination
- **Impact:** 70% reduction in size while maintaining full backward compatibility

This service extraction maintains consistency with the BrowseViewModel pattern, enabling code reuse and establishing clear architectural patterns.

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

### Database Schema Changes
1. Update entities in `core/database/src/main/java/com/deadarchive/core/database/`
2. Create migration in `DeadArchiveDatabase.kt`
3. Update corresponding DAOs
4. Add migration tests

### Service-Oriented ViewModel Development

When creating new ViewModels or refactoring existing ones, follow the established service-oriented architecture pattern:

**Service Extraction Pattern:**
1. **Analyze responsibilities** - Identify distinct functional areas (search, downloads, library, data loading)
2. **Create focused services** - Each service handles one specific responsibility (~40-220 lines)
3. **Use @Singleton @Inject** - All services are Hilt singletons with constructor injection
4. **StateFlow for reactive state** - Services expose state via StateFlow when needed
5. **Callback coordination** - ViewModel coordinates via callbacks: `onStateChange: (UiState) -> Unit`
6. **Maintain public interface** - ViewModel facade delegates to services, preserving backward compatibility

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
- **Impact:** Provides reactive state flows with rich Recording/Track metadata instead of URL parsing

**PlaybackCommandProcessor** (`com.deadarchive.core.media.player.service`)
- **Responsibility:** Command processing and queue operations
- **Key Methods:** `playTrack()`, `playPlaylist()`, `skipToNext()`, `updateQueueContext()`
- **Dependencies:** LocalFileResolver for offline playback support
- **Impact:** Handles all playback commands and MediaController synchronization

**MediaControllerRepository** (`com.deadarchive.core.media.player`)
- **Responsibility:** Facade coordinator using service composition (~400 lines, down from 1,087)
- **Key Methods:** All public methods delegate to appropriate services
- **Dependencies:** MediaServiceConnector, PlaybackStateSync, PlaybackCommandProcessor
- **Impact:** Maintains identical public interface while providing cleaner internal architecture

### Key Components
- **QueueManager:** Manages playback queue and track progression with Recording metadata
- **CurrentTrackInfo:** Rich metadata system with concert date, venue, track titles
- **LastPlayedTrackService:** Spotify-like resume functionality
- **PlaybackEventTracker:** Media3 event monitoring for history tracking
- **DeadArchivePlaybackService:** Background MediaSessionService with rich notifications

### Playback Flow
1. User selects track → QueueManager loads Recording with full metadata
2. MediaControllerRepository updates queue context with Recording data
3. PlaybackStateSync enriches CurrentTrackInfo with concert details
4. DeadArchivePlaybackService displays proper track names and show info in notifications
5. ExoPlayer handles actual audio streaming with Media3 integration
6. PlaybackEventTracker logs history for recommendations

### Rich Metadata System
- **Before:** URL parsing like `gd1975.07.23.studio.bershaw.t01-BosBlues1.mp3`
- **After:** Proper metadata like "Scarlet Begonias" by "May 8, 1977 - Barton Hall"
- **Benefits:** Professional music app experience, MiniPlayer visibility, accurate notifications

The media system supports background playback, queue management, offline file resolution, and automatic resume of last played track on app restart.