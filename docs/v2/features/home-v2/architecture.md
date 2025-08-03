# HomeV2 Architecture

## Overview

HomeV2 implements the fourth successful V2 architecture pattern in the Dead Archive app, following the proven methodologies established by LibraryV2, PlayerV2, and SearchV2. The architecture emphasizes foundation establishment, service readiness, and clean separation of concerns.

## V2 Architecture Principles

### 1. UI-First Development
**Philosophy**: Build complete UI before service integration

```kotlin
// UI-driven architecture discovery
@Composable
fun HomeV2Screen(
    // Navigation callbacks discovered through UI needs
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    initialEra: String? = null,
    
    // Service injection ready for future integration
    viewModel: HomeV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
)
```

**Benefits**:
- Complete UI functionality without waiting for backend services
- Service requirements discovered organically through UI development
- Risk reduction through working system at every stage

### 2. Service Composition Readiness
**Pattern**: Architecture prepared for service integration without over-engineering

```kotlin
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    // Ready for service injection when features are implemented
    // private val homeV2Service: HomeV2Service,
    // private val downloadV2Service: DownloadV2Service,
    // private val libraryV2Service: LibraryV2Service
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeV2UiState())
    val uiState: StateFlow<HomeV2UiState> = _uiState.asStateFlow()
    
    // Service integration points ready for implementation
    // fun loadFeaturedContent() = viewModelScope.launch { ... }
    // fun loadRecentLibraryItems() = viewModelScope.launch { ... }
}
```

### 3. Feature Flag Safety
**Implementation**: Production-safe deployment with instant rollback

```kotlin
// Settings-controlled deployment
data class AppSettings(
    val useHomeV2: Boolean = false,
    // ... other V2 feature flags
)

// Navigation routing with feature flag
composable("home") {
    if (settings.useHomeV2) {
        HomeV2Screen(...)  // V2 implementation
    } else {
        HomeScreen(...)    // V1 fallback
    }
}
```

**Impact**: Zero-risk deployment with user-level control

## Component Architecture

### 1. Screen-Level Architecture
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeV2Screen(
    // Navigation layer
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    
    // State management layer
    viewModel: HomeV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // State collection
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Debug infrastructure
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        HomeV2DebugDataFactory.createDebugData(uiState, initialEra)
    } else null
    
    // Material3 scaffold structure
    Scaffold(
        topBar = { HomeV2TopBar() }
    ) { paddingValues ->
        // Content layer
        Box(modifier = Modifier.padding(paddingValues)) {
            HomeV2Content()
            
            // Debug layer
            if (settings.showDebugInfo && debugData != null) {
                DebugActivator(...)
            }
        }
    }
    
    // Debug bottom sheet
    debugData?.let { data ->
        DebugBottomSheet(...)
    }
}
```

### 2. Content Component Architecture
```kotlin
// Main content structure
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    item { HomeV2WelcomeCard() }      // Introduction and V2 explanation
    item { HomeV2DevelopmentCard() }  // Implementation status
    item { HomeV2FoundationCard() }   // Architecture highlights
}

// Individual card components
@Composable
private fun HomeV2WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome content with proper theming
        }
    }
}
```

## State Management Architecture

### 1. HomeV2UiState Design
```kotlin
data class HomeV2UiState(
    // Core state
    val isLoading: Boolean = false,
    val isInitialized: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    
    // Content state (ready for service integration)
    val welcomeText: String = "Welcome to Dead Archive V2",
    val featuredShows: List<Show> = emptyList(),
    val quickActions: List<String> = emptyList()
)
```

**Design Principles**:
- **Immutable State**: Data class with immutable properties
- **Single Source**: All UI state in one location
- **Service Ready**: Properties prepared for future content integration
- **Error Handling**: Built-in error state management

### 2. StateFlow Implementation
```kotlin
@HiltViewModel
class HomeV2ViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeV2UiState())
    val uiState: StateFlow<HomeV2UiState> = _uiState.asStateFlow()
    
    init {
        // Initialize with foundation complete state
        _uiState.value = HomeV2UiState(isInitialized = true)
    }
    
    // Ready for service integration methods
    private fun updateState(update: HomeV2UiState.() -> HomeV2UiState) {
        _uiState.value = _uiState.value.update()
    }
}
```

## Debug Architecture

### 1. Debug Integration Pattern
Following established V2 debug architecture:

```kotlin
// Debug data factory pattern
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

