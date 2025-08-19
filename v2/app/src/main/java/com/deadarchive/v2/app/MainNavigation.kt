package com.deadarchive.v2.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deadarchive.v2.feature.search.ui.components.SearchScreen

/**
 * Main navigation entry point for the V2 Dead Archive app.
 * 
 * This is a complete, independent V2 application that does not depend on 
 * or delegate to any V1 components. Now testing the V2 SearchScreen.
 */
@Composable
fun MainNavigation() {
    // Test V2 SearchScreen
    SearchScreen(
        onNavigateToPlayer = { recordingId ->
            // TODO: Navigate to V2 Player
        },
        onNavigateToShow = { show ->
            // TODO: Navigate to V2 Show details
        },
        onNavigateToSearchResults = {
            // TODO: Navigate to V2 Search results
        },
        initialEra = null
    )
}