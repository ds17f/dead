package com.deadarchive.core.data.repository

import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.database.FavoriteEntity
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.FavoriteItem
import com.deadarchive.core.model.FavoriteType
import com.deadarchive.core.model.Track
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests focusing on FavoriteRepository business logic:
 * - ID generation algorithms
 * - Toggle state transitions
 * - Entity creation logic
 * - Error handling for business rules
 */
class FavoriteRepositoryTest {

    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var repository: FavoriteRepositoryImpl

    @Before
    fun setup() {
        mockFavoriteDao = mockk(relaxed = true)
        repository = FavoriteRepositoryImpl(mockFavoriteDao)
    }

    // ID Generation Business Logic Tests

    @Test
    fun `addConcertToFavorites generates correct ID format`() = runTest {
        // Given
        val concert = createTestConcert("gd1977-05-08", "Cornell '77")
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addConcertToFavorites(concert)

        // Then - Verify ID generation algorithm
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("concert_gd1977-05-08")
        assertThat(capturedEntity.type).isEqualTo("CONCERT")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(capturedEntity.trackFilename).isNull()
    }

    @Test
    fun `addTrackToFavorites generates correct ID format`() = runTest {
        // Given
        val track = createTestTrack("d1t01.mp3", "Fire On The Mountain")
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addTrackToFavorites("gd1977-05-08", track)

        // Then - Verify ID generation algorithm
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("track_gd1977-05-08_d1t01.mp3")
        assertThat(capturedEntity.type).isEqualTo("TRACK")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(capturedEntity.trackFilename).isEqualTo("d1t01.mp3")
    }

    @Test
    fun `ID generation handles special characters correctly`() = runTest {
        // Given - Concert ID with special characters
        val concert = createTestConcert("gd1977-05-08.sbd.miller.89174.sbeok.flac16", "Cornell")
        val track = createTestTrack("gd77-05-08d1t01.shn", "Fire On The Mountain")
        val concertEntitySlot = slot<FavoriteEntity>()
        val trackEntitySlot = slot<FavoriteEntity>()
        
        coEvery { mockFavoriteDao.insertFavorite(capture(concertEntitySlot)) } returns Unit
        coEvery { mockFavoriteDao.insertFavorite(capture(trackEntitySlot)) } returns Unit

        // When
        repository.addConcertToFavorites(concert)
        repository.addTrackToFavorites(concert.identifier, track)

        // Then - IDs should preserve special characters
        assertThat(concertEntitySlot.captured.id).isEqualTo("concert_gd1977-05-08.sbd.miller.89174.sbeok.flac16")
        assertThat(trackEntitySlot.captured.id).isEqualTo("track_gd1977-05-08.sbd.miller.89174.sbeok.flac16_gd77-05-08d1t01.shn")
    }

    // Toggle State Machine Logic Tests

    @Test
    fun `toggleConcertFavorite state machine - not favorite to favorite`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then - State transition: false -> true
        assertThat(result).isTrue()
        coVerify { mockFavoriteDao.insertFavorite(any()) }
        coVerify(exactly = 0) { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    @Test
    fun `toggleConcertFavorite state machine - favorite to not favorite`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns true
        coEvery { mockFavoriteDao.deleteFavoriteById("concert_test1") } returns Unit

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then - State transition: true -> false
        assertThat(result).isFalse()
        coVerify { mockFavoriteDao.deleteFavoriteById("concert_test1") }
        coVerify(exactly = 0) { mockFavoriteDao.insertFavorite(any()) }
    }

    @Test
    fun `toggleTrackFavorite state machine with correct ID deletion`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Track 1")
        coEvery { mockFavoriteDao.isTrackFavorite("concert123", "track1.mp3") } returns true
        coEvery { mockFavoriteDao.deleteFavoriteById("track_concert123_track1.mp3") } returns Unit

        // When
        val result = repository.toggleTrackFavorite("concert123", track)

