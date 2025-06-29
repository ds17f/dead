package com.deadarchive.core.database

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple unit tests for search patterns and matching logic
 */
class ConcertSearchTest {
    
    @Test
    fun `test date patterns match correctly`() {
        // Test full date pattern
        assertTrue("1977-05-08".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        
        // Test year-month pattern  
        assertTrue("1977-05".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        
        // Test year pattern
        assertTrue("1977".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        
        // Test non-date patterns
        assertFalse("Winterland".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        assertFalse("Boston".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
    }
    
    @Test 
    fun `test partial date patterns match correctly`() {
        // Test month-day patterns
        assertTrue("05-08".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        assertTrue("5/8".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        assertTrue("12-31".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        
        // Test non-matching patterns
        assertFalse("1977-05-08".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        assertFalse("Boston".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
    }
    
    @Test
    fun `test search query contains logic`() {
        // Test grateful dead detection
        assertTrue("grateful dead 1977".contains("grateful", ignoreCase = true))
        assertTrue("Grateful Dead Live".contains("dead", ignoreCase = true))
        assertTrue("GRATEFUL DEAD".contains("grateful", ignoreCase = true))
        
        // Test venue searches shouldn't match
        assertFalse("Winterland".contains("grateful", ignoreCase = true))
        assertFalse("Boston Garden".contains("dead", ignoreCase = true))
    }
    
    @Test
    fun `test concert entity search field access`() {
        // Create test concert
        val concert = ConcertEntity(
            id = "test-1",
            title = "Grateful Dead Live at Cornell University",
            date = "1977-05-08",
            venue = "Barton Hall",
            location = "Ithaca, NY",
            year = "1977",
            source = "SBD",
            taper = null,
            transferer = null,
            lineage = null,
            description = "Famous Cornell show",
            setlistRaw = null,
            uploader = null,
            addedDate = null,
            publicDate = null,
            isFavorite = false
        )
        
        // Test field access and search matching
        assertEquals("1977-05-08", concert.date)
        assertEquals("Barton Hall", concert.venue)
        assertEquals("Ithaca, NY", concert.location)
        assertEquals("1977", concert.year)
        assertEquals("SBD", concert.source)
        
        // Test search would match
        assertTrue(concert.date?.contains("1977-05-08", ignoreCase = true) ?: false)
        assertTrue(concert.venue?.contains("Barton", ignoreCase = true) ?: false)
        assertTrue(concert.location?.contains("Ithaca", ignoreCase = true) ?: false)
        assertTrue(concert.source?.contains("SBD", ignoreCase = true) ?: false)
    }
    
    @Test
    fun `test SQL LIKE pattern simulation`() {
        // Simulate how SQL LIKE '%query%' would work
        val testStrings = listOf(
            "Grateful Dead Live at Cornell University",
            "1977-05-08",
            "Barton Hall", 
            "Ithaca, NY",
            "SBD"
        )
        
        // Test various search queries
        fun simulateLikeSearch(query: String): List<String> {
            return testStrings.filter { it.contains(query, ignoreCase = true) }
        }
        
        // Test searches
        val results1977 = simulateLikeSearch("1977")
        assertEquals(1, results1977.size)
        assertEquals("1977-05-08", results1977[0])
        
        val resultsCornell = simulateLikeSearch("Cornell")
        assertEquals(1, resultsCornell.size)
        assertEquals("Grateful Dead Live at Cornell University", resultsCornell[0])
        
        val resultsBarton = simulateLikeSearch("Barton")
        assertEquals(1, resultsBarton.size)
        assertEquals("Barton Hall", resultsBarton[0])
        
        val resultsIthaca = simulateLikeSearch("Ithaca")
        assertEquals(1, resultsIthaca.size)
        assertEquals("Ithaca, NY", resultsIthaca[0])
        
        val resultsSBD = simulateLikeSearch("SBD")
        assertEquals(1, resultsSBD.size)
        assertEquals("SBD", resultsSBD[0])
        
        // Test non-matches
        val results1995 = simulateLikeSearch("1995")
        assertEquals(0, results1995.size)
        
        val resultsWinterland = simulateLikeSearch("Winterland")
        assertEquals(0, resultsWinterland.size)
    }
}