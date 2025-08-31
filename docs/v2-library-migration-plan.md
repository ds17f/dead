# V2 Library Architecture Migration Plan

## Source Material Analysis

The v1 LibraryV2 implementation is a comprehensive, well-architected library management system with:

### **Core Components (Source Material)**
1. **Domain Models**: `LibraryV2Show` with library-specific metadata (pin status, download status, library timestamps)
2. **Service Interface**: `LibraryV2Service` with full CRUD operations and reactive flows
3. **Service Implementation**: `LibraryV2ServiceStub` with realistic test data spanning 1960s-1990s
4. **UI Components**: Complete `LibraryV2Screen` with list/grid views, filtering, sorting, and bottom sheets
5. **ViewModel**: `LibraryV2ViewModel` with comprehensive service integration and debug logging
6. **Architecture Documentation**: Detailed `LibraryV2-Architecture-Plan.md` with domain model rationale

### **Key V1 Features to Port**
- **Pin Management**: Pin/unpin shows for prioritized display
- **Hierarchical Filtering**: Decade-based filtering (60s, 70s, 80s, 90s) with seasonal sub-filters
- **Advanced Sorting**: Date-based sorting with pin priority (pinned shows always first)
- **Download Integration**: Real-time download status overlay from DownloadService
- **Rich UI**: List/grid toggle, long-press actions, bottom sheets for actions/QR codes
- **Test Data System**: Realistic multi-decade concert data for immediate UI development

## V2 Migration Architecture

### **Module Structure (Following V2 Patterns)**

```
v2/
├── core/
│   ├── api/
│   │   └── library/
│   │       └── src/main/java/com/deadly/v2/core/api/library/
│   │           ├── LibraryService.kt (clean interface)
│   │           └── LibraryModels.kt (domain models)
│   ├── library/
│   │   └── src/main/java/com/deadly/v2/core/library/
│   │       ├── service/LibraryServiceImpl.kt (real implementation)
│   │       ├── service/LibraryServiceStub.kt (for development)
│   │       └── di/LibraryModule.kt (Hilt bindings)
│   ├── model/src/main/java/com/deadly/v2/core/model/
│   │   └── LibraryModels.kt (V2 domain models)
│   └── design/src/main/java/com/deadly/v2/core/design/component/
│       ├── LibraryButton.kt (unified library actions)
│       └── FilterComponents.kt (hierarchical filters)
└── feature/
    └── library/
        └── src/main/java/com/deadly/v2/feature/library/
            ├── screens/main/LibraryScreen.kt
            ├── screens/main/models/LibraryViewModel.kt
            ├── screens/main/components/ (focused components)
            └── navigation/LibraryNavigation.kt
```

### **Implementation Phases**

## **Phase 1: V2 Core Architecture (Foundation)** ✅
- [x] **Copy & Adapt Core Models**: Port `LibraryV2Show` → V2 `LibraryShow` with V2 model patterns
- [x] **Create V2 Service Interface**: Clean `LibraryService` interface following V2 API patterns
- [x] **Build V2 Service Stub**: Comprehensive stub with realistic multi-decade test data
- [x] **Setup V2 Hilt Module**: Dependency injection with V2 naming conventions

## **Phase 2: V2 UI Layer** ✅
- [x] **Port LibraryScreen**: Adapt to V2 component architecture and AppScaffold system
- [x] **Create Focused Components**: Extract reusable components following V2 patterns
- [x] **Build LibraryViewModel**: V2 ViewModel pattern with StateFlow and service delegation
- [x] **Setup Navigation**: V2 navigation integration with type-safe routing

## **Phase 3: Advanced Features**
- [ ] **Hierarchical Filtering**: Port decade/season filtering with V2 filter system
- [ ] **Pin Management**: Complete pin/unpin functionality with sorting priority
- [ ] **Download Integration**: Real-time download status overlay (when V2 download service available)
- [ ] **Bottom Sheets**: Actions, QR codes, and sort options following V2 modal patterns

