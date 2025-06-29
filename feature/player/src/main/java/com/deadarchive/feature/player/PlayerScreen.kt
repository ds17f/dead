package com.deadarchive.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    concertId: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (concertId.isNullOrBlank()) {
                "Player Screen - Coming Soon!"
            } else {
                "Player Screen - Coming Soon!\nConcert ID: $concertId"
            }
        )
    }
}