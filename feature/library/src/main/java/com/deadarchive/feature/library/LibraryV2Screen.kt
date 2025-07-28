package com.deadarchive.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.Show
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.library.debug.collectLibraryV2DebugData

/**
 * Spotify-like Library V2 Screen.
 * 
 * Features:
 * - Header with Steal Your Face logo, "Your Library" title, search and add buttons
 * - Decade filters (60s, 70s, 80s, 90s)
 * - Sort controls (by date, by date added) and grid/list toggle
 * - Show list with album covers, dates, and venue info
 * - MiniPlayer integration
 * - Debug panel for development
 */
@Composable
fun LibraryV2Screen(
    viewModel: LibraryV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToShow: (String) -> Unit = {},
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // UI State
    var selectedDecade by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf(SortOption.DATE_OF_SHOW) }
    var displayMode by remember { mutableStateOf(DisplayMode.LIST) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    
    // Debug panel state and data collection - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectLibraryV2DebugData(viewModel)
    } else null
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Spotify-like Header
            LibraryHeader(
                onSearchClick = { /* TODO: Implement search */ },
                onAddClick = { showAddBottomSheet = true }
            )
            
            // Decade Filters
            DecadeFilters(
                selectedDecade = selectedDecade,
                onDecadeSelected = { selectedDecade = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Sort Controls and Display Toggle
            SortAndDisplayControls(
                sortBy = sortBy,
                displayMode = displayMode,
                onSortChanged = { sortBy = it },
                onDisplayModeChanged = { displayMode = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Main Content
            when (val state = uiState) {
                is LibraryV2UiState.Loading -> {
                    LoadingContent(modifier = Modifier.weight(1f))
                }
                
                is LibraryV2UiState.Success -> {
                    if (state.shows.isEmpty()) {
                        EmptyLibraryContent(
                            onTestStubs = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LibraryContent(
                            shows = state.shows,
                            displayMode = displayMode,
                            onShowClick = onNavigateToShow,
                            onPlayClick = onNavigateToPlayer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                is LibraryV2UiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = viewModel::retry,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // MiniPlayer is already handled by the navigation system
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
    }
    
    // Add Bottom Sheet
    if (showAddBottomSheet) {
        AddToLibraryBottomSheet(
            onDismiss = { showAddBottomSheet = false }
        )
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

// Enums for UI state
enum class SortOption {
    DATE_OF_SHOW,
    DATE_ADDED
}

enum class DisplayMode {
    LIST,
    GRID
}

/**
 * Spotify-like header with logo, title, and action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHeader(
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steal Your Face icon
                Image(
                    painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                    contentDescription = "Dead Archive",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    painter = IconResources.Content.Search(),
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onAddClick) {
                Icon(
                    painter = IconResources.Navigation.Add(),
                    contentDescription = "Add to Library"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Decade filter chips (60s, 70s, 80s, 90s) with clear button
 */
@Composable
private fun DecadeFilters(
    selectedDecade: String?,
    onDecadeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val decades = listOf("60s", "70s", "80s", "90s")
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(decades) { decade ->
            FilterChip(
                onClick = {
                    onDecadeSelected(if (selectedDecade == decade) null else decade)
                },
                label = { Text(decade) },
                selected = selectedDecade == decade,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        
        // Clear button - only show if a decade is selected
        if (selectedDecade != null) {
            item {
                OutlinedButton(
                    onClick = { onDecadeSelected(null) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Sort controls and grid/list display toggle
 */
@Composable
private fun SortAndDisplayControls(
    sortBy: SortOption,
    displayMode: DisplayMode,
    onSortChanged: (SortOption) -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort options
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = { onSortChanged(SortOption.DATE_OF_SHOW) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (sortBy == SortOption.DATE_OF_SHOW) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Text(
                    text = "Date of Show",
                    fontWeight = if (sortBy == SortOption.DATE_OF_SHOW) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            TextButton(
                onClick = { onSortChanged(SortOption.DATE_ADDED) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (sortBy == SortOption.DATE_ADDED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                Text(
                    text = "Date Added",
                    fontWeight = if (sortBy == SortOption.DATE_ADDED) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        
        // Display mode toggle 
        IconButton(
            onClick = {
                onDisplayModeChanged(
                    if (displayMode == DisplayMode.LIST) DisplayMode.GRID else DisplayMode.LIST
                )
            }
        ) {
            Icon(
                painter = if (displayMode == DisplayMode.LIST) {
                    IconResources.Navigation.Menu() // Grid icon
                } else {
                    IconResources.Content.Queue() // List icon  
                },
                contentDescription = if (displayMode == DisplayMode.LIST) "Grid View" else "List View"
            )
        }
    }
}

/**
 * Loading state content
 */
@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your library...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main library content with list or grid display
 */
@Composable
private fun LibraryContent(
    shows: List<Show>,
    displayMode: DisplayMode,
    onShowClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (displayMode) {
        DisplayMode.LIST -> {
            LibraryListView(
                shows = shows,
                onShowClick = onShowClick,
                onPlayClick = onPlayClick,
                modifier = modifier
            )
        }
        DisplayMode.GRID -> {
            LibraryGridView(
                shows = shows,
                onShowClick = onShowClick,
                onPlayClick = onPlayClick,
                modifier = modifier
            )
        }
    }
}

/**
 * List view of shows (1 per line with album cover, date, venue)
 */
@Composable
private fun LibraryListView(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shows) { show ->
            ShowListItem(
                show = show,
                onShowClick = { onShowClick(show.showId) },
                onPlayClick = { onPlayClick(show.showId) }
            )
        }
    }
}

/**
 * Grid view of shows
 */
@Composable
private fun LibraryGridView(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shows) { show ->
            ShowGridItem(
                show = show,
                onShowClick = { onShowClick(show.showId) },
                onPlayClick = { onPlayClick(show.showId) }
            )
        }
    }
}

/**
 * Individual show item in list view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowListItem(
    show: Show,
    onShowClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        onClick = onShowClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Show info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = show.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${show.displayVenue}, ${show.displayLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.Play(),
                    contentDescription = "Play Show",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Individual show item in grid view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowGridItem(
    show: Show,
    onShowClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        onClick = onShowClick,
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Album cover placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Play button overlay
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Play Show",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show info
            Text(
                text = show.date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Empty library content with test buttons for stub validation
 */
@Composable
private fun EmptyLibraryContent(
    onTestStubs: LibraryV2ViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = IconResources.Content.LibraryMusic(),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your Library is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Add some shows to get started. In stub mode, use the test buttons below to verify the integration.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test buttons to verify stub integration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTestStubs.addToLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Add to Library")
            }
            
            OutlinedButton(
                onClick = { onTestStubs.removeFromLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Remove from Library")
            }
            
            OutlinedButton(
                onClick = { 
                    onTestStubs.downloadShow(
                        Show(
                            date = "1977-05-08",
                            venue = "Test Venue",
                            location = "Test City, NY",
                            recordings = emptyList(),
                            isInLibrary = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Download Show")
            }
            
            FilledTonalButton(
                onClick = { onTestStubs.clearLibrary() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Clear Library")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "💡 Actions log to debug panel (tap floating button)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Add to Library bottom sheet (empty for now)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToLibraryBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add to Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Coming soon! This will allow you to add shows to your library.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Error state content
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = IconResources.Status.Error(),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error Loading Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

