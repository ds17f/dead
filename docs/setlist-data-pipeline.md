# Setlist Data Pipeline Documentation

This document describes the comprehensive data processing pipeline used to collect, process, and integrate Grateful Dead setlist data from multiple sources.

## Overview

The Dead Archive application includes a sophisticated 4-stage data processing pipeline that transforms raw setlist data from multiple sources into a clean, normalized dataset optimized for mobile consumption.

## Pipeline Architecture

### Stage 1: Raw Data Collection

#### Archive.org Metadata Collection
- **Script**: `scripts/generate_metadata.py`
- **Purpose**: Collect recording metadata, ratings, and reviews from Archive.org
- **Output**: `ratings.json` (2-5MB) and full metadata cache (500MB+)
- **Coverage**: 17,790+ recording metadata files, 3,252+ show metadata files
- **Features**: Rate-limited, resumable collection with progress tracking

#### CMU Setlist Collection
- **Script**: `scripts/scrape_cmu_setlists.py`
- **Source**: CS.CMU.EDU Grateful Dead setlist archive
- **Coverage**: Comprehensive setlist data from 1972-1995 (post-Pigpen era)
- **Output**: `cmu_setlists.json`
- **Features**: Respectful crawling with error handling

#### GDSets Data Extraction
- **Script**: `scripts/scrape_gdsets.py`
- **Source**: GDSets.com HTML files (preprocessed)
- **Coverage**: Early years setlist data 1965-1971 plus show images
- **Output**: `gdsets_setlists.json` and `gdsets_images.json`
- **Features**: Fills the pre-1972 gap with high-quality early setlist data

### Stage 2: Data Processing

#### Setlist Merging
- **Script**: `scripts/merge_setlists.py`
- **Purpose**: Combine CMU and GDSets setlist data with conflict resolution
- **Strategy**: GDSets data takes priority due to superior data quality
- **Output**: `raw_setlists.json` (unified dataset)
- **Quality Control**: Handles overlapping shows and normalizes set classifications

#### Venue Normalization
- **Script**: `scripts/process_venues.py`
- **Purpose**: Extract and standardize venue names and locations
- **Features**: 
  - International venue handling
  - US venue variations (Theater/Theatre)
  - Country and state mappings
  - Duplicate venue consolidation
- **Output**: `venues.json` with unique venue IDs
- **Quality**: 100% venue matching rate

#### Song Normalization
- **Script**: `scripts/process_songs.py`
- **Purpose**: Extract and standardize song names and relationships
- **Features**:
  - Commentary filtering
  - Segue notation handling ("Scarlet > Fire")
  - Song frequency statistics
  - Alias resolution
- **Output**: `songs.json` with unique song IDs
- **Quality**: 99.995% song match rate

### Stage 3: Integration

#### Final Integration
- **Script**: `scripts/integrate_setlists.py`
- **Purpose**: Create ID-referenced setlist data with full relational integrity
- **Inputs**: `raw_setlists.json`, `venues.json`, `songs.json`
- **Output**: `setlists.json` (clean, ID-referenced database)
- **Features**:
  - Show ID standardization (YYYY-MM-DD format)
  - Venue and song ID linking
  - Error logging for manual review
  - Graceful handling of missing data

### Stage 4: App Deployment

#### Data Packaging
- **Script**: `scripts/package_datazip.py`
- **Purpose**: Bundle processed metadata for Android app deployment
- **Inputs**: All processed JSON files from previous stages
- **Output**: `app/src/main/assets/data.zip`
- **Features**:
  - Optimized compression for mobile deployment
  - Package metadata and validation
  - File integrity checking

## Data Quality Metrics

### Coverage Statistics
- **Total Shows**: 2,200+ concerts (1965-1995)
- **Venue Database**: 484+ unique venues worldwide
- **Song Database**: 550+ unique songs with aliases and statistics
- **Rating Data**: Comprehensive reviews and ratings from Archive.org

### Quality Assurance
- **Song Match Rate**: 99.995% successful song identification
- **Venue Match Rate**: 100% venue identification and normalization
- **Data Completeness**: Comprehensive coverage of the Grateful Dead's performing years
- **Source Priority**: GDSets data preferred over CMU for overlapping periods

## Pipeline Execution

### Makefile Commands

#### Development/Testing
```bash
make collect-metadata-test        # Test collection (10 recordings)
make collect-metadata-1977       # Collect 1977 shows (golden year) 
make collect-metadata-1995       # Collect 1995 shows (final year)
```

#### Production Data Collection
```bash
make collect-metadata-full        # Complete metadata (2-3 hours)
make collect-setlists-full        # Complete CMU setlist collection
make collect-gdsets-full          # Complete GDSets extraction
```

#### Processing Pipeline
```bash
make merge-setlists              # Combine setlist sources
make process-venues              # Normalize venue data
make process-songs               # Normalize song data  
make integrate-setlists          # Create final integration
make package-datazip             # Bundle for app deployment
```

### Pipeline Dependencies

#### Python Requirements
```python
requests==2.31.0          # HTTP requests for API calls
lxml==4.9.3              # XML/HTML parsing
python-dateutil>=2.8.0   # Date parsing and manipulation
beautifulsoup4==4.12.2   # HTML parsing and scraping
```

#### System Requirements
- Python 3.8+
- Virtual environment recommended
- ~5GB free disk space for full pipeline
- Network connectivity for data collection

## Data Flow Diagram

```
Archive.org API ──┐
                  ├── Stage 1: Collection ──┐
CS.CMU.EDU   ────┤                          │
                  │                          │
GDSets.com  ─────┘                          │
                                             │
                  ┌── Stage 2: Processing ◄──┘
                  │
raw_setlists.json ├── process_venues.py  ── venues.json
                  │
                  ├── process_songs.py   ── songs.json
                  │
                  └── integrate_setlists.py ── setlists.json
                                                    │
ratings.json + setlists.json + venues.json + songs.json
                                                    │
                  ┌── Stage 3: Packaging ◄────────┘
                  │
                  └── data.zip ── Android App Assets
```

## Error Handling and Logging

### Collection Phase
- **Rate Limiting**: Respectful API usage with configurable delays
- **Resume Capability**: Interrupted collections can be resumed from progress files
- **Network Resilience**: Automatic retry with exponential backoff
- **Data Validation**: Format checking and content validation

### Processing Phase
- **Data Quality Checks**: Validation at each processing stage
- **Error Logging**: Comprehensive logging of processing issues
- **Fallback Strategies**: Graceful handling of malformed or missing data
- **Progress Tracking**: Detailed statistics and completion reporting

### Integration Phase
- **Referential Integrity**: Verification of all ID relationships
- **Missing Data Handling**: Graceful degradation for incomplete records
- **Quality Metrics**: Detailed reporting on match rates and coverage
- **Manual Review**: Flagging of unusual or problematic records

## Maintenance and Updates

### Regular Maintenance
- **Archive.org Updates**: Quarterly collection of new recordings and reviews
- **Venue Database**: Annual review and expansion of venue normalization rules
- **Song Database**: Ongoing refinement of song aliases and segue handling

### Pipeline Evolution
- **Performance Optimization**: Continuous improvement of processing speed
- **Data Quality Enhancement**: Refinement of normalization algorithms
- **Source Integration**: Addition of new setlist sources as they become available

This comprehensive data pipeline ensures that the Dead Archive Android application has access to the most complete, accurate, and well-structured dataset of Grateful Dead concert information available.