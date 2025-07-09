#!/usr/bin/env python3
"""
Comprehensive Grateful Dead Metadata Collection Script

This script implements the database-first architecture for collecting ratings and metadata
from Archive.org. It creates both a full metadata cache for development and a simplified
ratings file for app distribution.

Architecture:
- Collects comprehensive metadata and caches locally (500MB)  
- Generates simplified ratings.json for app bundle (2-5MB)
- Supports resume, progress tracking, and incremental updates
- Rate-limited and respectful to Archive.org servers

Usage:
    # Full collection with metadata cache
    python scripts/generate_metadata.py --mode full --delay 0.25
    
    # Generate ratings from existing cache  
    python scripts/generate_metadata.py --mode ratings-only --cache scripts/metadata/
    
    # Resume interrupted collection
    python scripts/generate_metadata.py --mode resume --progress scripts/metadata/progress.json
"""

import json
import os
import re
import requests
import time
import zipfile
from datetime import datetime
from collections import defaultdict
from typing import Dict, List, Optional, Tuple, Any
import argparse
import logging
from dataclasses import dataclass, asdict
from pathlib import Path


@dataclass
class ReviewData:
    """Individual review data"""
    stars: float
    review_text: str
    date: str


@dataclass
class RecordingMetadata:
    """Complete metadata for a single recording"""
    identifier: str
    title: str
    date: str
    venue: str
    location: str
    source_type: str
    lineage: str
    taper: str
    description: str
    files: List[Dict[str, Any]]
    reviews: List[ReviewData]
    rating: float
    review_count: int
    confidence: float
    collection_timestamp: str


@dataclass
class ShowMetadata:
    """Aggregated metadata for an entire show"""
    show_key: str
    date: str
    venue: str
    location: str
    recordings: List[str]  # List of recording identifiers
    best_recording: str
    avg_rating: float
    confidence: float
    recording_count: int
    collection_timestamp: str


@dataclass
class ProgressState:
    """Collection progress tracking"""
    collection_started: str
    last_updated: str
    status: str
    total_recordings: int
    processed_recordings: int
    failed_recordings: int
    current_batch: int
    last_processed: str
    failed_identifiers: List[str]
    performance_stats: Dict[str, Any]


