package com.deadarchive.feature.library.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.library.LibraryScreen
import com.deadarchive.feature.library.LibraryV2Screen
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show

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