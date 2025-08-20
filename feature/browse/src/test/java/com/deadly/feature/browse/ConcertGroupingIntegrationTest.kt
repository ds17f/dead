package com.deadly.feature.browse

import com.deadly.core.model.Concert
import com.deadly.core.network.mapper.ArchiveMapper
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Integration test to verify concert grouping logic works correctly
 * Uses real data patterns from the app export
 */
@RunWith(JUnit4::class)
class ConcertGroupingIntegrationTest {

    @Test
    fun testConcertGroupingWithReal1995Data() {
        // Simulate the real data from your export for 1995-07-09
        val concerts = listOf(
            Concert(
                identifier = "gd1995-07-09.schoeps.wklitz.95444.flac1648",
                title = "Grateful Dead Live at Soldier Field on 1995-07-09",
                date = "1995-07-09T00:00:00Z", // ISO format with timestamp
                venue = "Soldier Field",
                location = "Chicago, IL",
                source = "Schoeps MK4's>Rbox>Casio DAR-100 FOB/DFC"
            ),
            Concert(
                identifier = "gd1995-07-09.sbd.miller.114369.flac16",
                title = "The Grateful Dead Live at Soldier Field on 1995-07-09",
                date = "1995-07-09T00:00:00Z", // Same date, different timestamp format
                venue = "Soldier Field",
                location = "Chicago, IL",
                source = "SBD -> Dat (44.1k)"
            ),
            Concert(
                identifier = "gd1995-07-09.sbd.gardner.4091.shnf",
                title = "Grateful Dead Live at Soldier Field on 1995-07-09",
                date = "1995-07-09T00:00:00Z",
                venue = "Soldier Field",
                location = "Chicago, IL",
                source = "dsbd(pre-FM)>dat"
            ),
            Concert(
                identifier = "gd1995-07-09.aud.mahoney.86405.flac16",
                title = "Grateful Dead Live at Soldier Field on 1995-07-09",
                date = "1995-07-09T00:00:00Z",
                venue = "Soldier Field",
                location = "Chicago, IL",
                source = "? > Patch > Sony D6 > Master Cassette"
            )
        )

        // Test the grouping logic
        val concertsNew = ArchiveMapper.run { concerts.migrateToConcertNew() }

        // Debug output
        println("=== CONCERT GROUPING TEST ===")
        println("Input concerts: ${concerts.size}")
        println("Output concerts: ${concertsNew.size}")
        
        concertsNew.forEachIndexed { index, concert ->
            println("Concert $index:")
            println("  ID: ${concert.concertId}")
            println("  Date: ${concert.date}")
            println("  Venue: ${concert.venue}")
            println("  Recordings: ${concert.recordingCount}")
            concert.recordings.forEach { recording ->
                println("    - ${recording.identifier} (${recording.source})")
            }
        }

        // Assertions
        assertEquals("Should group all recordings into 1 concert", 1, concertsNew.size)
        
        val concert = concertsNew.first()
        assertEquals("Concert date should be normalized", "1995-07-09", concert.date)
        assertEquals("Concert venue should match", "Soldier Field", concert.venue)
        assertEquals("Should have 4 recordings", 4, concert.recordingCount)
        
        // Verify recording source types are standardized
        val sources = concert.recordings.map { it.source }.toSet()
        println("Standardized sources: $sources")
        assertTrue("Should contain SBD recordings", sources.contains("SBD"))
        assertTrue("Should contain AUD recordings", sources.contains("AUD"))
    }

    @Test
    fun testEdgeCaseWithDifferentVenueSpellings() {
        val concerts = listOf(
            Concert(
                identifier = "test1",
                title = "Test Concert 1",
                date = "1995-07-09T00:00:00Z",
                venue = "Soldier Field",
                source = "SBD"
            ),
            Concert(
                identifier = "test2", 
                title = "Test Concert 2",
                date = "1995-07-09T00:00:00Z",
                venue = "Soldier Field ", // Extra space
                source = "AUD"
            )
        )

        val concertsNew = ArchiveMapper.run { concerts.migrateToConcertNew() }
        
        println("=== VENUE SPELLING TEST ===")
        println("Input concerts: ${concerts.size}")
        println("Output concerts: ${concertsNew.size}")
        
        // This might reveal why we're getting 2 instead of 1
        concertsNew.forEach { concert ->
            println("Venue: '${concert.venue}' (length: ${concert.venue?.length})")
        }
    }

    @Test
    fun testNullVenueHandling() {
        val concerts = listOf(
            Concert(
                identifier = "test1",
                title = "Test Concert 1", 
                date = "1995-07-09T00:00:00Z",
                venue = null,
                source = "SBD"
            ),
            Concert(
                identifier = "test2",
                title = "Test Concert 2",
                date = "1995-07-09T00:00:00Z", 
                venue = null,
                source = "AUD"
            )
        )

        val concertsNew = ArchiveMapper.run { concerts.migrateToConcertNew() }
        
        println("=== NULL VENUE TEST ===")
        println("Input concerts: ${concerts.size}")
        println("Output concerts: ${concertsNew.size}")
        
        assertEquals("Should group concerts with null venues", 1, concertsNew.size)
    }
}