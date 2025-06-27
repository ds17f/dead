# Archive.org API Documentation for DeadArchive

This document provides details on the Archive.org API endpoints and response formats used by the DeadArchive application.

## Base URL

```
https://archive.org/
```

## API Reference Documentation

Official Archive.org API documentation is available at: https://archive.org/developers/index-apis.html

## Authentication

The API calls in this application don't require authentication for read-only access. All calls use a custom user agent:

```
User-Agent: DeadArchive/1.0 (Android; Grateful Dead Concert Archive App)
```

## Endpoints

### 1. Advanced Search API

**Endpoint:** `advancedsearch.php`

Used to search the Archive.org collection with various parameters.

#### Request Parameters

| Parameter | Type   | Description |
|-----------|--------|-------------|
| q         | string | Search query (e.g., "collection:GratefulDead") |
| fl        | string | Comma-separated list of fields to return |
| rows      | int    | Number of results to return (max 10000) |
| start     | int    | Starting index for pagination |
| sort      | string | Field and direction to sort by (e.g., "date desc") |
| output    | string | Output format (json, xml, csv) |

#### Common Field Names

| Field       | Description |
|-------------|-------------|
| identifier  | Unique ID for the item |
| title       | Title of the recording |
| date        | Performance date (ISO format) |
| venue       | Venue where the show was performed |
| coverage    | Geographic location |
| creator     | Creator/performer (typically "Grateful Dead") |
| year        | Year of performance |
| source      | Recording source information |
| taper       | Person who recorded the show |
| transferer  | Person who transferred the recording |
| lineage     | Recording equipment lineage |
| description | Description of the recording |
| setlist     | Concert setlist |
| uploader    | Username of uploader |
| addeddate   | Date added to Archive.org |
| publicdate  | Date made public |
| downloads   | Number of downloads |
| collection  | Collection identifier |
| mediatype   | Media type (typically "etree") |

#### Response Format

The API returns JSON with the following structure:

```json
{
  "responseHeader": {
    "status": 0,
    "QTime": 14,
    "params": {
      "query": "...",
      "fields": "...",
      "wt": "json",
      "sort": "...",
      "rows": 5,
      "start": 0
    }
  },
  "response": {
    "numFound": 17796,
    "start": 0,
    "docs": [
      {
        "identifier": "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
        "title": "Grateful Dead Live at Barton Hall, Cornell University on 1977-05-08",
        "date": "1977-05-08T00:00:00Z",
        "venue": "Barton Hall, Cornell University",
        "coverage": "Ithaca, NY",
        // Additional fields as requested in the 'fl' parameter
      }
      // Additional results
    ]
  }
}
```

#### Field Type Variations

Note: Fields can have different types depending on the specific item:
- String fields like `venue` and `creator` can sometimes be returned as arrays
- Some numeric fields are returned as strings (e.g., `size`, `length`)
- Date fields are returned in ISO format (e.g., `2025-06-26T19:16:31Z`)
- Fields like `description`, `setlist`, and `lineage` can be either strings or arrays of strings

### 2. Metadata API

**Endpoint:** `metadata/{identifier}`

Used to fetch detailed metadata for a specific item.

#### Path Parameters

| Parameter  | Type   | Description |
|------------|--------|-------------|
| identifier | string | The Archive.org item identifier |

#### Response Format

The API returns detailed metadata with the following structure:

```json
{
  "files": [
    {
      "name": "filename.mp3",
      "format": "MP3",
      "size": "12345678",
      "length": "123.45",
      "title": "Track Title",
      "track": "1",
      "artist": "Grateful Dead",
      "album": "Live at Venue",
      "bitrate": "256",
      "sample_rate": "44100",
      "md5": "abcdef1234567890",
      "crc32": "12345678",
      "sha1": "abcdef1234567890abcdef1234567890",
      "mtime": "1234567890"
    }
    // Additional files
  ],
  "metadata": {
    "identifier": "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
    "title": "Grateful Dead Live at Barton Hall, Cornell University on 1977-05-08",
    "date": "1977-05-08",
    "venue": ["Barton Hall, Cornell University"],
    "coverage": "Ithaca, NY",
    "creator": ["Grateful Dead"],
    "description": ["..."],
    "setlist": ["..."],
    "source": ["Soundboard"],
    "taper": ["Charlie Miller"],
    "transferer": ["Charlie Miller"],
    "lineage": ["..."],
    "notes": ["..."],
    "uploader": "user@example.com",
    "addeddate": "2005-03-26",
    "publicdate": "2005-03-26",
    "collection": ["GratefulDead", "etree"],
    "subject": ["Live concert", "Rock"],
    "licenseurl": "..."
  },
  "reviews": [
    {
      "title": "Amazing show!",
      "body": "...",
      "reviewer": "username",
      "reviewdate": "2020-01-15",
      "stars": 5
    }
    // Additional reviews
  ],
  "server": "ia123456.us.archive.org",
  "dir": "/15/items/gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
  "workable_servers": ["ia123456.us.archive.org", "ia234567.us.archive.org"]
}
```

### 3. Files API

**Endpoint:** `metadata/{identifier}/files`

Used to retrieve file listings for a specific item.

#### Path Parameters

| Parameter  | Type   | Description |
|------------|--------|-------------|
| identifier | string | The Archive.org item identifier |

#### Response Format

Similar to the metadata API but focused on the files array.

## Common Search Query Patterns

1. **Collection Query**: `collection:GratefulDead`
2. **Date Range**: `date:[1977-05-01 TO 1977-05-31]`
3. **Combined Query**: `collection:GratefulDead AND venue:"Fillmore West"`
4. **Sort by Downloads**: `sort=downloads desc`
5. **Sort by Date**: `sort=date desc`
6. **Fields Selection**: `fl=identifier,title,date,venue,coverage,source`

## API Quirks and Issues

1. **Inconsistent Field Types**: Fields like `description`, `setlist`, and `lineage` can be returned as either strings or arrays of strings, requiring defensive parsing.

2. **String vs. Number Types**: Some numerical fields (`size`, `length`, etc.) are returned as strings and need conversion.

3. **Missing Fields**: Not all items have all fields. Always handle null/missing fields gracefully.

4. **Response Size**: Large queries can return substantial amounts of data. Always use pagination with the `rows` and `start` parameters.

## Best Practices

1. **Use Specific Collections**: Always include `collection:GratefulDead` in search queries to limit results to relevant content.

2. **Pagination**: Use the `start` and `rows` parameters for pagination. Maximum rows per request is 10000.

3. **Field Selection**: Only request fields you need using the `fl` parameter to reduce response size.

4. **Error Handling**: Always check for HTTP errors and handle JSON parsing exceptions for when fields contain unexpected types.

5. **Rate Limiting**: Be mindful of request frequency to avoid being rate-limited.

## File Access

Audio and other files can be accessed directly using:

```
https://archive.org/download/{identifier}/{filename}
```

For example:
```
https://archive.org/download/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08d1t01.shn
```

## Examples

### Search for Recent Concerts

```
GET https://archive.org/advancedsearch.php?q=collection:GratefulDead&fl=identifier,title,date,venue,coverage,source,addeddate,uploader&rows=5&start=0&sort=addeddate+desc&output=json
```

### Search for Popular Concerts

```
GET https://archive.org/advancedsearch.php?q=collection:GratefulDead&fl=identifier,title,date,venue,coverage,source,downloads&rows=5&start=0&sort=downloads+desc&output=json
```

### General Search with Multiple Fields

```
GET https://archive.org/advancedsearch.php?q=Grateful+Dead+grateful+dead&fl=identifier,title,date,venue,coverage,creator,year,source,taper,transferer,lineage,description,setlist,uploader,addeddate,publicdate&rows=5&start=0&sort=date+desc&output=json
```

### Get Item Metadata

```
GET https://archive.org/metadata/gd1977-05-08.sbd.hicks.4982.sbeok.shnf
```

### Get Item Files

```
GET https://archive.org/metadata/gd1977-05-08.sbd.hicks.4982.sbeok.shnf/files
```