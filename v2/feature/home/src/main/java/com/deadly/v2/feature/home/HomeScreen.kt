package com.deadly.v2.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deadly.v2.core.theme.api.ThemeAssets

/**
 * HomeScreen - Simple welcome screen for V2 app
 * 
 * Clean welcome screen featuring the app logo, name, and tagline.
 * Scaffold-free content designed for use within AppScaffold.
 */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main app icon from theme system
        Image(
            painter = ThemeAssets.current.primaryLogo(),
            contentDescription = "Deadly Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // App name
        Text(
            text = "Deadly",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Tagline with emojis
        Text(
            text = "The Killer App For the Golden Road",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}