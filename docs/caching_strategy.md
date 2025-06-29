# Dead Archive Caching Strategy

## Current Problem

The existing caching system has fundamental issues that create confusing and inconsistent user experiences:

### Issues with Current Implementation

1. **Flaky Search Results**: Same query returns different results on different attempts
   - Search "1993-05" → Returns May 1993 shows ✅
   - Search "1993-05-16" → Falls back to 1995 shows ❌ (wrong!)

2. **Cache-First Strategy Problems**: 
   - Repository always emits cached results first (`line 56-63` in ConcertRepository)
   - When API returns no results, cached results with broad SQL LIKE matching get shown
   - Users see irrelevant concerts instead of "no results found"

3. **Substring Matching Issues**:
   - Query "1993-05-16" matches "1995" concerts because "05" appears in both
   - SQL LIKE '%query%' is too permissive across all fields

4. **User Experience Problems**:
   - Cache behavior is visible to users (should be transparent)
   - Inconsistent results violate user expectations
   - No clear "no results" state when searches fail

## New Strategy: Complete Dataset Cache

### Philosophy
> "The cache is a feature of the application, not the user interface"

The caching layer should be completely invisible to users. They should get consistent, predictable results regardless of network state or cache contents.

### Approach 1: Complete Dataset Download (Primary Strategy)

#### Overview
Download the entire Grateful Dead concert catalog once and store it locally. All searches become local-only with precise matching.

#### Benefits
- **Ultra-fast searches** (Netflix-like performance)
- **100% consistent results** (no API/cache discrepancies)  
- **Works offline** (full functionality without network)
- **Finite dataset** (~2,000 concerts total - manageable size)
- **Stable data** (historical concerts don't change frequently)

#### Implementation Plan

**Phase 1: Background Sync Service**
```kotlin
// Create DataSyncService for complete catalog download
class DataSyncService {
    suspend fun downloadCompleteCatalog()
    suspend fun getDownloadProgress(): Flow<SyncProgress>  
    suspend fun isInitialSyncComplete(): Boolean
}
```

**Phase 2: Full Catalog Download**
- Download all GD concerts on first app launch
- Show progress indicator during initial sync
- Store everything in SQLite with proper indexing
- Include all metadata except audio files

**Phase 3: Local-Only Search**
- Replace API-first search with local-only queries
- Implement precise date matching logic
- Remove fallback cache behavior

**Phase 4: Background Refresh** 
- Periodic refresh (weekly/monthly) in background
- Update existing records, add new ones
- Never interrupt user experience

### Approach 2: Precise Cache Matching (Complementary)

Replace broad SQL LIKE matching with field-specific precise searches:

```sql
-- Instead of: LIKE '%1993-05-16%' (matches 1995 concerts)
-- Use precise date matching:
WHERE date = '1993-05-16'           -- Exact date
WHERE date LIKE '1993-05-%'         -- Month range  
WHERE date LIKE '1993-%'            -- Year range
```

### Approach 3: Separate Search Domains (Future Enhancement)

Cache and search different content types separately:
- **Date searches**: `date_cache` table with precise date indexing
- **Venue searches**: `venue_cache` table with normalized venue names
- **Location searches**: `location_cache` table with city/state data
- **General searches**: Full-text search across all fields

## Expected User Experience

### Search Behavior
- **"1993"** → All 1993 concerts
- **"1993-05"** → All May 1993 concerts  
- **"1993-05-16"** → Concert from 5/16/93 OR empty results (never wrong concerts)
- **"Winterland"** → All Winterland concerts
- **"Berkeley"** → All Berkeley area concerts

### Performance Expectations
- **Initial app launch**: Download progress indicator
- **Subsequent searches**: Instant results (<100ms)  
- **Background updates**: Invisible to user

### Error States
- **Network down**: Full functionality (local data)
- **No results found**: Empty state, not wrong results
- **Sync failures**: Graceful degradation with user notification

## Implementation Checklist

### Phase 1: Foundation
- [ ] Create `DataSyncService` for background downloads
- [ ] Add sync progress UI components  
- [ ] Create database indexes for optimal search performance
- [ ] Implement precise date matching logic

### Phase 2: Complete Download
- [ ] First-launch complete catalog download
- [ ] Progress tracking and user feedback
- [ ] Error handling for download failures
- [ ] Resume interrupted downloads

### Phase 3: Local Search
- [ ] Replace `ConcertRepository.searchConcerts()` with local-only implementation
- [ ] Remove API fallback behavior  
- [ ] Add field-specific search methods
- [ ] Implement "no results" states

### Phase 4: Background Sync
- [ ] Periodic background refresh mechanism
- [ ] Delta updates (only changed/new concerts)
- [ ] Sync conflict resolution
- [ ] User controls for sync frequency

## Database Schema Enhancements

```sql
-- Add indexes for optimal search performance
CREATE INDEX idx_concerts_date ON concerts(date);
CREATE INDEX idx_concerts_venue ON concerts(venue);
CREATE INDEX idx_concerts_location ON concerts(location);
CREATE INDEX idx_concerts_year ON concerts(year);

-- Add sync tracking
CREATE TABLE sync_metadata (
    id INTEGER PRIMARY KEY,
    last_full_sync TIMESTAMP,
    last_delta_sync TIMESTAMP,
    total_concerts INTEGER,
    sync_version INTEGER
);
```

## Migration Strategy

1. **Phase 1**: Implement alongside current system (no breaking changes)
2. **Phase 2**: Add user setting to enable "Complete Local Cache" mode
3. **Phase 3**: Make complete cache the default after testing
4. **Phase 4**: Remove old API-first caching logic

## Success Metrics

- **Search consistency**: 100% identical results for identical queries
- **Search speed**: <100ms for all local searches
- **User satisfaction**: No more "wrong results" reports  
- **Offline capability**: Full search functionality without network

---

*This strategy prioritizes user experience and data consistency over network optimization. Given the finite and stable nature of the Grateful Dead concert archive, local-first architecture is the optimal approach.*