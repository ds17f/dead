# Enhanced Playlist Features - Implementation Plan

## Overview

This document outlines the plan for three major enhancements to the playlist/player screen:

1. **Interactive Star Ratings with Reviews**
2. **Alternative Recording Selection & Preference Setting**  
3. **Chronological Show Navigation (Previous/Next)**

## Current Data Architecture Analysis

### ✅ **Robust Foundation Already Available:**
- **17,788 recordings** with pre-computed ratings across **3,022 shows**
- Weighted rating algorithm with confidence scoring
- Rating distribution (1-5 star breakdown) per recording
- Show-level rating aggregation across all recordings
- Chronological show data (1965-1995, 30 years)
- Complete venue and setlist metadata
- **Archive.org Reviews API:** `https://archive.org/metadata/{identifier}/reviews`
- **Full review text available:** `reviewtitle` + `reviewbody` from API

### 📊 **Current Playlist UI:**
```kotlin
// Currently shows compact star rating
if (recording.hasRawRating) {
    CompactStarRating(
        rating = recording.rawRating,
        confidence = recording.ratingConfidence,
        starSize = IconResources.Size.SMALL
    )
}
```
**Issue:** Small, non-interactive display that doesn't invite exploration

### 🔍 **Archive.org Reviews API Structure:**
```json
{
  "reviews": [
    {
      "stars": 4.5,
      "reviewtitle": "Great show!",
      "reviewbody": "Amazing Dark Star > Drums...",
      "reviewdate": "2023-01-15",
      "reviewer": "username"
    }
  ]
}
```

---

## Feature 1: Interactive Star Ratings with Reviews

### 🎯 **User Experience**
```
Current: [small stars] ← Barely visible, non-interactive
Enhanced: [★★★★☆ 4.2 (127 reviews)] ← Prominent, tappable card
          ↓ (When tapped)
          [Review Details Modal with full review text]
```

### 🔧 **Technical Implementation**

#### **Phase 1: Enhanced Rating Display**
```kotlin
@Composable
fun InteractiveRatingDisplay(
    rating: Double,
    reviewCount: Int,
    ratingDistribution: Map<Int, Int>,
    onShowReviews: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onShowReviews() }
    ) {
        Row {
            StarRating(rating = rating)
            Text("$rating ($reviewCount reviews)")
            Icon(Icons.Default.ChevronRight)
        }
    }
}
```

#### **Phase 2: Review Details Modal**
```kotlin
@Composable
fun ReviewDetailsSheet(
    recordingId: String,
    rating: Double,
    reviewCount: Int,
    ratingDistribution: Map<Int, Int>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            // Rating Summary
            RatingSummaryCard(rating, reviewCount)
            
            // Star Distribution Chart
            RatingDistributionChart(ratingDistribution)
            
            // Individual Reviews (if available)
            LazyColumn {
                items(reviews) { review ->
                    ReviewItem(review)
                }
            }
        }
    }
}
```

#### **Phase 3: Review Text API Integration**
```kotlin
// Archive.org Reviews API Service
class ArchiveReviewsService {
    suspend fun getRecordingReviews(recordingId: String): List<Review> {
        val response = httpClient.get("https://archive.org/metadata/$recordingId/reviews")
        val reviewsData = response.body<ArchiveReviewsResponse>()
        
        return reviewsData.reviews.map { apiReview ->
            Review(
                username = apiReview.reviewer ?: "Anonymous",
                rating = apiReview.stars.toInt(),
                reviewText = "${apiReview.reviewtitle} ${apiReview.reviewbody}".trim(),
                reviewDate = apiReview.reviewdate ?: "",
                stars = apiReview.stars
            )
        }
    }
}

data class Review(
    val username: String,
    val rating: Int,
    val stars: Double,
    val reviewText: String,
    val reviewDate: String
)

// Archive.org API Response Structure
data class ArchiveReviewsResponse(
    val reviews: List<ArchiveReview>
)

data class ArchiveReview(
    val stars: Double,
    val reviewtitle: String,
    val reviewbody: String,
    val reviewdate: String?,
    val reviewer: String?
)
```

