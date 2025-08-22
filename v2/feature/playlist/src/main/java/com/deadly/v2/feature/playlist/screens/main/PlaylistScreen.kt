package com.deadly.v2.feature.playlist.screens.main

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadly.v2.core.design.component.debug.DebugActivator
import com.deadly.v2.core.design.component.debug.DebugBottomSheet
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistHeader
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistAlbumArt
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistShowInfo
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistInteractiveRating
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistActionRow
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistTrackList
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistReviewDetailsSheet
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistMenuSheet
import com.deadly.v2.feature.playlist.screens.main.components.PlaylistRecordingSelectionSheet
import com.deadly.v2.feature.playlist.screens.main.models.PlaylistViewModel
import com.deadly.v2.core.design.component.debug.DebugData
import com.deadly.v2.core.design.component.debug.DebugSection
import com.deadly.v2.core.design.component.debug.DebugItem

/**
 * PlaylistScreen - Clean V2 playlist interface
 * 
 * Recreation of PlaylistV1 using V2 architecture patterns with
 * focused components and clean service integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToShow: (String, String) -> Unit = { _, _ -> },
    recordingId: String? = null,
    showId: String? = null,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    Log.d("PlaylistScreen", "=== PLAYLIST SCREEN LOADED === recordingId: $recordingId, showId: $showId")
    
    val uiState by viewModel.uiState.collectAsState()
    // Debug mode hardcoded to true for v2 development
    val showDebugInfo = true
    
    // Load show data when screen opens
    LaunchedEffect(showId) {
        viewModel.loadShow(showId)
    }
    
    // Debug panel state - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (showDebugInfo) {
        DebugData(
            screenName = "PlaylistScreen",
            sections = listOf(
                DebugSection(
                    title = "Navigation Parameters",
                    items = listOf(
                        DebugItem.KeyValue("showId", showId ?: "null"),
                        DebugItem.KeyValue("recordingId", recordingId ?: "null")
                    )
                ),
                DebugSection(
                    title = "UI State",
                    items = listOf(
                        DebugItem.BooleanValue("isLoading", uiState.isLoading),
                        DebugItem.KeyValue("error", uiState.error ?: "none"),
                        DebugItem.KeyValue("showData", if (uiState.showData != null) "loaded" else "null"),
                        DebugItem.NumericValue("tracks", uiState.trackData.size, " tracks")
                    )
                ),
                DebugSection(
                    title = "Debug Info",
                    items = listOf(
                        DebugItem.Timestamp("Screen loaded", System.currentTimeMillis()),
                        DebugItem.KeyValue("Architecture", "V2")
                    )
                )
            )
        )
    } else {
        null
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Back arrow overlay at the top
        PlaylistHeader(
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
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
                                Text(
                                    text = "Error: ${uiState.error}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { viewModel.loadShow(showId) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    // Album cover image - fixed size at top
                    item {
                        PlaylistAlbumArt()
                    }
                    
                    // Show info section - with navigation buttons
                    uiState.showData?.let { showData ->
                        item {
                            PlaylistShowInfo(
                                showData = showData,
                                isNavigationLoading = uiState.isNavigationLoading,
                                onPreviousShow = viewModel::navigateToPreviousShow,
                                onNextShow = viewModel::navigateToNextShow
                            )
                        }
                        
                        // Interactive rating display - always show
                        item {
                            PlaylistInteractiveRating(
                                showData = showData,
                                onShowReviews = viewModel::showReviews,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Action buttons row
                        item {
                            PlaylistActionRow(
                                showData = showData,
                                isPlaying = uiState.isPlaying,
                                onLibraryAction = viewModel::handleLibraryAction,
                                onDownload = viewModel::downloadShow,
                                onShowSetlist = viewModel::showSetlist,
                                onShowMenu = viewModel::showMenu,
                                onTogglePlayback = viewModel::togglePlayback
                            )
                        }
                    }
                    
                    // Track list
                    PlaylistTrackList(
                        tracks = uiState.trackData,
                        onPlayClick = viewModel::playTrack,
                        onDownloadClick = viewModel::downloadTrack
                    )
                }
            }
        }
        
        // Debug activator button (bottom-right when debug enabled)
        if (showDebugInfo && debugData != null) {
            DebugActivator(
                isVisible = true,
                onClick = { showDebugPanel = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
    
    // Debug bottom sheet
    if (showDebugPanel && debugData != null) {
        DebugBottomSheet(
            debugData = debugData,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
    
    // Review Details Modal
    if (uiState.showReviewDetails) {
        PlaylistReviewDetailsSheet(
            showData = uiState.showData,
            reviews = uiState.reviews,
            ratingDistribution = uiState.ratingDistribution,
            isLoading = uiState.reviewsLoading,
            errorMessage = uiState.reviewsError,
            onDismiss = viewModel::hideReviewDetails
        )
    }
    
    // Menu Bottom Sheet
    if (uiState.showMenu) {
        uiState.showData?.let { showData ->
            PlaylistMenuSheet(
                showDate = showData.displayDate,
                venue = showData.venue,
                location = showData.location,
                onShareClick = { 
                    // Share will be implemented in future iteration with proper model conversion
                },
                onChooseRecordingClick = viewModel::chooseRecording,
                onDismiss = viewModel::hideMenu
            )
        }
    }
    
    // Recording Selection Modal
    if (uiState.recordingSelection.isVisible) {
        PlaylistRecordingSelectionSheet(
            state = uiState.recordingSelection,
            onRecordingSelected = viewModel::selectRecording,
            onSetAsDefault = viewModel::setRecordingAsDefault,
            onResetToRecommended = if (uiState.recordingSelection.hasRecommended) {
                { viewModel.resetToRecommended() }
            } else null,
            onDismiss = viewModel::hideRecordingSelection
        )
    }
}