### 2. Debug Bottom Sheet Integration
```kotlin
// Consistent with other V2 screens
debugData?.let { data ->
    DebugBottomSheet(
        debugData = data,
        isVisible = showDebugPanel,
        onDismiss = { showDebugPanel = false }
    )
}
```

**Debug Categories**:
- **General Information**: Screen context and parameters
- **UI State**: Current component state and properties
- **V2 Architecture**: Development patterns and approach
- **Development Status**: Implementation progress and next steps

## Navigation Architecture

### 1. Routing Implementation
```kotlin
// MainAppScreen.kt - Main navigation integration
NavHost(
    navController = navController,
    startDestination = "home"
) {
    composable("home") {
        if (settings.useHomeV2) {
            // V2 implementation with proper callbacks
            com.deadarchive.feature.browse.HomeV2Screen(
                onNavigateToPlayer = { recordingId -> 
                    navController.navigate("playlist/$recordingId")
                },
                onNavigateToShow = { show ->
                    show.bestRecording?.let { recording ->
                        navController.navigate(
                            "playlist/${recording.identifier}?showId=${show.showId}"
                        )
                    }
                }
            )
        } else {
            // V1 fallback
            HomeScreen(...)
        }
    }
}
```

### 2. Navigation Callbacks
```kotlin
// Navigation interface design
onNavigateToPlayer: (String) -> Unit     // Direct player navigation
onNavigateToShow: (Show) -> Unit         // Show-based navigation with context
```

**Design Benefits**:
- **Type Safety**: Strong typing for navigation parameters
- **Context Preservation**: Show context maintained through navigation
- **Flexible Routing**: Multiple navigation paths supported

## Service Architecture Readiness

### 1. Future Service Integration
```kotlin
// Service interface design (ready for implementation)
interface HomeV2Service {
    // Content loading
    suspend fun loadFeaturedShows(): Result<List<Show>>
    suspend fun loadRecentAdditions(): Result<List<Show>>
    suspend fun loadQuickActions(): Result<List<String>>
    
    // Dynamic content
    fun getWelcomeMessage(): Flow<String>
    fun getFeaturedContent(): Flow<List<Show>>
    
    // User-specific content
    suspend fun loadRecommendations(userId: String): Result<List<Show>>
    suspend fun loadRecentActivity(): Result<List<String>>
}
```

### 2. V2 Service Ecosystem Integration
Ready for integration with existing V2 services:

```kotlin
// Service composition pattern
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    private val homeV2Service: HomeV2Service,           // Content loading
    private val downloadV2Service: DownloadV2Service,   // Download status
    private val libraryV2Service: LibraryV2Service,     // Library integration
    private val searchV2Service: SearchV2Service        // Search integration
) : ViewModel() {
    
    // Combined service orchestration
    val featuredContent = combine(
        homeV2Service.getFeaturedContent(),
        downloadV2Service.downloadStates,
        libraryV2Service.libraryShowIds
    ) { featured, downloads, library ->
        // Enrich featured content with download and library status
        featured.map { show ->
            HomeV2Show(
                show = show,
                downloadStatus = downloads[show.showId],
                isInLibrary = show.showId in library
            )
        }
    }
}
```

## Material3 Design Architecture

### 1. Theming Integration
```kotlin
// Color scheme integration
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )
) {
    Text(
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.headlineSmall
    )
}
```