## **Phase 4: Integration & Polish**
- [ ] **Database Layer**: V2 database entities and DAOs (when V2 database ready)
- [ ] **Real Service Implementation**: Replace stub with actual database operations
- [ ] **Performance Optimization**: Efficient sorting, filtering, and reactive updates
- [ ] **Testing**: Unit tests for domain models, service contracts, and UI components

### **V2 Architecture Compliance**

## **Service-Oriented Design**
```kotlin
// V2 Service Interface
interface LibraryService {
    suspend fun loadLibraryShows(): Result<Unit>
    fun getCurrentShows(): StateFlow<List<LibraryShow>>
    suspend fun addToLibrary(showId: String): Result<Unit>
    suspend fun pinShow(showId: String): Result<Unit>
    // ... other operations
}

// V2 Service Implementation with Direct Delegation
@Singleton
class LibraryServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val downloadService: DownloadService,
    @Named("LibraryScope") private val coroutineScope: CoroutineScope
) : LibraryService {
    // Real implementation with database integration
}
```

## **ViewModel Pattern** 
```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryService: LibraryService
) : ViewModel() {
    
    val uiState: StateFlow<LibraryUiState> = combine(
        libraryService.getCurrentShows(),
        libraryService.downloadStates
    ) { shows, downloadStates ->
        LibraryUiState.Success(
            shows = shows.map { show ->
                // Transform domain to UI models
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState.Loading)
}
```

## **Component Architecture**
```kotlin
// V2 Focused Components
@Composable
fun LibraryScreen() {
    AppScaffold(
        topBarConfig = TopBarConfig(title = "Your Library"),
        content = { paddingValues ->
            LibraryContent(...)
        }
    )
}

@Composable
fun LibraryShowList(
    shows: List<LibraryShowViewModel>,
    onShowClick: (String) -> Unit,
    // ... other callbacks
) {
    // Focused list component
}
```

### **Migration Benefits**

## **Architecture Quality**
- **Clean V2 Patterns**: Service-oriented design with focused responsibilities
- **No V1 Dependencies**: Complete independence from v1 legacy code
- **Reactive Architecture**: StateFlow-based with efficient UI updates
- **Testability**: Clean interfaces enable comprehensive testing

## **Feature Completeness**
- **Full Library Management**: Add, remove, pin, clear operations
- **Advanced UI**: List/grid toggle, hierarchical filtering, sorting with pin priority
- **Download Integration**: Real-time status updates from V2 download service
- **Rich Test Data**: Multi-decade concert data for immediate development

## **Performance**
- **Efficient Sorting**: Pin-aware sorting with single database queries
- **Reactive Updates**: Real-time UI updates without manual refreshes  
- **Memory Optimization**: Proper StateFlow usage and lifecycle management

This migration will create a fully-featured, architecturally sound V2 Library implementation that maintains all v1 functionality while following established V2 patterns and eliminating any v1 dependencies.

---

## Progress Tracking

### Current Status: Phase 2 Complete ✅ + Architecture Fixes Complete ✅

**Major Accomplishments:**
- [x] V1 LibraryV2 source material analysis complete
- [x] V2 architecture patterns documented and implemented
- [x] Migration plan created with 4-phase approach
- [x] Module structure defined following V2 conventions
- [x] **Phase 1 Complete**: V2 core architecture foundation implemented
- [x] **Phase 2 Complete**: V2 UI layer with proper V2 architecture patterns
- [x] **Critical Fixes**: Service plumbing, double scaffolding, V2 compliance

### Phase 1 Deliverables ✅
- [x] `v2/core/model/LibraryModels.kt` - V2 domain models with `LibraryShow`, `LibraryStats`, UI state models
- [x] `v2/core/api/library/LibraryService.kt` - Clean V2 service interface with StateFlow-based reactive operations
- [x] `v2/core/library/service/LibraryServiceStub.kt` - Comprehensive stub with realistic multi-decade test data
- [x] Hilt dependency injection with `@Named("stub")` qualifier following V2 patterns

