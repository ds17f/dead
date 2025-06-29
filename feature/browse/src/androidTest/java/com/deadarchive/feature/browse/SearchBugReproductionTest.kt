package com.deadarchive.feature.browse

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.data.repository.ConcertRepositoryImpl
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.feature.browse.domain.SearchConcertsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import org.junit.Assert.*

/**
 * Test that reproduces the real search bug: searching for "1977-05-08" should return 
 * the Cornell show, NOT shows from 1995 or other years
 */
@RunWith(AndroidJUnit4::class)
class SearchBugReproductionTest {

    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var searchUseCase: SearchConcertsUseCase
    private lateinit var concertRepository: ConcertRepositoryImpl
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        ).allowMainThreadQueries().build()
        
        concertDao = database.concertDao()
        favoriteDao = database.favoriteDao()
        
        // Mock API service to simulate network failure (forces cached search)
        val mockApiService = mockk<ArchiveApiService>()
        coEvery { mockApiService.searchConcerts(any(), any(), any()) } throws Exception("Network error")
        
        concertRepository = ConcertRepositoryImpl(mockApiService, concertDao, favoriteDao)
        searchUseCase = SearchConcertsUseCase(concertRepository)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `searching for 1977-05-08 should return Cornell show not 1995 shows`() = runTest {
        // Given - mixed cached data from different searches (this simulates the real scenario)
        val mixedCachedConcerts = listOf(
            // The famous Cornell show we want
            createTestConcert(
                id = "gd1977-05-08", 
                title = "Grateful Dead Live at Cornell University", 
                date = "1977-05-08", 
                venue = "Barton Hall", 
                location = "Ithaca, NY"
            ),
            // Other 1977 shows
            createTestConcert(
                id = "gd1977-05-07", 
                title = "Grateful Dead Live at Boston Garden", 
                date = "1977-05-07", 
                venue = "Boston Garden", 
                location = "Boston, MA"
            ),
            createTestConcert(
                id = "gd1977-05-09", 
                title = "Grateful Dead Live at Buffalo Memorial", 
                date = "1977-05-09", 
                venue = "Memorial Auditorium", 
                location = "Buffalo, NY"
            ),
            // 1995 shows that should NOT appear in 1977-05-08 search
            createTestConcert(
                id = "gd1995-07-09", 
                title = "Grateful Dead Live at Soldier Field", 
                date = "1995-07-09", 
                venue = "Soldier Field", 
                location = "Chicago, IL"
            ),
            createTestConcert(
                id = "jgb1995-12-31", 
                title = "Jerry Garcia Band New Years Eve", 
                date = "1995-12-31", 
                venue = "Oakland Coliseum", 
                location = "Oakland, CA"
            )
        )
        
        // Cache all the mixed data (simulates user having searched for various things)
        concertDao.insertConcerts(mixedCachedConcerts)
        
        // When - search for the specific Cornell show date
        val results = searchUseCase("1977-05-08").first()
        
        // Then - should ONLY return the Cornell show
        assertEquals("Should return exactly 1 concert", 1, results.size)
        
        val cornellShow = results[0]
        assertEquals("Should be the Cornell show", "gd1977-05-08", cornellShow.identifier)
        assertEquals("Should be correct date", "1977-05-08", cornellShow.date)
        assertEquals("Should be Cornell venue", "Barton Hall", cornellShow.venue)
        assertTrue("Should be in Ithaca", cornellShow.location?.contains("Ithaca") == true)
        
        // Verify NO 1995 shows are returned
        val has1995Shows = results.any { it.date.contains("1995") }
        assertFalse("Should NOT return any 1995 shows", has1995Shows)
        
        println("✅ SUCCESS: Search for '1977-05-08' correctly returned: ${cornellShow.title}")
    }
    
    @Test
    fun `searching for 1977-05 should return May 1977 shows not 1995 shows`() = runTest {
        // Given - same mixed cached data
        val mixedCachedConcerts = listOf(
            createTestConcert("gd1977-05-08", "Cornell Show", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1977-05-07", "Boston Show", "1977-05-07", "Boston Garden", "Boston, MA"),
            createTestConcert("gd1977-05-09", "Buffalo Show", "1977-05-09", "Memorial Auditorium", "Buffalo, NY"),
            createTestConcert("gd1977-06-07", "June Show", "1977-06-07", "Winterland", "San Francisco, CA"),
            createTestConcert("gd1995-07-09", "1995 Show", "1995-07-09", "Soldier Field", "Chicago, IL"),
        )
        concertDao.insertConcerts(mixedCachedConcerts)
        
        // When - search for May 1977
        val results = searchUseCase("1977-05").first()
        
        // Then - should return exactly 3 May 1977 shows
        assertEquals("Should return exactly 3 May 1977 concerts", 3, results.size)
        
        // Verify all results are from May 1977
        results.forEach { concert ->
            assertTrue("${concert.title} should be from May 1977", 
                concert.date.startsWith("1977-05"))
        }
        
        // Verify specific shows are included
        val identifiers = results.map { it.identifier }
        assertTrue("Should include Cornell show", identifiers.contains("gd1977-05-08"))
        assertTrue("Should include Boston show", identifiers.contains("gd1977-05-07"))
        assertTrue("Should include Buffalo show", identifiers.contains("gd1977-05-09"))
        
        // Verify NO 1995 or June shows
        val has1995Shows = results.any { it.date.contains("1995") }
        val hasJuneShows = results.any { it.date.contains("1977-06") }
        assertFalse("Should NOT return 1995 shows", has1995Shows)
        assertFalse("Should NOT return June 1977 shows", hasJuneShows)
        
        println("✅ SUCCESS: Search for '1977-05' correctly returned ${results.size} May 1977 shows")
    }
    
    @Test
    fun `debug why searches might return wrong results`() = runTest {
        // Given - problematic data that might cause the bug
        val problematicConcerts = listOf(
            createTestConcert("gd1977-05-08", "Cornell 1977", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createTestConcert("gd1995-07-09", "Soldier Field 1995", "1995-07-09", "Soldier Field", "Chicago, IL"),
        )
        concertDao.insertConcerts(problematicConcerts)
        
        // Test raw DAO search to see what's happening
        println("=== DEBUG: Testing raw DAO searches ===")
        
        val daoResults1977 = concertDao.searchConcerts("1977-05")
        println("DAO search for '1977-05': ${daoResults1977.size} results")
        daoResults1977.forEach { concert ->  
            println("  - ${concert.title} (${concert.date}) at ${concert.venue}")
        }
        
        val daoResultsSpecific = concertDao.searchConcerts("1977-05-08")
        println("DAO search for '1977-05-08': ${daoResultsSpecific.size} results")
        daoResultsSpecific.forEach { concert ->
            println("  - ${concert.title} (${concert.date}) at ${concert.venue}")
        }
        
        val daoResultsDate = concertDao.searchConcertsByDate("1977-05")
        println("DAO date search for '1977-05': ${daoResultsDate.size} results")
        daoResultsDate.forEach { concert ->
            println("  - ${concert.title} (${concert.date}) at ${concert.venue}")
        }
        
        // Test if "95" substring matching is the problem
        val problemSearch = concertDao.searchConcerts("95")
        println("DAO search for '95': ${problemSearch.size} results")
        problemSearch.forEach { concert ->
            println("  - ${concert.title} (${concert.date}) - PROBLEMATIC MATCH")
        }
        
        // This test should help us see exactly what the database is returning
    }
    
    private fun createTestConcert(
        id: String,
        title: String,
        date: String,
        venue: String,
        location: String
    ) = ConcertEntity(
        id = id,
        title = title,
        date = date,
        venue = venue,
        location = location,
        year = date.substring(0, 4),
        source = "SBD",
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