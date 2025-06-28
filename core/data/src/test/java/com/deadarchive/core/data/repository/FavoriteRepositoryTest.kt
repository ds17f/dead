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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FavoriteRepositoryTest {

    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var repository: FavoriteRepositoryImpl

    @Before
    fun setup() {
        mockFavoriteDao = mockk()
        repository = FavoriteRepositoryImpl(mockFavoriteDao)
    }

    @Test
    fun `getAllFavorites returns mapped favorite items`() = runTest {
        // Given
        val favoriteEntities = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test2_track1", FavoriteType.TRACK, "test2", "track1.mp3")
        )
        every { mockFavoriteDao.getAllFavorites() } returns flowOf(favoriteEntities)

        // When
        val result = repository.getAllFavorites().first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].type).isEqualTo(FavoriteType.CONCERT)
        assertThat(result[0].concertIdentifier).isEqualTo("test1")
        assertThat(result[1].type).isEqualTo(FavoriteType.TRACK)
        assertThat(result[1].trackFilename).isEqualTo("track1.mp3")
    }

    @Test
    fun `getFavoritesByType returns filtered favorites`() = runTest {
        // Given
        val concertEntities = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        )
        every { mockFavoriteDao.getFavoritesByType("CONCERT") } returns flowOf(concertEntities)

        // When
        val result = repository.getFavoritesByType(FavoriteType.CONCERT).first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(FavoriteType.CONCERT)
    }

    @Test
    fun `getFavoriteConcerts delegates to getFavoritesByType`() = runTest {
        // Given
        val concertEntities = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        )
        every { mockFavoriteDao.getFavoritesByType("CONCERT") } returns flowOf(concertEntities)

        // When
        val result = repository.getFavoriteConcerts().first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(FavoriteType.CONCERT)
        coVerify { mockFavoriteDao.getFavoritesByType("CONCERT") }
    }

    @Test
    fun `getFavoriteTracks delegates to getFavoritesByType`() = runTest {
        // Given
        val trackEntities = listOf(
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        )
        every { mockFavoriteDao.getFavoritesByType("TRACK") } returns flowOf(trackEntities)

        // When
        val result = repository.getFavoriteTracks().first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(FavoriteType.TRACK)
        coVerify { mockFavoriteDao.getFavoritesByType("TRACK") }
    }

    @Test
    fun `getFavoriteTracksForConcert filters by concert and track type`() = runTest {
        // Given
        val entities = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3"),
            createTestFavoriteEntity("track_test1_track2", FavoriteType.TRACK, "test1", "track2.mp3")
        )
        every { mockFavoriteDao.getFavoritesForConcert("test1") } returns flowOf(entities)

        // When
        val result = repository.getFavoriteTracksForConcert("test1").first()

        // Then
        assertThat(result).hasSize(2) // Only tracks, not the concert
        assertThat(result.all { it.type == FavoriteType.TRACK }).isTrue()
        assertThat(result.all { it.concertIdentifier == "test1" }).isTrue()
    }

    @Test
    fun `isConcertFavorite delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns true

        // When
        val result = repository.isConcertFavorite("test1")

        // Then
        assertThat(result).isTrue()
        coVerify { mockFavoriteDao.isConcertFavorite("test1") }
    }

    @Test
    fun `isTrackFavorite delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.isTrackFavorite("test1", "track1.mp3") } returns false

        // When
        val result = repository.isTrackFavorite("test1", "track1.mp3")

        // Then
        assertThat(result).isFalse()
        coVerify { mockFavoriteDao.isTrackFavorite("test1", "track1.mp3") }
    }

    @Test
    fun `addConcertToFavorites creates correct favorite entity`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addConcertToFavorites(concert, "Great show!")

        // Then
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("concert_test1")
        assertThat(capturedEntity.type).isEqualTo("CONCERT")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("test1")
        assertThat(capturedEntity.trackFilename).isNull()
        assertThat(capturedEntity.notes).isEqualTo("Great show!")
    }

    @Test
    fun `addTrackToFavorites creates correct favorite entity`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Track 1")
        val entitySlot = slot<FavoriteEntity>()
        coEvery { mockFavoriteDao.insertFavorite(capture(entitySlot)) } returns Unit

        // When
        repository.addTrackToFavorites("test1", track, "Amazing track!")

        // Then
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("track_test1_track1.mp3")
        assertThat(capturedEntity.type).isEqualTo("TRACK")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("test1")
        assertThat(capturedEntity.trackFilename).isEqualTo("track1.mp3")
        assertThat(capturedEntity.notes).isEqualTo("Amazing track!")
    }

    @Test
    fun `removeConcertFromFavorites deletes by correct ID`() = runTest {
        // Given
        coEvery { mockFavoriteDao.deleteFavoriteById("concert_test1") } returns Unit

        // When
        repository.removeConcertFromFavorites("test1")

        // Then
        coVerify { mockFavoriteDao.deleteFavoriteById("concert_test1") }
    }

    @Test
    fun `removeTrackFromFavorites deletes by correct ID`() = runTest {
        // Given
        coEvery { mockFavoriteDao.deleteFavoriteById("track_test1_track1.mp3") } returns Unit

        // When
        repository.removeTrackFromFavorites("test1", "track1.mp3")

        // Then
        coVerify { mockFavoriteDao.deleteFavoriteById("track_test1_track1.mp3") }
    }

    @Test
    fun `toggleConcertFavorite adds when not favorite`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then
        assertThat(result).isTrue() // Now favorited
        coVerify { mockFavoriteDao.insertFavorite(any()) }
        coVerify(exactly = 0) { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    @Test
    fun `toggleConcertFavorite removes when already favorite`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns true
        coEvery { mockFavoriteDao.deleteFavoriteById("concert_test1") } returns Unit

        // When
        val result = repository.toggleConcertFavorite(concert)

        // Then
        assertThat(result).isFalse() // No longer favorited
        coVerify { mockFavoriteDao.deleteFavoriteById("concert_test1") }
        coVerify(exactly = 0) { mockFavoriteDao.insertFavorite(any()) }
    }

    @Test
    fun `toggleTrackFavorite adds when not favorite`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Track 1")
        coEvery { mockFavoriteDao.isTrackFavorite("test1", "track1.mp3") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit

        // When
        val result = repository.toggleTrackFavorite("test1", track)

        // Then
        assertThat(result).isTrue() // Now favorited
        coVerify { mockFavoriteDao.insertFavorite(any()) }
        coVerify(exactly = 0) { mockFavoriteDao.deleteFavoriteById(any()) }
    }

    @Test
    fun `toggleTrackFavorite removes when already favorite`() = runTest {
        // Given
        val track = createTestTrack("track1.mp3", "Track 1")
        coEvery { mockFavoriteDao.isTrackFavorite("test1", "track1.mp3") } returns true
        coEvery { mockFavoriteDao.deleteFavoriteById("track_test1_track1.mp3") } returns Unit

        // When
        val result = repository.toggleTrackFavorite("test1", track)

        // Then
        assertThat(result).isFalse() // No longer favorited
        coVerify { mockFavoriteDao.deleteFavoriteById("track_test1_track1.mp3") }
        coVerify(exactly = 0) { mockFavoriteDao.insertFavorite(any()) }
    }

    @Test
    fun `updateFavoriteNotes delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.updateFavoriteNotes("concert_test1", "Updated notes") } returns Unit

        // When
        repository.updateFavoriteNotes("concert_test1", "Updated notes")

        // Then
        coVerify { mockFavoriteDao.updateFavoriteNotes("concert_test1", "Updated notes") }
    }

    @Test
    fun `getFavoriteCount delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.getFavoriteCount() } returns 42

        // When
        val result = repository.getFavoriteCount()

        // Then
        assertThat(result).isEqualTo(42)
        coVerify { mockFavoriteDao.getFavoriteCount() }
    }

    @Test
    fun `getConcertFavoriteCount gets count by type`() = runTest {
        // Given
        coEvery { mockFavoriteDao.getFavoriteCountByType("CONCERT") } returns 15

        // When
        val result = repository.getConcertFavoriteCount()

        // Then
        assertThat(result).isEqualTo(15)
        coVerify { mockFavoriteDao.getFavoriteCountByType("CONCERT") }
    }

    @Test
    fun `getTrackFavoriteCount gets count by type`() = runTest {
        // Given
        coEvery { mockFavoriteDao.getFavoriteCountByType("TRACK") } returns 27

        // When
        val result = repository.getTrackFavoriteCount()

        // Then
        assertThat(result).isEqualTo(27)
        coVerify { mockFavoriteDao.getFavoriteCountByType("TRACK") }
    }

    @Test
    fun `clearFavoritesByType delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.deleteFavoritesByType("CONCERT") } returns Unit

        // When
        repository.clearFavoritesByType(FavoriteType.CONCERT)

        // Then
        coVerify { mockFavoriteDao.deleteFavoritesByType("CONCERT") }
    }

    @Test
    fun `removeAllFavoritesForConcert delegates to DAO`() = runTest {
        // Given
        coEvery { mockFavoriteDao.deleteFavoritesForConcert("test1") } returns Unit

        // When
        repository.removeAllFavoritesForConcert("test1")

        // Then
        coVerify { mockFavoriteDao.deleteFavoritesForConcert("test1") }
    }

    // Helper methods for creating test data
    private fun createTestFavoriteEntity(
        id: String,
        type: FavoriteType,
        concertId: String,
        trackFilename: String? = null
    ): FavoriteEntity {
        return FavoriteEntity(
            id = id,
            type = type.name,
            concertIdentifier = concertId,
            trackFilename = trackFilename,
            addedTimestamp = System.currentTimeMillis()
        )
    }

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