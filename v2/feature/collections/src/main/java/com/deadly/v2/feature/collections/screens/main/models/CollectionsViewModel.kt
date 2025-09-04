package com.deadly.v2.feature.collections.screens.main.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.api.collections.DeadCollectionsService
import com.deadly.v2.core.design.component.FilterPath
import com.deadly.v2.core.model.DeadCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    
    // Filter state for HierarchicalFilter
    private val _filterPath = MutableStateFlow(FilterPath())
    val filterPath: StateFlow<FilterPath> = _filterPath.asStateFlow()
    
    // All collections from service
    private val _allCollections = MutableStateFlow<List<DeadCollection>>(emptyList())
    val allCollections: StateFlow<List<DeadCollection>> = _allCollections.asStateFlow()
    
    // Observe featured collections from service
    val featuredCollections: StateFlow<List<DeadCollection>> = 
        collectionsService.featuredCollections.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Filtered collections based on selected filter path
    val filteredCollections: StateFlow<List<DeadCollection>> = combine(
        allCollections,
        filterPath
    ) { collections, path ->
        if (path.isEmpty) {
            collections
        } else {
            // Filter collections based on selected tags
            val selectedTags = path.nodes.map { it.id }
            collections.filter { collection ->
                selectedTags.any { tag -> collection.tags.contains(tag) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        Log.d(TAG, "CollectionsViewModel initialized")
        loadAllCollections()
    }
    
    /**
     * Load all collections from service
     */
    private fun loadAllCollections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = collectionsService.getAllCollections()
                result.fold(
                    onSuccess = { collections ->
                        _allCollections.value = collections
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                        Log.d(TAG, "Loaded ${collections.size} collections")
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = exception.message)
                        Log.e(TAG, "Failed to load collections", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                Log.e(TAG, "Error loading collections", e)
            }
        }
    }
    
    /**
     * Handle filter path change from HierarchicalFilter
     */
    fun onFilterPathChanged(newPath: FilterPath) {
        Log.d(TAG, "Filter path changed: ${newPath.getCombinedId()}")
        _filterPath.value = newPath
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