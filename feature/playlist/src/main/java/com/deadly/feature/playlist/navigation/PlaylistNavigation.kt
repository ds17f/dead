package com.deadly.feature.playlist.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadly.core.settings.SettingsViewModel
import com.deadly.feature.playlist.PlaylistScreen
import com.deadly.feature.playlist.PlaylistV2Screen

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToShow: (String, String) -> Unit // showId, recordingId
) {
    composable("playlist/{recordingId}?showId={showId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
        val showId = backStackEntry.arguments?.getString("showId")
        
        // Read settings at navigation time, not setup time
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        val settings by settingsViewModel.settings.collectAsState()
        
        android.util.Log.d("PlaylistNavigation", "=== NAVIGATION TIME === usePlaylistV2: ${settings.usePlaylistV2}, recordingId: $recordingId, showId: $showId")
        
        if (settings.usePlaylistV2) {
            PlaylistV2Screen(
                onNavigateBack = onNavigateBack,
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                recordingId = recordingId,
                showId = showId
            )
        } else {
            PlaylistScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                recordingId = recordingId,
                showId = showId
            )
        }
    }
}