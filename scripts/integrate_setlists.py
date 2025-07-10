#!/usr/bin/env python3
"""
Setlist Integrator for Grateful Dead Archive

This script integrates processed setlist, venue, and song data to create the final 
normalized setlist database with ID references. It follows Stage 2.3 of the 
implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Loads processed setlists, venues, and songs data
- Matches setlists with venue IDs and song IDs for referential integrity
- Creates clean setlist database without data duplication
- Generates showId-keyed structure for app integration
- Handles missing data gracefully with fallback strategies

Key Features:
- ID-based referencing to venues.json and songs.json
- Date normalization and validation
- Show ID matching for integration with existing app data
- Error handling for missing venues or songs
- Clean JSON output optimized for mobile app consumption

Usage:
    python scripts/integrate_setlists.py --setlists raw_setlists.json --venues venues.json --songs songs.json --output setlists.json
    python scripts/integrate_setlists.py --help
"""

import argparse
import json
import logging
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
from collections import defaultdict, Counter
import hashlib


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('integrate_setlists.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class SetlistIntegrator:
    """Integrates setlist, venue, and song data into final normalized database"""
    
    def __init__(self):
        """Initialize the setlist integrator"""
        self.venues_by_id = {}
        self.songs_by_id = {}
        self.venues_lookup = {}  # For fuzzy matching venue names
        self.songs_lookup = {}   # For fuzzy matching song names
        self.processing_stats = {
            'total_shows': 0,
            'shows_integrated': 0,
            'venues_matched': 0,
            'songs_matched': 0,
            'missing_venues': 0,
            'missing_songs': 0,
            'start_time': datetime.now().isoformat()
        }
    
    def load_venues(self, venues_path: str) -> None:
        """
        Load venues data and create lookup tables
        
        Args:
            venues_path: Path to venues JSON file
        """
        try:
            with open(venues_path, 'r', encoding='utf-8') as f:
                venues_data = json.load(f)
            
            self.venues_by_id = venues_data.get('venues', {})
            
            # Create lookup table for venue name matching
            for venue_id, venue_info in self.venues_by_id.items():
                venue_name = venue_info.get('name', '').lower().strip()
                city = venue_info.get('city', '').lower().strip()
                
                # Primary lookup by name
                if venue_name:
                    self.venues_lookup[venue_name] = venue_id
                
                # Secondary lookup by name + city for disambiguation
                if venue_name and city:
                    self.venues_lookup[f"{venue_name} {city}"] = venue_id
                
                # Include aliases
                for alias in venue_info.get('aliases', []):
                    alias_key = alias.lower().strip()
                    if alias_key and alias_key not in self.venues_lookup:
                        self.venues_lookup[alias_key] = venue_id
            
            logger.info(f"Loaded {len(self.venues_by_id)} venues with {len(self.venues_lookup)} lookup entries")
            
        except Exception as e:
            logger.error(f"Failed to load venues: {e}")
            raise
    
    def load_songs(self, songs_path: str) -> None:
        """
        Load songs data and create lookup tables
        
        Args:
            songs_path: Path to songs JSON file
        """
        try:
            with open(songs_path, 'r', encoding='utf-8') as f:
                songs_data = json.load(f)
            
            self.songs_by_id = songs_data.get('songs', {})
            
            # Create lookup table for song name matching
            for song_id, song_info in self.songs_by_id.items():
                song_name = song_info.get('name', '').lower().strip()
                
                # Primary lookup by normalized name
                if song_name:
                    self.songs_lookup[song_name] = song_id
                
                # Include aliases
                for alias in song_info.get('aliases', []):
                    alias_key = alias.lower().strip()
                    if alias_key and alias_key not in self.songs_lookup:
                        self.songs_lookup[alias_key] = song_id
            
            logger.info(f"Loaded {len(self.songs_by_id)} songs with {len(self.songs_lookup)} lookup entries")
            
        except Exception as e:
            logger.error(f"Failed to load songs: {e}")
            raise
    
    def load_setlists(self, setlists_path: str) -> Dict[str, Any]:
        """
        Load raw setlists data
        
        Args:
            setlists_path: Path to raw setlists JSON file
            
        Returns:
            Loaded setlists data
        """
        try:
            with open(setlists_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            if 'setlists' not in data:
                raise ValueError("Input file missing 'setlists' key")
            
            logger.info(f"Loaded {len(data['setlists'])} setlists")
            return data
            
        except Exception as e:
            logger.error(f"Failed to load setlists: {e}")
            raise
    
    def normalize_venue_for_lookup(self, venue_line: str) -> str:
        """
        Extract and normalize venue name from venue line for lookup
        
        Args:
            venue_line: Raw venue line from setlist
            
        Returns:
            Normalized venue name for lookup
        """
        if not venue_line:
            return ""
        
        # Remove date information in parentheses
        venue_clean = re.sub(r'\s*\([^)]+\)\s*$', '', venue_line).strip()
        
        # Split by comma to get venue name (first part)
        parts = venue_clean.split(',')
        if parts:
            venue_name = parts[0].strip().lower()
            return venue_name
        
        return venue_clean.lower()
    
    def find_venue_id(self, venue_line: str) -> Optional[str]:
        """
        Find venue ID from venue line using fuzzy matching
        
        Args:
            venue_line: Raw venue line from setlist
            
        Returns:
            Venue ID if found, None otherwise
        """
        if not venue_line:
            return None
        
        # Try exact venue name match
        venue_name = self.normalize_venue_for_lookup(venue_line)
        if venue_name in self.venues_lookup:
            return self.venues_lookup[venue_name]
        
        # Try partial matches for common variations
        for lookup_key, venue_id in self.venues_lookup.items():
            if venue_name in lookup_key or lookup_key in venue_name:
                # Verify it's a reasonable match (not too short)
                if len(venue_name) > 3 and len(lookup_key) > 3:
                    return venue_id
        
        return None
    
    def normalize_song_for_lookup(self, song_name: str) -> str:
        """
        Normalize song name for lookup (should match song processor logic)
        
        Args:
            song_name: Raw song name
            
        Returns:
            Normalized song name for lookup
        """
        if not song_name:
            return ""
        
        normalized = song_name.lower().strip()
        
        # Remove trailing segue indicators (same as song processor)
        normalized = re.sub(r'\s*->\s*$', '', normalized)
        normalized = re.sub(r'\s*>\s*$', '', normalized)
        
        # Basic normalizations
        normalized = re.sub(r'[^\w\s\-\'\(\)]', '', normalized)
        normalized = re.sub(r'\s+', ' ', normalized).strip()
        
        return normalized
    
    def find_song_id(self, song_name: str) -> Optional[str]:
        """
        Find song ID from song name using fuzzy matching
        
        Args:
            song_name: Raw song name
            
        Returns:
            Song ID if found, None otherwise
        """
        if not song_name:
            return None
        
        # Handle segued songs (e.g., "Scarlet Begonias > Fire on the Mountain")
        if ' > ' in song_name:
            # For segued songs, try to find the first song
            parts = song_name.split(' > ', 1)
            if parts:
                first_song = self.normalize_song_for_lookup(parts[0])
                if first_song in self.songs_lookup:
                    return self.songs_lookup[first_song]
        
        # Try exact match
        normalized = self.normalize_song_for_lookup(song_name)
        if normalized in self.songs_lookup:
            return self.songs_lookup[normalized]
        
        return None
    
    def process_song_list(self, songs: List[str]) -> List[str]:
        """
        Process list of songs and return list of song IDs
        
        Args:
            songs: List of raw song names
            
        Returns:
            List of song IDs
        """
        song_ids = []
        
        for song_name in songs:
            if not song_name:
                continue
            
            song_id = self.find_song_id(song_name)
            if song_id:
                song_ids.append(song_id)
                self.processing_stats['songs_matched'] += 1
            else:
                # Log missing songs for debugging
                logger.debug(f"Song not found: '{song_name}'")
                self.processing_stats['missing_songs'] += 1
                # Still include the original name as fallback
                song_ids.append(song_name)
        
        return song_ids
    
    def integrate_setlists(self, setlists_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Integrate setlists with venue and song references
        
        Args:
            setlists_data: Raw setlists data
            
        Returns:
            Integrated setlists data
        """
        logger.info("Starting setlist integration")
        start_time = datetime.now()
        
        integrated_setlists = {}
        
        for show_id, setlist in setlists_data['setlists'].items():
            self.processing_stats['total_shows'] += 1
            
            try:
                # Find venue ID
                venue_line = setlist.get('venue_line', '')
                venue_id = self.find_venue_id(venue_line)
                
                if venue_id:
                    self.processing_stats['venues_matched'] += 1
                else:
                    self.processing_stats['missing_venues'] += 1
                    logger.debug(f"Venue not found for {show_id}: '{venue_line}'")
                
                # Process sets
                sets_data = setlist.get('sets', {})
                processed_sets = {}
                
                for set_type, songs in sets_data.items():
                    if isinstance(songs, list):
                        song_ids = self.process_song_list(songs)
                        if song_ids:  # Only include non-empty sets
                            processed_sets[set_type] = song_ids
                
                # Create integrated setlist entry
                integrated_entry = {
                    'showId': show_id,
                    'date': show_id,  # Assuming show_id is in YYYY-MM-DD format
                    'sets': processed_sets,
                    'source': setlist.get('source', 'unknown')
                }
                
                # Add venue ID if found
                if venue_id:
                    integrated_entry['venueId'] = venue_id
                else:
                    # Keep original venue line as fallback
                    integrated_entry['venue_line'] = venue_line
                
                integrated_setlists[show_id] = integrated_entry
                self.processing_stats['shows_integrated'] += 1
                
            except Exception as e:
                logger.error(f"Failed to process setlist {show_id}: {e}")
                continue
        
        duration = (datetime.now() - start_time).total_seconds()
        
        logger.info(f"Setlist integration completed: {len(integrated_setlists)} shows in {duration:.1f} seconds")
        
        return {
            'metadata': {
                'source': 'integrated from raw_setlists.json, venues.json, songs.json',
                'processed_at': datetime.now().isoformat(),
                'processing_duration_seconds': duration,
                'total_setlists': len(integrated_setlists),
                'processing_stats': self.processing_stats,
                'integrator_version': '1.0.0'
            },
            'setlists': integrated_setlists
        }
    
    def save_setlists(self, integrated_data: Dict[str, Any], output_path: str) -> None:
        """
        Save integrated setlists data
        
        Args:
            integrated_data: Integrated setlists data
            output_path: Output file path
        """
        try:
            output_file = Path(output_path)
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(integrated_data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Integrated setlists saved to {output_path}")
            
            # Log file size
            file_size = output_file.stat().st_size
            logger.info(f"Output file size: {file_size / 1024:.1f} KB")
            
        except Exception as e:
            logger.error(f"Failed to save integrated setlists: {e}")
            raise
    
    def print_integration_summary(self, integrated_data: Dict[str, Any]) -> None:
        """Print summary of setlist integration"""
        setlists = integrated_data.get('setlists', {})
        metadata = integrated_data.get('metadata', {})
        stats = metadata.get('processing_stats', {})
        
        print("\n" + "="*60)
        print("SETLIST INTEGRATION SUMMARY")
        print("="*60)
        print(f"Total shows processed:     {stats.get('total_shows', 0):,}")
        print(f"Shows successfully integrated: {stats.get('shows_integrated', 0):,}")
        print(f"Venues matched:            {stats.get('venues_matched', 0):,}")
        print(f"Songs matched:             {stats.get('songs_matched', 0):,}")
        print(f"Missing venues:            {stats.get('missing_venues', 0):,}")
        print(f"Missing songs:             {stats.get('missing_songs', 0):,}")
        print(f"Processing time:           {metadata.get('processing_duration_seconds', 0):.1f}s")
        
        if setlists:
            # Show venue matching statistics
            venue_match_rate = (stats.get('venues_matched', 0) / stats.get('total_shows', 1)) * 100
            song_match_rate = (stats.get('songs_matched', 0) / (stats.get('songs_matched', 0) + stats.get('missing_songs', 0))) * 100 if (stats.get('songs_matched', 0) + stats.get('missing_songs', 0)) > 0 else 0
            
            print(f"\nMatch Statistics:")
            print(f"Venue match rate:          {venue_match_rate:.1f}%")
            print(f"Song match rate:           {song_match_rate:.1f}%")
            
            # Show sample integrated setlist
            sample_show = next(iter(setlists.values()))
            print(f"\nSample Integrated Setlist:")
            print(f"Show ID: {sample_show.get('showId', 'N/A')}")
            print(f"Venue ID: {sample_show.get('venueId', 'N/A')}")
            print(f"Sets: {list(sample_show.get('sets', {}).keys())}")
            
            # Show sets breakdown
            set_counts = defaultdict(int)
            for setlist in setlists.values():
                for set_type in setlist.get('sets', {}).keys():
                    set_counts[set_type] += 1
            
            print(f"\nSet Types Distribution:")
            for set_type, count in sorted(set_counts.items()):
                print(f"  {set_type}: {count} shows")
        
        print("="*60)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Integrate setlists with venue and song references')
    parser.add_argument('--setlists', '-s', required=True,
                        help='Input raw setlists JSON file')
    parser.add_argument('--venues', '-v', required=True,
                        help='Input venues JSON file')  
    parser.add_argument('--songs', '-g', required=True,
                        help='Input songs JSON file')
    parser.add_argument('--output', '-o', required=True,
                        help='Output integrated setlists JSON file')
    parser.add_argument('--verbose', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize integrator
    integrator = SetlistIntegrator()
    
    try:
        # Load reference data
        integrator.load_venues(args.venues)
        integrator.load_songs(args.songs)
        
        # Load setlists
        setlists_data = integrator.load_setlists(args.setlists)
        
        # Integrate setlists
        integrated_data = integrator.integrate_setlists(setlists_data)
        
        # Save results
        integrator.save_setlists(integrated_data, args.output)
        
        # Print summary
        integrator.print_integration_summary(integrated_data)
        
        logger.info("Setlist integration completed successfully")
        
    except KeyboardInterrupt:
        logger.info("Processing interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Integration failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()