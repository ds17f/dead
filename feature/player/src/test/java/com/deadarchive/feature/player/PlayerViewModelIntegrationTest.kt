package com.deadarchive.feature.player

import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.media.player.PlayerRepository
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelIntegrationTest {

    private lateinit var playerRepository: PlayerRepository
    private lateinit var concertRepository: ConcertRepository
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        playerRepository = mock()
        concertRepository = mock()
        
        // Setup basic PlayerRepository flows
        whenever(playerRepository.isPlaying).thenReturn(MutableStateFlow(false))
        whenever(playerRepository.currentPosition).thenReturn(MutableStateFlow(0L))
        whenever(playerRepository.duration).thenReturn(MutableStateFlow(0L))
        whenever(playerRepository.playbackState).thenReturn(MutableStateFlow(1))
        
        viewModel = PlayerViewModel(playerRepository, concertRepository)
    }

    @Test
    fun `loadConcert should load tracks with downloadUrls`() = runTest {
        // Given - a concert with tracks that have download URLs
        val audioFile = AudioFile(
            filename = "gd1977-05-08d1t01.mp3",
            format = "VBR MP3",
            downloadUrl = "https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t01.mp3"
        )
        
        val track = Track(
            filename = "gd1977-05-08d1t01.mp3",
            title = "Promised Land",
            trackNumber = "1",
            audioFile = audioFile
        )
        
        val concert = Concert(
            identifier = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
            title = "Grateful Dead Live at Barton Hall on 1977-05-08",
            tracks = listOf(track)
        )
        
        whenever(concertRepository.getConcertById("gd1977-05-08.sbd.hicks.4982.sbeok.shnf"))
            .thenReturn(concert)
        
        // When - loading the concert
        viewModel.loadConcert("gd1977-05-08.sbd.hicks.4982.sbeok.shnf")
        
        // Then - the UI state should have the tracks with download URLs
        val uiState = viewModel.uiState.value
        assertThat(uiState.isLoading).isFalse()
        assertThat(uiState.error).isNull()
        assertThat(uiState.tracks).hasSize(1)
        
        val loadedTrack = uiState.tracks[0]
        assertThat(loadedTrack.filename).isEqualTo("gd1977-05-08d1t01.mp3")
        assertThat(loadedTrack.title).isEqualTo("Promised Land")
        assertThat(loadedTrack.audioFile).isNotNull()
        assertThat(loadedTrack.audioFile!!.downloadUrl)
            .isEqualTo("https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t01.mp3")
    }

    @Test
    fun `playTrack should call playerRepository with correct URL`() = runTest {
        // Given - a loaded concert with tracks
        val audioFile = AudioFile(
            filename = "track01.mp3",
            format = "VBR MP3",
            downloadUrl = "https://example.com/track01.mp3"
        )
        
        val track = Track(
            filename = "track01.mp3",
            title = "Test Track",
            trackNumber = "1",
            audioFile = audioFile
        )
        
        val concert = Concert(
            identifier = "test-concert",
            title = "Test Concert",
            tracks = listOf(track)
        )
        
        whenever(concertRepository.getConcertById("test-concert")).thenReturn(concert)
        
        // Load the concert first
        viewModel.loadConcert("test-concert")
        
        // When - playing a track
        viewModel.playTrack(0)
        
        // Then - playerRepository should be called with the correct URL
        verify(playerRepository).playTrack(
            url = "https://example.com/track01.mp3",
            title = "Test Track",
            artist = "Test Concert"
        )
        
        // And the UI state should be updated
        val uiState = viewModel.uiState.value
        assertThat(uiState.currentTrackIndex).isEqualTo(0)
        assertThat(uiState.error).isNull()
    }

    @Test
    fun `playTrack should set error when track has no downloadUrl`() = runTest {
        // Given - a track without downloadUrl
        val audioFile = AudioFile(
            filename = "track01.mp3",
            format = "VBR MP3",
            downloadUrl = null // No download URL
        )
        
        val track = Track(
            filename = "track01.mp3",
            title = "Test Track",
            trackNumber = "1",
            audioFile = audioFile
        )
        
        val concert = Concert(
            identifier = "test-concert",
            title = "Test Concert",
            tracks = listOf(track)
        )
        
        whenever(concertRepository.getConcertById("test-concert")).thenReturn(concert)
        
        // Load the concert first
        viewModel.loadConcert("test-concert")
        
        // When - trying to play the track
        viewModel.playTrack(0)
        
        // Then - should set an error in UI state
        val uiState = viewModel.uiState.value
        assertThat(uiState.error).isEqualTo("Audio file not available for this track")
    }

    @Test
    fun `loadConcert should handle repository returning null`() = runTest {
        // Given - repository returns null (concert not found)
        whenever(concertRepository.getConcertById("nonexistent-concert")).thenReturn(null)
        
        // When - loading a nonexistent concert
        viewModel.loadConcert("nonexistent-concert")
        
        // Then - should set error state
        val uiState = viewModel.uiState.value
        assertThat(uiState.isLoading).isFalse()
        assertThat(uiState.error).isEqualTo("Concert not found")
        assertThat(uiState.tracks).isEmpty()
    }
}