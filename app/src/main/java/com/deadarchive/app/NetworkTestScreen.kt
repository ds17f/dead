package com.deadarchive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.network.ArchiveApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkTestViewModel @Inject constructor(
    private val apiService: ArchiveApiService
) : ViewModel() {
    
    private val _message = MutableStateFlow("Ready to test Archive.org API")
    val message: StateFlow<String> = _message.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _results = MutableStateFlow<List<String>>(emptyList())
    val results: StateFlow<List<String>> = _results.asStateFlow()
    
    fun testApiConnection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _message.value = "Testing API connection..."
                
                // Test a simple API call - search for grateful dead concerts
                val response = apiService.searchConcerts(
                    query = "collection:GratefulDead",
                    rows = 5
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val concerts = response.body()!!.response.docs
                    _message.value = "✅ API connection successful! Found ${concerts.size} concerts"
                    _results.value = concerts.map { concert ->
                        "${concert.title} (${concert.date ?: "Unknown date"})"
                    }
                } else {
                    _message.value = "❌ API call failed: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _message.value = "❌ Network error: ${e.message}"
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearResults() {
        _results.value = emptyList()
        _message.value = "Results cleared"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTestScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkTestViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val results by viewModel.results.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text("← Back")
            }
            Text(
                text = "Network Test Screen",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // Test buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.testApiConnection() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Test API")
                }
            }
            
            Button(
                onClick = { viewModel.clearResults() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Results")
            }
        }
        
        // Results display
        if (results.isNotEmpty()) {
            Text(
                text = "API Results (${results.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { result ->
                    Card {
                        Text(
                            text = result,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}