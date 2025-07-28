# LibraryV2 Data Model Migration Strategy

## Overview

This document outlines the strategy for migrating from manual `isInLibrary` field management to a clean service-oriented architecture with computed properties, while maintaining display performance.

## Current Problems

### 1. Mixed Responsibilities
- Show model contains both concert data AND library status
- Multiple layers manually set `isInLibrary` instead of using services
- Redundant data: both `isInLibrary: Boolean` and `addedToLibraryAt: Long?`

### 2. Clean Architecture Violations
- ViewModels directly manipulate library state on Show objects
- Repository layers bypass library service for library operations
- Domain model polluted with persistence concerns

### 3. Consistency Issues
- Multiple sources of truth for library membership
- Race conditions between library operations and display queries
- Manual assignments can get out of sync with actual library state

## Target Architecture

### Performance-Optimized Display
- Keep library metadata on Show model for fast display queries
- Single JOIN query for browse/search screens with library status
- No N+1 query problems for list displays

### Clean Service Operations
- All library operations go through LibraryV2Service
- Services manipulate `addedToLibraryAt` timestamp
- `isInLibrary` becomes computed property for consistency

### Data Model
```kotlin
data class Show(
    // Concert data
    val showId: String,
    val date: String,
    val venue: String?,
    val location: String?,
    val recordings: List<Recording>,
    val rating: Float?,
    
    // Library metadata (performance optimization)
    val addedToLibraryAt: Long? = null,
    
    // Computed property (single source of truth)
    val isInLibrary: Boolean
        get() = addedToLibraryAt != null
)
```

## Migration Plan

### Phase 1: Add Deprecation Warnings (Immediate)

**Locations to Deprecate:**
- `ShowRepository.kt` (5 locations): `copy(isInLibrary = isInLibrary)` calls
- `ConcertListViewModel.kt` (4 locations): Manual library toggle operations  
- `ShowCreationServiceImpl.kt`: Manual `isInLibrary = false` assignments
- Any other manual `isInLibrary` assignments found

**Deprecation Template:**
```kotlin
// @Deprecated("Manual isInLibrary setting deprecated. Use LibraryV2Service for library operations.")
// TODO: Replace with LibraryV2Service.addToLibrary()/removeFromLibrary() calls
enrichedShow.copy(isInLibrary = isInLibrary)
```

### Phase 2: Enhance LibraryV2 Stubs (Current)

**Create Realistic Test Data:**
```kotlin
private val testLibraryShows = listOf(
    Show(
        date = "1977-05-08",
        venue = "Barton Hall, Cornell University",
        location = "Ithaca, NY", 
        addedToLibraryAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
        recordings = listOf(/* sample recordings */),
        rating = 4.8f
    ),
    Show(
        date = "1972-05-04", 
        venue = "Olympia Theatre",
        location = "Paris, France",
        addedToLibraryAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
        recordings = listOf(/* sample recordings */),
        rating = 4.2f
    )
    // 15-20 more realistic shows across different decades
)
```

**UI Enhancement:**
- Add "Populate Test Data" button to LibraryV2Screen
- Button calls `LibraryV2Service.populateTestData()`
- Enables immediate UI development and testing

### Phase 3: Complete LibraryV2 UI (Next)

**Implement Display Modes:**
- LibraryListView composable (using ExpandableConcertItem)
- LibraryGridView composable (new compact layout)
- Wire up display mode toggle

**Implement Functionality:**
- Wire sort selector to actually sort Show list
- Implement hierarchical filtering (decade → season)
- Add library operations (add/remove shows)

### Phase 4: Service Migration (Future)

**Update Manual Assignments:**
- Replace deprecated `copy(isInLibrary = ...)` with service calls
- Update ViewModels to use LibraryV2Service
- Remove manual library state manipulation

**Make isInLibrary Computed:**
```kotlin
// Change from:
val isInLibrary: Boolean = false

// To:
val isInLibrary: Boolean
    get() = addedToLibraryAt != null
```

## Database Query Strategy

### Display Queries (Performance Critical)
```sql
-- Browse/Search with library status (single query)
SELECT s.*, (l.showId IS NOT NULL) as isInLibrary, l.addedAt
FROM shows s  
LEFT JOIN library_entries l ON s.showId = l.showId
WHERE s.year BETWEEN '1980' AND '1989'
```

### Library Operations (Clean Service)
```kotlin
// Add to library
suspend fun addToLibrary(showId: String): Result<Unit> {
    showService.updateShow(showId) { show ->
        show.copy(addedToLibraryAt = System.currentTimeMillis())
    }
}

// Remove from library  
suspend fun removeFromLibrary(showId: String): Result<Unit> {
    showService.updateShow(showId) { show ->
        show.copy(addedToLibraryAt = null)
    }
}
```

## Benefits

### Performance
- Single database queries for display
- Fast list scrolling with immediate library status
- Efficient filtering and sorting

### Clean Architecture
- Clear separation between display and operations
- Services own their business logic
- Domain models stay focused

### Consistency  
- Single source of truth (addedToLibraryAt)
- No manual state management
- Computed properties prevent inconsistency

### Maintainability
- Deprecated code is clearly marked
- Migration path is documented
- Services are testable in isolation

## Implementation Notes

### Current Show Model Compatibility
- Keep existing `isInLibrary: Boolean` field during migration
- Both fields exist temporarily for compatibility
- Display code continues working unchanged

### Service Design
- LibraryV2Service handles all library operations
- Can delegate to ShowService for Show updates
- Maintains clean boundaries between concerns

### Testing Strategy
- Stub services with in-memory test data
- Realistic Show objects for UI testing
- Independent service testing for business logic

This approach balances performance requirements with clean architecture principles, providing a clear migration path while maintaining system stability.