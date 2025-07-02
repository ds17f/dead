package com.deadarchive.feature.browse

import com.deadarchive.core.model.Concert
import com.deadarchive.core.network.mapper.ArchiveMapper
import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test to debug concert grouping
 */
class SimpleConcertGroupingTest {

    @Test
    fun testBasicGrouping() {
        println("=== SIMPLE GROUPING TEST START ===")
        
        // Create test data that mimics the real 1995-07-09 issue
        val concerts = listOf(
            Concert(
                identifier = "test1",
                title = "Test Concert 1",
                date = "1995-07-09T00:00:00Z", // ISO format
                venue = "Soldier Field",
                source = "SBD"
            ),
            Concert(
                identifier = "test2",
                title = "Test Concert 2", 
                date = "1995-07-09T00:00:00Z", // Same date, same venue
                venue = "Soldier Field",
                source = "AUD"
            )
        )

        println("Input concerts: ${concerts.size}")
        concerts.forEach { concert ->
            println("  - ${concert.identifier}: date='${concert.date}', venue='${concert.venue}'")
        }

        // Test the grouping
        val grouped = ArchiveMapper.run { concerts.migrateToConcertNew() }
        
        println("Output concerts: ${grouped.size}")
        grouped.forEach { concert ->
            println("  - ${concert.concertId}: date='${concert.date}', venue='${concert.venue}', recordings=${concert.recordingCount}")
        }

        // This should pass if grouping works correctly
        assertEquals("Should group into 1 concert", 1, grouped.size)
        
        val concert = grouped.first()
        assertEquals("Should have 2 recordings", 2, concert.recordingCount)
        
        println("=== TEST PASSED ===")
    }
}