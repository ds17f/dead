package com.deadly.v2.core.design.scaffold

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.deadly.v2.core.design.component.topbar.TopBar
import com.deadly.v2.core.design.component.topbar.TopBarMode

/**
 * AppScaffold - Unified scaffold for all V2 screens
 * 
 * This component encapsulates all screen layout concerns and provides a consistent
 * API for V2 screens. It automatically handles status bar insets based on the
 * TopBar mode and provides proper padding for content.
 * 
 * Features:
 * - Automatic WindowInsets handling based on TopBar mode
 * - Unified API for all V2 screen layouts  
 * - Support for optional TopBar, bottom navigation
 * - Spotify-style status bar handling (SOLID/IMMERSIVE modes)
 * 
 * @param modifier Modifier for the scaffold
 * @param topBarMode How to handle status bar interaction (null = no top bar)
 * @param topBarTitle Title to display in the top bar
 * @param topBarNavigationIcon Optional navigation icon (typically back arrow)
 * @param topBarActions Optional action buttons
 * @param onNavigationClick Callback for navigation icon clicks
 * @param showBottomNav Whether to show bottom navigation (future feature)
 * @param content The main content of the screen
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    // Top bar configuration
    topBarMode: TopBarMode? = null, // null = no top bar
    topBarTitle: String = "",
    topBarNavigationIcon: @Composable (() -> Unit)? = null,
    topBarActions: @Composable (RowScope.() -> Unit) = {},
    onNavigationClick: (() -> Unit)? = null,
    // Bottom navigation
    showBottomNav: Boolean = false,
    // Content
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = if (topBarMode == TopBarMode.SOLID) {
            {
                TopBar(
                    title = topBarTitle,
                    mode = topBarMode,
                    navigationIcon = topBarNavigationIcon,
                    actions = topBarActions,
                    onNavigationClick = onNavigationClick
                )
            }
        } else {
            // IMMERSIVE mode or null - no TopBar rendered
            // In IMMERSIVE mode, content flows behind status bar with underlay
            {}
        },
        bottomBar = if (showBottomNav) {
            { 
                // TODO: V2 Bottom Navigation when we implement it
                // For now, empty placeholder
            }
        } else {
            {}
        },
        // Automatically handle window insets based on top bar mode
        contentWindowInsets = when (topBarMode) {
            TopBarMode.SOLID -> WindowInsets.systemBars
            TopBarMode.IMMERSIVE -> WindowInsets(0, 0, 0, 0)
            null -> WindowInsets.systemBars // No top bar, normal system insets
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}