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

**Service-Oriented Architecture:** Core data operations split into focused services

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

This architecture follows Single Responsibility Principle, making the codebase more maintainable and testable by isolating concerns into dedicated services.

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

### Key Components
- **MediaPlayer:** Core playback interface (`core/media-api`)
- **PlaybackService:** Background service for continuous playback
- **QueueManager:** Manages playback queue and track progression
- **MediaController:** Repository for UI state and control commands
- **LastPlayedTrackService:** Spotify-like resume functionality

### Playback Flow
1. User selects track → QueueManager builds playlist
2. MediaController sends commands to PlaybackService  
3. ExoPlayer handles actual audio streaming
4. MediaSession provides notification controls
5. PlaybackEventTracker logs history for recommendations

The media system supports background playback, queue management, and automatic resume of last played track on app restart.