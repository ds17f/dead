# V2 Development - Next Steps

## Current State

### Completed ✅
- **V2 Architecture**: Established patterns for service-oriented development
- **Documentation**: Requirements, architecture, and visual schema diagrams
- **Tooling**: `make view-db-schema` command for database visualization

### V2 Features Status
- **LibraryV2**: 🔄 **In Progress** - UI foundation built, needs data integration and feature completion
- **PlayerV2**: 🔄 **In Progress** - Professional UI with comprehensive stubs, needs real service implementation
- **SearchV2**: 🔄 **In Progress** - Basic transparent interface, needs V2 data integration and search logic
- **HomeV2**: 🔄 **In Progress** - Layout foundation, needs content and data integration
- **QRScannerV2**: ✅ Complete - Sharing strategy implementation
- **V2 Database Design**: 🔄 **Mostly Complete** - Core entities designed, some loose ends remain

## Implementation Phase Options

### Option 1: V2 Data Layer Implementation 🎯 **RECOMMENDED**
**Goal**: Make the V2 database design real with working Room entities and repositories

**Tasks**:
1. **Create Room Entities**
   - Implement all documented V2 entities (Show, Venue, Recording, Track, etc.)
   - Set up composite keys for TrackV2Entity
   - Configure proper Room annotations and relationships

2. **Build V2 Database**
   - Create DeadlyV2Database with version management
   - Set up proper indices for search optimization
   - Implement database migrations from V1

3. **V2 Repository Layer**
   - Create repository interfaces following V2 patterns
   - Implement repositories with proper Flow-based reactive streams
   - Build query methods optimized for V2 search patterns

4. **Data Import Pipeline**
   - Build V1→V2 migration utilities
   - Create collection population from JSON definitions
   - Set up Archive.org data import for V2 schema

**Why This First**: Provides working foundation that all V2 features can build upon. Without real data layer, V2 features remain UI-only prototypes.

### Option 2: V2 Service Layer Implementation
**Goal**: Build service layer to connect V2 UI features with V2 data

**Tasks**:
1. **User Activity Services**
   - Implement UserActivityService with resume functionality
   - Build CurrentPlaybackService for state management
   - Create ShowPlaythroughService for analytics tracking

2. **Enhanced V2 Services**
   - Replace PlayerV2ServiceStub with PlayerV2ServiceImpl
   - Build SearchV2Service with V2 data integration
   - Create LibraryV2Service using embedded library fields

3. **Media Integration**
   - Implement QueueManager with Media3 bidirectional sync
   - Build resume system with non-invasive state restoration
   - Create position tracking with batched updates (5-10 second intervals)

**Dependencies**: Requires V2 data layer implementation first.

### Option 3: V2 Feature Enhancement
**Goal**: Enhance existing V2 features to use real V2 data and services

**Tasks**:
1. **LibraryV2 Integration**
   - Connect LibraryV2 to embedded library fields in ShowV2Entity
   - Implement library backup/restore functionality
   - Add library statistics and insights

2. **PlayerV2 Integration**
   - Connect PlayerV2 to CurrentPlaybackV2Entity for resume
   - Implement real queue management with QueueManager
   - Add activity tracking integration

3. **SearchV2 Enhancement**
   - Connect SearchV2 to optimized V2 search fields
   - Implement "1977 Ramble on Rose" style complex queries
   - Add search result analytics and ranking

**Dependencies**: Requires V2 data layer and service layer.

### Option 4: New V2 Features
**Goal**: Build completely new features using V2 architecture

**Tasks**:
1. **PlaylistV2 System**
   - Design PlaylistV2Entity + PlaylistTrackV2Entity
   - Build playlist creation and management UI
   - Implement QR code sharing functionality

2. **AnalyticsV2 Dashboard**
   - Build user insights UI using ShowPlaythroughV2Entity data
   - Create listening pattern visualizations
   - Implement recommendation suggestions

3. **CollectionsV2 Browse**
   - Build collections browsing interface
   - Implement collection detail screens
   - Add collection-based navigation

