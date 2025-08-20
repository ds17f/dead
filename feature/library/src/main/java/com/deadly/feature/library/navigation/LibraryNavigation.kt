package com.deadly.feature.library.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadly.core.settings.SettingsViewModel
import com.deadly.feature.library.LibraryScreen
import com.deadly.feature.library.LibraryV2Screen
import com.deadly.core.model.Recording
import com.deadly.core.model.Show

fun NavGraphBuilder.libraryScreen(
    onNavigateToPlayer: (Recording) -> Unit,
    onNavigateToShow: (Show) -> Unit
) {
    composable("library") {
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        val settings by settingsViewModel.settings.collectAsState()
        
        if (settings.useLibraryV2) {
            LibraryV2Screen()
        } else {
            LibraryScreen(
                onNavigateToRecording = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow
            )
        }
    }
}