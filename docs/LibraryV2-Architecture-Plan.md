# LibraryV2 Architecture Refactoring Plan

## Current Architecture Problems

### Missing Domain Model for Library Context
- **Issue**: `Show` is a pure domain model but library-specific state (pin status, download status, library membership) is tracked separately
- **Impact**: UI must make separate queries for each show's library state, creating performance concerns and architectural inconsistency
- **Current Pattern**: 
  ```kotlin
  val show: Show // Domain model
  val isPinned by viewModel.getPinStatus(show).collectAsState() // Separate query
  val downloadStatus by viewModel.getDownloadStatus(show).collectAsState() // Separate query
  ```

### Architectural Inconsistency
- **Shows**: Domain model used directly in UI (no view model layer)
- **Library**: Service layer exists but no proper domain model for "show in library context"
- **Result**: Mixed patterns across the codebase

### Performance Concerns
- Pin sorting requires checking pin status for every show on every recomposition
- Separate Flow queries for each show's library state
- UI state scattered across multiple reactive streams

## Proposed LibraryV2 Domain Architecture

### LibraryV2Show Domain Model
Create a proper domain model that represents "Show in Library context":

```kotlin
package com.deadarchive.core.model

/**
 * Domain model representing a Show within the Library context.
 * Combines core concert data with library-specific metadata.
 */
data class LibraryV2Show(
    val show: Show,                           // Core concert data (immutable)
    val addedToLibraryAt: Long,              // When added to library
    val isPinned: Boolean = false,           // Library-specific pin status
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED // Download state
) {
    // Delegate Show properties for convenience
    val showId: String get() = show.showId
    val date: String get() = show.date
    val venue: String? get() = show.venue
    val location: String? get() = show.location
    val displayTitle: String get() = show.displayTitle
    val displayLocation: String get() = show.displayLocation
    val displayVenue: String get() = show.displayVenue
    val recordings: List<Recording> get() = show.recordings
    
    // Library-specific computed properties
    val isPinnedAndDownloaded: Boolean get() = isPinned && downloadStatus == DownloadStatus.COMPLETED
    val libraryAge: Long get() = System.currentTimeMillis() - addedToLibraryAt
}
```

### Database Layer: LibraryV2ItemEntity

```kotlin
package com.deadarchive.core.database.entity

@Entity(
    tableName = "library_v2",
    foreignKeys = [
        ForeignKey(
            entity = ShowEntity::class,
            parentColumns = ["show_id"],
            childColumns = ["show_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LibraryV2ItemEntity(
    @PrimaryKey 
    val showId: String,
    
    @ColumnInfo(name = "added_to_library_at")
    val addedToLibraryAt: Long,
    
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false
)

/**
 * Room relation for efficient querying of library shows with full show data
 */
data class LibraryV2ShowWithDetails(
    @Embedded 
    val libraryItem: LibraryV2ItemEntity,
    
    @Relation(
        parentColumn = "showId",
        entityColumn = "show_id"
    )
    val show: ShowEntity
)
```

### Database Access Layer

```kotlin
package com.deadarchive.core.database.dao

@Dao
interface LibraryV2Dao {
    @Query("SELECT * FROM library_v2 ORDER BY added_to_library_at DESC")
    fun getAllLibraryV2Items(): Flow<List<LibraryV2ItemEntity>>
    
    @Transaction
    @Query("SELECT * FROM library_v2 ORDER BY is_pinned DESC, added_to_library_at DESC")
    fun getAllLibraryV2ShowsWithDetails(): Flow<List<LibraryV2ShowWithDetails>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryV2Item(item: LibraryV2ItemEntity)
    
    @Query("DELETE FROM library_v2 WHERE showId = :showId")
    suspend fun removeLibraryV2Item(showId: String)
    
    @Query("UPDATE library_v2 SET is_pinned = :isPinned WHERE showId = :showId")
    suspend fun updatePinStatus(showId: String, isPinned: Boolean)
    
    @Query("SELECT is_pinned FROM library_v2 WHERE showId = :showId")
    fun getPinStatus(showId: String): Flow<Boolean?>
    
    @Query("DELETE FROM library_v2")
    suspend fun clearAllLibraryV2Items()
}
```

