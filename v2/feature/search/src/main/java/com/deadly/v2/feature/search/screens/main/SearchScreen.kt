package com.deadly.v2.feature.search.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadly.v2.core.design.component.debug.DebugActivator
import com.deadly.v2.core.design.component.debug.DebugBottomSheet
import com.deadly.v2.core.design.component.debug.DebugData
import com.deadly.v2.core.design.component.debug.DebugSection
import com.deadly.v2.core.design.component.debug.DebugItem
import com.deadly.v2.core.design.resources.IconResources
import com.deadly.v2.feature.search.screens.main.models.SearchViewModel
import com.deadly.v2.core.model.SearchUiState

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
 * SearchScreen - Next-generation search and discovery interface
 * 
 * This is the V2 implementation of the search/browse experience following
 * the V2 architecture pattern. Built using UI-first development methodology
 * where the UI drives the discovery of service requirements.
 * 
 * Architecture:
 * - Material3 design system with Search-specific enhancements
 * - Debug integration following PlayerV2 patterns
 * - Feature flag enabled foundation ready for UI development
 * - Clean navigation callbacks matching V1 interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (String) -> Unit,
    onNavigateToSearchResults: () -> Unit,
    initialEra: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Debug panel state - hard-coded to true for V2
    var showDebugPanel by remember { mutableStateOf(false) }
    
    // QR Scanner coming soon dialog state
    var showQrComingSoonDialog by remember { mutableStateOf(false) }
    
    // Simplified debug data
    val debugData = collectSearchDebugData(
        uiState = uiState,
        initialEra = initialEra
    )
    
    // Simple content layout with debug overlay like HomeScreen and SettingsScreen
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 2: Search box
            item {
                SearchSearchBox(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChanged,
                    onFocusReceived = onNavigateToSearchResults,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Row 3 & 4: Browse by decades
            item {
                SearchBrowseSection(
                    onDecadeClick = { era -> /* TODO: Handle decade browse */ }
                )
            }
            
            // Row 5 & 6: Discover section
            item {
                SearchDiscoverSection(
                    onDiscoverClick = { item -> /* TODO: Handle discover */ }
                )
            }
            
            // Row 7 & 8: Browse All section
            item {
                SearchBrowseAllSection(
                    onBrowseAllClick = { item -> /* TODO: Handle browse all */ }
                )
            }
        }
        
        // Debug activator overlay (always enabled in V2)
        DebugActivator(
            isVisible = true,
            onClick = { showDebugPanel = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
    
    // Debug bottom sheet
    DebugBottomSheet(
        debugData = debugData,
        isVisible = showDebugPanel,
        onDismiss = { showDebugPanel = false }
    )
    
    // QR Scanner coming soon dialog
    if (showQrComingSoonDialog) {
        QrScannerComingSoonDialog(
            onDismiss = { showQrComingSoonDialog = false }
        )
    }
}

/**
 * Collect debug data for SearchScreen
 * Following the established PlayerV2 debug data pattern
 */
@Composable
private fun collectSearchDebugData(
    uiState: SearchUiState,
    initialEra: String?
): DebugData {
    return DebugData(
        screenName = "SearchScreen",
        sections = listOf(
            DebugSection(
                title = "Search State",
                items = listOf(
                    DebugItem.KeyValue("Is Loading", uiState.isLoading.toString()),
                    DebugItem.KeyValue("Error State", uiState.error ?: "None"),
                    DebugItem.KeyValue("Initial Era", initialEra ?: "None"),
                    DebugItem.KeyValue("Feature Flag", "useSearchV2 = true"),
                    DebugItem.KeyValue("Scaffold Mode", "Pure Content (MainNavigation AppScaffold)")
                )
            ),
            DebugSection(
                title = "Development Status",
                items = listOf(
                    DebugItem.KeyValue("Implementation", "Foundation Complete"),
                    DebugItem.KeyValue("UI State", "Basic scaffold ready"),
                    DebugItem.KeyValue("Navigation", "Feature flag routing active"),
                    DebugItem.KeyValue("Next Phase", "UI-first development"),
                    DebugItem.KeyValue("Navigation", "Integrated with MainNavigation scaffold system")
                )
            )
        )
    )
}


/**
 * Row 2: Search box with search icon and placeholder text
 */
@Composable
private fun SearchSearchBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFocusReceived: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Navigate to search results when field receives focus
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusReceived()
        }
    }
    
    OutlinedTextField(
        value = "", // Always empty so placeholder always shows
        onValueChange = { /* No-op - this is just a button */ },
        interactionSource = interactionSource,
        readOnly = true, // Make it clear this isn't for typing
        placeholder = { 
            Text(
                text = "What do you want to listen to?",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        //shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Rows 3 & 4: Browse section with 2x2 decade grid
 */
@Composable
private fun SearchBrowseSection(
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
            // Background alt logo image (right justified)
            Image(
                painter = painterResource(com.deadly.v2.core.design.R.drawable.alt_logo),
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
private fun SearchDiscoverSection(
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
private fun SearchBrowseAllSection(
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

/**
 * QR Scanner Coming Soon Dialog
 * Shows when user taps camera icon before QrScannerV2 is implemented
 */
@Composable
private fun QrScannerComingSoonDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR Scanner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "QR code scanning is coming soon!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "This feature will let you scan QR codes to instantly discover and play Grateful Dead recordings shared by other users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        icon = {
            Icon(
                painter = IconResources.Content.QrCodeScanner(),
                contentDescription = "QR Code Scanner",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    )
}