# Pre-computed Ratings Approach

## Overview

Rather than fetching ratings at runtime, we'll create a pre-computed ratings database that ships with the app:

1. Create a script to fetch all ratings from Archive.org
2. Process and aggregate these ratings into an optimized JSON file
3. Bundle this file with the app
4. Load ratings data during app initialization

## Benefits

- Reduced network calls to Archive.org API
- Faster app performance
- Consistent rating data even with intermittent connectivity
- Lower bandwidth usage for users
- Reduced risk of API throttling
- Simplified caching mechanism

## Implementation

### 1. JSON Schema Design

```json
{
  "version": "1.0",
  "lastUpdated": "2025-07-08",
  "recordings": {
    "gd1970-02-13.sbd.miller.89490.sbeok.flac16": {
      "averageRating": 4.8,
      "numberOfReviews": 42
    },
    "gd1972-08-27.sbd.hollister.2199.sbeok.shnf": {
      "averageRating": 4.9,
      "numberOfReviews": 103
    }
    // Additional recordings...
  },
  "shows": {
    "1970-02-13": {
      "averageRating": 4.8,
      "numberOfReviews": 142,
      "bestRecordingId": "gd1970-02-13.sbd.miller.89490.sbeok.flac16"
    },
    "1972-08-27": {
      "averageRating": 4.9,
      "numberOfReviews": 204,
      "bestRecordingId": "gd1972-08-27.sbd.hollister.2199.sbeok.shnf"
    }
    // Additional shows...
  },
  "topRated": {
    "shows": ["1977-05-08", "1972-08-27", "1973-11-17"],
    "recordings": [
      "gd1977-05-08.sbd.betty.83516.sbeok.flac16",
      "gd1972-08-27.sbd.hollister.2199.sbeok.shnf"
    ]
  }
}
```

### 2. Python Script

Create a Python script to:
1. Query the Archive.org API for all Grateful Dead recordings
2. Extract rating data for each recording
3. Aggregate ratings by show date using our weighted algorithm
4. Generate the optimized JSON file
5. Include metadata like version and last updated timestamp

