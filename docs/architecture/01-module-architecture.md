# Module Architecture Analysis

## Module Dependency Graph

This document provides a comprehensive analysis of the 14-module architecture in Dead Archive, including dependency relationships, architectural patterns, and areas for improvement.

## Module Overview

### Layer Structure
```
┌─────────────────────────────────────────┐
│                App Layer                │
│              app                        │
├─────────────────────────────────────────┤
│           Feature Layer (5)             │
│  browse  player  playlist downloads lib │
├─────────────────────────────────────────┤  
│          API Layer (3)                  │
│    data-api  settings-api  media-api    │
├─────────────────────────────────────────┤
│         Core Layer (9)                  │
│ model database network data media       │
│ design common settings backup           │
└─────────────────────────────────────────┘
```

## Detailed Module Analysis

### Core Foundation Modules

#### core:model (0 internal dependencies)
**Purpose**: Domain entities and business objects
**Key Classes**:
- `Show.kt` - Primary aggregate with complex rating logic
- `Recording.kt` - Individual recording metadata
- `Track.kt` - Audio file representation with smart parsing
- `Rating.kt` - Multi-tier rating system

**Architecture Grade**: A
- Clean domain logic with rich computed properties
- Excellent use of Kotlin data classes
- Smart defaults and validation
- Complex venue normalization logic

**Dependencies**: Only external (kotlinx-serialization)

#### core:common → core:model
**Purpose**: Shared utilities and constants
**Key Components**:
- `ArchiveUrlUtil` - URL manipulation for Archive.org
- `ShareService` - Cross-feature sharing functionality
- `Constants` - App-wide constants

**Architecture Grade**: B+
- Good separation of utilities
- Could benefit from more specific utility classes

#### core:database → core:model
**Purpose**: Room database with comprehensive data access
**Key Components**:
- `DeadArchiveDatabase` - Central database (version 15)
- 11 Entity classes with proper relationships
- 8 DAO classes with complex queries
- Strategic indexing for performance

**Architecture Grade**: A-
- Excellent query coverage and reactive flows
- Strategic denormalization for search performance
- Complex ShowDao with 40+ methods (could be split)
- Good foreign key constraints and cascading

**Notable Features**:
```kotlin
// Strategic indexing for performance
@Entity(
    indices = [
        Index(value = ["date"]),
        Index(value = ["venue"]), 
        Index(value = ["songNames"])
    ]
)
```

#### core:network → core:model
**Purpose**: Archive.org API integration
**Key Components**:
- `ArchiveApiService` - Comprehensive endpoint coverage
- `ArchiveApiClient` - High-level API abstractions
- Custom serializers for API inconsistencies
- Robust error handling with Result wrappers

**Architecture Grade**: A
- Excellent API coverage and error resilience
- Flexible serialization handles API quirks
- Good separation of concerns
- Missing HTTP-level caching

### Data Coordination Layer

#### core:data → (data-api, model, network, database, settings-api)
**Purpose**: Repository implementations coordinating all data sources
**Key Components**:
- `ShowRepositoryImpl` (1132 lines) - Complex show orchestration
- `LibraryRepository` - User library management
- `DownloadRepository` - Offline content management
- `RatingsRepository` - Rating system integration

**Architecture Grade**: B
- Excellent data coordination and caching strategy
- Single source of truth architecture
- **Issue**: ShowRepository is extremely large
- **Issue**: Complex data flows difficult to test
- Sophisticated rating integration system

**Critical Code Path**:
```kotlin
// Show creation with venue normalization
private suspend fun createAndSaveShowsFromRecordings(recordings: List<Recording>): List<Show> {
    val groupedRecordings = recordings.groupBy { recording ->
        val normalizedDate = normalizeDate(recording.concertDate)
        val normalizedVenue = VenueUtil.normalizeVenue(recording.concertVenue)
        "${normalizedDate}_${normalizedVenue}"
    }
    // Complex show aggregation logic
}
```

### API Abstraction Layer

