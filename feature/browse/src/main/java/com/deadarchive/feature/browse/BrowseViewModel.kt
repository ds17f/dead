package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.network.ArchiveApiClient
import com.deadarchive.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val archiveApiClient: ArchiveApiClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    fun testSearchConcerts() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            
            when (val result = archiveApiClient.searchConcerts("grateful dead", limit = 5)) {
                is ApiResult.Success -> {
                    val concerts = result.data
                    val summary = buildString {
                        appendLine("Found ${concerts.size} concerts:")
                        concerts.take(3).forEach { concert ->
                            appendLine("• ${concert.title}")
                            appendLine("  Date: ${concert.date}")
                            appendLine("  Venue: ${concert.venue}")
                            appendLine()
                        }
                    }
                    _uiState.value = BrowseUiState.Success(summary)
                }
                is ApiResult.Error -> {
                    _uiState.value = BrowseUiState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun testPopularConcerts() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            
            when (val result = archiveApiClient.getPopularConcerts(limit = 5)) {
                is ApiResult.Success -> {
                    val concerts = result.data
                    val summary = buildString {
                        appendLine("Popular concerts (${concerts.size}):")
                        concerts.take(3).forEach { concert ->
                            appendLine("• ${concert.title}")
                            appendLine("  Date: ${concert.date}")
                            appendLine("  Source: ${concert.displaySource}")
                            appendLine()
                        }
                    }
                    _uiState.value = BrowseUiState.Success(summary)
                }
                is ApiResult.Error -> {
                    _uiState.value = BrowseUiState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }
    
    fun testRecentConcerts() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            
            when (val result = archiveApiClient.getRecentConcerts(limit = 5)) {
                is ApiResult.Success -> {
                    val concerts = result.data
                    val summary = buildString {
                        appendLine("Recent concerts (${concerts.size}):")
                        concerts.take(3).forEach { concert ->
                            appendLine("• ${concert.title}")
                            appendLine("  Date: ${concert.date}")
                            appendLine("  Added: ${concert.addedDate}")
                            appendLine()
                        }
                    }
                    _uiState.value = BrowseUiState.Success(summary)
                }
                is ApiResult.Error -> {
                    _uiState.value = BrowseUiState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val data: String) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}