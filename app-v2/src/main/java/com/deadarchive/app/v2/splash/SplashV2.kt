package com.deadarchive.app.v2.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.app.v2.model.PhaseV2
import com.deadarchive.core.design.component.IconResources
import kotlinx.coroutines.delay

@Composable
fun SplashV2(
    onSplashComplete: () -> Unit,
    viewModel: SplashViewModelV2 = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigate when ready
    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) {
            delay(1000) // Brief delay to show completion message
            onSplashComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Large Steal Your Face Logo
            Image(
                painter = painterResource(com.deadarchive.core.design.R.drawable.steal_your_face),
                contentDescription = "Steal Your Face - Grateful Dead Logo",
                modifier = Modifier.size(200.dp)
            )
            
            Text(
                text = "Dead Archive",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "V2 Database - Enhanced Performance",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Show initialization progress or error state
            when {
                uiState.showError -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = IconResources.Status.Error(),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = "V2 Database Setup Failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(onClick = { viewModel.retryInitialization() }) {
                                Text("Retry")
                            }
                            Button(onClick = { viewModel.skipInitialization() }) {
                                Text("Skip V2")
                            }
                        }
                    }
                }
                
                uiState.showProgress -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (uiState.progress.phase) {
                            PhaseV2.IMPORTING_SHOWS -> {
                                if (uiState.progress.totalShows > 0) {
                                    LinearProgressIndicator(
                                        progress = { uiState.progress.progressPercentage / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Text(
                                        text = "${uiState.progress.processedShows} / ${uiState.progress.totalShows} shows",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                            
                            else -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                        
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (uiState.progress.currentShow.isNotBlank() && uiState.progress.phase == PhaseV2.IMPORTING_SHOWS) {
                            Text(
                                text = uiState.progress.currentShow,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}