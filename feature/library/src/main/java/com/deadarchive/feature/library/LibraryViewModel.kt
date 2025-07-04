package com.deadarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.LibraryItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
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
                
                libraryRepository.getAllLibraryItems()
                    .catch { exception ->
                        _uiState.value = LibraryUiState.Error(
                            exception.message ?: "Failed to load library items"
                        )
                    }
                    .collect { libraryItems ->
                        _uiState.value = LibraryUiState.Success(libraryItems)
                    }
            } catch (e: Exception) {
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
                    LibraryItemType.RECORDING -> {
                        libraryRepository.removeRecordingFromLibrary(libraryItem.recordingId)
                    }
                    LibraryItemType.TRACK -> {
                        libraryItem.trackFilename?.let { filename ->
                            libraryRepository.removeTrackFromLibrary(libraryItem.recordingId, filename)
                        }
                    }
                    LibraryItemType.CONCERT -> {
                        // TODO: Implement if needed
                    }
                }
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
    data class Success(val libraryItems: List<LibraryItem>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}