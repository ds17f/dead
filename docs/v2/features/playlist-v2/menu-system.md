# PlaylistV2 Menu System

This document provides detailed documentation for the PlaylistV2 menu system, including the triple dot menu, share functionality, and choose recording feature.

## Overview

The PlaylistV2 menu system provides users with access to sharing and recording selection functionality through a clean, modal-based interface that maintains V1 design excellence while following V2 architecture patterns.

## Menu System Architecture

### Component Hierarchy

```
PlaylistV2ActionRow
├── Triple Dot Menu Button (IconButton)
│
PlaylistV2Screen
├── PlaylistV2MenuSheet (Modal)
│   ├── Share Option (ShareV2MenuRow)
│   └── Choose Recording Option (Row)
│
└── PlaylistV2RecordingSelectionSheet (Modal)
    ├── Current Recording (PlaylistV2RecordingOptionCard)
    ├── Alternative Recordings (List)
    │   └── PlaylistV2RecordingOptionCard (for each alternative)
    ├── Set as Default Button (conditional)
    └── Reset to Recommended Button (conditional)
```

## Triple Dot Menu

### Menu Button Implementation

The menu is triggered from the PlaylistV2ActionRow:

```kotlin
// Triple dot menu button
IconButton(
    onClick = { onShowMenu() },
    modifier = Modifier.size(40.dp)
) {
    Icon(
        painter = IconResources.Content.MoreVertical(),
        contentDescription = "More options",
        modifier = Modifier.size(24.dp)
    )
}
```

**Design Details**:
- **Size**: 40dp clickable area with 24dp icon
- **Icon**: `ic_more_vert` through IconResources system
- **Position**: Right side of action row, before main play button
- **Accessibility**: Proper content description for screen readers

### PlaylistV2MenuSheet Component

**Location**: `feature/playlist/src/main/java/com/deadarchive/feature/playlist/components/PlaylistV2MenuSheet.kt`

Modal bottom sheet that appears when triple dot button is tapped.

#### Component Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistV2MenuSheet(
    showDate: String?,
    venue: String?,
    location: String?,
    onShareClick: () -> Unit,
    onChooseRecordingClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Menu Options

**1. Share Option**
```kotlin
ShareV2MenuRow(
    onClick = {
        // Share functionality (currently stubbed)
        onShareClick()
        onDismiss()
    }
)
```

**2. Choose Recording Option**  
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { 
            onChooseRecordingClick()
            onDismiss()
        }
        .padding(vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        painter = IconResources.Content.LibraryMusic(),
        contentDescription = "Choose Recording",
        modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
        text = "Choose Recording",
        style = MaterialTheme.typography.bodyLarge
    )
}
```

#### Visual Design

- **Layout**: Vertical list of options with proper spacing
- **Icons**: 24dp icons with 16dp spacing to text
- **Typography**: Material3 bodyLarge for option text
- **Padding**: 16dp vertical padding for touch targets
- **Dismissal**: Auto-dismiss on option selection

### Menu State Management

#### State in PlaylistV2UiState

```kotlin
data class PlaylistV2UiState(
    // ... other state
    val showMenu: Boolean = false
)
```

#### ViewModel Methods

```kotlin
// Show menu modal
fun showMenu() {
    _uiState.value = _uiState.value.copy(showMenu = true)
}

// Hide menu modal
fun hideMenu() {
    _uiState.value = _uiState.value.copy(showMenu = false)
}

// Handle choose recording selection
fun chooseRecording() {
    hideMenu()
    showRecordingSelection()
}
```

#### Screen Integration

```kotlin
// In PlaylistV2Screen
if (uiState.showMenu) {
    uiState.showData?.let { showData ->
        PlaylistV2MenuSheet(
            showDate = showData.displayDate,
            venue = showData.venue,
            location = showData.location,
            onShareClick = { 
                // Share will be implemented in future iteration
            },
            onChooseRecordingClick = viewModel::chooseRecording,
            onDismiss = viewModel::hideMenu
        )
    }
}
```

## Share Functionality

### ShareV2Service Architecture

**Location**: `core/common/src/main/java/com/deadarchive/core/common/service/ShareV2Service.kt`

Complete V2 sharing service that maintains V1 functionality:

```kotlin
@Singleton
class ShareV2Service @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun shareShow(show: Show, recording: Recording) {
        val message = buildShowShareMessage(show, recording)
        val shareIntent = createShareIntent("Check out this Grateful Dead show!", message)
        context.startActivity(shareIntent)
    }
    
    fun shareTrack(show: Show, recording: Recording, track: Track, currentPosition: Long?) {
        val message = buildTrackShareMessage(show, recording, track, currentPosition)
        val shareIntent = createShareIntent("Check out this Grateful Dead track!", message)
        context.startActivity(shareIntent)
    }
}
```

#### Share Message Format

**Show Sharing Example**:
```
🎵 Grateful Dead - May 8, 1977