```python
#!/usr/bin/env python3

import requests
import json
import time
from datetime import datetime
from collections import defaultdict

API_BASE = "https://archive.org"
SEARCH_ENDPOINT = f"{API_BASE}/advancedsearch.php"
METADATA_ENDPOINT = f"{API_BASE}/metadata"

def fetch_all_gd_recordings():
    """Fetch all Grateful Dead recordings from Archive.org"""
    all_recordings = []
    page = 1
    rows_per_page = 500
    total_found = float('inf')
    
    while len(all_recordings) < total_found:
        params = {
            'q': 'collection:GratefulDead',
            'fl': 'identifier,title,date,venue,coverage,source,avg_rating,num_reviews',
            'rows': rows_per_page,
            'start': (page - 1) * rows_per_page,
            'output': 'json'
        }
        
        response = requests.get(SEARCH_ENDPOINT, params=params)
        
        if response.status_code == 200:
            data = response.json()
            
            if page == 1:
                total_found = data['response']['numFound']
                print(f"Found {total_found} recordings")
                
            records = data['response']['docs']
            all_recordings.extend(records)
            print(f"Fetched page {page}, {len(all_recordings)}/{total_found} recordings")
            
            page += 1
            time.sleep(1)  # Be nice to the API
        else:
            print(f"Error fetching page {page}: {response.status_code}")
            break
    
    return all_recordings

def determine_recording_quality(source):
    """Extract recording type (SBD, AUD, etc.) from source field"""
    if not source:
        return "UNKNOWN"
        
    source = source.upper()
    
    if "SBD" in source or "SOUNDBOARD" in source:
        return "SBD"
    elif "MATRIX" in source:
        return "MATRIX"
    elif "FM" in source or "RADIO" in source:
        return "FM"
    elif "AUD" in source or "AUDIENCE" in source:
        return "AUD"
    else:
        return "UNKNOWN"

def normalize_date(date_str):
    """Normalize date string to YYYY-MM-DD format"""
    if not date_str:
        return None
        
    # Handle common Archive.org date formats
    formats = ["%Y-%m-%d", "%Y/%m/%d", "%Y.%m.%d", "%Y %m %d"]
    for fmt in formats:
        try:
            dt = datetime.strptime(date_str, fmt)
            return dt.strftime("%Y-%m-%d")
        except ValueError:
            continue
            
    return None

def aggregate_show_ratings(recordings):
    """Aggregate recording ratings by show date using weighted algorithm"""
    show_ratings = defaultdict(lambda: {"recordings": [], "weighted_sum": 0, "total_reviews": 0})
    
    # Group recordings by show date
    for rec in recordings:
        date = normalize_date(rec.get('date'))
        if not date:
            continue
            
        if 'avg_rating' not in rec or 'num_reviews' not in rec:
            continue
            
        avg_rating = rec.get('avg_rating')
        num_reviews = rec.get('num_reviews')
        
        if not avg_rating or not num_reviews:
            continue
            
        quality = determine_recording_quality(rec.get('source', ''))
        
        show_ratings[date]["recordings"].append({
            "id": rec.get('identifier'),
            "avg_rating": float(avg_rating),
            "num_reviews": int(num_reviews),
            "quality": quality
        })
        
    # Calculate show-level ratings
    shows = {}
    for date, data in show_ratings.items():
        recordings = data["recordings"]
        
        if not recordings:
            continue
            
        # First try to use SBD recordings with good review count
        sbd_recs = [r for r in recordings if r["quality"] == "SBD" and r["num_reviews"] >= 3]
        if sbd_recs:
            best_rec = max(sbd_recs, key=lambda x: x["avg_rating"])
            shows[date] = {
                "averageRating": best_rec["avg_rating"],
                "numberOfReviews": sum(r["num_reviews"] for r in recordings),
                "bestRecordingId": best_rec["id"]
            }
            continue
            
        # Next try well-reviewed recordings
        well_reviewed = [r for r in recordings if r["num_reviews"] >= 5]
        if well_reviewed:
            best_rec = max(well_reviewed, key=lambda x: x["avg_rating"])
            shows[date] = {
                "averageRating": best_rec["avg_rating"],
                "numberOfReviews": sum(r["num_reviews"] for r in recordings),
                "bestRecordingId": best_rec["id"]
            }
            continue
            
        # Fall back to weighted average
        total_reviews = sum(r["num_reviews"] for r in recordings)
        weighted_sum = sum(r["avg_rating"] * r["num_reviews"] for r in recordings)
        
        if total_reviews > 0:
            best_rec = max(recordings, key=lambda x: x["num_reviews"])
            shows[date] = {
                "averageRating": weighted_sum / total_reviews,
                "numberOfReviews": total_reviews,
                "bestRecordingId": best_rec["id"]
            }
    
    return shows

def generate_recordings_dict(recordings):
    """Generate recordings dictionary for JSON output"""
    result = {}
    for rec in recordings:
        if 'identifier' not in rec or 'avg_rating' not in rec or 'num_reviews' not in rec:
            continue
            
        if not rec.get('avg_rating') or not rec.get('num_reviews'):
            continue
            
        result[rec['identifier']] = {
            "averageRating": float(rec['avg_rating']),
            "numberOfReviews": int(rec['num_reviews'])
        }
    
    return result

def generate_top_rated(shows, recordings_dict, limit=50):
    """Generate lists of top-rated shows and recordings"""
    top_shows = sorted(
        [(date, data) for date, data in shows.items()],
        key=lambda x: x[1]["averageRating"],
        reverse=True
    )[:limit]
    
    top_recordings = sorted(
        [(id, data) for id, data in recordings_dict.items()],
        key=lambda x: x[1]["averageRating"],
        reverse=True
    )[:limit]
    
    return {
        "shows": [date for date, _ in top_shows],
        "recordings": [id for id, _ in top_recordings]
    }

def main():
    print("Fetching Grateful Dead recordings from Archive.org...")
    recordings = fetch_all_gd_recordings()
    print(f"Successfully fetched {len(recordings)} recordings")
    
    print("Generating recordings dictionary...")
    recordings_dict = generate_recordings_dict(recordings)
    print(f"Generated data for {len(recordings_dict)} recordings")
    
    print("Aggregating show ratings...")
    shows = aggregate_show_ratings(recordings)
    print(f"Generated rating data for {len(shows)} shows")
    
    print("Generating top-rated lists...")
    top_rated = generate_top_rated(shows, recordings_dict)
    
    output = {
        "version": "1.0",
        "lastUpdated": datetime.now().strftime("%Y-%m-%d"),
        "recordings": recordings_dict,
        "shows": shows,
        "topRated": top_rated
    }
    
    with open("grateful_dead_ratings.json", "w") as f:
        json.dump(output, f, indent=2)
    
    print("Rating data saved to grateful_dead_ratings.json")
    print(f"File size: {round(os.path.getsize('grateful_dead_ratings.json') / (1024 * 1024), 2)} MB")

if __name__ == "__main__":
    main()
```

