# PlayerV2 UI Design Specification

## Overview

This document defines the complete UI design specification for PlayerV2, following a UI-first development approach. The design focuses on creating a professional music player experience that rivals modern streaming apps while maintaining the Dead Archive aesthetic and functionality.

**Key Principles:**
- UI-first development - build components first, discover data needs through implementation  
- Material3 design system with Dead & Company branding
- No database or original model modifications - pure UI development
- Mock data integration for all interactive elements
- Professional music player experience

## Complete UI Layout Structure

### 1. Top Navigation Bar
**Layout:** Fixed header with three sections

- **Left:** Down chevron icon (`IconResources.Navigation.KeyboardArrowDown()`)
  - Action: Navigate back to previous screen
  - Styling: Standard IconButton with Material3 theming

- **Center:** Context text display
  - Text: "Playing from [context]" where context is:
    - "Show" - when playing from a specific concert
    - "Playlist" - when playing from user playlist  
    - "Queue" - when playing from queue
    - "Library" - when playing from library
  - Styling: `MaterialTheme.typography.bodyMedium`

- **Right:** Vertical 3-dot menu icon (`IconResources.Navigation.MoreVert()`)
  - Action: Opens Track Actions Bottom Sheet
  - Styling: Standard IconButton

### 2. Track Actions Bottom Sheet
**Triggered:** Tap 3-dot menu in top navigation

**Content Structure:**
- **Track Card Section:**
  - Identical to LibraryV2 list item design
  - Left: Album cover (60dp square)
  - Center: Track title (top), Show date (middle), Venue/City/State (bottom)
  - Right: Download/pin status icons
  
- **Action Buttons:**
  - **Share** - Primary action button with share icon
  - **Add to Playlist** - Secondary action (shows "coming soon" snackbar)
  - **Download** - Download management (show current status)
  - **More Options** - Expandable section for future features

**Technical:** Use `ModalBottomSheet` following existing patterns in `ShowActionsBottomSheet.kt`

### 3. Main Cover Art Section
**Size:** ~40% of screen height (approximately 300-350dp on standard devices)

**Design:**
- Square aspect ratio album cover
- Rounded corners (`RoundedCornerShape(16.dp)`)
- Material3 surface with proper elevation
- Placeholder: Dead & Company stealie or iconography
- Background: Subtle gradient or solid color from Material3 scheme

**Responsive Behavior:**
- Maintains aspect ratio across device sizes
- Minimum size constraints for smaller screens
- Maximum size constraints for tablets

### 4. Track Information Row
**Layout:** Two-column row with space between

**Left Column (weighted to take most space):**
- **Line 1:** Track title
  - Typography: `MaterialTheme.typography.headlineSmall`
  - Font weight: `FontWeight.Bold`
  - Color: `MaterialTheme.colorScheme.onSurface`
  
- **Line 2:** Show date
  - Typography: `MaterialTheme.typography.bodyLarge`  
  - Color: `MaterialTheme.colorScheme.onSurfaceVariant`
  
- **Line 3:** Venue, City, State
  - Typography: `MaterialTheme.typography.bodyMedium`
  - Color: `MaterialTheme.colorScheme.onSurfaceVariant`

**Right Column:**
- **Circle Plus Icon** (`IconResources.Actions.AddCircle()`)
- Size: 32dp
- Action: Show "Playlists are coming soon" snackbar
- Styling: `MaterialTheme.colorScheme.primary` tint

### 5. Progress Control Section
**Layout:** Full-width progress control with time displays

**Components:**
- **Progress Slider:**
  - `Slider` composable with Material3 styling
  - Full width of container minus padding
  - Small circle thumb indicator
  - Track color highlighting for progress portion
  
- **Time Display Row:**
  - Left: Current position (e.g., "2:34")
  - Right: Total duration (e.g., "8:15")
  - Typography: `MaterialTheme.typography.bodySmall`
  - Color: `MaterialTheme.colorScheme.onSurfaceVariant`

### 6. Primary Controls Row
**Layout:** Horizontal row with equal spacing between controls

**Controls (left to right):**
1. **Shuffle Button**
   - Icon: `IconResources.PlayerControls.Shuffle()`
   - Toggle state with visual feedback
   - Size: 40dp IconButton

2. **Previous Track**
   - Icon: `IconResources.PlayerControls.SkipPrevious()`
   - Size: 48dp IconButton

3. **Play/Pause (Center)**
   - Icons: `IconResources.PlayerControls.Play()` / `IconResources.PlayerControls.Pause()`
   - Size: 64dp IconButton (larger than others)
   - Primary color background with surface color icon

4. **Next Track**
   - Icon: `IconResources.PlayerControls.SkipNext()`
   - Size: 48dp IconButton

