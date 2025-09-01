package com.deadly.v2.feature.home.screens.main

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
import com.deadly.v2.core.api.home.Collection
import com.deadly.v2.core.design.component.debug.DebugActivator
import com.deadly.v2.core.design.component.debug.DebugBottomSheet
import com.deadly.v2.core.design.component.debug.DebugData
import com.deadly.v2.core.design.component.debug.DebugSection
import com.deadly.v2.core.design.component.debug.DebugItem
import com.deadly.v2.core.design.resources.IconResources
import com.deadly.v2.core.model.Show

/**
 * HomeScreen - Rich home interface with content discovery
 * 
 * V2 implementation featuring:
 * - Recent Shows Grid (2x4 layout)
 * - Today In Grateful Dead History (horizontal scroll)
 * - Featured Collections (horizontal scroll)
 * - Debug integration for development
 * 
 * Scaffold-free content designed for use within AppScaffold.
 * Follows V2 architecture with single HomeService dependency.
 */
@Composable
fun HomeScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Debug panel state - always enabled in V2
    var showDebugPanel by remember { mutableStateOf(false) }
    
    // Debug data collection
    val debugData = collectHomeDebugData(
        uiState = uiState,
        onRefresh = viewModel::refresh
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Recent Shows Grid Section
            item {
                RecentShowsGrid(
                    shows = uiState.homeContent.recentShows,
                    onShowClick = onNavigateToShow,
                    onShowLongPress = { show -> 
                        // TODO: Implement context menu
                    }
                )
            }
            
            // Today In Grateful Dead History Section
            item {
                val todayItems = uiState.homeContent.todayInHistory.map { show ->
                    HorizontalCollectionItem(
                        id = show.id,
                        displayText = "${show.date}\n${show.location.displayText}",
                        type = CollectionItemType.SHOW
                    )
                }
                
                HorizontalCollection(
                    title = "Today In Grateful Dead History",
                    items = todayItems,
                    onItemClick = { item ->
                        // Find the show and navigate
                        val show = uiState.homeContent.todayInHistory.find { it.id == item.id }
                        show?.let { onNavigateToShow(it.id) }
                    }
                )
            }
            
            // Featured Collections Section
            item {
                val collectionItems = uiState.homeContent.featuredCollections.map { collection ->
                    HorizontalCollectionItem(
                        id = collection.id,
                        displayText = "${collection.name}\n${collection.showCount} shows",
                        type = CollectionItemType.COLLECTION
                    )
                }
                
                HorizontalCollection(
                    title = "Featured Collections",
                    items = collectionItems,
                    onItemClick = { item ->
                        // TODO: Navigate to collection view
                    }
                )
            }
        }
        
        // Debug activator button (always visible in V2)
        DebugActivator(
            isVisible = true,
            onClick = { showDebugPanel = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
    
    // Debug Bottom Sheet
    DebugBottomSheet(
        debugData = debugData,
        isVisible = showDebugPanel,
        onDismiss = { showDebugPanel = false }
    )
}

/**
 * Collect debug data for HomeScreen
 * Following the established V2 debug data pattern
 */
@Composable
private fun collectHomeDebugData(
    uiState: HomeUiState,
    onRefresh: () -> Unit
): DebugData {
    return DebugData(
        screenName = "HomeScreen",
        sections = listOf(
            DebugSection(
                title = "Home State",
                items = listOf(
                    DebugItem.BooleanValue("Is Loading", uiState.isLoading),
                    DebugItem.BooleanValue("Has Error", uiState.hasError),
                    DebugItem.KeyValue("Error", uiState.error ?: "None"),
                    DebugItem.KeyValue("Last Refresh", 
                        if (uiState.homeContent.lastRefresh > 0) 
                            java.text.SimpleDateFormat("HH:mm:ss").format(uiState.homeContent.lastRefresh)
                        else "Never"
                    )
                )
            ),
            DebugSection(
                title = "Content Stats",
                items = listOf(
                    DebugItem.NumericValue("Recent Shows", uiState.homeContent.recentShows.size),
                    DebugItem.NumericValue("Today In History", uiState.homeContent.todayInHistory.size),
                    DebugItem.NumericValue("Collections", uiState.homeContent.featuredCollections.size)
                )
            ),
            DebugSection(
                title = "V2 Architecture",
                items = listOf(
                    DebugItem.KeyValue("Pattern", "Service Orchestration"),
                    DebugItem.KeyValue("Service", "HomeService (Stub)"),
                    DebugItem.KeyValue("Navigation", "Graph-based"),
                    DebugItem.KeyValue("Scaffold", "AppScaffold integrated")
                )
            )
        )
    )
}

/**
 * Recent Shows Grid - 2x4 layout showing recently played shows
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentShowsGrid(
    shows: List<Show>,
    onShowClick: (String) -> Unit,
    onShowLongPress: (Show) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .height(268.dp) // 4 rows × 64dp card height + spacing
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false // Disable scrolling to hold its size
        ) {
            items(shows.take(8)) { show -> // 2x4 = 8 cards
                RecentShowCard(
                    show = show,
                    onShowClick = { onShowClick(show.id) },
                    onShowLongPress = { onShowLongPress(show) }
                )
            }
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
            .height(64.dp)
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
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
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
                    text = show.location.displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            fontWeight = FontWeight.Bold
        )
        
        // Horizontal scrolling row
        LazyRow(
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
            textAlign = TextAlign.Start,
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
    SHOW, COLLECTION
}