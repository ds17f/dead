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

### Option 2: Direct UI Testing

You can also test downloads through the normal UI:

1. **Browse Screen**:
   - Search for concerts (e.g., "grateful dead 1977")
   - Expand a concert to see recordings
   - Click the download button next to any recording

2. **Library Screen**:
   - Add shows to your library first
   - Expand shows and click download buttons

3. **Concert List Screen**:
   - Navigate to playlists/concerts
   - Use download buttons on recordings

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

## Files Involved

### Core Components
- `DownloadRepository` - Manages download database operations
- `DownloadQueueManager` - Manages WorkManager scheduling
- `AudioDownloadWorker` - Performs actual downloads
- `DownloadQueueManagerWorker` - Processes queue

### UI Components
- `ExpandableConcertItem` - Shows download buttons
- `DownloadState` - Manages download state UI
- `BrowseScreen/LibraryScreen/ConcertListScreen` - Download integration

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