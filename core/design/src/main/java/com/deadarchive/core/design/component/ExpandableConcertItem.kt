package com.deadarchive.core.design.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.deadarchive.core.design.R
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.settings.model.AppSettings

/**
 * Represents the download state of a recording
 */
sealed class DownloadState {
    object Available : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Represents the download state of an entire show (for the best/priority recording)
 */
sealed class ShowDownloadState {
    object NotDownloaded : ShowDownloadState()
    data class Downloading(
        val progress: Float = -1f, 
        val bytesDownloaded: Long = 0L,
        val completedTracks: Int = 0,
        val totalTracks: Int = 0
    ) : ShowDownloadState() {
        val trackProgress: Float
            get() = if (totalTracks > 0) completedTracks.toFloat() / totalTracks.toFloat() else 0f
    }
    object Downloaded : ShowDownloadState()
    data class Failed(val errorMessage: String? = null) : ShowDownloadState()
}

@Composable
fun ExpandableConcertItem(
    show: Show,
    settings: AppSettings,
    onShowClick: (Show) -> Unit,
    onRecordingClick: (Recording) -> Unit,
    onLibraryClick: (Show) -> Unit,
    onDownloadClick: (Recording) -> Unit = { },
    getDownloadState: (Recording) -> DownloadState = { DownloadState.Available },
    onShowDownloadClick: (Show) -> Unit = { },
    onCancelDownloadClick: (Show) -> Unit = { },
    onRemoveDownloadClick: (Show) -> Unit = { },
    getShowDownloadState: (Show) -> ShowDownloadState = { ShowDownloadState.NotDownloaded },
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main show header
            ShowHeader(
                show = show,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                onShowClick = onShowClick,
                onLibraryClick = onLibraryClick,
                onShowDownloadClick = onShowDownloadClick,
                onRemoveDownloadClick = onRemoveDownloadClick,
                getShowDownloadState = getShowDownloadState
            )
            
            // Expandable recordings section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RecordingsSection(
                    show = show,
                    recordings = show.recordings,
                    onRecordingClick = onRecordingClick,
                    onDownloadClick = onDownloadClick,
                    getDownloadState = getDownloadState,
                    settings = settings
                )
            }
        }
    }
}

