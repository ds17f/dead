package com.deadly.v2.feature.playlist.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadly.v2.feature.playlist.screens.main.PlaylistScreen

/**
 * Playlist navigation route constants
 */
const val PLAYLIST_SHOW_ROUTE = "playlist/{showId}"
const val PLAYLIST_RECORDING_ROUTE = "playlist/{showId}/{recordingId}"

/**
 * Extension function for NavController to navigate to Playlist
 * 
 * @param showId The show ID to display
 * @param recordingId Optional specific recording ID. If null, show logic decides which recording to display
 */
fun NavController.navigateToPlaylist(showId: String, recordingId: String? = null) {
    val route = if (recordingId != null) {
        "playlist/$showId/$recordingId"
    } else {
        "playlist/$showId"
    }
    navigate(route)
}

/**
 * Add Playlist destinations to NavGraphBuilder
 * 
 * Following V2 navigation patterns where screens accept
 * navigation callbacks rather than NavController directly.
 * 
 * Supports two routing patterns:
 * - playlist/{showId} - Let show logic decide which recording to display
 * - playlist/{showId}/{recordingId} - Display specific recording
 */
fun NavGraphBuilder.playlistGraph(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToShow: (String, String) -> Unit
) {
    // Specific recording route - playlist/{showId}/{recordingId}
    composable(PLAYLIST_RECORDING_ROUTE) { backStackEntry ->
        val showId = backStackEntry.arguments?.getString("showId") ?: ""
        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
        
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToShow = onNavigateToShow,
            showId = showId,
            recordingId = recordingId
        )
    }
    
    // Show-only route - playlist/{showId} (show decides recording)
    composable(PLAYLIST_SHOW_ROUTE) { backStackEntry ->
        val showId = backStackEntry.arguments?.getString("showId") ?: ""
        
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToShow = onNavigateToShow,
            showId = showId,
            recordingId = null // Let show logic decide
        )
    }
}