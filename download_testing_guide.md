# Download System Testing Guide

This guide explains how to test the download system implementation that was added in Task 4.

## Testing Options

### Option 1: Debug Screen Testing (Recommended)

The debug screen now includes comprehensive download system testing tools:

1. **Open the Debug Screen**:
   - Launch the app
   - Navigate to Settings → Debug & Testing

2. **Download System Testing Section**:
   - **Test Download**: Downloads a single sample recording
   - **Test Queue**: Queues multiple downloads to test concurrency
   - **Check Download Status**: Shows current system status and monitoring commands

3. **Test Steps**:
   ```
   1. First run "Export Test Data" to populate the database with recordings
   2. Click "Test Download" to test a single download
   3. Click "Test Queue" to test multiple downloads
   4. Use "Check Download Status" to monitor the system
   ```

### Option 2: Direct UI Testing (NEW Show-Level Downloads!)

You can now test downloads through the normal UI with enhanced show-level downloads:

1. **Show-Level Download (NEW)**:
   - **Browse Screen**: Search for concerts and click the download icon in the show header
   - **Library Screen**: Click download icons on shows in your library
   - **Concert List Screen**: Use download icons on show headers
   - Downloads the **best/priority recording** automatically
   - **Visual States**: Gray (not downloaded) → Orange (downloading) → Green (success) → Red (failed)

2. **Individual Recording Downloads** (existing):
   - Expand any concert to see individual recordings
   - Click download buttons next to specific recordings
   - Each recording has its own download state

## Monitoring Downloads

### Logcat Commands

Monitor download progress with these logcat commands:

```bash
# View all download-related logs
adb logcat | grep -E "(Download|Queue|Worker)"

# Specific download components
adb logcat | grep -E "(DownloadRepository|DownloadQueueManager|AudioDownloadWorker)"

# WorkManager status
adb logcat | grep WorkManager

# Network activity
adb logcat | grep -i network
```

### WorkManager Status

Check WorkManager status:

```bash
# Check scheduled work
adb shell dumpsys jobscheduler | grep androidx.work

# Check work status
adb shell dumpsys jobscheduler | grep -A 10 "androidx.work"
```

## Expected Behavior

### Show-Level Download States (NEW)
- **Gray Download Icon**: Show not downloaded (default state)
- **Orange Download Icon**: Show is currently downloading 
- **Green Checkmark Icon**: Show successfully downloaded
- **Red Download Icon**: Download failed or encountered error

### Single Download Test
1. Should create a download entry in the database
2. Should trigger the DownloadQueueManagerWorker
3. Should start the AudioDownloadWorker for the specific recording
4. Should show progress in logs

### Queue Test
1. Should create multiple download entries
2. Should respect concurrency limits (max 2 concurrent downloads)
3. Should process downloads in queue order
4. Should handle failures gracefully

### Show Download Behavior
1. Clicking show download icon downloads the **best/priority recording**
2. Best recording is determined by quality priority: SBD > MATRIX > FM > AUD
3. Download state reflects the status of the best recording download
4. Individual recording downloads work independently

### Status Check
Shows:
- Whether queue processing is active
- Current queue status
- Number of pending downloads
- Monitoring commands

## Database Verification

You can also verify downloads in the database:

```sql
-- Check download entries
SELECT * FROM downloads;

-- Check by status
SELECT * FROM downloads WHERE status = 'QUEUED';
SELECT * FROM downloads WHERE status = 'DOWNLOADING';
SELECT * FROM downloads WHERE status = 'COMPLETED';
```

## Troubleshooting

### Common Issues

1. **No Recordings Available**
   - Run "Export Test Data" first to populate the database

2. **Downloads Not Starting**
   - Check if WorkManager is properly initialized
   - Verify Hilt dependency injection is working
   - Check network connectivity

3. **Queue Not Processing**
   - Verify DownloadQueueManagerWorker is running
   - Check WorkManager constraints (network, storage, etc.)

### Debug Information

The debug screen provides:
- Real-time status updates
- Error messages with detailed information
- Logcat commands for monitoring
- System status verification

## UI Changes Summary

### Enhanced Show Header
- **New Download Button**: Added between Library and Expand buttons
- **State-Based Icons**: Visual feedback with color-coded states
- **Smart Download**: Automatically selects best/priority recording

### Visual Design
- **Colors Used**:
  - Gray: Default theme color for not downloaded
  - Orange (`#FFA726`): Downloading state  
  - Green (`#4CAF50`): Successfully downloaded
  - Red: Theme error color for failed downloads
- **Icons Used**:
  - `ic_file_download`: Default download icon
  - `ic_download_done`: Success checkmark icon

### User Experience
- **One-Click Downloads**: No need to expand shows to download
- **Priority Selection**: System chooses best quality automatically
- **Visual Feedback**: Clear state indication without text
- **Consistent Placement**: Same position across all screens

## Files Involved

### Core Components
- `DownloadRepository` - Manages download database operations
- `DownloadQueueManager` - Manages WorkManager scheduling
- `AudioDownloadWorker` - Performs actual downloads
- `DownloadQueueManagerWorker` - Processes queue

### UI Components (Enhanced)
- `ExpandableConcertItem` - Enhanced with show-level download buttons
- `ShowDownloadState` - New sealed class for show download states
- `DownloadState` - Existing recording-level download states
- `BrowseScreen/LibraryScreen/ConcertListScreen` - Updated with show download callbacks

### Testing Components
- `DebugScreen` - Download testing interface
- `DebugViewModel` - Testing logic and status monitoring

## Next Steps

After testing Task 4, you can proceed with:
- **Task 5**: Implement Download Progress Tracking
- **Task 6**: Add Download Notifications System
- **Task 7**: Implement Download Error Handling and Retry Logic
- **Task 8**: Add Download Settings and User Preferences

The testing infrastructure built in Task 4 will be valuable for testing all subsequent tasks.