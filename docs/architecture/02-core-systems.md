# Core Systems Architecture

## Data Pipeline & Processing

### Setlist Data Pipeline
The application includes a sophisticated 4-stage data processing pipeline for integrating Grateful Dead setlist data from multiple sources:

#### Stage 1: Raw Data Collection
- **CMU Setlists (1972-1995)**: Complete setlist data from CS.CMU.EDU covering post-Pigpen era
- **GDSets Data (1965-1971)**: Early years setlist data filling the pre-1972 gap
- **Archive.org Metadata**: 17,790+ recording metadata files and 3,252+ show metadata files

#### Stage 2: Data Processing
- **Venue Normalization**: Handles international venues, US venue variations (Theater/Theatre), country mappings
- **Song Processing**: Commentary filtering, segue notation normalization, frequency statistics
- **Quality Control**: 99.995% song match rate, 100% venue matching

#### Stage 3: Integration
- **GDSets-First Merge**: GDSets data takes priority over CMU for overlapping shows
- **ID Standardization**: Show ID matching using standardized YYYY-MM-DD format
- **Comprehensive Linking**: Venue and song ID integration with error logging

#### Stage 4: App Deployment
- **Data Packaging**: All metadata bundled into compressed data.zip (ratings.json, setlists.json, songs.json, venues.json)
- **Mobile Optimization**: Optimized compression for app deployment

**Data Quality Metrics**:
- Total Setlists: 2,200+ shows (1965-1995)
- Venue Coverage: 484+ unique venues
- Song Database: 550+ unique songs with aliases
- Match Rates: 99.995% songs, 100% venues

### Archive.org API Integration

#### API Architecture
- **Base URL**: `https://archive.org/`
- **Primary Endpoints**: 
  - `/advancedsearch.php` - Concert search and discovery
  - `/metadata/{identifier}` - Detailed recording metadata
  - `/download/{identifier}/{filename}` - Direct file streaming

#### Response Handling Strategy
- **Flexible Serialization**: Custom `FlexibleStringSerializer` handles API inconsistencies
- **Error Resilience**: Comprehensive error handling with fallbacks
- **Rate Limiting**: Exponential backoff with proper User-Agent headers
- **Caching Strategy**: Database-first with API fallback for performance

#### API Quirks Management
- **Inconsistent Field Types**: Defensive parsing for fields that can be strings or arrays
- **String/Number Conversion**: Handles numerical fields returned as strings
- **Missing Field Handling**: Graceful handling of null/missing fields
- **Response Size Management**: Pagination with `rows` and `start` parameters

### Rating System Architecture

#### Multi-Tier Rating Calculation
```kotlin
// Rating strategy hierarchy:
1. SBD recordings with 3+ reviews (highest priority)
2. Any recording with 5+ reviews
3. Weighted average of all ratings (fallback)
```

#### Rating Data Flow
- **Recording Level**: Individual Archive.org ratings and reviews
- **Show Level**: Aggregated ratings from multiple recordings
- **Confidence Scoring**: Review count influences rating reliability
- **Source Preference**: SBD > MATRIX > FM > AUD quality hierarchy

#### Review Integration
- **Individual Reviews**: Reviewer info, star rating, title, body, date
- **Aggregation Logic**: Weighted averages favoring higher review counts
- **Quality Metrics**: Review count thresholds for rating reliability

## Caching & Performance Strategy

### Complete Dataset Architecture
**Philosophy**: "The cache is a feature of the application, not the user interface"

#### Implementation Approach
- **Full Catalog Download**: Complete Grateful Dead catalog cached locally on first launch
- **Offline-First**: All searches become local-only with precise matching
- **Background Sync**: Periodic refresh without interrupting user experience
- **Consistent Results**: 100% identical results for identical queries

#### Cache Performance Targets
- **Search Speed**: <100ms for all local searches (Netflix-like performance)
- **Offline Capability**: Full functionality without network connection
- **Data Size**: ~2GB for complete processed dataset
- **Sync Strategy**: Weekly/monthly background refresh

### Database Performance Optimization

#### Strategic Indexing
```sql
CREATE INDEX idx_concerts_date ON concerts(date);        -- Date-based queries
CREATE INDEX idx_concerts_venue ON concerts(venue);      -- Venue searches  
CREATE INDEX idx_concerts_songNames ON concerts(songNames); -- Full-text search
CREATE INDEX idx_concerts_year ON concerts(year);        -- Year filtering
```

#### Query Optimization Patterns
- **Precise Date Matching**: `WHERE date = '1993-05-16'` instead of `LIKE '%1993-05-16%'`
- **Field-Specific Search**: Separate domains for dates, venues, locations
- **Denormalized Fields**: Song names stored as searchable JSON for performance

## Media Playback Architecture

### Service-Oriented Media Architecture
The `:core:media` module implements a service-oriented architecture with MediaControllerRepository coordinating three focused services:

