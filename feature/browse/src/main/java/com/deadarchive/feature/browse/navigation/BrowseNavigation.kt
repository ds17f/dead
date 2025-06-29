package com.deadarchive.feature.browse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.browse.BrowseScreen

fun NavGraphBuilder.browseScreen(
    onNavigateToPlayer: (String) -> Unit
) {
    composable("browse") {
        BrowseScreen(
            onNavigateToPlayer = { concert -> onNavigateToPlayer(concert.identifier) }
        )
    }
}