5. **Repeat Mode Button**
   - Icons: Cycle through repeat states
     - Normal: `IconResources.PlayerControls.Repeat()` (dim)
     - Repeat All: `IconResources.PlayerControls.Repeat()` (active)
     - Repeat One: `IconResources.PlayerControls.RepeatOne()` (active)
   - Size: 40dp IconButton

### 7. Secondary Controls Row
**Layout:** Three-section row with space between

**Left:**
- **Connections Icon** (`IconResources.Connectivity.Cast()`)
- Action: Opens "Connect" bottom sheet with "coming soon" content
- Size: 40dp IconButton

**Right Section (two buttons):**
- **Share Button** (`IconResources.Actions.Share()`)
  - Action: Share current track
  - Size: 40dp IconButton
  
- **Queue Button** (`IconResources.PlayerControls.Queue()`)
  - Action: Navigate to queue screen
  - Size: 40dp IconButton

### 8. Extended Content Sections
**Layout:** Scrollable content below controls with expanding sections

#### About the Venue
- **Header:** "About [Venue Name]"
- **Content:** Venue description, history, notable shows
- **Expandable:** Tap to show/hide full content
- **Styling:** Material3 expansion panel or custom collapsible

#### Lyrics
- **Header:** "Lyrics"
- **Content:** Song lyrics with proper formatting
- **Expandable:** Tap to show/hide
- **Features:** Scroll-to-current-position (future enhancement)

#### Explore/Similar Shows  
- **Header:** "Similar Shows" or "More from [Year]"
- **Content:** Horizontal scrollable list of related concerts
- **Item Design:** Mini cards with date, venue, and rating
- **Action:** Tap to navigate to show

#### Credits
- **Header:** "Credits"
- **Content:** Performer information, recording details
- **Layout:** Structured list with roles and names
- **Styling:** Clean typography hierarchy

## Technical Implementation Strategy

### Component Architecture
```kotlin
@Composable
fun PlayerV2Screen() {
    Column {
        PlayerV2TopBar()
        PlayerV2CoverArt()
        PlayerV2TrackInfo()
        PlayerV2ProgressControl()
        PlayerV2PrimaryControls()
        PlayerV2SecondaryControls()
        PlayerV2ExtendedContent()
    }
}
```

### Mock Data Classes
```kotlin
data class PlayerV2TrackInfo(
    val title: String,
    val showDate: String,
    val venue: String,
    val city: String,
    val state: String
)

data class PlayerV2ProgressInfo(
    val currentPosition: String,
    val totalDuration: String,
    val progress: Float
)

data class PlayerV2ControlState(
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode
)

enum class RepeatMode { NORMAL, REPEAT_ALL, REPEAT_ONE }
```

### State Management Integration
- Extend existing `PlayerV2Service` with new UI data requirements
- Add methods for track info, progress, and control states
- Implement mock data providers for all UI sections
- Wire up interactive callbacks with appropriate mock responses

### Bottom Sheet Components
```kotlin
@Composable
fun TrackActionsBottomSheet()

@Composable  
fun ConnectBottomSheet()
```

### Design Consistency Requirements
- Use existing `IconResources` for all icons
- Follow Material3 color scheme throughout
- Match LibraryV2 card design patterns for track information
- Maintain consistent spacing (8dp, 16dp, 24dp grid)
- Implement proper touch targets (minimum 44dp)
- Add appropriate content descriptions for accessibility

### Mock Data Integration
- Rich track metadata (titles, dates, venues)
- Realistic show and venue information
- Progress simulation with proper time formatting
- Control state management (play/pause, shuffle, repeat)
- Extended content placeholders (venue info, lyrics, credits)

## Implementation Phases

### Phase 1: Core Layout Structure
1. Redesign main PlayerV2Screen layout
2. Implement top navigation bar
3. Create large cover art section
4. Build track information display

### Phase 2: Interactive Controls
1. Implement progress slider with mock data
2. Create primary control row with state management
3. Add secondary control row
4. Wire up all button interactions

### Phase 3: Bottom Sheets & Extended Content
1. Build Track Actions bottom sheet
2. Create Connect bottom sheet placeholder
3. Implement extended content sections
4. Add smooth expand/collapse animations

### Phase 4: Polish & Refinement
1. Add proper mock data throughout
2. Implement responsive design adjustments
3. Add accessibility features
4. Test interaction flows and animations

## Success Criteria
- Professional music player appearance matching modern streaming apps
- All interactive elements respond with appropriate feedback
- Smooth animations and transitions
- Consistent design language with existing Dead Archive components
- Ready for real data integration without UI changes
- Maintains pure UI-first development approach (no database/model changes)

This specification provides the complete blueprint for implementing a world-class PlayerV2 interface while maintaining the V2 architecture principles and UI-first development methodology.