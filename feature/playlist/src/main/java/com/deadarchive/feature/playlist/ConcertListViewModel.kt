package com.deadarchive.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConcertListViewModel @Inject constructor(
    // TODO: Inject the new concert repository when it's created
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ConcertListUiState>(ConcertListUiState.Loading)
    val uiState: StateFlow<ConcertListUiState> = _uiState.asStateFlow()
    
    init {
        loadConcerts()
    }
    
    private fun loadConcerts() {
        viewModelScope.launch {
            try {
                _uiState.value = ConcertListUiState.Loading
                
                // TODO: Replace with actual repository call
                // val concerts = concertRepository.getAllConcertsWithRecordings()
                
                // Mock data for now
                val mockConcerts = createMockConcerts()
                
                _uiState.value = ConcertListUiState.Success(mockConcerts)
            } catch (e: Exception) {
                _uiState.value = ConcertListUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun retry() {
        loadConcerts()
    }
    
    fun toggleFavorite(show: Show) {
        viewModelScope.launch {
            try {
                // TODO: Update favorite status in repository
                // showRepository.updateFavoriteStatus(show.showId, !show.isFavorite)
                
                // For now, update the state locally
                val currentState = _uiState.value
                if (currentState is ConcertListUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        if (existingShow.showId == show.showId) {
                            existingShow.copy(isFavorite = !existingShow.isFavorite)
                        } else {
                            existingShow
                        }
                    }
                    _uiState.value = ConcertListUiState.Success(updatedShows)
                }
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    // Mock data for demonstration
    private fun createMockConcerts(): List<Show> {
        return listOf(
            Show(
                date = "1977-05-08",
                venue = "Cornell University",
                location = "Ithaca, NY",
                year = "1977",
                setlistRaw = "Set I: Minglewood Blues, Loser, El Paso, They Love Each Other...",
                recordings = listOf(
                    com.deadarchive.core.model.Recording(
                        identifier = "gd77-05-08.sbd",
                        title = "Grateful Dead Live at Cornell University on 1977-05-08",
                        source = "SBD",
                        taper = "Betty Cantor-Jackson",
                        concertDate = "1977-05-08",
                        concertVenue = "Cornell University",
                        tracks = emptyList() // Would be populated with actual tracks
                    ),
                    com.deadarchive.core.model.Recording(
                        identifier = "gd77-05-08.aud",
                        title = "Grateful Dead Live at Cornell University on 1977-05-08 (Audience)",
                        source = "AUD",
                        taper = "Unknown",
                        concertDate = "1977-05-08",
                        concertVenue = "Cornell University",
                        tracks = emptyList()
                    )
                ),
                isFavorite = false
            ),
            Show(
                date = "1977-05-07",
                venue = "Boston Garden",
                location = "Boston, MA", 
                year = "1977",
                setlistRaw = null,
                recordings = listOf(
                    com.deadarchive.core.model.Recording(
                        identifier = "gd77-05-07.sbd",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07",
                        source = "SBD",
                        taper = "Betty Cantor-Jackson",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    ),
                    com.deadarchive.core.model.Recording(
                        identifier = "gd77-05-07.matrix",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07 (Matrix)",
                        source = "MATRIX",
                        taper = "Charlie Miller",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    ),
                    com.deadarchive.core.model.Recording(
                        identifier = "gd77-05-07.aud",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07 (Audience)",
                        source = "AUD",
                        taper = "Jerry Moore",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    )
                ),
                isFavorite = true
            ),
            Show(
                date = "1972-05-03",
                venue = "Olympia Theatre",
                location = "Paris, France",
                year = "1972",
                setlistRaw = "Set I: Truckin', Sugaree, Jack Straw, Deal...",
                recordings = listOf(
                    com.deadarchive.core.model.Recording(
                        identifier = "gd72-05-03.sbd",
                        title = "Grateful Dead Live at Olympia Theatre on 1972-05-03",
                        source = "SBD",
                        taper = "Owsley Stanley",
                        concertDate = "1972-05-03",
                        concertVenue = "Olympia Theatre",
                        tracks = emptyList()
                    )
                ),
                isFavorite = false
            )
        )
    }
}

sealed class ConcertListUiState {
    object Loading : ConcertListUiState()
    
    data class Success(
        val shows: List<Show>
    ) : ConcertListUiState()
    
    data class Error(
        val message: String
    ) : ConcertListUiState()
}