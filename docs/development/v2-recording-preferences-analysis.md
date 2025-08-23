# V2 User Recording Preferences Implementation Analysis

*Analysis Date: August 23, 2025*  
*System: V2 Playlist Architecture*  
*Context: Evaluating complexity of adding user recording preferences to the current `bestRecordingId` system*

## Executive Summary

The V2 playlist system is **well-architected** for adding user recording preferences with **moderate implementation complexity (6/10)**. The current design uses clean service boundaries and abstracted recording ID resolution, making this feature a natural extension rather than a architectural overhaul.

**Estimated Implementation Effort: 1-2 weeks**

---

## Current Architecture Analysis

### Recording ID Flow

The system currently uses a simple, clean flow for determining which recording to load:

```kotlin
// Service State
private var currentShow: Show? = null
private var currentRecordingId: String? = null

// Flow 1: Initial Show Loading
override suspend fun loadShow(showId: String?) {
    currentShow = showRepository.getShowById(showId)
    currentRecordingId = currentShow!!.bestRecordingId  // Always use "best"
}

// Flow 2: Navigation Updates
override suspend fun navigateToNextShow() {
    val nextShow = showRepository.getNextShowByDate(current.date)
    currentShow = nextShow
    currentRecordingId = nextShow.bestRecordingId  // Always use "best"
}

// Flow 3: Track Loading
override suspend fun getTrackList(): List<PlaylistTrackViewModel> {
    val recordingId = currentRecordingId  // Uses the resolved recording ID
    val result = archiveService.getRecordingTracks(recordingId)
    trackCache[recordingId] = tracks  // Cache by recording ID
}
```

### Key Domain Models

**Show Domain Model:**
```kotlin
data class Show(
    val recordingIds: List<String>,      // All available recordings 
    val bestRecordingId: String?,        // System-recommended "best" recording
    val recordingCount: Int,             // How many recordings available
    // ... other fields
)
```

**Architecture Strengths:**
- ✅ **Single Point of Control**: `currentRecordingId` is set in exactly 2 methods
- ✅ **Clean Abstraction**: ViewModels never see recording IDs, only track data
- ✅ **Cache Compatibility**: Cache uses `recordingId` as key, naturally supports any recording
- ✅ **Service Boundaries**: Recording resolution logic contained in service layer

---

## User Preferences Implementation Plan

### 1. Data Layer Requirements

**New Data Structure:**
```kotlin
@Entity(tableName = "user_recording_preferences")
data class UserRecordingPreference(
    @PrimaryKey val showId: String,      // e.g. "1977-05-08"
    val recordingId: String,             // e.g. "gd1977-05-08.sbd.hicks.4982"
    val setAt: Long                      // Timestamp when preference was set
)

@Dao
interface UserRecordingPreferencesDao {
    @Query("SELECT recordingId FROM user_recording_preferences WHERE showId = :showId")
    suspend fun getPreferredRecordingId(showId: String): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreferredRecording(preference: UserRecordingPreference)
    
    @Query("DELETE FROM user_recording_preferences WHERE showId = :showId")
    suspend fun clearPreference(showId: String)
}
```

**Repository Layer:**
```kotlin
@Singleton
class UserRecordingPreferencesRepository @Inject constructor(
    private val dao: UserRecordingPreferencesDao
) {
    suspend fun getPreferredRecording(showId: String): String? = 
        dao.getPreferredRecordingId(showId)
    
    suspend fun setPreferredRecording(showId: String, recordingId: String) {
        dao.setPreferredRecording(
            UserRecordingPreference(showId, recordingId, System.currentTimeMillis())
        )
    }
    
    suspend fun clearPreference(showId: String) = dao.clearPreference(showId)
}
```

### 2. Service Layer Changes

**New Recording Resolution Service:**
```kotlin
// Add to PlaylistServiceImpl
@Inject
private lateinit var userPreferencesRepo: UserRecordingPreferencesRepository

private suspend fun getPreferredRecordingId(show: Show): String? {
    // Priority: User preference > bestRecordingId > first available > null
    return userPreferencesRepo.getPreferredRecording(show.id)
        ?: show.bestRecordingId
        ?: show.recordingIds.firstOrNull()
}
```

