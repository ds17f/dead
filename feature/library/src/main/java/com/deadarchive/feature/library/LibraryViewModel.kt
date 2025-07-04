package com.deadarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.LibraryItemType
import com.deadarchive.core.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val showRepository: ShowRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadLibraryItems()
    }
    
    private fun loadLibraryItems() {
        viewModelScope.launch {
            try {
                _uiState.value = LibraryUiState.Loading
                
                combine(
                    libraryRepository.getAllLibraryItems(),
                    showRepository.getAllShows()
                ) { libraryItems, allShows ->
                    println("DEBUG LibraryViewModel: Found ${libraryItems.size} library items")
                    libraryItems.forEach { item ->
                        println("DEBUG LibraryViewModel: Library item - showId: ${item.showId}, type: ${item.type}")
                    }
                    println("DEBUG LibraryViewModel: Found ${allShows.size} total shows")
                    
                    // Find matching shows with recordings, or create basic ones
                    val libraryShows = libraryItems.map { libraryItem ->
                        val matchingShow = allShows.find { show -> show.showId == libraryItem.showId }
                        
                        if (matchingShow != null) {
                            // Use the actual show data with recordings
                            matchingShow.copy(isInLibrary = true)
                        } else {
                            // Create basic show from showId if not found
                            val parts = libraryItem.showId.split("_")
                            val date = parts.getOrNull(0) ?: "Unknown Date"
                            val venue = parts.drop(1).joinToString(" ").replace("_", " ") 
                            
                            Show(
                                date = date,
                                venue = venue,
                                location = null,
                                year = date.take(4),
                                recordings = emptyList(),
                                isInLibrary = true
                            )
                        }
                    }
                    println("DEBUG LibraryViewModel: Created ${libraryShows.size} shows for display")
                    LibraryUiState.Success(libraryItems, libraryShows)
                }
                .catch { exception ->
                    println("ERROR LibraryViewModel: ${exception.message}")
                    _uiState.value = LibraryUiState.Error(
                        exception.message ?: "Failed to load library items"
                    )
                }
                .collect { uiState ->
                    _uiState.value = uiState
                }
            } catch (e: Exception) {
                println("EXCEPTION LibraryViewModel: ${e.message}")
                _uiState.value = LibraryUiState.Error(
                    e.message ?: "Failed to load library items"
                )
            }
        }
    }
    
    fun removeFromLibrary(libraryItem: LibraryItem) {
        viewModelScope.launch {
            try {
                when (libraryItem.type) {
                    LibraryItemType.SHOW -> {
                        libraryRepository.removeShowFromLibrary(libraryItem.showId)
                    }
                }
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    fun removeShowFromLibrary(showId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.removeShowFromLibrary(showId)
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    fun retry() {
        loadLibraryItems()
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val libraryItems: List<LibraryItem>,
        val shows: List<Show>
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}