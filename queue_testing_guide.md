# Download Queue Management Testing Guide

## Prerequisites
- App installed with queue management implementation
- Android device/emulator connected
- Access to WorkManager Test Screen

## Test Scenarios

### Scenario 1: Basic Queue Processing
**Objective:** Verify queue manager starts and processes downloads

1. **Setup:**
   - Open WorkManager Test Screen
   - Tap "Test Queue Manager" 
   - Verify status shows queue processing is active

2. **Add Downloads:**
   - Tap "Test Download Worker" 3-4 times rapidly
   - Watch Work Queue Status panel

3. **Expected Results:**
   - ✅ Queue processing worker appears (download_queue_manager)
   - ✅ Max 3 AudioDownloadWorkers running concurrently  
   - ✅ Additional downloads remain ENQUEUED until slots available
   - ✅ Downloads process in priority order

### Scenario 2: Concurrency Limit Testing
**Objective:** Verify max 3 concurrent downloads enforced

1. **Setup:**
   - Ensure queue processing is active
   - Add 6+ test downloads rapidly

2. **Monitor:**
   - Check Work Queue Status: "Running" should never exceed 3
   - Additional downloads should show as "Enqueued"

3. **Expected Results:**
   - ✅ Never more than 3 RUNNING download workers
   - ✅ Queue processes additional downloads as slots become available

### Scenario 3: Worker Cleanup Testing
**Objective:** Verify orphaned worker cleanup

1. **Setup:**
   - Start several downloads
   - While downloads are running, tap "Cancel All Work"

2. **Wait:**
   - Wait for next queue processing cycle (up to 15 minutes)
   - Or tap "Test Queue Manager" to trigger immediate processing

3. **Expected Results:**
   - ✅ Cancelled workers are cleaned up
   - ✅ No orphaned workers remain in system

### Scenario 4: Queue Processing Lifecycle
**Objective:** Test start/stop functionality

1. **Start Processing:**
   - Tap "Test Queue Manager"
   - Verify periodic processing begins

2. **Stop Processing:**
   - Tap "Stop Queue Processing"
   - Verify periodic processing stops

3. **Expected Results:**
   - ✅ Queue processing worker appears/disappears correctly
   - ✅ Manual processing still works via download operations

## Verification Commands

### Check WorkManager Status
```bash
adb shell dumpsys jobscheduler | grep -A 10 -B 5 androidx.work
```

### Monitor Database State
```bash
adb shell "run-as com.deadarchive.app sqlite3 /data/data/com.deadarchive.app/databases/dead_archive_database '.tables'"
adb shell "run-as com.deadarchive.app sqlite3 /data/data/com.deadarchive.app/databases/dead_archive_database 'SELECT * FROM downloads ORDER BY created_timestamp DESC LIMIT 10;'"
```

### Check File System
```bash
adb shell "run-as com.deadarchive.app ls -la /data/data/com.deadarchive.app/files/Downloads/"
```

## Success Criteria

### ✅ Queue Manager Working Correctly When:
1. **Periodic Processing:** Queue worker runs every 15 minutes
2. **Concurrency Limits:** Never exceeds 3 concurrent downloads
3. **Priority Processing:** Downloads process in correct priority order
4. **Worker Cleanup:** Orphaned/cancelled workers are cleaned up
5. **Integration:** Works seamlessly with existing download infrastructure
6. **Resource Management:** Respects network and storage constraints

### ❌ Issues to Watch For:
1. **Memory Leaks:** Workers not properly cleaned up
2. **Resource Exhaustion:** Too many concurrent downloads
3. **Queue Starvation:** High priority downloads stuck behind low priority
4. **Worker Orphaning:** Cancelled downloads leaving running workers
5. **Integration Failures:** Queue management not triggered by repository operations

## Performance Metrics
- **Queue Processing Time:** Should complete within 30 seconds
- **Worker Startup Time:** AudioDownloadWorkers should start within 5 seconds
- **Memory Usage:** No significant increase during queue processing
- **Battery Impact:** Minimal battery drain from periodic processing