# CMU Setlist Scraper

This scraper implements the CMU setlist collection phase of the setlist implementation plan outlined in `docs/setlist-implementation-plan.md`.

## Overview

The CMU scraper collects setlist data from the CS.CMU.EDU Grateful Dead setlist archive covering 1972-1995. It follows the data collection architecture designed for the Dead Archive app.

## Features

- **Complete Archive Coverage**: Scrapes all available years (1972-1995)
- **Respectful Crawling**: Configurable delays and rate limiting
- **Robust Error Handling**: Retries, timeout handling, and detailed logging
- **Resume Support**: Progress tracking for interrupted sessions
- **Flexible Range Selection**: Scrape specific years or year ranges
- **Structured Output**: Consistent JSON format preserving original data structure

## Usage

### Basic Usage

```bash
# Scrape all years (1972-1995)
python scripts/scrape_cmu_setlists.py --output scripts/metadata/setlists/cmu_setlists.json

# Scrape with custom delay (slower, more respectful)
python scripts/scrape_cmu_setlists.py --output cmu_setlists.json --delay 1.0

# Scrape specific year range
python scripts/scrape_cmu_setlists.py --year-range 1977-1980 --output cmu_subset.json

# Scrape single year
python scripts/scrape_cmu_setlists.py --year-range 1977 --output cmu_1977.json

# Verbose logging
python scripts/scrape_cmu_setlists.py --output cmu_setlists.json --verbose
```

### Command Line Arguments

- `--output`, `-o`: Output JSON file path (required)
- `--delay`, `-d`: Delay between requests in seconds (default: 0.5)
- `--year-range`, `-y`: Year range to scrape (e.g., "1977-1980" or "1977")
- `--verbose`, `-v`: Enable verbose logging

## Output Format

The scraper outputs a structured JSON file with the following format:

```json
{
  "metadata": {
    "source": "cs.cmu.edu/~mleone/gdead/setlists.html",
    "scraped_at": "2025-07-09T05:56:14.123456",
    "duration_seconds": 120.5,
    "total_shows": 245,
    "year_range": "1972-1995",
    "scraper_version": "1.0.0"
  },
  "progress": {
    "years_completed": [1972, 1973, 1974],
    "current_year": null,
    "shows_scraped": 245,
    "total_shows": 245,
    "start_time": "2025-07-09T05:54:14.123456",
    "errors": []
  },
  "setlists": {
    "1977-05-08": {
      "show_id": "1977-05-08",
      "venue_line": "Barton Hall, Cornell University, Ithaca, NY 5/8/77",
      "sets": {
        "set1": [
          "Minglewood Blues",
          "Loser",
          "El Paso",
          "They Love Each Other",
          "Jack Straw",
          "Deal"
        ],
        "set2": [
          "Samson and Delilah",
          "Terrapin Station",
          "Playing in the Band",
          "Drums"
        ],
        "encore": [
          "One More Saturday Night"
        ]
      },
      "raw_content": "Barton Hall, Cornell University, Ithaca, NY 5/8/77\\n\\nSet 1:\\n...",
      "source": "cs.cmu.edu",
      "scraped_at": "2025-07-09T05:56:14.123456"
    }
  }
}
```

## Data Structure

### Setlist Entry Fields

- `show_id`: Date-based identifier (YYYY-MM-DD format)
- `venue_line`: Raw venue information from first line of setlist
- `sets`: Dictionary of set names to song lists
  - Common set names: `set1`, `set2`, `set3`, `encore`
  - Songs preserved as listed in original source
- `raw_content`: Complete original text content
- `source`: Source identifier ("cs.cmu.edu")
- `scraped_at`: ISO timestamp of when this setlist was scraped

### Set Detection

The parser automatically identifies sets using these patterns:

- **Set 1**: "set 1", "first set", "1st set"
- **Set 2**: "set 2", "second set", "2nd set"  
- **Set 3**: "set 3", "third set", "3rd set"
- **Encore**: "encore"
- **Fallback**: Auto-numbered sets if no headers found

## Error Handling

The scraper includes robust error handling:

- **Network Errors**: Automatic retries with exponential backoff
- **Parsing Errors**: Logged but don't stop overall process
- **Missing Pages**: Recorded in error log for later review
- **Timeouts**: Configurable request timeouts

All errors are logged to both `cmu_scraper.log` and the output JSON's `progress.errors` array.

## Integration with Implementation Plan

This scraper implements **Stage 1.1** of the setlist implementation plan:

### Input
- CS.CMU.EDU setlist archive (https://www.cs.cmu.edu/~mleone/gdead/setlists.html)

### Output  
- `scripts/metadata/setlists/cmu_setlists.json`

### Next Steps
The output from this scraper feeds into:

1. **Stage 1.2**: `scripts/scrape_gdsets.py` (for 1965-1971 coverage)
2. **Stage 1.3**: `scripts/merge_setlists.py` (combine CMU + GDSets data)
3. **Stage 2**: Data enrichment and processing scripts

## Testing

The scraper includes a comprehensive test suite that validates parsing logic without network dependencies:

```bash
python scripts/test_cmu_parser.py
```

Tests cover:
- Setlist parsing with various formats
- Year link extraction from HTML
- Show link extraction from year pages
- Error handling edge cases

## Requirements

- Python 3.7+
- requests >= 2.31.0
- Standard library modules: json, re, time, datetime, pathlib, logging

Install requirements:
```bash
pip install -r scripts/requirements.txt
```

## Architecture Notes

### Design Principles

1. **Preserve Original Data**: Minimal processing, maintain raw content
2. **Respectful Crawling**: Rate limiting and polite request patterns
3. **Resumable Operations**: Progress tracking for large collections
4. **Error Resilience**: Continue processing despite individual failures
5. **Structured Output**: Consistent format for downstream processing

### Performance Considerations

- Default 0.5s delay between requests
- Exponential backoff on failures
- Session reuse for connection pooling
- Memory-efficient streaming approach

### Data Integrity

- Raw content preserved for verification
- Source attribution for all data
- Timestamps for data freshness tracking
- Comprehensive error logging

## Troubleshooting

### Common Issues

1. **Network Timeouts**: Increase `--delay` parameter
2. **Connection Refused**: Check if CMU server is accessible
3. **Empty Results**: Verify year range and CMU site structure
4. **Memory Issues**: Process smaller year ranges if needed

### Debugging

Enable verbose logging to see detailed progress:
```bash
python scripts/scrape_cmu_setlists.py --output test.json --verbose
```

Check the log file `cmu_scraper.log` for detailed error information.

## Future Enhancements

- Resume capability from partially completed runs
- Parallel processing for faster collection
- Data validation against known show databases
- Automatic retry scheduling for failed shows
- Integration with show rating correlation