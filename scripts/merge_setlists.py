#!/usr/bin/env python3
"""
Setlist Merger for Grateful Dead Archive

This script merges setlist data from CMU and GDSets sources to create a unified dataset.
It follows Stage 1.3 of the implementation plan outlined in docs/setlist-implementation-plan.md.

Key Features:
- Combines CMU (1972-1995) and GDSets (1965-1971) setlist data
- Resolves CMU set3/encore classification issue  
- Uses CMU as primary source for overlapping dates (1972+)
- Preserves all available metadata fields
- Handles conflicts with documented precedence rules
- Outputs unified raw setlist dataset

Architecture:
- CMU covers 1972-1995 (1,604 shows) - primary source for these years
- GDSets covers 1965-1971 (268 shows) - primary source for early years
- Set normalization: CMU "set3" → "encore" when appropriate
- Conflict resolution with source preference logging
- Comprehensive metadata merge

Usage:
    python scripts/merge_setlists.py --cmu scripts/metadata/setlists/cmu_setlists.json --gdsets scripts/metadata/setlists/gdsets_early_setlists.json --output scripts/metadata/setlists/raw_setlists.json
    python scripts/merge_setlists.py --cmu cmu.json --gdsets gdsets.json --output merged.json --verbose
"""

import argparse
import json
import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
from collections import defaultdict


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('merge_setlists.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class SetlistMerger:
    """Merges CMU and GDSets setlist data with normalization and conflict resolution"""
    
    def __init__(self):
        """Initialize the merger"""
        self.merge_stats = {
            'cmu_shows': 0,
            'gdsets_shows': 0,
            'total_merged': 0,
            'conflicts_resolved': 0,
            'set3_normalized': 0,
            'overlapping_dates': [],
            'year_coverage': {},
            'errors': []
        }
        
        # Encore detection patterns for set normalization
        self.encore_indicators = [
            'one more saturday night',
            'sugar magnolia',
            'morning dew',
            'brokedown palace',
            'we bid you goodnight',
            'black peter',
            'ripple',
            'attics of my life'
        ]
        
    def load_setlist_file(self, file_path: str) -> Dict[str, Any]:
        """
        Load and validate a setlist JSON file
        
        Args:
            file_path: Path to the JSON file
            
        Returns:
            Loaded setlist data dictionary
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            # Validate required structure
            if not all(key in data for key in ['metadata', 'setlists']):
                raise ValueError(f"Invalid setlist file structure: missing required keys")
            
            logger.info(f"Loaded {len(data['setlists'])} shows from {file_path}")
            return data
            
        except Exception as e:
            logger.error(f"Failed to load {file_path}: {e}")
            raise
    
    def is_likely_encore(self, songs: List[str], set_position: str) -> bool:
        """
        Determine if a set is likely an encore based on content and position
        
        Args:
            songs: List of songs in the set
            set_position: Set identifier (set1, set2, set3, etc.)
            
        Returns:
            True if this appears to be an encore
        """
        if set_position != 'set3':
            return False
            
        # Short sets (1-3 songs) in set3 position are likely encores
        if len(songs) <= 3:
            return True
        
        # Check for common encore songs
        for song in songs:
            song_lower = song.lower().strip()
            for indicator in self.encore_indicators:
                if indicator in song_lower:
                    return True
        
        return False
    
    def normalize_sets(self, setlist: Dict[str, Any], source: str) -> Dict[str, Any]:
        """
        Normalize set structure, particularly handling CMU set3 → encore conversion
        
        Args:
            setlist: Setlist dictionary
            source: Source identifier for logging
            
        Returns:
            Normalized setlist dictionary
        """
        normalized = setlist.copy()
        sets = normalized.get('sets', {}).copy()
        
        # Handle CMU set3 → encore normalization
        if 'set3' in sets and source == 'cs.cmu.edu':
            songs = sets['set3']
            
            if self.is_likely_encore(songs, 'set3'):
                # Convert set3 to encore
                sets['encore'] = songs
                del sets['set3']
                normalized['sets'] = sets
                self.merge_stats['set3_normalized'] += 1
                logger.debug(f"Normalized set3 → encore for {setlist.get('show_id', 'unknown')}")
        
        return normalized
    
    def resolve_conflict(self, cmu_show: Dict[str, Any], gdsets_show: Dict[str, Any], show_id: str) -> Dict[str, Any]:
        """
        Resolve conflicts between CMU and GDSets data for the same show
        
        Args:
            cmu_show: CMU setlist data
            gdsets_show: GDSets setlist data  
            show_id: Show identifier
            
        Returns:
            Resolved setlist entry
        """
        logger.warning(f"Conflict detected for {show_id}: both CMU and GDSets have data")
        self.merge_stats['conflicts_resolved'] += 1
        self.merge_stats['overlapping_dates'].append(show_id)
        
        # CMU takes precedence for 1972+ (its primary coverage area)
        try:
            year = int(show_id.split('-')[0])
            if year >= 1972:
                logger.info(f"Using CMU data for {show_id} (year {year} >= 1972)")
                resolved = cmu_show.copy()
                # Preserve GDSets source_url if available
                if 'source_url' in gdsets_show and 'source_url' not in resolved:
                    resolved['source_url'] = gdsets_show['source_url']
                return resolved
            else:
                logger.info(f"Using GDSets data for {show_id} (year {year} < 1972)")
                resolved = gdsets_show.copy()
                # Preserve CMU raw_content if available
                if 'raw_content' in cmu_show and 'raw_content' not in resolved:
                    resolved['raw_content'] = cmu_show['raw_content']
                return resolved
        except (ValueError, IndexError):
            # Fallback: prefer CMU for malformed date
            logger.warning(f"Malformed date {show_id}, defaulting to CMU")
            return cmu_show.copy()
    
    def merge_setlists(self, cmu_data: Dict[str, Any], gdsets_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Merge CMU and GDSets setlist data
        
        Args:
            cmu_data: CMU setlist data
            gdsets_data: GDSets setlist data
            
        Returns:
            Merged setlist dictionary
        """
        logger.info("Starting setlist merge process")
        start_time = datetime.now()
        
        # Initialize merged structure
        merged = {
            'metadata': self.merge_metadata(cmu_data.get('metadata', {}), gdsets_data.get('metadata', {})),
            'progress': self.merge_progress(cmu_data.get('progress', {}), gdsets_data.get('progress', {})),
            'setlists': {}
        }
        
        # Track statistics
        self.merge_stats['cmu_shows'] = len(cmu_data.get('setlists', {}))
        self.merge_stats['gdsets_shows'] = len(gdsets_data.get('setlists', {}))
        
        # Process CMU setlists with normalization
        logger.info(f"Processing {self.merge_stats['cmu_shows']} CMU shows")
        for show_id, setlist in cmu_data.get('setlists', {}).items():
            normalized = self.normalize_sets(setlist, 'cs.cmu.edu')
            merged['setlists'][show_id] = normalized
            
            # Track year coverage
            try:
                year = int(show_id.split('-')[0])
                self.merge_stats['year_coverage'][year] = self.merge_stats['year_coverage'].get(year, 0) + 1
            except (ValueError, IndexError):
                pass
        
        # Process GDSets setlists
        logger.info(f"Processing {self.merge_stats['gdsets_shows']} GDSets shows")
        for show_id, setlist in gdsets_data.get('setlists', {}).items():
            if show_id in merged['setlists']:
                # Conflict: both sources have this show
                cmu_show = merged['setlists'][show_id]
                resolved = self.resolve_conflict(cmu_show, setlist, show_id)
                merged['setlists'][show_id] = resolved
            else:
                # No conflict: use GDSets data
                normalized = self.normalize_sets(setlist, 'gdsets.com')
                merged['setlists'][show_id] = normalized
                
                # Track year coverage
                try:
                    year = int(show_id.split('-')[0])
                    self.merge_stats['year_coverage'][year] = self.merge_stats['year_coverage'].get(year, 0) + 1
                except (ValueError, IndexError):
                    pass
        
        # Final statistics
        self.merge_stats['total_merged'] = len(merged['setlists'])
        duration = (datetime.now() - start_time).total_seconds()
        
        # Update merged metadata with merge info
        merged['metadata'].update({
            'merged_at': datetime.now().isoformat(),
            'merge_duration_seconds': duration,
            'total_shows': self.merge_stats['total_merged'],
            'year_range': f"{min(self.merge_stats['year_coverage'].keys())}-{max(self.merge_stats['year_coverage'].keys())}" if self.merge_stats['year_coverage'] else "unknown",
            'merger_version': '1.0.0',
            'merge_stats': self.merge_stats
        })
        
        logger.info(f"Merge completed: {self.merge_stats['total_merged']} total shows in {duration:.1f} seconds")
        return merged
    
    def merge_metadata(self, cmu_meta: Dict[str, Any], gdsets_meta: Dict[str, Any]) -> Dict[str, Any]:
        """
        Merge metadata from both sources
        
        Args:
            cmu_meta: CMU metadata
            gdsets_meta: GDSets metadata
            
        Returns:
            Merged metadata dictionary
        """
        merged_meta = {
            'sources': [
                {
                    'name': 'CMU Setlist Archive',
                    'url': cmu_meta.get('source', 'cs.cmu.edu/~mleone/gdead/setlists.html'),
                    'coverage': '1972-1995',
                    'shows': cmu_meta.get('total_shows', 0),
                    'scraped_at': cmu_meta.get('scraped_at'),
                    'scraper_version': cmu_meta.get('scraper_version')
                },
                {
                    'name': 'GDSets Early Years',
                    'url': gdsets_meta.get('source', 'gdsets.com'),
                    'coverage': gdsets_meta.get('focus_years', '1965-1971'),
                    'shows': gdsets_meta.get('total_setlists', 0),
                    'scraped_at': gdsets_meta.get('scraped_at'),
                    'scraper_version': gdsets_meta.get('scraper_version')
                }
            ],
            'description': 'Merged Grateful Dead setlist data from CMU (1972-1995) and GDSets (1965-1971)',
            'data_type': 'merged_setlists'
        }
        
        return merged_meta
    
    def merge_progress(self, cmu_progress: Dict[str, Any], gdsets_progress: Dict[str, Any]) -> Dict[str, Any]:
        """
        Merge progress information from both sources
        
        Args:
            cmu_progress: CMU progress data
            gdsets_progress: GDSets progress data
            
        Returns:
            Merged progress dictionary
        """
        merged_progress = {
            'cmu_progress': cmu_progress,
            'gdsets_progress': gdsets_progress,
            'merge_process': {
                'started_at': datetime.now().isoformat(),
                'completed_at': None  # Will be updated when merge completes
            }
        }
        
        # Combine errors from both sources
        all_errors = []
        all_errors.extend(cmu_progress.get('errors', []))
        all_errors.extend(gdsets_progress.get('errors', []))
        
        if all_errors:
            merged_progress['combined_errors'] = all_errors
        
        return merged_progress
    
    def validate_merged_data(self, merged: Dict[str, Any]) -> bool:
        """
        Validate the merged setlist data
        
        Args:
            merged: Merged setlist dictionary
            
        Returns:
            True if validation passes
        """
        try:
            # Check required structure
            required_keys = ['metadata', 'setlists']
            for key in required_keys:
                if key not in merged:
                    raise ValueError(f"Missing required key: {key}")
            
            # Validate setlist entries
            setlists = merged['setlists']
            if not isinstance(setlists, dict):
                raise ValueError("setlists must be a dictionary")
            
            # Sample validation of setlist entries
            sample_size = min(10, len(setlists))
            sample_keys = list(setlists.keys())[:sample_size]
            
            for show_id in sample_keys:
                setlist = setlists[show_id]
                required_fields = ['show_id', 'sets', 'source']
                
                for field in required_fields:
                    if field not in setlist:
                        logger.warning(f"Show {show_id} missing field: {field}")
                
                # Validate sets structure
                sets = setlist.get('sets', {})
                if not isinstance(sets, dict):
                    logger.warning(f"Show {show_id} has invalid sets structure")
                
                for set_name, songs in sets.items():
                    if not isinstance(songs, list):
                        logger.warning(f"Show {show_id} set {set_name} is not a list")
            
            logger.info("Merged data validation passed")
            return True
            
        except Exception as e:
            logger.error(f"Validation failed: {e}")
            return False
    
    def save_merged_data(self, merged: Dict[str, Any], output_path: str) -> None:
        """
        Save merged setlist data to JSON file
        
        Args:
            merged: Merged setlist data
            output_path: Output file path
        """
        try:
            output_file = Path(output_path)
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(merged, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Merged setlist data saved to {output_path}")
            
            # Log file size
            file_size = output_file.stat().st_size
            logger.info(f"Output file size: {file_size / (1024*1024):.1f} MB")
            
        except Exception as e:
            logger.error(f"Failed to save merged data: {e}")
            raise
    
    def print_merge_summary(self) -> None:
        """Print a summary of the merge process"""
        print("\n" + "="*50)
        print("SETLIST MERGE SUMMARY")
        print("="*50)
        print(f"CMU shows:              {self.merge_stats['cmu_shows']:,}")
        print(f"GDSets shows:           {self.merge_stats['gdsets_shows']:,}")
        print(f"Total merged shows:     {self.merge_stats['total_merged']:,}")
        print(f"Conflicts resolved:     {self.merge_stats['conflicts_resolved']}")
        print(f"Set3 → Encore normalized: {self.merge_stats['set3_normalized']}")
        
        if self.merge_stats['overlapping_dates']:
            print(f"\nOverlapping dates: {', '.join(self.merge_stats['overlapping_dates'][:5])}")
            if len(self.merge_stats['overlapping_dates']) > 5:
                print(f"  ... and {len(self.merge_stats['overlapping_dates']) - 5} more")
        
        # Year coverage summary
        if self.merge_stats['year_coverage']:
            years = sorted(self.merge_stats['year_coverage'].keys())
            min_year, max_year = min(years), max(years)
            print(f"\nYear coverage: {min_year}-{max_year}")
            print(f"Years with shows: {len(years)}")
            
            # Show coverage gaps
            all_years = set(range(min_year, max_year + 1))
            missing_years = sorted(all_years - set(years))
            if missing_years:
                print(f"Missing years: {', '.join(map(str, missing_years))}")
        
        if self.merge_stats['errors']:
            print(f"\nErrors encountered: {len(self.merge_stats['errors'])}")
            print("Check merge_setlists.log for details")
        
        print("="*50)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Merge CMU and GDSets setlist data')
    parser.add_argument('--cmu', required=True,
                        help='Path to CMU setlist JSON file')
    parser.add_argument('--gdsets', required=True,
                        help='Path to GDSets setlist JSON file')
    parser.add_argument('--output', '-o', required=True,
                        help='Output path for merged setlist JSON')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize merger
    merger = SetlistMerger()
    
    try:
        # Load source files
        logger.info("Loading setlist data files...")
        cmu_data = merger.load_setlist_file(args.cmu)
        gdsets_data = merger.load_setlist_file(args.gdsets)
        
        # Perform merge
        merged_data = merger.merge_setlists(cmu_data, gdsets_data)
        
        # Validate merged data
        if not merger.validate_merged_data(merged_data):
            logger.error("Merged data validation failed")
            sys.exit(1)
        
        # Save merged data
        merger.save_merged_data(merged_data, args.output)
        
        # Print summary
        merger.print_merge_summary()
        
        logger.info("Setlist merge completed successfully")
        
    except KeyboardInterrupt:
        logger.info("Merge interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Merge failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()