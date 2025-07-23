# Debug Panel System

## Overview

The Dead Archive app includes a comprehensive, reusable debug panel system that provides consistent debugging capabilities across all screens. This system is designed to help developers diagnose issues, monitor app state, and share debug information without disrupting the user experience.

## Architecture

### Core Components

The debug panel system consists of several reusable components located in the `core:design` module:

#### 1. DebugBottomSheet
**Location**: `core/design/src/main/java/com/deadarchive/core/design/component/DebugBottomSheet.kt`

A reusable sliding bottom sheet component that provides:
- Consistent Material 3 themed appearance
- Slide-up animation from bottom of screen
- Built-in copy functionality for all debug data
- Settings integration (only shows when `showDebugInfo = true`)
- Dismissible via tap-outside or drag-down gestures

#### 2. DebugData Models
**Location**: `core/design/src/main/java/com/deadarchive/core/design/component/DebugData.kt`

Structured data models for organizing debug information:

```kotlin
data class DebugSection(
    val title: String,
    val items: List<DebugItem>
)

sealed class DebugItem {
    data class KeyValue(val key: String, val value: String) : DebugItem()
    data class Multiline(val key: String, val value: String) : DebugItem()
    data class Error(val message: String, val stackTrace: String?) : DebugItem()
    data class Timestamp(val label: String, val time: Long) : DebugItem()
    data class JsonData(val key: String, val json: String) : DebugItem()
}
```

#### 3. Debug Activation Component
**Location**: `core/design/src/main/java/com/deadarchive/core/design/component/DebugActivator.kt`

A floating debug button (🐛) that appears in the bottom-right corner of screens when debug mode is enabled. This component provides consistent activation across all screens.

## Usage Guide

### Adding Debug Panel to a Screen

1. **Create Screen-Specific Debug Data Collector**

Create a debug data collector for your screen (e.g., `PlaylistDebugData.kt`):

```kotlin
@Composable
fun collectPlaylistDebugData(
    viewModel: PlayerViewModel,
    recordingId: String?,
    showId: String?
): DebugData {
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()
    
    return DebugData(
        screenName = "PlaylistScreen",
        sections = listOf(
            DebugSection(
                title = "Request Parameters",
                items = listOf(
                    DebugItem.KeyValue("recordingId", recordingId ?: "null"),
                    DebugItem.KeyValue("showId", showId ?: "null"),
                    DebugItemFactory.createTimestamp("Last Request")
                )
            ),
            DebugSection(
                title = "Loading State",
                items = listOf(
                    DebugItem.BooleanValue("isLoading", uiState.isLoading),
                    DebugItem.KeyValue("currentRecording", currentRecording?.identifier ?: "null"),
                    uiState.error?.let { DebugItemFactory.createErrorItem(it) } 
                        ?: DebugItem.KeyValue("Last Error", "None")
                )
            )
            // Add more sections as needed
        )
    )
}
```

2. **Integrate with Screen**

Add the debug panel to your screen Composable:

```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()
    val debugData = if (settings.showDebugInfo) {
        collectMyScreenDebugData(viewModel, /* other params */)
    } else {
        null
    }
    
    var showDebugPanel by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Your main screen content
        MyScreenContent()
        
        // Debug activation button
        if (settings.showDebugInfo && debugData != null) {
            DebugActivator(
                isVisible = true,
                onClick = { showDebugPanel = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
    
    // Debug Bottom Sheet
    if (settings.showDebugInfo && debugData != null) {
        DebugBottomSheet(
            debugData = debugData,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}
```

### Debug Data Collection Patterns

#### Real-time State Monitoring
```kotlin
// Collect StateFlow values
val currentState by viewModel.someState.collectAsState()

DebugItem.KeyValue("currentState", currentState.toString())
```

#### Service Call Logging
```kotlin
// Track service method calls with timestamps
data class ServiceCall(
    val method: String,
    val params: String,
    val result: String?,
    val error: String?,
    val timestamp: Long
)

// In your debug collector
DebugItem.Multiline(
    "Recent Service Calls", 
    recentCalls.joinToString("\n") { 
        "${it.method}(${it.params}) -> ${it.result ?: it.error} [${it.timestamp}]" 
    }
)
```

