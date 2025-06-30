package com.deadarchive.core.network.mapper

import com.deadarchive.core.network.mapper.ArchiveMapper.toConcert
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArchiveMapperTest {

    @Test
    fun `maps ArchiveMetadataResponse to Concert with complete metadata`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "gd1995-07-09d1t01.mp3",
                    format = "VBR MP3",
                    size = "12345678",
                    length = "245.5",
                    title = "Touch of Grey",
                    bitrate = "160"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "gd1995-07-09.sbd.miller.114369.flac16",
                title = "Grateful Dead Live at Soldier Field on 1995-07-09",
                date = "1995-07-09",
                venue = "Soldier Field",
                coverage = "Chicago, IL",
                creator = "Grateful Dead",
                source = "SBD > DAT > CD > EAC > FLAC",
                taper = "Miller",
                description = "Great show from the final tour",
                uploader = "deadhead123"
            ),
            server = "ia800207.us.archive.org",
            directory = "/26/items/gd1995-07-09.sbd.miller.114369.flac16"
        )

        val concert = metadataResponse.toConcert()

        assertThat(concert.identifier).isEqualTo("gd1995-07-09.sbd.miller.114369.flac16")
        assertThat(concert.title).isEqualTo("Grateful Dead Live at Soldier Field on 1995-07-09")
        assertThat(concert.date).isEqualTo("1995-07-09")
        assertThat(concert.venue).isEqualTo("Soldier Field")
        assertThat(concert.location).isEqualTo("Chicago, IL")
        assertThat(concert.year).isEqualTo("1995")
        assertThat(concert.source).isEqualTo("SBD > DAT > CD > EAC > FLAC")
        assertThat(concert.taper).isEqualTo("Miller")
        assertThat(concert.description).isEqualTo("Great show from the final tour")
        assertThat(concert.uploader).isEqualTo("deadhead123")
        assertThat(concert.tracks).hasSize(1)
        assertThat(concert.audioFiles).hasSize(1)
    }

    @Test
    fun `maps ArchiveMetadataResponse with null metadata`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = null,
            server = "ia800207.us.archive.org",
            directory = "/26/items/unknown"
        )

        val concert = metadataResponse.toConcert()

        assertThat(concert.identifier).isEmpty()
        assertThat(concert.title).isEmpty()
        assertThat(concert.date).isEmpty()
        assertThat(concert.venue).isNull()
        assertThat(concert.location).isNull()
        assertThat(concert.year).isNull()
        assertThat(concert.tracks).isEmpty()
        assertThat(concert.audioFiles).isEmpty()
    }

    @Test
    fun `extracts year from date correctly`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test",
                title = "Test",
                date = "1977-05-08"
            )
        )

        val concert = metadataResponse.toConcert()
        assertThat(concert.year).isEqualTo("1977")
    }

    @Test
    fun `handles invalid date format gracefully`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test",
                title = "Test",
                date = "invalid-date"
            )
        )

        val concert = metadataResponse.toConcert()
        assertThat(concert.year).isNull()
    }

    @Test
    fun `filters audio files correctly`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(name = "track01.mp3", format = "VBR MP3"),
                ArchiveMetadataResponse.ArchiveFile(name = "track02.flac", format = "Flac"),
                ArchiveMetadataResponse.ArchiveFile(name = "info.txt", format = "Text"),
                ArchiveMetadataResponse.ArchiveFile(name = "cover.jpg", format = "JPEG"),
                ArchiveMetadataResponse.ArchiveFile(name = "track03.ogg", format = "Ogg Vorbis")
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test",
                title = "Test"
            )
        )

        val concert = metadataResponse.toConcert()
        
        assertThat(concert.audioFiles).hasSize(3)
        assertThat(concert.tracks).hasSize(3)
        
        val audioFileNames = concert.audioFiles.map { it.filename }
        assertThat(audioFileNames).containsExactly("track01.mp3", "track02.flac", "track03.ogg")
    }

    @Test
    fun `creates tracks with correct track numbers`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(name = "track01.mp3", format = "VBR MP3", title = "Touch of Grey"),
                ArchiveMetadataResponse.ArchiveFile(name = "track02.mp3", format = "VBR MP3", title = "Fire on the Mountain")
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            )
        )

        val concert = metadataResponse.toConcert()
        
        assertThat(concert.tracks).hasSize(2)
        assertThat(concert.tracks[0].trackNumber).isEqualTo("1")
        assertThat(concert.tracks[0].title).isEqualTo("Touch of Grey")
        assertThat(concert.tracks[1].trackNumber).isEqualTo("2")
        assertThat(concert.tracks[1].title).isEqualTo("Fire on the Mountain")
    }

    @Test
    fun `generates correct download URLs for audio files`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "gd1977-05-08d1t01.mp3",
                    format = "VBR MP3",
                    title = "Promised Land"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "gd1977-05-08d1t02 with spaces.flac",
                    format = "Flac",
                    title = "Fire on the Mountain"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                title = "Grateful Dead Live at Barton Hall on 1977-05-08"
            ),
            server = "ia800207.us.archive.org",
            directory = "/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf"
        )

        val concert = metadataResponse.toConcert()
        
        // Verify tracks have audio files with download URLs
        assertThat(concert.tracks).hasSize(2)
        
        val track1 = concert.tracks[0]
        assertThat(track1.audioFile).isNotNull()
        assertThat(track1.audioFile!!.downloadUrl)
            .isEqualTo("https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t01.mp3")
        
        val track2 = concert.tracks[1]
        assertThat(track2.audioFile).isNotNull()
        assertThat(track2.audioFile!!.downloadUrl)
            .isEqualTo("https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t02%20with%20spaces.flac")
        
        // Also verify AudioFile objects have correct URLs
        assertThat(concert.audioFiles).hasSize(2)
        assertThat(concert.audioFiles[0].downloadUrl)
            .isEqualTo("https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t01.mp3")
        assertThat(concert.audioFiles[1].downloadUrl)
            .isEqualTo("https://ia800207.us.archive.org/26/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd1977-05-08d1t02%20with%20spaces.flac")
    }

    @Test
    fun `handles missing server and directory information`() {
        val metadataResponse = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "test.mp3",
                    format = "VBR MP3"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = null,
            directory = null
        )

        val concert = metadataResponse.toConcert()
        
        // Should fallback to default server and directory
        assertThat(concert.tracks).hasSize(1)
        assertThat(concert.tracks[0].audioFile!!.downloadUrl)
            .isEqualTo("https://ia800000.us.archive.org/0/test.mp3")
    }
}