# In-App Auto Updater Implementation

## Overview

This document describes the in-app auto updater feature implemented for Dead Archive Android app. The feature provides automatic update checking, download, and installation capabilities directly within the app, eliminating the need for users to manually check for updates or navigate to external sources.

## User Experience

### Update Discovery
- Automatic update checking on app startup (configurable in Settings)
- Update notifications appear as overlays on any screen
- Non-intrusive design that doesn't interrupt the user's workflow

### Update Flow
1. **Update Available Dialog** appears when a new version is detected
2. User can choose to:
   - **Download**: Starts in-place download with progress indicators
   - **Skip This Version**: Permanently skips the current version
   - **Later**: Dismisses dialog, will show again on next app start

3. **Download Progress** shows real-time download status:
   - Progress bar with percentage
   - Downloaded/Total file size display
   - Download speed and error handling

4. **Installation** automatically triggers when download completes:
   - One-tap install button
   - Progress indicators during installation
   - Success/error feedback

### Settings Integration
- **Auto Update Check**: Toggle automatic startup checking
- **Manual Check**: Button to manually check for updates
- **Update History**: View current version and last check time

## Technical Implementation

### Architecture

The auto updater follows a service-oriented architecture with clear separation of concerns:

```
┌─────────────────────┐    ┌──────────────────────┐    ┌────────────────────┐
│   UI Components     │    │    ViewModels        │    │     Services       │
├─────────────────────┤    ├──────────────────────┤    ├────────────────────┤
│ UpdateAvailableDialog│◄──►│  SettingsViewModel   │◄──►│   UpdateService    │
│ SettingsScreen      │    │  BrowseViewModel     │    │ GlobalUpdateManager│
│ MainAppScreen       │    │                      │    │                    │
└─────────────────────┘    └──────────────────────┘    └────────────────────┘
```

### Core Components

#### 1. UpdateService (`core/data/service/UpdateService.kt`)
**Interface defining update operations:**
- `checkForUpdates()`: GitHub API integration for release checking
- `downloadUpdate()`: APK download with progress tracking
- `installUpdate()`: PackageInstaller integration
- `getDownloadProgress()`: Real-time download state flow
- `getInstallationStatus()`: Installation progress monitoring
- Version skipping and preference management

#### 2. GlobalUpdateManager (`core/data/service/GlobalUpdateManager.kt`)
**Cross-app state coordination:**
- Singleton service for sharing update status between components
- Manages update notifications from startup checks
- Prevents duplicate update dialogs across screens
- Clean state management with automatic cleanup

#### 3. UpdateAvailableDialog (`core/design/component/UpdateAvailableDialog.kt`)
**Reusable UI component:**
- Material3 design with progress indicators
- Conditional rendering based on update state
- Real-time progress updates during download/install
- Error handling with retry capabilities
- Success state with restart prompts

### State Management

The implementation uses StateFlow for reactive state management:

```kotlin
// Update detection state
val updateStatus: StateFlow<UpdateStatus?>
val currentUpdate: StateFlow<AppUpdate?>

// Download progress state  
val downloadState: StateFlow<UpdateDownloadState>
val installationStatus: StateFlow<UpdateInstallationState>
```

#### State Flow Details

**UpdateDownloadState:**
- `isDownloading: Boolean`
- `progress: Float` (0.0 to 1.0)
- `downloadedBytes: Long`
- `totalBytes: Long`
- `downloadedFile: String?`
- `error: String?`

**UpdateInstallationState:**
- `isInstalling: Boolean`
- `isSuccess: Boolean`
- `isError: Boolean`
- `statusMessage: String?`
- `errorMessage: String?`

### Integration Points

#### Application Startup
```kotlin
// DeadArchiveApplication.onCreate()
if (settings?.autoUpdateCheckEnabled == true) {
    val result = updateService.checkForUpdates()
    result.fold(
        onSuccess = { status ->
            if (status.isUpdateAvailable && !status.isSkipped) {
                globalUpdateManager.setUpdateStatus(status)
            }
        }
    )
}
```

