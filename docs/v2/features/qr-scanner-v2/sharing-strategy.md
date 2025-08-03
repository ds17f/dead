# QrScannerV2 Sharing Strategy

## Strategic Vision

QrScannerV2 represents the foundation for a comprehensive content sharing ecosystem within Dead Archive, enabling seamless discovery and sharing of Grateful Dead recordings through both QR code scanning and generation.

**Current Scope**: Archive.org URL scanning and validation  
**Future Vision**: Bidirectional sharing with app-generated QR codes and deep linking  
**Integration Dependencies**: Enhanced show/recording service architecture

## Sharing Architecture Evolution

### Phase 1: URL Consumption (Immediate)
**Capability**: Scan existing Archive.org QR codes shared externally
- Parse and validate Archive.org URLs from QR codes
- Navigate to appropriate content within Dead Archive
- Handle various URL formats (recordings, shows, tracks)
- Provide fallback for invalid or unsupported URLs

### Phase 2: App Content Sharing (Post-Service Rework)
**Capability**: Generate QR codes for app content
- Create QR codes for recordings, shows, playlists within the app
- Generate app-specific deep links with Archive.org fallbacks
- Integrate with enhanced show/recording services for rich metadata
- Support cross-platform sharing with intelligent routing

### Phase 3: Community Sharing Ecosystem (Future)
**Capability**: Advanced sharing features for community building
- User-generated content collections and recommendations
- Social sharing with app-specific metadata
- Tracking and analytics for shared content discovery
- Integration with potential user accounts and preferences

## Technical Sharing Framework

### URL Schema Design

**Archive.org URLs** (Current):
```
https://archive.org/details/{identifier}           # Recording/Show
https://archive.org/details/{identifier}/{filename} # Specific track
```

**App Deep Links** (Future):
```
deadarchive://recording/{identifier}                # App-specific recording
deadarchive://show/{show-id}                       # Enhanced show entity
deadarchive://playlist/{playlist-id}               # User playlists
deadarchive://collection/{collection-id}           # Content collections
```

**Hybrid URLs** (Advanced):
```
https://deadarchive.app/share/recording/{identifier}?fallback=archive.org
```

### QR Code Generation Strategy

**Archive.org Fallback Pattern**:
```kotlin
fun generateShareableUrl(content: ShareableContent): String {
    return when (content) {
        is Recording -> {
            // Primary: App deep link with Archive.org fallback
            val appUrl = "deadarchive://recording/${content.identifier}"
            val fallbackUrl = ArchiveUrlUtil.getRecordingUrl(content)
            createHybridUrl(appUrl, fallbackUrl)
        }
        is Show -> {
            // Enhanced show sharing when service architecture supports it
            generateShowShareUrl(content)
        }
        is Playlist -> {
            // User-generated playlists with track lists
            generatePlaylistShareUrl(content)
        }
    }
}
```

### Sharing Service Architecture

**Enhanced Service Integration** (Future):
```kotlin
interface SharingV2Service {
    // QR code generation
    suspend fun generateQrCode(content: ShareableContent): Result<QrCodeData>
    suspend fun generateShareUrl(content: ShareableContent): Result<String>
    
    // Deep link handling
    suspend fun parseDeepLink(url: String): Result<DeepLinkInfo>
    suspend fun resolveContent(deepLink: DeepLinkInfo): Result<ShareableContent>
    
    // Analytics and tracking
    suspend fun trackShare(content: ShareableContent, method: ShareMethod): Result<Unit>
    suspend fun trackScan(url: String, source: ScanSource): Result<Unit>
}
```

## Content Sharing Capabilities

### Recording Sharing

**Current**: Archive.org URL sharing via existing `QrCodeBottomSheet`
**Future Enhanced**:
```kotlin
data class RecordingShareData(
    val recording: Recording,
    val appDeepLink: String,
    val archiveFallback: String,
    val shareMetadata: ShareMetadata
)

data class ShareMetadata(
    val title: String,                    // "Cornell 5/8/77 - Scarlet Begonias"
    val subtitle: String,                 // "Barton Hall, Ithaca NY"
    val description: String,              // Rich show context
    val thumbnailUrl: String?,            // Album art if available
    val sharingUser: String?,             // Attribution if user accounts exist
    val shareTimestamp: Long
)
```