#### core:data-api → core:model
**Purpose**: Repository interfaces for dependency inversion
**Architecture Grade**: A
- Clean interface segregation
- Enables easy testing and mocking
- Good use of Flow for reactive programming

#### core:settings-api → minimal dependencies
**Purpose**: Settings interfaces and models
**Architecture Grade**: A
- Minimal, focused API surface
- Good separation of interface from implementation

#### core:media-api → core:model
**Purpose**: Media player interface abstractions
**Architecture Grade**: A
- Clean media player abstractions
- Good separation from Media3 implementation details

### Implementation Layer

#### core:media → (data-api, model, data, database)
**Purpose**: Media3/ExoPlayer audio playback system
**Key Components**:
- `DeadArchivePlaybackService` - Background media service
- `MediaControllerRepository` (1087 lines) - Service communication
- `QueueManager` - Playlist management
- `PlaybackEventTracker` - Usage analytics

**Architecture Grade**: B+
- Excellent modern Media3 integration
- Sophisticated background playback
- **Issue**: MediaControllerRepository is extremely large
- **Issue**: Complex async state synchronization
- Good offline file resolution

#### core:design → (model, settings-api)
**Purpose**: UI components and Material 3 theme
**Key Components**:
- Material 3 theme with Grateful Dead styling
- Reusable components (`StarRating`, `ExpandableConcertItem`)
- `IconResources` with 50+ icons
- Comprehensive design tokens

**Architecture Grade**: A
- Excellent Material 3 implementation
- Good component reusability
- Clean theme organization

#### core:settings → (settings-api, backup, design, model)
**Purpose**: Settings UI and DataStore persistence
**Architecture Grade**: A-
- Clean settings implementation
- Good DataStore integration
- Well-organized preferences

#### core:backup → (data-api, settings-api, data, database, model)
**Purpose**: Data backup and restore functionality
**Architecture Grade**: B+
- Good backup/restore architecture
- Clean service abstraction

### Feature Layer Analysis

#### feature:browse → (data-api, design, model, data, database, network, settings, playlist)
**Purpose**: Concert browsing and search
**Key Features**:
- Era-based filtering (60s, 70s, 80s, 90s)
- Today in History integration
- Search functionality
- Library integration

**Architecture Grade**: B+
- Good MVVM implementation
- **Issue**: Direct dependency on implementation modules
- **Issue**: Dependency on feature:playlist creates coupling
- Clean Compose UI structure

#### feature:player → (data-api, design, model, data, database, media, network, settings-api, common)
**Purpose**: Full-screen audio player
**Key Features**:
- Swipe navigation between tracks
- Rich metadata display
- MediaController integration
- Queue management UI

**Architecture Grade**: B+
- Excellent UI/UX design
- Good MediaController integration
- **Issue**: Complex PlayerViewModel (1227 lines)
- **Issue**: Direct dependencies on implementation modules

#### feature:playlist → (design, model, data, database, media, network, settings, common, player)
**Purpose**: Recording selection and playlist management
**Key Features**:
- Alternative recording selection
- Recording preferences
- Review system integration
- Mini-player component

**Architecture Grade**: B
- Complex UI with good functionality
- **Issue**: Large PlaylistScreen (1393 lines)
- **Issue**: Dependency on feature:player
- Good integration with recording preferences

#### feature:downloads → (design, model, data)
**Purpose**: Download management
**Architecture Grade**: A-
- Clean, focused implementation
- Good separation of concerns
- Real-time progress tracking

#### feature:library → (data-api, design, model, data, settings-api, settings)
**Purpose**: User library management
**Architecture Grade**: A
- Excellent library management features
- Good filtering and sorting
- Clean MVVM implementation

### App Module

#### app → All features + core modules
**Purpose**: Application entry point and navigation
**Key Components**:
- `DeadArchiveApplication` - DI setup and service initialization
- `MainActivity` - Single Activity with Compose
- `DeadArchiveNavigation` - Navigation routing
- `DebugViewModel` (1702 lines) - Comprehensive debugging tools

**Architecture Grade**: B
- Good application structure
- **Issue**: Massive DebugViewModel should be split
- Excellent DI coordination
- Clean navigation setup

