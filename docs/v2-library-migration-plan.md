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

### Current Status: Phase 2 Complete ✅
- [x] V1 LibraryV2 source material analysis complete
- [x] V2 architecture patterns documented
- [x] Migration plan created with 4-phase approach
- [x] Module structure defined following V2 conventions
- [x] **Phase 1 Complete**: V2 core architecture foundation implemented
- [x] **Phase 2 Complete**: V2 UI layer with AppScaffold integration

### Phase 1 Deliverables ✅
- [x] `v2/core/model/LibraryModels.kt` - V2 domain models with `LibraryShow`, `LibraryStats`, UI state models
- [x] `v2/core/api/library/LibraryService.kt` - Clean V2 service interface with StateFlow-based reactive operations
- [x] `v2/core/library/service/LibraryServiceStub.kt` - Comprehensive stub with realistic multi-decade test data
- [x] `v2/core/library/di/LibraryModule.kt` - Hilt dependency injection following V2 patterns

### Phase 2 Deliverables ✅
- [x] `v2/feature/library/screens/main/LibraryScreen.kt` - V2 main screen with AppScaffold integration
- [x] `v2/feature/library/screens/main/models/LibraryViewModel.kt` - V2 ViewModel with service delegation
- [x] `v2/feature/library/screens/main/components/` - Focused UI components (SortControls, ShowItems, BottomSheets)
- [x] `v2/feature/library/navigation/LibraryNavigation.kt` - Type-safe V2 navigation routing
- [x] **Build Verification ✅**: Complete V2 Library UI compiles and installs successfully

### Next Steps
1. Begin Phase 3: Advanced Features
2. Implement hierarchical filtering with V2 filter system
3. Complete pin management with sorting priority
4. Add download integration and bottom sheet functionality