### 📱 **UI Components Needed**
1. **InteractiveRatingDisplay** - Clickable rating component
2. **ReviewDetailsSheet** - Bottom sheet with review details
3. **RatingDistributionChart** - Visual breakdown of star ratings
4. **ReviewItem** - Individual review display
5. **ReviewLoadingState** - Loading indicator for API calls

### 🗄️ **Data Layer Changes**
```kotlin
// New repository methods
interface ReviewRepository {
    suspend fun getRecordingReviews(recordingId: String): Result<List<Review>>
    suspend fun cacheReviews(recordingId: String, reviews: List<Review>)
}

// New database entities
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val recordingId: String,
    val username: String,
    val rating: Int,
    val reviewText: String,
    val reviewDate: String,
    val cachedTimestamp: Long
)
```

---

## Feature 2: Alternative Recording Selection & Preference Setting

### 🎯 **User Experience**
```
Playlist Header: [★★★★☆ 4.2] [🎵 Other Recordings] ← New Button
                                    ↓ (When tapped)
                 [Alternative Recordings Sheet]
                 
Sheet Content:
┌─────────────────────────────────────────┐
│ Recordings for 1977-05-08 Barton Hall  │
│                                         │
│ ● SBD #1 ★★★★★ 4.8 (203) [CURRENT]    │
│ ○ SBD #2 ★★★★☆ 4.5 (89)               │
│ ○ AUD #1 ★★★☆☆ 3.9 (45)               │
│ ○ MATRIX ★★★★☆ 4.2 (67)                │
│                                         │
│ [Set as Default Recording] ← If changed │
└─────────────────────────────────────────┘
```

### 🔧 **Technical Implementation**

#### **Phase 1: Alternative Recordings Discovery**
```kotlin
// Enhanced ShowRepository method
suspend fun getShowWithAllRecordings(showId: String): ShowWithRecordings {
    // Get all recordings for the show
    // Sort by rating, then source quality
    // Include current preference
}

data class ShowWithRecordings(
    val show: Show,
    val recordings: List<Recording>,
    val currentPreferredId: String?,
    val bestRatedId: String
)
```

#### **Phase 2: Recording Selection UI**
```kotlin
@Composable
fun AlternativeRecordingsSheet(
    showId: String,
    currentRecordingId: String,
    onRecordingSelected: (String) -> Unit,
    onSetAsPreferred: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRecordingId by remember { mutableStateOf(currentRecordingId) }
    var showSetDefaultButton by remember { mutableStateOf(false) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn {
            items(recordings) { recording ->
                RecordingSelectionItem(
                    recording = recording,
                    isSelected = recording.id == selectedRecordingId,
                    isCurrent = recording.id == currentRecordingId,
                    onSelect = { 
                        selectedRecordingId = recording.id
                        showSetDefaultButton = recording.id != currentRecordingId
                        onRecordingSelected(recording.id)
                    }
                )
            }
            
            if (showSetDefaultButton) {
                item {
                    Button(
                        onClick = { 
                            onSetAsPreferred(selectedRecordingId)
                            onDismiss()
                        }
                    ) {
                        Text("Set as Default Recording")
                    }
                }
            }
        }
    }
}
```

#### **Phase 3: Preference Storage**
```kotlin
// New database entity
@Entity(tableName = "show_preferences")
data class ShowPreferenceEntity(
    @PrimaryKey val showId: String,
    val preferredRecordingId: String,
    val setByUser: Boolean = true,
    val updatedTimestamp: Long
)

// Repository methods
interface ShowPreferencesRepository {
    suspend fun setPreferredRecording(showId: String, recordingId: String)
    suspend fun getPreferredRecording(showId: String): String?
    suspend fun clearPreference(showId: String)
}
```

