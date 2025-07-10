# Setlist Search Implementation Plan

## Overview

This document outlines the implementation plan for extracting setlist data from our comprehensive Grateful Dead metadata collection and enabling song-based search functionality in the Dead Archive app.

## Data Source Analysis

### Archive.org Metadata
From the Archive.org metadata in `scripts/metadata/recordings/`, each recording JSON contains:

1. **Track listings** - Individual song files with names
2. **Description fields** - Often contain setlist information
3. **Title fields** - Sometimes include set breakdowns
4. **File metadata** - Track titles, durations

### External Data Sources

1. **Primary Setlist Source: CS.CMU.EDU Setlist Archive**
   - URL: https://www.cs.cmu.edu/~mleone/gdead/setlists.html
   - Format: Structured text files with predictable format
   - Coverage: Complete setlists from 1972-1995
   - Structure:
     - First line: Venue, City, State, Date
     - Double newlines separate sets (Set 1, Set 2, Encore)
     - Simple and consistent format ideal for parsing

2. **Supplemental Early Years Source: GDSets.com**
   - URL: https://gdsets.com/grateful-dead.htm
   - Coverage: Pre-1972 setlists (particularly valuable for 1965-1971)
   - No public API (requires screen scraping)
   - Valuable visual media collection:
     - Show posters and advertisements
     - Tickets and backstage passes
     - Programs and periodicals
     - Historical documents
   - Essential for completing our dataset for early years
   - Will require additional parsing logic for potentially less structured data

3. **Additional Reference: Relisten API**
   - URL: https://api.relisten.net/
   - Can be used for cross-reference and validation
   - Song data endpoint: `https://api.relisten.net/api/v2/artists/grateful-dead/songs`
   - Helpful for song name normalization if needed

## Proposed Implementation Architecture

### 1. Data Collection and Processing Pipeline

#### Stage 1: Raw Data Collection

##### 1.1 CMU Setlist Scraper (`scripts/scrape_cmu_setlists.py`)