#### Error Tracking
```kotlin
// Capture and display errors with stack traces
DebugItem.Error(
    message = exception.message ?: "Unknown error",
    stackTrace = exception.stackTraceToString()
)
```

#### JSON/Object Data
```kotlin
// Display complex objects as formatted JSON
DebugItem.JsonData(
    "Recording Object",
    Gson().toJson(recording)
)
```

## Features

### Copy Functionality
- **Copy All**: Copies all debug data as formatted text with timestamps
- **Copy Section**: Copies individual debug sections
- **Formatted Output**: Includes section headers, timestamps, and proper formatting
- **Logcat Integration**: Automatically outputs to logcat with searchable tags when copying

### Logcat Integration

All copy actions automatically log the same data to logcat for easy access via `adb logcat`. Use these commands to view debug output:

```bash
# View all debug panel output
adb logcat -s DEAD_DEBUG_PANEL

# Filter by specific screen
adb logcat -s DEAD_DEBUG_PANEL | grep "PlaylistScreen"

# View in real-time as you copy debug data
adb logcat -s DEAD_DEBUG_PANEL -v time
```

The logcat output includes:
- Searchable headers with screen name and action type
- Full debug data with consistent formatting
- Timestamps for correlation with app behavior
- Visual separators (======) for easy parsing

### Visual Design
- **Material 3 Integration**: Uses app theme colors and typography
- **Debug Accent**: Red accent colors (#FF5722) to distinguish debug UI
- **Monospace Data**: Code/data values use monospace fonts for readability
- **Expandable Sections**: Collapsible sections to manage large amounts of data

### Settings Integration
- **showDebugInfo Setting**: Controls visibility of all debug functionality
- **No Production Impact**: Debug panels never appear for end users
- **Development Only**: Enabled through developer settings

## Best Practices

### Data Organization
1. **Group Related Data**: Use sections to organize related debug information
2. **Descriptive Labels**: Use clear, descriptive keys for debug items
3. **Include Timestamps**: Add timestamps for time-sensitive data
4. **Limit Data Volume**: Don't overwhelm with too much information

### Performance Considerations
1. **Conditional Collection**: Only collect debug data when `showDebugInfo = true`
2. **Efficient Updates**: Use `collectAsState()` for reactive updates
3. **Memory Management**: Clear old debug data periodically

### Error Handling
1. **Graceful Degradation**: Debug panels should never crash the app
2. **Exception Wrapping**: Wrap debug data collection in try-catch blocks
3. **Clear Error Messages**: Provide actionable error information

## Example Implementations

### Current Implementations
- **PlaylistScreen**: Monitors recording loading, service calls, and state changes
- *(More screens will be added as the system is expanded)*

### Common Debug Scenarios
- **Network Requests**: API call parameters, responses, and errors
- **Database Operations**: Query parameters, results, and performance
- **State Management**: ViewModel state changes and transitions
- **Navigation**: Route parameters and navigation events
- **Media Playback**: Player state, queue management, and audio stream info

## Extension Guide

### Adding New Debug Item Types
1. Add new sealed class variant to `DebugItem`
2. Update `DebugBottomSheet` to handle new type
3. Create helper functions for common patterns

### Screen-Specific Extensions
1. Create debug data collector following established patterns
2. Document specific debug data available for the screen
3. Add to this documentation with usage examples

## Maintenance

### Keeping This Document Updated
- Add new debug item types as they're created
- Document new screen implementations
- Update best practices based on usage experience
- Include screenshots of debug panels in action

### Version History
- **v1.0**: Core debug panel system with UI components implemented
  - DebugData models and DebugBottomSheet component working
  - Debug activation and status indicator components created
  - Copy-to-clipboard functionality with emoji icons
  - Mock data integration started for PlaylistScreen
- *(Add versions as system evolves)*

---

**Last Updated**: 2025-01-23  
**Next Review**: When adding debug panels to new screens