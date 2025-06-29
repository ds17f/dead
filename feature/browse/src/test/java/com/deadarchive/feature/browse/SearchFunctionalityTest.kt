package com.deadarchive.feature.browse

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.data.repository.ConcertRepositoryImpl
import com.deadarchive.core.data.repository.FavoriteRepositoryImpl
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.feature.browse.domain.SearchConcertsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFunctionalityTest {

    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var searchUseCase: SearchConcertsUseCase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        ).allowMainThreadQueries().build()
        
        concertDao = database.concertDao()
        favoriteDao = database.favoriteDao()
        
        // Create real repository with mock API (for offline/cached search testing)
        val mockApiService = mockk<ArchiveApiService>(relaxed = true)
        val concertRepository = ConcertRepositoryImpl(mockApiService, concertDao, favoriteDao)
        
        searchUseCase = SearchConcertsUseCase(concertRepository)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `search by year finds concerts from that year`() = runTest {
        // Given - concerts from different years
        val concerts = listOf(
            createTestConcert("gd1977-05-08", "Grateful Dead Live at Cornell", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1977-05-09", "Grateful Dead Live at Buffalo", "1977-05-09", "Memorial Auditorium", "Buffalo, NY"),
            createTestConcert("gd1978-04-15", "Grateful Dead Live at Portland", "1978-04-15", "Paramount Theatre", "Portland, OR"),
            createTestConcert("gd1995-07-09", "Jerry Garcia Band", "1995-07-09", "Soldier Field", "Chicago, IL")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for 1977
        val results = searchUseCase("1977").first()
        
        // Then - only 1977 concerts returned
        assertThat(results).hasSize(2)
        assertThat(results.map { it.date }).containsExactly("1977-05-08", "1977-05-09")
    }
    
    @Test
    fun `search by specific date finds exact concert`() = runTest {
        // Given - the famous Cornell show and others
        val concerts = listOf(
            createTestConcert("gd1977-05-08", "Grateful Dead Live at Cornell University", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1977-05-07", "Grateful Dead Live at Boston Garden", "1977-05-07", "Boston Garden", "Boston, MA"),
            createTestConcert("gd1977-05-09", "Grateful Dead Live at Buffalo", "1977-05-09", "Memorial Auditorium", "Buffalo, NY")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for specific date
        val results = searchUseCase("1977-05-08").first()
        
        // Then - only that specific concert
        assertThat(results).hasSize(1)
        assertThat(results[0].date).isEqualTo("1977-05-08")
        assertThat(results[0].venue).isEqualTo("Barton Hall")
    }
    
    @Test
    fun `search by venue finds all concerts at that venue`() = runTest {
        // Given - concerts at different venues
        val concerts = listOf(
            createTestConcert("gd1977-06-07", "Grateful Dead Live at Winterland", "1977-06-07", "Winterland Ballroom", "San Francisco, CA"),
            createTestConcert("gd1977-06-08", "Grateful Dead Live at Winterland", "1977-06-08", "Winterland Ballroom", "San Francisco, CA"),
            createTestConcert("gd1977-06-09", "Grateful Dead Live at Fillmore", "1977-06-09", "Fillmore West", "San Francisco, CA"),
            createTestConcert("gd1978-01-22", "Grateful Dead Live at Berkeley", "1978-01-22", "Berkeley Community Theatre", "Berkeley, CA")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for Winterland
        val results = searchUseCase("Winterland").first()
        
        // Then - both Winterland shows
        assertThat(results).hasSize(2)
        assertThat(results.map { it.venue }).containsExactly("Winterland Ballroom", "Winterland Ballroom")
    }
    
    @Test
    fun `search by city finds all concerts in that city`() = runTest {
        // Given - concerts in different cities
        val concerts = listOf(
            createTestConcert("gd1977-05-07", "Grateful Dead Live in Boston", "1977-05-07", "Boston Garden", "Boston, MA"),
            createTestConcert("gd1977-05-08", "Grateful Dead Live in Boston", "1977-05-08", "Boston Music Hall", "Boston, MA"),
            createTestConcert("gd1977-05-09", "Grateful Dead Live in NYC", "1977-05-09", "Madison Square Garden", "New York, NY"),
            createTestConcert("gd1978-04-15", "Grateful Dead Live in Portland", "1978-04-15", "Paramount Theatre", "Portland, OR")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for Boston
        val results = searchUseCase("Boston").first()
        
        // Then - both Boston concerts
        assertThat(results).hasSize(2)
        assertThat(results.map { it.location }).containsExactly("Boston, MA", "Boston, MA")
    }
    
    @Test
    fun `search by month and year finds concerts from that period`() = runTest {
        // Given - concerts from different months
        val concerts = listOf(
            createTestConcert("gd1977-05-08", "Grateful Dead Live at Cornell", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1977-05-09", "Grateful Dead Live at Buffalo", "1977-05-09", "Memorial Auditorium", "Buffalo, NY"),
            createTestConcert("gd1977-06-07", "Grateful Dead Live at Winterland", "1977-06-07", "Winterland Ballroom", "San Francisco, CA"),
            createTestConcert("gd1978-05-15", "Grateful Dead Live at Cornell", "1978-05-15", "Barton Hall", "Ithaca, NY")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for May 1977
        val results = searchUseCase("1977-05").first()
        
        // Then - only May 1977 concerts
        assertThat(results).hasSize(2)
        assertThat(results.map { it.date }).containsExactly("1977-05-08", "1977-05-09")
    }
    
    @Test
    fun `search returns empty list when no matches found`() = runTest {
        // Given - concerts that don't match search
        val concerts = listOf(
            createTestConcert("gd1977-05-08", "Grateful Dead Live at Cornell", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1978-04-15", "Grateful Dead Live at Portland", "1978-04-15", "Paramount Theatre", "Portland, OR")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for something that doesn't exist
        val results = searchUseCase("1990-12-31").first()
        
        // Then - empty results
        assertThat(results).isEmpty()
    }
    
    @Test
    fun `search by source type finds recordings of that quality`() = runTest {
        // Given - concerts with different source types
        val concerts = listOf(
            createTestConcert("gd1977-05-08", "Grateful Dead Live at Cornell", "1977-05-08", "Barton Hall", "Ithaca, NY", "SBD"),
            createTestConcert("gd1977-05-09", "Grateful Dead Live at Buffalo", "1977-05-09", "Memorial Auditorium", "Buffalo, NY", "AUD"),
            createTestConcert("gd1977-06-07", "Grateful Dead Live at Winterland", "1977-06-07", "Winterland Ballroom", "San Francisco, CA", "SBD")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for soundboard recordings
        val results = searchUseCase("SBD").first()
        
        // Then - only SBD recordings
        assertThat(results).hasSize(2)
        assertThat(results.map { it.source }).containsExactly("SBD", "SBD")
    }
    
    private fun createTestConcert(
        id: String,
        title: String,
        date: String,
        venue: String,
        location: String,
        source: String = "SBD"
    ) = ConcertEntity(
        id = id,
        title = title,
        date = date,
        venue = venue,
        location = location,
        year = date.substring(0, 4),
        source = source,
        taper = null,
        transferer = null,
        lineage = null,
        description = null,
        setlistRaw = null,
        uploader = null,
        addedDate = null,
        publicDate = null,
        isFavorite = false
    )
}