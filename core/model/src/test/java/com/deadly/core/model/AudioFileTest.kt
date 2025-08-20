package com.deadly.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioFileTest {

    @Test
    fun `displayFormat normalizes format names correctly`() {
        assertThat(AudioFile("test.flac", "flac").displayFormat).isEqualTo("FLAC")
        assertThat(AudioFile("test.mp3", "VBR MP3").displayFormat).isEqualTo("MP3")
        assertThat(AudioFile("test.mp3", "mp3").displayFormat).isEqualTo("MP3")
        assertThat(AudioFile("test.ogg", "ogg vorbis").displayFormat).isEqualTo("OGG")
        assertThat(AudioFile("test.wav", "wav").displayFormat).isEqualTo("WAV")
        assertThat(AudioFile("test.unknown", "custom").displayFormat).isEqualTo("CUSTOM")
    }

    @Test
    fun `displaySize formats file sizes correctly`() {
        assertThat(AudioFile("test", "mp3", sizeBytes = "512").displaySize).isEqualTo("512 B")
        assertThat(AudioFile("test", "mp3", sizeBytes = "1024").displaySize).isEqualTo("1.0 KB")
        assertThat(AudioFile("test", "mp3", sizeBytes = "1536").displaySize).isEqualTo("1.5 KB")
        assertThat(AudioFile("test", "mp3", sizeBytes = "1048576").displaySize).isEqualTo("1.0 MB")
        assertThat(AudioFile("test", "mp3", sizeBytes = "1572864").displaySize).isEqualTo("1.5 MB")
        assertThat(AudioFile("test", "mp3", sizeBytes = "1073741824").displaySize).isEqualTo("1.0 GB")
        assertThat(AudioFile("test", "mp3", sizeBytes = "2147483648").displaySize).isEqualTo("2.0 GB")
    }

    @Test
    fun `displaySize handles invalid or null sizes`() {
        assertThat(AudioFile("test", "mp3", sizeBytes = null).displaySize).isEqualTo("Unknown")
        assertThat(AudioFile("test", "mp3", sizeBytes = "invalid").displaySize).isEqualTo("Unknown")
        assertThat(AudioFile("test", "mp3", sizeBytes = "").displaySize).isEqualTo("Unknown")
    }

    @Test
    fun `displayBitrate formats bitrate correctly`() {
        assertThat(AudioFile("test", "mp3", bitrate = "320").displayBitrate).isEqualTo("320 kbps")
        assertThat(AudioFile("test", "mp3", bitrate = "128").displayBitrate).isEqualTo("128 kbps")
        assertThat(AudioFile("test", "mp3", bitrate = null).displayBitrate).isEqualTo("")
    }

    @Test
    fun `duration parses duration correctly`() {
        assertThat(AudioFile("test", "mp3", durationSeconds = "180.5").duration).isEqualTo(180L)
        assertThat(AudioFile("test", "mp3", durationSeconds = "245").duration).isEqualTo(245L)
        assertThat(AudioFile("test", "mp3", durationSeconds = null).duration).isEqualTo(0L)
        assertThat(AudioFile("test", "mp3", durationSeconds = "invalid").duration).isEqualTo(0L)
    }

    @Test
    fun `isLossless identifies FLAC files correctly`() {
        assertThat(AudioFile("test", "flac").isLossless).isTrue()
        assertThat(AudioFile("test", "FLAC").isLossless).isTrue()
        assertThat(AudioFile("test", "Flac").isLossless).isTrue()
        assertThat(AudioFile("test", "VBR MP3").isLossless).isFalse()
        assertThat(AudioFile("test", "mp3").isLossless).isFalse()
    }

    @Test
    fun `quality determines audio quality correctly`() {
        // Lossless
        assertThat(AudioFile("test", "flac").quality).isEqualTo(AudioQuality.LOSSLESS)
        
        // High quality MP3
        assertThat(AudioFile("test", "mp3", bitrate = "320").quality).isEqualTo(AudioQuality.HIGH)
        assertThat(AudioFile("test", "mp3", bitrate = "256").quality).isEqualTo(AudioQuality.MEDIUM)
        
        // Medium quality
        assertThat(AudioFile("test", "mp3", bitrate = "192").quality).isEqualTo(AudioQuality.MEDIUM)
        assertThat(AudioFile("test", "mp3", bitrate = "224").quality).isEqualTo(AudioQuality.MEDIUM)
        
        // Low quality
        assertThat(AudioFile("test", "mp3", bitrate = "128").quality).isEqualTo(AudioQuality.LOW)
        assertThat(AudioFile("test", "mp3", bitrate = "96").quality).isEqualTo(AudioQuality.LOW)
        assertThat(AudioFile("test", "mp3", bitrate = null).quality).isEqualTo(AudioQuality.LOW)
        assertThat(AudioFile("test", "mp3", bitrate = "invalid").quality).isEqualTo(AudioQuality.LOW)
    }

    @Test
    fun `audio file with all properties constructs correctly`() {
        val audioFile = AudioFile(
            filename = "gd1995-07-09d1t01.mp3",
            format = "VBR MP3",
            sizeBytes = "12345678",
            durationSeconds = "245.5",
            md5Hash = "abc123",
            sha1Hash = "def456",
            crc32Hash = "789ghi",
            bitrate = "160",
            sampleRate = "44100",
            downloadUrl = "https://example.com/file.mp3",
            isDownloaded = true,
            localPath = "/storage/file.mp3"
        )

        assertThat(audioFile.filename).isEqualTo("gd1995-07-09d1t01.mp3")
        assertThat(audioFile.format).isEqualTo("VBR MP3")
        assertThat(audioFile.displayFormat).isEqualTo("MP3")
        assertThat(audioFile.displaySize).isEqualTo("11.8 MB")
        assertThat(audioFile.displayBitrate).isEqualTo("160 kbps")
        assertThat(audioFile.duration).isEqualTo(245L)
        assertThat(audioFile.quality).isEqualTo(AudioQuality.LOW)
        assertThat(audioFile.isLossless).isFalse()
        assertThat(audioFile.isDownloaded).isTrue()
    }

    @Test
    fun `audio file with minimal properties constructs correctly`() {
        val audioFile = AudioFile(
            filename = "test.flac", 
            format = "flac"
        )

        assertThat(audioFile.filename).isEqualTo("test.flac")
        assertThat(audioFile.format).isEqualTo("flac")
        assertThat(audioFile.displayFormat).isEqualTo("FLAC")
        assertThat(audioFile.displaySize).isEqualTo("Unknown")
        assertThat(audioFile.displayBitrate).isEqualTo("")
        assertThat(audioFile.duration).isEqualTo(0L)
        assertThat(audioFile.quality).isEqualTo(AudioQuality.LOSSLESS)
        assertThat(audioFile.isLossless).isTrue()
        assertThat(audioFile.isDownloaded).isFalse()
        assertThat(audioFile.localPath).isNull()
    }
}