# Grateful Dead Setlist Data Pipeline Documentation

## Overview

This document provides comprehensive instructions for rebuilding the complete Grateful Dead setlist data processing pipeline. The pipeline transforms raw scraped data from multiple sources into normalized, integrated databases ready for use in the Dead Archive application.

## Architecture

The pipeline follows a 3-stage architecture:

1. **Raw Data Collection** - Scrape and collect raw data from external sources
2. **Data Processing** - Normalize and clean the raw data into structured databases  
3. **Integration** - Merge processed data with existing show metadata

## File Structure

```
scripts/metadata/
├── recordings/          # 17,790 Archive.org recording metadata files (foundational data)
├── shows/              # 3,252 individual show metadata files (foundational data)
├── sources/
│   └── gdsets/
│       └── index.html  # Original scraped GDSets HTML source
├── setlists/
│   ├── cmu_setlists.json    # Raw scraped CMU data (1972-1995)
│   ├── gdsets_setlists.json # Raw scraped GDSets data (1965-1971)
│   └── setlists.json        # Final integrated setlists
├── venues/
│   └── venues.json     # Final normalized venues database
├── songs/
│   └── songs.json      # Final normalized songs database
└── images/
    └── images.json     # Final processed image database
```

## Prerequisites

- Python 3.8+
- Required Python packages: `beautifulsoup4`, `requests`, `argparse`, `pathlib`
- Internet connection for initial data scraping
- Approximately 2GB disk space for all processed data

## Stage 1: Raw Data Collection

### Step 1.1: Scrape CMU Setlist Data (1972-1995)

**Purpose**: Collect comprehensive setlist data from CS.CMU.EDU archive  
**Source**: https://www.cs.cmu.edu/~mleone/gdead/setlists.html  
**Coverage**: Complete setlists from 1972-1995 (post-Pigpen era)

```bash
# Run CMU scraper
python scripts/scrape_cmu_setlists.py \
    --output scripts/metadata/setlists/cmu_setlists.json \
    --verbose

# Expected output: ~1,800+ setlists covering 1972-1995
# Processing time: ~15-30 minutes depending on network speed
```

**Output**: `scripts/metadata/setlists/cmu_setlists.json`
- Structured JSON with show dates, venues, and complete setlists
- Original text format preserved for reference
- Source attribution maintained

### Step 1.2: Scrape GDSets Data (1965-1971)

**Purpose**: Fill the pre-1972 gap with early years setlist data  
**Source**: https://gdsets.com/grateful-dead.htm  
**Coverage**: Early years (1965-1971) focusing on pre-Pigpen and transition era

```bash
# Run GDSets scraper for setlists
python scripts/scrape_gdsets.py \
    --html-file scripts/metadata/sources/gdsets/index.html \
    --focus-years 1965-1995 \
    --output-setlists scripts/metadata/setlists/gdsets_setlists.json \
    --verbose

# Expected output: ~270+ early years setlists
# Processing time: ~5-10 minutes
```

**Output**: `scripts/metadata/setlists/gdsets_setlists.json`
- Early years setlist data with adaptive parsing for less structured format
- Venue and date information extracted from HTML structure
- Jerry Garcia memorial hard stop (no shows after 7/9/95)

## Stage 2: Data Processing

### Step 2.1: Process Venue Data

**Purpose**: Create normalized venue database from raw setlist data

```bash
# Run venue processor
python scripts/process_venues.py \
    --input-cmu scripts/metadata/setlists/cmu_setlists.json \
    --input-gdsets scripts/metadata/setlists/gdsets_setlists.json \
    --output scripts/metadata/venues/venues.json \
    --verbose

# Expected output: ~480+ unique venues with normalizations
# Processing time: ~2-5 minutes
```

**Key Features**:
- International venue format handling (Denmark, Germany, France, etc.)
- Major US venue normalizations (Theater vs Theatre, Amphitheatre vs Amphitheater)
- City-based country mappings for international shows
- Venue alias tracking for consistent identification