        // Then - Verify correct ID used for deletion
        assertThat(result).isFalse()
        coVerify { mockFavoriteDao.deleteFavoriteById("track_concert123_track1.mp3") }
    }

    // Entity Creation Business Logic Tests

    @Test
    fun `addConcertToFavorites preserves notes in entity creation`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Great Show")
        val notes = "Best version of Fire I've ever heard!"
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addConcertToFavorites(concert, notes)

        // Then - Notes should be preserved in entity
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.notes).isEqualTo(notes)
        assertThat(capturedEntity.id).isEqualTo("concert_test1")
    }

    @Test
    fun `addTrackToFavorites creates entity with timestamp`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Fire On The Mountain")
        val beforeTime = System.currentTimeMillis()
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addTrackToFavorites("test1", track)
        val afterTime = System.currentTimeMillis()

        // Then - Entity should have realistic timestamp
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.addedTimestamp).isAtLeast(beforeTime)
        assertThat(capturedEntity.addedTimestamp).isAtMost(afterTime)
    }

    // Business Rule Validation Tests

    @Test
    fun `removeConcertFromFavorites uses correct ID format for deletion`() = runTest {
        // Given
        val concertId = "gd1977-05-08.sbd.special-chars_test"
        coEvery { mockFavoriteDao.deleteFavoriteById(any()) } returns Unit

        // When
        repository.removeConcertFromFavorites(concertId)

        // Then - Should construct correct deletion ID
        coVerify { mockFavoriteDao.deleteFavoriteById("concert_gd1977-05-08.sbd.special-chars_test") }
    }

    @Test
    fun `removeTrackFromFavorites constructs compound ID correctly`() = runTest {
        // Given
        val concertId = "gd1977-05-08"
        val trackFilename = "gd77-05-08d1t01.shn"
        coEvery { mockFavoriteDao.deleteFavoriteById(any()) } returns Unit

        // When
        repository.removeTrackFromFavorites(concertId, trackFilename)

        // Then - Should construct correct compound ID
        coVerify { mockFavoriteDao.deleteFavoriteById("track_gd1977-05-08_gd77-05-08d1t01.shn") }
    }

    // Domain Model Factory Logic Tests

    @Test
    fun `FavoriteItem fromConcert factory creates correct structure`() {
        // Given
        val concert = createTestConcert("gd1977-05-08", "Cornell '77")

        // When
        val favoriteItem = FavoriteItem.fromConcert(concert)

        // Then - Factory should create correct structure
        assertThat(favoriteItem.id).isEqualTo("concert_gd1977-05-08")
        assertThat(favoriteItem.type).isEqualTo(FavoriteType.CONCERT)
        assertThat(favoriteItem.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(favoriteItem.trackFilename).isNull()
    }

    @Test
    fun `FavoriteItem fromTrack factory creates correct structure`() {
        // Given
        val track = createTestTrack("d1t01.mp3", "Fire On The Mountain")

        // When
        val favoriteItem = FavoriteItem.fromTrack("gd1977-05-08", track)

        // Then - Factory should create correct structure
        assertThat(favoriteItem.id).isEqualTo("track_gd1977-05-08_d1t01.mp3")
        assertThat(favoriteItem.type).isEqualTo(FavoriteType.TRACK)
        assertThat(favoriteItem.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(favoriteItem.trackFilename).isEqualTo("d1t01.mp3")
    }

    // Error Scenarios and Edge Cases

    @Test
    fun `toggle operations handle empty concert IDs correctly`() = runTest {
        // Given
        val concert = createTestConcert("", "Empty ID Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit

        // When
        repository.toggleConcertFavorite(concert)

        // Then - Should still generate valid ID
        coVerify { 
            mockFavoriteDao.insertFavorite(match { entity ->
                entity.id == "concert_" && entity.concertIdentifier == ""
            })
        }
    }

    // Helper methods for creating test data
    private fun createTestConcert(identifier: String, title: String): Concert {
        return Concert(
            identifier = identifier,
            title = title,
            date = "1977-05-08",
            venue = "Barton Hall",
            location = "Ithaca, NY"
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