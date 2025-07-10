#!/usr/bin/env python3
"""
Song Processor for Grateful Dead Archive

This script processes song information from raw setlist data to create a normalized songs database.
It follows Stage 2.2 of the implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Extracts all songs from setlist data across all sets (set1, set2, set3, encore)
- Normalizes song names to handle variations, typos, and aliases
- Creates unique song IDs for consistent referencing
- Generates song statistics (frequency, first/last played, performance data)
- Identifies song relationships and common segues
- Produces clean songs.json for app integration

Key Features:
- Smart song name normalization (e.g., "Sugar Magnolia" vs "Sugar Mag")
- Segue detection and relationship mapping ("Scarlet > Fire")
- Song alias tracking for different name variations
- Performance statistics and historical data
- Song transition analysis for common pairings

Usage:
    python scripts/process_songs.py --input scripts/metadata/setlists/raw_setlists.json --output scripts/metadata/songs/songs.json
    python scripts/process_songs.py --input raw_setlists.json --output songs.json --verbose
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
        logging.FileHandler('process_songs.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class SongProcessor:
    """Processes and normalizes song data from raw setlists"""
    
    def __init__(self):
        """Initialize the song processor"""
        self.songs = {}
        self.song_aliases = {}
        self.song_relationships = defaultdict(lambda: defaultdict(int))
        self.processing_stats = {
            'total_shows': 0,
            'total_song_occurrences': 0,
            'unique_songs': 0,
            'songs_normalized': 0,
            'segues_detected': 0,
            'start_time': datetime.now().isoformat()
        }
        
        # Common song name normalizations
        self.song_normalizations = {
            # Remove common prefixes/suffixes and punctuation
            r'^the\s+': '',
            r'\s+the$': '',
            r'^\s*': '',
            r'\s*$': '',
            r'[^\w\s\-\'\(\)]': '',  # Keep only word chars, spaces, hyphens, apostrophes, parens
            
            # Standardize common abbreviations and variations
            r'\bst\b\.?': 'street',
            r'\bmt\b\.?': 'mount',
            r'\bmtn\b\.?': 'mountain',
            r'\bd\.?\s*s\.?': 'drums space',  # Drums > Space
            r'\bdr\b\.?\s*sp\b\.?': 'drums space',
            r'\btuning\b': '',  # Remove tuning segments
            r'\bsound\s*check\b': '',  # Remove sound check
            
            # Handle common Grateful Dead song variations
            r'\bsugar\s*mag\b': 'sugar magnolia',
            r'\bsugar\s*mags\b': 'sugar magnolia',
            r'\bsugaree\b': 'sugaree',  # Different from Sugar Magnolia
            r'\btruckin\b': "truckin'",
            r'\btruck\b': "truckin'",
            r'\bripple\b': 'ripple',
            r'\bfotm\b': 'fire on the mountain',
            r'\bfire\b': 'fire on the mountain',
            r'\bmountain\b(?!\s+song)': 'fire on the mountain',  # Not "Mountain Song"
            r'\bother\s*one\b': 'the other one',
            r'\bdark\s*star\b': 'dark star',
            r'\bspace\b(?!\s+oddity)': 'space',  # Jam segment
            r'\bdrums\b': 'drums',  # Jam segment
            r'\bjam\b': 'jam',
            r'\btiger\b': 'the tiger',
            r'\btighten\s*up\b': 'tighten up',
            
            # Handle segue indicators
            r'\s*>\s*': ' > ',  # Normalize segue arrows
            r'\s*->\s*': ' > ',
            r'\s*→\s*': ' > ',
            r'\s+into\s+': ' > ',
            r'\s+jam\s*>\s*': ' > ',
        }
        
        # Common song aliases (song variations that should map to canonical names)
        self.song_alias_mappings = {
            # Jerry Garcia Band songs sometimes played
            'catfish john': 'catfish john',
            'cats under the stars': 'cats under the stars',
            
            # Bob Dylan covers
            'all along the watchtower': 'all along the watchtower',
            'knockin on heavens door': "knockin' on heaven's door",
            'knockin on heaven\'s door': "knockin' on heaven's door",
            
            # Traditional/cover songs
            'not fade away': 'not fade away',
            'good lovin': "good lovin'",
            'good loving': "good lovin'",
            'turn on your love light': 'turn on your love light',
            'love light': 'turn on your love light',
            
            # Grateful Dead originals with variations
            'uncle johns band': "uncle john's band",
            'uncle john\'s band': "uncle john's band",
            'friend of the devil': 'friend of the devil',
            'fotd': 'friend of the devil',
            'casey jones': 'casey jones',
            'jack straw': 'jack straw',
            'deal': 'deal',
            'tennessee jed': 'tennessee jed',
            'tennesse jed': 'tennessee jed',
            'bertha': 'bertha',
            
            # Jam segments
            'space jam': 'space',
            'drum solo': 'drums',
            'drum jam': 'drums',
            'feedback': 'feedback',
            'tuning': 'tuning',
            
            # Encore-specific variations
            'encore break': '',  # Remove
            'encore': '',  # Remove standalone encore markers
            
            # Common goodnight song variations
            'and we bid you goodnight': 'we bid you goodnight',
            'we bid you goodnight': 'we bid you goodnight',
            'bid you goodnight': 'we bid you goodnight',
            
            # Early years songs from GDSets
            'the eleven jam': 'the eleven',
            'eleven jam': 'the eleven',
            'the main ten': 'the main ten',
            'main ten': 'the main ten',
            'the raven space': 'the raven',
            'raven space': 'the raven',
            'the boxer': 'the boxer',
            'boxer': 'the boxer',
            'goodnight irene': 'goodnight irene',
            'the ballad of frankie lee judas priest': 'ballad of frankie lee and judas priest',
            'the ballad of frankie lee & judas priest': 'ballad of frankie lee and judas priest',
            'ballad of frankie lee judas priest': 'ballad of frankie lee and judas priest',
            'ballad of frankie lee & judas priest': 'ballad of frankie lee and judas priest',
            'the things i used to do': 'things i used to do',
            'things i used to do': 'things i used to do',
        }
        
        # Commentary patterns to filter out (not actual songs)
        self.commentary_patterns = [
            r'.*on piano.*',
            r'.*on keyboards.*',
            r'.*on guitar.*',
            r'.*on bass.*',
            r'.*on drums.*',
            r'.*the entire show.*',
            r'.*entire set.*',
            r'.*guest.*',
            r'.*appeared.*',
            r'.*joined.*',
            r'.*sits in.*',
            r'.*special guest.*',
            r'.*birthday.*',
            r'.*dedication.*',
            r'.*acoustic.*set.*',
            r'.*electric.*set.*',
            r'.*sound.*check.*',
            r'.*tuning.*problems.*',
            r'.*technical.*problems.*',
            r'.*equipment.*problems.*',
            r'.*difficulty.*',
            r'.*break.*',
            r'.*intermission.*',
            r'.*announcement.*',
            r'.*talk.*',
            r'.*banter.*',
            r'.*microphone.*check.*',
            r'.*mic.*check.*',
            r'.*happy.*birthday.*',
            r'.*thank.*you.*audience.*',
            r'.*good.*night.*everyone.*',
            r'.*see.*you.*next.*',
            r'.*billed as.*',
            r'.*bill.*as.*',
            r'.*advertised as.*',
            r'.*listed as.*',
            r'\(.*billed.*\)',
            r'\(.*bill.*\)',
            r'^and$',
            r'^with$',
            r'^plus$',
            r'^featuring$',
            r'^guest$',
            r'^special$',
            r'^acoustic set:$',
            r'^electric set:$',
            r'^\d+\s+songs?$'
        ]
        
        # Segue patterns to detect song relationships
        self.segue_indicators = [
            r'\s*>\s*',
            r'\s*->\s*', 
            r'\s*→\s*',
            r'\s+into\s+',
            r'\s+jam\s*>\s*'
        ]
    
    def load_setlists(self, input_path: str) -> Dict[str, Any]:
        """
        Load raw setlist data
        
        Args:
            input_path: Path to raw setlists JSON file
            
        Returns:
            Loaded setlist data
        """
        try:
            with open(input_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            if 'setlists' not in data:
                raise ValueError("Input file missing 'setlists' key")
            
            logger.info(f"Loaded {len(data['setlists'])} shows from {input_path}")
            return data
            
        except Exception as e:
            logger.error(f"Failed to load setlists: {e}")
            raise
    
    def is_commentary(self, song_name: str) -> bool:
        """
        Check if a song entry is actually commentary and should be filtered out
        
        Args:
            song_name: Raw song name to check
            
        Returns:
            True if this appears to be commentary, not a song
        """
        if not song_name or not song_name.strip():
            return True
        
        song_lower = song_name.lower().strip()
        
        # Check against commentary patterns
        for pattern in self.commentary_patterns:
            if re.match(pattern, song_lower, re.IGNORECASE):
                return True
        
        return False
    
    def normalize_song_name(self, song_name: str) -> str:
        """
        Normalize song name for consistent matching
        
        Args:
            song_name: Raw song name
            
        Returns:
            Normalized song name (empty string if commentary)
        """
        if not song_name or not song_name.strip():
            return ""
        
        # Filter out commentary first
        if self.is_commentary(song_name):
            return ""
        
        normalized = song_name.lower().strip()
        
        # Remove asterisks and special annotations first
        normalized = re.sub(r'\*+', '', normalized)  # Remove asterisks
        normalized = re.sub(r'\s*\([^)]*\)\s*', ' ', normalized)  # Remove parenthetical notes
        
        # Remove trailing segue indicators first  
        normalized = re.sub(r'\s*->\s*$', '', normalized)
        normalized = re.sub(r'\s*>\s*$', '', normalized)
        
        # Clean up extra whitespace before other processing
        normalized = re.sub(r'\s+', ' ', normalized).strip()
        
        # Apply normalization rules
        for pattern, replacement in self.song_normalizations.items():
            normalized = re.sub(pattern, replacement, normalized, flags=re.IGNORECASE)
        
        # Clean up extra whitespace
        normalized = re.sub(r'\s+', ' ', normalized).strip()
        
        # Apply alias mappings
        if normalized in self.song_alias_mappings:
            normalized = self.song_alias_mappings[normalized]
        
        # Remove empty results
        if not normalized or normalized in ['', ' ', 'unknown', 'untitled']:
            return ""
        
        return normalized
    
    def detect_segues(self, song_name: str) -> List[str]:
        """
        Detect and split segued songs
        
        Args:
            song_name: Song name that might contain segues
            
        Returns:
            List of individual song names
        """
        if not song_name:
            return []
        
        # Check for segue indicators
        for pattern in self.segue_indicators:
            if re.search(pattern, song_name, re.IGNORECASE):
                # Split on segue indicators
                songs = re.split(pattern, song_name, flags=re.IGNORECASE)
                # Normalize each song in the segue
                normalized_songs = []
                for song in songs:
                    normalized = self.normalize_song_name(song.strip())
                    if normalized:
                        normalized_songs.append(normalized)
                return normalized_songs
        
        # No segue detected, return single normalized song
        normalized = self.normalize_song_name(song_name)
        return [normalized] if normalized else []
    
    def generate_song_id(self, song_name: str) -> str:
        """
        Generate unique song ID
        
        Args:
            song_name: Normalized song name
            
        Returns:
            Unique song ID
        """
        # Create a unique string for hashing
        song_key = song_name.lower().strip()
        
        # Generate short hash
        song_hash = hashlib.md5(song_key.encode()).hexdigest()[:8]
        
        # Create readable ID
        words = re.findall(r'\b\w+\b', song_name.lower())
        significant_words = [w for w in words[:3] if len(w) > 2]  # Take first few significant words
        
        if not significant_words:
            # Fallback for very short song names
            clean_name = re.sub(r'[^a-z0-9]', '', song_name.lower())
            significant_words = [clean_name[:8]] if clean_name else ['song']
        
        # Create base ID
        base_id = '-'.join(significant_words)
        # Add hash for uniqueness
        song_id = f"{base_id}-{song_hash}"
        
        # Clean up the ID
        song_id = re.sub(r'[^a-z0-9\-]', '', song_id)
        song_id = re.sub(r'\-+', '-', song_id).strip('-')
        
        return song_id
    
    def process_songs(self, setlists_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process all songs from setlist data
        
        Args:
            setlists_data: Raw setlist data
            
        Returns:
            Processed songs data
        """
        logger.info("Starting song processing")
        start_time = datetime.now()
        
        # Track song occurrences and relationships
        song_occurrences = defaultdict(list)
        song_names_seen = defaultdict(set)  # Track aliases
        song_transitions = defaultdict(lambda: defaultdict(int))
        
        # Process each show
        for show_id, setlist in setlists_data['setlists'].items():
            self.processing_stats['total_shows'] += 1
            
            # Get the sets data
            sets_data = setlist.get('sets', {})
            if not sets_data:
                continue
                
            # Process each set type
            for set_type in ['set1', 'set2', 'set3', 'encore']:
                if set_type not in sets_data or not sets_data[set_type]:
                    continue
                
                songs_in_set = sets_data[set_type]
                if not isinstance(songs_in_set, list):
                    continue
                
                previous_songs = []
                
                for position, song_name in enumerate(songs_in_set):
                    if not song_name or not isinstance(song_name, str):
                        continue
                    
                    self.processing_stats['total_song_occurrences'] += 1
                    
                    # Handle segued songs
                    segued_songs = self.detect_segues(song_name)
                    if len(segued_songs) > 1:
                        self.processing_stats['segues_detected'] += 1
                    
                    for seg_idx, normalized_song in enumerate(segued_songs):
                        if not normalized_song:
                            continue
                        
                        # Generate song ID
                        song_id = self.generate_song_id(normalized_song)
                        
                        # Track song occurrence
                        song_occurrences[song_id].append({
                            'show_id': show_id,
                            'set_type': set_type,
                            'position': position + seg_idx,
                            'original_name': song_name,
                            'normalized_name': normalized_song
                        })
                        
                        # Track name variations
                        song_names_seen[song_id].add(song_name.strip())
                        song_names_seen[song_id].add(normalized_song)
                        
                        # Track transitions (what songs commonly follow this one)
                        if previous_songs:
                            for prev_song_id in previous_songs[-3:]:  # Look at last few songs
                                song_transitions[prev_song_id][song_id] += 1
                        
                        previous_songs.append(song_id)
        
        # Build song database
        songs_db = {}
        
        for song_id, occurrences in song_occurrences.items():
            # Get canonical song name (most common normalized version)
            song_names = [occ['normalized_name'] for occ in occurrences]
            canonical_name = Counter(song_names).most_common(1)[0][0]
            
            # Get show date range
            show_ids = [occ['show_id'] for occ in occurrences]
            show_dates = []
            for show_id in show_ids:
                try:
                    show_date = datetime.strptime(show_id, '%Y-%m-%d')
                    show_dates.append(show_date)
                except ValueError:
                    pass
            
            first_played = min(show_dates).strftime('%Y-%m-%d') if show_dates else ""
            last_played = max(show_dates).strftime('%Y-%m-%d') if show_dates else ""
            
            # Get common transitions
            common_transitions = []
            if song_id in song_transitions:
                transitions = Counter(song_transitions[song_id])
                # Get top 5 most common transitions
                for next_song_id, count in transitions.most_common(5):
                    if count >= 2:  # Only include if it happened at least twice
                        common_transitions.append({
                            'song_id': next_song_id,
                            'frequency': count
                        })
            
            # Create song entry
            songs_db[song_id] = {
                'song_id': song_id,
                'name': canonical_name,
                'times_played': len(occurrences),
                'first_played': first_played,
                'last_played': last_played,
                'aliases': sorted(list(song_names_seen[song_id])),
                'common_transitions': common_transitions,
                'shows': sorted(list(set(show_ids)))
            }
        
        # Update statistics
        self.processing_stats['unique_songs'] = len(songs_db)
        self.processing_stats['songs_normalized'] = len(song_occurrences)
        
        duration = (datetime.now() - start_time).total_seconds()
        
        logger.info(f"Song processing completed: {len(songs_db)} unique songs in {duration:.1f} seconds")
        
        return {
            'metadata': {
                'source': 'processed from raw_setlists.json',
                'processed_at': datetime.now().isoformat(),
                'processing_duration_seconds': duration,
                'total_songs': len(songs_db),
                'total_occurrences': self.processing_stats['total_song_occurrences'],
                'processing_stats': self.processing_stats,
                'processor_version': '1.0.0'
            },
            'songs': songs_db
        }
    
    def save_songs(self, songs_data: Dict[str, Any], output_path: str) -> None:
        """
        Save processed songs data
        
        Args:
            songs_data: Processed songs data
            output_path: Output file path
        """
        try:
            output_file = Path(output_path)
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(songs_data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Songs data saved to {output_path}")
            
            # Log file size
            file_size = output_file.stat().st_size
            logger.info(f"Output file size: {file_size / 1024:.1f} KB")
            
        except Exception as e:
            logger.error(f"Failed to save songs data: {e}")
            raise
    
    def print_song_summary(self, songs_data: Dict[str, Any]) -> None:
        """Print summary of song processing"""
        songs = songs_data.get('songs', {})
        metadata = songs_data.get('metadata', {})
        stats = metadata.get('processing_stats', {})
        
        print("\n" + "="*60)
        print("SONG PROCESSING SUMMARY")
        print("="*60)
        print(f"Total shows processed:     {stats.get('total_shows', 0):,}")
        print(f"Total song occurrences:    {stats.get('total_song_occurrences', 0):,}")
        print(f"Unique songs identified:   {metadata.get('total_songs', 0):,}")
        print(f"Segues detected:           {stats.get('segues_detected', 0):,}")
        print(f"Processing time:           {metadata.get('processing_duration_seconds', 0):.1f}s")
        
        if songs:
            # Show song statistics
            play_counts = [s['times_played'] for s in songs.values()]
            most_played = max(play_counts) if play_counts else 0
            avg_plays = sum(play_counts) / len(play_counts) if play_counts else 0
            
            print(f"\nSong Statistics:")
            print(f"Most played song:          {most_played} times")
            print(f"Average plays per song:    {avg_plays:.1f}")
            
            # Show top songs
            top_songs = sorted(songs.values(), key=lambda x: x['times_played'], reverse=True)[:10]
            print(f"\nTop 10 Most Played Songs:")
            for i, song in enumerate(top_songs, 1):
                name = song['name'] or 'Unknown'
                times = song['times_played']
                first = song['first_played']
                last = song['last_played']
                date_range = f"({first} - {last})" if first and last else ""
                print(f"  {i:2d}. {name} - {times} times {date_range}")
        
        print("="*60)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Process songs from raw setlist data')
    parser.add_argument('--input', '-i', required=True,
                        help='Input raw setlists JSON file')
    parser.add_argument('--output', '-o', required=True,
                        help='Output songs JSON file')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize processor
    processor = SongProcessor()
    
    try:
        # Load setlist data
        setlists_data = processor.load_setlists(args.input)
        
        # Process songs
        songs_data = processor.process_songs(setlists_data)
        
        # Save results
        processor.save_songs(songs_data, args.output)
        
        # Print summary
        processor.print_song_summary(songs_data)
        
        logger.info("Song processing completed successfully")
        
    except KeyboardInterrupt:
        logger.info("Processing interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Processing failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()