package com.deadly.v2.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deadly.v2.core.design.resources.IconResources
import com.deadly.v2.core.theme.api.ThemeAssets

/**
 * HomeScreen - Main navigation hub for V2 app
 * 
 * This screen provides navigation to all major features:
 * - Search & Browse
 * - Library Management  
 * - Player
 * - Settings
 * 
 * Following navigation-agnostic design patterns,
 * it accepts callbacks rather than handling navigation directly.
 */
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBottomNav: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Logo and title
            Image(
                painter = ThemeAssets.current.primaryLogo(),
                contentDescription = "Deadly Logo",
                modifier = Modifier.size(120.dp)
            )
            
            Text(
                text = "Deadly V2",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Grateful Dead Recordings & Shows",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search button - primary action
                Button(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = IconResources.Content.Search(),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Search & Browse Shows",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                // Library button
                OutlinedButton(
                    onClick = onNavigateToLibrary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = IconResources.Content.LibraryMusic(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "My Library",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                
                // Player button  
                OutlinedButton(
                    onClick = onNavigateToPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = IconResources.PlayerControls.Play(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Player",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                
                // Settings button
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = IconResources.Navigation.Settings(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Version info
            Text(
                text = "Enhanced search • FTS integration • V2 architecture",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}