**Updated Navigation Logic:**
```kotlin
// Modified methods (only 2 changes needed)
override suspend fun loadShow(showId: String?) {
    currentShow = showRepository.getShowById(showId)
    currentRecordingId = getPreferredRecordingId(currentShow!!)  // NEW
}

override suspend fun navigateToNextShow() {
    val nextShow = showRepository.getNextShowByDate(current.date)
    currentShow = nextShow
    currentRecordingId = getPreferredRecordingId(nextShow)  // NEW
}
```

**Enhanced API Methods:**
```kotlin
// Update existing methods
override suspend fun setRecordingAsDefault(recordingId: String) {
    currentShow?.let { show ->
        // Store user preference
        userPreferencesRepo.setPreferredRecording(show.id, recordingId)
        // Update current session
        currentRecordingId = recordingId
        // Invalidate cache since user changed recording
        trackCache.remove(recordingId)
        Log.d(TAG, "Set recording $recordingId as default for ${show.displayTitle}")
    }
}

override suspend fun resetToRecommended() {
    currentShow?.let { show ->
        // Clear user preference
        userPreferencesRepo.clearPreference(show.id)
        // Reset to system recommendation
        currentRecordingId = show.bestRecordingId
        Log.d(TAG, "Reset ${show.displayTitle} to recommended recording")
    }
}
```

### 3. Prefetch System Updates

**Enhanced Prefetch Logic:**
```kotlin
private suspend fun startAdjacentPrefetch() {
    currentShow?.let { current ->
        coroutineScope.launch {
            try {
                val nextShow = showRepository.getNextShowByDate(current.date)
                val prevShow = showRepository.getPreviousShowByDate(current.date)
                
                // Prefetch with user preferences considered
                nextShow?.let { show ->
                    val recordingId = getPreferredRecordingId(show)  // Lookup preference
                    if (recordingId != null && !trackCache.containsKey(recordingId)) {
                        prefetchRecording(show, recordingId, "next")
                    }
                }
                
                prevShow?.let { show ->
                    val recordingId = getPreferredRecordingId(show)  // Lookup preference
                    if (recordingId != null && !trackCache.containsKey(recordingId)) {
                        prefetchRecording(show, recordingId, "previous")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in adjacent prefetch with preferences", e)
            }
        }
    }
}

private suspend fun prefetchRecording(show: Show, recordingId: String, direction: String) {
    if (prefetchJobs[recordingId]?.isActive == true) {
        Log.d(TAG, "Prefetch already active for recording: $recordingId")
        return
    }
    
    Log.d(TAG, "Starting $direction prefetch for ${show.displayTitle} (recording: $recordingId)")
    
    val job = coroutineScope.launch {
        try {
            val result = archiveService.getRecordingTracks(recordingId)
            if (result.isSuccess) {
                val tracks = result.getOrNull() ?: emptyList()
                val filteredTracks = filterPreferredAudioTracks(tracks)
                trackCache[recordingId] = tracks  // Cache by actual recording ID
                Log.d(TAG, "Prefetch completed for ${show.displayTitle}: ${filteredTracks.size} tracks")
            }
        } finally {
            prefetchJobs.remove(recordingId)
        }
    }
    
    prefetchJobs[recordingId] = job
}
```

---

## Implementation Complexity Assessment

### Difficulty Rating: **6/10 (Moderate)**

#### Easy Components ✅
- **Service Architecture Compatibility**: Current design supports this naturally
- **API Interface Stability**: No changes needed to public API methods
- **Cache System Integration**: Recording ID is already the cache key 
- **Navigation Logic Isolation**: Only 2 methods need core changes
- **Clean Separation**: Preferences stay in service layer, not ViewModels

#### Moderate Complexity 🔶
- **New Data Layer**: Room table, DAO, Repository implementation needed
- **Preference Resolution Logic**: Implementing priority system (user > best > fallback)
- **Cache Invalidation Strategy**: When user changes preference, invalidate old recording's cache
- **Prefetch Enhancement**: Must lookup preferences for multiple shows efficiently
- **UI Integration**: Recording selection sheet needs persistence wiring

