package com.deadly.v2.app.navigation

// Re-export core design configuration types for convenience
import com.deadly.v2.core.design.scaffold.BarConfiguration
import com.deadly.v2.core.design.scaffold.TopBarConfig  
import com.deadly.v2.core.design.scaffold.BottomBarConfig
import com.deadly.v2.core.design.scaffold.BottomBarStyle

/**
 * Central route mapping to feature bar configurations
 * 
 * This delegates to feature-specific configuration objects,
 * keeping the actual configurations colocated with their features.
 */
object NavigationBarConfig {
    fun getBarConfig(route: String?): BarConfiguration = when (route) {
        // Home routes
        "home" -> com.deadly.v2.feature.home.HomeBarConfiguration.getHomeBarConfig()
        
        // Search routes - delegate to SearchBarConfiguration
        "search-main" -> com.deadly.v2.feature.search.ui.components.SearchBarConfiguration.getSearchBarConfig()
        "search-results" -> com.deadly.v2.feature.search.ui.components.SearchBarConfiguration.getSearchResultsBarConfig()
        
        // Settings routes
        "settings" -> com.deadly.v2.feature.settings.ui.components.SettingsBarConfiguration.getSettingsBarConfig()
        
        // Library routes
        "library" -> BarConfiguration(
            topBar = null, // Library doesn't need top bar yet
            bottomBar = BottomBarConfig(visible = true)
        )
        
        // Splash and other routes
        "splash" -> BarConfiguration(
            topBar = null,
            bottomBar = BottomBarConfig(visible = false) // Hide bottom nav on splash
        )
        
        // Default configuration
        else -> BarConfiguration(
            topBar = null,
            bottomBar = BottomBarConfig(visible = true)
        )
    }
}