### Phase 2 Deliverables ✅
- [x] `v2/feature/library/screens/main/LibraryScreen.kt` - Content-only screen following V2 patterns
- [x] `v2/feature/library/screens/main/LibraryBarConfiguration.kt` - Proper V2 bar configuration
- [x] `v2/app/navigation/BarConfiguration.kt` - Integrated library into main navigation config
- [x] `v2/feature/library/screens/main/models/LibraryViewModel.kt` - V2 ViewModel with service delegation
- [x] `v2/feature/library/screens/main/components/` - Focused UI components (SortControls, ShowItems, BottomSheets)
- [x] **Build Verification ✅**: Complete V2 Library UI working with proper architecture

### Critical Fixes & Architecture Discoveries ✅
- [x] **Service Plumbing Fix**: Resolved blocking `.collect()` operation that prevented data loading
- [x] **Double Scaffolding Fix**: Eliminated duplicate AppScaffold causing double padding
- [x] **V2 Pattern Compliance**: MainNavigation handles scaffolding, screens are content-only  
- [x] **Bar Configuration**: Proper V2 bar config system with centralized navigation management
- [x] **Reactive Flow Setup**: Non-blocking service initialization with auto-populated test data

### Real Service Implementation Plan (Phase 2.5)

Following V2 patterns established by PlaylistServiceImpl and MiniPlayerServiceImpl, implement:

#### **LibraryServiceImpl Architecture**
```kotlin
@Singleton  
class LibraryServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,              // Real show data
    private val libraryRepository: LibraryRepository,        // Library-specific data  
    private val downloadService: DownloadService,            // Shared download state
    private val mediaControllerRepository: MediaControllerRepository, // Media integration
    @Named("LibraryApplicationScope") private val coroutineScope: CoroutineScope
) : LibraryService
```

#### **Dependencies to Add**
1. **v2:core:domain** - Access to ShowRepository like PlaylistServiceImpl
2. **v2:core:media** - MediaController integration for shared state  
3. **LibraryRepository** - New repository for library-specific operations
4. **Real Database Integration** - Library entities and DAOs

#### **Service Implementation Pattern**
- **Direct Delegation**: No wrapper layers, direct repository access
- **StateFlow Exposure**: Service exposes StateFlow, ViewModel composes
- **Reactive Composition**: Use `combine()` for real-time updates
- **Repository Integration**: Follow ShowRepository patterns from existing V2 services

#### **Data Layer Architecture**
```kotlin
// New V2 entities
@Entity(tableName = "v2_library_shows")  
data class V2LibraryShowEntity(
    @PrimaryKey val showId: String,
    val addedToLibraryAt: Long,
    val isPinned: Boolean,
    val libraryNotes: String?
)

// Repository following V2 patterns
@Singleton
class LibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao,
    private val showRepository: ShowRepository
) {
    fun getLibraryShowsFlow(): Flow<List<LibraryShow>> = 
        libraryDao.getAllLibraryShows().combine(showRepository.getAllShows()) { ... }
}
```

### Phase 2.5: Incremental Real Service Implementation Strategy ✅

**Previous Working State:**
- LibraryServiceStub: 0 dependencies, comprehensive test data, working UI
- LibraryViewModel: `@Named("stub")` injection working  
- Build: Clean compile and install successful

#### **Step 1: Dependencies Validation** ✅
- [x] Add `v2:core:domain` to build.gradle.kts → **build test**
- [x] Add `v2:core:media` to build.gradle.kts → **build test**  
- [x] Verify ShowRepository, MediaControllerRepository available → **compilation test**

#### **Step 2: Shell Implementation** ✅
- [x] Create `LibraryServiceImpl` with real dependencies (ShowRepository, MediaControllerRepository)
- [x] Add `@Named("real")` Hilt binding → **build test**
- [x] Keep ViewModel using `@Named("stub")` → **runtime test**

