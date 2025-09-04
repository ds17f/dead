package com.deadly.v2.feature.collections.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deadly.v2.core.design.component.CollectionCard
import com.deadly.v2.core.design.component.FeaturedCollectionsCarousel
import com.deadly.v2.core.design.component.LargeCollectionsCarousel
import com.deadly.v2.core.design.component.HierarchicalFilter
import com.deadly.v2.core.design.component.FilterTrees
import com.deadly.v2.core.design.component.debug.DebugActivator
import com.deadly.v2.core.design.component.debug.DebugBottomSheet
import com.deadly.v2.feature.collections.screens.main.models.CollectionsViewModel
import com.deadly.v2.feature.collections.screens.main.components.CollectionShowCard

/**
 * CollectionsScreen - Main screen for browsing curated collections
 * 
 * V2 implementation featuring:
 * - Featured collections grid
 * - Collection browsing and search
 * - Debug integration for development
 * 
 * Scaffold-free content designed for use within AppScaffold.
 * Follows V2 architecture with CollectionsService integration.
 */
@Composable
fun CollectionsScreen(
    onNavigateToCollection: (String) -> Unit = {},
    onNavigateToShow: (String) -> Unit = {},
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val featuredCollections by viewModel.featuredCollections.collectAsStateWithLifecycle()
    val filterPath by viewModel.filterPath.collectAsStateWithLifecycle()
    val filteredCollections by viewModel.filteredCollections.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    
    // Debug panel state
    var showDebugPanel by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hierarchical Filter for collections - at the very top
            item {
                HierarchicalFilter(
                    filterTree = FilterTrees.buildCollectionsTagsTree(),
                    selectedPath = filterPath,
                    onSelectionChanged = viewModel::onFilterPathChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Featured Collections section - temporarily hidden
            // TODO: Re-enable featured collections later
            /* 
            if (featuredCollections.isNotEmpty()) {
                item {
                    Text(
                        text = "Featured Collections",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                item {
                    FeaturedCollectionsCarousel(
                        collections = featuredCollections,
                        onCollectionClick = onNavigateToCollection
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Loading Collections...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            */
            
            // Large Collections Carousel
            if (filteredCollections.isNotEmpty()) {
                item {
                    val headerText = when {
                        filterPath.isEmpty -> "All Collections (${filteredCollections.size})"
                        filterPath.nodes.size == 1 -> {
                            when (filterPath.nodes.first().id) {
                                "official" -> "Official Collections (${filteredCollections.size})"
                                "guest" -> "Guest Collections (${filteredCollections.size})"
                                "era" -> "Era Collections (${filteredCollections.size})"
                                else -> "${filterPath.nodes.first().label} Collections (${filteredCollections.size})"
                            }
                        }
                        filterPath.nodes.size == 2 -> {
                            // Official subcategory selected
                            "${filterPath.nodes.last().label} (${filteredCollections.size})"
                        }
                        else -> "Collections (${filteredCollections.size})"
                    }
                    
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                item {
                    LargeCollectionsCarousel(
                        collections = filteredCollections,
                        onCollectionSelected = viewModel::onCollectionSelected,
                        onCollectionClick = onNavigateToCollection,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Shows section for selected collection
            selectedCollection?.let { collection ->
                if (collection.shows.isNotEmpty()) {
                    item {
                        Text(
                            text = "Shows (${collection.shows.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(collection.shows) { show ->
                        CollectionShowCard(
                            show = show,
                            onClick = { onNavigateToShow(show.id) }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No shows available for this collection",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Debug activator
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
        debugData = collectCollectionsDebugData(uiState, featuredCollections),
        isVisible = showDebugPanel,
        onDismiss = { showDebugPanel = false }
    )
}


/**
 * Collect debug data for Collections screen
 */
private fun collectCollectionsDebugData(
    uiState: com.deadly.v2.feature.collections.screens.main.models.CollectionsUiState,
    featuredCollections: List<com.deadly.v2.core.model.DeadCollection>
): com.deadly.v2.core.design.component.debug.DebugData {
    return com.deadly.v2.core.design.component.debug.DebugData(
        screenName = "Collections",
        sections = listOf(
            com.deadly.v2.core.design.component.debug.DebugSection(
                title = "UI State",
                items = listOf(
                    com.deadly.v2.core.design.component.debug.DebugItem.KeyValue("Collections Count", featuredCollections.size.toString()),
                    com.deadly.v2.core.design.component.debug.DebugItem.KeyValue("Is Loading", uiState.isLoading.toString()),
                    com.deadly.v2.core.design.component.debug.DebugItem.KeyValue("Error", uiState.error ?: "None"),
                    com.deadly.v2.core.design.component.debug.DebugItem.KeyValue("Search Query", uiState.searchQuery.ifEmpty { "None" })
                )
            ),
            com.deadly.v2.core.design.component.debug.DebugSection(
                title = "Featured Collections",
                items = featuredCollections.map { collection ->
                    com.deadly.v2.core.design.component.debug.DebugItem.KeyValue(
                        collection.name,
                        "${collection.shows.size} shows, tags: ${collection.tags.joinToString(", ")}"
                    )
                }
            )
        )
    )
}