📍 Barton Hall
🌎 Cornell University, Ithaca, NY

🎧 Source: Miller SBD
📼 Taper: Betty Boards
⭐ Rating: 4.8/5.0

🔗 Listen on Archive.org:
https://archive.org/details/gd1977-05-08...
```

**Track Sharing Example**:
```
🎵 Scarlet Begonias > Fire on the Mountain
🎭 Grateful Dead - May 8, 1977

📍 Barton Hall
🌎 Cornell University, Ithaca, NY

🔢 Track 5-6
⏱️ Duration: 23:47
▶️ Starting at: 2:34

🎧 Source: Miller SBD
⭐ Rating: 4.8/5.0

🔗 Listen on Archive.org:
https://archive.org/details/gd1977-05-08...#track5
```

### ShareV2Component System

**Location**: `core/design/src/main/java/com/deadarchive/core/design/component/ShareV2Component.kt`

Flexible sharing UI component that adapts to different contexts:

#### Component Variants

**1. Full Component**
```kotlin
ShareV2Component(
    onClick = { /* share action */ },
    showIcon = true,        // Show share icon
    showText = true,        // Show "Share" text
    iconSize = 24.dp,       // Icon size
    spacing = 16.dp,        // Icon-text spacing
    text = "Share"          // Custom text
)
```

**2. Icon-Only Variant**
```kotlin
ShareV2Icon(
    onClick = { /* share action */ },
    iconSize = 24.dp
)
```

**3. Menu Row Variant**
```kotlin
ShareV2MenuRow(
    onClick = { /* share action */ }
)
```

#### Usage Contexts

- **Action Bars**: Use `ShareV2Icon()` for compact toolbar sharing
- **Menu Systems**: Use `ShareV2MenuRow()` for consistent menu appearance
- **Custom Layouts**: Use `ShareV2Component()` with custom parameters

### Current Share Integration Status

#### ✅ Completed Components
- **ShareV2Service**: Fully functional sharing service
- **ShareV2Component**: Flexible UI components with multiple variants
- **Message Formatting**: Rich, emoji-enhanced share messages
- **Intent Handling**: Proper Android sharing intent creation

#### 🚧 Integration Limitations
- **Domain Model Dependency**: ShareV2Service requires V1 Show/Recording models
- **PlaylistV2 Integration**: Currently stubbed pending V2 domain model availability
- **Modal Connection**: Share button triggers callback but doesn't execute sharing

#### 📋 Future Integration Plan
When V2 domain models become available:
1. Create conversion methods: `PlaylistShowViewModel.toShow()` and `PlaylistShowViewModel.toRecording()`
2. Update PlaylistV2MenuSheet to call ShareV2Service directly
3. Remove share functionality stub from menu handling
4. Enable full sharing functionality

## Choose Recording Feature

### Recording Selection System

The choose recording feature provides users with a comprehensive interface to browse and select alternative recordings for the current show.

#### Feature Overview

- **Current Recording Display**: Shows currently playing recording with "Currently Playing" badge
- **Alternative Recordings**: Lists all available recordings with ratings and metadata
- **Recommendation System**: Highlights algorithm-recommended recordings
- **User Preferences**: Allows setting recordings as default for the show
- **Visual Selection**: Clear selection states with colors and icons

### PlaylistV2RecordingSelectionSheet

**Location**: `feature/playlist/src/main/java/com/deadarchive/feature/playlist/components/PlaylistV2RecordingSelectionSheet.kt`

Main modal component for recording selection following V1 design patterns.

#### Component Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistV2RecordingSelectionSheet(
    state: RecordingSelectionV2State,
    onRecordingSelected: (String) -> Unit,
    onSetAsDefault: (String) -> Unit,
    onResetToRecommended: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Modal Layout Structure

```
┌─────────────────────────────────────────┐
│ Choose Recording                        │
│ May 8, 1977 - Barton Hall              │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Miller SBD        ⭐⭐⭐⭐⭐ (4.8)  ✓ │ │ ← Current
│ │ Currently Playing                   │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Bershaw AUD      ⭐⭐⭐⭐☆ (4.2)    │ │ ← Recommended
│ │ Recommended                         │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Seamons MTX      ⭐⭐⭐⭐⭐ (4.6)    │ │ ← Alternative
│ │ High Rating (4.6)                   │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ [Set as Default Recording]              │
│ [Reset to Recommended]                  │
└─────────────────────────────────────────┘
```

#### Modal Features

**Header Section**:
- "Choose Recording" title with Material3 headlineSmall typography
- Show context (date and venue) with proper text overflow handling

**Content Section**:
- Current recording displayed first (if available)
- Alternative recordings in scrollable LazyColumn
- Loading state with centered CircularProgressIndicator  
- Error state with error message display

**Action Buttons**:
- "Set as Default Recording" (appears when different recording selected)
- "Reset to Recommended" (appears when recommendation exists and differs from current)
- Both buttons use proper Material3 styling and iconography

### PlaylistV2RecordingOptionCard

**Location**: `feature/playlist/src/main/java/com/deadarchive/feature/playlist/components/PlaylistV2RecordingOptionCard.kt`

Individual recording option display with selection state and visual feedback.

#### Component Structure

```kotlin
@Composable
fun PlaylistV2RecordingOptionCard(
    recordingOption: RecordingOptionV2ViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Visual States

**1. Selected State**:
- **Container**: `MaterialTheme.colorScheme.primaryContainer`
- **Border**: 2dp `MaterialTheme.colorScheme.primary` border
- **Icon**: Check mark icon (24dp) on right side
- **Text Color**: `MaterialTheme.colorScheme.onPrimaryContainer` for match reason

**2. Recommended State**:
- **Container**: `MaterialTheme.colorScheme.tertiaryContainer`  
- **Text Color**: `MaterialTheme.colorScheme.onTertiaryContainer` for match reason

**3. Default State**:
- **Container**: `MaterialTheme.colorScheme.surface`
- **Text Color**: `MaterialTheme.colorScheme.primary` for match reason

#### Card Content Layout

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Column(modifier = Modifier.weight(1f)) {
        // Source and rating row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = source, fontWeight = FontWeight.Medium)
            CompactStarRating(rating = rating, starSize = 12.dp)
        }
        
        // Recording title/quality
        Text(text = title, color = onSurfaceVariant)
        
        // Match reason
        Text(text = matchReason, color = contextualColor, fontWeight = FontWeight.Medium)
    }
    
    if (isSelected) {
        Icon(Icons.Default.Check, tint = primary)
    }
}
```

### Recording Selection Data Models

#### RecordingOptionV2ViewModel

UI representation of a recording option without V1 domain dependencies:

```kotlin
data class RecordingOptionV2ViewModel(
    val identifier: String,        // "gd1977-05-08.sbd.miller.97065.flac16"
    val source: String,            // "Miller SBD"
    val title: String,             // "16-bit FLAC • Soundboard • Excellent quality"
    val rating: Float?,            // 4.8f
    val reviewCount: Int?,         // 184
    val isSelected: Boolean,       // true/false
    val isRecommended: Boolean,    // true/false for recommendation highlighting
    val matchReason: String?       // "Currently Playing", "Recommended", "High Rating"
)
```

#### RecordingSelectionV2State

Complete UI state for the recording selection modal:

```kotlin
data class RecordingSelectionV2State(
    val isVisible: Boolean = false,                    // Modal visibility
    val showTitle: String = "",                        // "May 8, 1977"
    val currentRecording: RecordingOptionV2ViewModel?, // Currently playing recording
    val alternativeRecordings: List<RecordingOptionV2ViewModel> = emptyList(),
    val hasRecommended: Boolean = false,               // Controls action button display
    val isLoading: Boolean = false,                    // Loading state
    val errorMessage: String? = null                   // Error message display
)
```

#### RecordingOptionsV2Result

Service result containing all recording options:

```kotlin
data class RecordingOptionsV2Result(
    val currentRecording: RecordingOptionV2ViewModel?,
    val alternativeRecordings: List<RecordingOptionV2ViewModel>,
    val hasRecommended: Boolean
)
```

### Recording Selection Flow

#### User Interaction Flow

1. **Access**: User taps triple dot menu → "Choose Recording"
2. **Modal Display**: `PlaylistV2MenuSheet` dismisses, `PlaylistV2RecordingSelectionSheet` appears
3. **Loading**: Modal shows loading spinner while fetching recording options (600ms delay)
4. **Content Display**: 
   - Current recording shown first with "Currently Playing" badge
   - Alternative recordings listed with ratings and match reasons
   - Proper sorting (recommended first, then by rating)
5. **Selection**: User taps any recording to select it
6. **Visual Feedback**: Immediate selection state update with colors and check icon
7. **Action Options**:
   - "Set as Default Recording" appears if different recording selected
   - "Reset to Recommended" appears if recommended recording exists
8. **Preference Handling**: User can save preferences or reset to recommendations

#### ViewModel State Management

```kotlin
// 1. User selects "Choose Recording" from menu
fun chooseRecording() {
    hideMenu()
    showRecordingSelection()
}