#### **Step 3: Method Implementation Attempted** 🔄
- [x] Basic method signatures implemented
- [~] Real database integration blocked by v1 dependency issues
- **Lesson Learned**: V1 integration violates "No V1 Dependencies" principle

**Next Phase: Pure V2 Database Implementation** (Current Goal)

### Phase 3: Pure V2 Database Implementation ✅

**Objective:** Build clean V2 database layer with zero v1 dependencies

#### **Step 1: V2 Database Entities** ✅
- [x] Created `LibraryShowEntity` following V2 database patterns (`library_shows` table)
- [x] Defined complete schema: showId, addedToLibraryAt, isPinned, libraryNotes, customRating, lastAccessedAt, tags
- [x] Added foreign key relationship to ShowEntity following V2 entity patterns

#### **Step 2: V2 Data Access Layer** ✅
- [x] Created `LibraryDao` with comprehensive Room annotations
- [x] Implemented full CRUD operations with reactive Flow returns
- [x] Added statistics queries (show count, pinned count) 
- [x] Added to DeadlyDatabase component (entities + DAO method)
- [x] Incremented database version to 8

#### **Step 3: V2 Repository Layer** ✅
- [x] Created `LibraryRepository` integrating ShowRepository + LibraryDao
- [x] Converts between database entities and domain models (LibraryShow)
- [x] Implemented reactive `getLibraryShowsFlow()` with Show metadata enrichment
- [x] Added statistics flow for LibraryStats generation

#### **Step 4: Real LibraryServiceImpl** ✅
- [x] Completely replaced stub implementation with pure V2 architecture
- [x] Zero v1 dependencies - uses LibraryRepository + ShowRepository only
- [x] Direct delegation architecture following PlaylistServiceImpl patterns
- [x] All methods implemented using V2 database operations
- [x] StateFlow conversion for reactive UI integration

#### **Step 5: Integration** (Next Step)
- [ ] Switch ViewModel from `@Named("stub")` to `@Named("real")`  
- [ ] End-to-end testing with real V2 database operations
- [ ] Remove stub implementation from bindings

### Phase 3 Deliverables ✅

**Complete V2 Database Stack:**
- [x] `v2/core/database/entities/LibraryShowEntity.kt` - V2 database entity with foreign keys
- [x] `v2/core/database/dao/LibraryDao.kt` - Complete CRUD operations with reactive queries  
- [x] `v2/core/database/DeadlyDatabase.kt` - Updated with LibraryShowEntity and libraryDao()
- [x] `v2/core/library/repository/LibraryRepository.kt` - Domain model conversion and Show integration
- [x] `v2/core/library/service/LibraryServiceImpl.kt` - Pure V2 real implementation
- [x] **Build Status**: ✅ Compiles successfully with zero v1 dependencies

**Architecture Compliance:**
- ✅ **Zero V1 Dependencies**: Pure V2 implementation using only ShowRepository and LibraryRepository
- ✅ **Direct Delegation Pattern**: Following PlaylistServiceImpl architecture with real database operations
- ✅ **Reactive StateFlows**: All service methods return proper StateFlow for UI integration
- ✅ **Foreign Key Relationships**: LibraryShowEntity properly references ShowEntity
- ✅ **Database Migration**: Version 8 with clean schema design

**Ready for Integration:**
The complete V2 library database stack is built and functional. The final step is switching the ViewModel from `@Named("stub")` to `@Named("real")` to activate the real implementation with full V2 database operations.

### Legacy Implementation Plan (Reference)
1. **Add missing V2 dependencies** (domain, media repositories) ✅  
2. **Create LibraryRepository** following V2 database patterns ✅
3. **Implement LibraryServiceImpl** with direct delegation architecture ✅
4. **Replace stub binding** with real service in Hilt module (Next Step)
5. **Test end-to-end** library operations with real data (Next Step)