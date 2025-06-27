package com.deadarchive.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BrowseScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Archive.org API Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Test buttons
        Button(
            onClick = { viewModel.testSearchConcerts() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Search Concerts")
        }
        
        Button(
            onClick = { viewModel.testPopularConcerts() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Popular Concerts")
        }
        
        Button(
            onClick = { viewModel.testRecentConcerts() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Recent Concerts")
        }
        
        // Results display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "API Test Results:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                when (val state = uiState) {
                    is BrowseUiState.Loading -> {
                        Text("Loading...")
                    }
                    is BrowseUiState.Success -> {
                        Text(
                            text = state.data,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is BrowseUiState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is BrowseUiState.Idle -> {
                        Text("Click a button to test the API")
                    }
                }
            }
        }
    }
}