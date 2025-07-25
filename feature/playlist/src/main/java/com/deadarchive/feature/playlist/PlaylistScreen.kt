package com.deadarchive.feature.playlist

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.design.component.ShowDownloadState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel  
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.util.VenueUtil
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.design.component.CompactStarRating
import com.deadarchive.feature.playlist.components.InteractiveRatingDisplay
import com.deadarchive.feature.playlist.components.ReviewDetailsSheet
import com.deadarchive.feature.playlist.components.RecordingSelectionSheet
import com.deadarchive.feature.playlist.components.SetlistBottomSheet
import com.deadarchive.core.common.service.ShareService
import androidx.compose.ui.platform.LocalContext
import com.deadarchive.core.data.service.RecordingSelectionService
import com.deadarchive.core.model.RecordingOption
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.player.PlayerViewModel
import com.deadarchive.feature.player.SetlistState
import com.deadarchive.feature.playlist.ReviewViewModel
import com.deadarchive.core.design.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.feature.playlist.debug.collectPlaylistDebugData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToShow: (String, String) -> Unit = { _, _ -> }, // showId, recordingId
    recordingId: String? = null,
    showId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    reviewViewModel: ReviewViewModel = hiltViewModel()
) {
    
    // Create RecordingSelectionService manually since it has no dependencies
    val recordingSelectionService = remember { RecordingSelectionService() }
    
    // Create ShareService manually
    val context = LocalContext.current
    val shareService = remember { ShareService(context) }
    
    // Set navigation callback on PlayerViewModel for next/prev show navigation
    LaunchedEffect(onNavigateToShow) {
        viewModel.onNavigateToShow = onNavigateToShow
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val reviewState by reviewViewModel.reviewState.collectAsState()
    
    // Debug data collection - only when debug mode is enabled
    val debugData = if (settings.showDebugInfo) {
        collectPlaylistDebugData(
            viewModel = viewModel,
            recordingId = recordingId,
            showId = showId
        )
    } else {
        null
    }
    
    // Collect download and library states
    val downloadStates by viewModel.downloadStates.collectAsState()
    val trackDownloadStates by viewModel.trackDownloadStates.collectAsState()
    
    // Navigation loading state
    val isNavigationLoading by viewModel.isNavigationLoading.collectAsState()
    
    
    // Alternative recordings state
    var hasAlternativeRecordings by remember { mutableStateOf(false) }
    
    // Check for alternative recordings when current recording changes
    LaunchedEffect(currentRecording, showId) {
        if (currentRecording != null) {
            try {
                val allRecordings = if (showId != null) {
                    // Use showId directly if available
                    viewModel.getAlternativeRecordingsById(showId)
                } else {
                    // Fallback to the old method
                    viewModel.getAlternativeRecordings()
                }
                
                hasAlternativeRecordings = allRecordings.size > 1 // More than just current recording
            } catch (e: Exception) {
                hasAlternativeRecordings = false
            }
        } else {
            hasAlternativeRecordings = false
        }
    }
    
    // Debug panel state
    var showDebugPanel by remember { mutableStateOf(false) }
    
    // Review modal state
    var showReviewDetails by remember { mutableStateOf(false) }
    
    // Recording selection modal state
    var showRecordingSelection by remember { mutableStateOf(false) }
    
    // Setlist modal state
    var showSetlist by remember { mutableStateOf(false) }
    val setlistState by viewModel.setlistState.collectAsState()
    
    // Menu bottom sheet state
    var showMenu by remember { mutableStateOf(false) }
    
    
    // Library state
    val isInLibrary by viewModel.isInLibrary.collectAsState()
    
    // Current track URL for play state detection
    val currentTrackUrl by viewModel.mediaControllerRepository.currentTrackUrl.collectAsState()
    
    // Check library status when show changes
    LaunchedEffect(showId) {
        showId?.let { id ->
            viewModel.checkLibraryStatus(id)
        }
    }
    
    
    // Load best recording for the show (including user preferences)
    LaunchedEffect(showId, recordingId) {
        when {
            // If we have a showId, get the best recording for that show (preferred approach)
            !showId.isNullOrBlank() -> {
                try {
                    val bestRecording = viewModel.getBestRecordingForShowId(showId)
                    if (bestRecording != null) {
                        // Only load if it's different from current recording to avoid disrupting playback
                        if (currentRecording?.identifier != bestRecording.identifier) {
                            viewModel.loadRecording(bestRecording.identifier)
                        }
                    }
                } catch (e: Exception) {
                    // Log error for recording loading issues
                    Log.e("PlaylistScreen", "Exception loading best recording for show", e)
                }
            }
            // Fallback: use specific recordingId if no showId available
            !recordingId.isNullOrBlank() && currentRecording?.identifier != recordingId -> {
                try {
                    viewModel.loadRecording(recordingId)
                } catch (e: Exception) {
                    Log.e("PlaylistScreen", "Exception in loadRecording", e)
                }
            }
        }
    }
    
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Back arrow overlay at the top
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
                .zIndex(1f)
        ) {
            Icon(
                painter = IconResources.Navigation.Back(), 
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Debug activation button - floating in bottom-right corner
        if (settings.showDebugInfo && debugData != null) {
            DebugActivator(
                isVisible = true,
                onClick = { showDebugPanel = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
        
        // Main content - Spotify-style LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = IconResources.Status.Error(),
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = uiState.error ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { recordingId?.let { viewModel.loadRecording(it) } }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                
                currentRecording == null -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading recording...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    // Album cover image - fixed size at top
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    
                    // Show info section - with navigation buttons
                    currentRecording?.let { recording ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Left side: Show info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    // Show Date
                                    Text(
                                        text = formatConcertDate(recording.concertDate),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Venue, City, State
                                    val venueLine = buildString {
                                        if (!recording.concertVenue.isNullOrBlank()) {
                                            append(recording.concertVenue)
                                        }
                                        if (!recording.concertLocation.isNullOrBlank()) {
                                            if (!recording.concertVenue.isNullOrBlank()) {
                                                append(", ")
                                            }
                                            append(recording.concertLocation)
                                        }
                                    }
                                    
                                    if (venueLine.isNotBlank()) {
                                        Text(
                                            text = venueLine,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                }
                                
                                // Right side: Navigation buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous show button
                                    IconButton(
                                        onClick = { viewModel.navigateToPreviousShow() },
                                        enabled = !isNavigationLoading,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (isNavigationLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                painter = IconResources.Navigation.ChevronLeft(),
                                                contentDescription = "Previous Show",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    // Next show button
                                    IconButton(
                                        onClick = { viewModel.navigateToNextShow() },
                                        enabled = !isNavigationLoading,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (isNavigationLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                painter = IconResources.Navigation.ChevronRight(),
                                                contentDescription = "Next Show",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Review stars - full width
                    currentRecording?.let { recording ->
                        if (recording.hasRawRating) {
                            item {
                                InteractiveRatingDisplay(
                                    rating = recording.rawRating,
                                    reviewCount = recording.reviewCount,
                                    confidence = recording.ratingConfidence,
                                    onShowReviews = { showReviewDetails = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                        
                    // Action row with icons
                    currentRecording?.let { recording ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left side: Grouped action buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Library button
                                    IconButton(
                                        onClick = { viewModel.toggleLibrary() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = if (isInLibrary) {
                                                IconResources.Content.LibraryAddCheck()
                                            } else {
                                                IconResources.Content.LibraryAdd()
                                            },
                                            contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isInLibrary) {
                                                MaterialTheme.colorScheme.error // Red color when in library
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    
                                    // Download button
                                    IconButton(
                                        onClick = { viewModel.downloadRecording() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        val downloadState = currentRecording?.let { downloadStates[it.identifier] } 
                                            ?: ShowDownloadState.NotDownloaded
                                        
                                        when (downloadState) {
                                            is ShowDownloadState.NotDownloaded -> {
                                                Icon(
                                                    painter = IconResources.Content.FileDownload(),
                                                    contentDescription = "Download Recording",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            is ShowDownloadState.Downloading -> {
                                                Box(contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(
                                                        progress = { downloadState.trackProgress },
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_stop),
                                                        contentDescription = "Stop Download",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            is ShowDownloadState.Downloaded -> {
                                                Icon(
                                                    painter = IconResources.Status.CheckCircle(),
                                                    contentDescription = "Downloaded",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            is ShowDownloadState.Failed -> {
                                                Icon(
                                                    painter = IconResources.Content.FileDownload(),
                                                    contentDescription = "Download Failed",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Setlist button
                                    IconButton(
                                        onClick = { 
                                            showId?.let { id ->
                                                viewModel.loadSetlist(id)
                                                showSetlist = true
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_format_list_bulleted),
                                            contentDescription = "Show Setlist",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    // Menu button
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_vert),
                                            contentDescription = "More options",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                // Right side: Play/Pause button alone
                                val isCurrentRecordingPlaying = currentRecording?.tracks?.any { track ->
                                    track.audioFile?.downloadUrl == currentTrackUrl
                                } == true && uiState.isPlaying
                                
                                IconButton(
                                    onClick = { 
                                        if (isCurrentRecordingPlaying) {
                                            viewModel.mediaControllerRepository.pause()
                                        } else if (currentRecording?.tracks?.any { track ->
                                            track.audioFile?.downloadUrl == currentTrackUrl
                                        } == true) {
                                            // Current recording is loaded but paused, resume
                                            viewModel.mediaControllerRepository.play()
                                        } else {
                                            // Play from beginning
                                            viewModel.playRecordingFromBeginning()
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        painter = if (isCurrentRecordingPlaying) {
                                            painterResource(R.drawable.ic_pause_circle_filled)
                                        } else {
                                            painterResource(R.drawable.ic_play_circle_filled)
                                        },
                                        contentDescription = if (isCurrentRecordingPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Tracks section
                        item {
                            Text(
                                text = "Tracks (${uiState.tracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }
                        
                        
                        // Tracks
                        itemsIndexed(uiState.tracks) { index, track ->
                            // Check if this specific track is currently playing
                            val isCurrentTrack = track.audioFile?.downloadUrl == currentTrackUrl
                            
                            TrackItem(
                                track = track,
                                isCurrentTrack = isCurrentTrack,
                                isPlaying = isCurrentTrack && uiState.isPlaying,
                                isDownloaded = track.audioFile?.filename?.let { filename ->
                                    trackDownloadStates[filename] == true
                                } ?: false,
                                onClick = { viewModel.playTrack(index) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Load reviews when modal opens
    LaunchedEffect(showReviewDetails, currentRecording) {
        if (showReviewDetails) {
            currentRecording?.let { recording ->
                reviewViewModel.loadReviews(recording.identifier)
            }
        } else {
            reviewViewModel.clearReviews()
        }
    }
    
    // Review Details Modal
    if (showReviewDetails) {
        currentRecording?.let { recording ->
            ReviewDetailsSheet(
                recordingTitle = recording.title,
                rating = recording.rawRating ?: 0f,
                reviewCount = recording.reviewCount ?: 0,
                ratingDistribution = reviewState.ratingDistribution,
                reviews = reviewState.reviews,
                isLoading = reviewState.isLoading,
                errorMessage = reviewState.errorMessage,
                onDismiss = { showReviewDetails = false }
            )
        }
    }
    
    // Recording Selection Modal
    if (showRecordingSelection && currentRecording != null) {
        var alternativeRecordings by remember { mutableStateOf<List<RecordingOption>>(emptyList()) }
        
        // Load alternative recordings when modal opens
        LaunchedEffect(showRecordingSelection, currentRecording) {
            if (showRecordingSelection) {
                val recording = currentRecording
                if (recording != null) {
                    try {
                        val allRecordings = if (showId != null) {
                            // Use showId directly if available
                            viewModel.getAlternativeRecordingsById(showId)
                        } else {
                            // Fallback to the old method
                            viewModel.getAlternativeRecordings()
                        }
                        // Get the recommended recording ID using show's built-in recommendation
                        val recommendedRecordingId = if (showId != null) {
                            viewModel.getRecommendedRecordingId(showId)
                        } else null
                        
                        alternativeRecordings = recordingSelectionService.getRecordingOptions(
                            recordings = allRecordings,
                            currentRecording = recording,
                            settings = settings,
                            ratingsBestRecordingId = recommendedRecordingId
                        )
                    } catch (e: Exception) {
                        alternativeRecordings = emptyList()
                    }
                }
            }
        }
        
        RecordingSelectionSheet(
            showTitle = currentRecording?.title ?: "",
            currentRecording = currentRecording,
            alternativeRecordings = alternativeRecordings,
            settings = settings,
            onRecordingSelected = { selectedRecording ->
                viewModel.loadRecording(selectedRecording.identifier)
                // Also save as preference when a different recording is selected
                if (showId != null && selectedRecording.identifier != currentRecording?.identifier) {
                    viewModel.setRecordingPreference(showId, selectedRecording.identifier)
                }
                showRecordingSelection = false
            },
            onSetAsDefault = { recordingId ->
                if (showId != null) {
                    // Store as default recording preference
                    viewModel.setRecordingPreference(showId, recordingId)
                }
                showRecordingSelection = false
            },
            onResetToRecommended = if (showId != null) {
                {
                    viewModel.resetToRecommendedRecording(showId)
                    showRecordingSelection = false
                }
            } else null,
            onDismiss = { showRecordingSelection = false }
        )
    }
    
    // Setlist Bottom Sheet
    if (showSetlist) {
        val currentSetlistState = setlistState
        SetlistBottomSheet(
            setlist = when (currentSetlistState) {
                is SetlistState.Success -> currentSetlistState.setlist
                else -> null
            },
            isLoading = currentSetlistState is SetlistState.Loading,
            errorMessage = when (currentSetlistState) {
                is SetlistState.Error -> currentSetlistState.message
                else -> null
            },
            onDismiss = { 
                showSetlist = false
                viewModel.clearSetlist()
            }
        )
    }
    
    // Menu Bottom Sheet
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Share option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentRecording?.let { recording ->
                                // Create a minimal show object for sharing
                                val show = Show(
                                    date = recording.concertDate,
                                    venue = recording.concertVenue,
                                    location = recording.concertLocation
                                )
                                shareService.shareShow(show, recording)
                            }
                            showMenu = false
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = IconResources.Content.Share(),
                        contentDescription = "Share",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Choose Recording option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showRecordingSelection = true
                            showMenu = false
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = IconResources.Content.LibraryMusic(),
                        contentDescription = "Choose Recording",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Choose Recording",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Debug Bottom Sheet - only shown when debug mode is enabled
    if (settings.showDebugInfo && debugData != null) {
        DebugBottomSheet(
            debugData = debugData,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}

@Composable
private fun RecordingHeader(
    recording: Recording?,
    onPlayRecording: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onRemoveDownloadClick: () -> Unit = {},
    onShowReviews: () -> Unit = {},
    onShowRecordingSelection: () -> Unit = {},
    onPreviousShow: () -> Unit = {},
    onNextShow: () -> Unit = {},
    downloadState: ShowDownloadState = ShowDownloadState.NotDownloaded,
    isInLibrary: Boolean = false,
    hasAlternativeRecordings: Boolean = false,
    isNavigationLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (recording == null) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recording title
            Text(
                text = recording.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Date and venue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDate(recording.concertDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    recording.concertVenue?.takeIf { it.isNotBlank() }?.let { venue ->
                        Text(
                            text = venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (recording.displaySource.isNotBlank()) {
                        Text(
                            text = "Source: ${recording.displaySource}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                }
            }
            
            // Action buttons and rating display on the same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action buttons (on the left side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recording selection button (gear icon)
                    if (hasAlternativeRecordings) {
                        IconButton(
                            onClick = onShowRecordingSelection
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Choose Recording",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Download button
                    IconButton(
                        onClick = onDownloadClick
                    ) {
                        when (downloadState) {
                            is com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded -> {
                                Icon(
                                    painter = IconResources.Content.FileDownload(),
                                    contentDescription = "Download Recording",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is com.deadarchive.core.design.component.ShowDownloadState.Downloading -> {
                                // Spotify-style: Stop icon with circular progress ring
                                val progressValue = when {
                                    downloadState.totalTracks > 0 -> downloadState.trackProgress
                                    downloadState.progress >= 0f -> downloadState.progress
                                    else -> 0f
                                }
                                
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    // Background progress ring
                                    CircularProgressIndicator(
                                        progress = { progressValue },
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    
                                    // Stop icon in center - clickable to cancel
                                    Icon(
                                        painter = painterResource(R.drawable.ic_stop),
                                        contentDescription = "Cancel download",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onCancelDownloadClick() }
                                    )
                                }
                            }
                            is com.deadarchive.core.design.component.ShowDownloadState.Downloaded -> {
                                Icon(
                                    painter = IconResources.Status.CheckCircle(),
                                    contentDescription = "Downloaded - Click to remove",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onRemoveDownloadClick() }
                                )
                            }
                            is com.deadarchive.core.design.component.ShowDownloadState.Failed -> {
                                Icon(
                                    painter = IconResources.Content.FileDownload(),
                                    contentDescription = "Download Failed - ${downloadState.errorMessage ?: "Unknown error"}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // Interactive rating display (takes remaining space)
                if (recording.hasRawRating) {
                    InteractiveRatingDisplay(
                        rating = recording.rawRating,
                        reviewCount = recording.reviewCount,
                        confidence = recording.ratingConfidence,
                        onShowReviews = onShowReviews,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Play button with navigation arrows
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous show button
                IconButton(
                    onClick = onPreviousShow,
                    enabled = !isNavigationLoading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isNavigationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Icon(
                            painter = IconResources.Navigation.ChevronLeft(),
                            contentDescription = "Previous Show",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Play button (smaller, centered)
                Button(
                    onClick = onPlayRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Play",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Next show button
                IconButton(
                    onClick = onNextShow,
                    enabled = !isNavigationLoading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isNavigationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Icon(
                            painter = IconResources.Navigation.ChevronRight(),
                            contentDescription = "Next Show",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
    }
}

@Composable
private fun TrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean = false,
    isDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Music note icon (only shown for current track that is playing)
        if (isCurrentTrack && isPlaying) {
            Icon(
                painter = IconResources.PlayerControls.MusicNote(),
                contentDescription = "Playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        // Track info
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = track.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentTrack) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentTrack && isPlaying) {
                    // Currently playing track - blue
                    MaterialTheme.colorScheme.primary
                } else if (isCurrentTrack && !isPlaying) {
                    // Current track but paused - red highlight
                    Color.Red
                } else {
                    // Normal track
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Start
            )
            
            track.audioFile?.let { audioFile ->
                Text(
                    text = "${audioFile.displayFormat} • ${track.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Start
                )
            }
        }
        
        // Download indicator - only shown if track is downloaded
        if (isDownloaded) {
            Icon(
                painter = IconResources.Status.CheckCircle(),
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ScrollingText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    fontWeight: FontWeight? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var shouldScroll by remember { mutableStateOf(false) }
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    // Custom easing function that pauses at both ends
    val pausingEasing = Easing { fraction ->
        when {
            fraction < 0.15f -> 0f // Pause at start (15% of time at position 0)
            fraction > 0.85f -> 1f // Pause at end (15% of time at position 1)
            else -> {
                // Smooth transition for the middle 70% of the time
                val adjustedFraction = (fraction - 0.15f) / 0.7f
                adjustedFraction
            }
        }
    }
    
    // Animation for scrolling - back and forth motion with pauses at ends
    val animatedOffset by animateFloatAsState(
        targetValue = if (shouldScroll) -(textWidth - containerWidth).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000, // Fixed 8 second duration for consistency
                easing = pausingEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scrolling_text"
    )
    
    // Auto-start scrolling after a delay if text is too long
    LaunchedEffect(shouldScroll, textWidth, containerWidth) {
        if (textWidth > containerWidth && containerWidth > 0) {
            delay(2000) // Wait 2 seconds before starting to scroll
            shouldScroll = true
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
            },
        contentAlignment = if (shouldScroll) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            textAlign = if (shouldScroll) TextAlign.Start else TextAlign.Center,
            modifier = Modifier
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .wrapContentWidth(
                    if (shouldScroll) Alignment.Start else Alignment.CenterHorizontally, 
                    unbounded = shouldScroll
                )
                .onGloballyPositioned { coordinates ->
                    textWidth = coordinates.size.width
                }
        )
    }
}

private fun formatConcertDate(dateString: String): String {
    return try {
        // Convert from YYYY-MM-DD to more readable format
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            
            "${monthNames[month - 1]} $day, $year"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

