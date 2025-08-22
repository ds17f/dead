# V2 PlaylistServiceImpl Implementation Plan

## Goal
Replace PlaylistServiceStub with real PlaylistServiceImpl using V2 domain architecture and efficient database navigation. Direct replacement - no conditional binding, no unit tests.

## Implementation Strategy

### Phase 1: Core Real Functionality
- ✅ **Show Loading**: Real database integration via ShowRepository
- ✅ **Efficient Navigation**: Database-level next/previous queries
- ✅ **Domain Model Conversion**: Show → PlaylistShowViewModel with real data
- 🔲 **Stubbed Areas**: Track lists, media operations, library operations (marked with TODOs)

## Implementation Plan

### 1. Add Navigation Methods to Domain Layer

**Update**: `v2/core/domain/src/main/java/com/deadly/v2/core/domain/repository/ShowRepository.kt`

**Add Methods**:
```kotlin
interface ShowRepository {
    // ... existing methods ...
    
    // Efficient chronological navigation
    suspend fun getNextShowByDate(currentDate: String): Show?
    suspend fun getPreviousShowByDate(currentDate: String): Show?
}
```

### 2. Add Database Navigation Queries

**Update**: `v2/core/database/src/main/java/com/deadly/v2/core/database/dao/ShowDao.kt`

**Add Methods**:
```kotlin
@Query("SELECT * FROM shows WHERE date > :currentDate ORDER BY date ASC LIMIT 1")
suspend fun getNextShowByDate(currentDate: String): ShowEntity?

@Query("SELECT * FROM shows WHERE date < :currentDate ORDER BY date DESC LIMIT 1") 
suspend fun getPreviousShowByDate(currentDate: String): ShowEntity?
```

### 3. Implement Navigation in Repository

**Update**: `v2/core/database/src/main/java/com/deadly/v2/core/database/repository/ShowRepositoryImpl.kt`

**Add Methods**:
```kotlin
override suspend fun getNextShowByDate(currentDate: String): Show? {
    return showDao.getNextShowByDate(currentDate)?.let { 
        showMappers.entityToDomain(it) 
    }
}

override suspend fun getPreviousShowByDate(currentDate: String): Show? {
    return showDao.getPreviousShowByDate(currentDate)?.let { 
        showMappers.entityToDomain(it) 
    }
}
```

### 4. Create PlaylistServiceImpl

**Create**: `v2/core/playlist/service/PlaylistServiceImpl.kt`

**Architecture**:
```kotlin
@Singleton
class PlaylistServiceImpl @Inject constructor(
    private val showRepository: ShowRepository
) : PlaylistService {
    
    companion object {
        private const val TAG = "PlaylistServiceImpl"
    }
    
    private var currentShow: Show? = null
    
    // === PHASE 1: REAL IMPLEMENTATIONS ===
    
    override suspend fun loadShow(showId: String?) {
        Log.d(TAG, "Loading show: $showId")
        currentShow = if (showId != null) {
            showRepository.getShowById(showId)
        } else {
            // Default to a well-known show (Cornell '77)
            showRepository.getShowById("1977-05-08")
        }
        Log.d(TAG, "Loaded show: ${currentShow?.displayTitle}")
    }
    
    override suspend fun getCurrentShowInfo(): PlaylistShowViewModel? {
        return currentShow?.let { show ->
            // Convert Show domain model to PlaylistShowViewModel
            convertShowToViewModel(show)
        }
    }
    
    override suspend fun navigateToNextShow() {
        currentShow?.let { current ->
            val nextShow = showRepository.getNextShowByDate(current.date)
            if (nextShow != null) {
                currentShow = nextShow
                Log.d(TAG, "Navigated to next show: ${nextShow.displayTitle}")
            } else {
                Log.d(TAG, "No next show available")
            }
        }
    }
    
    override suspend fun navigateToPreviousShow() {
        currentShow?.let { current ->
            val previousShow = showRepository.getPreviousShowByDate(current.date)
            if (previousShow != null) {
                currentShow = previousShow
                Log.d(TAG, "Navigated to previous show: ${previousShow.displayTitle}")
            } else {
                Log.d(TAG, "No previous show available")
            }
        }
    }
    
    // === DOMAIN MODEL CONVERSION ===
    
    private suspend fun convertShowToViewModel(show: Show): PlaylistShowViewModel {
        // Calculate navigation availability
        val hasNext = showRepository.getNextShowByDate(show.date) != null
        val hasPrevious = showRepository.getPreviousShowByDate(show.date) != null
        
        return PlaylistShowViewModel(
            date = show.date,
            displayDate = formatDisplayDate(show.date),
            venue = show.venue.name,
            location = show.location.displayText,
            rating = show.averageRating ?: 0.0f,
            reviewCount = show.totalReviews,
            trackCount = show.recordingCount, // Use recording count as proxy for now
            hasNextShow = hasNext,
            hasPreviousShow = hasPrevious,
            isInLibrary = show.isInLibrary,
            downloadProgress = null // TODO: Integrate with download service
        )
    }
    
    private fun formatDisplayDate(date: String): String {
        // Convert "1977-05-08" to "May 8, 1977"
        return try {
            val parts = date.split("-")
            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            val monthNames = arrayOf(
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            
            "${monthNames[month]} $day, $year"
        } catch (e: Exception) {
            date // Fallback to original format
        }
    }
    
    // === PHASE 1: STUBBED IMPLEMENTATIONS (TODOs) ===
    
    override suspend fun getTrackList(): List<PlaylistTrackViewModel> {
        Log.d(TAG, "getTrackList() - TODO: Implement when Archive API integration is ready")
        // TODO: Convert Recording domain models to PlaylistTrackViewModel
        // TODO: Integration requires Archive API for track-level metadata
        return emptyList()
    }
    
    override suspend fun playTrack(trackIndex: Int) {
        Log.d(TAG, "playTrack($trackIndex) - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service when available
    }
    
    override suspend fun addToLibrary() {
        Log.d(TAG, "addToLibrary() - TODO: Integrate with V2 library service")
        // TODO: Integrate with V2 library service when available
    }
    
    override suspend fun downloadShow() {
        Log.d(TAG, "downloadShow() - TODO: Integrate with V2 download service")
        // TODO: Integrate with V2 download service when available
    }
    
    override suspend fun shareShow() {
        Log.d(TAG, "shareShow() - TODO: Implement sharing functionality")
        // TODO: Implement sharing functionality
    }
    
    override suspend fun loadSetlist() {
        Log.d(TAG, "loadSetlist() - TODO: Use setlist data from Show domain model")
        // TODO: Use setlist data from Show domain model
    }
    
    override suspend fun pause() {
        Log.d(TAG, "pause() - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service
    }
    
    override suspend fun resume() {
        Log.d(TAG, "resume() - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service
    }
    
    override suspend fun getCurrentReviews(): List<PlaylistReview> {
        Log.d(TAG, "getCurrentReviews() - TODO: Implement when Archive API integration is ready")
        // TODO: Load reviews from Archive API when integration is ready
        return emptyList()
    }
    
    override suspend fun getRatingDistribution(): Map<Int, Int> {
        Log.d(TAG, "getRatingDistribution() - TODO: Calculate from recording ratings")
        // TODO: Calculate from recording ratings when available
        return emptyMap()
    }
    
    override suspend fun getRecordingOptions(): RecordingOptionsResult {
        Log.d(TAG, "getRecordingOptions() - TODO: Load from Recording domain models")
        // TODO: Convert Recording domain models to RecordingOptionViewModel
        return RecordingOptionsResult(
            currentRecording = null,
            alternativeRecordings = emptyList(),
            hasRecommended = false
        )
    }
    
    override suspend fun selectRecording(recordingId: String) {
        Log.d(TAG, "selectRecording($recordingId) - TODO: Implement recording selection")
        // TODO: Implement recording selection logic
    }
    
    override suspend fun setRecordingAsDefault(recordingId: String) {
        Log.d(TAG, "setRecordingAsDefault($recordingId) - TODO: Implement user preferences")
        // TODO: Implement user preference storage
    }
    
    override suspend fun resetToRecommended() {
        Log.d(TAG, "resetToRecommended() - TODO: Implement recommendation logic")
        // TODO: Implement recommendation logic
    }
}
```