class GratefulDeadMetadataCollector:
    """
    Comprehensive metadata collector with database-first architecture.
    """
    
    def __init__(self, delay: float = 0.25, cache_dir: str = "scripts/metadata"):
        """Initialize the collector with configuration."""
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'DeadArchive-MetadataCollector/1.0 (Educational Use)'
        })
        
        # Performance configuration
        self.api_delay = delay
        self.last_api_call = 0
        self.batch_size = 100
        self.batch_delay = 0  # seconds between batches
        
        # Directories
        self.cache_dir = Path(cache_dir)
        self.recordings_dir = self.cache_dir / "recordings"
        self.shows_dir = self.cache_dir / "shows"
        
        # Create directories
        self.cache_dir.mkdir(exist_ok=True)
        self.recordings_dir.mkdir(exist_ok=True) 
        self.shows_dir.mkdir(exist_ok=True)
        
        # Rating configuration
        self.source_weights = {
            'SBD': 1.0,
            'MATRIX': 0.9,
            'AUD': 0.7,
            'FM': 0.8,
            'REMASTER': 1.0,
        }
        
        self.min_reviews_for_confidence = 3  # For confidence calculation only
        
        # Progress tracking
        self.progress_file = self.cache_dir / "progress.json"
        self.progress = None
        
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def rate_limit(self):
        """Enforce rate limiting between API calls."""
        elapsed = time.time() - self.last_api_call
        if elapsed < self.api_delay:
            time.sleep(self.api_delay - elapsed)
        self.last_api_call = time.time()

    def save_progress(self):
        """Save current progress state."""
        if self.progress:
            with open(self.progress_file, 'w') as f:
                json.dump(asdict(self.progress), f, indent=2)

    def load_progress(self) -> Optional[ProgressState]:
        """Load existing progress state."""
        if self.progress_file.exists():
            try:
                with open(self.progress_file, 'r') as f:
                    data = json.load(f)
                return ProgressState(**data)
            except Exception as e:
                self.logger.error(f"Failed to load progress: {e}")
        return None

    def normalize_date(self, date_str: str) -> Optional[str]:
        """Normalize various date formats to YYYY-MM-DD."""
        if not date_str:
            return None
            
        # Remove time component if present
        date_str = date_str.split('T')[0]
        
        # Handle YYYY-MM-DD (already normalized)
        if re.match(r'^\d{4}-\d{2}-\d{2}$', date_str):
            return date_str
            
        # Handle YYYY-M-D (pad with zeros)
        if re.match(r'^\d{4}-\d{1,2}-\d{1,2}$', date_str):
            parts = date_str.split('-')
            return f"{parts[0]}-{parts[1].zfill(2)}-{parts[2].zfill(2)}"
            
        # Handle MM/DD/YYYY
        if re.match(r'^\d{1,2}/\d{1,2}/\d{4}$', date_str):
            parts = date_str.split('/')
            return f"{parts[2]}-{parts[0].zfill(2)}-{parts[1].zfill(2)}"
            
        self.logger.warning(f"Unrecognized date format: {date_str}")
        return None

    def extract_source_type(self, title: str, description: str) -> str:
        """Extract recording source type from title and description."""
        text = f"{title} {description}".upper()
        
        if 'SBD' in text or 'SOUNDBOARD' in text:
            return 'SBD'
        elif 'MATRIX' in text:
            return 'MATRIX'  
        elif 'AUD' in text or 'AUDIENCE' in text:
            return 'AUD'
        elif 'FM' in text or 'BROADCAST' in text:
            return 'FM'
        elif 'REMASTER' in text:
            return 'REMASTER'
        else:
            return 'UNKNOWN'

    def get_grateful_dead_recordings(self, year: Optional[int] = None, date_range: Optional[str] = None) -> List[str]:
        """Get list of Grateful Dead recording identifiers from Archive.org search.
        
        Archive.org has a ~10k pagination limit, so for full collection we break it down intelligently.
        
        Args:
            year: Specific year to filter (e.g., 1977)
            date_range: Custom date range (e.g., '[1977-01-01 TO 1977-12-31]')
        """
        # If specific year or date range provided, use single query
        if year or date_range:
            return self._get_recordings_single_query(year, date_range)
        
        # For full collection, break down by year, with monthly breakdown for high-volume years
        self.logger.info("Full collection mode: fetching recordings by year/month to avoid pagination limits...")
        all_identifiers = []
        
        # High-volume years that need monthly breakdown (those hitting 10k limit)
        high_volume_years = [1983, 1984, 1985, 1987, 1989, 1990]
        
        # Grateful Dead active years: 1965-1995
        for year_num in range(1965, 1996):
            if year_num in high_volume_years:
                # Break down by month for high-volume years
                year_identifiers = self._get_recordings_by_month(year_num)
            else:
                # Single query for lower-volume years
                year_identifiers = self._get_recordings_single_query(year_num, None)
            
            all_identifiers.extend(year_identifiers)
            self.logger.info(f"Year {year_num}: {len(year_identifiers)} recordings (total: {len(all_identifiers)})")
            
            # Small delay between years
            time.sleep(0.2)
            
        self.logger.info(f"Found {len(all_identifiers)} total recordings across all years")
        return all_identifiers
    
    def _get_recordings_by_month(self, year: int) -> List[str]:
        """Get all recordings for a year by breaking it down month by month."""
        self.logger.info(f"  Breaking down {year} by month due to high volume...")
        all_identifiers = []
        
        # Ultra high-volume years that might need weekly breakdown
        ultra_high_volume = [1983, 1984, 1985, 1987, 1989, 1990]
        
        for month in range(1, 13):
            if year in ultra_high_volume:
                # For ultra-high volume years, break down by week within each month
                month_identifiers = self._get_recordings_by_week(year, month)
            else:
                # Regular monthly breakdown
                date_range = f'[{year}-{month:02d}-01 TO {year}-{month:02d}-31]'
                month_identifiers = self._get_recordings_single_query(None, date_range)
            
            all_identifiers.extend(month_identifiers)
            
            if len(month_identifiers) > 0:
                self.logger.info(f"    {year}-{month:02d}: {len(month_identifiers)} recordings")
            
            # Small delay between months
            time.sleep(0.1)
            
        return all_identifiers
    
    def _get_recordings_by_week(self, year: int, month: int) -> List[str]:
        """Get recordings for a month by breaking it down week by week."""
        import calendar
        
        all_identifiers = []
        days_in_month = calendar.monthrange(year, month)[1]
        
        # Break month into ~weekly chunks (7-8 days each)
        week_starts = [1, 8, 15, 22]
        
        for i, week_start in enumerate(week_starts):
            if i == len(week_starts) - 1:
                # Last week goes to end of month
                week_end = days_in_month
            else:
                week_end = min(week_starts[i + 1] - 1, days_in_month)
            
            if week_start > days_in_month:
                break
                
            date_range = f'[{year}-{month:02d}-{week_start:02d} TO {year}-{month:02d}-{week_end:02d}]'
            week_identifiers = self._get_recordings_single_query(None, date_range)
            all_identifiers.extend(week_identifiers)
            
            # Very small delay between weeks
            time.sleep(0.05)
        
        return all_identifiers
    
    def _get_recordings_single_query(self, year: Optional[int] = None, date_range: Optional[str] = None) -> List[str]:
        """Perform a single search query with pagination up to Archive.org's limits."""
        try:
            search_url = "https://archive.org/advancedsearch.php"
            
            # Build the search query
            base_query = 'collection:GratefulDead AND mediatype:etree'
            
            if date_range:
                query = f'{base_query} AND date:{date_range}'
            elif year:
                query = f'{base_query} AND date:[{year}-01-01 TO {year}-12-31]'
            else:
                query = base_query
            
            all_identifiers = []
            start = 0
            page_size = 1000  # Reliable page size
            max_safe_results = 9500  # Stay under Archive.org's ~10k limit for safety
            
            while start < max_safe_results:
                self.rate_limit()
                
                params = {
                    'q': query,
                    'fl': 'identifier,date,title,venue',
                    'sort[]': 'date asc',
                    'rows': page_size,
                    'start': start,
                    'output': 'json'
                }
                
                response = self.session.get(search_url, params=params, timeout=60)
                response.raise_for_status()
                
                search_results = response.json()
                docs = search_results.get('response', {}).get('docs', [])
                
                if not docs:
                    break
                    
                batch_identifiers = []
                for doc in docs:
                    identifier = doc.get('identifier')
                    if identifier:
                        batch_identifiers.append(identifier)
                
                all_identifiers.extend(batch_identifiers)
                
                # If we got fewer results than requested, we've reached the end
                if len(docs) < page_size:
                    break
                    
                start += page_size
                
                # Add a small delay between pages
                time.sleep(0.1)
                    
            return all_identifiers
            
        except Exception as e:
            self.logger.error(f"Failed to search for recordings: {e}")
            return []

    def fetch_recording_metadata(self, identifier: str) -> Optional[Dict]:
        """Fetch complete metadata for a single recording."""
        self.rate_limit()
        
        try:
            url = f"https://archive.org/metadata/{identifier}"
            response = self.session.get(url, timeout=30)
            response.raise_for_status()
            return response.json()
        except Exception as e:
            self.logger.error(f"Failed to fetch metadata for {identifier}: {e}")
            return None

    def fetch_recording_reviews(self, identifier: str) -> List[ReviewData]:
        """Fetch review data for a single recording."""
        self.rate_limit()
        
        try:
            url = f"https://archive.org/metadata/{identifier}/reviews"
            response = self.session.get(url, timeout=30)
            response.raise_for_status()
            
            reviews_data = response.json()
            reviews = []
            
            for review in reviews_data.get('result', []):
                stars = float(review.get('stars', 0))
                if stars > 0:
                    reviews.append(ReviewData(
                        stars=stars,
                        review_text=review.get('reviewtitle', '') + ' ' + review.get('reviewbody', ''),
                        date=review.get('reviewdate', '')
                    ))
                    
            return reviews
            
        except Exception as e:
            self.logger.error(f"Failed to fetch reviews for {identifier}: {e}")
            return []

    def compute_recording_rating(self, reviews: List[ReviewData], source_type: str) -> Tuple[float, float]:
        """Compute weighted rating and confidence for a recording."""
        if not reviews:
            return 0.0, 0.0  # No reviews = 0 rating, 0 confidence
            
        # Filter out very low ratings (likely spam)
        valid_reviews = [r for r in reviews if r.stars >= 1.0]
        if not valid_reviews:
            return 0.0, 0.0
            
        # Compute basic average
        avg_rating = sum(r.stars for r in valid_reviews) / len(valid_reviews)
        
        # Apply source type weighting
        source_weight = self.source_weights.get(source_type, 0.5)
        weighted_rating = avg_rating * source_weight
        
        # Confidence based on review count
        confidence = min(len(valid_reviews) / 5.0, 1.0)
        
        return weighted_rating * (0.5 + 0.5 * confidence), confidence

    def process_recording(self, identifier: str) -> Optional[RecordingMetadata]:
        """Process a single recording and return complete metadata."""
        try:
            # Check if already cached
            cache_file = self.recordings_dir / f"{identifier}.json"
            if cache_file.exists():
                self.logger.debug(f"Loading cached metadata for {identifier}")
                with open(cache_file, 'r') as f:
                    return RecordingMetadata(**json.load(f))
            
            self.logger.info(f"Processing {identifier}")
            
            # Fetch metadata and reviews
            metadata = self.fetch_recording_metadata(identifier)
            if not metadata:
                return None
                
            reviews = self.fetch_recording_reviews(identifier)
            
            # Extract basic info
            meta = metadata.get('metadata', {})
            title = meta.get('title', '')
            description = meta.get('description', '')
            date_str = meta.get('date', '')
            venue = meta.get('venue', '')
            location = meta.get('coverage', '')
            
            normalized_date = self.normalize_date(date_str)
            if not normalized_date:
                return None
                
            source_type = self.extract_source_type(title, description)
            rating, confidence = self.compute_recording_rating(reviews, source_type)
            
            self.logger.debug(f"  → {len(reviews)} reviews, rating: {rating:.2f}, source: {source_type}")
            
            # No filtering - collect all recordings regardless of rating
            
            # Create recording metadata
            recording_meta = RecordingMetadata(
                identifier=identifier,
                title=title,
                date=normalized_date,
                venue=venue or '',
                location=location or '',
                source_type=source_type,
                lineage=meta.get('lineage', ''),
                taper=meta.get('taper', ''),
                description=description,
                files=metadata.get('files', []),
                reviews=reviews,
                rating=rating,
                review_count=len(reviews),
                confidence=confidence,
                collection_timestamp=datetime.now().isoformat()
            )
            
            # Save to cache
            with open(cache_file, 'w') as f:
                json.dump(asdict(recording_meta), f, indent=2, default=str)
            
            return recording_meta
            
        except Exception as e:
            self.logger.error(f"Error processing {identifier}: {e}")
            return None

    def collect_all_metadata(self, max_recordings: Optional[int] = None, year: Optional[int] = None, date_range: Optional[str] = None):
        """Collect metadata for all recordings."""
        # Get list of recordings
        recording_ids = self.get_grateful_dead_recordings(year=year, date_range=date_range)
        if max_recordings:
            recording_ids = recording_ids[:max_recordings]
            
        total_recordings = len(recording_ids)
        
        # Initialize progress
        self.progress = ProgressState(
            collection_started=datetime.now().isoformat(),
            last_updated=datetime.now().isoformat(),
            status="in_progress",
            total_recordings=total_recordings,
            processed_recordings=0,
            failed_recordings=0,
            current_batch=0,
            last_processed="",
            failed_identifiers=[],
            performance_stats={
                "start_time": time.time(),
                "api_calls_made": 0,
                "api_errors": 0
            }
        )
        
        self.logger.info(f"Starting collection of {total_recordings} recordings...")
        
        # Process in batches
        for batch_start in range(0, total_recordings, self.batch_size):
            batch_end = min(batch_start + self.batch_size, total_recordings)
            batch_recordings = recording_ids[batch_start:batch_end]
            
            self.progress.current_batch += 1
            self.logger.info(f"Processing batch {self.progress.current_batch}: recordings {batch_start+1}-{batch_end}")
            
            for identifier in batch_recordings:
                recording_meta = self.process_recording(identifier)
                
                if recording_meta:
                    self.progress.processed_recordings += 1
                    self.progress.last_processed = identifier
                else:
                    self.progress.failed_recordings += 1
                    self.progress.failed_identifiers.append(identifier)
                
                self.progress.performance_stats["api_calls_made"] += 2  # metadata + reviews
                self.progress.last_updated = datetime.now().isoformat()
                
                # Save progress periodically
                if self.progress.processed_recordings % 10 == 0:
                    self.save_progress()
            
            # Batch break (except for last batch)
            if batch_end < total_recordings:
                self.logger.info(f"Batch complete. Taking {self.batch_delay}s break...")
                time.sleep(self.batch_delay)
        
        # Final progress update
        self.progress.status = "completed"
        self.progress.performance_stats["total_time"] = time.time() - self.progress.performance_stats["start_time"]
        self.save_progress()
        
        self.logger.info(f"Collection complete! Processed {self.progress.processed_recordings} recordings")

    def generate_show_metadata(self):
        """Generate show-level metadata from recording cache."""
        self.logger.info("Generating show-level metadata...")
        
        shows_data = defaultdict(list)
        
        # Group recordings by show
        for cache_file in self.recordings_dir.glob("*.json"):
            try:
                with open(cache_file, 'r') as f:
                    recording_meta = RecordingMetadata(**json.load(f))
                
                show_key = f"{recording_meta.date}_{recording_meta.venue.replace(' ', '_')}"
                shows_data[show_key].append(recording_meta)
                
            except Exception as e:
                self.logger.error(f"Error reading {cache_file}: {e}")
        
        # Generate show metadata
        for show_key, recordings in shows_data.items():
            if len(recordings) == 0:
                continue
                
            # Sort recordings by preference (SBD > others, then by rating)
            recordings.sort(key=lambda r: (
                r.source_type == 'SBD' and r.review_count >= 3,
                r.review_count >= 5,
                r.rating,
                r.review_count
            ), reverse=True)
            
            best_recording = recordings[0]
            
            # Compute show-level rating
            total_weight = 0
            weighted_sum = 0
            
            for recording in recordings:
                weight = recording.review_count * self.source_weights.get(recording.source_type, 0.5)
                weighted_sum += recording.rating * weight
                total_weight += weight
            
            show_avg_rating = weighted_sum / total_weight if total_weight > 0 else 0
            total_reviews = sum(r.review_count for r in recordings)
            confidence = min(total_reviews / 10.0, 1.0)
            
            show_meta = ShowMetadata(
                show_key=show_key,
                date=best_recording.date,
                venue=best_recording.venue,
                location=best_recording.location,
                recordings=[r.identifier for r in recordings],
                best_recording=best_recording.identifier,
                avg_rating=show_avg_rating,
                confidence=confidence,
                recording_count=len(recordings),
                collection_timestamp=datetime.now().isoformat()
            )
            
            # Save show metadata
            show_file = self.shows_dir / f"{show_key}.json"
            with open(show_file, 'w') as f:
                json.dump(asdict(show_meta), f, indent=2, default=str)
        
        self.logger.info(f"Generated metadata for {len(shows_data)} shows")

    def generate_ratings_json(self, output_file: str):
        """Generate simplified ratings.json for app bundle."""
        self.logger.info("Generating simplified ratings.json...")
        
        recording_ratings = {}
        show_ratings = {}
        
        # Collect recording ratings
        for cache_file in self.recordings_dir.glob("*.json"):
            try:
                with open(cache_file, 'r') as f:
                    recording_meta = RecordingMetadata(**json.load(f))
                
                recording_ratings[recording_meta.identifier] = {
                    "rating": recording_meta.rating,
                    "review_count": recording_meta.review_count,
                    "source_type": recording_meta.source_type,
                    "confidence": recording_meta.confidence
                }
                
            except Exception as e:
                self.logger.error(f"Error reading {cache_file}: {e}")
        
        # Collect show ratings  
        for show_file in self.shows_dir.glob("*.json"):
            try:
                with open(show_file, 'r') as f:
                    show_meta = ShowMetadata(**json.load(f))
                
                show_ratings[show_meta.show_key] = {
                    "date": show_meta.date,
                    "venue": show_meta.venue,
                    "rating": show_meta.avg_rating,
                    "confidence": show_meta.confidence,
                    "best_recording": show_meta.best_recording,
                    "recording_count": show_meta.recording_count
                }
                
            except Exception as e:
                self.logger.error(f"Error reading {show_file}: {e}")
        
        # Create simplified structure
        ratings_data = {
            "metadata": {
                "generated_at": datetime.now().isoformat(),
                "version": "1.0.0",
                "total_recordings": len(recording_ratings),
                "total_shows": len(show_ratings)
            },
            "recording_ratings": recording_ratings,
            "show_ratings": show_ratings
        }
        
        # Create compressed version directly
        zip_file = output_file.replace('.json', '.zip')
        with zipfile.ZipFile(zip_file, 'w', zipfile.ZIP_DEFLATED) as zf:
            # Write JSON directly to ZIP without creating uncompressed file
            zf.writestr('ratings.json', json.dumps(ratings_data, indent=2, sort_keys=True))
        
        # Create uncompressed version only for development/debugging
        temp_json_file = output_file + '.temp'
        with open(temp_json_file, 'w') as f:
            json.dump(ratings_data, f, indent=2, sort_keys=True)
        
        # Get file sizes and then remove temp file
        json_size = os.path.getsize(temp_json_file) / (1024 * 1024)  # MB
        zip_size = os.path.getsize(zip_file) / (1024 * 1024)  # MB
        os.remove(temp_json_file)
        
        self.logger.info(f"Generated ratings.zip with {len(recording_ratings)} recordings and {len(show_ratings)} shows")
        self.logger.info(f"Compressed: {json_size:.1f}MB → {zip_size:.1f}MB ({zip_size/json_size*100:.1f}% of original)")


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(description='Comprehensive Grateful Dead metadata collection')
    parser.add_argument('--mode', choices=['full', 'ratings-only', 'resume', 'test'], 
                       default='full', help='Collection mode')
    parser.add_argument('--delay', type=float, default=0.25, 
                       help='Delay between API calls in seconds')
    parser.add_argument('--cache', default='scripts/metadata', 
                       help='Metadata cache directory')
    parser.add_argument('--output', default='app/src/main/assets/ratings.json',
                       help='Output path for ratings file')
    parser.add_argument('--max-recordings', type=int, 
                       help='Maximum recordings to process (for testing)')
    parser.add_argument('--year', type=int,
                       help='Filter by specific year (e.g., 1977)')
    parser.add_argument('--date-range', 
                       help='Filter by date range (e.g., "[1977-01-01 TO 1977-12-31]")')
    parser.add_argument('--progress', 
                       help='Progress file to resume from')
    parser.add_argument('--verbose', action='store_true',
                       help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    collector = GratefulDeadMetadataCollector(delay=args.delay, cache_dir=args.cache)
    
    if args.mode == 'full':
        collector.collect_all_metadata(
            max_recordings=args.max_recordings,
            year=args.year,
            date_range=args.date_range
        )
        collector.generate_show_metadata()
        collector.generate_ratings_json(args.output)
        
    elif args.mode == 'ratings-only':
        collector.generate_show_metadata()
        collector.generate_ratings_json(args.output)
        
    elif args.mode == 'resume':
        # TODO: Implement resume functionality
        print("Resume mode not yet implemented")
        
    elif args.mode == 'test':
        collector.collect_all_metadata(
            max_recordings=args.max_recordings or 10,
            year=args.year,
            date_range=args.date_range
        )
        collector.generate_show_metadata()
        collector.generate_ratings_json(args.output)
    
    print(f"✅ Collection complete! Output: {args.output}")


if __name__ == '__main__':
    main()