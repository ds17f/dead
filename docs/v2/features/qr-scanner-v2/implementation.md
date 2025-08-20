# QrScannerV2 Technical Implementation Guide

## Overview

This document provides comprehensive technical implementation guidance for QrScannerV2, following V2 architecture patterns and defining the integration strategy for future development.

**Implementation Status**: 📋 **Documented for Future Implementation**  
**Architecture Pattern**: V2 UI-first development with camera integration  
**Dependencies**: CameraX, ZXing Android Embedded, enhanced show/recording services  
**Implementation Timeline**: After show/recording service rework

## Technical Dependencies

### Required Library Additions

**Feature Browse Module** (`feature/browse/build.gradle.kts`):
```kotlin
dependencies {
    // Camera functionality
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // QR code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3") // Move from library module or add here
    
    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Haptic feedback
    implementation("androidx.compose.ui:ui-hapticfeedback")
}
```

### Manifest Permissions

**AndroidManifest.xml**:
```xml
<!-- Camera permission for QR scanning -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Optional: Vibration for scan feedback -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- Camera feature requirements -->
<uses-feature 
    android:name="android.hardware.camera" 
    android:required="true" />
<uses-feature 
    android:name="android.hardware.camera.autofocus" 
    android:required="false" />
```

## File Structure & Organization

### Core Implementation Files

```
feature/browse/src/main/java/com/deadly/feature/browse/
├── QrScannerV2Screen.kt                 # Main camera scanning UI
├── QrScannerV2ViewModel.kt              # State coordination and service integration
├── component/
│   ├── QrScannerV2CameraPreview.kt      # CameraX integration component
│   ├── QrScannerV2ScanOverlay.kt        # Scanning target and feedback UI
│   ├── QrScannerV2TopBar.kt             # Scanner header with close button
│   └── QrScannerV2BottomPanel.kt        # Instructions and status display
└── navigation/BrowseNavigation.kt        # Updated with scanner route
```

### Service Layer Files

```
core/qr-scanner-api/src/main/java/com/deadly/core/qr/api/
└── QrScannerV2Service.kt                # Clean service interface

core/qr-scanner/src/main/java/com/deadly/core/qr/
├── service/QrScannerV2ServiceStub.kt     # Mock implementation with realistic behavior
├── di/QrScannerV2StubModule.kt          # Hilt dependency injection
└── model/                               # Domain models (if not in core/model)
    ├── QrScanResult.kt
    ├── ScannerStatus.kt
    └── ArchiveUrlInfo.kt
```

### Documentation Files

```
docs/v2/features/qr-scanner-v2/
├── overview.md                          # ✅ Complete
├── architecture.md                      # ✅ Complete  
├── implementation.md                    # ✅ This document
└── sharing-strategy.md                  # Future sharing integration
```

## Implementation Phases

### Phase 1: Core Infrastructure

**Dependencies and Permissions**:
- Add CameraX and ZXing dependencies to `:feature:browse`
- Add camera permissions to AndroidManifest.xml
- Create core module structure for QR scanner services

**Service Foundation**:
```kotlin
interface QrScannerV2Service {
    val scannerStatus: Flow<ScannerStatus>
    val lastScanResult: Flow<QrScanResult?>
    val permissionState: Flow<PermissionState>
    
    suspend fun startScanning(): Result<Unit>
    suspend fun stopScanning(): Result<Unit>
    suspend fun processQrCode(rawContent: String): Result<QrScanResult>
    suspend fun simulateQrScan(mockUrl: String): Result<Unit>
}
```

### Phase 2: Camera Integration

**CameraX Setup**:
```kotlin
@Composable
fun QrScannerV2CameraPreview(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        
        // Configure camera with QR analysis
        val preview = Preview.Builder().build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    QrCodeAnalyzer(onQrDetected)
                )
            }
        
        // Bind camera lifecycle
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("QrScannerV2", "Camera binding failed", exc)
        }
        
        onDispose {
            cameraProvider.unbindAll()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { preview.setSurfaceProvider(it.surfaceProvider) }
        },
        modifier = modifier.fillMaxSize()
    )
}
```