```
UI Components → MediaControllerRepository → Service Composition:
                   ↓                       • MediaServiceConnector (connection)
                   ↓                       • PlaybackStateSync (state flows)  
                   ↓                       • PlaybackCommandProcessor (commands)
                   ↓                                ↓
               MediaController ↔ DeadArchivePlaybackService
                                          ↓
                                   ExoPlayer + MediaSession
```

#### Media Service Components
- **MediaServiceConnector**: Connection lifecycle and service binding management
- **PlaybackStateSync**: StateFlow synchronization with MediaId tracking for UI highlighting
- **PlaybackCommandProcessor**: Command processing and queue operations
- **MediaControllerRepository**: Facade coordinator maintaining clean public interface

#### Background Service Management
- **Foreground Service**: Continuous playback with rich notifications
- **MediaSession Integration**: System media controls and lock screen support
- **Service Communication**: MediaController pattern for UI-service interaction
- **Connection Management**: Robust service connection with timeout handling

#### Queue Management System
- **Native Media3 Queue**: Leverages Media3's native playlist management
- **Smart Queue Building**: Complete show loading with track progression via QueueManager
- **MediaItem Conflict Resolution**: QueueManager uses `updatePlaybackStateSyncOnly()` to prevent service conflicts
- **Position Tracking**: Periodic position updates with service synchronization
- **Queue Context**: Metadata enrichment for notifications and controls

#### Offline Playback Support
```kotlin
// Local file resolution with fallback to streaming
private suspend fun resolvePlaybackUrl(originalUrl: String): String? {
    val localFileUri = localFileResolver.resolveLocalFile(originalUrl, recordingId)
    return if (localFileUri != null) {
        Log.i(TAG, "🎵 OFFLINE PLAYBACK: Using local file")
        localFileUri
    } else {
        originalUrl // Fall back to streaming
    }
}
```

#### State Management
- **Single Source of Truth**: All UI state flows from MediaController via PlaybackStateSync
- **Reactive Architecture**: StateFlow-based UI updates across the application
- **MediaId-Based Track Matching**: Stable track identification using MediaItem.mediaId instead of URL parsing
- **Position Synchronization**: Regular sync between service and UI state
- **Error Recovery**: Graceful handling of service disconnection and reconnection
- **Feedback Loop Prevention**: QueueStateManager uses distinctUntilChanged to prevent duplicate emissions

### Critical Bug Fixes
**Media Player Looping Resolution**:
- **Root Cause**: QueueManager and PlaybackCommandProcessor both creating MediaItems
- **Solution**: QueueManager calls `updatePlaybackStateSyncOnly()` to bypass conflicting sync
- **Impact**: Eliminates rapid URL switching that caused start/stop looping behavior

**Downloaded Track Highlighting Fix**:
- **Root Cause**: URL mismatch between local file paths and download URLs in track matching
- **Solution**: MediaId-based matching using stable `MediaItem.mediaId` (original downloadUrl)
- **Impact**: Consistent track highlighting for both streaming and downloaded content

### Unified Library Service Architecture
**Centralized Library Management**:
- **LibraryService**: Single service for all library operations across features
- **LibraryButton**: Reusable UI component with unified add/remove/cleanup actions
- **Download Integration**: Library removal supports optional download cleanup
- **Feature Migration**: All ViewModels (Browse, Library, Player, Playlist) use unified service

### Last Played Track System
**Spotify-like Resume Functionality**:
- **Track Position Persistence**: Automatic saving of playback position
- **App Restart Recovery**: Seamless resume of last played track
- **Queue State Restoration**: Complete queue context restoration
- **Background Monitoring**: Continuous playback state tracking

## Performance Characteristics

### Memory Management
- **StateFlow Efficiency**: Minimal memory footprint for reactive state
- **Queue Optimization**: Smart memory management for large playlists
- **Image Caching**: Efficient album art and show image caching
- **Background Processing**: Memory-conscious background operations

### Network Efficiency
- **Request Deduplication**: Prevents duplicate API calls
- **Response Caching**: HTTP-level caching with appropriate headers
- **Retry Logic**: Exponential backoff for failed requests
- **Bandwidth Adaptation**: Quality selection based on network conditions

### Database Performance
- **Connection Pooling**: Efficient database connection management
- **Query Optimization**: Strategic indexing for fast searches
- **Batch Operations**: Bulk inserts and updates for large datasets
- **Cache Expiry**: Timestamp-based cache invalidation

## Integration Points

### Cross-System Communication
- **Repository Coordination**: Data repositories orchestrate between local/remote sources
- **Event-Driven Updates**: Reactive flows connect all architectural layers
- **Service Integration**: Media system uses show/recording data from repositories
- **State Synchronization**: Consistent state management across all components

### Error Handling Strategy
- **Graceful Degradation**: System continues functioning when individual components fail
- **User-Friendly Errors**: Clear error messages with actionable feedback
- **Retry Mechanisms**: Automatic recovery for transient failures
- **Offline Support**: Full functionality available without network connectivity

This core systems architecture provides the foundation for a robust, scalable Android application that efficiently handles complex data processing, media playback, and user interactions while maintaining excellent performance characteristics.