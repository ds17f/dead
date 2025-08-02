# SearchV2 Technical Implementation Guide

## Overview

This document provides a comprehensive technical deep-dive into the SearchV2 implementation, covering the actual code structure, architectural patterns, and development approaches used to build the third V2 feature in the Dead Archive app.

**Implementation Status**: UI Complete - Professional Material3 interface with comprehensive component architecture  
**Architecture Pattern**: V2 UI-first development with component composition  
**File Count**: 2 core files, 511 total lines of implementation code

## File Structure & Organization

### Core Implementation Files

```
feature/browse/src/main/java/com/deadarchive/feature/browse/
├── SearchV2Screen.kt                 # 511 lines - Complete UI implementation
├── SearchV2ViewModel.kt              # 94 lines - State management foundation
└── navigation/BrowseNavigation.kt    # Modified for SearchV2 routing
```

### Supporting Infrastructure

```
core/settings-api/src/main/java/com/deadarchive/core/settings/api/model/
└── AppSettings.kt                    # Feature flag: useSearchV2: Boolean

app/src/main/java/com/deadarchive/app/
├── MainAppScreen.kt                  # Navigation parameter passing
└── DeadArchiveNavigation.kt          # Feature flag routing
```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        SearchV2 Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer (Compose - 511 lines)                               │
│  ├── SearchV2Screen.kt (LazyColumn layout)                     │
│  ├── SearchV2TopBar (SYF + title + camera)                     │
│  ├── SearchV2SearchBox (Material3 text field)                  │
│  ├── SearchV2BrowseSection (4 decade cards)                    │
│  ├── SearchV2DiscoverSection (3 discovery cards)               │
│  ├── SearchV2BrowseAllSection (2-column grid)                  │
│  └── Individual components (DecadeCard, DiscoverCard, etc.)    │
├─────────────────────────────────────────────────────────────────┤
│  State Management (94 lines)                                   │
│  ├── SearchV2ViewModel (Hilt + StateFlow)                      │
│  ├── SearchV2UiState (Basic state model)                       │
│  └── Ready for SearchV2Service injection                       │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer (Domain Models)                                    │
│  ├── DecadeBrowse (title, gradient, era)                       │
│  ├── DiscoverItem (title, subtitle)                            │
│  └── BrowseAllItem (title, subtitle, searchQuery)              │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure                                                │
│  ├── Feature Flag Control (useSearchV2)                        │
│  ├── Navigation Routing (V1/V2 switching)                      │
│  ├── Debug System Integration                                  │
│  └── Material3 Design System                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Component Architecture Deep-Dive

### 1. SearchV2Screen - Main Coordinator (511 lines)

**Primary Structure**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchV2Screen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    initialEra: String? = null,
    viewModel: SearchV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // State collection
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Debug panel state
    var showDebugPanel by remember { mutableStateOf(false) }
    
    // LazyColumn layout with component composition
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SearchV2TopBar(...) }
        item { SearchV2SearchBox(...) }
        item { SearchV2BrowseSection(...) }
        item { SearchV2DiscoverSection(...) }
        item { SearchV2BrowseAllSection(...) }
    }
}
```

**Key Architectural Decisions**:
- **LazyColumn Layout**: Prevents performance issues with complex layouts
- **Component Composition**: Each row is a separate, focused component
- **State Flow Integration**: Ready for reactive data updates
- **Debug Integration**: Follows PlayerV2 debug panel patterns

### 2. SearchV2TopBar - Header Component (32 lines)

**Implementation Pattern**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchV2TopBar(onCameraClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: SYF logo + Search title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                    contentDescription = "Dead Archive",
                    modifier = Modifier.size(32.dp)
                )
                Text("Search", style = MaterialTheme.typography.headlineSmall)
            }
            
            // Right: Camera/QR scanner icon
            IconButton(onClick = onCameraClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "QR Code Scanner")
            }
        }
    }
}
```

**Design Principles**:
- **Material3 Surface**: Proper elevation and theming
- **Consistent Spacing**: 16dp padding following design system
- **Resource Integration**: Uses existing SYF drawable from core.design
- **Icon Placeholder**: Settings icon as temporary camera placeholder

### 3. SearchV2SearchBox - Input Component (25 lines)

**Material3 Integration**:
```kotlin
@Composable
private fun SearchV2SearchBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { 
            Text(
                text = "What do you want to listen to",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
```

**Implementation Notes**:
- **White Background Override**: Forced white background as specified
- **Material3 Colors**: Proper tinting with theme-aware colors
- **Rounded Corners**: 12dp corner radius for modern appearance
- **Single Line Input**: Optimized for search queries

