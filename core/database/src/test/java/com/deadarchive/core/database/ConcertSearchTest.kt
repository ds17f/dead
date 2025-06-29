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
class ConcertSearchTest {

    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        ).allowMainThreadQueries().build()
        
        concertDao = database.concertDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `database search finds concerts by date`() = runTest {
        // Given - concerts with different dates
        val concerts = listOf(
            createConcert("1", "Cornell Show", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createConcert("2", "Buffalo Show", "1977-05-09", "Memorial Auditorium", "Buffalo, NY"),
            createConcert("3", "Chicago Show", "1995-07-09", "Soldier Field", "Chicago, IL")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search by year
        val results1977 = concertDao.searchConcerts("1977")
        
        // Then - finds 1977 concerts
        assertThat(results1977).hasSize(2)
        assertThat(results1977.map { it.date }).containsExactly("1977-05-08", "1977-05-09")
        
        // When - search by specific date
        val resultsCornell = concertDao.searchConcerts("1977-05-08")
        
        // Then - finds exact concert
        assertThat(resultsCornell).hasSize(1)
        assertThat(resultsCornell[0].venue).isEqualTo("Barton Hall")
    }
    
    @Test
    fun `database search finds concerts by venue`() = runTest {
        // Given - concerts at different venues
        val concerts = listOf(
            createConcert("1", "Winterland Night 1", "1977-06-07", "Winterland Ballroom", "San Francisco, CA"),
            createConcert("2", "Winterland Night 2", "1977-06-08", "Winterland Ballroom", "San Francisco, CA"),
            createConcert("3", "Fillmore Show", "1977-06-09", "Fillmore West", "San Francisco, CA")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search by venue
        val winterlandResults = concertDao.searchConcerts("Winterland")
        
        // Then - finds both Winterland shows
        assertThat(winterlandResults).hasSize(2)
        assertThat(winterlandResults.map { it.title }).containsExactly("Winterland Night 1", "Winterland Night 2")
    }
    
    @Test
    fun `database search finds concerts by location`() = runTest {
        // Given - concerts in different cities
        val concerts = listOf(
            createConcert("1", "Boston Garden", "1977-05-07", "Boston Garden", "Boston, MA"),
            createConcert("2", "Boston Music Hall", "1977-05-08", "Music Hall", "Boston, MA"),
            createConcert("3", "NYC Show", "1977-05-09", "Madison Square Garden", "New York, NY")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search by city
        val bostonResults = concertDao.searchConcerts("Boston")
        
        // Then - finds both Boston concerts
        assertThat(bostonResults).hasSize(2)
        assertThat(bostonResults.map { it.location }).containsExactly("Boston, MA", "Boston, MA")
    }
    
    @Test
    fun `database date-specific search works correctly`() = runTest {
        // Given - concerts with different date patterns
        val concerts = listOf(
            createConcert("1", "Cornell Show", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            createConcert("2", "Buffalo Show", "1977-05-09", "Memorial Auditorium", "Buffalo, NY"),
            createConcert("3", "June Show", "1977-06-07", "Winterland Ballroom", "San Francisco, CA"),
            createConcert("4", "Next Year", "1978-05-15", "Some Venue", "Some City, ST")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search by date prefix
        val may1977Results = concertDao.searchConcertsByDate("1977-05")
        
        // Then - finds only May 1977 concerts
        assertThat(may1977Results).hasSize(2)
        assertThat(may1977Results.map { it.date }).containsExactly("1977-05-08", "1977-05-09")
    }
    
    @Test
    fun `search returns empty when no matches`() = runTest {
        // Given - concerts that won't match search
        val concerts = listOf(
            createConcert("1", "Cornell Show", "1977-05-08", "Barton Hall", "Ithaca, NY")
        )
        concertDao.insertConcerts(concerts)
        
        // When - search for non-existent data
        val results = concertDao.searchConcerts("1990")
        
        // Then - no results
        assertThat(results).isEmpty()
    }
    
    private fun createConcert(
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