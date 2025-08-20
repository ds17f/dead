package com.deadly.feature.browse

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import javax.inject.Inject

/**
 * HomeV2ViewModel - State management for HomeV2Screen
 * 
 * Following V2 architecture patterns:
 * - Basic UI state management
 * - Service injection placeholder
 * - Foundation for UI-first development
 */
@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    // TODO: Add V2 services as they're discovered through UI development
    // private val homeV2Service: HomeV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeV2ViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeV2UiState.initial())
    val uiState: StateFlow<HomeV2UiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    /**
     * Load initial data for the home screen
     */
    private fun loadInitialData() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitialized = true,
            recentShows = generateMockRecentShows(),
            todayInHistory = generateMockTodayInHistory(),
            exploreCollections = generateMockCollections()
        )
    }
    
    /**
     * Generate mock recent shows data for development
     */
    private fun generateMockRecentShows(): List<Show> {
        return listOf(
            Show(
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                location = "Ithaca, NY",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                date = "1972-05-11",
                venue = "Civic Auditorium",
                location = "Albuquerque, NM",
                recordings = emptyList(),
                isInLibrary = true
            ),
            Show(
                // Removed duplicate parameter: "gd1974-06-28",
                date = "1974-06-28",
                venue = "Boston Garden",
                location = "Boston, MA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1973-02-28",
                date = "1973-02-28",
                venue = "Pershing Municipal Auditorium",
                location = "Lincoln, NE",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1976-06-14",
                date = "1976-06-14",
                venue = "Beacon Theatre",
                location = "New York, NY",
                recordings = emptyList(),
                isInLibrary = true
            ),
            Show(
                // Removed duplicate parameter: "gd1971-04-29",
                date = "1971-04-29",
                venue = "Fillmore East",
                location = "New York, NY",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1975-08-13",
                date = "1975-08-13",
                venue = "Great American Music Hall",
                location = "San Francisco, CA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1978-04-16",
                date = "1978-04-16",
                venue = "Huntington Civic Center",
                location = "Huntington, WV",
                recordings = emptyList(),
                isInLibrary = true
            ),
            Show(
                // Removed duplicate parameter: "gd1973-11-10",
                date = "1973-11-10",
                venue = "Winterland Arena",
                location = "San Francisco, CA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1974-09-21",
                date = "1974-09-21",
                venue = "Palace Theatre",
                location = "Waterbury, CT",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1970-09-20",
                date = "1970-09-20",
                venue = "Fillmore East",
                location = "New York, NY",
                recordings = emptyList(),
                isInLibrary = true
            ),
            Show(
                // Removed duplicate parameter: "gd1972-08-21",
                date = "1972-08-21",
                venue = "Berkeley Community Theatre",
                location = "Berkeley, CA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1976-09-13",
                date = "1976-09-13",
                venue = "Dillon Stadium",
                location = "Hartford, CT",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1974-12-18",
                date = "1974-12-18",
                venue = "Omni",
                location = "Atlanta, GA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1975-03-23",
                date = "1975-03-23",
                venue = "Kezar Stadium",
                location = "San Francisco, CA",
                recordings = emptyList(),
                isInLibrary = true
            ),
            Show(
                // Removed duplicate parameter: "gd1973-06-10",
                date = "1973-06-10",
                venue = "RFK Stadium",
                location = "Washington, DC",
                recordings = emptyList(),
                isInLibrary = false
            )
        )
    }
    
    /**
     * Generate mock "Today In History" shows
     */
    private fun generateMockTodayInHistory(): List<Show> {
        return listOf(
            Show(
                // Removed duplicate parameter: "gd1977-05-08",
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                location = "Ithaca, NY",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1970-05-08",
                date = "1970-05-08",
                venue = "Kresge Plaza, MIT",
                location = "Cambridge, MA",
                recordings = emptyList(),
                isInLibrary = false
            ),
            Show(
                // Removed duplicate parameter: "gd1972-05-08",
                date = "1972-05-08",
                venue = "Civic Arena",
                location = "Pittsburgh, PA",
                recordings = emptyList(),
                isInLibrary = true
            )
        )
    }
    
    /**
     * Generate mock collection categories
     */
    private fun generateMockCollections(): List<String> {
        return listOf(
            "Greatest Shows",
            "Rare Recordings", 
            "Europe '72",
            "Wall of Sound",
            "Dick's Picks",
            "Acoustic Sets"
        )
    }
    
    /**
     * Handle refresh action
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // TODO: Implement refresh logic through services
        loadInitialData()
    }
    
    /**
     * Handle error state reset
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        // TODO: Cleanup services if needed
    }
}

/**
 * UI State for HomeV2Screen
 */
data class HomeV2UiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val welcomeText: String = "Welcome to Dead Archive",
    val recentShows: List<Show> = emptyList(),
    val todayInHistory: List<Show> = emptyList(),
    val exploreCollections: List<String> = emptyList() // Collection categories for now
) {
    companion object {
        fun initial() = HomeV2UiState(
            isLoading = true,
            isInitialized = false
        )
    }
    
    val hasError: Boolean get() = errorMessage != null
}