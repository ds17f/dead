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

### Material3 Design System
HomeV2 uses complete Material3 theming with proper color schemes:

```kotlin
// Welcome card with primary container theming
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
) {
    Text(
        text = "Welcome to HomeV2 🚀",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
```

### Card-Based Layout
The interface uses three main content cards:

#### 1. Welcome Card
- **Purpose**: Introduction to HomeV2 and V2 architecture
- **Theming**: Primary container with proper color contrast
- **Content**: V2 architecture explanation and development approach

#### 2. Development Status Card
- **Purpose**: Real-time V2 implementation progress
- **Features**: Checkmark indicators for completed features
- **Status Items**: Foundation, Settings, Navigation, Debug integration

#### 3. Foundation Card
- **Purpose**: Technical architecture highlights
- **Content**: Material3, Service-Oriented Architecture, Debug integration
- **Theming**: Surface variant with proper contrast

### Spacing and Layout
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    // 24dp spacing between cards
    // 16dp content padding around entire column
}
```

## State Management

### HomeV2UiState
```kotlin
data class HomeV2UiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val welcomeText: String = "Welcome to Dead Archive V2",
    val featuredShows: List<Show> = emptyList(),
    val quickActions: List<String> = emptyList()
)
```

### ViewModel Implementation
```kotlin
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    // Ready for service injection when needed
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeV2UiState())
    val uiState: StateFlow<HomeV2UiState> = _uiState.asStateFlow()
    
    init {
        // Initialize with foundation state
        _uiState.value = HomeV2UiState(isInitialized = true)
    }
    
    // Ready for future service integration
    // fun loadFeaturedContent() = viewModelScope.launch { ... }
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
                createUiStateSection(uiState),
                createArchitectureSection(),
                createDevelopmentStatusSection()
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

### Content Features
1. **Featured Shows**: Popular Archive.org concerts
2. **Recent Additions**: Latest concerts added
3. **Quick Actions**: Search, Library, Recent plays
4. **User Recommendations**: Personalized discovery

### Service Integration
1. **HomeV2Service**: Content loading and caching
2. **BrowseV2Service**: Search and discovery integration
3. **LibraryV2Service**: Recent library content
4. **MediaV2Service**: Recently played shows

### UI Enhancements
1. **Dynamic Content**: Real-time Archive.org integration
2. **Personalization**: User-specific recommendations
3. **Quick Actions**: Direct navigation shortcuts
4. **Visual Polish**: Enhanced Material3 animations

---

**Implementation Status**: Foundation Complete ✅  
**Service Integration**: Ready for Future Development  
**Debug Support**: Fully Functional  
**Production Safety**: Feature Flag Protected

HomeV2 implementation demonstrates the maturity and reliability of the V2 architecture pattern, providing a solid foundation for future home screen development.