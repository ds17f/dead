# Material Icons Reference for Dead Archive

This document provides a reference for Material Icons available for use in Jetpack Compose applications.

## Material Icons Libraries

Jetpack Compose Material Icons come in two separate libraries:

1. **Material Icons Core** (Default, included with Compose Material)
   - Contains a small subset of commonly used icons
   - No additional dependency required
   - Default import in our project

2. **Material Icons Extended**
   - Contains the full set of Material Design icons (+900 icons)
   - **Requires additional dependency:**
   ```kotlin
   implementation("androidx.compose.material:material-icons-extended")
   ```
   - Significantly increases app size
   - Not currently included in our project

## How to Import Icons

### Core Icons (Already available)
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*    // For Default/Filled icons
import androidx.compose.material.icons.outlined.*  // For Outlined icons
import androidx.compose.material.icons.rounded.*   // For Rounded icons
import androidx.compose.material.icons.sharp.*     // For Sharp icons
import androidx.compose.material.icons.twotone.*   // For Two-tone icons
```

### Extended Icons (Requires dependency)
```kotlin
// If you add the extended icons dependency:
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download  // Specific import for extended icons
// OR
import androidx.compose.material.icons.filled.*         // Wild card import (increases compile time)
```

## Icon Access Patterns

Icons can be accessed using one of these patterns:
- `Icons.Default.IconName` (same as Filled)
- `Icons.Filled.IconName`
- `Icons.Outlined.IconName`
- `Icons.Rounded.IconName`
- `Icons.Sharp.IconName`
- `Icons.TwoTone.IconName`

## Verified Available Core Icons in Dead Archive

These icons from the core set have been verified to work in the Dead Archive application:

### Navigation Icons
- `Icons.Default.Home` / `Icons.Filled.Home` / `Icons.Outlined.Home`
- `Icons.Default.KeyboardArrowRight` (Note: Consider `Icons.AutoMirrored.Filled.KeyboardArrowRight` for RTL support)
- `Icons.Default.ArrowBack` / `Icons.AutoMirrored.Filled.ArrowBack` (newer RTL-aware)
- `Icons.Default.Close`
- `Icons.Default.Menu`

### Action Icons
- `Icons.Default.Favorite` / `Icons.Filled.Favorite` / `Icons.Outlined.FavoriteBorder`
- `Icons.Default.Search` / `Icons.Filled.Search` / `Icons.Outlined.Search`
- `Icons.Default.Settings` / `Icons.Filled.Settings` / `Icons.Outlined.Settings`
- `Icons.Default.Star`
- `Icons.Default.Share`
- `Icons.Default.MoreVert` (options menu)
- `Icons.Default.ArrowDownward` (download alternative)
- `Icons.Default.GetApp` (download alternative)
- `Icons.Default.SaveAlt` (download alternative)
- `Icons.Default.List` (for playlists)

### Media Control Icons
- `Icons.Default.PlayArrow`
- `Icons.Default.Pause`
- `Icons.Default.SkipNext`
- `Icons.Default.SkipPrevious`
- `Icons.Default.Repeat`
- `Icons.Default.RepeatOne`
- `Icons.Default.Shuffle`

### Volume Control Icons
- `Icons.Default.VolumeUp`
- `Icons.Default.VolumeDown`
- `Icons.Default.VolumeMute`
- `Icons.Default.VolumeOff`

## Icon Usage in Dead Archive

### Bottom Navigation Bar
```kotlin
// From BottomNavigation.kt
HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
SEARCH("browse", "Search", Icons.Filled.Search, Icons.Outlined.Search),
LIBRARY("library", "Library", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
SETTINGS("debug", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
```

### Library Screen Sections
```kotlin
// From LibraryScreen.kt
LibrarySection(
    icon = Icons.Default.Favorite,
    title = "Liked Shows",
    subtitle = "Your favorite concerts"
)

LibrarySection(
    icon = Icons.Default.GetApp,  // Core icon alternative for download
    // OR icon = Icons.Default.SaveAlt
    // OR icon = Icons.Default.ArrowDownward
    title = "Downloaded",
    subtitle = "Available offline"
)

LibrarySection(
    icon = Icons.Default.PlayArrow,
    title = "Recently Played",
    subtitle = "Your listening history"
)

LibrarySection(
    icon = Icons.Default.Star,
    title = "Top Shows",
    subtitle = "Most popular concerts"
)
```

## Common Pitfalls

1. **Core vs Extended Icons**: Not all Material Design icons are available in the core library
   - ❌ `Icons.Default.Download` - Not available in core (requires extended dependency)
   - ❌ `Icons.Default.FileDownload` - Not available in core (requires extended dependency)
   - ❌ `Icons.Default.Folder` - Not available in core (requires extended dependency)
   - ✅ `Icons.Default.GetApp` - Available in core (alternative for download)
   - ✅ `Icons.Default.SaveAlt` - Available in core (alternative for download)

2. **Mismatched Package and Style**: Some icons have different naming patterns between styles
   - `Icons.Filled.Favorite` vs `Icons.Outlined.FavoriteBorder` (not `Icons.Outlined.Favorite`)
   - `Icons.Default.KeyboardArrowRight` vs `Icons.AutoMirrored.Filled.KeyboardArrowRight` (newer RTL-aware version)

3. **Missing Imports**: Ensure you've imported the correct style packages:
   ```kotlin
   import androidx.compose.material.icons.Icons
   import androidx.compose.material.icons.filled.*
   import androidx.compose.material.icons.outlined.*
   ```

4. **App Size Considerations**: Adding the extended icons dependency significantly increases app size
   - Core set: ~100 most common icons (minimal app size impact)
   - Extended set: ~900+ icons (significant app size impact)

## Finding Available Icons

To see all available Material Icons:
1. Check the [Material Icons page](https://fonts.google.com/icons) for visual reference
2. **Use only core icons** unless the extended dependency has been added
3. Test icons at compile time rather than assuming they exist
4. If an icon isn't available in core, find a suitable alternative from the core set
5. For exact naming conventions, check existing usage in the codebase

## Adding Extended Icons (If Needed)

If you need icons from the extended set:
1. Add the dependency to your app's build.gradle:
   ```kotlin
   implementation("androidx.compose.material:material-icons-extended")
   ```
2. Import the specific icons you need:
   ```kotlin
   import androidx.compose.material.icons.filled.Download
   ```
3. Document the addition in this reference guide
4. Consider app size impact - use only when necessary

## Media Player Icon Usage Examples

Here are examples of how to use icons in a media player UI:

### Player Controls
```kotlin
// Basic media controls
Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
    IconButton(onClick = { /* Skip to previous */ }) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous track"
        )
    }
    
    IconButton(
        onClick = { /* Play/Pause toggle */ },
        modifier = Modifier
            .size(64.dp)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(32.dp)
        )
    }
    
    IconButton(onClick = { /* Skip to next */ }) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next track"
        )
    }
}

// Additional controls
Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
) {
    IconButton(onClick = { /* Toggle shuffle */ }) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
    
    IconButton(onClick = { /* Toggle repeat */ }) {
        Icon(
            imageVector = when (repeatMode) {
                RepeatMode.NONE -> Icons.Default.Repeat
                RepeatMode.ALL -> Icons.Default.Repeat
                RepeatMode.ONE -> Icons.Default.RepeatOne
            },
            contentDescription = "Repeat mode",
            tint = if (repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
```

### Volume Controls
```kotlin
// Volume slider with icon
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 16.dp)
) {
    val volumeIcon = when {
        volume <= 0f -> Icons.Default.VolumeOff
        volume < 0.5f -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }
    
    IconButton(onClick = { /* Mute/unmute */ }) {
        Icon(
            imageVector = volumeIcon,
            contentDescription = "Volume"
        )
    }
    
    Slider(
        value = volume,
        onValueChange = { /* Update volume */ },
        modifier = Modifier.weight(1f)
    )
}