### 📱 **UI Components Needed**
1. **AlternativeRecordingsButton** - Button in playlist header
2. **AlternativeRecordingsSheet** - Modal with recording options
3. **RecordingSelectionItem** - Individual recording with radio button
4. **SetDefaultConfirmationDialog** - Confirmation for preference change
5. **RecordingComparisonCard** - Side-by-side recording details

---

## Feature 3: Chronological Show Navigation (Previous/Next)

### 🎯 **User Experience**
```
Playlist Header: [← 5/7/77] [Barton Hall 5/8/77] [5/9/77 →]
                     ↓ (Navigation buttons)
                 Smooth transition to adjacent show's playlist
```

### 🔧 **Technical Implementation**

#### **Phase 1: Chronological Show Discovery**
```kotlin
data class ShowNavigationInfo(
    val currentShow: Show,
    val previousShow: Show?,
    val nextShow: Show?,
    val currentPosition: Int,
    val totalShows: Int
)

// Repository method
suspend fun getShowNavigationInfo(showId: String): ShowNavigationInfo {
    // Get all shows in chronological order
    // Find current show position
    // Return adjacent shows
}
```

#### **Phase 2: Navigation UI Components**
```kotlin
@Composable
fun ChronologicalNavigationBar(
    navigationInfo: ShowNavigationInfo,
    onNavigateToPrevious: () -> Unit,
    onNavigateToNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Show Button
        navigationInfo.previousShow?.let { prevShow ->
            NavigationButton(
                direction = NavigationDirection.Previous,
                show = prevShow,
                onClick = onNavigateToPrevious
            )
        } ?: Spacer(modifier = Modifier.weight(1f))
        
        // Current Show Info
        Text(
            text = "${navigationInfo.currentPosition} of ${navigationInfo.totalShows}",
            style = MaterialTheme.typography.bodySmall
        )
        
        // Next Show Button  
        navigationInfo.nextShow?.let { nextShow ->
            NavigationButton(
                direction = NavigationDirection.Next,
                show = nextShow,
                onClick = onNavigateToNext
            )
        } ?: Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun NavigationButton(
    direction: NavigationDirection,
    show: Show,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        if (direction == NavigationDirection.Previous) {
            Icon(Icons.Default.ChevronLeft, null)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = show.date,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = show.venue?.take(20) ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (direction == NavigationDirection.Next) {
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
```

#### **Phase 3: Performance Optimization**
```kotlin
// Caching strategy for navigation
class ShowNavigationCache {
    private val navigationCache = mutableMapOf<String, ShowNavigationInfo>()
    
    suspend fun getNavigationInfo(showId: String): ShowNavigationInfo {
        return navigationCache[showId] ?: run {
            val info = computeNavigationInfo(showId)
            navigationCache[showId] = info
            info
        }
    }
    
    // Pre-load adjacent shows for smooth navigation
    suspend fun preloadAdjacentShows(showId: String) {
        val info = getNavigationInfo(showId)
        info.previousShow?.let { preloadShow(it.showId) }
        info.nextShow?.let { preloadShow(it.showId) }
    }
}
```

### 📱 **UI Components Needed**
1. **ChronologicalNavigationBar** - Previous/current/next show display
2. **NavigationButton** - Individual navigation button with show preview
3. **ShowPreviewCard** - Quick show info for navigation
4. **NavigationLoadingIndicator** - Loading state during transitions

---

## Integration Plan

### 🗂️ **File Structure**
```
feature/playlist/src/main/java/com/deadarchive/feature/playlist/
├── components/
│   ├── InteractiveRatingDisplay.kt
│   ├── ReviewDetailsSheet.kt
│   ├── AlternativeRecordingsSheet.kt
│   ├── ChronologicalNavigationBar.kt
│   └── NavigationButton.kt
├── data/
│   ├── ReviewRepository.kt
│   ├── ShowPreferencesRepository.kt
│   └── ShowNavigationCache.kt
├── viewmodel/
│   ├── ReviewViewModel.kt
│   ├── RecordingSelectionViewModel.kt
│   └── NavigationViewModel.kt
└── PlaylistScreen.kt (enhanced)
```

