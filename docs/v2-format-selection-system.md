# V2 Format Selection and Fallback System

## Problem Statement

Currently, V2 MediaControllerRepository hardcodes `"VBR MP3"` format, causing 0 tracks when that format isn't available for a recording. We need a robust format selection system with intelligent fallbacks that keeps playlist UI and playback perfectly synchronized.

## Architecture Overview

### Core Principles
1. **Smart Format Selection at Playlist Building Time** - Format selection with fallbacks happens once when building the track list
2. **Dumb MediaController** - MCR plays exactly what it's told or fails explicitly with detailed error information
3. **Perfect UI/Playback Sync** - What the user sees in the playlist is exactly what will play
4. **No Magic Fallbacks in MCR** - Explicit failures enable proper error handling and debugging

## System Design

### 1. Format Priority System

**Default Format Priority** (hardcoded initially, user-configurable later):
```kotlin
val DEFAULT_FORMAT_PRIORITY = listOf(
    "VBR MP3",      // Best balance for streaming
    "MP3",          // Universal fallback  
    "Ogg Vorbis"    // Good quality, efficient
    // Note: FLAC excluded - ExoPlayer compatibility issues
)
```

**Future Extensibility**:
- User preferences can override default priorities
- Per-show format selection via context menu
- Quality vs. bandwidth preferences

### 2. Component Responsibilities

#### PlaylistService (Smart Format Selection)
- **Responsibility**: Determine best available format using priority fallback
- **Timing**: During `getTrackList()` - format selection happens once
- **Behavior**: Try formats in priority order until tracks found
- **Storage**: Remember selected format for playback coordination

```kotlin
// PlaylistServiceImpl.getTrackList()
override suspend fun getTrackList(): List<PlaylistTrackViewModel> {
    val allTracks = archiveService.getRecordingTracks(currentRecording).getOrNull() ?: emptyList()
    
    // Smart format selection with fallback
    val selectedFormat = selectBestAvailableFormat(allTracks, DEFAULT_FORMAT_PRIORITY)
    
    if (selectedFormat == null) {
        // Handle no compatible format found
        return emptyList() // or show error state
    }
    
    // Store selected format for playback
    currentSelectedFormat = selectedFormat
    
    // Filter to selected format only
    val selectedTracks = allTracks.filter { it.format.equals(selectedFormat, ignoreCase = true) }
    
    return selectedTracks.mapIndexed { index, track ->
        PlaylistTrackViewModel(
            number = index + 1,
            title = track.title ?: track.name,
            duration = formatDuration(track.duration),
            format = selectedFormat, // UI shows selected format
            // ... other properties
        )
    }
}

private fun selectBestAvailableFormat(
    allTracks: List<Track>,
    formatPriorities: List<String>
): String? {
    for (preferredFormat in formatPriorities) {
        val tracksInFormat = allTracks.filter { 
            it.format.equals(preferredFormat, ignoreCase = true) 
        }
        
        if (tracksInFormat.isNotEmpty()) {
            Log.d(TAG, "Selected format '$preferredFormat' (${tracksInFormat.size} tracks)")
            return preferredFormat
        }
        
        Log.d(TAG, "Format '$preferredFormat' not available, trying next...")
    }
    
    Log.w(TAG, "No tracks found in any preferred format. Available formats: ${allTracks.map { it.format }.distinct()}")
    return null
}
```

#### MediaControllerRepository (Dumb Playback Service)
- **Responsibility**: Play exactly the format requested or fail explicitly
- **Behavior**: Strict format matching - no magic fallbacks
- **Failure Handling**: Throw descriptive exception with available format information

```kotlin
// MediaControllerRepository.playAll()
suspend fun playAll(recordingId: String, format: String, startPosition: Long = 0L) {
    val allTracks = archiveService.getRecordingTracks(recordingId).getOrNull() ?: emptyList()
    
    // Exact format matching only
    val filteredTracks = allTracks.filter { it.format.equals(format, ignoreCase = true) }
    
    if (filteredTracks.isEmpty()) {
        // Explicit failure with debugging information
        throw FormatNotAvailableException(
            message = "Format '$format' not available for recording $recordingId",
            requestedFormat = format,
            availableFormats = allTracks.map { it.format }.distinct()
        )
    }
    
    Log.d(TAG, "Playing ${filteredTracks.size} tracks in format: $format")
    
    // Convert and play selected format tracks
    val mediaItems = convertToMediaItems(recordingId, filteredTracks)
    // ... continue with playback
}
```

#### PlaylistViewModel (Format Coordination)
- **Responsibility**: Coordinate between format selection and playback
- **Behavior**: Use format selected by PlaylistService, handle MCR failures gracefully