### 2. Typography Hierarchy
```kotlin
// Complete Material3 typography usage
Text(
    style = MaterialTheme.typography.headlineSmall,    // Card titles
    fontWeight = FontWeight.Bold
)

Text(
    style = MaterialTheme.typography.bodyLarge,        // Card content
)

Text(
    style = MaterialTheme.typography.bodyMedium,       // Status items
    color = MaterialTheme.colorScheme.primary
)
```

### 3. Spacing and Layout
```kotlin
// Consistent spacing system
LazyColumn(
    contentPadding = PaddingValues(16.dp),           // Outer padding
    verticalArrangement = Arrangement.spacedBy(24.dp) // Inter-card spacing
)

Column(
    modifier = Modifier.padding(24.dp),              // Card internal padding
    verticalArrangement = Arrangement.spacedBy(16.dp) // Internal spacing
)
```

## Performance Architecture

### 1. Efficient Rendering
```kotlin
// LazyColumn for efficient scrolling
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    // Efficient content composition
) {
    item { /* Card content loaded on demand */ }
}

// State optimization
val uiState by viewModel.uiState.collectAsState()  // Single state source
```

### 2. Memory Management
```kotlin
// Proper Compose lifecycle
@Composable
fun HomeV2Screen() {
    // Automatic memory cleanup on composition disposal
    val debugData = remember(settings.showDebugInfo) {
        if (settings.showDebugInfo) {
            HomeV2DebugDataFactory.createDebugData(uiState, initialEra)
        } else null
    }
}
```

## Error Handling Architecture

### 1. State-Based Error Management
```kotlin
data class HomeV2UiState(
    val hasError: Boolean = false,
    val errorMessage: String? = null
) {
    val isErrorState: Boolean get() = hasError && errorMessage != null
}

// UI error display
if (uiState.isErrorState) {
    HomeV2ErrorCard(
        message = uiState.errorMessage!!,
        onRetry = { viewModel.retry() }
    )
}
```

### 2. Service Error Handling (Ready)
```kotlin
// Future service error handling
private fun handleServiceError(error: Throwable) {
    _uiState.value = _uiState.value.copy(
        hasError = true,
        errorMessage = when (error) {
            is NetworkException -> "Network connection error"
            is ServiceException -> "Service temporarily unavailable"
            else -> "An unexpected error occurred"
        }
    )
}
```

## Testing Architecture

### 1. Component Testing Strategy
```kotlin
// Isolated component testing
@Test
fun homeV2WelcomeCard_displaysCorrectContent() {
    composeTestRule.setContent {
        HomeV2WelcomeCard()
    }
    
    composeTestRule
        .onNodeWithText("Welcome to HomeV2 🚀")
        .assertIsDisplayed()
}
```

### 2. State Testing Strategy
```kotlin
// ViewModel state testing
@Test
fun homeV2ViewModel_initialState_isCorrect() {
    val viewModel = HomeV2ViewModel()
    
    val state = viewModel.uiState.value
    assertTrue(state.isInitialized)
    assertFalse(state.hasError)
    assertEquals("Welcome to Dead Archive V2", state.welcomeText)
}
```

## Deployment Architecture

### 1. Feature Flag Strategy
```kotlin
// Safe deployment pattern
@Preview
@Composable
fun HomeV2ScreenPreview() {
    // Preview with feature flag enabled
    DeadArchiveTheme {
        HomeV2Screen(
            onNavigateToPlayer = {},
            onNavigateToShow = {}
        )
    }
}
```

### 2. Rollback Strategy
- **Instant Rollback**: Toggle feature flag in Settings
- **No Data Migration**: V2 doesn't modify existing data
- **Safe Fallback**: V1 HomeScreen remains unchanged
- **User Control**: Per-user feature flag control

---

**Architecture Status**: Foundation Complete ✅  
**Service Integration**: Ready for Future Development  
**Testing Strategy**: Component and State Testing Ready  
**Deployment Safety**: Feature Flag Protected

HomeV2 architecture demonstrates the maturity of the V2 pattern, providing a clean foundation for future home screen development while maintaining production safety and development velocity.