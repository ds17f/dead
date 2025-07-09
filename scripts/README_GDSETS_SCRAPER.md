# GDSets Scraper

This scraper implements the GDSets collection phase of the setlist implementation plan outlined in `docs/setlist-implementation-plan.md`.

## Overview

The GDSets scraper collects setlist and image data from GDSets.com with primary focus on early years (1965-1971) to complement the CMU archive data. It also captures comprehensive visual memorabilia across all years.

## Features

- **Early Years Focus**: Prioritizes 1965-1971 setlists to fill CMU coverage gaps
- **Comprehensive Image Collection**: Scrapes posters, tickets, backstage passes, programs, and memorabilia
- **Adaptive Parsing**: Handles less structured early setlist formats
- **Dual Output**: Separate JSON files for setlists and images
- **Respectful Crawling**: Configurable delays and rate limiting
- **Image Classification**: Automatic categorization of visual content

## Usage

### Basic Usage

```bash
# Scrape both setlists and images (default focus: 1965-1971)
python scripts/scrape_gdsets.py --output-setlists scripts/metadata/setlists/gdsets_setlists.json --output-images scripts/metadata/images/gdsets_images.json

# Focus on specific year range
python scripts/scrape_gdsets.py --focus-years 1965-1971 --output-setlists gdsets_early.json --output-images gdsets_images.json

# Images only (all years)
python scripts/scrape_gdsets.py --images-only --output-images gdsets_all_images.json

# Custom delay for respectful crawling
python scripts/scrape_gdsets.py --delay 1.0 --output-setlists gdsets.json --output-images images.json

# Verbose logging
python scripts/scrape_gdsets.py --verbose --output-setlists gdsets.json --output-images images.json
```

### Command Line Arguments

- `--output-setlists`, `-s`: Output JSON file for setlist data
- `--output-images`, `-i`: Output JSON file for image data  
- `--delay`, `-d`: Delay between requests in seconds (default: 0.5)
- `--focus-years`, `-f`: Year range to prioritize (default: "1965-1971")
- `--images-only`: Only scrape images, skip setlist parsing
- `--verbose`, `-v`: Enable verbose logging

## Output Formats

### Setlist Output Format

```json
{
  "metadata": {
    "source": "gdsets.com",
    "scraped_at": "2025-07-09T05:56:14.123456",
    "duration_seconds": 180.5,
    "total_setlists": 45,
    "total_images": 0,
    "focus_years": "1965-1971",
    "scraper_version": "1.0.0",
    "data_type": "setlists"
  },
  "progress": {
    "pages_scraped": 67,
    "setlists_found": 45,
    "images_found": 0,
    "start_time": "2025-07-09T05:53:14.123456",
    "errors": []
  },
  "setlists": {
    "1965-12-04": {
      "show_id": "1965-12-04",
      "venue_line": "The Matrix, San Francisco, CA",
      "sets": {
        "set1": [
          "Beat It On Down The Line",
          "Viola Lee Blues",
          "Stealin'"
        ]
      },
      "source_url": "https://gdsets.com/1965-12-04.htm",
      "source": "gdsets.com",
      "scraped_at": "2025-07-09T05:56:14.123456"
    }
  }
}
```

### Image Output Format

```json
{
  "metadata": {
    "source": "gdsets.com",
    "scraped_at": "2025-07-09T05:56:14.123456",
    "duration_seconds": 180.5,
    "total_setlists": 0,
    "total_images": 234,
    "focus_years": "1965-1971",
    "scraper_version": "1.0.0",
    "data_type": "images"
  },
  "images": {
    "1977-05-08_poster.jpg": {
      "url": "https://gdsets.com/images/1977-05-08-poster.jpg",
      "filename": "poster.jpg",
      "description": "Cornell University Barton Hall concert poster",
      "type": "poster",
      "show_id": "1977-05-08",
      "source_url": "https://gdsets.com/1977-05-08.htm",
      "scraped_at": "2025-07-09T05:56:14.123456"
    }
  }
}
```

## Data Structure

### Setlist Entry Fields

- `show_id`: Date-based identifier (YYYY-MM-DD format)
- `venue_line`: Extracted venue information
- `sets`: Dictionary of set names to song lists
- `source_url`: Original GDSets page URL
- `source`: Source identifier ("gdsets.com")
- `scraped_at`: ISO timestamp

