package com.deadly.v2.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadly.v2.core.design.component.ThemeChooser
import com.deadly.v2.core.design.component.topbar.TopBarDefaults
import com.deadly.v2.core.design.component.topbar.TopBarMode
import com.deadly.v2.core.design.scaffold.AppScaffold

/**
 * SettingsScreen - V2 Settings interface
 * 
 * Provides access to app configuration options including:
 * - Theme management and import
 * - Future: App preferences, about info, etc.
 * 
 * Following V2 architecture patterns with clean separation
 * between UI and business logic via ViewModel.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    AppScaffold(
        topBarMode = TopBarMode.SOLID,
        topBarTitle = "Settings",
        topBarNavigationIcon = TopBarDefaults.BackNavigationIcon(onNavigateBack)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Themes Section
            item {
                SettingsSection(title = "Themes") {
                    ThemeChooser(
                        onThemeImported = viewModel::onThemeImported,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Future sections can be added here
            // item {
            //     SettingsSection(title = "Preferences") {
            //         // App preferences UI
            //     }
            // }
            //
            // item {
            //     SettingsSection(title = "About") {
            //         // About app info
            //     }
            // }
        }
    }
}

/**
 * Reusable settings section component
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            content()
        }
    }
}