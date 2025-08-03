# QrScannerV2 Architecture Specification

## Overview

This document defines the V2 architecture patterns for QrScannerV2, following the established clean architecture principles demonstrated by SearchV2, LibraryV2, and PlayerV2 implementations.

**Architecture Type**: V2 UI-first with service composition  
**Design Pattern**: Clean Architecture with Material3 camera integration  
**State Management**: Reactive StateFlow with camera and scanning state  
**Service Strategy**: Stub-first development with mock QR detection

## Component Hierarchy

### 1. Top-Level Architecture

```
QrScannerV2Screen
├── QrScannerV2ViewModel (State Coordination)
│   └── QrScannerV2Service (Clean Service Interface)
│       └── QrScannerV2ServiceStub (Mock QR Detection)
├── UI Components (Camera Interface)
│   ├── QrScannerV2CameraPreview (CameraX integration)
│   ├── QrScannerV2ScanOverlay (Scanning target UI)
│   ├── QrScannerV2TopBar (Close button and status)
│   └── QrScannerV2BottomPanel (Instructions and feedback)
├── Camera Components (Hardware Integration)
│   ├── CameraPermissionHandler (Permission management)
│   ├── QrDetectionProcessor (ZXing integration)
│   └── CameraLifecycleManager (CameraX lifecycle)
├── Data Models (Domain Layer)
│   ├── QrScannerV2UiState (Comprehensive state model)
│   ├── QrScanResult (Scan result with metadata)
│   ├── ScannerStatus (Scanning, processing, success, error states)
│   └── PermissionState (Camera permission management)
└── Infrastructure (System Layer)
    ├── Camera Permission Management
    ├── Navigation Integration with SearchV2
    └── URL Processing with ArchiveUrlUtil
```

## Service Layer Architecture

### QrScannerV2Service Interface

```kotlin
/**
 * Clean API interface for QR scanning operations.
 * Follows V2 architecture pattern with reactive flows and Result types.
 */
interface QrScannerV2Service {
    // Reactive state flows
    val scannerStatus: Flow<ScannerStatus>
    val lastScanResult: Flow<QrScanResult?>
    val permissionState: Flow<PermissionState>
    
    // Scanning operations with Result types
    suspend fun startScanning(): Result<Unit>
    suspend fun stopScanning(): Result<Unit>
    suspend fun processQrCode(rawContent: String): Result<QrScanResult>
    suspend fun validateArchiveUrl(url: String): Result<ArchiveUrlInfo>
    suspend fun requestCameraPermission(): Result<PermissionState>
    
    // Stub-specific method for UI development
    suspend fun simulateQrScan(mockUrl: String): Result<QrScanResult>
}
```

### QrScannerV2ServiceStub Implementation

**Key Features**:
- **Realistic mock QR detection** with simulated camera scanning
- **Archive.org URL validation** using existing ArchiveUrlUtil patterns
- **Permission simulation** for development without camera hardware
- **Smart URL categorization** (recordings, shows, tracks, invalid)
- **Scanning delay simulation** to mimic real camera detection timing

**Mock Data Examples**:
```kotlin
private val mockQrUrls = listOf(
    "https://archive.org/details/gd1977-05-08.sbd.hicks.4982.sbeok.shnf", // Cornell
    "https://archive.org/details/gd72-05-03.sbd.unknown.30057.sbeok.shnf", // Europe '72
    "deadarchive://recording/gd1977-05-08.sbd.hicks.4982.sbeok.shnf", // App deep link
    "https://invalid-qr-content.com", // Error case
)
```

## Camera Integration Architecture

### CameraX + ZXing Integration

```kotlin
@Composable
fun QrScannerV2CameraPreview(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // CameraX setup with QR detection
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                // Configure CameraX with QR detection analyzer
                setupQrDetection(onQrDetected)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
```

### Permission Management

```kotlin
/**
 * Handle camera permissions with educational dialogs
 */
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(permissionState.status) {
        when {
            permissionState.status.isGranted -> onPermissionGranted()
            permissionState.status.shouldShowRationale -> {
                // Show educational dialog
            }
            else -> onPermissionDenied()
        }
    }
}
```

## Data Flow Architecture

### State Management

```kotlin
@HiltViewModel
class QrScannerV2ViewModel @Inject constructor(
    @Named("stub") private val qrScannerV2Service: QrScannerV2Service
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QrScannerV2UiState())
    val uiState: StateFlow<QrScannerV2UiState> = _uiState.asStateFlow()
    
    // Reactive flow observation
    private fun observeServiceFlows() {
        viewModelScope.launch {
            qrScannerV2Service.scannerStatus.collect { status ->
                _uiState.value = _uiState.value.copy(scannerStatus = status)
            }
        }
        // ... other flow observations
    }
}
```

