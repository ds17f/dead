package com.deadarchive.core.data.repository

import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.Track
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Simplified tests focusing on behavior rather than implementation details.
 * Tests what the system DOES, not HOW it does it.
 */
class FavoriteRepositoryTest {

    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockConcertRepository: ConcertRepository
    private lateinit var repository: FavoriteRepositoryImpl

    @Before
    fun setup() {
        mockFavoriteDao = mockk(relaxed = true)
        mockConcertRepository = mockk(relaxed = true)
        repository = FavoriteRepositoryImpl(mockFavoriteDao, mockConcertRepository)
    }

    // Behavior-focused tests - what matters to users

    @Test
    fun `can add concert to favorites`() = runTest {
        // Given
        val concert = createTestConcert("gd1977", "Cornell")

        // When
        repository.addConcertToFavorites(concert)

        // Then - verify the right method was called with right type
        coVerify { 
            mockFavoriteDao.insertFavorite(match { entity ->
                entity.type == "CONCERT" && 
                entity.concertIdentifier == "gd1977"
            })
        }
    }

    @Test
    fun `can add track to favorites`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Fire")

        // When
        repository.addTrackToFavorites("gd1977", track)

        // Then - verify the right method was called with right type
        coVerify { 
            mockFavoriteDao.insertFavorite(match { entity ->
                entity.type == "TRACK" && 
                entity.concertIdentifier == "gd1977" &&
                entity.trackFilename == "track1.mp3"
            })
        }
    }

    @Test
    fun `toggle adds favorite when not currently favorite`() = runTest {
        // Given - not currently a favorite
        val concert = createTestConcert("test1", "Test")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns false

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then - should add favorite and return true
        assertThat(result).isTrue()
        coVerify { mockFavoriteDao.insertFavorite(any()) }
        coVerify(exactly = 0) { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    @Test
    fun `toggle removes favorite when currently favorite`() = runTest {
        // Given - currently a favorite
        val concert = createTestConcert("test1", "Test")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns true

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then - should remove favorite and return false
        assertThat(result).isFalse()
        coVerify { mockFavoriteDao.deleteFavoriteById(any()) }
        coVerify(exactly = 0) { mockFavoriteDao.insertFavorite(any()) }
    }

    @Test
    fun `notes are preserved when adding favorite`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Great Show")
        val notes = "Amazing setlist!"

        // When
        repository.addConcertToFavorites(concert, notes)

        // Then - notes should be included
        coVerify { 
            mockFavoriteDao.insertFavorite(match { entity ->
                entity.notes == notes
            })
        }
    }

    @Test
    fun `can update favorite notes`() = runTest {
        // When
        repository.updateFavoriteNotes("favorite_id", "New notes")

        // Then
        coVerify { mockFavoriteDao.updateFavoriteNotes("favorite_id", "New notes") }
    }

    @Test
    fun `can remove concert favorites`() = runTest {
        // When
        repository.removeConcertFromFavorites("test1")

        // Then - should call delete with some ID for this concert
        coVerify { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    @Test
    fun `can remove track favorites`() = runTest {
        // When
        repository.removeTrackFromFavorites("test1", "track1.mp3")

        // Then - should call delete with some ID for this track
        coVerify { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    // Helper methods
    private fun createTestConcert(identifier: String, title: String): Concert {
        return Concert(
            identifier = identifier,
            title = title,
            date = "1977-05-08",
            venue = "Test Venue",
            location = "Test Location"
        )
    }

    private fun createTestTrack(filename: String, title: String): Track {
        return Track(
            filename = filename,
            title = title,
            trackNumber = "01"
        )
    }
}