// 2. Load recording options with loading state
fun showRecordingSelection() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            recordingSelection = _uiState.value.recordingSelection.copy(
                isVisible = true,
                isLoading = true,
                errorMessage = null
            )
        )
        
        val recordingOptions = playlistV2Service.getRecordingOptions()
        
        _uiState.value = _uiState.value.copy(
            recordingSelection = _uiState.value.recordingSelection.copy(
                currentRecording = recordingOptions.currentRecording,
                alternativeRecordings = recordingOptions.alternativeRecordings,
                hasRecommended = recordingOptions.hasRecommended,
                isLoading = false
            )
        )
    }
}

// 3. Handle recording selection
fun selectRecording(recordingId: String) {
    viewModelScope.launch {
        playlistV2Service.selectRecording(recordingId)
        
        // Update UI selection state immediately
        val currentSelection = _uiState.value.recordingSelection
        val updatedAlternatives = currentSelection.alternativeRecordings.map { option ->
            option.copy(isSelected = option.identifier == recordingId)
        }
        
        _uiState.value = _uiState.value.copy(
            recordingSelection = currentSelection.copy(
                alternativeRecordings = updatedAlternatives
            )
        )
    }
}
```

### Service Integration

#### PlaylistV2Service Methods

```kotlin
interface PlaylistV2Service {
    // Get recording options for current show
    suspend fun getRecordingOptions(): RecordingOptionsV2Result
    
