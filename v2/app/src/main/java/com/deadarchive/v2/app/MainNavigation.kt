package com.deadarchive.v2.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.deadarchive.v2.feature.splash.navigation.splashGraph
import com.deadarchive.v2.feature.home.navigation.homeGraph
import com.deadarchive.v2.feature.search.navigation.searchGraph

/**
 * MainNavigation - Scalable navigation architecture for V2 app
 * 
 * This is the main navigation coordinator that orchestrates routing between
 * all feature modules. Each feature owns its own navigation subgraph,
 * maintaining clean separation of concerns.
 * 
 * Navigation Flow:
 * 1. splash → home (after database initialization) 
 * 2. home → search-graph (user taps search)
 * 3. search → search-results (user taps search box)
 * 4. search-results → search (back navigation)
 * 
 * Architecture Benefits:
 * - Scalable: Easy to add new feature subgraphs
 * - Modular: Each feature manages its own navigation
 * - Testable: Features accept navigation callbacks
 * - Clean: App module stays minimal and focused
 */
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash feature - handles V2 database initialization
        splashGraph(navController)
        
        // Home feature - main navigation hub
        homeGraph(navController)
        
        // Search feature - search and browse functionality
        searchGraph(navController)
    }
}