# Library Performance Fix Documentation

## Problem Statement

The library screen was taking 5+ seconds to load even when the user's library was empty. Investigation revealed that the library screen was incorrectly loading ALL shows from the database instead of only library shows.

## Root Cause Analysis

### Current Architecture Issue
```
LibraryViewModel
├── libraryRepository.getAllLibraryItems() ✅ (correct - gets library items)
└── showRepository.getAllShows() ❌ (wrong - gets ALL shows, not just library)
```

### Performance Impact
- **Empty Library (0 shows)**: Still loads 1000+ shows + 5000+ recordings + ratings
- **Non-Empty Library (5 shows)**: Loads 1000+ shows, filters to 5 in memory
- **Database Queries**: N+1 problem - individual queries per show/recording/rating

### Why This Happens
1. `ShowRepository` interface missing library-specific methods
2. `ShowDao` HAS the right methods (`getLibraryShows()`) but not exposed
3. UI falls back to loading all data + client-side filtering

## Solution Design

### Database Layer (Already Exists)
```sql
-- ShowDao.getLibraryShows()
SELECT * FROM concerts_new WHERE isInLibrary = 1 ORDER BY date DESC

-- ShowDao.getLibraryShowsFlow()
-- Flow version of above query
```

### Repository Layer (Missing - Need to Add)
```kotlin
interface ShowRepository {
    // Add these methods:
    fun getLibraryShows(): Flow<List<Show>>
    suspend fun getLibraryShowsList(): List<Show>
}
```

### UI Layer Fix
```kotlin
// LibraryViewModel - Change from:
combine(
    libraryRepository.getAllLibraryItems(),
    showRepository.getAllShows()  // ← Wrong
)

// To:
combine(
    libraryRepository.getAllLibraryItems(),
    showRepository.getLibraryShows()  // ← Correct
)
```

## Implementation Plan

### Phase 1: Repository Methods
1. Add `getLibraryShows()` to `ShowRepository` interface
2. Implement in `ShowRepositoryImpl` using existing `ShowDao.getLibraryShows()`
3. Apply same optimization pattern as other methods (batch loading)

### Phase 2: UI Integration
1. Update `LibraryViewModel` to use new methods
2. Remove client-side filtering logic
3. Test with empty and populated libraries

### Phase 3: Performance Validation
1. Measure load times before/after
2. Verify database query count reduction
3. Test with various library sizes

## Expected Performance Impact

### Before Fix
- **Empty Library**: ~5+ seconds (loads all shows)
- **5 Shows in Library**: ~5+ seconds (loads all shows, filters to 5)
- **Database Queries**: 3000+ queries for 1000 shows

### After Fix
- **Empty Library**: ~10ms (database returns empty list immediately)
- **5 Shows in Library**: ~50ms (loads only 5 shows + their recordings)
- **Database Queries**: 3-5 queries total

## Files to Modify

### Core Repository
- `/workspace/core/data/src/main/java/com/deadarchive/core/data/repository/ShowRepository.kt`
  - Add interface methods

### Implementation
- `/workspace/core/data/src/main/java/com/deadarchive/core/data/repository/ShowRepository.kt`
  - Add implementation methods

### UI Layer
- `/workspace/feature/library/src/main/java/com/deadarchive/feature/library/LibraryViewModel.kt`
  - Change method calls

## Testing Strategy

### Unit Tests
- Verify `getLibraryShows()` returns only library shows
- Test empty library scenario
- Test populated library scenario

### Integration Tests
- Measure actual load times
- Verify UI displays correctly
- Test library add/remove operations

### Performance Tests
- Benchmark before/after query counts
- Measure memory usage
- Test with large datasets

## Risk Assessment

### Low Risk Changes
- Adding new repository methods (doesn't break existing)
- UI method call change (simple substitution)

### Validation Required
- Ensure library shows load correctly
- Verify ratings/recordings still included
- Test library state management

## Success Criteria

1. **Performance**: Empty library loads in <100ms
2. **Functionality**: All library features work as before
3. **Architecture**: Proper separation of concerns restored
4. **Scalability**: Performance doesn't degrade with total show count

## Follow-up Optimizations

After this fix, consider:
1. Apply same batch loading pattern to search screens
2. Add database indexes for common queries
3. Implement pagination for very large libraries
4. Add memory caching for frequently accessed data

---

**Priority**: High - User-facing performance issue
**Effort**: Medium - Clean architectural fix
**Risk**: Low - Additive changes with fallback capability