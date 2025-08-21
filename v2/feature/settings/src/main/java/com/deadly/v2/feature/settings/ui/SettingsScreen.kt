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

/**
 * SettingsScreen - V2 Settings interface
 * 
 * Simple settings content for theme management and configuration.
 * Scaffold-free content designed for use within MainNavigation's AppScaffold.
 * 
 * Provides access to app configuration options including:
 * - Theme management and import
 * - Future: App preferences, about info, etc.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ClearThemesButton(
                        onClearThemes = viewModel::onClearThemes,
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

/**
 * Button to clear all themes with confirmation dialog
 */
@Composable
private fun ClearThemesButton(
    onClearThemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showConfirmDialog = true },
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.error
        )
    ) {
        Text("Clear All Themes")
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Clear All Themes") },
            text = { 
                Text("This will delete all imported themes and restart the app to restore the default theme. Continue?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearThemes()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}