### Domain Models

```kotlin
data class QrScannerV2UiState(
    val scannerStatus: ScannerStatus = ScannerStatus.INITIALIZING,
    val permissionState: PermissionState = PermissionState.UNKNOWN,
    val lastScanResult: QrScanResult? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
)

data class QrScanResult(
    val rawContent: String,
    val urlType: ArchiveUrlType,
    val archiveInfo: ArchiveUrlInfo?,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ScannerStatus {
    INITIALIZING,
    SCANNING,
    PROCESSING,
    SUCCESS,
    ERROR,
    STOPPED
}

enum class ArchiveUrlType {
    RECORDING,
    SHOW,
    TRACK,
    APP_DEEP_LINK,
    INVALID
}
```

## Navigation Architecture

### Integration with SearchV2

```kotlin
// SearchV2Screen camera icon handler
SearchV2TopBar(onCameraClick = {
    // Navigate to QR scanner
    navController.navigate("qr_scanner_v2")
})

// Navigation graph addition
composable("qr_scanner_v2") {
    QrScannerV2Screen(
        onNavigateBack = { navController.popBackStack() },
        onScanSuccess = { scanResult ->
            // Route based on scan result type
            when (scanResult.urlType) {
                ArchiveUrlType.RECORDING -> navController.navigate("player/${scanResult.archiveInfo?.identifier}")
                ArchiveUrlType.SHOW -> navController.navigate("show_details/${scanResult.archiveInfo?.identifier}")
                // ... other navigation cases
            }
        }
    )
}
```

## URL Processing Architecture

### Archive.org URL Handling

```kotlin
/**
 * Process scanned URLs using existing ArchiveUrlUtil patterns
 */
suspend fun processArchiveUrl(url: String): Result<ArchiveUrlInfo> {
    return try {
        val archiveInfo = when {
            url.contains("/details/") -> {
                val identifier = extractIdentifier(url)
                val filename = extractFilename(url)
                ArchiveUrlInfo(identifier, filename, ArchiveUrlType.RECORDING)
            }
            url.startsWith("deadarchive://") -> {
                processAppDeepLink(url)
            }
            else -> return Result.failure(InvalidUrlException("Unsupported URL format"))
        }
        Result.success(archiveInfo)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Future Service Integration

### Show/Recording Service Dependencies

When show/recording services are reworked, QrScannerV2 will integrate with:

```kotlin
// Future service composition
class QrScannerV2ServiceImpl @Inject constructor(
    private val archiveUrlService: ArchiveUrlService,        // Enhanced URL processing
    private val showService: ShowService,                    // Show lookup and validation
    private val recordingService: RecordingService,          // Recording metadata
    private val deepLinkService: DeepLinkService,           // App URL handling
    private val navigationService: NavigationService        // Smart routing
) : QrScannerV2Service {
    
    override suspend fun processQrCode(rawContent: String): Result<QrScanResult> {
        // Leverage enhanced service architecture
        val urlInfo = archiveUrlService.parseUrl(rawContent)
        val showInfo = showService.validateShow(urlInfo.identifier)
        // ... comprehensive processing
    }
}
```

## Performance Considerations

### Camera Lifecycle

- **Efficient Camera Usage**: CameraX lifecycle tied to screen visibility
- **Battery Optimization**: Stop scanning when app backgrounded
- **Memory Management**: Proper camera resource cleanup

### QR Detection Optimization

- **Scan Throttling**: Limit detection frequency to prevent duplicate scans
- **Image Processing**: Optimize ZXing analysis for performance
- **UI Responsiveness**: Offload processing to background threads

## Architecture Validation

### V2 Pattern Compliance

**✅ Clean Architecture**: Separated presentation, domain, and infrastructure layers  
**✅ Service Abstraction**: Clean interfaces with stub implementations  
**✅ Reactive State**: StateFlow-based reactive UI updates  
**✅ Dependency Injection**: Hilt-based service composition  
**✅ Material3 Integration**: Consistent design system usage  
**✅ Error Handling**: Proper Result types and user feedback  

---

**Architecture Status**: ✅ **Complete V2 Architecture Specification**  
**Implementation Readiness**: Awaiting show/recording service rework  
**Service Dependencies**: ArchiveUrlUtil, enhanced show/recording services  
**Created**: January 2025

QrScannerV2 architecture demonstrates V2 pattern maturity while defining clear integration points for future enhanced service architecture.