### 🎨 **Updated Playlist Screen Layout**
```kotlin
@Composable
fun EnhancedPlaylistScreen(/* ... */) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Current title */ },
                actions = {
                    // Alternative Recordings Button
                    IconButton(onClick = { showAlternativeRecordings = true }) {
                        Icon(Icons.Default.LibraryMusic, "Other Recordings") 
                    }
                }
            )
        }
    ) { paddingValues ->
        Column {
            // Chronological Navigation Bar (NEW)
            ChronologicalNavigationBar(
                navigationInfo = navigationInfo,
                onNavigateToPrevious = { /* Navigate to previous show */ },
                onNavigateToNext = { /* Navigate to next show */ }
            )
            
            LazyColumn {
                item {
                    // Enhanced Rating Display (ENHANCED)
                    InteractiveRatingDisplay(
                        rating = currentRecording.rating,
                        reviewCount = currentRecording.reviewCount,
                        ratingDistribution = currentRecording.ratingDistribution,
                        onShowReviews = { showReviewDetails = true }
                    )
                }
                
                // Existing playlist items...
            }
        }
    }
    
    // Modal Sheets
    if (showReviewDetails) {
        ReviewDetailsSheet(/* ... */)
    }
    
    if (showAlternativeRecordings) {
        AlternativeRecordingsSheet(/* ... */)
    }
}
```

## Implementation Timeline

### 🚀 **Phase 1: Foundation (Week 1)**
- [ ] Enhanced rating display component
- [ ] Basic navigation bar UI
- [ ] Alternative recordings button

### 🔧 **Phase 2: Core Features (Week 2-3)**  
- [ ] Review details modal with API integration
- [ ] Alternative recordings selection
- [ ] Chronological navigation logic

### ✨ **Phase 3: Polish & Optimization (Week 4)**
- [ ] Preference storage and management
- [ ] Performance optimizations
- [ ] Error handling and loading states
- [ ] Animations and transitions

### 🧪 **Phase 4: Testing & Refinement (Week 5)**
- [ ] User testing and feedback
- [ ] Performance profiling
- [ ] Edge case handling
- [ ] Documentation

## Technical Considerations

### 🎭 **User Experience**
- **Smooth Transitions**: Animate between shows for continuity
- **Loading States**: Show skeletons while fetching data
- **Error Handling**: Graceful degradation when API fails
- **Accessibility**: Screen reader support for all new components

### ⚡ **Performance**
- **Lazy Loading**: Only fetch review text when requested
- **Caching Strategy**: Cache reviews and navigation info locally
- **Background Processing**: Pre-load adjacent shows for faster navigation
- **Memory Management**: Limit cached data size

### 🔒 **Data Integrity**
- **Preference Sync**: Handle conflicts between local and remote preferences  
- **Review Validation**: Sanitize review text from API
- **Offline Support**: Graceful degradation without network
- **Migration Strategy**: Handle schema changes gracefully

### 🎨 **Design System**
- **Consistent Styling**: Follow existing Material 3 patterns
- **Responsive Layout**: Handle different screen sizes
- **Dark Mode Support**: Ensure components work in both themes
- **Animation Guidelines**: Subtle, purposeful motion

## Success Metrics

### 📊 **User Engagement**
- **Review Interaction Rate**: % of users who tap rating to see reviews
- **Recording Switch Rate**: % of users who change to alternative recordings
- **Navigation Usage**: Frequency of previous/next show navigation
- **Session Duration**: Time spent exploring related shows

### 🎯 **Feature Adoption**
- **Default Recording Changes**: Number of user-set preferences
- **Review Page Views**: Time spent reading review details
- **Show Discovery**: Number of shows discovered via navigation
- **User Retention**: Return rate after using new features

This comprehensive plan provides a roadmap for implementing all three requested features while maintaining the app's existing architecture and user experience patterns.