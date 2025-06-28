package com.deadarchive.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConcertTest {

    @Test
    fun `displayTitle uses title when available`() {
        val concert = Concert(
            identifier = "test",
            title = "Grateful Dead Live at Soldier Field",
            date = "1995-07-09",
            venue = "Soldier Field"
        )

        assertThat(concert.displayTitle).isEqualTo("Grateful Dead Live at Soldier Field")
    }

    @Test
    fun `displayTitle falls back to venue and date when title is blank`() {
        val concert = Concert(
            identifier = "test",
            title = "",
            date = "1995-07-09",
            venue = "Soldier Field"
        )

        assertThat(concert.displayTitle).isEqualTo("Soldier Field - 1995-07-09")
    }

    @Test
    fun `displayTitle falls back to venue and date when title is whitespace`() {
        val concert = Concert(
            identifier = "test",
            title = "   ",
            date = "1995-07-09",
            venue = "Soldier Field"
        )

        assertThat(concert.displayTitle).isEqualTo("Soldier Field - 1995-07-09")
    }

    @Test
    fun `displayLocation returns location when available`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            location = "Chicago, IL"
        )

        assertThat(concert.displayLocation).isEqualTo("Chicago, IL")
    }

    @Test
    fun `displayLocation returns default when location is null`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            location = null
        )

        assertThat(concert.displayLocation).isEqualTo("Unknown Location")
    }

    @Test
    fun `displayVenue returns venue when available`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            venue = "RFK Stadium"
        )

        assertThat(concert.displayVenue).isEqualTo("RFK Stadium")
    }

    @Test
    fun `displayVenue returns default when venue is null`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            venue = null
        )

        assertThat(concert.displayVenue).isEqualTo("Unknown Venue")
    }

    @Test
    fun `displaySource returns source when available`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            source = "SBD > DAT > CD"
        )

        assertThat(concert.displaySource).isEqualTo("SBD > DAT > CD")
    }

    @Test
    fun `displaySource returns default when source is null`() {
        val concert = Concert(
            identifier = "test",
            title = "Test Concert",
            date = "1995-07-09",
            source = null
        )

        assertThat(concert.displaySource).isEqualTo("Unknown")
    }

    @Test
    fun `archiveUrl generates correct URL`() {
        val concert = Concert(
            identifier = "gd1995-07-09.sbd.miller.114369.flac16",
            title = "Test Concert",
            date = "1995-07-09"
        )

        assertThat(concert.archiveUrl).isEqualTo("https://archive.org/details/gd1995-07-09.sbd.miller.114369.flac16")
    }

    @Test
    fun `concert with all properties constructs correctly`() {
        val tracks = listOf(
            Track(
                filename = "track01.mp3",
                title = "Touch of Grey",
                trackNumber = "1",
                durationSeconds = "180",
                audioFile = AudioFile(
                    filename = "track01.mp3",
                    format = "VBR MP3"
                )
            )
        )

        val audioFiles = listOf(
            AudioFile(
                filename = "track01.mp3",
                format = "VBR MP3",
                sizeBytes = "1234567",
                durationSeconds = "180"
            )
        )

        val concert = Concert(
            identifier = "test-concert",
            title = "Test Concert",
            date = "1995-07-09",
            venue = "Soldier Field",
            location = "Chicago, IL",
            year = "1995",
            source = "SBD",
            taper = "Miller",
            transferer = "Bob",
            lineage = "SBD > DAT > CD",
            description = "Great show",
            setlistRaw = "Set 1: Touch of Grey",
            uploader = "deadhead123",
            addedDate = "2023-01-01",
            publicDate = "2023-01-01",
            tracks = tracks,
            audioFiles = audioFiles,
            isFavorite = true,
            isDownloaded = false
        )

        assertThat(concert.identifier).isEqualTo("test-concert")
        assertThat(concert.tracks).hasSize(1)
        assertThat(concert.audioFiles).hasSize(1)
        assertThat(concert.isFavorite).isTrue()
        assertThat(concert.isDownloaded).isFalse()
    }
}