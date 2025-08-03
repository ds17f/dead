# HomeV2 Implementation Guide

## Quick Start

HomeV2 follows the established V2 architecture pattern with UI-first development and service composition. This implementation represents the fourth successful V2 feature, building on proven patterns from LibraryV2, PlayerV2, and SearchV2.

### Enable HomeV2
1. Navigate to **Settings → Developer Options → Experimental Features**
2. Enable **"Use HomeV2 Interface"** toggle
3. Tap **Home** button to see the new interface

### Key Files
```
feature/browse/src/main/java/com/deadarchive/feature/browse/
├── HomeV2Screen.kt                 # Main UI implementation
├── HomeV2ViewModel.kt              # State management
└── debug/HomeV2DebugData.kt        # Debug support
```

## Architecture Overview

### V2 Pattern Implementation
HomeV2 follows the established V2 architecture with these components:

1. **UI-First Development**: Complete UI built before service integration
2. **StateFlow Management**: Reactive state with `HomeV2UiState`
3. **Feature Flag Control**: Safe deployment with `useHomeV2` setting
4. **Debug Integration**: DebugBottomSheet for development support
5. **Service Readiness**: Architecture prepared for future integration

### Component Structure
```kotlin
@Composable
fun HomeV2Screen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    initialEra: String? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
)
```

## UI Implementation

### Enhanced V2TopBar with Filter Integration
HomeV2 introduces the enhanced V2TopBar with backward-compatible titleContent support:

```kotlin
V2TopBar(
    titleContent = {
        HierarchicalFilter(
            filterTree = FilterTrees.buildHomeFiltersTree(),
            selectedPath = filterPath,
            onSelectionChanged = { filterPath = it }
        )
    },
    actions = {
        IconButton(onClick = { /* Settings */ }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
)
```

### Production Layout Structure
The interface uses three main sections optimized for discovery and browsing:

