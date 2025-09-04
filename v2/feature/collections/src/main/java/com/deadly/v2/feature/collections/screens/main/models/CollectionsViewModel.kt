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
    
    // Currently selected collection for showing shows
    private val _selectedCollection = MutableStateFlow<DeadCollection?>(null)
    val selectedCollection: StateFlow<DeadCollection?> = _selectedCollection.asStateFlow()
    
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
        Log.d(TAG, "Filtering collections - Total: ${collections.size}, Filter path: ${path.getCombinedId()}")
        if (path.isEmpty) {
            Log.d(TAG, "No filter selected, showing all ${collections.size} collections")
            collections
        } else {
            val selectedTags = path.nodes.map { it.id }
            Log.d(TAG, "Selected filter tags: $selectedTags")
            
            val filtered = collections.filter { collection ->
                // Handle filtering logic based on filter structure:
                // - If "official" is selected (root), show all official collections
                // - If "official" + subcategory (e.g., "dicks-picks"), filter further
                // - If "guest" or "era" is selected (single-level), show all matching collections
                
                val hasMatch = when {
                    // Official root filter logic
                    selectedTags.contains("official") && selectedTags.size == 1 -> {
                        // Just "Official" selected - show all official collections
                        collection.tags.contains("official")
                    }
                    selectedTags.contains("official") && selectedTags.size == 2 -> {
                        // "Official" + subcategory selected - filter further
                        val subcategory = selectedTags.find { it != "official" }
                        collection.tags.contains("official") && collection.tags.contains(subcategory)
                    }
                    // Single-level filters (guest, era)
                    selectedTags.contains("guest") -> {
                        collection.tags.contains("guest")
                    }
                    selectedTags.contains("era") -> {
                        collection.tags.contains("era")
                    }
                    else -> {
                        // Fallback to any tag matching
                        selectedTags.any { tag -> collection.tags.contains(tag) }
                    }
                }
                
                Log.d(TAG, "Collection '${collection.name}' tags: ${collection.tags}, matches: $hasMatch")
                hasMatch
            }
            
            Log.d(TAG, "Filtered result: ${filtered.size} collections")
            filtered
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
     * Initialize with a specific collection ID (for routing from collection details)
     */
    fun initializeWithCollectionId(collectionId: String?) {
        collectionId?.let { id ->
            Log.d(TAG, "Initializing with collection ID: $id")
            // The collection will be selected automatically by the carousel's LaunchedEffect
            // when it finds the matching collection in the filteredCollections list
        }
    }
    
    /**
     * Load all collections from service
     */
    private fun loadAllCollections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                Log.d(TAG, "Calling collectionsService.getAllCollections()")
                val result = collectionsService.getAllCollections()
                result.fold(
                    onSuccess = { collections ->
                        Log.d(TAG, "Successfully loaded ${collections.size} collections from service")
                        collections.forEach { collection ->
                            Log.d(TAG, "Collection: '${collection.name}' with tags: ${collection.tags}")
                        }
                        _allCollections.value = collections
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load collections from service", exception)
                        _uiState.value = _uiState.value.copy(isLoading = false, error = exception.message)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading collections", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
     * Handle collection selection from carousel
     */
    fun onCollectionSelected(collection: DeadCollection) {
        Log.d(TAG, "Collection selected: ${collection.name} with ${collection.shows.size} shows")
        _selectedCollection.value = collection
    }
    
    /**
     * Handle collection selection by ID (for navigation)
     */
    fun onCollectionSelectedById(collectionId: String) {
        Log.d(TAG, "Collection selected by ID: $collectionId")
        val collection = _allCollections.value.find { it.id == collectionId }
        if (collection != null) {
            onCollectionSelected(collection)
        } else {
            Log.w(TAG, "Collection not found: $collectionId")
        }
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