```kotlin
// PlaylistViewModel.togglePlayback()
fun togglePlayback() {
    viewModelScope.launch {
        try {
            val showData = _uiState.value.showData ?: return@launch
            val recordingId = showData.currentRecordingId ?: return@launch
            
            // Get the format that was selected during playlist building
            val selectedFormat = playlistService.getCurrentSelectedFormat()
            
            if (selectedFormat == null) {
                Log.w(TAG, "No format selected - cannot start playback")
                return@launch
            }
            
            Log.d(TAG, "Starting playback with format: $selectedFormat")
            
            // MCR plays exactly what playlist selected
            mediaControllerRepository.playAll(recordingId, selectedFormat)
            
        } catch (e: FormatNotAvailableException) {
            Log.e(TAG, "Format playback failed: ${e.message}")
            Log.e(TAG, "Available formats: ${e.availableFormats}")
            
            // Could show user error or retry with different format
            _uiState.value = _uiState.value.copy(
                error = "Playback format not available: ${e.requestedFormat}"
            )
        }
    }
}
```

### 3. Exception Handling

```kotlin
/**
 * Exception thrown when requested audio format is not available
 * Provides detailed information for debugging and user feedback
 */
class FormatNotAvailableException(
    message: String,
    val requestedFormat: String,
    val availableFormats: List<String>
) : Exception("$message. Available formats: $availableFormats")
```

## Data Flow

### Playlist Building Flow
1. **User navigates to show** → PlaylistViewModel.loadShow()
2. **PlaylistService.getTrackList()** called
3. **ArchiveService** provides all available tracks (all formats)
4. **selectBestAvailableFormat()** tries priority list: VBR MP3 → MP3 → Ogg Vorbis
5. **First available format selected** (e.g., "MP3" if VBR MP3 not available)
6. **Tracks filtered** to selected format only
7. **UI displays** tracks in selected format
8. **Selected format stored** for playback coordination

### Playback Flow
1. **User hits play** → PlaylistViewModel.togglePlayback()
2. **Get selected format** from PlaylistService
3. **MediaControllerRepository.playAll(recordingId, selectedFormat)**
4. **MCR loads tracks** and filters to exact requested format
5. **If format available**: Play tracks (perfect UI sync)
6. **If format unavailable**: Throw FormatNotAvailableException
7. **ViewModel handles exception** with error logging/user feedback

## Benefits

### Reliability
- **Always finds playable content** when any compatible format exists
- **Graceful degradation** through format priority fallback
- **Explicit failure handling** when no compatible formats available

### Performance  
- **Format selection happens once** during playlist building
- **No repeated format queries** during playback
- **Cached format decision** used for all playback operations

### User Experience
- **Perfect UI/playback sync** - no format surprises
- **Transparent format selection** - logs show selection reasoning
- **Predictable behavior** - consistent format choice per recording

### Developer Experience
- **Clean separation of concerns** - smart selection vs. dumb playback
- **Excellent debugging** - detailed format availability logging
- **Testable components** - isolated format selection logic
- **Future-ready architecture** - easy user preferences integration

## Future Enhancements

### User Format Preferences
```kotlin
// Future UserPreferencesService integration
val userFormatPriorities = userPreferencesService.getFormatPreferences() 
                           ?: DEFAULT_FORMAT_PRIORITY

val selectedFormat = selectBestAvailableFormat(allTracks, userFormatPriorities)
```

### Per-Show Format Override
```kotlin
// Context menu: "Choose Format" → format picker dialog
fun selectSpecificFormat(format: String) {
    playlistService.overrideFormatSelection(format)
    loadTrackList() // Rebuild with chosen format
}
```

### Quality vs. Bandwidth Settings
```kotlin
// User preference: "High Quality" vs "Fast Streaming"
val formatPriorities = when (userPrefs.qualityMode) {
    QualityMode.HIGH -> listOf("Flac", "VBR MP3", "MP3")
    QualityMode.BALANCED -> listOf("VBR MP3", "MP3", "Ogg Vorbis") 
    QualityMode.FAST -> listOf("MP3", "Ogg Vorbis", "VBR MP3")
}
```

## Implementation Plan

### Phase 1: Core Format Selection
1. Create `FormatNotAvailableException` in media module
2. Add format selection logic to `PlaylistServiceImpl`
3. Update `MediaControllerRepository` to throw explicit format exceptions
4. Update `PlaylistViewModel` to coordinate format selection and handle exceptions
5. Test with recordings lacking VBR MP3 format

### Phase 2: Enhanced Error Handling
1. Add user-friendly error messages for format failures
2. Improve logging and debugging information
3. Add format availability indicators in UI

### Phase 3: User Preferences Integration
1. Create format preferences in settings
2. Integrate user preferences with format selection
3. Add per-show format override functionality

## Testing Strategy

### Unit Tests
- Format selection logic with various track combinations
- Exception handling for missing formats
- Priority fallback ordering

### Integration Tests  
- End-to-end playlist building with format selection
- Playback coordination with selected formats
- Error scenarios with no compatible formats

### Manual Testing
- Recordings with only MP3 (no VBR MP3)
- Recordings with only Flac
- Recordings with no audio tracks
- User preference override scenarios

## Conclusion

This system provides robust format handling while maintaining clean architecture principles. Smart format selection at playlist building time ensures perfect UI/playback synchronization, while explicit failure handling enables proper error management and debugging. The design is extensible for future user preferences and per-show format overrides while keeping the MediaControllerRepository as a predictable, testable service.