#### UI Integration
```kotlin
// MainAppScreen.kt
UpdateAvailableDialog(
    update = update,
    downloadState = downloadState,
    installationStatus = installationStatus,
    onDownload = { settingsViewModel.downloadUpdate() },
    onInstall = { settingsViewModel.installUpdate() },
    onSkip = { settingsViewModel.skipUpdate() },
    onDismiss = { settingsViewModel.clearUpdateState() }
)
```

## GitHub Integration

### Release Detection
- Queries GitHub API: `https://api.github.com/repos/ds17f/dead/releases/latest`
- Compares current app version with latest release tag
- Respects version skipping preferences
- Handles rate limiting and network errors

### APK Download
- Downloads APK assets from GitHub release attachments
- Supports resume-interrupted downloads
- Progress tracking with chunked reading
- File integrity verification

### Security Considerations
- APK signature verification (handled by Android PackageInstaller)
- Network traffic over HTTPS only
- No automatic installation without user consent
- Temporary file cleanup after installation

## Configuration

### Settings Options
```kotlin
// Auto update checking (default: enabled)
autoUpdateCheckEnabled: Boolean

// Skipped versions (persisted)
skippedVersions: Set<String>

// Last update check timestamp
lastUpdateCheck: Long
```

### Build Configuration
```kotlin
// app/build.gradle.kts
buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
buildConfigField("String", "VERSION_CODE", "\"${versionCode}\"")
buildConfigField("String", "BUILD_TYPE", "\"${buildType.name}\"")
```

## Error Handling

### Network Errors
- Graceful handling of connectivity issues
- Retry mechanisms with exponential backoff
- User-friendly error messages
- Offline mode detection

### Download Errors
- Partial download recovery
- Storage space validation
- Permission handling
- Corrupt file detection

### Installation Errors
- PackageInstaller error mapping
- Permission requirement prompts
- Rollback capabilities
- Clear error messaging

## Limitations & Known Issues

### Current Limitations
1. **Single Update Source**: Only supports GitHub releases
2. **Manual Restart**: Requires app restart after installation
3. **Storage Dependency**: Requires external storage access
4. **Network Dependency**: No offline update capabilities

### Future Improvements
1. **Multiple Update Channels**: Support beta/alpha release tracks
2. **Delta Updates**: Incremental updates for smaller downloads
3. **Background Downloads**: Download during app usage
4. **Hot Reload**: In-app restart without full app restart

## Testing Strategy

### Manual Testing
```bash
# Test update flow
make build && make install-quiet

# Simulate update availability
# (Modify version code in build.gradle.kts to be lower than latest release)

# Test download interruption
# (Disable network during download, re-enable to test resume)

# Test installation failure
# (Test with insufficient storage or permission issues)
```

### Integration Testing
- Mock GitHub API responses for different scenarios
- Simulate network conditions (slow, interrupted, offline)
- Test state transitions and UI updates
- Verify cleanup and error recovery

## Deployment Considerations

### Release Process
1. Ensure GitHub release includes APK asset
2. Tag format must match version name pattern
3. Release notes are included in GitHub release
4. APK must be signed with same certificate

### Monitoring
- Track update adoption rates
- Monitor download success/failure rates
- Log installation issues
- Track user skip behavior

## Implementation Notes

### Why Not Service-Oriented Design?
This implementation was developed as a rapid feature delivery focused on getting the functionality working. The architecture mixes ViewModel logic with service calls rather than following pure service-oriented patterns used elsewhere in the codebase.

**Current Pattern:**
```kotlin
// SettingsViewModel handles business logic directly
fun downloadUpdate() {
    viewModelScope.launch {
        updateService.downloadUpdate(update)
    }
}
```

**Ideal Service Pattern:**
```kotlin
// Dedicated UpdateManagementService would handle coordination
class UpdateManagementService {
    fun downloadAndInstall(update: AppUpdate, callbacks: UpdateCallbacks)
}
```

### Future Refactoring
For v2 architecture consideration:
- Extract UpdateManagementService for business logic
- Separate UpdateProgressService for state management  
- Create UpdateNotificationService for user communications
- Implement proper dependency injection patterns

This approach would align with the service-oriented architecture used in other features like player management and library operations.