### Show Sharing

**Future Capability**: Enhanced show entities with comprehensive metadata
```kotlin
data class ShowShareData(
    val show: EnhancedShow,
    val recommendedRecording: Recording,   // Best quality/preferred recording
    val alternativeRecordings: List<Recording>,
    val setlistInfo: SetlistInfo?,
    val venueInfo: VenueInfo?,
    val shareContext: ShareContext
)
```

### Playlist Sharing

**Advanced Feature**: User-generated content sharing
```kotlin
data class PlaylistShareData(
    val playlist: UserPlaylist,
    val tracks: List<PlaylistTrack>,
    val creator: UserInfo?,
    val isPublic: Boolean,
    val sharePermissions: SharePermissions
)
```

## Cross-Platform Sharing Strategy

### Mobile-to-Mobile

**QR Code Scanning**: 
- Direct app-to-app sharing via QR codes
- Instant content discovery and playback
- Offline sharing capability (QR codes work without internet)

**Share Sheet Integration**:
- Native iOS/Android sharing with deep links
- Fallback to Archive.org URLs for non-app users
- Rich preview metadata for social platforms

### Web-to-Mobile

**URL Scanning**:
- Scan Archive.org URLs from browser, social media, forums
- Parse shared content from web platforms
- Bridge between web discovery and mobile playback

### Mobile-to-Web

**Universal URLs**:
- Generate URLs that work in both app and browser
- Intelligent routing based on platform detection
- Preserve user intent across platform boundaries

## Community Integration Strategy

### Discovery Enhancement

**Shared Content Analytics**:
- Track popular shared recordings for discovery
- Identify trending content within the community
- Surface frequently shared shows in browse sections

**Social Features** (Future):
- User-generated recommendations via QR sharing
- Community playlists and collections
- Attribution and credit for content discovery

### Privacy and Permissions

**Sharing Controls**:
- User preferences for sharing analytics
- Opt-in tracking for community features
- Privacy-preserving sharing options

**Content Attribution**:
- Respect for Archive.org source attribution
- Recognition of taper and transferer credits
- Community contribution acknowledgments

## Integration with Service Rework

### Show/Recording Service Dependencies

The sharing strategy depends on enhanced service architecture:

**Enhanced Show Service**:
- Rich show metadata with venue, setlist, historical context
- Multiple recording options with quality recommendations
- Relationship mapping between shows, tours, and eras

**Enhanced Recording Service**:
- Comprehensive track metadata and timing information
- Quality assessments and source information
- Alternative format availability (SBD, AUD, different transferers)

**New Playlist Service**:
- User-generated content curation
- Cross-recording playlist support
- Sharing permissions and privacy controls

### URL Resolution Service

**Intelligent URL Processing**:
```kotlin
interface UrlResolutionService {
    suspend fun resolveArchiveUrl(url: String): Result<ArchiveContent>
    suspend fun resolveAppDeepLink(url: String): Result<AppContent>
    suspend fun generateUniversalUrl(content: Content): Result<UniversalUrl>
    suspend fun createQrShareData(content: Content): Result<QrShareData>
}
```

## Implementation Roadmap

### Near-Term (Post-QrScannerV2)
1. **Enhanced QR Generation**: Improve existing `QrCodeBottomSheet` with sharing metadata
2. **URL Resolution**: Expand `ArchiveUrlUtil` with comprehensive URL parsing
3. **Basic Analytics**: Track QR generation and scanning for usage insights

### Medium-Term (Post-Service Rework)
1. **Deep Link System**: Implement app-specific URL schema
2. **Hybrid URLs**: Create URLs that work across platforms
3. **Share Service**: Unified sharing service with QR integration

### Long-Term (Community Features)
1. **User Playlists**: Shareable user-generated content
2. **Social Discovery**: Community-driven content recommendations
3. **Advanced Analytics**: Comprehensive sharing and discovery metrics

---

**Strategy Status**: ✅ **Complete Vision and Technical Framework**  
**Implementation Dependencies**: Enhanced show/recording service architecture  
**Community Impact**: Foundation for Dead Archive content sharing ecosystem  
**Created**: January 2025

The QrScannerV2 sharing strategy establishes the foundation for a comprehensive content sharing ecosystem that enhances community discovery while respecting the open nature of Archive.org and the Grateful Dead community.