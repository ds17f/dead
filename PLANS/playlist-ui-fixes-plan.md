# Playlist UI Fixes Implementation Plan

## Current Status
Working on fixing 4 main issues with the playlist screen UI and adding menu functionality.

## Issues to Fix

### 1. ✅ COMPLETED - Play/Pause Button Logic
**Status**: Fixed - play button now shows pause when current recording is playing
- Added `isCurrentRecordingPlaying` logic to check if current track matches what's playing
- Downloads `pause_circle_filled` icon via material_icons_config.json 
- Button now toggles between play/pause correctly
- Uses `mediaControllerRepository.play()` and `pause()` methods

### 2. 🟡 IN PROGRESS - Library Icon Red When In Library  
**Status**: Code added but needs testing
- Added `isInLibrary` StateFlow to PlayerViewModel
- Added `checkLibraryStatus()` and `toggleLibrary()` methods to PlayerViewModel
- Updated PlaylistScreen to collect library state and check status when showId changes
- Library button now shows different icon (LibraryAddCheck) and red color when in library
- **NEEDS**: Testing to verify it works correctly

### 3. ❌ TODO - Download Icon Shows Stop During Download
**Status**: Not started
- Currently download button shows no icon during download (should show stop icon)
- Progress wheel works correctly but missing stop icon in center
- **NEEDS**: Update download button to show stop icon with progress ring around it
- **LOCATION**: PlaylistScreen.kt around line 500-520, download button section

### 4. ❌ TODO - Triple Dot Menu Bottom Sheet
**Status**: Not started  
- Currently menu button does nothing (`onClick = { /* TODO: Show menu */ }`)
- **NEEDS**: Create menu bottom sheet component with 2 options:
  1. "Share" - implement sharing functionality  
  2. "Choose Recording" - open existing recording selection sheet
- **LOCATION**: PlaylistScreen.kt around line 540, menu button

## Implementation Steps

### Step 3: Fix Download Icon (Next Task)
1. **Location**: Find download button in PlaylistScreen.kt (around line 500)
2. **Current Issue**: In `ShowDownloadState.Downloading` case, no stop icon shown
3. **Fix**: Add stop icon in center of progress ring
4. **Code Pattern**:
   ```kotlin
   is ShowDownloadState.Downloading -> {
       Box(contentAlignment = Alignment.Center) {
           CircularProgressIndicator(progress = downloadState.trackProgress)
           Icon(painter = painterResource(R.drawable.ic_stop), ...)
       }
   }
   ```

### Step 4: Create Menu Bottom Sheet
1. **Create**: New component `MenuBottomSheet.kt` in `feature/playlist/components/`
2. **Add**: Menu state to PlaylistScreen (`showMenu` boolean)
3. **Update**: Menu button onClick to show sheet
4. **Add**: Share functionality (use existing ShareService)
5. **Connect**: "Choose Recording" to existing recording selection sheet

## Files Modified So Far
- `feature/player/src/main/java/com/deadarchive/feature/player/PlayerViewModel.kt` - Added library state management
- `feature/playlist/src/main/java/com/deadarchive/feature/playlist/PlaylistScreen.kt` - Updated library button and play/pause logic
- `scripts/material_icons_config.json` - Added pause_circle_filled and stop icons

## Files That Need Changes
- `feature/playlist/src/main/java/com/deadarchive/feature/playlist/PlaylistScreen.kt` - Fix download icon, add menu functionality
- `feature/playlist/src/main/java/com/deadarchive/feature/playlist/components/MenuBottomSheet.kt` - New file to create

## Testing Checklist
- [ ] Play/pause button toggles correctly when playing current recording
- [ ] Library icon turns red and shows check when show is in library  
- [ ] Download button shows stop icon during download
- [ ] Menu opens bottom sheet with Share and Choose Recording options
- [ ] Share functionality works
- [ ] Choose Recording opens recording selection sheet

## Notes
- Recording selection sheet already exists and was working before - need to reconnect it
- ShareService already exists in codebase
- All required icons are now downloaded (pause_circle_filled, stop)