**Dependencies**: Requires V2 data layer, service layer, and enhanced features.

## Recommended Implementation Sequence

### Phase 1: Foundation (4-6 weeks)
1. **V2 Data Layer Implementation** (Option 1)
   - Highest priority - enables all other V2 development
   - Provides working database with real data
   - Establishes migration path from V1

### Phase 2: Integration (3-4 weeks)  
2. **V2 Service Layer Implementation** (Option 2)
   - Connect V2 UI features to real data
   - Implement resume functionality and activity tracking
   - Build Media3 integration

### Phase 3: Enhancement (2-3 weeks)
3. **V2 Feature Enhancement** (Option 3)
   - Polish existing V2 features with real data integration
   - Add advanced functionality enabled by V2 architecture
   - Performance optimization and testing

### Phase 4: Expansion (Ongoing)
4. **New V2 Features** (Option 4)
   - Build additional features as requirements emerge
   - PlaylistV2, AnalyticsV2, enhanced Collections browsing
   - Continuous improvement based on user feedback

## Success Criteria

### Phase 1 Complete When:
- [ ] All V2 entities implemented as Room entities
- [ ] V2 database creates and migrates from V1 successfully  
- [ ] V2 repositories provide reactive data streams
- [ ] Archive.org data imports into V2 schema
- [ ] Collections populate from JSON definitions

### Phase 2 Complete When:
- [ ] PlayerV2 resumes playback using CurrentPlaybackV2Entity
- [ ] LibraryV2 uses embedded library fields (no separate table)
- [ ] SearchV2 queries optimized V2 fields (year, month, songList)
- [ ] User activity tracking records show playthroughs
- [ ] Media3 integration provides bidirectional sync

### Phase 3 Complete When:
- [ ] All V2 features use real data (no more stubs)
- [ ] Performance meets or exceeds V1 benchmarks
- [ ] V2 features are production-ready and feature-complete
- [ ] Migration path from V1 is thoroughly tested
- [ ] Documentation updated with implementation details

## Risk Mitigation

### Technical Risks
- **Migration Complexity**: V1→V2 data migration may be complex
  - *Mitigation*: Build comprehensive migration testing with real user data
- **Performance**: V2 queries may not perform as expected
  - *Mitigation*: Early performance testing, query optimization
- **Media3 Integration**: Resume system integration may be complex
  - *Mitigation*: Incremental implementation, extensive testing

### Project Risks  
- **Scope Creep**: V2 implementation may expand beyond core requirements
  - *Mitigation*: Strict adherence to documented entity design
- **V1 Maintenance**: Maintaining V1 while building V2
  - *Mitigation*: Clear V1/V2 boundaries, parallel development approach

## Context Preservation

This document captures the current state and next steps for V2 development. Key context:

- **Database design is mostly complete** for core music listening experience (some loose ends remain)
- **V2 UI features exist** but are foundation-level implementations, not complete features
- **Architecture patterns established** through LibraryV2 and PlayerV2 development
- **Data layer implementation** is the critical next step to enable real V2 functionality

## Realistic Current V2 Status

**🔄 Work In Progress:**
- **LibraryV2**: UI foundation built, needs data integration and feature completion
- **PlayerV2**: Professional UI with comprehensive stubs, needs real service implementation  
- **SearchV2**: Basic transparent interface, needs V2 data integration and search logic
- **HomeV2**: Layout foundation, needs content and data integration
- **Database Design**: Core entities designed, but loose ends remain (playlists?, preferences?, sync?)

**✅ Actually Complete:**
- QRScannerV2 sharing strategy
- V2 architectural patterns established
- Database visualization tooling (`make view-db-schema`)
- Comprehensive documentation

**📋 Key Gaps to Address:**
- Complete database design loose ends
- Finish V2 feature implementations beyond UI foundations
- Build real data layer (Room entities, repositories)
- Connect V2 UI features to real data and services

Significant progress made, but substantial work remains to achieve fully functional V2 features.

**Recommendation: Address database loose ends, then proceed with Phase 1 - V2 Data Layer Implementation**