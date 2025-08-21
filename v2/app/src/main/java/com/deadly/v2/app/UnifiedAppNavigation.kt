package com.deadly.v2.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.deadly.v2.app.navigation.BottomNavDestination
import com.deadly.v2.app.navigation.NavigationBarConfig
import com.deadly.v2.core.design.scaffold.AppScaffold
import com.deadly.v2.feature.home.HomeScreen

/**
 * UnifiedAppNavigation - New AppScaffold-based navigation architecture
 * 
 * This replaces MainAppScreen and uses AppScaffold as the unified layout controller.
 * AppScaffold now handles both top and bottom navigation based on route configuration,
 * eliminating scaffold nesting and padding conflicts.
 * 
 * Architecture:
 * - Single AppScaffold handles all layout concerns
 * - Route-based bar configuration from feature modules
 * - Bottom navigation integrated into AppScaffold
 * - Content screens are just content (no scaffolds)
 */
@Composable
fun UnifiedAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Get bar configuration based on current route
    val barConfig = NavigationBarConfig.getBarConfig(currentRoute)
    
    AppScaffold(
        topBarConfig = barConfig.topBar,
        bottomBarConfig = barConfig.bottomBar,
        bottomNavigationContent = if (barConfig.bottomBar?.visible == true) {
            {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        } else null,
        onNavigationClick = {
            navController.popBackStack()
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home screen - welcome screen with app branding
            composable(BottomNavDestination.Home.route) {
                HomeScreen()
            }
            
            // Search navigation graph - nested navigation for search flow
            navigation(
                startDestination = "search-main",
                route = BottomNavDestination.Search.route
            ) {
                composable("search-main") {
                    // TODO: Use scaffold-free SearchContent when we extract it
                    // For now, temporary content to avoid double scaffolding
                    SearchContentPlaceholder(
                        onNavigateToSearchResults = {
                            navController.navigate("search-results")
                        }
                    )
                }
                
                composable("search-results") {
                    // TODO: Use scaffold-free SearchResultsContent
                    SearchResultsContentPlaceholder(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            
            // Library screen - placeholder
            composable(BottomNavDestination.Library.route) {
                LibraryContentPlaceholder()
            }
            
            // Settings screen - scaffold-free content
            composable(BottomNavDestination.Settings.route) {
                SettingsContentPlaceholder()
            }
        }
    }
}

/**
 * Bottom navigation bar component (extracted from MainAppScreen)
 */
@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onNavigateToDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavDestination.destinations.forEach { destination ->
                BottomNavItem(
                    destination = destination,
                    isSelected = currentRoute == destination.route,
                    onClick = { onNavigateToDestination(destination.route) }
                )
            }
        }
    }
}

/**
 * Individual bottom navigation item (extracted from MainAppScreen)
 */
@Composable
private fun BottomNavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = if (isSelected) destination.selectedIcon() else destination.unselectedIcon(),
            contentDescription = destination.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = destination.title,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Temporary placeholder content screens (to avoid double scaffolding)
// These will be replaced with extracted content from the real screens


@Composable
private fun SearchContentPlaceholder(
    onNavigateToSearchResults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToSearchResults
        ) {
            Text(
                text = "What do you want to listen to?",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = "Search content will be extracted from SearchScreen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultsContentPlaceholder(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Search Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Results content will be extracted from SearchResultsScreen",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LibraryContentPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Library Coming Soon",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun SettingsContentPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Theme")
                }
            }
        }
    }
}