# Home Screen Port: Architecture & Implementation Plan

## Executive Summary
Complete guide for porting V1's sophisticated HomeV2Screen to V2 architecture with proper service separation, simplified Today in Grateful Dead History implementation, and correct StateFlow/Flow patterns.

## Architecture Overview

### Current State Analysis
- **V2 Home**: Simple logo/tagline (56 lines) - needs replacement
- **V1 HomeV2**: Rich content (502 lines) - source for port
- **Navigation Issue**: MainNavigation violates V2 patterns (direct composable vs graph call)

### Target Architecture (Simplified)
```
Service Layer:
├── ShowRepository (existing + new getShowsForDate method)
├── CollectionsService (curated show collections)
├── RecentShowsService (user activity tracking)
└── HomeService (orchestrator - composes above services)

UI Layer:
├── HomeScreen (main screen)
├── RecentShowsGrid (2x4 show grid)
├── TodayInHistoryCarousel (horizontal scroll)
└── CollectionsSection (horizontal scroll)

Future Extensibility:
├── Recent Activity Screen (detailed recent shows)
├── Today in History Screen (rich historical view)
└── Collections Browser Screen (full collection management)
```

## Service Architecture Detailed Design

### 1. ShowRepository (Enhanced, Not New Service)
**Module**: Existing `:v2:core:database` or wherever ShowRepository lives
**Enhancement**: Add date-based query method
**New Method**:
```kotlin
// Add to existing ShowRepository interface
suspend fun getShowsForDate(month: Int, day: Int): Result<List<Show>>
```
**Implementation**: Simple SQL query on existing show data
**Benefits**: No new modules needed, leverages existing infrastructure

### 2. CollectionsService
**Module**: `:v2:core:api:collections` + `:v2:core:collections`
**Purpose**: Curated show collections management
**Data Sources**: Predefined collections (Dick's Picks, Europe '72, etc.)
**Interface**:
```kotlin
interface CollectionsService {
    val featuredCollections: StateFlow<List<Collection>>  // Reactive state for UI
    suspend fun getAllCollections(): Result<List<Collection>>  // One-time queries
    suspend fun getCollectionShows(collectionId: String): Result<List<Show>>
}
```
**Components**: CollectionGrid, CollectionCard, CollectionDetailView
**Future Screens**: Collections browser, individual collection screens

### 3. RecentShowsService
**Module**: `:v2:core:api:recent` + `:v2:core:recent`
**Purpose**: Track user's recently played/viewed shows
**Data Sources**: **NEW** user activity tracking system
**Interface**:
```kotlin
interface RecentShowsService {
    val recentShows: StateFlow<List<Show>>  // Reactive state for UI
    suspend fun trackShowAccess(showId: String): Result<Unit>  // Actions
    suspend fun trackRecordingPlay(recordingId: String): Result<Unit>
    suspend fun getRecentActivity(limit: Int = 10): Result<List<RecentActivity>>  // One-time queries
}
```
**Components**: RecentShowsGrid, RecentShowCard, RecentActivityList
**Future Screens**: "Recent Activity" with detailed play history

### 4. HomeService (Orchestrator)
**Module**: `:v2:core:api:home` + `:v2:core:home`
**Purpose**: Compose data from other services for unified home experience
**Interface**:
```kotlin
interface HomeService {
    val homeContent: StateFlow<HomeContent>  // Reactive composed state
    suspend fun refreshAll(): Result<Unit>  // Actions
}

data class HomeContent(
    val recentShows: List<Show>,
    val todayInHistory: List<Show>,
    val featuredCollections: List<Collection>,
    val lastRefresh: Long
) {
    companion object {
        fun initial() = HomeContent(emptyList(), emptyList(), emptyList(), 0L)
    }
}
```

## StateFlow vs Flow Usage Patterns

### StateFlow Usage (Reactive UI State)
- **When**: UI needs to observe changes over time
- **Example**: Service properties that UI components collect
- **Pattern**: `val recentShows: StateFlow<List<Show>>`
- **UI**: `val uiState by viewModel.uiState.collectAsState()`

