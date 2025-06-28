# Internet Archive Streaming Fix

## Problem

The DeadArchive application is encountering HTTP 503 errors when attempting to stream audio content from Archive.org. Analysis of logs and testing reveals that the current URL format being used is incorrect or outdated.

Current problematic URL pattern:
```
https://archive.org/download/gd73-06-10.aud.weiner.gdadt.26267.sbeok.shnf/gd73-06-10d1t01.shn
```

## Solution

Archive.org provides a metadata API that can be used to retrieve direct streaming URLs for audio content. These URLs use a different domain structure and include proper server identifiers.

### Step 1: Fetch Metadata

Make a request to the Archive.org metadata API for the concert:

```
https://archive.org/metadata/{identifier}
```

Example:
```
https://archive.org/metadata/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644
```

### Step 2: Parse Response

The metadata response contains a `files` array with detailed information about each file, including:
- `name`: Filename
- `format`: File format (MP3, FLAC, etc.)
- `size`: File size in bytes
- `length`: Duration in seconds
- `source`: Source file
- `original`: Whether it's an original or derivative file

### Step 3: Construct Direct URLs

For each track, construct a direct URL using:
```
https://{server}.us.archive.org/{dir}/items/{identifier}/{filename}
```

Example:
```
https://ia801509.us.archive.org/1/items/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644/02%20Shakedown%20Street.mp3
```

### Implementation Guidelines

1. **Add Metadata Retrieval**:
   ```kotlin
   suspend fun getArchiveMetadata(identifier: String): ArchiveMetadata {
       val url = "https://archive.org/metadata/$identifier"
       // Use HTTP client to fetch and parse JSON
       // Return structured metadata
   }
   ```

2. **Get Track URLs**:
   ```kotlin
   fun getTrackUrl(metadata: ArchiveMetadata, trackName: String): String {
       val file = metadata.files.find { it.name.contains(trackName) && it.format == "MP3" }
       if (file != null) {
           // Construct direct URL
           return "https://${metadata.server}.us.archive.org/${metadata.dir}/items/${metadata.identifier}/${file.name}"
       }
       throw FileNotFoundException("Track not found in metadata")
   }
   ```

3. **Error Handling**:
   - Add appropriate error handling for API failures
   - Implement retry logic with exponential backoff
   - Add logging for failed requests

4. **Caching** (Optional):
   - Cache metadata responses to reduce API calls
   - Use OkHttp's built-in caching mechanisms

### Example Usage

```kotlin
// In MediaPlayerViewModel
suspend fun playArchiveTrack(concertId: String, trackTitle: String) {
    try {
        // Fetch metadata
        val metadata = archiveRepository.getArchiveMetadata(concertId)
        
        // Get direct URL for track
        val trackUrl = archiveRepository.getTrackUrl(metadata, trackTitle)
        
        // Play using MediaPlayer
        mediaPlayer.playTrack(
            url = trackUrl,
            title = trackTitle,
            artist = "Grateful Dead"
        )
    } catch (e: Exception) {
        // Handle errors
        _message.value = "Error loading track: ${e.message}"
    }
}
```

## Additional Recommendations

1. **URL Encoding**:
   - Ensure all filenames are properly URL encoded

2. **Format Selection**:
   - Prefer MP3 files for compatibility and bandwidth efficiency
   - Add option for high-quality FLAC streaming when available

3. **File Size Consideration**:
   - Consider file size when streaming over mobile networks
   - Add adaptive quality selection based on network conditions

4. **Retry Logic**:
   - Implement the retry with exponential backoff as suggested in the API guide
   - Example implementation provided in section 5.2.1 of archive_api_guide.md

5. **User Agent**:
   - Continue using the descriptive User Agent header: "DeadArchive/1.0 (Android; Grateful Dead Concert Archive App)"

## Testing

Test the new implementation with various concert identifiers:

1. `gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644` (confirmed working)
2. `gd1977-05-08.sbd.hicks.4982.sbeok.shnf` (recommended for testing)
3. `gd1973-06-10.sbd.miller.25971.sbeok.shnf` (also recommended)

## References

- Archive.org API Documentation: https://archive.org/services/docs/api/
- Archive.org Metadata API: https://archive.org/metadata/
- Grateful Dead Archive: https://archive.org/details/GratefulDead