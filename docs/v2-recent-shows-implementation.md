# V2 Recent Shows Implementation Plan

## Overview

This document outlines the implementation plan for tracking recently played shows in the V2 architecture. The system will capture user listening behavior and provide a reactive list of recently played shows for the HomeScreen.

## Architecture Decision

**Real Implementation** - Build production-ready service directly, no stubs needed.

**UPSERT Strategy** - Maintain single record per show in database, update timestamp and increment play count on each play. This eliminates need for complex GROUP BY queries.

**Smart Play Detection** - Observe track-level StateFlows with smart filtering (10 seconds MAX, 25% only for short tracks) to avoid rapid-skip noise.

## Core Components

### 1. Database Layer

**RecentShowEntity** - Single table with UPSERT pattern:
```kotlin
@Entity(tableName = "recent_shows")
data class RecentShowEntity(
    @PrimaryKey val showId: String,
    val lastPlayedTimestamp: Long,    // Updated each play
    val firstPlayedTimestamp: Long,   // Set once on first play  
    val totalPlayCount: Int           // Incremented each play
)
```

**RecentShowDao** - Simple queries without GROUP BY:
```kotlin
// Main query - simple ORDER BY since UPSERT ensures one record per show
@Query("SELECT * FROM recent_shows ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
suspend fun getRecentShows(limit: Int = 8): List<RecentShowEntity>

// UPSERT support queries
@Query("SELECT * FROM recent_shows WHERE showId = :showId")
suspend fun getShowById(showId: String): RecentShowEntity?

@Insert
suspend fun insert(entity: RecentShowEntity)

@Query("UPDATE recent_shows SET lastPlayedTimestamp = :timestamp, totalPlayCount = :playCount WHERE showId = :showId")
suspend fun updateShow(showId: String, timestamp: Long, playCount: Int)

@Query("DELETE FROM recent_shows")
suspend fun clearAll()
```

### 2. Service Layer

**RecentShowsService Interface**:
```kotlin
interface RecentShowsService {
    val recentShows: StateFlow<List<Show>>
    suspend fun recordShowPlay(showId: String, timestamp: Long = System.currentTimeMillis())
    suspend fun clearRecentShows()
}
```

**RecentShowsServiceImpl Implementation**:

Key responsibilities:
- Subscribe to MediaControllerRepository StateFlows
- Apply smart filtering (10 seconds MAX, 25% only for short tracks)
- Execute UPSERT logic for database updates
- Provide reactive StateFlow of recent shows for UI consumption

Core logic:
```kotlin
// UPSERT - much simpler without GROUP BY complexity
private suspend fun recordShowPlay(showId: String, timestamp: Long) {
    val existing = recentShowDao.getShowById(showId)
    if (existing != null) {
        // Update existing record
        recentShowDao.updateShow(showId, timestamp, existing.totalPlayCount + 1)
    } else {
        // Insert new record
        recentShowDao.insert(RecentShowEntity(showId, timestamp, timestamp, 1))
    }
}

// Hybrid play detection to avoid rapid-skip noise
private fun observePlayback() {
    combine(
        mediaControllerRepository.currentTrackInfo,
        mediaControllerRepository.playbackStatus,
        mediaControllerRepository.isPlaying
    ) { trackInfo, status, playing ->
        if (trackInfo != null && playing) {
            // Record play after 10 seconds MAX (25% only for very short tracks)
            val meaningfulPlay = if (status.duration > 40_000L) {
                status.currentPosition > 10_000L
            } else {
                status.currentPosition > minOf(status.duration * 0.25f, 10_000L)
            }
            
            if (meaningfulPlay) {
                extractShowIdFromTrack(trackInfo)?.let { recordShowPlay(it) }
            }
        }
    }.collect()
}
```

## Integration Points

### MediaController Integration

The service subscribes to MediaControllerRepository StateFlows rather than being embedded in the MediaController. This maintains clean separation of concerns:

- **MediaControllerRepository** - Focuses purely on playback control
- **RecentShowsService** - Observes playback state and handles analytics

This approach captures all play entry points:
- Full recording playback (`playAll()`)
- Individual track plays from playlist
- Queue navigation between shows
- Resume playback scenarios
- Any other play mechanism

### HomeService Integration

Replace mock recent shows data with reactive StateFlow:

```kotlin
@Singleton
class HomeServiceStub @Inject constructor(
    private val recentShowsService: RecentShowsService,
    private val showRepository: ShowRepository
) : HomeService {
    
    override val homeContent: StateFlow<HomeContent> = combine(
        recentShowsService.recentShows,
        loadTodayInHistoryFlow(),
        loadFeaturedCollectionsFlow()
    ) { recentShows, todayInHistory, collections ->
        HomeContent(
            recentShows = recentShows,
            todayInHistory = todayInHistory, 
            featuredCollections = collections,
            lastRefresh = System.currentTimeMillis()
        )
    }.stateIn(serviceScope, SharingStarted.WhileSubscribed(), HomeContent.initial())
}
```

## Database Setup

### Migration SQL
```sql
CREATE TABLE recent_shows (
    showId TEXT PRIMARY KEY NOT NULL,
    lastPlayedTimestamp INTEGER NOT NULL,
    firstPlayedTimestamp INTEGER NOT NULL,
    totalPlayCount INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_recent_shows_timestamp ON recent_shows(lastPlayedTimestamp DESC);
```

### Module Configuration

**New Module**: `v2/core/recent/`
- Add to `settings.gradle.kts`
- Create `build.gradle.kts` with dependencies on database, domain, media modules

**Hilt Bindings**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RecentModule {
    
    @Binds
    abstract fun bindRecentShowsService(
        impl: RecentShowsServiceImpl
    ): RecentShowsService
}
```

## Implementation Benefits

### Technical Benefits
- **Simple & Efficient**: UPSERT pattern eliminates complex GROUP BY queries
- **Comprehensive Tracking**: Captures all play entry points automatically
- **Smart Filtering**: Hybrid threshold prevents rapid-skip noise while handling short tracks
- **Real-time Reactive**: HomeScreen updates immediately via StateFlow
- **Clean Architecture**: Proper separation between playback control and analytics

### User Experience Benefits
- **Persistent History**: Recent shows survive app restarts
- **Accurate Tracking**: Only meaningful listens are recorded
- **Deduplication**: Each show appears once, ordered by most recent play
- **Performance**: Efficient queries with indexed timestamp sorting

### Development Benefits
- **Foundation for Analytics**: Clean base for future stats/analytics features
- **Testable**: Clear service boundaries enable focused unit testing
- **Scalable**: Database design ready for additional analytics tables

## Implementation Sequence

1. **Database Foundation**
   - Create RecentShowEntity and RecentShowDao
   - Add database migration
   - Set up module structure

2. **Service Implementation**
   - Create RecentShowsService interface
   - Implement RecentShowsServiceImpl with hybrid filtering
   - Add Hilt bindings

3. **HomeService Integration**
   - Replace mock data with RecentShowsService StateFlow
   - Test reactive updates

4. **Module Configuration**
   - Update build dependencies
   - Configure Hilt modules
   - Database version increment

5. **Testing & Validation**
   - Test various play scenarios
   - Verify filtering logic
   - Confirm UI reactivity

## Future Considerations

This implementation provides foundation for:
- **Play Analytics**: Total listening time, most played shows, listening patterns
- **Recommendations**: "Similar to recently played" suggestions
- **Backend Sync**: Easy to add API calls for cloud analytics
- **User Controls**: Privacy settings, history management
- **Advanced Filtering**: Hide shows, favorite recent shows, etc.

The clean service architecture makes these extensions straightforward without requiring architectural changes.