### Flow Usage (Data Streams)
- **When**: One-time data fetching or internal data processing
- **Example**: Database queries, API calls, internal data transformation
- **Pattern**: `suspend fun getShows(): Result<List<Show>>` or internal flows

### Combined Pattern in HomeService:
```kotlin
@Singleton
class HomeServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val collectionsService: CollectionsService,
    private val recentShowsService: RecentShowsService
) : HomeService {
    
    private val _homeContent = MutableStateFlow(HomeContent.initial())
    override val homeContent: StateFlow<HomeContent> = _homeContent.asStateFlow()
    
    init {
        serviceScope.launch {
            combine(
                recentShowsService.recentShows,        // StateFlow from service
                getTodayInHistoryFlow(),               // Internal Flow converted to StateFlow
                collectionsService.featuredCollections // StateFlow from service
            ) { recent, history, collections ->
                HomeContent(recent, history, collections, System.currentTimeMillis())
            }.collect { content ->
                _homeContent.value = content
            }
        }
    }
    
    private fun getTodayInHistoryFlow(): Flow<List<Show>> = flow {
        val today = LocalDate.now()
        val result = showRepository.getShowsForDate(today.monthValue, today.dayOfMonth)
        emit(result.getOrElse { emptyList() })
    }.stateIn(serviceScope, SharingStarted.Lazily, emptyList()) // Convert Flow to StateFlow
}
```

## Implementation Phases

### Phase 1: Navigation Architecture Fix (CRITICAL - 1 day)
**Problem**: MainNavigation directly defines home route instead of feature graph
**Current Code (WRONG)**:
```kotlin
// MainNavigation.kt lines 131-133
composable("home") {
    HomeScreen()
}
```
**Required Fix**:
```kotlin
// MainNavigation.kt
homeGraph(navController)

// HomeNavigation.kt
fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable("home") {
        HomeScreen(
            onNavigateToPlayer = { navController.navigate("player") },
            onNavigateToShow = { showId -> navController.navigate("playlist/$showId") },
            onNavigateToSearch = { navController.navigate("search") }
        )
    }
}
```

### Phase 2: UI-First Home Screen (3-4 days)
**Goal**: Replace simple home with rich content using stub HomeService

#### 2.1 Create Stub HomeService
```kotlin
@Singleton
class HomeServiceStub @Inject constructor() : HomeService {
    private val _homeContent = MutableStateFlow(
        HomeContent(
            recentShows = generateMockRecentShows(),
            todayInHistory = generateMockHistoryShows(),
            featuredCollections = generateMockCollections(),
            lastRefresh = System.currentTimeMillis()
        )
    )
    override val homeContent: StateFlow<HomeContent> = _homeContent.asStateFlow()
    
    override suspend fun refreshAll(): Result<Unit> {
        // Mock refresh logic
        _homeContent.value = _homeContent.value.copy(lastRefresh = System.currentTimeMillis())
        return Result.success(Unit)
    }
}
```

#### 2.2 Port HomeScreen UI
**Key Adaptations**:
- Remove Scaffold (use AppScaffold)
- Port RecentShowsGrid (2x4 layout)
- Port HorizontalCollection components
- Add navigation callbacks
- Integrate debug system