### 4. SearchV2BrowseSection - Decade Navigation (30 lines + 42 lines DecadeCard)

**Data-Driven Implementation**:
```kotlin
@Composable
private fun SearchV2BrowseSection(onDecadeClick: (String) -> Unit) {
    val decades = listOf(
        DecadeBrowse("1960s", listOf(Color(0xFF1976D2), Color(0xFF42A5F5)), "1960s"),
        DecadeBrowse("1970s", listOf(Color(0xFF388E3C), Color(0xFF66BB6A)), "1970s"),
        DecadeBrowse("1980s", listOf(Color(0xFFD32F2F), Color(0xFFEF5350)), "1980s"),
        DecadeBrowse("1990s", listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)), "1990s")
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Text("Start Browsing", style = MaterialTheme.typography.titleLarge)
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(decades) { decade ->
                DecadeCard(decade = decade, onClick = { onDecadeClick(decade.era) })
            }
        }
    }
}
```

**DecadeCard Visual Design**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecadeCard(decade: DecadeBrowse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp).height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(decade.gradient),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // SYF watermark (right-justified, 30% opacity)
            Image(
                painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                modifier = Modifier.size(40.dp).align(Alignment.BottomEnd).padding(8.dp),
                alpha = 0.3f
            )
            
            // Decade text (bottom-left)
            Text(
                text = decade.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }
    }
}
```

**Visual Design Features**:
- **Gradient Backgrounds**: Unique color combinations per decade
- **SYF Watermarks**: 30% opacity, right-justified placement
- **Color Consistency**: Each decade has distinct visual identity
- **Typography**: Bold white text for high contrast

### 5. SearchV2BrowseAllSection - Category Grid (40 lines + 35 lines BrowseAllCard)

**Grid Layout Implementation**:
```kotlin
@Composable
private fun SearchV2BrowseAllSection(onBrowseAllClick: (BrowseAllItem) -> Unit) {
    val browseAllItems = listOf(
        BrowseAllItem("Popular Shows", "Most listened to concerts", "popular"),
        BrowseAllItem("Recent Uploads", "Latest additions to Archive.org", "recent"),
        BrowseAllItem("Top Rated", "Highest community ratings", "top-rated"),
        BrowseAllItem("Audience Recordings", "Taped from the crowd", "audience"),
        BrowseAllItem("Soundboard", "Direct from the mixing board", "soundboard"),
        BrowseAllItem("Live Albums", "Official releases", "official")
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Text("Browse All", style = MaterialTheme.typography.titleLarge)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(400.dp) // Fixed height for demo
        ) {
            items(browseAllItems) { item ->
                BrowseAllCard(item = item, onClick = { onBrowseAllClick(item) })
            }
        }
    }
}
```

**BrowseAllCard Implementation**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseAllCard(item: BrowseAllItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp), // 2x height
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
```

**Grid Design Features**:
- **2-Column Layout**: Responsive grid with fixed 2 columns
- **2x Height Cards**: 120dp height (2x decade cards as specified)
- **Primary Container**: Consistent Material3 theming
- **Title/Subtitle Layout**: Clear information hierarchy

## Data Model Design

### Domain Models for UI State

```kotlin
// Data classes for UI components
data class DecadeBrowse(
    val title: String,
    val gradient: List<Color>,
    val era: String
)

data class DiscoverItem(
    val title: String,
    val subtitle: String = ""
)

data class BrowseAllItem(
    val title: String,
    val subtitle: String,
    val searchQuery: String
)
```

### ViewModel State Management

```kotlin
@HiltViewModel
class SearchV2ViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchV2UiState())
    val uiState: StateFlow<SearchV2UiState> = _uiState.asStateFlow()
    
    // Foundation ready for service injection
    // Future: SearchV2Service dependency
}

data class SearchV2UiState(
    val isLoading: Boolean = false,
    val error: String? = null
    // Future: search results, discovery content, etc.
)
```

## Feature Flag Integration

### Settings Infrastructure

**AppSettings Model**:
```kotlin
data class AppSettings(
    // UI settings
    val useLibraryV2: Boolean = false,
    val usePlayerV2: Boolean = false,
    val useSearchV2: Boolean = false, // ✅ Added
    // ...
)
```

**Navigation Routing**:
```kotlin
// BrowseNavigation.kt
fun NavGraphBuilder.browseScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    useSearchV2: Boolean = false  // ✅ Feature flag parameter
) {
    composable("browse") {
        if (useSearchV2) {
            SearchV2Screen(...)  // ✅ V2 routing
        } else {
            BrowseScreen(...)    // V1 fallback
        }
    }
}

// MainAppScreen.kt
browseScreen(
    onNavigateToPlayer = { ... },
    onNavigateToShow = { ... },
    useSearchV2 = settings.useSearchV2  // ✅ Feature flag passed through
)
```