#### 1. Recent Shows Grid (2x4 Layout)
Ultra-compact horizontal cards for recently played shows:
- **Layout**: LazyVerticalGrid with 2 columns, 4 rows (8 total cards)
- **Card Design**: 64dp height horizontal cards with minimal 4dp padding
- **Album Art**: 56dp prominent placement just 4dp from card edge
- **Non-scrolling**: Grid holds its position in main scroll flow
- **Metadata**: Date and location with proper typography hierarchy

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = modifier.height(268.dp), // Compact fixed height
    userScrollEnabled = false, // Non-scrolling grid
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    items(shows.take(8)) { show ->
        RecentShowCard(show = show, ...)
    }
}
```

#### 2. Today In Grateful Dead History
Horizontal scrolling collection for historical concerts:
- **Design**: 160dp square cards with large album art placeholders
- **Content**: Shows from today's date in Dead history
- **Layout**: Left-aligned text with generous spacing for visual impact

#### 3. Explore Collections
Discovery section for curated content categories:
- **Categories**: Greatest Shows, Rare Recordings, Europe '72, Wall of Sound, Dick's Picks, Acoustic Sets
- **Design**: 160dp square cards matching history section
- **Purpose**: Future expansion for collection-based browsing

### Material3 Design Integration
```kotlin
// Compact show cards with proper theming
Card(
    modifier = Modifier.height(64.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Row(modifier = Modifier.padding(4.dp)) {
        // Ultra-minimal padding for maximum content density
    }
}
```

### Responsive Layout Spacing
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    // Optimized spacing between major sections
    // No top content padding for maximum screen usage
}
```

## State Management

### HomeV2UiState
```kotlin
data class HomeV2UiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val welcomeText: String = "Welcome to Dead Archive",
    val recentShows: List<Show> = emptyList(),
    val todayInHistory: List<Show> = emptyList(),
    val exploreCollections: List<String> = emptyList()
) {
    val hasError: Boolean get() = errorMessage != null
}
```

### ViewModel Implementation
```kotlin
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    // TODO: Add V2 services as they're discovered through UI development
    // private val homeV2Service: HomeV2Service
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeV2UiState.initial())
    val uiState: StateFlow<HomeV2UiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitialized = true,
            recentShows = generateMockRecentShows(),
            todayInHistory = generateMockTodayInHistory(),
            exploreCollections = generateMockCollections()
        )
    }
    
    private fun generateMockRecentShows(): List<Show> {
        // Returns 16 realistic Dead shows for 2x4 grid + overflow
        return listOf(/* Cornell '77, Fillmore shows, etc. */)
    }
    
    private fun generateMockTodayInHistory(): List<Show> {
        // Returns shows from today's date in Dead history
        return listOf(/* Date-matched historical shows */)
    }
    
    private fun generateMockCollections(): List<String> {
        return listOf(
            "Greatest Shows", "Rare Recordings", "Europe '72",
            "Wall of Sound", "Dick's Picks", "Acoustic Sets"
        )
    }
}
```

## Feature Flag Integration

### Settings Implementation
Complete DataStore integration with Settings UI:

```kotlin
// AppSettings.kt - Feature flag definition
data class AppSettings(
    val useHomeV2: Boolean = false,
    // ... other settings
)

// SettingsScreen.kt - UI toggle
Switch(
    checked = settings.useHomeV2,
    onCheckedChange = viewModel::updateUseHomeV2
)
```

### Navigation Routing
Precise routing to Home button (not Search button):

```kotlin
// MainAppScreen.kt - Home route
composable("home") {
    if (settings.useHomeV2) {
        com.deadarchive.feature.browse.HomeV2Screen(
            onNavigateToPlayer = { recordingId -> 
                navController.navigate("playlist/$recordingId")
            },
            onNavigateToShow = { show ->
                show.bestRecording?.let { recording ->
                    navController.navigate("playlist/${recording.identifier}?showId=${show.showId}")
                }
            }
        )
    } else {
        HomeScreen(
            // Original home screen
        )
    }
}
```

## Debug Integration

### DebugBottomSheet Implementation
Following established V2 pattern:

```kotlin
// Debug state management
var showDebugPanel by remember { mutableStateOf(false) }
val debugData = if (settings.showDebugInfo) {
    HomeV2DebugDataFactory.createDebugData(uiState, initialEra)
} else null

// Debug activator button
if (settings.showDebugInfo && debugData != null) {
    DebugActivator(
        isVisible = true,
        onClick = { showDebugPanel = true },
        modifier = Modifier.align(Alignment.BottomEnd)
    )
}

// Debug bottom sheet
debugData?.let { data ->
    DebugBottomSheet(
        debugData = data,
        isVisible = showDebugPanel,
        onDismiss = { showDebugPanel = false }
    )
}
```

### Debug Data Structure
Comprehensive debug information for development:

```kotlin
object HomeV2DebugDataFactory {
    fun createDebugData(
        uiState: HomeV2UiState,
        initialEra: String?
    ): DebugData {
        return DebugData(
            screenName = "HomeV2Screen",
            sections = listOf(
                createGeneralSection(initialEra),
                createUiStateSection(uiState), // Shows recent shows count, history count, collections count
                createArchitectureSection(),
                createDevelopmentStatusSection()
            )
        )
    }
    
    private fun createUiStateSection(uiState: HomeV2UiState): DebugSection {
        return DebugSection(
            title = "UI State",
            items = listOf(
                DebugItem.BooleanValue("Is Loading", uiState.isLoading),
                DebugItem.BooleanValue("Is Initialized", uiState.isInitialized),
                DebugItem.BooleanValue("Has Error", uiState.hasError),
                DebugItem.KeyValue("Error Message", uiState.errorMessage ?: "None"),
                DebugItem.NumericValue("Recent Shows Count", uiState.recentShows.size),
                DebugItem.NumericValue("Today In History Count", uiState.todayInHistory.size),
                DebugItem.NumericValue("Collections Count", uiState.exploreCollections.size)
            )
        )
    }
}
```

## Service Architecture Readiness

### Future Service Integration
Architecture prepared for service composition:

```kotlin
// Future service interface
interface HomeV2Service {
    suspend fun loadFeaturedContent(): List<Show>
    suspend fun loadRecentAdditions(): List<Show>
    suspend fun loadQuickActions(): List<String>
    fun getWelcomeMessage(): Flow<String>
}

// Ready for injection in ViewModel
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    private val homeV2Service: HomeV2Service, // When implemented
    private val downloadV2Service: DownloadV2Service, // Already available
    private val libraryV2Service: LibraryV2Service // Already available
) : ViewModel()
```

### V2 Service Integration Points
Ready for integration with existing V2 services:

- **DownloadV2Service**: Show download status in featured content
- **LibraryV2Service**: Display recent library additions
- **SearchV2Service**: Quick search access (when implemented)
- **MediaV2Service**: Recently played content (when implemented)

## Development Workflow

### 1. Building and Testing
```bash
# Build and install with feature flag
make install-quiet

# Enable debug mode in Settings
# Enable HomeV2 in Developer Options
# Navigate to Home button to test
```

### 2. Debug Panel Usage
1. Enable **"Show Debug Info"** in Settings
2. Navigate to Home with HomeV2 enabled
3. Tap debug activator (bottom-right button)
4. View comprehensive debug information

### 3. Feature Flag Testing
- **Enabled**: Shows HomeV2Screen with Material3 cards
- **Disabled**: Shows original HomeScreen
- **Safe Rollback**: Instant toggle without restart

## Testing Guidelines

### Manual Testing Checklist
- [ ] Feature flag toggle works in Settings
- [ ] Home button shows HomeV2 when enabled
- [ ] Home button shows original when disabled
- [ ] Debug panel activates correctly
- [ ] Material3 theming renders properly
- [ ] Cards display with proper spacing
- [ ] Navigation buttons work correctly

### Debug Panel Verification
- [ ] General information shows correct screen name
- [ ] UI state reflects current component state
- [ ] Architecture section shows V2 patterns
- [ ] Development status indicates completion

## Performance Considerations

### Efficiency Features
- **Lazy Loading**: LazyColumn for efficient scrolling
- **State Optimization**: Single StateFlow source
- **Component Isolation**: Independent card components
- **Memory Management**: Proper Compose lifecycle

### Resource Usage
- **Minimal Dependencies**: Only essential V2 services
- **Efficient Rendering**: Material3 optimized components
- **State Management**: Reactive updates without polling
- **Debug Overhead**: Only when debug mode enabled

## Troubleshooting

### Common Issues

#### HomeV2 Not Appearing
1. Verify **useHomeV2** flag enabled in Settings
2. Check that you're tapping **Home** button, not Search
3. Restart app if navigation state is cached

#### Debug Panel Not Working
1. Ensure **"Show Debug Info"** enabled in Settings
2. Look for debug activator button in bottom-right
3. Check that debug data is being created

#### Navigation Problems
1. Verify correct routing in MainAppScreen.kt
2. Check feature flag state in Settings
3. Ensure proper navigation parameter passing

### Development Support

#### Adding New Content Cards
```kotlin
// Add to LazyColumn in HomeV2Screen
item {
    HomeV2NewFeatureCard()
}

// Implement new card component
@Composable
private fun HomeV2NewFeatureCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        // Card content implementation
    }
}
```

#### Extending Debug Information
```kotlin
// Add to HomeV2DebugDataFactory
private fun createNewSection(): DebugSection {
    return DebugSection(
        title = "New Feature Debug",
        items = listOf(
            DebugItem.KeyValue("Feature", "New functionality"),
            DebugItem.BooleanValue("Enabled", true)
        )
    )
}
```

## Future Enhancement Opportunities

### Interactive Features
1. **Progress Bars**: Show playback progress on Recent Shows cards
2. **Context Menus**: Long-press actions (Share, Library, Download, Go To Show)
3. **Real-time Updates**: Live sync with playback state
4. **Quick Actions**: Direct navigation to player/library

### Service Integration
1. **HomeV2Service**: Content loading and personalization
2. **MediaV2Service**: Recently played shows integration
3. **LibraryV2Service**: Library status and recent additions
4. **DownloadV2Service**: Download status indicators

### Content Enhancement
1. **Dynamic Collections**: Real-time Archive.org integration
2. **Personalized Recommendations**: User-specific discovery
3. **Seasonal Content**: Date-based historical shows
4. **Featured Content**: Popular and trending concerts

### Performance Optimizations
1. **Image Loading**: Album art from Archive.org
2. **Data Caching**: Offline-first content strategy
3. **Lazy Loading**: Progressive content discovery
4. **State Persistence**: Resume on app restart

---

**Implementation Status**: Production Layout Complete ✅  
**UI Architecture**: Fully Developed with Material3 Design  
**Service Integration**: Ready for V2 Service Composition  
**Debug Support**: Comprehensive Development Tools  
**Production Safety**: Feature Flag Protected with Safe Rollback  

HomeV2 represents the culmination of V2 architecture maturity, delivering a production-ready home screen with ultra-compact Recent Shows, prominent discovery collections, and seamless integration points for future service development.