**Component Structure**:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            RecentShowsGrid(
                shows = uiState.homeContent.recentShows,
                onShowClick = onNavigateToShow
            )
        }
        item {
            TodayInHistorySection(
                shows = uiState.homeContent.todayInHistory,
                onShowClick = onNavigateToShow
            )
        }
        item {
            CollectionsSection(
                collections = uiState.homeContent.featuredCollections,
                onCollectionClick = { /* TODO */ }
            )
        }
    }
}
```

### Phase 3A: Add Date Query to ShowRepository (EASIEST - 1 day)
**Why First**: Just adding one method to existing repository
- Add `getShowsForDate(month: Int, day: Int)` to ShowRepository interface
- Implement in ShowRepositoryImpl with simple SQL query: `SELECT * FROM shows WHERE month = ? AND day = ?`
- No new modules needed
- Leverages existing database infrastructure

### Phase 3B: CollectionsService Implementation (MEDIUM - 2-3 days)
**Why Second**: Mostly static curated data
**Implementation**:
- Define collection data structure
- Implement predefined collections (Dick's Picks, Europe '72, etc.)
- Create collection-to-shows relationships
- StateFlow for reactive featured collections

### Phase 3C: RecentShowsService Implementation (HARDEST - 4-5 days)
**Why Last**: Requires new infrastructure
**Implementation**:
- **Database changes**: New tables for user activity tracking
- **Service modifications**: Need to modify Player, Playlist, Search services to record when users access shows/recordings
- **Backend integration**: User session tracking, play history, visit tracking
- **Privacy considerations**: User data collection and storage

### Phase 4: HomeService Real Implementation (1-2 days)
**Goal**: Replace stub with real service orchestration
**Implementation**:
- Inject ShowRepository + CollectionsService + RecentShowsService
- Combine StateFlows efficiently using `combine()`
- Handle refresh logic across all services
- Error handling and fallbacks

## Technical Requirements

### AppScaffold Integration
- **NO Scaffold in HomeScreen** - content only
- Work within AppScaffold padding system
- Update HomeBarConfiguration for top bar needs

### V2 Navigation Patterns
- Feature owns navigation through homeGraph()
- Navigation callbacks from MainNavigation
- Type-safe navigation parameters

### Model Mapping
**Challenge**: V1 Show model ≠ V2 Show model
**Solution**: Mapping functions for model conversion
```kotlin
private fun mapV1ShowToV2(v1Show: V1Show): V2Show {
    return V2Show(
        id = v1Show.showId,
        date = v1Show.date,
        venue = mapVenue(v1Show.venue),
        // Handle structure differences
    )
}
```

### Debug Integration
- HomeDebugDataFactory following V2 patterns
- Debug panel with service state information
- Performance metrics for service orchestration
- StateFlow observation debugging

## Module Dependencies

### New Modules Required (Simplified)
```
v2/
├── core/
│   ├── api/
│   │   ├── collections/     # CollectionsService interface
│   │   ├── recent/          # RecentShowsService interface
│   │   └── home/           # HomeService interface
│   ├── collections/        # CollectionsService implementation
│   ├── recent/             # RecentShowsService implementation
│   └── home/              # HomeService implementation
└── feature/
    └── home/              # Updated with rich HomeScreen
```
**Note**: No history module needed - using enhanced ShowRepository instead

### Dependency Flow
```
HomeService depends on:
├── ShowRepository (existing + new method)
├── CollectionsService
└── RecentShowsService

Each service depends on:
├── V2 Database modules
├── V2 Model definitions
└── V2 Network (if needed)
```

## Risk Assessment & Mitigation

### High Risk Items
1. **Model mapping complexity** - V1/V2 Show model differences
2. **RecentShowsService complexity** - Requires modifying multiple existing services
3. **Database schema changes** - User activity tracking tables
4. **StateFlow performance** - Multiple combined StateFlows

### Mitigation Strategies
1. **Incremental development** - UI first with stubs
2. **Start with easiest enhancement** - ShowRepository date query first
3. **Thorough testing** - Each service independently
4. **Performance monitoring** - Debug panels for StateFlow timing
5. **Fallback strategies** - Graceful degradation if services fail

## Success Metrics
- [ ] Navigation architecture fixed (no direct composable in MainNavigation)
- [ ] Rich HomeScreen replaces simple logo screen
- [ ] ShowRepository date query working with real data
- [ ] CollectionsService providing curated collections
- [ ] RecentShowsService tracking user activity
- [ ] HomeService orchestration performing well with combined StateFlows
- [ ] Debug integration complete
- [ ] Future extensibility demonstrated

## Future Roadmap
- **Recent Activity Screen** - Detailed user history
- **Today in History Screen** - Rich historical exploration
- **Collections Browser** - Full collection management
- **Personalized Recommendations** - ML-based suggestions
- **Social Features** - Share recent listening activity

This comprehensive document serves as our complete implementation guide with simplified TIGDH approach and proper StateFlow/Flow usage patterns.