### 5. Update Module Dependencies

**Update**: `v2/core/playlist/build.gradle.kts`

**Add Domain Dependency**:
```kotlin
dependencies {
    // V2 API dependencies
    implementation(project(":v2:core:api:playlist"))

    // V2 domain dependencies  
    implementation(project(":v2:core:domain"))

    // V2 model dependencies
    implementation(project(":v2:core:model"))
    
    // ... existing dependencies
}
```

### 6. Update Dependency Injection

**Update**: `v2/core/playlist/di/PlaylistModule.kt`

**Replace Stub Binding**:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
abstract class PlaylistModule {

    @Binds
    abstract fun bindPlaylistService(
        impl: PlaylistServiceImpl  // Direct replacement - no stub
    ): PlaylistService
}
```

### 7. Remove Stub Implementation

**Delete**: `v2/core/playlist/service/PlaylistServiceStub.kt`

### 8. Manual Testing Plan

**Testing Steps**:
1. ✅ Build and run V2 playlist screen
2. ✅ Verify real show data loads from database (should see actual Dead shows)
3. ✅ Test chronological navigation between shows (next/previous buttons)
4. ✅ Confirm UI remains visually identical to stub version
5. ✅ Verify stubbed methods log TODOs instead of crashing
6. ✅ Check that show details (venue, date, rating) display real database values

**Success Criteria**:
- Real show data displayed in playlist UI
- Navigation works through actual Dead shows chronologically  
- No crashes or build errors
- UI behavior unchanged (ViewModels contract maintained)
- Clear TODO logging for future implementation areas

## Implementation Benefits

### Immediate Benefits:
1. **Real Database Integration**: Shows actual Dead shows from V2 database
2. **Efficient Navigation**: Single database queries instead of loading entire datasets
3. **Clean Architecture**: Uses established V2 domain patterns
4. **Scalable**: Database-level queries handle large datasets efficiently

### Future Integration Points:
- **Archive API**: Track lists, recording options, reviews
- **V2 Library Service**: Real library operations
- **V2 Download Service**: Real download management  
- **V2 Media Service**: Real playback control

### Architecture Alignment:
- Follows SearchServiceImpl patterns (domain repository injection)
- Maintains clean separation of concerns (domain → UI ViewModels)
- Establishes foundation for future V2 service integrations

## Implementation Order

1. ✅ Add navigation methods to ShowRepository interface
2. ✅ Add database queries to ShowDao
3. ✅ Implement navigation in ShowRepositoryImpl
4. ✅ Create PlaylistServiceImpl with Phase 1 functionality
5. ✅ Update playlist module dependencies
6. ✅ Update DI module (replace stub binding)
7. ✅ Delete PlaylistServiceStub
8. ✅ Manual testing and verification

## Post-Implementation

**Documentation**: Update this plan with implementation results and any discoveries during testing.

**Future Work**: The TODO-marked methods provide clear integration points for:
- Archive API integration (tracks, reviews, recording options)
- V2 service integrations (library, download, media)
- Advanced features (user preferences, recommendations)