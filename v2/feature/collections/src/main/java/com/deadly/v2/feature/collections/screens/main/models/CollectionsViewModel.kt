package com.deadly.v2.feature.collections.screens.main.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.api.collections.DeadCollectionsService
import com.deadly.v2.core.model.DeadCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

/**
 * CollectionsViewModel - ViewModel for Collections screen
 * 
 * Manages UI state for the collections browsing experience.
 * Integrates with DeadCollectionsService for real collection data.
 */
@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionsService: DeadCollectionsService
) : ViewModel() {
    
    companion object {
        private const val TAG = "CollectionsViewModel"
    }
    
    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()
    
    // Observe featured collections from service
    val featuredCollections: StateFlow<List<DeadCollection>> = 
        collectionsService.featuredCollections.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        Log.d(TAG, "CollectionsViewModel initialized")
    }
    
    /**
     * Handle collection selection
     */
    fun onCollectionSelected(collectionId: String) {
        Log.d(TAG, "Collection selected: $collectionId")
        // TODO: Navigate to collection detail or update UI state
    }
    
    /**
     * Handle collection search
     */
    fun onSearchCollections() {
        Log.d(TAG, "Search collections requested")
        // TODO: Implement collections search
    }
}

/**
 * UI state for Collections screen
 */
data class CollectionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)