    // Select different recording
    suspend fun selectRecording(recordingId: String)
    
    // Set recording as user's default for this show
    suspend fun setRecordingAsDefault(recordingId: String)
    
    // Reset to algorithm-recommended recording
    suspend fun resetToRecommended()
}
```

#### Mock Data Implementation

The stub service provides realistic Cornell '77 recording data:

```kotlin
override suspend fun getRecordingOptions(): RecordingOptionsV2Result {
    delay(600) // Simulate realistic loading time
    
    val currentRecording = RecordingOptionV2ViewModel(
        identifier = "gd1977-05-08.sbd.miller.97065.flac16",
        source = "Miller SBD",
        title = "16-bit FLAC • Soundboard • Excellent quality",
        rating = 4.8f,
        reviewCount = 184,
        isSelected = true,
        isRecommended = false,
        matchReason = "Currently Playing"
    )
    
    val alternatives = listOf(
        RecordingOptionV2ViewModel(
            identifier = "gd1977-05-08.aud.bershaw.97066.flac16",
            source = "Bershaw AUD",
            title = "16-bit FLAC • Audience • Very good quality",
            rating = 4.2f,
            reviewCount = 67,
            isSelected = false,
            isRecommended = true,
            matchReason = "Recommended"
        ),
        // ... more realistic alternatives
    )
    
    return RecordingOptionsV2Result(
        currentRecording = currentRecording,
        alternativeRecordings = alternatives,
        hasRecommended = true
    )
}
```

## User Experience Design

### Accessibility Features

- **Screen Reader Support**: Proper contentDescription for all interactive elements
- **Touch Targets**: Minimum 40dp touch areas for all buttons and cards
- **Visual Hierarchy**: Clear information hierarchy with proper typography scaling
- **Color Accessibility**: High contrast colors meeting WCAG guidelines
- **Loading States**: Clear loading indicators with proper announcements

### Interaction Patterns

- **Predictable Navigation**: Consistent back/dismiss button behavior
- **Visual Feedback**: Immediate selection state changes
- **Error Handling**: Graceful error display with retry options
- **Loading Management**: Appropriate loading states during data fetching
- **Modal Management**: Proper focus management and dismissal handling

### Performance Considerations

- **Lazy Loading**: Recording options loaded only when modal opened
- **State Efficiency**: Minimal UI updates through focused state management
- **Memory Management**: Proper component cleanup and modal dismissal
- **Animation Performance**: Smooth modal transitions without jank

## Testing Strategy

### Component Testing

```kotlin
@Test
fun testPlaylistV2MenuSheet_shareClick() {
    var shareClicked = false
    
    composeTestRule.setContent {
        PlaylistV2MenuSheet(
            showDate = "May 8, 1977",
            venue = "Barton Hall",
            location = "Cornell University",
            onShareClick = { shareClicked = true },
            onChooseRecordingClick = { },
            onDismiss = { }
        )
    }
    
    composeTestRule.onNodeWithText("Share").performClick()
    assertTrue(shareClicked)
}
```

### Integration Testing

```kotlin
@Test
fun testRecordingSelection_fullFlow() = runTest {
    // 1. Open menu
    viewModel.showMenu()
    assertTrue(viewModel.uiState.value.showMenu)
    
    // 2. Choose recording
    viewModel.chooseRecording()
    assertFalse(viewModel.uiState.value.showMenu)
    
    // 3. Wait for recording options to load
    advanceUntilIdle()
    
    // 4. Verify recording options displayed
    val recordingState = viewModel.uiState.value.recordingSelection
    assertTrue(recordingState.isVisible)
    assertFalse(recordingState.isLoading)
    assertEquals(4, recordingState.alternativeRecordings.size)
    assertTrue(recordingState.hasRecommended)
}
```

### User Flow Testing

Complete user journey testing from menu access through recording selection and preference setting:

1. **Menu Access**: Triple dot button opens menu modal
2. **Option Selection**: "Choose Recording" opens recording selection modal
3. **Recording Display**: Current and alternative recordings properly displayed
4. **Selection Interaction**: Recording selection updates visual state
5. **Preference Management**: Set as default and reset to recommended functionality
6. **Modal Dismissal**: Proper cleanup and state reset on modal dismissal

## Future Enhancements

### Planned Improvements

#### Real Data Integration
When V2 domain models become available:
- Replace mock data with real Archive.org recordings
- Implement actual recording switching with media player integration
- Connect preference saving to user database
- Enable full sharing functionality through ShareV2Service

#### Advanced Recording Features
Following V1 patterns with V2 improvements:
- **Preview Playback**: Short audio previews of alternative recordings
- **Quality Comparison**: Visual comparison of recording quality metrics
- **Download Integration**: Show download status for each recording option
- **User Reviews**: Display user reviews for each recording option

#### Enhanced Sharing
Extended sharing capabilities:
- **Social Media Integration**: Direct sharing to specific platforms
- **Playlist Sharing**: Share entire playlists or track sequences
- **Custom Messages**: User-customizable share message templates
- **Track Timestamp Sharing**: Precise timestamp sharing for specific moments

## Conclusion

The PlaylistV2 menu system provides a comprehensive, professional-grade interface for accessing sharing and recording selection functionality. The implementation maintains V1 design excellence while following clean V2 architecture patterns, resulting in a system that is both powerful and maintainable.

Key achievements:
- **Complete Functionality**: All menu features working through comprehensive stubs
- **V1 Design Parity**: Exact visual reproduction of V1 interface patterns
- **Clean Architecture**: V2 patterns with proper separation of concerns
- **Rich Mock Data**: Realistic Cornell '77 data for authentic testing experience
- **Future Ready**: Architecture prepared for V2 domain model integration

The menu system demonstrates the effectiveness of V2 architecture in delivering complex functionality through clean, testable code while maintaining the beloved user experience that Dead Archive users expect.