### Step 2.2: Process Song Data

**Purpose**: Create normalized song database from raw setlist data

```bash
# Run song processor  
python scripts/process_songs.py \
    --input-cmu scripts/metadata/setlists/cmu_setlists.json \
    --input-gdsets scripts/metadata/setlists/gdsets_setlists.json \
    --output scripts/metadata/songs/songs.json \
    --verbose

# Expected output: ~550+ unique songs with aliases and statistics
# Processing time: ~2-5 minutes
```

**Key Features**:
- Commentary filtering (excludes entries like "Bruce Hornsby on piano the entire show!")
- Segue notation normalization (`->` vs `>`)
- Early years song variations from GDSets data
- Song frequency and performance statistics
- Common misspelling and variation handling

## Stage 3: Integration

### Step 3.1: Integrate Setlists with Show Data

**Purpose**: Merge processed setlist data with existing show metadata using GDSets-first precedence

```bash
# Run integration
python scripts/integrate_setlists.py \
    --setlists-cmu scripts/metadata/setlists/cmu_setlists.json \
    --setlists-gdsets scripts/metadata/setlists/gdsets_setlists.json \
    --songs scripts/metadata/songs/songs.json \
    --venues scripts/metadata/venues/venues.json \
    --shows scripts/metadata/shows/ \
    --output scripts/metadata/setlists/setlists.json \
    --verbose

# Expected output: ~2,200+ integrated setlists with 99.995% song match rate
# Processing time: ~5-10 minutes
```

**Key Features**:
- **GDSets-first merge precedence** - GDSets data takes priority over CMU for overlapping shows
- Enhanced lookup logic with exact alias matching (case-sensitive and case-insensitive)
- Show ID matching using standardized YYYY-MM-DD format
- Comprehensive error logging and match rate reporting

## Data Quality Metrics

After successful completion, expect these match rates:

- **Venue Matching**: 100% (484 venues matched)
- **Song Matching**: 99.995% (~2 songs unmatched out of ~40,000 song instances)  
- **Show Coverage**: 1965-1995 (30 years of Grateful Dead history)
- **Total Setlists**: ~2,200+ shows processed

## Pipeline Execution Order

### Recommended: Makefile Commands (Preferred Method)

**Complete rebuild from scratch using Makefile:**

```bash
# 0. Foundation Data Collection (6-12 hours) - REQUIRED FIRST
make collect-metadata-full          # Archive.org metadata collection (17,790 recordings)

# 1. Data Collection (30-45 minutes total)
make collect-setlists-full          # Scrape CMU setlists (1972-1995)
make collect-gdsets-full            # Scrape GDSets setlists and images (1965-1995)

# 2. Data Processing (5-10 minutes total)
make merge-setlists                 # Merge CMU and GDSets data
make process-venues                 # Create normalized venues database
make process-songs                  # Create normalized songs database

# 3. Integration (5-10 minutes)
make integrate-setlists             # Final integration with venue/song IDs
```

**Total pipeline execution time**: ~7-13 hours (mostly Step 0)

### Alternative: Direct Python Commands

**For advanced users or custom requirements:**