## Performance Considerations

### Lazy Composition Strategy

**LazyColumn Main Layout**:
- **Benefit**: Only renders visible components
- **Memory**: Efficient for scrollable content
- **Composition**: Each item() is independently composable

**LazyRow for Decade Cards**:
- **Benefit**: Horizontal scrolling with minimal memory impact
- **Performance**: Only renders visible decade cards
- **Spacing**: Consistent 12dp spacing via Arrangement.spacedBy()

**LazyVerticalGrid for Browse All**:
- **Benefit**: Efficient 2-column layout rendering
- **Memory**: Only renders visible grid items
- **Fixed Height**: 400dp container prevents infinite height issues

### State Management Efficiency

**StateFlow Integration**:
```kotlin
// Efficient state observation
val uiState by viewModel.uiState.collectAsState()
val settings by settingsViewModel.settings.collectAsState()

// Debug state with proper lifecycle
var showDebugPanel by remember { mutableStateOf(false) }
```

**Component Isolation**:
- Each component only recomposes when its specific data changes
- Props-based data flow prevents unnecessary recompositions
- Callback functions are stable and don't cause recomposition cascades

## Debug System Integration

### Debug Data Collection

```kotlin
@Composable
private fun collectSearchV2DebugData(
    uiState: SearchV2UiState,
    initialEra: String?
): DebugData {
    return DebugData(
        screenName = "SearchV2Screen",
        sections = listOf(
            DebugSection(
                title = "SearchV2 State",
                items = listOf(
                    DebugItem.KeyValue("Is Loading", uiState.isLoading.toString()),
                    DebugItem.KeyValue("Error State", uiState.error ?: "None"),
                    DebugItem.KeyValue("Initial Era", initialEra ?: "None"),
                    DebugItem.KeyValue("Feature Flag", "useSearchV2 = true")
                )
            )
        )
    )
}
```

### Debug Panel Implementation

**Conditional Rendering**:
```kotlin
// Debug activator (bottom-right floating button)
if (settings.showDebugInfo && debugData != null) {
    DebugActivator(
        isVisible = true,
        onClick = { showDebugPanel = true },
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
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

## Next Phase: Service Integration

### Service Interface Discovery

UI implementation naturally revealed service requirements:

```kotlin
interface SearchV2Service {
    // Search functionality
    suspend fun searchShows(query: String): Flow<List<Show>>
    
    // Decade browsing
    suspend fun getShowsByEra(era: String): Flow<List<Show>>
    
    // Discovery content
    suspend fun getDiscoveryContent(): Flow<List<DiscoverItem>>
    
    // Browse categories
    suspend fun getBrowseCategories(): Flow<List<BrowseAllItem>>
    suspend fun getShowsByCategory(searchQuery: String): Flow<List<Show>>
}
```

### Service Integration Strategy

1. **Stub Implementation**: Create SearchV2ServiceStub with realistic mock data
2. **Real Implementation**: Wrap existing Archive.org API calls
3. **StateFlow Wiring**: Connect service flows to ViewModel state
4. **Navigation Integration**: Wire click handlers to actual screen navigation
5. **Error Handling**: Add proper loading states and error recovery

## Code Quality Metrics

**Component Architecture**:
- **Single Responsibility**: Each component has one clear purpose
- **Material3 Compliance**: 100% design system integration
- **Type Safety**: Proper data classes for all UI state
- **Memory Efficiency**: Lazy composition prevents performance issues

**Line Count Analysis**:
- **SearchV2Screen**: 511 lines (comprehensive UI implementation)
- **SearchV2ViewModel**: 94 lines (clean state management)
- **Component Average**: ~35 lines per component (focused, maintainable)
- **Total Implementation**: 605 lines (UI + ViewModel + data classes)

**Maintainability Features**:
- **Component Isolation**: Each UI section is independently testable
- **Props-Based Data Flow**: Clear dependency injection points
- **Callback Architecture**: Ready for service integration
- **Debug Integration**: Development tools built-in from foundation

---

**Implementation Status**: ✅ **Complete UI with Component Architecture**  
**Next Milestone**: SearchV2Service integration and real data implementation  
**Architecture Achievement**: Clean V2 patterns with professional Material3 design  
**Created**: January 2025

SearchV2 implementation successfully demonstrates the effectiveness of V2 UI-first development, creating a maintainable, performant, and visually excellent search interface ready for service integration.