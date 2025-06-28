package com.deadarchive.core.data.mapper

import com.deadarchive.core.data.mapper.DataMappers.determineQuality
import com.deadarchive.core.data.mapper.DataMappers.formatDuration
import com.deadarchive.core.data.mapper.DataMappers.formatFileSize
import com.deadarchive.core.data.mapper.DataMappers.generateSafeFilename
import com.deadarchive.core.data.mapper.DataMappers.isAudioFile
import com.deadarchive.core.data.mapper.DataMappers.isValidConcertId
import com.deadarchive.core.data.mapper.DataMappers.parseDurationToSeconds
import com.deadarchive.core.data.mapper.DataMappers.toConcert
import com.deadarchive.core.data.mapper.DataMappers.toConcertEntities
import com.deadarchive.core.data.mapper.DataMappers.toConcertEntity
import com.deadarchive.core.data.mapper.DataMappers.toConcerts
import com.deadarchive.core.data.mapper.DataMappers.toDownloadEntity
import com.deadarchive.core.data.mapper.DataMappers.toDownloadState
import com.deadarchive.core.data.mapper.DataMappers.toFavoriteEntity
import com.deadarchive.core.data.mapper.DataMappers.toFavoriteItem
import com.deadarchive.core.data.mapper.DataMappers.toIntOrZero
import com.deadarchive.core.data.mapper.DataMappers.toLongOrZero
import com.deadarchive.core.model.*
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * End-to-end tests for DataMappers focusing on behavior and data integrity.
 * Tests what the mapping functions DO, not HOW they do it.
 */
class DataMappersTest {

    // ============ Network to Domain Mapping Tests ============

    @Test
    fun `can convert search result to concert`() {
        // Given - Archive search document
        val searchDoc = ArchiveSearchResponse.ArchiveDoc(
            identifier = "gd1977-05-08",
            title = "Cornell '77",
            date = "1977-05-08",
            venue = "Barton Hall",
            coverage = "Ithaca, NY",
            year = "1977",
            source = "Soundboard",
            taper = "Betty Cantor-Jackson",
            description = "Legendary show"
        )

        // When - convert to concert
        val concert = searchDoc.toConcert()

        // Then - verify essential concert data is preserved
        assertThat(concert.identifier).isEqualTo("gd1977-05-08")
        assertThat(concert.title).isEqualTo("Cornell '77")
        assertThat(concert.date).isEqualTo("1977-05-08")
        assertThat(concert.venue).isEqualTo("Barton Hall")
        assertThat(concert.location).isEqualTo("Ithaca, NY")
        assertThat(concert.year).isEqualTo("1977")
    }

    @Test
    fun `can convert metadata response to detailed concert`() {
        // Given - Complete metadata response
        val metadata = createTestMetadataResponse()

        // When - convert to concert
        val concert = metadata.toConcert()

        // Then - verify detailed concert data including tracks
        assertThat(concert.identifier).isEqualTo("gd1977-05-08")
        assertThat(concert.title).isEqualTo("Cornell '77")
        assertThat(concert.tracks).hasSize(2) // Two audio files
        assertThat(concert.audioFiles).hasSize(2)
        assertThat(concert.tracks[0].filename).isEqualTo("track1.flac")
        assertThat(concert.tracks[1].filename).isEqualTo("track2.mp3")
    }

    @Test
    fun `handles malformed network data gracefully`() {
        // Given - Incomplete search document
        val searchDoc = ArchiveSearchResponse.ArchiveDoc(
            identifier = "test-id",
            title = "Test Concert",
            date = null, // Missing date
            venue = null, // Missing venue
            coverage = null
        )

        // When - convert to concert
        val concert = searchDoc.toConcert()

        // Then - should handle nulls gracefully
        assertThat(concert.identifier).isEqualTo("test-id")
        assertThat(concert.title).isEqualTo("Test Concert")
        assertThat(concert.date).isEmpty() // Null becomes empty string
        assertThat(concert.venue).isNull()
        assertThat(concert.location).isNull()
    }

    // ============ Domain to Database Mapping Tests ============

    @Test
    fun `can convert concert to database entity`() {
        // Given - Concert domain model
        val concert = createTestConcert()

        // When - convert to entity
        val entity = concert.toConcertEntity(isFavorite = true)

        // Then - verify data is preserved for storage
        assertThat(entity.id).isEqualTo("gd1977-05-08")
        assertThat(entity.title).isEqualTo("Cornell '77")
        assertThat(entity.isFavorite).isTrue()
        assertThat(entity.cachedTimestamp).isGreaterThan(0L)
    }

