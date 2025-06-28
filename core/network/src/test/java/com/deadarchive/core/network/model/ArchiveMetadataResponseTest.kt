package com.deadarchive.core.network.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class ArchiveMetadataResponseTest {

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `deserializes complete Archive org metadata response`() {
        val jsonResponse = """
        {
            "files": [
                {
                    "name": "gd1995-07-09d1t01.mp3",
                    "format": "VBR MP3",
                    "size": "12345678",
                    "length": "245.5",
                    "title": "Touch of Grey",
                    "bitrate": "160"
                }
            ],
            "metadata": {
                "identifier": "gd1995-07-09.sbd.miller.114369.flac16",
                "title": "Grateful Dead Live at Soldier Field on 1995-07-09",
                "date": "1995-07-09",
                "venue": "Soldier Field",
                "coverage": "Chicago, IL",
                "creator": "Grateful Dead",
                "source": ["SBD", "DAT", "CD", "EAC", "FLAC"],
                "taper": "Miller",
                "description": "Great show from the final tour",
                "uploader": "deadhead123"
            },
            "server": "ia800207.us.archive.org",
            "dir": "/26/items/gd1995-07-09.sbd.miller.114369.flac16"
        }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveMetadataResponse>(jsonResponse)

        assertThat(response.files).hasSize(1)
        assertThat(response.files[0].name).isEqualTo("gd1995-07-09d1t01.mp3")
        assertThat(response.files[0].format).isEqualTo("VBR MP3")
        
        assertThat(response.metadata).isNotNull()
        assertThat(response.metadata!!.identifier).isEqualTo("gd1995-07-09.sbd.miller.114369.flac16")
        assertThat(response.metadata!!.venue).isEqualTo("Soldier Field")
        assertThat(response.metadata!!.creator).isEqualTo("Grateful Dead")
        assertThat(response.metadata!!.source).isEqualTo("SBD\nDAT\nCD\nEAC\nFLAC")
        
        assertThat(response.server).isEqualTo("ia800207.us.archive.org")
        assertThat(response.directory).isEqualTo("/26/items/gd1995-07-09.sbd.miller.114369.flac16")
    }

    @Test
    fun `handles empty metadata response`() {
        val jsonResponse = """{"files": [], "metadata": null}"""
        
        val response = json.decodeFromString<ArchiveMetadataResponse>(jsonResponse)
        
        assertThat(response.files).isEmpty()
        assertThat(response.metadata).isNull()
    }

    @Test
    fun `handles creator as string vs array`() {
        val stringCreatorJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test",
                "creator": "Grateful Dead"
            }
        }
        """.trimIndent()

        val arrayCreatorJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test",
                "creator": ["Grateful Dead", "Jerry Garcia"]
            }
        }
        """.trimIndent()

        val stringResponse = json.decodeFromString<ArchiveMetadataResponse>(stringCreatorJson)
        val arrayResponse = json.decodeFromString<ArchiveMetadataResponse>(arrayCreatorJson)

        assertThat(stringResponse.metadata!!.creator).isEqualTo("Grateful Dead")
        assertThat(arrayResponse.metadata!!.creator).isEqualTo("Grateful Dead") // Takes first element
    }

    @Test
    fun `handles venue as string vs array`() {
        val stringVenueJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test",
                "venue": "RFK Stadium"
            }
        }
        """.trimIndent()

        val arrayVenueJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test",
                "venue": ["RFK Stadium", "Washington DC"]
            }
        }
        """.trimIndent()

        val stringResponse = json.decodeFromString<ArchiveMetadataResponse>(stringVenueJson)
        val arrayResponse = json.decodeFromString<ArchiveMetadataResponse>(arrayVenueJson)

        assertThat(stringResponse.metadata!!.venue).isEqualTo("RFK Stadium")
        assertThat(arrayResponse.metadata!!.venue).isEqualTo("RFK Stadium")
    }

    @Test
    fun `handles description as string vs array`() {
        val arrayDescriptionJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test",
                "description": ["Line 1", "Line 2", "Line 3"]
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveMetadataResponse>(arrayDescriptionJson)
        
        assertThat(response.metadata!!.description).isEqualTo("Line 1\nLine 2\nLine 3")
    }

    @Test
    fun `handles missing optional fields gracefully`() {
        val minimalJson = """
        {
            "files": [],
            "metadata": {
                "identifier": "test",
                "title": "Test Concert"
            }
        }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveMetadataResponse>(minimalJson)
        
        assertThat(response.metadata!!.identifier).isEqualTo("test")
        assertThat(response.metadata!!.title).isEqualTo("Test Concert")
        assertThat(response.metadata!!.venue).isNull()
        assertThat(response.metadata!!.creator).isNull()
        assertThat(response.metadata!!.date).isNull()
    }

    @Test
    fun `handles real world Archive response with various field types`() {
        // This mimics actual responses from Archive.org that caused issues
        val realWorldJson = """
        {
            "files": [
                {
                    "name": "gd77-05-08aud-d1t09.mp3",
                    "format": "VBR MP3",
                    "size": "8901234"
                }
            ],
            "metadata": {
                "identifier": "gd1977-05-08.aud.clugston.23661.sbeok.flac16",
                "title": "Grateful Dead Live at Barton Hall on 1977-05-08",
                "date": "1977-05-08",
                "creator": ["Grateful Dead"],
                "venue": ["Barton Hall", "Cornell University"],
                "source": "AUD > CASS > DAT > CDR > EAC > SHN > FLAC",
                "taper": ["Paul Clugston"],
                "description": ["Legendary show", "Considered one of the best"],
                "collection": ["GratefulDead", "etree"]
            },
            "server": "ia800207.us.archive.org",
            "dir": "/1/items/gd1977-05-08.aud.clugston.23661.sbeok.flac16"
        }
        """.trimIndent()

        val response = json.decodeFromString<ArchiveMetadataResponse>(realWorldJson)
        
        assertThat(response.metadata!!.creator).isEqualTo("Grateful Dead")
        assertThat(response.metadata!!.venue).isEqualTo("Barton Hall")
        assertThat(response.metadata!!.taper).isEqualTo("Paul Clugston")
        assertThat(response.metadata!!.description).isEqualTo("Legendary show\nConsidered one of the best")
        assertThat(response.metadata!!.collection).isEqualTo("GratefulDead\netree")
    }
}