**QR Detection Processing**:
```kotlin
class QrCodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)))
    }
    
    override fun analyze(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        
        val source = PlanarYUVLuminanceSource(
            data,
            imageProxy.width,
            imageProxy.height,
            0, 0,
            imageProxy.width,
            imageProxy.height,
            false
        )
        
        try {
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            onQrDetected(result.text)
        } catch (e: Exception) {
            // No QR code found, continue scanning
        } finally {
            imageProxy.close()
        }
    }
}
```

### Phase 3: UI Components

**Main Scanner Screen**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerV2Screen(
    onNavigateBack: () -> Unit,
    onScanSuccess: (QrScanResult) -> Unit,
    viewModel: QrScannerV2ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        QrScannerV2CameraPreview(
            onQrDetected = viewModel::onQrDetected,
            modifier = Modifier.fillMaxSize()
        )
        
        // Scanning overlay
        QrScannerV2ScanOverlay(
            scannerStatus = uiState.scannerStatus,
            modifier = Modifier.fillMaxSize()
        )
        
        // Top bar
        QrScannerV2TopBar(
            onCloseClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Bottom instructions
        QrScannerV2BottomPanel(
            scannerStatus = uiState.scannerStatus,
            instructions = "Point camera at QR code",
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Handle scan success
    LaunchedEffect(uiState.lastScanResult) {
        uiState.lastScanResult?.let { result ->
            onScanSuccess(result)
        }
    }
}
```

**Scanning Overlay Component**:
```kotlin
@Composable
fun QrScannerV2ScanOverlay(
    scannerStatus: ScannerStatus,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val scanSize = 250.dp.toPx()
        
        // Draw scanning frame
        val cornerLength = 20.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        
        val frameColor = when (scannerStatus) {
            ScannerStatus.SCANNING -> Color.White
            ScannerStatus.PROCESSING -> Color.Yellow
            ScannerStatus.SUCCESS -> Color.Green
            ScannerStatus.ERROR -> Color.Red
            else -> Color.White.copy(alpha = 0.5f)
        }
        
        // Draw corner brackets
        drawPath(
            path = Path().apply {
                // Top-left corner
                moveTo(centerX - scanSize/2, centerY - scanSize/2 + cornerLength)
                lineTo(centerX - scanSize/2, centerY - scanSize/2)
                lineTo(centerX - scanSize/2 + cornerLength, centerY - scanSize/2)
                
                // Top-right corner
                moveTo(centerX + scanSize/2 - cornerLength, centerY - scanSize/2)
                lineTo(centerX + scanSize/2, centerY - scanSize/2)
                lineTo(centerX + scanSize/2, centerY - scanSize/2 + cornerLength)
                
                // Bottom-right corner
                moveTo(centerX + scanSize/2, centerY + scanSize/2 - cornerLength)
                lineTo(centerX + scanSize/2, centerY + scanSize/2)
                lineTo(centerX + scanSize/2 - cornerLength, centerY + scanSize/2)
                
                // Bottom-left corner
                moveTo(centerX - scanSize/2 + cornerLength, centerY + scanSize/2)
                lineTo(centerX - scanSize/2, centerY + scanSize/2)
                lineTo(centerX - scanSize/2, centerY + scanSize/2 - cornerLength)
            },
            color = frameColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
```

### Phase 4: Service Implementation

**Stub Service with Realistic Behavior**:
```kotlin
@Singleton
class QrScannerV2ServiceStub @Inject constructor() : QrScannerV2Service {
    
    private val _scannerStatus = MutableStateFlow(ScannerStatus.INITIALIZING)
    private val _lastScanResult = MutableStateFlow<QrScanResult?>(null)
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    
    override val scannerStatus: Flow<ScannerStatus> = _scannerStatus.asStateFlow()
    override val lastScanResult: Flow<QrScanResult?> = _lastScanResult.asStateFlow()
    override val permissionState: Flow<PermissionState> = _permissionState.asStateFlow()
    
    override suspend fun processQrCode(rawContent: String): Result<QrScanResult> {
        return try {
            _scannerStatus.value = ScannerStatus.PROCESSING
            delay(500) // Simulate processing time
            
            val urlType = determineUrlType(rawContent)
            val archiveInfo = if (urlType != ArchiveUrlType.INVALID) {
                parseArchiveUrl(rawContent)
            } else null
            
            val result = QrScanResult(
                rawContent = rawContent,
                urlType = urlType,
                archiveInfo = archiveInfo
            )
            
            _lastScanResult.value = result
            _scannerStatus.value = ScannerStatus.SUCCESS
            
            Result.success(result)
        } catch (e: Exception) {
            _scannerStatus.value = ScannerStatus.ERROR
            Result.failure(e)
        }
    }
    
    private fun determineUrlType(url: String): ArchiveUrlType {
        return when {
            url.contains("archive.org/details/") -> ArchiveUrlType.RECORDING
            url.startsWith("deadly://") -> ArchiveUrlType.APP_DEEP_LINK
            else -> ArchiveUrlType.INVALID
        }
    }
}
```

## URL Processing Integration

### Archive.org URL Parsing

```kotlin
/**
 * Enhanced URL processing using existing ArchiveUrlUtil patterns
 */
fun parseArchiveUrl(url: String): ArchiveUrlInfo {
    return when {
        url.contains("/details/") -> {
            val identifier = url.substringAfter("/details/").substringBefore("/")
            val filename = if (url.contains("/$identifier/")) {
                url.substringAfter("/$identifier/").substringBefore("?")
            } else null
            
            ArchiveUrlInfo(
                identifier = identifier,
                filename = filename,
                urlType = if (filename != null) ArchiveUrlType.TRACK else ArchiveUrlType.RECORDING
            )
        }
        url.startsWith("deadly://") -> {
            parseAppDeepLink(url)
        }
        else -> throw InvalidUrlException("Unsupported URL format: $url")
    }
}
```

### Navigation Integration

```kotlin
// Update BrowseNavigation.kt
fun NavGraphBuilder.qrScannerRoute(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit
) {
    composable("qr_scanner_v2") {
        QrScannerV2Screen(
            onNavigateBack = onNavigateBack,
            onScanSuccess = { scanResult ->
                when (scanResult.urlType) {
                    ArchiveUrlType.RECORDING -> {
                        onNavigateToPlayer(scanResult.archiveInfo?.identifier ?: "")
                    }
                    ArchiveUrlType.SHOW -> {
                        // Navigate to show details when show service is enhanced
                    }
                    ArchiveUrlType.TRACK -> {
                        // Navigate to specific track when supported
                    }
                    else -> {
                        // Show error message
                    }
                }
            }
        )
    }
}
```

## Testing Strategy

### Unit Testing

```kotlin
class QrScannerV2ServiceStubTest {
    
    @Test
    fun `processQrCode with valid Archive URL returns success`() = runTest {
        val service = QrScannerV2ServiceStub()
        val url = "https://archive.org/details/gd1977-05-08.sbd.hicks.4982.sbeok.shnf"
        
        val result = service.processQrCode(url)
        
        assertTrue(result.isSuccess)
        assertEquals(ArchiveUrlType.RECORDING, result.getOrNull()?.urlType)
    }
    
    @Test
    fun `processQrCode with invalid URL returns error`() = runTest {
        val service = QrScannerV2ServiceStub()
        val url = "https://invalid-url.com"
        
        val result = service.processQrCode(url)
        
        assertEquals(ArchiveUrlType.INVALID, result.getOrNull()?.urlType)
    }
}
```

### Integration Testing

```kotlin
@HiltAndroidTest
class QrScannerV2ScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun qrScannerScreen_displaysCamera_andHandlesPermissions() {
        // Test camera permission flow
        // Test scanning UI elements
        // Test navigation behavior
    }
}
```

## Performance Considerations

### Camera Resource Management

- **Lifecycle Integration**: Bind camera to Compose lifecycle
- **Background Handling**: Stop camera when app backgrounded
- **Memory Optimization**: Efficient image processing and cleanup

### QR Detection Optimization

- **Scan Throttling**: Prevent duplicate rapid scans
- **Image Resolution**: Balance quality vs performance
- **Background Processing**: Keep UI responsive during analysis

---

**Implementation Status**: ✅ **Complete Technical Specification**  
**Dependencies**: CameraX, ZXing, enhanced show/recording services  
**Implementation Priority**: After service architecture rework  
**Created**: January 2025

QrScannerV2 implementation guide provides comprehensive technical foundation for future development following established V2 architecture patterns.