**Input**: CS.CMU.EDU setlist archive (https://www.cs.cmu.edu/~mleone/gdead/setlists.html)  
**Output**: `scripts/metadata/setlists/cmu_setlists.json`

**Process**:
```python
# Scrape all year links from the main setlist page (1972-1995)
# For each year, scrape all individual show links
# Parse each show text file to extract raw data:
#   - First line: venue, city, state, date information
#   - Set information based on double newline separators
#   - Raw song lists for each set
# Preserve the original data structure with minimal processing
# Store in structured JSON format with original date format
```

##### 1.2 GDSets Scraper (`scripts/scrape_gdsets.py`)

**Input**: GDSets.com Grateful Dead section (https://gdsets.com/grateful-dead.htm)  
**Output**: 
- `scripts/metadata/setlists/gdsets_setlists.json`
- `scripts/metadata/images/gdsets_images.json`

**Process**:
```python
# Scrape setlists with focus on early years (1965-1971) from GDSets.com
# Extract show dates, venues, and setlists from HTML
# Implement adaptive parsing for less structured early data
# Collect image URLs for ALL years, including:
#   - Show posters and advertisements
#   - Tickets and backstage passes
#   - Programs and other memorabilia
#   - Venue photos where available
# Implement comprehensive image scraping across all galleries and sections
# Organize images by show date, venue, and type
# Generate consistent image metadata (descriptions, types, sources)
# Store setlist and image data in separate JSON files
```

##### 1.3 Setlist Merger (`scripts/merge_setlists.py`)

**Input**: 
- `scripts/metadata/setlists/cmu_setlists.json`
- `scripts/metadata/setlists/gdsets_setlists.json`  
**Output**: `scripts/metadata/setlists/raw_setlists.json`

**Process**:
```python
# Combine setlists from both sources
# Use CMU as primary source for 1972-1995
# Use GDSets as primary source for 1965-1971
# Handle overlapping data with preference to CMU for structure
# Record source information for each setlist
# Output unified raw setlist dataset
```

#### Stage 2: Data Enrichment and Integration

##### 2.1. Venue and Location Processing (`scripts/process_venues.py`)

**Input**: `scripts/metadata/setlists/raw_setlists.json` and `scripts/metadata/shows/`  
**Output**: `scripts/metadata/venues/venues.json`

**Process**:
```python
# Extract and normalize venue, city, and state information from setlists
# Create a comprehensive venues database with unique venue IDs
# Generate mappings from raw venue names to normalized venue IDs
# Include geographical data where available
```

##### 2.2. Song Database Creation (`scripts/process_songs.py`)

**Input**: `scripts/metadata/setlists/raw_setlists.json`  
**Output**: `scripts/metadata/songs/songs.json`

**Process**:
```python
# Extract all songs from setlists
# Generate unique song IDs and normalize song names
# Track song frequency and performance statistics
# Identify song variations, aliases, and segues
# Build relationships between songs (common pairings, transitions)
```

##### 2.3. Setlist Integration (`scripts/integrate_setlists.py`)

**Input**: 
- `scripts/metadata/setlists/raw_setlists.json`
- `scripts/metadata/shows/` (existing show JSON files)
- `scripts/metadata/venues/venues.json`
- `scripts/metadata/songs/songs.json`  
**Output**: `scripts/metadata/setlists/setlists.json`

**Process**:
```python
# Normalize dates to YYYY-MM-DD format
# Match setlists with existing show data using date as key
# Use showId from existing show data as the foreign key
# Reference venue and song IDs rather than duplicating information
# Structure setlist data with:
#   - showId (matches existing show files)
#   - songIds for each set (references songs.json)
#   - venueId (references venues.json)
# Generate final setlists JSON without duplicating show/venue info
```

**Output Format**:
```json
// venues.json
{
  "venues": {
    "v001": {
      "name": "Barton Hall",
      "city": "Ithaca",
      "state": "NY",
      "country": "USA",
      "aliases": ["Barton Hall, Cornell University"]
    }
  }
}

// songs.json
{
  "songs": {
    "s001": {
      "name": "Jack Straw",
      "aliases": ["Jack-Straw"],
      "first_played": "1971-10-19",
      "last_played": "1995-06-21",
      "times_played": 453,
      "common_segues": ["s002", "s015"]
    }
  }
}

// images.json
{
  "images": {
    "img001": {
      "type": "poster",
      "url": "https://gdsets.com/posters/gd77-05-08_poster.jpg",
      "date": "1977-05-08",
      "venue": "v001",
      "description": "Cornell University show poster",
      "source": "gdsets.com",
      "source_url": "https://gdsets.com/grateful-dead.htm"
    },
    "img002": {
      "type": "ticket",
      "url": "https://gdsets.com/tickets/gd77-05-08_ticket.jpg",
      "date": "1977-05-08",
      "venue": "v001",
      "description": "Cornell University ticket stub",
      "source": "gdsets.com",
      "source_url": "https://gdsets.com/grateful-dead.htm"
    },
    "img003": {
      "type": "backstage",
      "url": "https://gdsets.com/passes/gd71-02-18_backstage.jpg",
      "date": "1971-02-18",
      "venue": "v042",
      "description": "Capitol Theater backstage pass",
      "source": "gdsets.com",
      "source_url": "https://gdsets.com/grateful-dead.htm"
    },
    "img004": {
      "type": "program",
      "url": "https://gdsets.com/programs/gd69-01-24_program.jpg",
      "date": "1969-01-24",
      "venue": "v078",
      "description": "Avalon Ballroom program",
      "source": "gdsets.com",
      "source_url": "https://gdsets.com/grateful-dead.htm"
    }
  }
}

// setlists.json
{
  "setlists": {
    "1977-05-08": {
      "showId": "1977-05-08",
      "venueId": "v001",
      "sets": {
        "set1": ["s001", "s034", "s022"],
        "set2": ["s015", "s008"],
        "encore": ["s045"]
      },
      "source": "cs.cmu.edu",
      "images": ["img001", "img002"]
    }
  }
}
```

### 2. Database Schema Extension

**New Tables**:
```sql
-- Show setlists
CREATE TABLE SetlistEntity (
    showId TEXT PRIMARY KEY,
    date TEXT NOT NULL,
    venue TEXT,
    set1 TEXT, -- JSON array of songs
    set2 TEXT,
    set3 TEXT,
    encore TEXT,
    confidence REAL,
    sourceRecordings TEXT -- JSON array of recording IDs used
)

-- Song search index
CREATE TABLE SongEntity (
    songId TEXT PRIMARY KEY,
    songName TEXT NOT NULL,
    normalizedName TEXT, -- For search matching
    aliases TEXT -- JSON array of alternate names
)

-- Many-to-many relationship
CREATE TABLE ShowSongEntity (
    showId TEXT,
    songId TEXT,
    setNumber INTEGER,
    position INTEGER,
    PRIMARY KEY (showId, songId, setNumber, position)
)
```

### 3. Data Loading Integration

**Update `DataSyncService`**:
- Load setlists during initial database build
- Similar to how we load ratings from `ratings.zip`
- Extract `setlists.zip` → parse JSON → populate database

**Repository Layer**:
```kotlin
class SetlistRepository {
    suspend fun getShowSetlist(showId: String): Setlist?
    suspend fun searchShowsBySong(songName: String): List<Show>
    suspend fun getSongHistory(songName: String): List<ShowPerformance>
}
```

### 4. Search Implementation

**Enhanced Search Functionality**:
```kotlin
// New search capabilities
searchRepository.searchShowsBySong("Fire on the Mountain")
searchRepository.getShowsWithSongCombination(listOf("Scarlet Begonias", "Fire on the Mountain"))
searchRepository.getSongStatistics("Dark Star") // frequency, last played, etc.
```

## Implementation Challenges & Solutions

### Challenge 1: Show ID Matching
**Solutions**:
- Use standardized date format (YYYY-MM-DD) as primary key
- Handle multiple shows on same date with venue disambiguation
- Create mapping between CMU dates and our existing show IDs
- Fallback to fuzzy date matching for edge cases

### Challenge 2: Song Name Normalization
**Solutions**:
- Build comprehensive song aliases database from the setlists
- Handle variations: "Sugar Magnolia" vs "Sugar Mag" vs "Sugaree"
- Account for segues: "Scarlet > Fire" vs separate tracks
- Develop consistent naming conventions for jams, transitions, and medleys

### Challenge 3: Venue Consistency
**Solutions**:
- Create venue normalization rules
- Standardize venue names and locations
- Handle venue name changes over time
- Geocode venues where possible for mapping features

### Challenge 4: Data Completeness
**Solutions**:
- Use GDSets.com to fill the 1965-1971 gap not covered by CMU data
- Create consistent parsing approach for less structured early data
- Extract and include show imagery from GDSets.com where available
- Compress final data (similar to ratings.zip approach) for efficient distribution
- Consider incremental loading strategy for data volume management

## Implementation Priority

1. **Phase 1**: Data Collection and Processing
   - Scrape and parse CMU setlist archive
   - Create normalized venues and songs databases
   - Integrate with existing show data

2. **Phase 2**: Database Integration
   - Extend app database schema
   - Load setlist data during sync process
   - Implement basic setlist display in show details

3. **Phase 3**: Search Features
   - Implement song search functionality
   - Add song statistics and analytics
   - Develop setlist browsing interfaces
   - Enable segue and song progression analysis

## File Structure
```
scripts/
├── scrape_cmu_setlists.py      # CMU setlist scraper (1972-1995)
├── scrape_gdsets.py           # GDSets scraper for early years (1965-1971)
├── merge_setlists.py          # Combine setlists from multiple sources
├── process_venues.py          # Venue normalization and processing
├── process_songs.py           # Song database creation
├── extract_images.py          # Image URL extraction and organization
├── integrate_setlists.py      # Setlist integration with shows

scripts/metadata/
├── venues/
│   └── venues.json            # Normalized venue database
├── songs/
│   └── songs.json             # Song database with statistics
├── setlists/
│   ├── cmu_setlists.json      # Raw scraped CMU setlist data
│   ├── gdsets_setlists.json   # Raw scraped GDSets setlist data
│   ├── raw_setlists.json      # Combined raw setlist data
│   └── setlists.json          # Processed setlists with IDs
├── images/
│   ├── gdsets_images.json     # Raw image URLs from GDSets
│   └── images.json            # Processed image database with IDs

app/src/main/assets/
├── setlists.zip               # Compressed setlist data
├── songs.zip                  # Compressed song database
├── venues.zip                 # Compressed venue data
└── images.zip                 # Compressed image metadata

core/database/
├── SetlistEntity.kt
├── SongEntity.kt
├── VenueEntity.kt
├── ShowSongEntity.kt
└── ImageEntity.kt

core/data/repository/
├── SetlistRepository.kt
├── SongRepository.kt
└── ImageRepository.kt
```

## Use Cases Enabled

This implementation would enable powerful search capabilities:

- **Song-based search**: "Show me every show that opened with 'Jack Straw'"
- **Segue analysis**: "Find shows with 'Dark Star' > 'Other One' segues"
- **Set structure**: "Shows where 'Fire on the Mountain' closed the second set"
- **Song statistics**: "How often did they play 'Ripple'?"
- **Historical analysis**: "Evolution of setlists over the years"

## Data Quality Considerations

- **Source reliability**: Prioritize soundboard recordings and audience recordings with good documentation
- **Community verification**: Allow users to suggest corrections
- **Confidence scoring**: Rate setlist accuracy based on source quality and cross-verification
- **Iterative improvement**: Start with high-confidence shows and expand coverage

## Technical Notes

- The metadata we've collected should contain most setlist information in track listings and descriptions
- Archive.org's etree collection often has detailed track information
- Some recordings may have incomplete or inaccurate setlists that need validation
- Consider leveraging existing Grateful Dead setlist databases for cross-reference