```bash
# 0. Foundation Data Collection (6-12 hours) - REQUIRED FIRST
python scripts/scrape_archive_org.py --output-dir scripts/metadata/recordings/ --creator "Grateful Dead" --verbose
python scripts/process_shows.py --input-dir scripts/metadata/recordings/ --output-dir scripts/metadata/shows/ --verbose

# 1. Data Collection (30-45 minutes total)
python scripts/scrape_cmu_setlists.py --output scripts/metadata/setlists/cmu_setlists.json --verbose
python scripts/scrape_gdsets.py --html-file scripts/metadata/sources/gdsets/index.html --focus-years 1965-1995 --output-setlists scripts/metadata/setlists/gdsets_setlists.json --verbose

# 2. Data Processing (5-10 minutes total)  
python scripts/process_venues.py --input-cmu scripts/metadata/setlists/cmu_setlists.json --input-gdsets scripts/metadata/setlists/gdsets_setlists.json --output scripts/metadata/venues/venues.json --verbose
python scripts/process_songs.py --input-cmu scripts/metadata/setlists/cmu_setlists.json --input-gdsets scripts/metadata/setlists/gdsets_setlists.json --output scripts/metadata/songs/songs.json --verbose

# 3. Integration (5-10 minutes)
python scripts/integrate_setlists.py --setlists-cmu scripts/metadata/setlists/cmu_setlists.json --setlists-gdsets scripts/metadata/setlists/gdsets_setlists.json --songs scripts/metadata/songs/songs.json --venues scripts/metadata/venues/venues.json --shows scripts/metadata/shows/ --output scripts/metadata/setlists/setlists.json --verbose
```

### Individual Stage Commands (Makefile)

**For running individual stages:**

```bash
# Collection variants
make collect-setlists-year YEAR=1977    # Collect specific year
make collect-gdsets-early                # Early years only (1965-1971)
make collect-gdsets-images               # Images only

# Processing variants  
make merge-setlists-early                # Merge with early years data only
```

## Data Sources and Attribution

### Primary Sources

1. **CS.CMU.EDU Setlist Archive**
   - URL: https://www.cs.cmu.edu/~mleone/gdead/setlists.html
   - Coverage: 1972-1995 (post-Pigpen era)
   - Format: Structured text files with consistent format
   - Strengths: Complete, reliable, well-structured

2. **GDSets.com**
   - URL: https://gdsets.com/grateful-dead.htm  
   - Coverage: 1965-1971 (early years)
   - Format: HTML with embedded setlist data
   - Strengths: Only comprehensive source for early years

### Existing Foundation Data

- **recordings/** - 17,790 Archive.org recording metadata files (already collected)
- **shows/** - 3,252 individual show metadata files (already collected)

## File Dependencies

**Critical Files (DO NOT DELETE)**:
- All files in `recordings/` and `shows/` directories (foundation data)
- `sources/gdsets/index.html` (original scraped source)
- `setlists/cmu_setlists.json` and `setlists/gdsets_setlists.json` (raw scraped data)
- Final processed files: `venues.json`, `songs.json`, `setlists.json`, `images.json`

## Error Handling and Debugging

### Common Issues

1. **Network Timeouts**: CMU scraper may timeout on slow connections
   - **Solution**: Re-run scraper, it will resume from where it left off

2. **Missing Venue Matches**: Some international venues may not match
   - **Solution**: Check venue processor logs, add new country mappings if needed

3. **Song Lookup Failures**: Commentary or unusual formatting may cause mismatches  
   - **Solution**: Review song processor commentary patterns, add new filters

### Debug Logging

All scripts support `--verbose` flag for detailed logging:
- Processing progress and statistics
- Error details with specific show/song information  
- Match rate reporting and quality metrics

### Validation

After pipeline completion, verify:
- File sizes are reasonable (setlists.json should be several MB)
- Match rates are above 99% for songs and 100% for venues
- Date ranges cover 1965-1995
- No critical errors in log files

## Performance Considerations

- **Memory Usage**: Peak usage ~1-2GB during integration phase
- **Disk Space**: ~2GB for all processed data
- **Network**: Initial scraping requires stable internet connection
- **CPU**: Processing is single-threaded, benefits from faster single-core performance

## Future Maintenance

### Adding New Data Sources
1. Create new scraper following existing patterns
2. Add to venue/song processors to handle new format variations
3. Update integration script to include new source with appropriate precedence

### Updating Existing Data
- Re-run individual stages as needed
- CMU and GDSets scrapers can be run independently
- Processing stages can be re-run with updated source data

This pipeline provides a complete, reproducible system for maintaining the Grateful Dead setlist database with high data quality and comprehensive coverage from 1965-1995.