### 3. App Integration

1. Add the generated JSON to the assets directory
2. Create a RatingsManager class to load and provide access to ratings
3. Initialize the manager during app startup
4. Use the preloaded data instead of making API calls

```kotlin
// RatingsManager.kt
class RatingsManager @Inject constructor(
    private val context: Context
) {
    private var ratingsData: RatingsData? = null

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (ratingsData == null) {
            val json = context.assets.open("grateful_dead_ratings.json").bufferedReader().use { it.readText() }
            ratingsData = Json.decodeFromString<RatingsData>(json)
            Timber.d("Ratings data loaded: ${ratingsData?.recordings?.size} recordings, " +
                     "${ratingsData?.shows?.size} shows")
        }
    }
    
    fun getRecordingRating(identifier: String): Rating? {
        val data = ratingsData?.recordings?.get(identifier) ?: return null
        return Rating(data.averageRating, data.numberOfReviews)
    }
    
    fun getShowRating(date: String): Rating? {
        val data = ratingsData?.shows?.get(date) ?: return null
        return Rating(data.averageRating, data.numberOfReviews)
    }
    
    fun getTopRatedShows(limit: Int = 20): List<String> {
        return ratingsData?.topRated?.shows?.take(limit) ?: emptyList()
    }
    
    fun getTopRatedRecordings(limit: Int = 20): List<String> {
        return ratingsData?.topRated?.recordings?.take(limit) ?: emptyList()
    }
    
    @Serializable
    data class RatingsData(
        val version: String,
        val lastUpdated: String,
        val recordings: Map<String, RecordingRatingData>,
        val shows: Map<String, ShowRatingData>,
        val topRated: TopRatedData
    )
    
    @Serializable
    data class RecordingRatingData(
        val averageRating: Double,
        val numberOfReviews: Int
    )
    
    @Serializable
    data class ShowRatingData(
        val averageRating: Double,
        val numberOfReviews: Int,
        val bestRecordingId: String
    )
    
    @Serializable
    data class TopRatedData(
        val shows: List<String>,
        val recordings: List<String>
    )
}
```

### 4. Repository Layer Updates

Modify the Show and Recording repositories to use the RatingsManager:

```kotlin
// ShowRepository.kt
class ShowRepository @Inject constructor(
    private val archiveApiClient: ArchiveApiClient,
    private val showDao: ShowDao,
    private val ratingsManager: RatingsManager
) {
    // Existing methods...

    suspend fun getShows(): Flow<List<Show>> = flow {
        val shows = showDao.getShows().map { it.toShow() }
        
        // Enhance shows with ratings data
        val enhancedShows = shows.map { show ->
            val rating = ratingsManager.getShowRating(show.date)
            show.copy(aggregatedRating = rating)
        }
        
        emit(enhancedShows)
    }
    
    suspend fun getTopRatedShows(limit: Int = 20): Flow<List<Show>> = flow {
        val topDates = ratingsManager.getTopRatedShows(limit)
        val shows = showDao.getShowsByDates(topDates).map { it.toShow() }
        
        // Enhance with rating data
        val enhancedShows = shows.map { show ->
            val rating = ratingsManager.getShowRating(show.date)
            show.copy(aggregatedRating = rating)
        }
        
        emit(enhancedShows)
    }
}
```

### 5. Update Process

1. Schedule regular updates of the ratings data (e.g., quarterly)
2. Run the Python script to generate a fresh JSON file
3. Include the updated file in new app releases
4. Consider version checking to allow dynamic updates of ratings data

## Migration Path

1. Create and test the Python script locally
2. Generate an initial ratings JSON file
3. Implement the RatingsManager to load the data
4. Update repositories to use the pre-computed ratings
5. Update UI components to display ratings
6. Create a mechanism for updating ratings with new app versions

## Benefits Over Original Approach

1. **Performance**: No API calls for ratings data
2. **Reliability**: Works offline and is resistant to API changes
3. **Control**: We can curate and correct ratings if needed
4. **Size**: The JSON file would be a one-time download with the app
5. **Updates**: Can be synchronized with app releases

This approach maintains all the UI benefits of the original plan while significantly improving performance and reducing API dependencies.