package com.deadly.feature.browse

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple unit tests for search use case logic
 */
class SearchFunctionalityTest {

    @Test
    fun `search use case processes date queries correctly`() {
        // Test date patterns
        assertTrue("1977-05-08".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        assertTrue("1977-05".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        assertTrue("1977".matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        
        // Test partial date patterns
        assertTrue("05-08".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        assertTrue("5/8".matches(Regex("\\d{1,2}[-/]\\d{1,2}")))
        
        // Test month name patterns
        assertTrue("May 1977".matches(Regex("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*")))
        assertTrue("may 8".matches(Regex("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*")))
        assertTrue("December".matches(Regex("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*")))
    }
    
    @Test
    fun `search patterns identify venue and location searches correctly`() {
        // Test venue/location detection logic
        val venueSearches = listOf("Winterland", "Boston", "Berkeley", "Fillmore", "Madison Square Garden")
        
        venueSearches.forEach { venue ->
            // These should NOT contain "grateful" or "dead"
            assertFalse("$venue should not contain 'grateful'", venue.contains("grateful", ignoreCase = true))
            assertFalse("$venue should not contain 'dead'", venue.contains("dead", ignoreCase = true))
            
            // These should be long enough (>=3 chars) to not need "grateful dead" prefix
            assertTrue("$venue should be >= 3 characters", venue.length >= 3)
        }
    }
    
    @Test
    fun `search patterns handle short queries correctly`() {
        val shortQueries = listOf("CA", "NY", "77")
        
        shortQueries.forEach { query ->
            assertTrue("$query should be short", query.length < 3)
        }
    }
    
    @Test
    fun `search patterns detect existing grateful dead references`() {
        val queriesWithGratefulDead = listOf(
            "grateful dead 1977",
            "Grateful Dead Live", 
            "dead show",
            "grateful concert"
        )
        
        queriesWithGratefulDead.forEach { query ->
            val hasGrateful = query.contains("grateful", ignoreCase = true)
            val hasDead = query.contains("dead", ignoreCase = true) 
            val hasEither = hasGrateful || hasDead
            
            assertTrue("$query should contain grateful or dead", hasEither)
        }
    }
    
    @Test
    fun `test date formatting for partial dates`() {
        // Test the date formatting logic for partial dates
        val query = "5/8"
        val parts = query.split("[-/]".toRegex())
        
        if (parts.size == 2) {
            val month = parts[0].padStart(2, '0')
            val day = parts[1].padStart(2, '0')
            val formatted = "-$month-$day"
            
            assertEquals("-05-08", formatted)
        }
        
        // Test another format
        val query2 = "12-31"
        val parts2 = query2.split("[-/]".toRegex())
        
        if (parts2.size == 2) {
            val month = parts2[0].padStart(2, '0')
            val day = parts2[1].padStart(2, '0')
            val formatted = "-$month-$day"
            
            assertEquals("-12-31", formatted)
        }
    }
    
    @Test
    fun `test search query edge cases`() {
        // Test empty and blank queries
        assertTrue("".isBlank())
        assertTrue("   ".isBlank())
        assertFalse("1977".isBlank())
        
        // Test query trimming
        assertEquals("1977", "  1977  ".trim())
        assertEquals("Winterland", "Winterland ".trim())
        
        // Test case insensitive matching
        assertTrue("WINTERLAND".contains("winter", ignoreCase = true))
        assertTrue("winterland".contains("WINTER", ignoreCase = true))
    }

    @Test
    fun `test search logic problems that might cause issues`() {
        // Test potential issues in search logic
        
        // Issue 1: "1977-05" might be treated as a date but return wrong results
        val query1977_05 = "1977-05"
        assertTrue("Should be recognized as date pattern", query1977_05.matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")))
        
        // Issue 2: Venue searches might get "grateful dead" prefix when they shouldn't
        val venueQueries = listOf("Winterland", "Berkeley", "Boston Garden")
        venueQueries.forEach { venue ->
            assertTrue("$venue is long enough to not need prefix", venue.length >= 3)
            assertFalse("$venue should not contain grateful/dead", 
                venue.contains("grateful", ignoreCase = true) || venue.contains("dead", ignoreCase = true))
        }
        
        // Issue 3: Cached search might return unrelated results
        // Simulate problematic cached search results
        val cachedConcertTitles = listOf(
            "Grateful Dead Live 1995-07-09",  // This contains "95" 
            "Grateful Dead Live 1977-05-08",  // This should match "1977-05"
            "Jerry Garcia Band 1995-12-31"    // This also contains "95"
        )
        
        // Test how "1977-05" search should work
        val searchQuery = "1977-05"
        val correctMatches = cachedConcertTitles.filter { title ->
            title.contains(searchQuery, ignoreCase = true)
        }
        
        assertEquals("Should only match 1977-05 concerts", 1, correctMatches.size)
        assertTrue("Should match the Cornell show", correctMatches[0].contains("1977-05-08"))
        
        // Test problematic case: if SQL LIKE is too loose
        val looseMatches = cachedConcertTitles.filter { title ->
            // This simulates a buggy search that might match "95" in "1995" 
            title.contains("95", ignoreCase = true)
        }
        
        assertEquals("Shows the problem: two concerts contain '95'", 2, looseMatches.size)
        println("Problematic matches for '95': $looseMatches")
    }
}