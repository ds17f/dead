package com.deadarchive.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.deadarchive.core.design.component.HierarchicalFilter
import com.deadarchive.core.design.component.FilterPath
import com.deadarchive.core.design.component.FilterTrees
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
    var filterPath by remember { mutableStateOf(FilterPath()) }
    var sortBy by remember { mutableStateOf(SortOption.DATE_OF_SHOW) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var displayMode by remember { mutableStateOf(DisplayMode.LIST) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var selectedShowForActions by remember { mutableStateOf<Show?>(null) }
    
    // Debug panel state and data collection - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectLibraryV2DebugData(viewModel)
    } else {
        null
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Spotify-like Header
            LibraryHeader(
                onSearchClick = { /* TODO: Implement search */ },
                onAddClick = { showAddBottomSheet = true }
            )
            
            // Hierarchical Filters
            HierarchicalFilter(
                filterTree = FilterTrees.buildDeadToursTree(),
                selectedPath = filterPath,
                onSelectionChanged = { filterPath = it }
            )
            
            // Sort Controls and Display Toggle
            SortAndDisplayControls(
                sortBy = sortBy,
                sortDirection = sortDirection,
                displayMode = displayMode,
                onSortSelectorClick = { showSortBottomSheet = true },
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
                        // Apply filtering and sorting
                        val filteredAndSortedShows = remember(state.shows, filterPath, sortBy, sortDirection) {
                            applyFiltersAndSorting(
                                shows = state.shows,
                                filterPath = filterPath,
                                sortBy = sortBy,
                                sortDirection = sortDirection
                            )
                        }
                        
                        LibraryContent(
                            shows = filteredAndSortedShows,
                            displayMode = displayMode,
                            onShowClick = onNavigateToShow,
                            onPlayClick = onNavigateToPlayer,
                            // onShowLongPress temporarily removed
                // onShowLongPress = { show -> selectedShowForActions = show },
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
    
    // Sort Bottom Sheet
    if (showSortBottomSheet) {
        SortOptionsBottomSheet(
            currentSortOption = sortBy,
            currentSortDirection = sortDirection,
            onSortOptionSelected = { option, direction ->
                sortBy = option
                sortDirection = direction
                showSortBottomSheet = false
            },
            onDismiss = { showSortBottomSheet = false }
        )
    }
    
    // Show Actions Bottom Sheet - Temporarily commented out until ShowActionsBottomSheet is implemented
    /* 
    selectedShowForActions?.let { show ->
        ShowActionsBottomSheet(
            show = show,
            onDismiss = { selectedShowForActions = null },
            onShare = { 
                // TODO: Implement share functionality
                selectedShowForActions = null
            },
            onRemoveFromLibrary = { 
                viewModel.removeFromLibrary(show.showId)
                selectedShowForActions = null
            },
            onDownload = {
                viewModel.downloadShow(show)
                selectedShowForActions = null
            },
            onPin = {
                // TODO: Implement pin functionality
                selectedShowForActions = null
            },
            onShowQRCode = {
                // TODO: Implement QR code functionality
                selectedShowForActions = null
            }
        )
    }
    */
    
    // Debug Bottom Sheet - only shown when debug mode is enabled
    debugData?.let { data ->
        DebugBottomSheet(
            debugData = data,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}

// Enums for UI state
enum class SortOption(val displayName: String) {
    DATE_OF_SHOW("Show Date"),
    DATE_ADDED("Date Added")
}

enum class SortDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
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
 * Sort controls and grid/list display toggle
 */
@Composable
private fun SortAndDisplayControls(
    sortBy: SortOption,
    sortDirection: SortDirection,
    displayMode: DisplayMode,
    onSortSelectorClick: () -> Unit,
    onDisplayModeChanged: (DisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort selector button (like old library)
        SortSelectorButton(
            sortBy = sortBy,
            sortDirection = sortDirection,
            onClick = onSortSelectorClick
        )
        
        // Display mode toggle with proper icons
        IconButton(
            onClick = {
                onDisplayModeChanged(
                    if (displayMode == DisplayMode.LIST) DisplayMode.GRID else DisplayMode.LIST
                )
            }
        ) {
            Icon(
                painter = if (displayMode == DisplayMode.LIST) {
                    IconResources.Content.GridView() // Grid icon when in list mode
                } else {
                    IconResources.Content.FormatListBulleted() // List icon when in grid mode
                },
                contentDescription = if (displayMode == DisplayMode.LIST) "Grid View" else "List View"
            )
        }
    }
}

/**
 * Sort selector button following the old library pattern
 */
@Composable
private fun SortSelectorButton(
    sortBy: SortOption,
    sortDirection: SortDirection,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = IconResources.Navigation.SwapVert(),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = sortBy.displayName)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = if (sortDirection == SortDirection.ASCENDING) {
                    IconResources.Navigation.KeyboardArrowUp()
                } else {
                    IconResources.Navigation.KeyboardArrowDown()
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
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
    // Make onShowLongPress optional with empty default implementation
    onShowLongPress: (Show) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (displayMode) {
        DisplayMode.LIST -> {
            LibraryListView(
                shows = shows,
                onShowClick = onShowClick,
                onPlayClick = onPlayClick,
                // onShowLongPress temporarily removed
                // onShowLongPress = onShowLongPress,
                modifier = modifier
            )
        }
        DisplayMode.GRID -> {
            LibraryGridView(
                shows = shows,
                onShowClick = onShowClick,
                onPlayClick = onPlayClick,
                // onShowLongPress temporarily removed
                // onShowLongPress = onShowLongPress,
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
            text = "Add some shows to get started. In stub mode, use \"Populate Test Data\" to load realistic test data for UI development.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test buttons to verify stub integration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTestStubs.populateTestData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Populate Test Data")
            }
            
            OutlinedButton(
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

/**
 * Sort options bottom sheet following the old library pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsBottomSheet(
    currentSortOption: SortOption,
    currentSortDirection: SortDirection,
    onSortOptionSelected: (SortOption, SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Sort library by",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Sort by options
            SortOption.values().forEach { option ->
                Column {
                    // Sort option header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Direction options for this sort type
                    SortDirection.values().forEach { direction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onSortOptionSelected(option, direction)
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSortOption == option && currentSortDirection == direction,
                                onClick = { 
                                    onSortOptionSelected(option, direction) 
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = direction.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    if (option != SortOption.values().last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
            
            // Bottom padding for gesture area
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Apply filtering and sorting to the shows list
 */
private fun applyFiltersAndSorting(
    shows: List<Show>,
    filterPath: FilterPath,
    sortBy: SortOption,
    sortDirection: SortDirection
): List<Show> {
    // Step 1: Apply decade and seasonal filtering
    val filteredShows = if (filterPath.isNotEmpty) {
        val selectedDecadeNode = filterPath.nodes.firstOrNull()
        val selectedSeasonNode = filterPath.nodes.getOrNull(1) // Second level is season
        
        if (selectedDecadeNode != null) {
            shows.filter { show ->
                // Parse year from show data
                val year = show.year?.toIntOrNull() ?: 0
                
                // First filter by decade
                val decadeMatches = when (selectedDecadeNode.id) {
                    "60s" -> year in 1960..1969
                    "70s" -> year in 1970..1979
                    "80s" -> year in 1980..1989
                    "90s" -> year in 1990..1999
                    else -> true // Show all if unknown decade
                }
                
                // If decade matches and we have a season filter, also check season
                if (decadeMatches && selectedSeasonNode != null) {
                    val month = extractMonthFromDate(show.date)
                    if (month != null) {
                        when (selectedSeasonNode.id.substringAfter("_")) { // Extract season from ID like "70s_spring"
                            "spring" -> month in 3..5   // March, April, May
                            "summer" -> month in 6..8   // June, July, August  
                            "fall" -> month in 9..11    // September, October, November
                            "winter" -> month == 12 || month in 1..2  // December, January, February
                            else -> true
                        }
                    } else {
                        true // If we can't parse month, include the show
                    }
                } else {
                    decadeMatches
                }
            }
        } else {
            shows
        }
    } else {
        shows // No filter applied
    }
    
    // Step 2: Apply sorting
    val sortedShows = when (sortBy) {
        SortOption.DATE_OF_SHOW -> {
            if (sortDirection == SortDirection.ASCENDING) {
                filteredShows.sortedBy { it.date }
            } else {
                filteredShows.sortedByDescending { it.date }
            }
        }
        SortOption.DATE_ADDED -> {
            if (sortDirection == SortDirection.ASCENDING) {
                filteredShows.sortedBy { it.addedToLibraryAt ?: 0L }
            } else {
                filteredShows.sortedByDescending { it.addedToLibraryAt ?: 0L }
            }
        }
    }
    
    return sortedShows
}

/**
 * Extract month number from date string (YYYY-MM-DD format)
 * @param date Date string in YYYY-MM-DD format
 * @return Month number (1-12) or null if parsing fails
 */
private fun extractMonthFromDate(date: String): Int? {
    return try {
        val parts = date.split("-")
        if (parts.size >= 2) {
            parts[1].toIntOrNull()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

