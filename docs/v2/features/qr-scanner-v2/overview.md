# QrScannerV2 Overview

## Feature Vision

QrScannerV2 represents the fourth major V2 architecture implementation, providing seamless QR code scanning capabilities that integrate with the Dead Archive ecosystem for both consuming Archive.org content and sharing app-generated links.

**Current Status**: 📋 **Documented for Future Implementation**  
**Implementation Priority**: After show/recording service rework  
**Architecture Pattern**: V2 stub-first development with service composition

## User Experience

### Primary Use Cases

**Archive.org QR Scanning**:
- Scan QR codes containing Archive.org URLs shared by other users
- Instantly navigate to recordings, shows, or specific tracks
- Support for both browser-generated and app-generated Archive URLs
- Smart navigation based on URL content type

**App Sharing Integration** (Future):
- Generate QR codes for recordings, shows, playlists within the app
- Share app-specific deep links that open directly in Dead Archive
- Cross-platform sharing with fallback to Archive.org URLs
- Integration with revised show/recording service architecture

### User Flow

1. **Access**: Tap camera icon in SearchV2Screen header
2. **Scan**: Camera opens with Material3 scanning overlay
3. **Detection**: QR code detected with haptic feedback and visual confirmation
4. **Processing**: URL parsed and validated using ArchiveUrlUtil patterns
5. **Navigation**: Smart routing to appropriate screen based on content type
6. **Fallback**: Clear error messages for invalid or unsupported QR codes

## Integration Points

### Current Architecture Dependencies
- **SearchV2Screen**: Camera icon launches QR scanner
- **ArchiveUrlUtil**: URL parsing and validation
- **Navigation System**: Route to player, browse, or show screens based on scanned content
- **Permissions**: Camera access with educational prompts

### Future Architecture Dependencies
- **Revised Show/Recording Services**: Enhanced URL generation and deep linking
- **App Sharing System**: QR generation for internal content
- **Deep Link Handling**: App-specific URL schemes and routing

## Technical Foundation

### Existing Infrastructure
- ✅ **ZXing Library**: Already included in `:feature:library` module
- ✅ **QR Generation**: `QrCodeBottomSheet.kt` demonstrates QR creation patterns
- ✅ **URL Handling**: `ArchiveUrlUtil` provides Archive.org URL processing
- ✅ **Material3 Design**: Consistent with SearchV2 design patterns

### Required Additions
- **CameraX Integration**: Modern camera handling for Android
- **ZXing Android Embedded**: QR detection capabilities
- **Permission Management**: Camera access with user education
- **Scanner UI Components**: Material3 camera overlay and feedback

## Strategic Value

### Immediate Benefits
- **Enhanced User Experience**: Seamless content discovery through QR scanning
- **Community Integration**: Easy sharing and discovery of Grateful Dead recordings
- **Cross-Platform Bridge**: Connect web-shared content to mobile app experience

### Long-Term Vision
- **App Ecosystem**: Internal sharing system with QR generation
- **Service Integration**: Leverage improved show/recording architecture
- **Community Features**: User-generated content sharing and discovery
- **Analytics Integration**: Track QR sharing and discovery patterns

## Implementation Readiness

### Prerequisites
1. **Show/Recording Service Rework**: Enhanced URL generation and handling
2. **Deep Link Strategy**: App-specific URL schemes and routing
3. **Sharing Architecture**: Comprehensive content sharing system design

### V2 Architecture Alignment
- **Stub-First Development**: QrScannerV2ServiceStub with realistic mock scanning
- **UI-First Design**: Build camera interface first, discover service requirements
- **Clean Service Layer**: Abstract QR detection, URL parsing, and navigation logic
- **Feature Flag Ready**: Named injection for safe deployment and testing
- **Material3 Integration**: Consistent design patterns with SearchV2

---

**Documentation Status**: ✅ **Complete Design Specification**  
**Next Steps**: Await show/recording service rework completion  
**Implementation Timeline**: TBD based on service architecture progress  
**Created**: January 2025

QrScannerV2 documentation preserves the complete design vision while acknowledging current implementation dependencies and strategic priorities.