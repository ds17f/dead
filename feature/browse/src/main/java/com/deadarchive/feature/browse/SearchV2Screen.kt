package com.deadarchive.feature.browse

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Show
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.core.design.component.DebugData
import com.deadarchive.core.design.component.DebugSection
import com.deadarchive.core.design.component.DebugItem
import com.deadarchive.core.settings.SettingsViewModel

// Data classes for UI components
data class DecadeBrowse(
    val title: String,
    val gradient: List<Color>,
    val era: String
)

data class DiscoverItem(
    val title: String,
    val subtitle: String = ""
)

data class BrowseAllItem(
    val title: String,
    val subtitle: String,
    val searchQuery: String
)

/**
 * SearchV2Screen - Next-generation search and discovery interface
 * 
 * This is the V2 implementation of the search/browse experience following
 * the V2 architecture pattern. Built using UI-first development methodology
 * where the UI drives the discovery of service requirements.
 * 
 * Architecture:
 * - Material3 design system with SearchV2-specific enhancements
 * - Debug integration following PlayerV2 patterns
 * - Feature flag enabled foundation ready for UI development
 * - Clean navigation callbacks matching V1 interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchV2Screen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    initialEra: String? = null,
    viewModel: SearchV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Debug panel state - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectSearchV2DebugData(uiState, initialEra)
    } else {
        null
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Row 1: Top bar with SYF, Search title, and camera icon
            item {
                SearchV2TopBar(onCameraClick = { 
                    // TODO: Implement QR code scanner
                    // Will scan Archive.org URLs and navigate appropriately
                })
            }
            
            // Row 2: Search box
            item {
                SearchV2SearchBox(
                    searchQuery = "",
                    onSearchQueryChange = { /* TODO: Handle search */ },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Row 3 & 4: Browse by decades
            item {
                SearchV2BrowseSection(
                    onDecadeClick = { era -> /* TODO: Handle decade browse */ }
                )
            }
            
            // Row 5 & 6: Discover section
            item {
                SearchV2DiscoverSection(
                    onDiscoverClick = { item -> /* TODO: Handle discover */ }
                )
            }
            
            // Row 7 & 8: Browse All section
            item {
                SearchV2BrowseAllSection(
                    onBrowseAllClick = { item -> /* TODO: Handle browse all */ }
                )
            }
        }
        
        // Debug activator (conditional rendering based on settings)
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
    
    // Debug bottom sheet
    debugData?.let { data ->
        DebugBottomSheet(
            debugData = data,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}

/**
 * Collect debug data for SearchV2Screen
 * Following the established PlayerV2 debug data pattern
 */
@Composable
private fun collectSearchV2DebugData(
    uiState: SearchV2UiState,
    initialEra: String?
): DebugData {
    return DebugData(
        screenName = "SearchV2Screen",
        sections = listOf(
            DebugSection(
                title = "SearchV2 State",
                items = listOf(
                    DebugItem.KeyValue("Is Loading", uiState.isLoading.toString()),
                    DebugItem.KeyValue("Error State", uiState.error ?: "None"),
                    DebugItem.KeyValue("Initial Era", initialEra ?: "None"),
                    DebugItem.KeyValue("Feature Flag", "useSearchV2 = true")
                )
            ),
            DebugSection(
                title = "Development Status",
                items = listOf(
                    DebugItem.KeyValue("Implementation", "Foundation Complete"),
                    DebugItem.KeyValue("UI State", "Basic scaffold ready"),
                    DebugItem.KeyValue("Navigation", "Feature flag routing active"),
                    DebugItem.KeyValue("Next Phase", "UI-first development")
                )
            )
        )
    )
}

/**
 * Row 1: Top bar with Steal Your Face logo, Search title, and camera icon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchV2TopBar(
    onCameraClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: SYF logo + Search title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                    contentDescription = "Dead Archive",
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Right side: Camera icon for QR code scanning
            IconButton(onClick = onCameraClick) {
                Icon(
                    painter = IconResources.Content.QrCodeScanner(),
                    contentDescription = "QR Code Scanner",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Row 2: Search box with search icon and placeholder text
 */
@Composable
private fun SearchV2SearchBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { 
            Text(
                text = "What do you want to listen to",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

/**
 * Rows 3 & 4: Browse section with 2x2 decade grid
 */
@Composable
private fun SearchV2BrowseSection(
    onDecadeClick: (String) -> Unit
) {
    val decades = listOf(
        DecadeBrowse("1960s", listOf(Color(0xFF1976D2), Color(0xFF42A5F5)), "1960s"),
        DecadeBrowse("1970s", listOf(Color(0xFF388E3C), Color(0xFF66BB6A)), "1970s"),
        DecadeBrowse("1980s", listOf(Color(0xFFD32F2F), Color(0xFFEF5350)), "1980s"),
        DecadeBrowse("1990s", listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)), "1990s")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Start Browsing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(180.dp) // Fixed height for 2x2 grid
        ) {
            items(decades) { decade ->
                DecadeCard(
                    decade = decade,
                    onClick = { onDecadeClick(decade.era) }
                )
            }
        }
    }
}

/**
 * Individual decade card component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecadeCard(
    decade: DecadeBrowse,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(decade.gradient),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Background SYF image (right justified)
            Image(
                painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                alpha = 0.3f
            )
            
            // Decade text
            Text(
                text = decade.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Rows 5 & 6: Discover section
 */
@Composable
private fun SearchV2DiscoverSection(
    onDiscoverClick: (DiscoverItem) -> Unit
) {
    val discoverItems = listOf(
        DiscoverItem("Discover 1"),
        DiscoverItem("Discover 2"),
        DiscoverItem("Discover 3")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Discover Something New",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            discoverItems.forEach { item ->
                DiscoverCard(
                    item = item,
                    onClick = { onDiscoverClick(item) },
                    modifier = Modifier.weight(1f) // Each card takes equal width
                )
            }
        }
    }
}

/**
 * Individual discover card component - taller design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverCard(
    item: DiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp), // Flexible width, tall height
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Rows 7 & 8: Browse All section with 2-column grid
 */
@Composable
private fun SearchV2BrowseAllSection(
    onBrowseAllClick: (BrowseAllItem) -> Unit
) {
    val browseAllItems = listOf(
        BrowseAllItem("Popular Shows", "Most listened to concerts", "popular"),
        BrowseAllItem("Recent Uploads", "Latest additions to Archive.org", "recent"),
        BrowseAllItem("Top Rated", "Highest community ratings", "top-rated"),
        BrowseAllItem("Audience Recordings", "Taped from the crowd", "audience"),
        BrowseAllItem("Soundboard", "Direct from the mixing board", "soundboard"),
        BrowseAllItem("Live Albums", "Official releases", "official")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Browse All",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(400.dp) // Fixed height for demonstration
        ) {
            items(browseAllItems) { item ->
                BrowseAllCard(
                    item = item,
                    onClick = { onBrowseAllClick(item) }
                )
            }
        }
    }
}

/**
 * Individual browse all card component (2x height of browse cards)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseAllCard(
    item: BrowseAllItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // 2x the height of decade cards
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}