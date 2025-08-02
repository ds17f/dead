package com.deadarchive.feature.browse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "SearchV2 - Coming Soon",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main content - placeholder for future UI development
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SearchV2 Interface",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Next-generation search and discovery experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        if (initialEra != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Initial Era Filter: $initialEra",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "UI-first development foundation ready for implementation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Status information
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Development Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Feature flag integration complete",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "✅ Navigation routing functional",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "✅ Debug system integrated",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "🚧 UI development ready to begin",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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