### Image Entry Fields

- `url`: Full URL to the image
- `filename`: Image filename
- `description`: Alt text or caption
- `type`: Classified image type (see Image Types below)
- `show_id`: Associated show identifier (if applicable)
- `source_url`: Page where image was found
- `scraped_at`: ISO timestamp

### Image Types

The scraper automatically classifies images into these categories:

- **poster**: Show posters, flyers, handbills
- **ticket**: Ticket stubs and admission tickets
- **backstage_pass**: Backstage passes and credentials
- **program**: Concert programs and setlists
- **venue**: Venue photos and architecture
- **performance**: Band performance photos
- **memorabilia**: Other Dead-related items

## Parsing Strategy

### Setlist Parsing

The scraper uses adaptive parsing for various GDSets formats:

1. **Structured Sets**: Looks for "Set 1", "Set 2", "Encore" headers
2. **HTML Lists**: Parses `<ol>` and `<ul>` elements  
3. **Paragraph Format**: Extracts from `<p>` tags with line breaks
4. **Fallback**: Attempts to identify any song lists

### Date Parsing

Supports multiple date formats commonly found on GDSets:

- MM/DD/YYYY (e.g., "5/8/1977")
- MM/DD/YY (e.g., "5/8/77") 
- YYYY-MM-DD (e.g., "1977-05-08")
- MM-DD-YYYY (e.g., "5-8-1977")

## Integration with Implementation Plan

This scraper implements **Stage 1.2** of the setlist implementation plan:

### Input
- GDSets.com Grateful Dead section (https://gdsets.com/grateful-dead.htm)

### Output  
- `scripts/metadata/setlists/gdsets_setlists.json`
- `scripts/metadata/images/gdsets_images.json`

### Focus Areas
1. **Primary**: Early years setlists (1965-1971) for CMU gap coverage
2. **Secondary**: Visual memorabilia collection across all years

### Next Steps
The output feeds into:

1. **Stage 1.3**: `scripts/merge_setlists.py` (combine with CMU data)
2. **Stage 2**: Data enrichment and venue processing
3. **Image Integration**: Visual content for app enhancement

## Error Handling

Robust error handling includes:

- **Network Errors**: Automatic retries with exponential backoff
- **Parsing Failures**: Logged but don't halt overall process
- **Missing Content**: Graceful handling of empty or malformed pages
- **Duplicate Detection**: URL deduplication to avoid reprocessing

All errors logged to `gdsets_scraper.log` and stored in output JSON.

## Architecture Notes

### Design Principles

1. **Complementary Coverage**: Focus on years not well-covered by CMU
2. **Visual Preservation**: Comprehensive image collection for historical value
3. **Adaptive Parsing**: Handle varying GDSets page formats
4. **Source Attribution**: Maintain links to original GDSets pages
5. **Separate Concerns**: Split setlist and image data for modularity

### Performance Considerations

- Default 0.5s delay between requests
- Priority processing for focus years (1965-1971)
- Limit non-priority shows to prevent excessive crawling
- Session reuse for connection efficiency
- Duplicate URL detection

### Data Quality

- Adaptive parsing for inconsistent early formats
- Image classification for organized browsing
- Source URL preservation for verification
- Comprehensive error logging

## Requirements

- Python 3.7+
- requests >= 2.31.0
- Standard library modules: json, re, time, datetime, pathlib, logging

Install requirements:
```bash
pip install -r scripts/requirements.txt
```

## Troubleshooting

### Common Issues

1. **Connection Timeouts**: Increase `--delay` parameter
2. **Empty Results**: Check GDSets site accessibility and structure
3. **Parsing Failures**: Enable `--verbose` for detailed parsing logs
4. **Memory Usage**: Use `--images-only` for large collections

### Debugging

Enable verbose logging:
```bash
python scripts/scrape_gdsets.py --verbose --output-images test.json
```

Check `gdsets_scraper.log` for detailed error information.

## Future Enhancements

- Enhanced date parsing for variant formats
- Image download and local caching
- Cross-reference validation with CMU data
- Improved venue extraction algorithms
- Parallel processing for faster collection