package com.deadarchive.feature.playlist

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel  
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Show
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.design.component.DebugPanel
import com.deadarchive.core.design.component.DebugText
import com.deadarchive.core.design.component.DebugDivider
import com.deadarchive.core.design.component.DebugMultilineText
import com.deadarchive.core.design.component.CompactStarRating
import com.deadarchive.feature.playlist.components.InteractiveRatingDisplay
import com.deadarchive.feature.playlist.components.ReviewDetailsSheet
import com.deadarchive.feature.playlist.components.RecordingSelectionSheet
import com.deadarchive.core.common.service.ShareService
import androidx.compose.ui.platform.LocalContext
import com.deadarchive.feature.playlist.data.RecordingSelectionService
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.player.PlayerViewModel
import com.deadarchive.feature.playlist.ReviewViewModel
import com.deadarchive.core.design.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

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
    Log.d("PlaylistScreen", "PlaylistScreen: Composing with recordingId: $recordingId")
    
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
                Log.d("PlaylistScreen", "Checking alternative recordings for: ${currentRecording?.identifier}")
                Log.d("PlaylistScreen", "Using showId: $showId")
                
                val allRecordings = if (showId != null) {
                    // Use showId directly if available
                    Log.d("PlaylistScreen", "Using direct showId lookup")
                    viewModel.getAlternativeRecordingsById(showId)
                } else {
                    // Fallback to the old method
                    Log.d("PlaylistScreen", "Falling back to venue normalization method")
                    viewModel.getAlternativeRecordings()
                }
                
                Log.d("PlaylistScreen", "Alternative recordings check: found ${allRecordings.size} recordings")
                hasAlternativeRecordings = allRecordings.size > 1 // More than just current recording
                Log.d("PlaylistScreen", "Setting hasAlternativeRecordings to: ${allRecordings.size > 1}")
            } catch (e: Exception) {
                Log.e("PlaylistScreen", "Error checking for alternative recordings", e)
                hasAlternativeRecordings = false
            }
        } else {
            hasAlternativeRecordings = false
        }
    }
    
    // Debug information states
    var debugShow by remember { mutableStateOf<Show?>(null) }
    var debugShowEntity by remember { mutableStateOf<ShowEntity?>(null) }
    
    // Review modal state
    var showReviewDetails by remember { mutableStateOf(false) }
    
    // Recording selection modal state
    var showRecordingSelection by remember { mutableStateOf(false) }
    
    // Fetch debug information when recording changes
    LaunchedEffect(currentRecording, settings.showDebugInfo) {
        if (settings.showDebugInfo && currentRecording != null) {
            try {
                val showId = currentRecording?.let { recording ->
                    val normalizedDate = if (recording.concertDate.contains("T")) {
                        recording.concertDate.substringBefore("T")
                    } else {
                        recording.concertDate
                    }
                    val normalizedVenue = recording.concertVenue
                        ?.replace("'", "")
                        ?.replace(".", "")
                        ?.replace(" - ", "_")
                        ?.replace(", ", "_")
                        ?.replace(" & ", "_and_")
                        ?.replace("&", "_and_")
                        ?.replace(" University", "_U", true)
                        ?.replace(" College", "_C", true)
                        ?.replace("Memorial", "Mem", true)
                        ?.replace("\\s+".toRegex(), "_")
                        ?.replace("_+".toRegex(), "_")
                        ?.trim('_')
                        ?.lowercase()
                        ?: "unknown"
                    "${normalizedDate}_${normalizedVenue}"
                }
                
                if (showId != null) {
                    debugShowEntity = viewModel.getShowEntityById(showId)
                    debugShow = viewModel.getShowByRecording(currentRecording!!)
                }
            } catch (e: Exception) {
                Log.e("PlaylistScreen", "Error fetching debug info", e)
            }
        } else {
            debugShow = null
            debugShowEntity = null
        }
    }
    
    // Load best recording for the show (including user preferences)
    LaunchedEffect(showId, recordingId) {
        Log.d("PlaylistScreen", "LaunchedEffect: showId = $showId, recordingId = $recordingId")
        Log.d("PlaylistScreen", "LaunchedEffect: currentRecording.identifier = ${currentRecording?.identifier}")
        
        when {
            // If we have a showId, get the best recording for that show (preferred approach)
            !showId.isNullOrBlank() -> {
                Log.d("PlaylistScreen", "LaunchedEffect: Loading best recording for showId: $showId")
                try {
                    val bestRecording = viewModel.getBestRecordingForShowId(showId)
                    if (bestRecording != null) {
                        // Only load if it's different from current recording to avoid disrupting playback
                        if (currentRecording?.identifier != bestRecording.identifier) {
                            Log.d("PlaylistScreen", "LaunchedEffect: Loading new best recording: ${bestRecording.identifier}")
                            viewModel.loadRecording(bestRecording.identifier)
                        } else {
                            Log.d("PlaylistScreen", "LaunchedEffect: Best recording ${bestRecording.identifier} already loaded")
                        }
                    } else {
                        Log.w("PlaylistScreen", "LaunchedEffect: No recordings found for showId: $showId")
                    }
                } catch (e: Exception) {
                    Log.e("PlaylistScreen", "LaunchedEffect: Exception loading best recording for show", e)
                }
            }
            // Fallback: use specific recordingId if no showId available
            !recordingId.isNullOrBlank() && currentRecording?.identifier != recordingId -> {
                Log.d("PlaylistScreen", "LaunchedEffect: Fallback - Loading specific recording: $recordingId")
                try {
                    viewModel.loadRecording(recordingId)
                } catch (e: Exception) {
                    Log.e("PlaylistScreen", "LaunchedEffect: Exception in loadRecording", e)
                }
            }
            currentRecording?.identifier == recordingId -> {
                Log.d("PlaylistScreen", "LaunchedEffect: Recording $recordingId already loaded")
            }
            else -> {
                Log.w("PlaylistScreen", "LaunchedEffect: No showId or recordingId provided")
            }
        }
    }
    
    // Debug current state
    LaunchedEffect(uiState, currentRecording) {
        Log.d("PlaylistScreen", "State Update - isLoading: ${uiState.isLoading}, " +
                "tracks.size: ${uiState.tracks.size}, " +
                "error: ${uiState.error}, " +
                "recording: ${currentRecording?.title}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    currentRecording?.let { recording ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // First line: Date only
                            if (recording.concertDate.isNotBlank()) {
                                Text(
                                    text = formatConcertDate(recording.concertDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // Second line: Venue, City/State
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
                                ScrollingText(
                                    text = venueLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } ?: Text(
                        text = "Playlist",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painter = IconResources.Navigation.Back(), contentDescription = "Back")
                    }
                },
                actions = {
                    // Share button
                    if (currentRecording != null) {
                        IconButton(
                            onClick = {
                                // Create show object from recording data
                                val show = Show(
                                    date = currentRecording!!.concertDate,
                                    venue = currentRecording!!.concertVenue,
                                    location = currentRecording!!.concertLocation
                                )
                                shareService.shareShow(show, currentRecording!!)
                            }
                        ) {
                            Icon(
                                painter = IconResources.Content.Share(), 
                                contentDescription = "Share show",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                
                currentRecording == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading recording...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                uiState.tracks.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Recording header
                        RecordingHeader(
                            recording = currentRecording,
                            onPlayRecording = { viewModel.playRecordingFromBeginning() },
                            onLibraryClick = { viewModel.toggleLibrary() },
                            onDownloadClick = { viewModel.downloadRecording() },
                            onCancelDownloadClick = { viewModel.cancelRecordingDownloads() },
                            onRemoveDownloadClick = { viewModel.showRemoveDownloadConfirmation() },
                            onShowReviews = { showReviewDetails = true },
                            onShowRecordingSelection = { showRecordingSelection = true },
                            onPreviousShow = { viewModel.navigateToPreviousShow() },
                            onNextShow = { viewModel.navigateToNextShow() },
                            downloadState = currentRecording?.let { downloadStates[it.identifier] } ?: com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded,
                            isInLibrary = false, // TODO: Add library state tracking
                            hasAlternativeRecordings = hasAlternativeRecordings,
                            isNavigationLoading = isNavigationLoading,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        // Empty tracks message
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No tracks available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (recordingId != null && !uiState.isLoading) {
                                    Button(onClick = { viewModel.loadRecording(recordingId) }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            RecordingHeader(
                                recording = currentRecording,
                                onPlayRecording = { viewModel.playRecordingFromBeginning() },
                                onLibraryClick = { viewModel.toggleLibrary() },
                                onDownloadClick = { viewModel.downloadRecording() },
                                onCancelDownloadClick = { viewModel.cancelRecordingDownloads() },
                                onRemoveDownloadClick = { viewModel.showRemoveDownloadConfirmation() },
                                onShowReviews = { showReviewDetails = true },
                                onShowRecordingSelection = { showRecordingSelection = true },
                                onPreviousShow = { viewModel.navigateToPreviousShow() },
                                onNextShow = { viewModel.navigateToNextShow() },
                                downloadState = currentRecording?.let { downloadStates[it.identifier] } ?: com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded,
                                isInLibrary = false, // TODO: Add library state tracking
                                hasAlternativeRecordings = hasAlternativeRecordings,
                                isNavigationLoading = isNavigationLoading
                            )
                        }
                        
                        item {
                            Text(
                                text = "Tracks (${uiState.tracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Debug panel
                        if (settings.showDebugInfo && currentRecording != null) {
                            item {
                                DebugPanel(
                                    title = "Database Debug Info",
                                    isVisible = settings.showDebugInfo,
                                    initiallyExpanded = false
                                ) {
                                    currentRecording?.let { recording ->
                                        // Show basic recording info
                                        DebugText("Recording ID", recording.identifier)
                                        DebugText("Title", recording.title ?: "N/A")
                                        DebugText("Concert Date", recording.concertDate)
                                        DebugText("Concert Venue", recording.concertVenue ?: "N/A")
                                        DebugText("Concert Location", recording.concertLocation ?: "N/A")
                                        DebugText("Source", recording.source ?: "N/A")
                                        DebugText("Tracks Count", "${recording.tracks.size}")
                                        
                                        DebugDivider()
                                        
                                        // Show calculated showId
                                        val normalizedDate = if (recording.concertDate.contains("T")) {
                                            recording.concertDate.substringBefore("T")
                                        } else {
                                            recording.concertDate
                                        }
                                        val normalizedVenue = recording.concertVenue
                                            ?.replace("'", "")
                                            ?.replace(".", "")
                                            ?.replace(" - ", "_")
                                            ?.replace(", ", "_")
                                            ?.replace(" & ", "_and_")
                                            ?.replace("&", "_and_")
                                            ?.replace(" University", "_U", true)
                                            ?.replace(" College", "_C", true)
                                            ?.replace("Memorial", "Mem", true)
                                            ?.replace("\\s+".toRegex(), "_")
                                            ?.replace("_+".toRegex(), "_")
                                            ?.trim('_')
                                            ?.lowercase()
                                            ?: "unknown"
                                        val calculatedShowId = "${normalizedDate}_${normalizedVenue}"
                                        
                                        DebugText("Calculated ShowID", calculatedShowId)
                                        DebugText("Normalized Date", normalizedDate)
                                        DebugText("Normalized Venue", normalizedVenue)
                                        
                                        DebugDivider()
                                        
                                        // Show entity information if available
                                        debugShowEntity?.let { entity ->
                                            DebugText("DB ShowEntity ID", entity.showId)
                                            DebugText("DB Date", entity.date)
                                            DebugText("DB Venue", entity.venue ?: "N/A")
                                            DebugText("DB Location", entity.location ?: "N/A")
                                            DebugText("DB Year", entity.year ?: "N/A")
                                            DebugText("DB In Library", entity.isInLibrary.toString())
                                            DebugText("DB Cache Time", entity.cachedTimestamp.toString())
                                            entity.setlistRaw?.let { setlist ->
                                                DebugMultilineText("DB Setlist", setlist, maxLines = 3)
                                            }
                                        } ?: run {
                                            DebugText("DB ShowEntity", "Not found or not loaded")
                                        }
                                        
                                        DebugDivider()
                                        
                                        // Show object information if available
                                        debugShow?.let { show ->
                                            DebugText("Show Date", show.date)
                                            DebugText("Show Venue", show.venue ?: "N/A")
                                            DebugText("Show Location", show.location ?: "N/A")
                                            DebugText("Show Year", show.year ?: "N/A")
                                            DebugText("Show In Library", show.isInLibrary.toString())
                                            DebugText("Show Recordings", "${show.recordings.size}")
                                            DebugText("Show ID", show.showId)
                                        } ?: run {
                                            DebugText("Show Object", "Not found or not loaded")
                                        }
                                    }
                                }
                            }
                        }
                        
                        itemsIndexed(uiState.tracks) { index, track ->
                            TrackItem(
                                track = track,
                                isCurrentTrack = index == uiState.currentTrackIndex,
                                isDownloaded = currentRecording?.let { recording ->
                                    val trackKey = "${recording.identifier}_${track.audioFile?.filename}"
                                    trackDownloadStates[trackKey] == true
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
        var alternativeRecordings by remember { mutableStateOf<List<com.deadarchive.feature.playlist.components.RecordingOption>>(emptyList()) }
        
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
                        Log.d("PlaylistScreen", "Recording selection: Found ${allRecordings.size} total recordings")
                        allRecordings.forEachIndexed { index, rec ->
                            Log.d("PlaylistScreen", "Recording $index: ${rec.identifier} - ${rec.displayTitle}")
                        }
                        
                        // Get the recommended recording ID using show's built-in recommendation
                        val recommendedRecordingId = if (showId != null) {
                            viewModel.getRecommendedRecordingId(showId)
                        } else null
                        Log.d("PlaylistScreen", "Recording selection: Recommended recording ID = $recommendedRecordingId")
                        Log.d("PlaylistScreen", "Recording selection: Current recording ID = ${recording.identifier}")
                        
                        // Debug: Check if any recording matches the recommended best
                        allRecordings.forEach { rec ->
                            val isRecommended = rec.identifier == recommendedRecordingId
                            Log.d("PlaylistScreen", "Recording ${rec.identifier} - isRecommended: $isRecommended, title: ${rec.displayTitle}")
                        }
                        
                        alternativeRecordings = recordingSelectionService.getRecordingOptions(
                            recordings = allRecordings,
                            currentRecording = recording,
                            settings = settings,
                            ratingsBestRecordingId = recommendedRecordingId
                        )
                        Log.d("PlaylistScreen", "Recording selection: ${alternativeRecordings.size} alternative recordings after filtering")
                        Log.d("PlaylistScreen", "Recording selection: Current recording = ${recording.identifier}")
                    } catch (e: Exception) {
                        Log.e("PlaylistScreen", "Error loading alternative recordings", e)
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
    isDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Currently playing",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = track.displayTrackNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentTrack) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                track.audioFile?.let { audioFile ->
                    Text(
                        text = "${audioFile.displayFormat} • ${track.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentTrack) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Download indicator - only shown if track is downloaded
            if (isDownloaded) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = IconResources.Status.CheckCircle(),
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
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