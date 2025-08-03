package com.deadarchive.feature.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Show
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.browse.debug.HomeV2DebugDataFactory
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet

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
    
    // Debug state management
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        HomeV2DebugDataFactory.createDebugData(uiState, initialEra)
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dead Archive V2")
                    }
                },
                windowInsets = WindowInsets(0.dp),
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    HomeV2WelcomeCard()
                }
                
                item {
                    HomeV2DevelopmentCard()
                }
                
                item {
                    HomeV2FoundationCard()
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

