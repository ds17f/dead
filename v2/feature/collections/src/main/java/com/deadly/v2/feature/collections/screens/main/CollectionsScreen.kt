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
import com.deadly.v2.core.design.component.debug.DebugActivator
import com.deadly.v2.core.design.component.debug.DebugBottomSheet
import com.deadly.v2.feature.collections.screens.main.models.CollectionsViewModel

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
    
    // Debug panel state
    var showDebugPanel by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Collections",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Curated collections of Grateful Dead shows including Dick's Picks, Europe '72, and more",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Featured Collections section
            if (featuredCollections.isNotEmpty()) {
                item {
                    Text(
                        text = "Featured Collections",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(featuredCollections) { collection ->
                    CollectionCard(
                        collection = collection,
                        onClick = { onNavigateToCollection(collection.id) }
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