package com.deadly.feature.browse

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadly.core.model.Show
import com.deadly.core.settings.SettingsViewModel
import com.deadly.feature.browse.debug.HomeV2DebugDataFactory
import com.deadly.core.design.component.DebugActivator
import com.deadly.core.design.component.DebugBottomSheet
import com.deadly.core.design.component.HierarchicalFilter
import com.deadly.core.design.component.FilterPath
import com.deadly.core.design.component.FilterTrees
import com.deadly.core.design.component.IconResources
import com.deadly.core.design.component.V2TopBar

/**
 * HomeV2Screen - V2 implementation of the home/browse interface
 * 
 * This is the main landing screen of the app, featuring:
 * - Material3 design system
 * - V2 UI-first development approach
 * - Debug integration for development
 * - Foundation for future UI enhancements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeV2Screen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    initialEra: String? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Filter state management
    var filterPath by remember { mutableStateOf(FilterPath()) }
    
    // Debug state management
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        HomeV2DebugDataFactory.createDebugData(uiState, initialEra)
    } else null

    Scaffold(
        topBar = {
            V2TopBar(
                titleContent = {
                    HierarchicalFilter(
                        filterTree = FilterTrees.buildHomeFiltersTree(),
                        selectedPath = filterPath,
                        onSelectionChanged = { filterPath = it }
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to settings */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Recent Shows Grid Section
                item {
                    RecentShowsGrid(
                        shows = uiState.recentShows,
                        onShowClick = { show -> onNavigateToShow(show) },
                        onShowLongPress = { show -> 
                            // TODO: Implement context menu
                        }
                    )
                }
                
                // Today In Grateful Dead History Section
                item {
                    val todayItems = uiState.todayInHistory.map { show ->
                        HorizontalCollectionItem(
                            id = show.showId,
                            displayText = "${show.date}\n${show.location ?: "Unknown Location"}",
                            type = CollectionItemType.SHOW
                        )
                    }
                    
                    HorizontalCollection(
                        title = "Today In Grateful Dead History",
                        items = todayItems,
                        onItemClick = { item ->
                            // Find the show and navigate
                            val show = uiState.todayInHistory.find { it.showId == item.id }
                            show?.let { onNavigateToShow(it) }
                        }
                    )
                }
                
                // Explore Collections Section
                item {
                    val collectionItems = uiState.exploreCollections.map { category ->
                        HorizontalCollectionItem(
                            id = category.lowercase().replace(" ", "_"),
                            displayText = category,
                            type = CollectionItemType.COLLECTION_CATEGORY
                        )
                    }
                    
                    HorizontalCollection(
                        title = "Explore Collections",
                        items = collectionItems,
                        onItemClick = { item ->
                            // TODO: Navigate to collection view
                        }
                    )
                }
            }
            
            // Debug activator when debug mode is enabled
            if (settings.showDebugInfo && debugData != null) {
                DebugActivator(
                    isVisible = true,
                    onClick = { showDebugPanel = true },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
    
    // Debug Bottom Sheet
    debugData?.let { data ->
        DebugBottomSheet(
            debugData = data,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}

@Composable
private fun HomeV2WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to HomeV2 🚀",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "This is the redesigned home interface built using V2 architecture patterns. " +
                      "The UI-first development approach allows for rapid iteration and professional design.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HomeV2DevelopmentCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "V2 Development Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "✅ Foundation Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "✅ Settings Integration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "✅ Navigation Routing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "✅ Debug Integration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "🔄 Next: UI Component Development",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun HomeV2FoundationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Architecture Foundation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "• Material3 Design System",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "• Service-Oriented Architecture",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "• Debug Integration Ready",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "• Feature Flag Controlled",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Ready for UI-first development iterations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Recent Shows Grid - 2x8 layout showing recently played shows
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentShowsGrid(
    shows: List<Show>,
    onShowClick: (Show) -> Unit,
    onShowLongPress: (Show) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .height(268.dp) // 4 rows × 64dp card height + spacing
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false // Disable scrolling to hold its own
    ) {
        items(shows.take(8)) { show -> // 2x4 = 8 horizontal cards
            RecentShowCard(
                show = show,
                onShowClick = { onShowClick(show) },
                onShowLongPress = { onShowLongPress(show) }
            )
        }
    }
}

/**
 * Individual show card for recent shows grid
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentShowCard(
    show: Show,
    onShowClick: () -> Unit,
    onShowLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Reduced height for compact layout
            .combinedClickable(
                onClick = onShowClick,
                onLongClick = onShowLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp), // Minimal padding - just a few pixels
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover placeholder
            Box(
                modifier = Modifier
                    .size(56.dp) // Increased from 48dp
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp), // Increased from 20dp
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Show metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Date
                Text(
                    text = show.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Location
                Text(
                    text = show.location ?: "Unknown Location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // TODO: Add progress indicator when playback progress data is available
        }
    }
}

/**
 * Reusable horizontal collection component for large square images
 */
@Composable
private fun HorizontalCollection(
    title: String,
    items: List<HorizontalCollectionItem>,
    onItemClick: (HorizontalCollectionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Horizontal scrolling row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                CollectionItemCard(
                    item = item,
                    onItemClick = { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Individual item in horizontal collection
 */
@Composable
private fun CollectionItemCard(
    item: HorizontalCollectionItem,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onItemClick() },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Large square image
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = IconResources.PlayerControls.AlbumArt(),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Descriptive text
        Text(
            text = item.displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start, // Left justified
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Data class for horizontal collection items
 */
data class HorizontalCollectionItem(
    val id: String,
    val displayText: String,
    val type: CollectionItemType = CollectionItemType.SHOW
)

enum class CollectionItemType {
    SHOW, COLLECTION_CATEGORY
}

