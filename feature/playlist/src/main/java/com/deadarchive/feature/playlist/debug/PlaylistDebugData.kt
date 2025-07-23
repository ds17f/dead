package com.deadarchive.feature.playlist.debug

import androidx.compose.runtime.*
import com.deadarchive.core.design.component.DebugData
import com.deadarchive.core.design.component.DebugItem
import com.deadarchive.core.design.component.DebugSection
import com.deadarchive.core.design.component.DebugItemFactory
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.player.PlayerViewModel
import com.deadarchive.feature.player.PlayerUiState

/**
 * Debug data collector for PlaylistScreen.
 * Collects debug information about recording loading, player state, and service calls.
 */
@Composable
fun collectPlaylistDebugData(
    viewModel: PlayerViewModel,
    recordingId: String?,
    showId: String?
): DebugData {
    // Collect reactive state
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isNavigationLoading by viewModel.isNavigationLoading.collectAsState()
    val isInLibrary by viewModel.isInLibrary.collectAsState()
    
    // For now, create mock data to test the UI
    return DebugData(
        screenName = "PlaylistScreen",
        sections = listOf(
            createRequestInfoSection(recordingId, showId),
            createLoadingStateSection(uiState, currentRecording, isNavigationLoading),
            createPlayerServicesSection(),
            createRecordingDataSection(currentRecording),
            createDownloadStateSection(downloadStates),
            createLibraryStateSection(isInLibrary),
            createErrorSection()
        )
    )
}

private fun createRequestInfoSection(recordingId: String?, showId: String?): DebugSection {
    return DebugSection(
        title = "Request Parameters",
        items = listOf(
            DebugItem.KeyValue("recordingId", recordingId ?: "null"),
            DebugItem.KeyValue("showId", showId ?: "null"),
            DebugItemFactory.createTimestamp("Last Request"),
            DebugItem.KeyValue("Format Preferences", "[VBR MP3, Ogg Vorbis, MP3, Flac]"),
            DebugItem.BooleanValue("Has ShowId", !showId.isNullOrBlank()),
            DebugItem.BooleanValue("Has RecordingId", !recordingId.isNullOrBlank())
        )
    )
}

private fun createLoadingStateSection(
    uiState: PlayerUiState, 
    currentRecording: com.deadarchive.core.model.Recording?,
    isNavigationLoading: Boolean
): DebugSection {
    return DebugSection(
        title = "Loading State",
        items = listOf(
            DebugItem.BooleanValue("isLoading", uiState.isLoading),
            DebugItem.BooleanValue("hasCurrentRecording", currentRecording != null),
            DebugItem.BooleanValue("isNavigationLoading", isNavigationLoading),
            DebugItem.KeyValue("Loading Phase", when {
                uiState.isLoading -> "Loading Recording"
                currentRecording == null -> "No Recording Loaded"
                else -> "Recording Loaded"
            }),
            DebugItemFactory.createTimestamp("State Updated"),
            uiState.error?.let { error ->
                DebugItemFactory.createErrorItem(error)
            } ?: DebugItem.KeyValue("Last Error", "None")
        )
    )
}

private fun createPlayerServicesSection(): DebugSection {
    return DebugSection(
        title = "Player Services",
        items = listOf(
            DebugItem.KeyValue("PlayerDataService", "Available"),
            DebugItemFactory.createTimestamp("Debug Panel Generated"),
            DebugItem.KeyValue("Service Status", "Real-time data collection needed")
        )
    )
}

private fun createRecordingDataSection(currentRecording: com.deadarchive.core.model.Recording?): DebugSection {
    return DebugSection(
        title = "Recording Data",
        items = if (currentRecording != null) {
            listOf(
                DebugItem.KeyValue("Recording ID", currentRecording.identifier),
                DebugItem.KeyValue("Title", currentRecording.title ?: "Unknown"),
                DebugItem.KeyValue("Date", currentRecording.concertDate ?: "Unknown"),
                DebugItem.KeyValue("Venue", currentRecording.concertVenue ?: "Unknown"),
                DebugItem.NumericValue("Track Count", currentRecording.tracks.size),
                DebugItem.KeyValue("Source", currentRecording.source ?: "Unknown"),
                DebugItem.Multiline(
                    "Track Formats",
                    currentRecording.tracks.take(3).joinToString("\n") { track ->
                        "${track.displayTitle}: ${track.audioFile?.format ?: "No format"}"
                    }
                )
            )
        } else {
            listOf(
                DebugItem.KeyValue("Recording Status", "NULL - Not Loaded"),
                DebugItemFactory.createErrorItem("Recording failed to load - check service calls"),
                DebugItem.Multiline(
                    "Possible Causes",
                    """1. Invalid recording ID
2. Network connectivity issue
3. Database query failure
4. Format filtering removed all tracks
5. Missing getRecordingById() method"""
                )
            )
        }
    )
}

private fun createDownloadStateSection(downloadStates: Map<String, com.deadarchive.core.design.component.ShowDownloadState>): DebugSection {
    return DebugSection(
        title = "Download States",
        items = listOf(
            DebugItem.NumericValue("Tracked Downloads", downloadStates.size),
            DebugItem.Multiline(
                "Download Status",
                if (downloadStates.isEmpty()) {
                    "No downloads being tracked"
                } else {
                    downloadStates.entries.take(5).joinToString("\n") { (id, state) ->
                        "$id: $state"
                    }
                }
            ),
            DebugItem.BooleanValue("Has Active Downloads", downloadStates.values.any { 
                it is ShowDownloadState.Downloading 
            })
        )
    )
}

private fun createLibraryStateSection(isInLibrary: Boolean): DebugSection {
    return DebugSection(
        title = "Library Integration",
        items = listOf(
            DebugItem.BooleanValue("Is In Library", isInLibrary),
            DebugItemFactory.createTimestamp("Library Status Checked"),
            DebugItem.KeyValue("Library Service", "Connected"),
            DebugItem.KeyValue("Show ID for Library", "Generated from recording data")
        )
    )
}

private fun createErrorSection(): DebugSection {
    return DebugSection(
        title = "Error Tracking",
        items = listOf(
            DebugItem.KeyValue("Last Error", "None captured yet"),
            DebugItem.KeyValue("Error Collection", "Real-time tracking needed"),
            DebugItemFactory.createTimestamp("Debug Session Started")
        )
    )
}