package com.deadarchive.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.model.FavoriteType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var database: DeadArchiveDatabase
    private lateinit var favoriteDao: FavoriteDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        ).allowMainThreadQueries().build()
        
        favoriteDao = database.favoriteDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertFavorite_storesAndRetrievesFavorite() = runTest {
        // Given
        val favorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        
        // When
        favoriteDao.insertFavorite(favorite)
        
        // Then
        val retrieved = favoriteDao.getFavoriteById("concert_test1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.id).isEqualTo("concert_test1")
        assertThat(retrieved.type).isEqualTo("CONCERT")
        assertThat(retrieved.concertIdentifier).isEqualTo("test1")
        assertThat(retrieved.trackFilename).isNull()
    }

    @Test
    fun insertMultipleFavorites_storesAllCorrectly() = runTest {
        // Given
        val favorites = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3"),
            createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        )
        
        // When
        favoriteDao.insertFavorites(favorites)
        
        // Then
        val allFavorites = favoriteDao.getAllFavorites().first()
        assertThat(allFavorites).hasSize(3)
        
        val concertFavorites = allFavorites.filter { it.type == "CONCERT" }
        val trackFavorites = allFavorites.filter { it.type == "TRACK" }
        assertThat(concertFavorites).hasSize(2)
        assertThat(trackFavorites).hasSize(1)
    }

    @Test
    fun getAllFavorites_returnsInDescendingTimestampOrder() = runTest {
        // Given - Insert favorites with different timestamps
        val older = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(addedTimestamp = 1000L)
        val newer = createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
            .copy(addedTimestamp = 2000L)
        
        favoriteDao.insertFavorite(older)
        favoriteDao.insertFavorite(newer)
        
        // When
        val favorites = favoriteDao.getAllFavorites().first()
        
        // Then
        assertThat(favorites).hasSize(2)
        assertThat(favorites[0].addedTimestamp).isEqualTo(2000L) // Newer first
        assertThat(favorites[1].addedTimestamp).isEqualTo(1000L) // Older second
    }

    @Test
    fun getFavoritesByType_filtersCorrectly() = runTest {
        // Given
        val concertFavorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        val trackFavorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        
        favoriteDao.insertFavorite(concertFavorite)
        favoriteDao.insertFavorite(trackFavorite)
        
        // When
        val concerts = favoriteDao.getFavoritesByType("CONCERT").first()
        val tracks = favoriteDao.getFavoritesByType("TRACK").first()
        
        // Then
        assertThat(concerts).hasSize(1)
        assertThat(concerts[0].type).isEqualTo("CONCERT")
        
        assertThat(tracks).hasSize(1)
        assertThat(tracks[0].type).isEqualTo("TRACK")
        assertThat(tracks[0].trackFilename).isEqualTo("track1.mp3")
    }

    @Test
    fun getFavoritesForConcert_returnsAllFavoritesForConcert() = runTest {
        // Given
        val concertFavorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        val track1Favorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        val track2Favorite = createTestFavoriteEntity("track_test1_track2", FavoriteType.TRACK, "test1", "track2.mp3")
        val otherConcertFavorite = createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        
        favoriteDao.insertFavorites(listOf(concertFavorite, track1Favorite, track2Favorite, otherConcertFavorite))
        
        // When
        val test1Favorites = favoriteDao.getFavoritesForConcert("test1").first()
        
        // Then
        assertThat(test1Favorites).hasSize(3) // Concert + 2 tracks
        assertThat(test1Favorites.all { it.concertIdentifier == "test1" }).isTrue()
        
        val concertFavs = test1Favorites.filter { it.type == "CONCERT" }
        val trackFavs = test1Favorites.filter { it.type == "TRACK" }
        assertThat(concertFavs).hasSize(1)
        assertThat(trackFavs).hasSize(2)
    }

    @Test
    fun isFavorite_returnsTrueForExistingFavorite() = runTest {
        // Given
        val favorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        favoriteDao.insertFavorite(favorite)
        
        // When & Then
        assertThat(favoriteDao.isFavorite("concert_test1")).isTrue()
        assertThat(favoriteDao.isFavorite("nonexistent")).isFalse()
    }

    @Test
    fun isConcertFavorite_checksForConcertTypeSpecifically() = runTest {
        // Given
        val concertFavorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        val trackFavorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        
        favoriteDao.insertFavorite(concertFavorite)
        favoriteDao.insertFavorite(trackFavorite)
        
        // When & Then
        assertThat(favoriteDao.isConcertFavorite("test1")).isTrue()
        assertThat(favoriteDao.isConcertFavorite("test2")).isFalse()
    }

    @Test
    fun isTrackFavorite_checksForTrackTypeSpecifically() = runTest {
        // Given
        val trackFavorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        favoriteDao.insertFavorite(trackFavorite)
        
        // When & Then
        assertThat(favoriteDao.isTrackFavorite("test1", "track1.mp3")).isTrue()
        assertThat(favoriteDao.isTrackFavorite("test1", "track2.mp3")).isFalse()
        assertThat(favoriteDao.isTrackFavorite("test2", "track1.mp3")).isFalse()
    }

    @Test
    fun getFavoriteCount_returnsCorrectCount() = runTest {
        // Given
        val favorites = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3"),
            createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        )
        favoriteDao.insertFavorites(favorites)
        
        // When & Then
        assertThat(favoriteDao.getFavoriteCount()).isEqualTo(3)
    }

    @Test
    fun getFavoriteCountByType_returnsCorrectCountPerType() = runTest {
        // Given
        val favorites = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3"),
            createTestFavoriteEntity("track_test1_track2", FavoriteType.TRACK, "test1", "track2.mp3"),
            createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        )
        favoriteDao.insertFavorites(favorites)
        
        // When & Then
        assertThat(favoriteDao.getFavoriteCountByType("CONCERT")).isEqualTo(2)
        assertThat(favoriteDao.getFavoriteCountByType("TRACK")).isEqualTo(2)
        assertThat(favoriteDao.getFavoriteCountByType("NONEXISTENT")).isEqualTo(0)
    }

    @Test
    fun deleteFavoriteById_removesFavorite() = runTest {
        // Given
        val favorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        favoriteDao.insertFavorite(favorite)
        
        // Verify it exists
        assertThat(favoriteDao.isFavorite("concert_test1")).isTrue()
        
        // When
        favoriteDao.deleteFavoriteById("concert_test1")
        
        // Then
        assertThat(favoriteDao.isFavorite("concert_test1")).isFalse()
        assertThat(favoriteDao.getFavoriteCount()).isEqualTo(0)
    }

    @Test
    fun deleteFavoritesForConcert_removesAllConcertFavorites() = runTest {
        // Given
        val concertFavorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
        val track1Favorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
        val track2Favorite = createTestFavoriteEntity("track_test1_track2", FavoriteType.TRACK, "test1", "track2.mp3")
        val otherConcertFavorite = createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        
        favoriteDao.insertFavorites(listOf(concertFavorite, track1Favorite, track2Favorite, otherConcertFavorite))
        
        // When
        favoriteDao.deleteFavoritesForConcert("test1")
        
        // Then
        val remaining = favoriteDao.getAllFavorites().first()
        assertThat(remaining).hasSize(1) // Only test2 concert should remain
        assertThat(remaining[0].concertIdentifier).isEqualTo("test2")
    }

    @Test
    fun deleteFavoritesByType_removesAllOfSpecificType() = runTest {
        // Given
        val favorites = listOf(
            createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1"),
            createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3"),
            createTestFavoriteEntity("track_test1_track2", FavoriteType.TRACK, "test1", "track2.mp3"),
            createTestFavoriteEntity("concert_test2", FavoriteType.CONCERT, "test2")
        )
        favoriteDao.insertFavorites(favorites)
        
        // When
        favoriteDao.deleteFavoritesByType("TRACK")
        
        // Then
        val remaining = favoriteDao.getAllFavorites().first()
        assertThat(remaining).hasSize(2) // Only concerts should remain
        assertThat(remaining.all { it.type == "CONCERT" }).isTrue()
    }

    @Test
    fun updateFavoriteNotes_updatesNotesCorrectly() = runTest {
        // Given
        val favorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(notes = "Original notes")
        favoriteDao.insertFavorite(favorite)
        
        // When
        favoriteDao.updateFavoriteNotes("concert_test1", "Updated notes")
        
        // Then
        val updated = favoriteDao.getFavoriteById("concert_test1")
        assertThat(updated).isNotNull()
        assertThat(updated!!.notes).isEqualTo("Updated notes")
    }

    @Test
    fun updateFavoriteNotes_canSetNotesToNull() = runTest {
        // Given
        val favorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(notes = "Some notes")
        favoriteDao.insertFavorite(favorite)
        
        // When
        favoriteDao.updateFavoriteNotes("concert_test1", null)
        
        // Then
        val updated = favoriteDao.getFavoriteById("concert_test1")
        assertThat(updated).isNotNull()
        assertThat(updated!!.notes).isNull()
    }

    @Test
    fun insertFavorite_withConflict_replacesExisting() = runTest {
        // Given
        val original = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(notes = "Original notes", addedTimestamp = 1000L)
        favoriteDao.insertFavorite(original)
        
        // When - Insert with same ID but different data
        val replacement = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(notes = "Replacement notes", addedTimestamp = 2000L)
        favoriteDao.insertFavorite(replacement)
        
        // Then
        val result = favoriteDao.getFavoriteById("concert_test1")
        assertThat(result).isNotNull()
        assertThat(result!!.notes).isEqualTo("Replacement notes")
        assertThat(result.addedTimestamp).isEqualTo(2000L)
        
        // Should still be only one favorite total
        assertThat(favoriteDao.getFavoriteCount()).isEqualTo(1)
    }

    @Test
    fun favoriteEntities_convertToFavoriteItemCorrectly() = runTest {
        // Given
        val concertFavorite = createTestFavoriteEntity("concert_test1", FavoriteType.CONCERT, "test1")
            .copy(notes = "Great show!")
        val trackFavorite = createTestFavoriteEntity("track_test1_track1", FavoriteType.TRACK, "test1", "track1.mp3")
            .copy(notes = "Amazing solo!")
        
        favoriteDao.insertFavorites(listOf(concertFavorite, trackFavorite))
        
        // When
        val storedConcert = favoriteDao.getFavoriteById("concert_test1")!!
        val storedTrack = favoriteDao.getFavoriteById("track_test1_track1")!!
        
        // Then
        val concertItem = storedConcert.toFavoriteItem()
        assertThat(concertItem.id).isEqualTo("concert_test1")
        assertThat(concertItem.type).isEqualTo(FavoriteType.CONCERT)
        assertThat(concertItem.concertIdentifier).isEqualTo("test1")
        assertThat(concertItem.trackFilename).isNull()
        assertThat(concertItem.notes).isEqualTo("Great show!")
        
        val trackItem = storedTrack.toFavoriteItem()
        assertThat(trackItem.id).isEqualTo("track_test1_track1")
        assertThat(trackItem.type).isEqualTo(FavoriteType.TRACK)
        assertThat(trackItem.concertIdentifier).isEqualTo("test1")
        assertThat(trackItem.trackFilename).isEqualTo("track1.mp3")
        assertThat(trackItem.notes).isEqualTo("Amazing solo!")
    }

    // Helper method for creating test data
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
}