## Dependency Issues Analysis

### 1. Feature-to-Feature Dependencies (Moderate Issue)
```
feature:browse → feature:playlist
feature:playlist → feature:player
```
**Impact**: Reduces modularity, makes features harder to develop independently
**Recommendation**: Extract shared navigation/communication patterns

### 2. Implementation Dependencies in Features (High Priority Issue)
Multiple features depend directly on implementation modules:
```
feature:browse → core:data, core:database, core:network
feature:player → core:data, core:database, core:network  
feature:playlist → core:data, core:database, core:network
```
**Impact**: Violates dependency inversion principle, makes testing harder
**Recommendation**: Features should primarily depend on API modules

### 3. Missing API Abstractions
- No `core:database-api` - features directly access Room entities
- No `core:network-api` - features directly access network layer

### 4. Large Class Issue (Critical)
Several classes exceed maintainability thresholds:
- `DebugViewModel` - 1702 lines
- `PlaylistScreen` - 1393 lines  
- `PlayerViewModel` - 1227 lines
- `ShowRepositoryImpl` - 1132 lines
- `MediaControllerRepository` - 1087 lines

## Architecture Strengths

### 1. Modern Android Practices
- ✅ Jetpack Compose with Material 3
- ✅ Hilt dependency injection throughout
- ✅ StateFlow/Flow reactive programming
- ✅ Modern architecture components

### 2. Clean Architecture Principles  
- ✅ Proper layering with API abstractions
- ✅ Single responsibility for most modules
- ✅ Good separation of concerns
- ✅ Interface-based dependency inversion (where implemented)

### 3. Modular Design Benefits
- ✅ Feature isolation enables parallel development
- ✅ Clear module boundaries
- ✅ Testable architecture with DI
- ✅ Good build performance with parallel compilation

## Recommendations for Improvement

### Priority 1: Reduce Class Complexity
```kotlin
// Split large classes:
ShowRepositoryImpl → ShowRepository + ShowOrchestrator + ShowCache
MediaControllerRepository → MediaController + StateManager + ServiceBridge
DebugViewModel → Multiple focused debug ViewModels
```

### Priority 2: Fix Feature Dependencies
```kotlin
// Move features to API-only dependencies:
feature:browse → core:data-api (remove core:data, core:database, core:network)
feature:player → core:data-api + core:media-api (remove direct implementations)
```

### Priority 3: Reduce Feature Coupling
```kotlin
// Extract shared components:
core:navigation    // Shared navigation logic
core:communication // Event-based feature communication
```

### Priority 4: Add Missing APIs
```kotlin
// Consider adding:
core:database-api  // Database abstraction interfaces
core:network-api   // Network layer abstractions  
```

## Module Metrics

| Module | Files | Lines | Complexity | Grade |
|--------|-------|-------|------------|-------|
| core:model | 15 | 1,200 | Medium | A |
| core:database | 20 | 2,100 | High | A- |
| core:network | 12 | 800 | Medium | A |
| core:data | 8 | 3,500 | Very High | B |
| core:media | 10 | 2,800 | Very High | B+ |
| feature:browse | 6 | 1,200 | Medium | B+ |
| feature:player | 8 | 2,300 | High | B+ |
| feature:playlist | 12 | 2,800 | High | B |
| app | 8 | 3,200 | High | B |

## Overall Architecture Assessment

**Grade: B+**

The Dead Archive project demonstrates a well-structured modular architecture that follows modern Android development practices. The use of API modules shows good architectural thinking, and the separation of concerns is generally well-executed.

**Key Strengths**:
- Modern tech stack with consistent patterns
- Good use of clean architecture principles  
- Excellent UI/UX implementation
- Sophisticated media and data systems

**Priority Improvements**:
1. Break down large classes for better maintainability
2. Strengthen API boundaries to improve testability
3. Reduce feature coupling for better modularity
4. Add comprehensive unit and integration tests

The architecture provides a solid foundation for a complex Android application, with clear paths for improvement that would elevate it from good to excellent.