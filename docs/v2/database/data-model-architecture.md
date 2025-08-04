# V2 Data Model Architecture

## Overview

The V2 data model implements a clean, date-centric architecture with clear domain boundaries and comprehensive user activity tracking. This greenfield design prioritizes performance, maintainability, and user experience.

## Entity Design

### Core Entities

#### Show
```kotlin
@Entity(tableName = "shows_v2")
data class ShowV2Entity(
    @PrimaryKey
    val showId: String,           // UUID or similar
    
    // Date components for flexible searching
    val date: String,             // "1977-05-08" (full date)
    val year: Int,                // 1977 (indexed)
    val month: Int,               // 5 (indexed)
    val yearMonth: String,        // "1977-05" (indexed)
    
    // Location
    val venueId: String,          // FK to venues_v2
    val city: String?,            // Denormalized for fast search
    val state: String?,           // Denormalized for fast search
    val country: String = "USA",
    
    // Simple setlist storage
    val setlistRaw: String?,      // Raw text as scraped
    val songList: String?,        // "Scarlet Begonias,Fire on the Mountain,Brokedown Palace"
    
    // Multiple shows same date/venue
    val showSequence: Int = 1,    // 1, 2, 3... for multiple shows
    
    // Library status (no separate table needed)
    val isInLibrary: Boolean = false,
    val libraryAddedAt: Long?,        // When added to library
    
    // Computed/cached stats
    val recordingCount: Int = 0,
    val bestRecordingId: String?,
    val averageRating: Float?,
    val totalReviews: Int = 0,
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Venue
```kotlin
@Entity(tableName = "venues_v2")
data class VenueV2Entity(
    @PrimaryKey
    val venueId: String,          // UUID
    
    // Venue identity
    val name: String,             // "Cornell University"
    val normalizedName: String,   // "cornell university" (keep for now)
    val aliases: String?,         // "Fillmore West,The Fillmore,Fillmore Auditorium" (comma-separated)
    val url: String?,             // Wikipedia URL or venue website
    
    // Location hierarchy
    val city: String?,            // "Ithaca"
    val state: String?,           // "NY"
    val country: String = "USA",
    
    // Statistics (cached for performance)
    val showCount: Int = 0,
    val firstShowDate: String?,   // "1977-05-08"
    val lastShowDate: String?,    // "1995-07-09"
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Recording
```kotlin
@Entity(tableName = "recordings_v2")
data class RecordingV2Entity(
    @PrimaryKey
    val recordingId: String,      // Archive.org identifier
    
    // Show relationship
    val showId: String,           // FK to shows_v2
    
    // Basic recording info
    val title: String,            // Archive.org title
    val source: String?,          // "SBD", "AUD", "MATRIX", "FM"
    
    // Quality metrics (if available)
    val averageRating: Float?,
    val reviewCount: Int = 0,
    
    // Archive.org metadata
    val archiveUrl: String,       // Full Archive.org URL
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Track
```kotlin
@Entity(
    tableName = "tracks_v2",
    primaryKeys = ["trackNumber", "recordingId", "format"]
)
data class TrackV2Entity(
    // Composite primary key
    val trackNumber: Int,         // 1, 2, 3... (logical track number)
    val recordingId: String,      // FK to recordings_v2
    val format: String,           // "mp3", "flac", "ogg"
    
    // Track metadata
    val setNumber: Int,           // 1, 2, 3 (first set, second set, encore)
    val filename: String,         // "gd77-05-08d1t01.mp3"
    val title: String?,           // "Scarlet Begonias"
    
    // Audio metadata
    val duration: String?,        // "08:23"
    val durationSeconds: Int?,    // 503
    val bitrate: String?,         // "128kbps"
    val fileSize: Long?,          // Bytes
    
    // URLs and download tracking
    val streamUrl: String,        // Archive.org stream URL
    val downloadUrl: String,      // Archive.org download URL
    val isDownloaded: Boolean = false,
    val downloadedFilePath: String?,  // Local file path if downloaded
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

### Collection System

#### Collection
```kotlin
@Entity(tableName = "collections_v2")
data class CollectionV2Entity(
    @PrimaryKey
    val collectionId: String,     // "guest-ornette-coleman"
    
    // Collection metadata
    val name: String,             // "Ornette Coleman Guest Shows"
    val description: String?,     // "Shows featuring jazz legend..."
    val tags: String?,            // "guest,ornette-coleman,official,live"
    
    // Display assets
    val coverImageUrl: String?,   // Primary cover image
    val thumbnailUrl: String?,    // Small thumbnail  
    val bannerUrl: String?,       // Wide banner
    
    // Ordering and display
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    
    // Statistics (pre-computed)
    val showCount: Int = 0,       // Count from junction table
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Collection-Show Relationship
```kotlin
@Entity(
    tableName = "collection_shows_v2",
    primaryKeys = ["collectionId", "showId"]
)
data class CollectionShowV2Entity(
    val collectionId: String,     // FK to collections_v2
    val showId: String,           // FK to shows_v2
    val sequenceNumber: Int,      // Order within collection
    val createdAt: Long
)
```

### User Data

#### User Reviews (MVP - eventually sync to Archive)
```kotlin
@Entity(tableName = "user_reviews_v2")
data class UserReviewV2Entity(
    @PrimaryKey
    val reviewId: String,         // UUID
    
    // Show relationship
    val showId: String,           // FK to shows_v2
    
    // Review content
    val rating: Int?,             // 1-5 stars
    val notes: String?,           // User's review text
    val tags: String?,            // User tags
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

#### User Activity Tracking

The V2 user activity tracking system provides Spotify-like resume functionality and comprehensive listening analytics. It's designed around intent-based sessions and non-invasive state restoration.

##### Current Playback State (Resume System)
```kotlin
@Entity(tableName = "current_playback_v2")
data class CurrentPlaybackV2Entity(
    @PrimaryKey 
    val id: String = "singleton",        // Always "singleton" - one record only
    
    // Current position
    val showId: String?,                 // FK to shows_v2
    val recordingId: String?,            // FK to recordings_v2  
    val trackNumber: Int?,               // Current track (1, 2, 3...)
    val format: String?,                 // "mp3", "flac", "ogg" - critical for track lookup
    val positionSeconds: Int = 0,        // Position within track
    
    // Queue state for UI restore
    val queueJson: String?,              // JSON snapshot of queue
    val queuePosition: Int = 0,          // Current position in queue
    val shuffleMode: Boolean = false,
    val repeatMode: String = "NONE",     // "NONE", "ONE", "ALL"
    
    // Context for resume (enables app state restoration)
    val contextType: String?,            // "SHOW", "COLLECTION", "LIBRARY"  
    val contextId: String?,              // ID of what triggered session
    
    // State tracking
    val isActuallyPlaying: Boolean = false, // vs just UI state
    val lastUpdatedAt: Long,
    val createdAt: Long
)
```

##### Show Listening Analytics
```kotlin
@Entity(tableName = "show_playthroughs_v2")
data class ShowPlaythroughV2Entity(
    @PrimaryKey
    val playthroughId: String,           // UUID
    
    // Show/recording being played
    val showId: String,                  // FK to shows_v2
    val recordingId: String,             // FK to recordings_v2
    
    // Playthrough session
    val startedAt: Long,                 // When user started this listen
    val completedAt: Long?,              // When finished or switched shows
    val totalListenTime: Long = 0,       // Actual seconds listened (excluding pauses)
    
    // Progress tracking
    val furthestTrack: Int = 1,          // Highest track number reached
    val tracksCompleted: Int = 0,        // Tracks listened to >80%
    val completionPercentage: Float = 0f, // Overall show completion
    val isCompleted: Boolean = false,    // Finished entire show
    
    // Context
    val startedFrom: String?,            // "LIBRARY", "SEARCH", "COLLECTION", etc.
    val startedFromId: String?,          // ID of starting context
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)
```

##### Track Play Events (Optional - for detailed analytics)
```kotlin
@Entity(tableName = "track_plays_v2")  
data class TrackPlayV2Entity(
    @PrimaryKey
    val playId: String,                  // UUID
    
    // Track being played
    val playthroughId: String,           // FK to show_playthroughs_v2
    val trackNumber: Int,                // Which track (1, 2, 3...)
    val recordingId: String,             // FK to recordings_v2 (denormalized)
    val showId: String,                  // FK to shows_v2 (denormalized)
    
    // Play event
    val startedAt: Long,                 // When track play started
    val endedAt: Long?,                  // When track play ended
    val startPosition: Int = 0,          // Starting position in seconds
    val endPosition: Int?,               // Ending position in seconds
    val wasCompleted: Boolean = false,   // Listened to >80% of track
    
    // Play context
    val playSource: String,              // "AUTO_NEXT", "USER_SKIP", "RESUME"
    
    // Timestamps
    val createdAt: Long
)
```

### User Activity Tracking Architecture

#### Resume System Design
The resume system uses an intent-based approach rather than time-based sessions:

**Resume vs New Playthrough Triggers:**
- **Resume**: User hits play on mini-player, clicks resume UI, restarts current track
- **New Playthrough**: User selects different show, picks specific track of same show

**App Lifecycle Flow:**
1. **App Start**: Load current state, restore UI (mini-player shows paused at correct position)
2. **During Playback**: Position updates every 5-10 seconds, immediate updates on track changes
3. **Queue Changes**: Event-driven updates to queueJson (rare)
4. **App Background**: Save current state without interfering with other apps

#### Media3 Integration Strategy
```
User Actions: UI → QueueManager → Media3
Media3 Events: Media3 → QueueManager → Database Updates

Queue Management:
- QueueManager handles business logic (recording selection, format preference)
- Media3 handles actual playback queue and audio
- Bidirectional sync keeps both in alignment
```

#### Analytics Foundation
The system tracks:
- **Show completion patterns**: Which shows get finished vs abandoned
- **Listening preferences**: Preferred years, venues, recording sources
- **Progress insights**: User's completion rate, favorite songs
- **Context analysis**: How users discover and navigate content

#### Performance Characteristics
- **CurrentPlaybackV2Entity**: 1 record (singleton pattern)
- **ShowPlaythroughV2Entity**: ~1 per show listen session
- **TrackPlayV2Entity**: Optional detailed tracking (can be disabled for performance)

Position updates are batched (every 5-10 seconds) while critical state changes (track transitions, queue modifications) update immediately.

## Relationships and Indices

### Primary Relationships
- Show → Venue (many-to-one)
- Recording → Show (many-to-one)
- Track → Recording (many-to-one)
- Collection ↔ Show (many-to-many via collection_shows_v2)
- Library → Show (many-to-one)
- TrackPlay → Track, Session (many-to-one)

### Critical Indices
```sql
-- Show search queries (primary use cases)
CREATE INDEX idx_shows_year ON shows_v2(year);                    -- "1977" searches
CREATE INDEX idx_shows_year_month ON shows_v2(yearMonth);         -- "1977-05" searches  
CREATE INDEX idx_shows_date ON shows_v2(date);                    -- "1977-05-08" searches
CREATE INDEX idx_shows_city ON shows_v2(city);                    -- City searches
CREATE INDEX idx_shows_state ON shows_v2(state);                  -- State searches
CREATE INDEX idx_shows_venue ON shows_v2(venueId);                -- Venue searches
CREATE INDEX idx_shows_songs ON shows_v2(songList);               -- Song searches

-- Recording quality queries
CREATE INDEX idx_recordings_show_rating ON recordings_v2(showId, averageRating DESC);
CREATE INDEX idx_recordings_source ON recordings_v2(source, averageRating DESC);

-- Library queries
CREATE INDEX idx_library_added_at ON library_v2(addedAt DESC);
CREATE INDEX idx_library_show_id ON library_v2(showId);

-- Activity tracking queries
CREATE INDEX idx_track_plays_session ON track_plays_v2(sessionId, startedAt);
CREATE INDEX idx_track_plays_show_date ON track_plays_v2(showId, startedAt DESC);
CREATE INDEX idx_sessions_started_at ON listen_sessions_v2(startedAt DESC);

-- Collection queries
CREATE INDEX idx_collection_shows_collection ON collection_shows_v2(collectionId, sequenceNumber);
CREATE INDEX idx_collection_shows_show ON collection_shows_v2(showId);
```

## Key Design Principles

### 1. Date-Centric Organization
- Shows are organized primarily by date
- Fast date-range queries for timeline navigation
- Era-based filtering and grouping

### 2. Denormalization for Performance
- Critical foreign keys duplicated in activity tables
- Computed statistics cached in parent entities
- Frequently-accessed data pre-calculated

### 3. Comprehensive Activity Tracking
- Every user interaction tracked for recommendations
- Session-based organization for context
- Resume functionality built into data model

### 4. Clean Domain Boundaries
- Clear separation between core entities and user data
- Collection system separate from core show data
- Activity tracking isolated from core entities

### 5. Future-Proof Design
- Extensible JSON fields for metadata
- Version-aware entity design
- Migration-friendly structure

## Migration Strategy

### Phase 1: Parallel Implementation
- Build V2 entities alongside V1
- Dual-write to both systems during transition
- V2 features use V2 data, V1 features use V1 data

### Phase 2: Data Migration
- Bulk migrate core entities (Show, Recording, Track)
- Migrate user libraries with preserved timestamps
- Migrate listening history where possible

### Phase 3: Feature Migration
- Update features one by one to use V2 data
- Maintain backward compatibility during transition
- Remove V1 data after all features migrated

### Phase 4: Cleanup
- Remove V1 entities and code
- Optimize V2 indices and queries
- Performance testing and optimization

This architecture provides a solid foundation for the V2 data model while maintaining the flexibility to evolve as requirements change.