package com.deadarchive.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConcertDaoTest {

    private lateinit var database: ConcertDatabase
    private lateinit var concertDao: ConcertDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ConcertDatabase::class.java
        ).allowMainThreadQueries().build()
        
        concertDao = database.concertDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertConcert_storesAndRetrievesConcert() = runTest {
        val concert = createTestConcert("test-id", "Test Concert", "1977-12-31")
        
        concertDao.insertConcert(concert)
        
        val retrieved = concertDao.getConcertById("test-id")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.id).isEqualTo("test-id")
        assertThat(retrieved.title).isEqualTo("Test Concert")
        assertThat(retrieved.date).isEqualTo("1977-12-31")
    }

    @Test
    fun insertConcerts_storesMultipleConcerts() = runTest {
        val concerts = listOf(
            createTestConcert("id1", "Concert 1", "1977-01-01"),
            createTestConcert("id2", "Concert 2", "1977-01-02"),
            createTestConcert("id3", "Concert 3", "1978-01-01")
        )
        
        concertDao.insertConcerts(concerts)
        
        val concert1 = concertDao.getConcertById("id1")
        val concert2 = concertDao.getConcertById("id2")
        val concert3 = concertDao.getConcertById("id3")
        
        assertThat(concert1?.title).isEqualTo("Concert 1")
        assertThat(concert2?.title).isEqualTo("Concert 2")
        assertThat(concert3?.title).isEqualTo("Concert 3")
    }

    @Test
    fun insertConcert_replacesExistingConcert() = runTest {
        val originalConcert = createTestConcert("test-id", "Original Title", "1977-12-31")
        val updatedConcert = createTestConcert("test-id", "Updated Title", "1977-12-31")
        
        concertDao.insertConcert(originalConcert)
        concertDao.insertConcert(updatedConcert)
        
        val retrieved = concertDao.getConcertById("test-id")
        assertThat(retrieved?.title).isEqualTo("Updated Title")
    }

    @Test
    fun searchConcerts_findsMatchingTitle() = runTest {
        val concerts = listOf(
            createTestConcert("id1", "Grateful Dead Live at Winterland", "1977-12-31"),
            createTestConcert("id2", "Jerry Garcia Band", "1977-01-01"),
            createTestConcert("id3", "Phil Lesh Concert", "1977-02-01")
        )
        concertDao.insertConcerts(concerts)
        
        val results = concertDao.searchConcerts("Grateful Dead")
        
        assertThat(results).hasSize(1)
        assertThat(results[0].title).isEqualTo("Grateful Dead Live at Winterland")
    }

    @Test
    fun searchConcerts_findsMatchingVenue() = runTest {
        val concerts = listOf(
            createTestConcert("id1", "Concert 1", "1977-12-31", venue = "Winterland Arena"),
            createTestConcert("id2", "Concert 2", "1977-01-01", venue = "Madison Square Garden"),
            createTestConcert("id3", "Concert 3", "1977-02-01", venue = "The Forum")
        )
        concertDao.insertConcerts(concerts)
        
        val results = concertDao.searchConcerts("Winterland")
        
        assertThat(results).hasSize(1)
        assertThat(results[0].venue).isEqualTo("Winterland Arena")
    }

    @Test
    fun searchConcerts_findsMatchingDate() = runTest {
        val concerts = listOf(
            createTestConcert("id1", "Concert 1", "1977-12-31"),
            createTestConcert("id2", "Concert 2", "1978-01-01"),
            createTestConcert("id3", "Concert 3", "1979-02-01")
        )
        concertDao.insertConcerts(concerts)
        
        val results = concertDao.searchConcerts("1977")
        
        assertThat(results).hasSize(1)
        assertThat(results[0].date).isEqualTo("1977-12-31")
    }

    @Test
    fun cleanupOldCachedConcerts_removesOldNonFavorites() = runTest {
        val currentTime = System.currentTimeMillis()
        val oldTimestamp = currentTime - (48 * 60 * 60 * 1000L) // 48 hours ago
        val recentTimestamp = currentTime - (12 * 60 * 60 * 1000L) // 12 hours ago
        
        val concerts = listOf(
            createTestConcert("old-nonfav", "Old Non-Favorite", "1977-01-01", 
                isFavorite = false, cachedTimestamp = oldTimestamp),
            createTestConcert("old-fav", "Old Favorite", "1977-01-02", 
                isFavorite = true, cachedTimestamp = oldTimestamp),
            createTestConcert("recent-nonfav", "Recent Non-Favorite", "1977-01-03", 
                isFavorite = false, cachedTimestamp = recentTimestamp)
        )
        concertDao.insertConcerts(concerts)
        
        val cutoffTimestamp = currentTime - (24 * 60 * 60 * 1000L) // 24 hours ago
        concertDao.cleanupOldCachedConcerts(cutoffTimestamp)
        
        // Old non-favorite should be removed
        assertThat(concertDao.getConcertById("old-nonfav")).isNull()
        
        // Old favorite should be preserved
        assertThat(concertDao.getConcertById("old-fav")).isNotNull()
        
        // Recent non-favorite should be preserved
        assertThat(concertDao.getConcertById("recent-nonfav")).isNotNull()
    }

    @Test
    fun concertExists_returnsCorrectCount() = runTest {
        val concert = createTestConcert("test-id", "Test Concert", "1977-12-31")
        
        // Before insert
        assertThat(concertDao.concertExists("test-id")).isEqualTo(0)
        
        // After insert
        concertDao.insertConcert(concert)
        assertThat(concertDao.concertExists("test-id")).isEqualTo(1)
        
        // Non-existent ID
        assertThat(concertDao.concertExists("nonexistent")).isEqualTo(0)
    }

    // ===================
    // TEST HELPER METHODS
    // ===================

    private fun createTestConcert(
        id: String,
        title: String,
        date: String,
        venue: String = "Test Venue",
        isFavorite: Boolean = false,
        cachedTimestamp: Long = System.currentTimeMillis()
    ): ConcertEntity {
        return ConcertEntity(
            id = id,
            title = title,
            date = date,
            venue = venue,
            location = "Test City, CA",
            year = date.take(4),
            source = "SBD",
            taper = "Test Taper",
            transferer = "Test Transferer", 
            lineage = "Test Lineage",
            description = "Test Description",
            setlistRaw = "Test Setlist",
            uploader = "Test Uploader",
            addedDate = "2023-01-01T00:00:00Z",
            publicDate = "2023-01-01T00:00:00Z",
            isFavorite = isFavorite,
            cachedTimestamp = cachedTimestamp
        )
    }
}