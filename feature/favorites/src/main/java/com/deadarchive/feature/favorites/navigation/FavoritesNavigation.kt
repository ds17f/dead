package com.deadarchive.feature.favorites.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.favorites.FavoritesScreen

fun NavGraphBuilder.favoritesScreen(
    onNavigateToPlayer: () -> Unit
) {
    composable("favorites") {
        FavoritesScreen(
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}