## Service Layer Architecture

### LibraryV2Service Interface Update

```kotlin
package com.deadarchive.core.library.api

interface LibraryV2Service {
    // Returns LibraryV2Show instead of Show
    fun getLibraryV2Shows(): Flow<List<LibraryV2Show>>
    
    // Library management (unchanged interface)
    suspend fun addShowToLibraryV2(showId: String): Result<Unit>
    suspend fun removeShowFromLibraryV2(showId: String): Result<Unit>
    suspend fun clearLibraryV2(): Result<Unit>
    fun isShowInLibraryV2(showId: String): Flow<Boolean>
    
    // Pin management (unchanged interface)
    suspend fun pinShowV2(showId: String): Result<Unit>
    suspend fun unpinShowV2(showId: String): Result<Unit>
    fun isShowPinnedV2(showId: String): Flow<Boolean>
    
    // Statistics and utilities
    suspend fun getLibraryV2Stats(): LibraryV2Stats
    suspend fun populateTestDataV2(): Result<Unit>
}
```

### Service Implementation Strategy

```kotlin
package com.deadarchive.core.library.service

@Singleton
class LibraryV2ServiceImpl @Inject constructor(
    private val libraryV2Dao: LibraryV2Dao,
    private val downloadService: DownloadService // For download status
) : LibraryV2Service {
    
    override fun getLibraryV2Shows(): Flow<List<LibraryV2Show>> {
        return libraryV2Dao.getAllLibraryV2ShowsWithDetails()
            .map { libraryShowsWithDetails ->
                libraryShowsWithDetails.map { libraryShowWithDetails ->
                    val show = libraryShowWithDetails.show.toShow()
                    val libraryItem = libraryShowWithDetails.libraryItem
                    val downloadStatus = getDownloadStatusForShow(show.showId)
                    
                    LibraryV2Show(
                        show = show,
                        addedToLibraryAt = libraryItem.addedToLibraryAt,
                        isPinned = libraryItem.isPinned,
                        downloadStatus = downloadStatus
                    )
                }
            }
    }
    
    private suspend fun getDownloadStatusForShow(showId: String): DownloadStatus {
        // Query download service for this show's download status
        return downloadService.getDownloadState(showId)?.status ?: DownloadStatus.NOT_DOWNLOADED
    }
}
```

## Implementation Roadmap

### Phase 1: Immediate Fixes (Current Session)
- [x] **Grid Height Fix**: Change `aspectRatio(1f)` to `aspectRatio(0.8f)` in ShowGridItem
- [x] **Documentation**: Create this architecture plan

### Phase 2: Domain Model Creation
- [ ] **LibraryV2Show Model**: Create new domain model in `core/model`
- [ ] **LibraryV2Stats Model**: Update stats model for new architecture
- [ ] **Unit Tests**: Create tests for LibraryV2Show domain logic

### Phase 3: Database Layer Updates
- [ ] **LibraryV2ItemEntity**: Create new database entity
- [ ] **LibraryV2Dao**: Create new DAO with Room relations
- [ ] **Database Migration**: Create migration from current library table
- [ ] **Integration Tests**: Test database operations

### Phase 4: Service Layer Refactoring
- [ ] **LibraryV2Service Interface**: Update to return LibraryV2Show
- [ ] **LibraryV2ServiceImpl**: New implementation using database layer
- [ ] **LibraryV2ServiceStub**: Update stub for UI development
- [ ] **Service Tests**: Update tests for new service contract

### Phase 5: UI Layer Updates
- [ ] **LibraryV2ViewModel**: Update to work with LibraryV2Show
- [ ] **LibraryV2Screen**: Update UI components to use new domain model
- [ ] **Remove Separate Queries**: Eliminate individual pin/download status queries
- [ ] **UI Tests**: Update component tests