@Composable
private fun ShowHeader(
    show: Show,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onShowClick: (Show) -> Unit,
    onLibraryClick: (Show) -> Unit,
    onShowDownloadClick: (Show) -> Unit,
    onRemoveDownloadClick: (Show) -> Unit,
    getShowDownloadState: (Show) -> ShowDownloadState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onShowClick(show) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                contentDescription = "Concert Art",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Concert information
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Date (most prominent)
            Text(
                text = show.displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Venue name
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // City, State
            Text(
                text = show.displayLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Recording count and sources
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${show.recordingCount} recordings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (show.availableSources.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = show.availableSources.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Library button
            IconButton(
                onClick = { onLibraryClick(show) }
            ) {
                Icon(
                    painter = if (show.isInLibrary) painterResource(R.drawable.ic_library_add_check) else painterResource(R.drawable.ic_library_add),
                    contentDescription = if (show.isInLibrary) "Remove from Library" else "Add to Library",
                    tint = if (show.isInLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Download button (for best recording)
            val downloadState = getShowDownloadState(show)
            Box {
                IconButton(
                    onClick = { onShowDownloadClick(show) }
                ) {
                    when (downloadState) {
                        is ShowDownloadState.NotDownloaded -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_file_download),
                                contentDescription = "Download Show",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is ShowDownloadState.Downloading -> {
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
                                    color = MaterialTheme.colorScheme.primary, // Theme primary color
                                    strokeWidth = 2.dp,
                                    trackColor = Color(0xFFE0E0E0) // Light gray track
                                )
                                
                                // Stop icon in center - clickable to cancel
                                Icon(
                                    painter = painterResource(R.drawable.ic_stop),
                                    contentDescription = "Cancel download",
                                    tint = MaterialTheme.colorScheme.primary, // Theme primary color to match progress
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            // TODO: Add cancel functionality - temporarily disabled for compilation
                                            // onCancelDownloadClick(show)
                                        }
                                )
                            }
                        }
                        is ShowDownloadState.Downloaded -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = "Downloaded - Click to remove",
                                tint = MaterialTheme.colorScheme.primary, // Theme primary color for success
                                modifier = Modifier.clickable {
                                    onRemoveDownloadClick(show)
                                }
                            )
                        }
                        is ShowDownloadState.Failed -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_file_download),
                                contentDescription = "Download Failed - ${downloadState.errorMessage ?: "Unknown error"}",
                                tint = MaterialTheme.colorScheme.error // Red for error
                            )
                        }
                    }
                }
                
                // Spotify-style: Clean progress ring with stop icon, no text overlay
            }
            
            // Expand/collapse button
            IconButton(onClick = onExpandClick) {
                Icon(
                    painter = if (isExpanded) IconResources.Navigation.ExpandLess() else IconResources.Navigation.ExpandMore(),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordingsSection(
    show: Show,
    recordings: List<Recording>,
    onRecordingClick: (Recording) -> Unit,
    onDownloadClick: (Recording) -> Unit,
    getDownloadState: (Recording) -> DownloadState,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Debug panel - moved above recordings list
        if (settings.showDebugInfo) {
            DebugPanel(
                title = "Show Debug Info",
                isVisible = true,
                initiallyExpanded = false,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Debug data
                val debugData = buildDebugString(show, recordings)
                
                // Copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(debugData))
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_library_add), // Using available icon, ideally would be copy icon
                            contentDescription = "Copy to clipboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Copy",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show basic information
                DebugText("Show ID", show.showId ?: "N/A")
                DebugText("Date", show.date)
                DebugText("Venue", show.venue ?: "N/A")
                DebugText("Location", show.location ?: "N/A")
                DebugText("Year", show.year ?: "N/A")
                
                DebugDivider()
                
                // Show the actual showId that was used (already normalized)
                DebugText("Show ID (stored)", show.showId ?: "N/A")
                DebugText("First Recording Venue", recordings.firstOrNull()?.concertVenue ?: "N/A")
                
                DebugDivider()
                
                // Recording information
                DebugText("Recording Count", recordings.size.toString())
                DebugText("Is In Library", show.isInLibrary.toString())
                DebugText("Available Sources", show.availableSources.joinToString(", "))
                
                if (recordings.isNotEmpty()) {
                    DebugDivider()
                    DebugText("Recordings", "")
                    recordings.forEachIndexed { index, recording ->
                        DebugText("  Recording ${index + 1}", recording.identifier)
                        DebugText("    Title", recording.title ?: "N/A")
                        DebugText("    Source", recording.source ?: "N/A")
                        DebugText("    Clean Source", recording.cleanSource ?: "N/A")
                    }
                }
            }
        }
        
        // Section header
        Text(
            text = "Available Recordings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Recordings list - sorted by quality priority (SBD > MATRIX > FM > AUD > others)
        recordings.sortedBy { recording ->
            when (recording.cleanSource?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
        }.forEach { recording ->
            RecordingItem(
                recording = recording,
                onRecordingClick = onRecordingClick,
                onDownloadClick = onDownloadClick,
                getDownloadState = getDownloadState
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun buildDebugString(show: Show, recordings: List<Recording>): String {
    return buildString {
        appendLine("=== SHOW DEBUG INFO ===")
        appendLine("Show ID: ${show.showId ?: "N/A"}")
        appendLine("Date: ${show.date}")
        appendLine("Venue: ${show.venue ?: "N/A"}")
        appendLine("Location: ${show.location ?: "N/A"}")
        appendLine("Year: ${show.year ?: "N/A"}")
        appendLine()
        appendLine("=== STORED DATA ===")
        appendLine("Show ID (stored): ${show.showId ?: "N/A"}")
        appendLine("First Recording Venue: ${recordings.firstOrNull()?.concertVenue ?: "N/A"}")
        appendLine()
        appendLine("=== RECORDING INFO ===")
        appendLine("Recording Count: ${recordings.size}")
        appendLine("Is In Library: ${show.isInLibrary}")
        appendLine("Available Sources: ${show.availableSources.joinToString(", ")}")
        
        if (recordings.isNotEmpty()) {
            appendLine()
            appendLine("=== RECORDINGS ===")
            recordings.forEachIndexed { index, recording ->
                appendLine("Recording ${index + 1}: ${recording.identifier}")
                appendLine("  Title: ${recording.title ?: "N/A"}")
                appendLine("  Source: ${recording.source ?: "N/A"}")
                appendLine("  Clean Source: ${recording.cleanSource ?: "N/A"}")
            }
        }
    }
}

@Composable
private fun RecordingItem(
    recording: Recording,
    onRecordingClick: (Recording) -> Unit,
    onDownloadClick: (Recording) -> Unit,
    getDownloadState: (Recording) -> DownloadState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRecordingClick(recording) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (recording.cleanSource?.uppercase()) {
                            "SBD" -> Color(0xFFD32F2F)      // Bright Red - Highest priority
                            "MATRIX" -> Color(0xFFFF9800)   // Orange - High priority  
                            "FM" -> Color(0xFF4CAF50)       // Green - Medium priority
                            "AUD" -> Color(0xFF2196F3)      // Blue - Lower priority
                            else -> MaterialTheme.colorScheme.outline // Grey - Unknown
                        }.also { color ->
                            // Debug logging to see what's happening
                            println("DEBUG: Recording ${recording.identifier} - source='${recording.source}' cleanSource='${recording.cleanSource}' color=$color")
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = recording.cleanSource?.take(3)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (recording.cleanSource?.uppercase()) {
                        "SBD" -> Color.White       // White text on red background
                        "MATRIX" -> Color.White    // White text on orange background
                        "FM" -> Color.White        // White text on green background
                        "AUD" -> Color.White       // White text on blue background
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Recording details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.recordingQuality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                val details = buildList {
                    recording.taper?.let { add("Taper: $it") }
                    if (recording.tracks.isNotEmpty()) {
                        add("${recording.tracks.size} tracks")
                    }
                }.joinToString(" • ")
                
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Row {
                // Download button
                val downloadState = getDownloadState(recording)
                IconButton(
                    onClick = { onDownloadClick(recording) },
                    modifier = Modifier.size(32.dp)
                ) {
                    when (downloadState) {
                        is DownloadState.Available -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_download),
                                contentDescription = "Download Recording",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is DownloadState.Downloading -> {
                            // TODO: Add circular progress indicator
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_download),
                                contentDescription = "Downloading...",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is DownloadState.Downloaded -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download_done),
                                contentDescription = "Downloaded",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is DownloadState.Error -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_download),
                                contentDescription = "Download Error: ${downloadState.message}",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Play button
                IconButton(
                    onClick = { onRecordingClick(recording) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Play Recording",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}