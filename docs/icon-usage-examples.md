# Material Icon Usage Examples

This document provides practical examples of how to use the IconResources utility in your Android application.

## Basic Usage

### In PlayerScreen.kt

```kotlin
import com.deadarchive.core.design.component.IconResources

// Replace:
Icon(
    Icons.Default.PlayArrow,
    contentDescription = "Play",
    modifier = Modifier.size(36.dp)
)

// With:
Icon(
    IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.LARGE)
)
```

### In MiniPlayer.kt

```kotlin
// Replace:
Icon(
    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
)

// With:
Icon(
    if (uiState.isPlaying) IconResources.PlayerControls.Pause else IconResources.PlayerControls.Play,
    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
)
```

## Examples for Missing Icons

### Star Icon (currently using as Pause replacement)

```kotlin
// Current implementation with missing Pause icon
Icon(
    if (uiState.isPlaying) Icons.Default.Star else Icons.Default.PlayArrow,
    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
)

// Better implementation using IconResources
Icon(
    if (uiState.isPlaying) IconResources.PlayerControls.Pause else IconResources.PlayerControls.Play,
    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
)
```

### SkipPrevious and SkipNext

```kotlin
// Previous button - currently using rotated PlayArrow
Icon(
    Icons.Default.PlayArrow,
    contentDescription = "Previous",
    modifier = Modifier.graphicsLayer(rotationZ = 180f)
)

// Better implementation with proper icon
Icon(
    IconResources.PlayerControls.SkipPrevious,
    contentDescription = "Previous"
)

// Next button - currently using PlayArrow
Icon(
    Icons.Default.PlayArrow,
    contentDescription = "Next"
)

// Better implementation with proper icon
Icon(
    IconResources.PlayerControls.SkipNext,
    contentDescription = "Next"
)
```

## Custom Icons Example

When a standard Material Design icon doesn't exist, you can add a custom SVG icon:

1. Download the SVG icon
2. Convert it to Vector Drawable using the script
3. Access it through the IconResources utility

```kotlin
// Using a custom icon
Icon(
    painter = IconResources.customIcon(R.drawable.ic_custom_icon),
    contentDescription = "Custom icon",
    modifier = Modifier.size(IconResources.Size.MEDIUM)
)
```

## Consistent Sizing

```kotlin
// Use consistent sizes from IconResources.Size
Icon(
    IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.SMALL)  // 16.dp
)

Icon(
    IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.MEDIUM) // 24.dp
)

Icon(
    IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.LARGE)  // 32.dp
)

Icon(
    IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.XLARGE) // 48.dp
)
```

## Adding New Icons

When you need new icons, add them to the appropriate category in `IconResources.kt`:

```kotlin
object PlayerControls {
    // Existing icons
    val Play = Icons.Default.PlayArrow
    val Pause = Icons.Default.Pause
    
    // New icon
    val Rewind10 = Icons.Default.Replay10
}
```

Or use the download script to fetch and add them automatically:

```bash
python scripts/download_material_icons.py --icon-name "replay_10" --update-registry --registry-category "PlayerControls"
```