### Phase 6: Pin Sorting Implementation
- [ ] **Sorting Logic**: Implement pin-aware sorting in `applyFiltersAndSorting()`
- [ ] **Performance Optimization**: Ensure efficient sorting with large lists
- [ ] **User Testing**: Verify pin sorting behavior matches expectations

## Naming Convention Standards

All Library-related classes must use "LibraryV2" prefix for consistency:

### Domain Models
- `LibraryV2Show` (new)
- `LibraryV2Stats` (update existing)

### Database Layer
- `LibraryV2ItemEntity` (new)
- `LibraryV2Dao` (new)
- `LibraryV2ShowWithDetails` (new)

### Service Layer
- `LibraryV2Service` (update existing interface)
- `LibraryV2ServiceImpl` (new implementation)
- `LibraryV2ServiceStub` (update existing stub)

### UI Layer
- `LibraryV2Screen` (existing)
- `LibraryV2ViewModel` (existing)
- `LibraryV2UiState` (existing)

## Benefits of This Architecture

### Performance Improvements
- **Single Query**: Get all library shows with metadata in one database query
- **No Separate Flows**: Eliminate individual pin/download status queries per show
- **Efficient Sorting**: Pin status available directly on domain model
- **Room Optimization**: Leverage Room's query optimization for JOINs

### Clean Architecture
- **Proper Domain Separation**: LibraryV2Show represents library context clearly
- **Single Responsibility**: Each layer has clear, focused responsibilities
- **Testability**: Domain model can be unit tested independently
- **Maintainability**: Changes to library logic contained within library domain

### Developer Experience
- **Type Safety**: LibraryV2Show provides compile-time safety for library operations
- **IntelliSense**: Better IDE support with proper domain model
- **Debugging**: Easier to debug with consolidated library state
- **Consistency**: Uniform approach across all library features

## Migration Strategy

### Backward Compatibility
During migration, both old and new systems can coexist:
- New LibraryV2 tables alongside existing library tables
- Gradual migration of UI components to use LibraryV2Show
- Feature flags to switch between implementations
- Data migration scripts to populate LibraryV2 from existing data

### Risk Mitigation
- **Database Migrations**: Thoroughly tested migration scripts
- **Rollback Plan**: Ability to revert to previous implementation
- **A/B Testing**: Compare performance between old and new implementations
- **Incremental Rollout**: Migrate features one at a time

## Success Metrics

### Performance Metrics
- Reduce UI state queries from N queries per show to 1 query total
- Improve list rendering performance by 50%+ (measured via systrace)
- Reduce memory pressure from separate Flow subscriptions

### Code Quality Metrics
- Eliminate UI-specific state management code (pin/download status queries)
- Reduce coupling between UI and service layers
- Improve test coverage with better domain model testability

### User Experience Metrics
- Faster library screen loading times
- Smoother scrolling in library views
- Consistent pin sorting behavior across all library contexts

---

## Notes for Implementation

### Room Migration Example
```sql
-- Migration from library to library_v2
CREATE TABLE library_v2 (
    showId TEXT PRIMARY KEY,
    added_to_library_at INTEGER NOT NULL,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (showId) REFERENCES shows(show_id) ON DELETE CASCADE
);

-- Migrate existing data
INSERT INTO library_v2 (showId, added_to_library_at, is_pinned)
SELECT show_id, added_to_library_at, 0 
FROM library;
```

### Testing Strategy
- **Unit Tests**: LibraryV2Show domain logic
- **Integration Tests**: Database operations with Room
- **Repository Tests**: Service layer with mock dependencies  
- **UI Tests**: Component behavior with LibraryV2Show data
- **End-to-End Tests**: Complete library workflows

This architecture provides a solid foundation for scalable library management while maintaining clean separation of concerns and optimal performance.