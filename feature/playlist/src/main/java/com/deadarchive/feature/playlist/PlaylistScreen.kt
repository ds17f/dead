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
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.player.PlayerViewModel
import com.deadarchive.core.design.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    recordingId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    Log.d("PlaylistScreen", "PlaylistScreen: Composing with recordingId: $recordingId")
    
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Collect download and library states
    val downloadStates by viewModel.downloadStates.collectAsState()
    val trackDownloadStates by viewModel.trackDownloadStates.collectAsState()
    
    // Debug information states
    var debugShow by remember { mutableStateOf<Show?>(null) }
    var debugShowEntity by remember { mutableStateOf<ShowEntity?>(null) }
    
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
    
    // Only load recording metadata for display, not for playback
    LaunchedEffect(recordingId) {
        Log.d("PlaylistScreen", "LaunchedEffect: recordingId = $recordingId, currentRecording.identifier = ${currentRecording?.identifier}")
        if (!recordingId.isNullOrBlank() && currentRecording?.identifier != recordingId) {
            Log.d("PlaylistScreen", "LaunchedEffect: Loading recording metadata for display only: $recordingId")
            try {
                viewModel.loadRecording(recordingId)
            } catch (e: Exception) {
                Log.e("PlaylistScreen", "LaunchedEffect: Exception in loadRecording", e)
            }
        } else if (currentRecording?.identifier == recordingId) {
            Log.d("PlaylistScreen", "LaunchedEffect: Recording $recordingId already loaded")
        } else {
            Log.w("PlaylistScreen", "LaunchedEffect: recordingId is null or blank")
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
                    ScrollingText(
                        text = currentRecording?.let { recording ->
                            buildString {
                                // Start with artist name
                                append("Grateful Dead")
                                
                                // Add date if available
                                if (recording.concertDate.isNotBlank()) {
                                    append(" - ")
                                    append(formatConcertDate(recording.concertDate))
                                }
                                
                                // Add venue if available
                                if (!recording.concertVenue.isNullOrBlank()) {
                                    append(" - ")
                                    append(recording.concertVenue)
                                }
                                
                                // Add location (city, state) if available
                                if (!recording.concertLocation.isNullOrBlank()) {
                                    append(" - ")
                                    append(recording.concertLocation)
                                }
                            }
                        } ?: "Playlist"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painter = IconResources.Navigation.Back(), contentDescription = "Back")
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
                            downloadState = currentRecording?.let { downloadStates[it.identifier] } ?: com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded,
                            isInLibrary = false, // TODO: Add library state tracking
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
                                downloadState = currentRecording?.let { downloadStates[it.identifier] } ?: com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded,
                                isInLibrary = false // TODO: Add library state tracking
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
}

@Composable
private fun RecordingHeader(
    recording: Recording?,
    onPlayRecording: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit = {},
    onRemoveDownloadClick: () -> Unit = {},
    downloadState: ShowDownloadState = ShowDownloadState.NotDownloaded,
    isInLibrary: Boolean = false,
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
                    
                    // Star rating
                    if (recording.hasRating) {
                        Spacer(modifier = Modifier.height(4.dp))
                        CompactStarRating(
                            rating = recording.rating,
                            confidence = recording.ratingConfidence,
                            starSize = IconResources.Size.SMALL
                        )
                    }
                }
                
                // Album art placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.AlbumArt(),
                        contentDescription = "Album Art",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Library and Download buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Library button
                IconButton(
                    onClick = onLibraryClick
                ) {
                    Icon(
                        painter = if (isInLibrary) IconResources.Navigation.Back() else IconResources.Navigation.Back(), // TODO: Add proper library icons
                        contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
                        tint = if (isInLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Play button
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onPlayRecording,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = IconResources.PlayerControls.Play(),
                    contentDescription = "Play",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
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
            }
    ) {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .wrapContentWidth(Alignment.Start, unbounded = true)
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