#### Potential Challenges ⚠️
- **Migration Considerations**: Existing users need seamless default behavior
- **Race Conditions**: User changing preference during active prefetch operations
- **Performance Impact**: Preference lookups during navigation (should be fast with proper indexing)
- **Edge Cases**: Handling preferences for recordings that no longer exist

---

## Implementation Roadmap

### Phase 1: Foundation (3-4 days)
1. **Add Database Schema**: Create `UserRecordingPreference` entity and DAO
2. **Create Repository**: Implement `UserRecordingPreferencesRepository`
3. **Update Database Migration**: Add table creation to database migrations
4. **Write Unit Tests**: Test preference storage and retrieval

### Phase 2: Service Integration (2-3 days)
1. **Add Recording Resolution**: Implement `getPreferredRecordingId()` method
2. **Update Navigation Methods**: Modify `loadShow()` and navigation methods
3. **Enhance API Methods**: Complete `setRecordingAsDefault()` and `resetToRecommended()`
4. **Add Cache Invalidation**: Ensure cache updates when preferences change

### Phase 3: Prefetch Enhancement (2-3 days)
1. **Update Prefetch Logic**: Modify `startAdjacentPrefetch()` to use preferences
2. **Add Preference-Aware Caching**: Ensure prefetch respects user choices
3. **Handle Race Conditions**: Add proper synchronization for concurrent operations
4. **Performance Testing**: Verify preference lookups don't slow navigation

### Phase 4: UI Integration (1-2 days)
1. **Wire Recording Selection**: Connect UI to `setRecordingAsDefault()`
2. **Add Preference Indicators**: Show user which recordings have preferences set
3. **Test User Flows**: Verify end-to-end preference setting and navigation

### Phase 5: Testing & Polish (1-2 days)
1. **Integration Testing**: Test navigation with mixed preferred/non-preferred shows
2. **Performance Validation**: Ensure no regression in navigation speed
3. **Edge Case Testing**: Handle missing recordings, invalid preferences
4. **User Experience Testing**: Verify intuitive behavior

---

## Architectural Advantages

The current V2 architecture provides several advantages for this enhancement:

### 1. **Single Point of Control**
Recording ID resolution happens in exactly 2 methods, making changes surgical and testable.

### 2. **Clean Service Boundaries** 
User preferences stay in the service layer, maintaining clean architecture. ViewModels continue to work with abstract track lists.

### 3. **Cache System Compatibility**
The existing cache uses `recordingId` as the key, so it naturally works with any recording - preferred or otherwise.

### 4. **Existing UI Infrastructure**
The V2 recording selection sheet already exists and just needs persistence wiring.

### 5. **Database Integration**
The V2 Room database system can easily accommodate a new preferences table.

### 6. **Future-Proof Design**
This approach scales to additional preference types (playback position, custom setlists, etc.).

---

## Migration Strategy

### For Existing Users
- **Default Behavior**: Users see no change initially - system continues using `bestRecordingId`
- **Gradual Adoption**: Users can set preferences show-by-show as they explore
- **No Breaking Changes**: All existing functionality preserved
- **Preference Discovery**: UI can suggest setting preferences when users manually switch recordings

### Database Migration
```sql
-- Migration script for V2 database
CREATE TABLE user_recording_preferences (
    showId TEXT PRIMARY KEY NOT NULL,
    recordingId TEXT NOT NULL,
    setAt INTEGER NOT NULL
);

CREATE INDEX idx_user_recording_preferences_showId 
ON user_recording_preferences(showId);
```

---

## Conclusion

The V2 playlist system's clean architecture makes user recording preferences a **natural extension** rather than a disruptive change. The service-oriented design, abstracted recording resolution, and existing UI infrastructure provide a solid foundation.

**Key Benefits:**
- ✅ Maintains clean architecture principles
- ✅ Preserves existing user experience 
- ✅ Leverages existing cache and prefetch systems
- ✅ Provides clear upgrade path for users

**Recommended Approach:**
Implement incrementally over 1-2 weeks, starting with data layer and working up through service integration to UI polish. The modular design allows for thorough testing at each layer.

This enhancement would significantly improve user experience for power users while maintaining simplicity for casual users - exactly the kind of feature that demonstrates the value of clean architecture.