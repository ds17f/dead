# Playback History Tracking - Implementation Analysis & Summary

## Original Problem Statement
- **Primary Issue**: Wrong tracks being recorded in playback history
- **Secondary Issues**: 
  - Tracks recorded as "played" when not actually played
  - Multiple false entries during show loading
  - Track-to-show associations incorrect
  - Multiple tracks from same show not all being recorded

## Core Architecture Understanding

### Current System Design
```
User Action → MediaController → PlaybackHistoryTracker → PlaybackHistoryRepository → Database
```

**Key Components**:
- `MediaControllerRepository`: Manages Media3 ExoPlayer via MediaController
- `PlaybackHistoryTracker`: Listens to MediaController state changes, creates sessions
- `PlaybackHistoryRepository`: Persists history entries to database

### State Flow Dependencies
```kotlin
// PlaybackHistoryTracker listens to:
mediaControllerRepository.isPlaying          // Play/pause state
mediaControllerRepository.currentTrackUrl    // Current track
mediaControllerRepository.currentRecordingId // Current recording
mediaControllerRepository.queueMetadata      // Track metadata
```

## Attempted Solutions & Results

### Attempt 1: Recording ID Synchronization
**What we tried**: Updated recording IDs during track transitions in `MediaControllerRepository.onMediaItemTransition()`

**Code changes**:
```kotlin
// Extract and update recording ID from track URL
mediaItem?.mediaId?.let { trackUrl ->
    val extractedRecordingId = localFileResolver.extractRecordingIdFromUrl(trackUrl)
    // Update recording ID if changed
}
```

**Result**: ✅ Fixed track-to-show associations, but core timing issues remained

### Attempt 2: Pending Session Info Pattern
**What we tried**: Two-phase session creation - prepare on track change, create on playback start

**Code changes**:
```kotlin
// onTrackChanged: Create pendingSessionInfo, don't create session yet
pendingSessionInfo = trackInfo.copy(trackId = trackUrl, recordingId = recordingId)

// onPlaybackStarted: Create session from pendingSessionInfo
if (currentSession == null && pendingSessionInfo != null) {
    createSessionFromPendingInfo()
}
```

**Result**: ✅ Eliminated false entries during show loading, but failed for track transitions during playback

### Attempt 3: Track URL Validation
**What we tried**: Added validation to ensure sessions match current MediaController state

**Code changes**:
```kotlin
// Verify pending session matches current track URL
val currentTrackUrl = mediaControllerRepository.currentTrackUrl.value
if (currentTrackUrl != trackInfo.trackId) {
    // Skip session creation or use fallback validation
}
```

**Result**: ⚠️ Helped with timing issues but introduced new edge cases

### Attempt 4: Immediate Session Creation During Playback
**What we tried**: Create sessions immediately when track changes and `isPlaying=true`

**Code changes**:
```kotlin
// If playback is already active, create session immediately
if (mediaControllerRepository.isPlaying.value) {
    createSessionFromPendingInfo()
}
```

**Result**: ❌ Created sessions for every track change during playback, causing multiple false entries

### Attempt 5: Position-Based Session Creation
**What we tried**: Trigger session creation when position advances (indicating actual playback)

**Code changes**:
```kotlin
// In onPositionChanged: Create session when position advances
if (currentSession == null && pendingSessionInfo != null && position > 0 && position > lastPosition) {
    createSessionFromPendingInfo()
}
```

**Result**: ❌ Still creating unwanted sessions

## Fundamental Issues Identified

### Core Timing Problem
**Issue**: `onPlaybackStarted()` is only called when `isPlaying` state changes from `false` to `true`. During track transitions while already playing, `isPlaying` stays `true`, so `onPlaybackStarted()` is never called again.

**Flow**:
1. Track A starts → `isPlaying: false→true` → `onPlaybackStarted()` ✅
2. User switches to Track B → `isPlaying: true→true` → No `onPlaybackStarted()` ❌

### MediaController State Synchronization
**Issue**: Multiple state flows updating at different times cause race conditions:
- `currentTrackUrl` changes
- `currentRecordingId` changes  
- `isPlaying` may or may not change
- `queueMetadata` updates

### Session Lifecycle Complexity
**Issue**: Difficult to distinguish between:
- Track preparation (MediaController loads track)
- Track selection (user clicks but doesn't play)
- Actual track playback (track is playing audio)

## Database & Architecture Findings

### Database Schema (Working Correctly)
- `playback_history` table allows multiple entries with same `sessionId`
- Primary key is unique `id` (UUID per entry)
- `sessionId` groups tracks from same recording session
- No database constraints preventing multiple track entries

### Session ID Management (Working Correctly)
- New `sessionId` generated per recording change
- Multiple tracks from same recording share same `sessionId`
- This is the intended behavior

## What IS Working

1. ✅ **Basic session creation** for initial track playback
2. ✅ **Recording ID extraction** from URLs
3. ✅ **Track metadata extraction** from queue
4. ✅ **Session finalization** when tracks change
5. ✅ **Database persistence** of history entries
6. ✅ **No false entries during show loading** (after pending session fix)

## What ISN'T Working

1. ❌ **Track transitions during active playback** - subsequent tracks not recorded
2. ❌ **Multiple track recording** from same show
3. ❌ **Session creation timing** - relying solely on `isPlaying` state changes
4. ❌ **State synchronization** - race conditions between different flows

## Alternative Approaches to Consider

### Option 1: MediaController Event-Based Approach
Listen directly to `onMediaItemTransition` events with reasons:
```kotlin
Player.MEDIA_ITEM_TRANSITION_REASON_SEEK        // User navigation
Player.MEDIA_ITEM_TRANSITION_REASON_AUTO        // Auto advance
Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT      // Track repeat
```

### Option 2: Unified State Flow Approach
Combine ALL relevant states into single flow to avoid race conditions:
```kotlin
combine(trackUrl, recordingId, isPlaying, playbackState, position) { ... }
```

### Option 3: Service-Level Tracking
Move tracking logic into `DeadArchivePlaybackService` where Media3 events are native

### Option 4: Timer-Based Validation
Use short delays/timers to ensure all state updates have settled before creating sessions

## Key Architectural Insights

1. **MediaController is asynchronous** - state updates don't happen atomically
2. **Media3 track transitions** don't always trigger `isPlaying` changes
3. **Position updates** happen frequently and may not indicate actual playback intent
4. **Queue preparation vs. playback** are difficult to distinguish in current architecture

## Recommendations for New Implementation

1. **Move tracking to service level** where Media3 events are native and synchronous
2. **Use MediaController transition reasons** to distinguish preparation vs. playback
3. **Implement state debouncing** to handle asynchronous updates
4. **Consider session creation triggers beyond just `isPlaying` changes**
5. **Add integration tests** to validate specific user flows

This summary should provide a solid foundation for your new implementation approach, Captain.