    @Test
    fun `can convert favorite item to database entity`() {
        // Given - FavoriteItem domain model
        val favoriteItem = FavoriteItem.fromConcert(
            createTestConcert()
        ).copy(notes = "Amazing show!")

        // When - convert to entity
        val entity = favoriteItem.toFavoriteEntity()

        // Then - verify favorite data is preserved
        assertThat(entity.type).isEqualTo("CONCERT")
        assertThat(entity.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(entity.notes).isEqualTo("Amazing show!")
        assertThat(entity.addedTimestamp).isGreaterThan(0L)
    }

    @Test
    fun `can convert download state to database entity`() {
        // Given - DownloadState domain model
        val downloadState = DownloadState(
            concertIdentifier = "gd1977-05-08",
            trackFilename = "track1.flac",
            status = DownloadStatus.DOWNLOADING,
            progress = 0.5f,
            bytesDownloaded = 500L
        )

        // When - convert to entity
        val entity = downloadState.toDownloadEntity()

        // Then - verify download data is preserved
        assertThat(entity.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(entity.trackFilename).isEqualTo("track1.flac")
        assertThat(entity.status).isEqualTo("DOWNLOADING")
        assertThat(entity.progress).isEqualTo(0.5f)
        assertThat(entity.bytesDownloaded).isEqualTo(500L)
    }

    // ============ Batch Conversion Tests ============

    @Test
    fun `can convert list of search results to concerts`() {
        // Given - List of search documents
        val searchResults = listOf(
            ArchiveSearchResponse.ArchiveDoc(
                identifier = "gd1977-05-08",
                title = "Cornell '77",
                date = "1977-05-08"
            ),
            ArchiveSearchResponse.ArchiveDoc(
                identifier = "gd1969-08-16",
                title = "Woodstock",
                date = "1969-08-16"
            )
        )

        // When - convert to concerts
        val concerts = searchResults.toConcerts()

        // Then - should convert all valid entries
        assertThat(concerts).hasSize(2)
        assertThat(concerts[0].identifier).isEqualTo("gd1977-05-08")
        assertThat(concerts[1].identifier).isEqualTo("gd1969-08-16")
    }

    @Test
    fun `batch conversion handles malformed data gracefully`() {
        // Given - List with some malformed entries
        val mixedResults = listOf(
            ArchiveSearchResponse.ArchiveDoc(
                identifier = "valid-id",
                title = "Valid Concert",
                date = "1977-05-08"
            )
            // Could add malformed entries that throw exceptions
        )

        // When - convert to concerts (should not throw)
        val concerts = mixedResults.toConcerts()

        // Then - should return valid concerts only
        assertThat(concerts).hasSize(1)
        assertThat(concerts[0].identifier).isEqualTo("valid-id")
    }

    // ============ Utility Function Tests ============

    @Test
    fun `can identify audio files correctly`() {
        // Given - Various file types
        val flacFile = createTestArchiveFile("track.flac", "flac")
        val mp3File = createTestArchiveFile("track.mp3", "mp3")
        val textFile = createTestArchiveFile("info.txt", "text")

        // When & Then - verify audio file detection
        assertThat(flacFile.isAudioFile()).isTrue()
        assertThat(mp3File.isAudioFile()).isTrue()
        assertThat(textFile.isAudioFile()).isFalse()
    }

    @Test
    fun `can determine audio quality correctly`() {
        // Given - Files with different formats and bitrates
        val flacFile = createTestArchiveFile("track.flac", "flac")
        val highMp3 = createTestArchiveFile("track.mp3", "mp3", bitrate = "320")
        val lowMp3 = createTestArchiveFile("track.mp3", "mp3", bitrate = "128")

        // When & Then - verify quality determination
        assertThat(flacFile.determineQuality()).isEqualTo(AudioQuality.LOSSLESS)
        assertThat(highMp3.determineQuality()).isEqualTo(AudioQuality.HIGH)
        assertThat(lowMp3.determineQuality()).isEqualTo(AudioQuality.LOW)
    }

    @Test
    fun `can parse duration formats correctly`() {
        // When & Then - verify duration parsing
        assertThat(parseDurationToSeconds("4:32")).isEqualTo(272) // 4*60 + 32
        assertThat(parseDurationToSeconds("1:23:45")).isEqualTo(5025) // 1*3600 + 23*60 + 45
        assertThat(parseDurationToSeconds("123.5")).isEqualTo(123)
        assertThat(parseDurationToSeconds("invalid")).isEqualTo(0)
        assertThat(parseDurationToSeconds(null)).isEqualTo(0)
    }

    @Test
    fun `can format duration to readable string`() {
        // When & Then - verify duration formatting
        assertThat(formatDuration(272)).isEqualTo("4:32")
        assertThat(formatDuration(5025)).isEqualTo("1:23:45")
        assertThat(formatDuration(45)).isEqualTo("0:45")
        assertThat(formatDuration(0)).isEqualTo("0:00")
    }

    @Test
    fun `can format file sizes correctly`() {
        // When & Then - verify file size formatting
        assertThat(formatFileSize(1024)).isEqualTo("1.0 KB")
        assertThat(formatFileSize(1048576)).isEqualTo("1.0 MB")
        assertThat(formatFileSize(1073741824)).isEqualTo("1.0 GB")
        assertThat(formatFileSize(0)).isEqualTo("0 B")
    }

    @Test
    fun `can validate concert identifiers`() {
        // When & Then - verify ID validation
        assertThat(isValidConcertId("gd1977-05-08.sbd.hicks")).isTrue()
        assertThat(isValidConcertId("valid_id-123")).isTrue()
        assertThat(isValidConcertId("")).isFalse()
        assertThat(isValidConcertId(null)).isFalse()
        assertThat(isValidConcertId("ab")).isFalse() // Too short
    }

    @Test
    fun `can generate safe filenames`() {
        // When - generate safe filenames
        val safeFilename = generateSafeFilename("gd1977-05-08", "track with spaces.flac")

        // Then - should replace unsafe characters
        assertThat(safeFilename).isEqualTo("gd1977-05-08_track_with_spaces.flac")
        assertThat(safeFilename).doesNotContain(" ") // No spaces
    }

    @Test
    fun `handles null strings safely`() {
        // When & Then - verify null handling utilities
        assertThat("test".toLongOrZero()).isEqualTo(0L) // Invalid number
        assertThat("123".toLongOrZero()).isEqualTo(123L)
        assertThat("invalid".toIntOrZero()).isEqualTo(0)
        assertThat("42".toIntOrZero()).isEqualTo(42)
    }

    // ============ Roundtrip Conversion Tests ============

    @Test
    fun `concert survives roundtrip to database and back`() {
        // Given - Original concert
        val originalConcert = createTestConcert()

        // When - convert to entity and back
        val entity = originalConcert.toConcertEntity()
        val convertedConcert = entity.toConcert()

        // Then - essential data should be preserved
        assertThat(convertedConcert.identifier).isEqualTo(originalConcert.identifier)
        assertThat(convertedConcert.title).isEqualTo(originalConcert.title)
        assertThat(convertedConcert.date).isEqualTo(originalConcert.date)
        assertThat(convertedConcert.venue).isEqualTo(originalConcert.venue)
    }

    @Test
    fun `favorite item survives roundtrip to database and back`() {
        // Given - Original favorite
        val originalFavorite = FavoriteItem.fromConcert(
            createTestConcert()
        ).copy(notes = "Great show!")

        // When - convert to entity and back
        val entity = originalFavorite.toFavoriteEntity()
        val convertedFavorite = entity.toFavoriteItem()

        // Then - data should be preserved
        assertThat(convertedFavorite.type).isEqualTo(originalFavorite.type)
        assertThat(convertedFavorite.concertIdentifier).isEqualTo(originalFavorite.concertIdentifier)
        assertThat(convertedFavorite.notes).isEqualTo(originalFavorite.notes)
    }

    @Test
    fun `download state survives roundtrip to database and back`() {
        // Given - Original download state
        val originalDownload = DownloadState(
            concertIdentifier = "gd1977-05-08",
            trackFilename = "track1.flac",
            status = DownloadStatus.COMPLETED,
            progress = 1.0f,
            localPath = "/storage/music/track1.flac"
        )

        // When - convert to entity and back
        val entity = originalDownload.toDownloadEntity()
        val convertedDownload = entity.toDownloadState()

        // Then - state should be preserved
        assertThat(convertedDownload.concertIdentifier).isEqualTo(originalDownload.concertIdentifier)
        assertThat(convertedDownload.trackFilename).isEqualTo(originalDownload.trackFilename)
        assertThat(convertedDownload.status).isEqualTo(originalDownload.status)
        assertThat(convertedDownload.progress).isEqualTo(originalDownload.progress)
        assertThat(convertedDownload.localPath).isEqualTo(originalDownload.localPath)
    }

    // ============ Helper Methods ============

    private fun createTestConcert(): Concert {
        return Concert(
            identifier = "gd1977-05-08",
            title = "Cornell '77",
            date = "1977-05-08",
            venue = "Barton Hall",
            location = "Ithaca, NY",
            year = "1977",
            source = "Soundboard"
        )
    }

    private fun createTestMetadataResponse(): ArchiveMetadataResponse {
        return ArchiveMetadataResponse(
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "gd1977-05-08",
                title = "Cornell '77",
                date = "1977-05-08",
                venue = "Barton Hall",
                coverage = "Ithaca, NY"
            ),
            files = listOf(
                createTestArchiveFile("track1.flac", "flac"),
                createTestArchiveFile("track2.mp3", "mp3"),
                createTestArchiveFile("info.txt", "text") // Non-audio file
            )
        )
    }

    private fun createTestArchiveFile(
        name: String,
        format: String,
        bitrate: String? = null
    ): ArchiveMetadataResponse.ArchiveFile {
        return ArchiveMetadataResponse.ArchiveFile(
            name = name,
            format = format,
            size = "1000000",
            length = "240.5",
            bitrate = bitrate,
            title = name.substringBeforeLast('.')
        )
    }
}