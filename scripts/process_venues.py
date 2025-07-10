#!/usr/bin/env python3
"""
Venue Processor for Grateful Dead Archive

This script processes venue information from raw setlist data to create a normalized venues database.
It follows Stage 2.1 of the implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Extracts venue, city, and state/country information from setlist venue_lines
- Normalizes venue names to handle variations and typos
- Creates unique venue IDs for consistent referencing
- Generates geographical mappings and venue metadata
- Handles international venues with proper country codes
- Produces clean venues.json for app integration

Key Features:
- Smart venue name normalization (e.g., "Fillmore West" vs "The Fillmore West")
- City/state/country parsing with error handling
- Duplicate venue detection and merging
- Venue statistics (show counts, date ranges)
- Geographical data preparation for mapping features

Usage:
    python scripts/process_venues.py --input scripts/metadata/setlists/raw_setlists.json --output scripts/metadata/venues/venues.json
    python scripts/process_venues.py --input raw_setlists.json --output venues.json --verbose
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
        logging.FileHandler('process_venues.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class VenueProcessor:
    """Processes and normalizes venue data from raw setlists"""
    
    def __init__(self):
        """Initialize the venue processor"""
        self.venues = {}
        self.venue_aliases = {}
        self.processing_stats = {
            'total_shows': 0,
            'venues_found': 0,
            'venues_normalized': 0,
            'parsing_errors': 0,
            'duplicate_merges': 0,
            'start_time': datetime.now().isoformat()
        }
        
        # Common venue name variations to normalize
        self.venue_normalizations = {
            # Remove common prefixes/suffixes
            r'^the\s+': '',
            r'\s+the$': '',
            r'^\s*': '',
            r'\s*$': '',
            
            # Standardize common terms
            r'\bst\b\.?': 'street',
            r'\bave\b\.?': 'avenue', 
            r'\bdr\b\.?': 'drive',
            r'\bblvd\b\.?': 'boulevard',
            r'\brd\b\.?': 'road',
            r'\bmt\b\.?': 'mount',
            r'\buniv\b\.?': 'university',
            r'\bu\s+of\s+': 'university of ',
            r'\bctr\b\.?': 'center',
            r'\bcenter\b': 'center',
            r'\bcentre\b': 'center',
            r'\baud\b\.?': 'auditorium',
            r'\btheatre\b': 'theater',
            r'\bcoliseum\b': 'coliseum',
            r'\bstadium\b': 'stadium',
            r'\barena\b': 'arena',
            r'\bhall\b': 'hall',
            r'\bballroom\b': 'ballroom',
            
            # Handle common typos and variations
            r'winteriand': 'winterland',  # Common typo
            r'fillmore\s*west': 'fillmore west',
            r'fillmore\s*east': 'fillmore east',
        }
        
        # State abbreviations mapping
        self.state_abbreviations = {
            'AL': 'Alabama', 'AK': 'Alaska', 'AZ': 'Arizona', 'AR': 'Arkansas',
            'CA': 'California', 'CO': 'Colorado', 'CT': 'Connecticut', 'DE': 'Delaware',
            'FL': 'Florida', 'GA': 'Georgia', 'HI': 'Hawaii', 'ID': 'Idaho',
            'IL': 'Illinois', 'IN': 'Indiana', 'IA': 'Iowa', 'KS': 'Kansas',
            'KY': 'Kentucky', 'LA': 'Louisiana', 'ME': 'Maine', 'MD': 'Maryland',
            'MA': 'Massachusetts', 'MI': 'Michigan', 'MN': 'Minnesota', 'MS': 'Mississippi',
            'MO': 'Missouri', 'MT': 'Montana', 'NE': 'Nebraska', 'NV': 'Nevada',
            'NH': 'New Hampshire', 'NJ': 'New Jersey', 'NM': 'New Mexico', 'NY': 'New York',
            'NC': 'North Carolina', 'ND': 'North Dakota', 'OH': 'Ohio', 'OK': 'Oklahoma',
            'OR': 'Oregon', 'PA': 'Pennsylvania', 'RI': 'Rhode Island', 'SC': 'South Carolina',
            'SD': 'South Dakota', 'TN': 'Tennessee', 'TX': 'Texas', 'UT': 'Utah',
            'VT': 'Vermont', 'VA': 'Virginia', 'WA': 'Washington', 'WV': 'West Virginia',
            'WI': 'Wisconsin', 'WY': 'Wyoming', 'DC': 'District of Columbia'
        }
        
        # Country codes for international venues
        self.country_codes = {
            'XENG': 'England',
            'XE': 'England', 
            'XDEN': 'Denmark',
            'XGER': 'Germany',
            'XWGER': 'West Germany',
            'XG': 'Germany',
            'XFRA': 'France',
            'XF': 'France',
            'XCAN': 'Canada',
            'XCON': 'Canada',
            'XCBC': 'Canada',
            'XH': 'Netherlands',
            'XHOL': 'Netherlands',
            'XNETH': 'Netherlands',
            'XNETHER': 'Netherlands',
            'XS': 'Sweden',
            'XSWE': 'Sweden',
            'XNOR': 'Norway',
            'XBEL': 'Belgium',
            'XSWI': 'Switzerland',
            'XITA': 'Italy',
            'XAUS': 'Australia',
            'XLUX': 'Luxembourg',
            'ON': 'Ontario, Canada',
            'QC': 'Quebec, Canada',
            'BC': 'British Columbia, Canada'
        }
        
        # Special city-based country mappings for venues without proper country codes
        self.city_country_mappings = {
            'France': 'France',  # City listed as "France" 
            'Giza': 'Egypt',
            'Copenhagen': 'Denmark',
            'Barcelona': 'Spain', 
            'Paris': 'France',
            'Calgary': 'Canada',
            'Montego Bay': 'Jamaica'
        }
    
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
    
    def parse_venue_line(self, venue_line: str) -> Tuple[str, str, str, str]:
        """
        Parse venue line into components
        
        Args:
            venue_line: Raw venue line from setlist
            
        Returns:
            Tuple of (venue_name, city, state_or_country, raw_date)
        """
        if not venue_line or not venue_line.strip():
            return "", "", "", ""
        
        venue_line = venue_line.strip()
        
        # Remove date information in parentheses at the end
        date_match = re.search(r'\s*\(([^)]+)\)\s*$', venue_line)
        raw_date = date_match.group(1) if date_match else ""
        if date_match:
            venue_line = venue_line[:date_match.start()].strip()
        
        # Split on comma to get venue, city, state/country
        parts = [part.strip() for part in venue_line.split(',')]
        
        if len(parts) >= 3:
            # Standard format: Venue, City, State/Country
            venue_name = parts[0]
            city = parts[1]
            state_or_country = parts[2]
        elif len(parts) == 2:
            # Format: Venue, City or City, State/Country
            if any(code in parts[1] for code in self.state_abbreviations.keys()) or \
               any(code in parts[1] for code in self.country_codes.keys()):
                # Assuming City, State/Country
                venue_name = ""
                city = parts[0]
                state_or_country = parts[1]
            else:
                # Assuming Venue, City
                venue_name = parts[0]
                city = parts[1]
                state_or_country = ""
        elif len(parts) == 1:
            # Just venue name
            venue_name = parts[0]
            city = ""
            state_or_country = ""
        else:
            # Empty or malformed
            venue_name = venue_line  # Use whole line as venue
            city = ""
            state_or_country = ""
        
        return venue_name, city, state_or_country, raw_date
    
    def normalize_venue_name(self, venue_name: str) -> str:
        """
        Normalize venue name for consistent matching
        
        Args:
            venue_name: Raw venue name
            
        Returns:
            Normalized venue name
        """
        if not venue_name:
            return ""
        
        normalized = venue_name.lower().strip()
        
        # Apply normalization rules
        for pattern, replacement in self.venue_normalizations.items():
            normalized = re.sub(pattern, replacement, normalized, flags=re.IGNORECASE)
        
        # Clean up extra whitespace
        normalized = re.sub(r'\s+', ' ', normalized).strip()
        
        return normalized
    
    def resolve_location(self, city: str, state_or_country: str) -> Tuple[str, str, str]:
        """
        Resolve and normalize location information
        
        Args:
            city: City name
            state_or_country: State abbreviation or country code
            
        Returns:
            Tuple of (normalized_city, state_name, country)
        """
        city = city.strip() if city else ""
        state_or_country = state_or_country.strip() if state_or_country else ""
        
        # Default to US
        country = "United States"
        state_name = ""
        
        # Check if it's a US state abbreviation
        if state_or_country in self.state_abbreviations:
            state_name = self.state_abbreviations[state_or_country]
        # Check if it's an international venue
        elif state_or_country in self.country_codes:
            country = self.country_codes[state_or_country]
            state_name = ""
        # Check for city-based country mappings (venues without proper country codes)
        elif city in self.city_country_mappings:
            country = self.city_country_mappings[city]
            state_name = ""
        # Check for other international patterns
        elif state_or_country and len(state_or_country) > 2:
            # Might be a full country/province name
            if any(keyword in state_or_country.lower() for keyword in ['canada', 'england', 'germany', 'france']):
                country = state_or_country
                state_name = ""
            # Check if it's a full US state name
            elif state_or_country in {v for v in self.state_abbreviations.values()}:
                state_name = state_or_country
            else:
                # Invalid state data - ignore it, keep as US with empty state
                state_name = ""
        
        return city, state_name, country
    
    def clean_state_field(self, state_name: str) -> str:
        """
        Clean and validate state field - only valid US states allowed
        
        Args:
            state_name: Raw state name
            
        Returns:
            Valid US state name or empty string
        """
        if not state_name:
            return ""
        
        # Valid US states (full names)
        valid_us_states = {
            'Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware',
            'Florida', 'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas',
            'Kentucky', 'Louisiana', 'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi',
            'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire', 'New Jersey', 'New Mexico', 'New York',
            'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina',
            'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont', 'Virginia', 'Washington', 'West Virginia',
            'Wisconsin', 'Wyoming', 'District of Columbia'
        }
        
        # Return state only if it's valid, otherwise empty string
        return state_name if state_name in valid_us_states else ""
    
    def generate_venue_id(self, venue_name: str, city: str, state: str, country: str) -> str:
        """
        Generate unique venue ID
        
        Args:
            venue_name: Normalized venue name
            city: City name
            state: State name
            country: Country name
            
        Returns:
            Unique venue ID
        """
        # Create a unique string for hashing
        venue_key = f"{venue_name}|{city}|{state}|{country}".lower()
        
        # Generate short hash
        venue_hash = hashlib.md5(venue_key.encode()).hexdigest()[:8]
        
        # Create readable ID
        venue_id_parts = []
        if venue_name:
            # Take first few significant words from venue name
            words = re.findall(r'\b\w+\b', venue_name.lower())
            significant_words = [w for w in words[:3] if len(w) > 2]  # Skip short words
            if significant_words:
                venue_id_parts.extend(significant_words)
        
        if city:
            venue_id_parts.append(city.lower().replace(' ', ''))
        
        if len(venue_id_parts) == 0:
            venue_id_parts.append('unknown')
        
        # Create base ID
        base_id = '-'.join(venue_id_parts)
        # Add hash for uniqueness
        venue_id = f"{base_id}-{venue_hash}"
        
        # Clean up the ID
        venue_id = re.sub(r'[^a-z0-9\-]', '', venue_id)
        venue_id = re.sub(r'\-+', '-', venue_id).strip('-')
        
        return venue_id
    
    def process_venues(self, setlists_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process all venues from setlist data
        
        Args:
            setlists_data: Raw setlist data
            
        Returns:
            Processed venues data
        """
        logger.info("Starting venue processing")
        start_time = datetime.now()
        
        # Track venue occurrences for statistics
        venue_occurrences = defaultdict(list)
        raw_venue_lines = []
        
        # Process each show
        for show_id, setlist in setlists_data['setlists'].items():
            self.processing_stats['total_shows'] += 1
            
            venue_line = setlist.get('venue_line', '').strip()
            if not venue_line:
                logger.warning(f"No venue line for show {show_id}")
                continue
            
            raw_venue_lines.append(venue_line)
            
            try:
                # Parse venue components
                venue_name, city, state_country, raw_date = self.parse_venue_line(venue_line)
                
                # Normalize venue name
                normalized_venue = self.normalize_venue_name(venue_name)
                
                # Resolve location
                resolved_city, state_name, country = self.resolve_location(city, state_country)
                
                # Generate venue ID
                venue_id = self.generate_venue_id(normalized_venue, resolved_city, state_name, country)
                
                # Track this venue occurrence
                venue_occurrences[venue_id].append({
                    'show_id': show_id,
                    'raw_venue_line': venue_line,
                    'venue_name': venue_name,
                    'normalized_venue': normalized_venue,
                    'city': resolved_city,
                    'state': state_name,
                    'country': country
                })
                
            except Exception as e:
                logger.error(f"Failed to process venue for {show_id}: {venue_line} - {e}")
                self.processing_stats['parsing_errors'] += 1
        
        # Build venue database
        venues_db = {}
        
        for venue_id, occurrences in venue_occurrences.items():
            # Use the most common version of each field
            venue_names = [occ['venue_name'] for occ in occurrences if occ['venue_name']]
            cities = [occ['city'] for occ in occurrences if occ['city']]
            states = [occ['state'] for occ in occurrences if occ['state']]
            countries = [occ['country'] for occ in occurrences if occ['country']]
            
            # Get most common values
            most_common_venue = Counter(venue_names).most_common(1)[0][0] if venue_names else ""
            most_common_city = Counter(cities).most_common(1)[0][0] if cities else ""
            raw_state = Counter(states).most_common(1)[0][0] if states else ""
            most_common_country = Counter(countries).most_common(1)[0][0] if countries else "United States"
            
            # Clean state field - only valid US states allowed
            most_common_state = self.clean_state_field(raw_state)
            
            # Get show date range
            show_ids = [occ['show_id'] for occ in occurrences]
            show_dates = []
            for show_id in show_ids:
                try:
                    show_date = datetime.strptime(show_id, '%Y-%m-%d')
                    show_dates.append(show_date)
                except ValueError:
                    pass
            
            first_show = min(show_dates).strftime('%Y-%m-%d') if show_dates else ""
            last_show = max(show_dates).strftime('%Y-%m-%d') if show_dates else ""
            
            # Create venue entry
            venues_db[venue_id] = {
                'venue_id': venue_id,
                'name': most_common_venue,
                'city': most_common_city,
                'state': most_common_state,
                'country': most_common_country,
                'show_count': len(occurrences),
                'first_show': first_show,
                'last_show': last_show,
                'show_ids': sorted(show_ids),
                'aliases': list(set(venue_names)),  # All name variations seen
                'raw_venue_lines': list(set(occ['raw_venue_line'] for occ in occurrences))
            }
        
        # Update statistics
        self.processing_stats['venues_found'] = len(venues_db)
        self.processing_stats['venues_normalized'] = len(venue_occurrences)
        
        duration = (datetime.now() - start_time).total_seconds()
        
        logger.info(f"Venue processing completed: {len(venues_db)} unique venues in {duration:.1f} seconds")
        
        return {
            'metadata': {
                'source': 'processed from raw_setlists.json',
                'processed_at': datetime.now().isoformat(),
                'processing_duration_seconds': duration,
                'total_venues': len(venues_db),
                'total_shows_processed': self.processing_stats['total_shows'],
                'processing_stats': self.processing_stats,
                'processor_version': '1.0.0'
            },
            'venues': venues_db
        }
    
    def save_venues(self, venues_data: Dict[str, Any], output_path: str) -> None:
        """
        Save processed venues data
        
        Args:
            venues_data: Processed venues data
            output_path: Output file path
        """
        try:
            output_file = Path(output_path)
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(venues_data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Venues data saved to {output_path}")
            
            # Log file size
            file_size = output_file.stat().st_size
            logger.info(f"Output file size: {file_size / 1024:.1f} KB")
            
        except Exception as e:
            logger.error(f"Failed to save venues data: {e}")
            raise
    
    def print_venue_summary(self, venues_data: Dict[str, Any]) -> None:
        """Print summary of venue processing"""
        venues = venues_data.get('venues', {})
        metadata = venues_data.get('metadata', {})
        stats = metadata.get('processing_stats', {})
        
        print("\n" + "="*60)
        print("VENUE PROCESSING SUMMARY")
        print("="*60)
        print(f"Total shows processed:    {stats.get('total_shows', 0):,}")
        print(f"Total unique venues:      {metadata.get('total_venues', 0):,}")
        print(f"Parsing errors:           {stats.get('parsing_errors', 0)}")
        print(f"Processing time:          {metadata.get('processing_duration_seconds', 0):.1f}s")
        
        if venues:
            # Show venue statistics
            show_counts = [v['show_count'] for v in venues.values()]
            most_shows = max(show_counts) if show_counts else 0
            avg_shows = sum(show_counts) / len(show_counts) if show_counts else 0
            
            print(f"\nVenue Statistics:")
            print(f"Most shows at one venue:  {most_shows}")
            print(f"Average shows per venue:  {avg_shows:.1f}")
            
            # Show top venues
            top_venues = sorted(venues.values(), key=lambda x: x['show_count'], reverse=True)[:10]
            print(f"\nTop 10 Venues by Show Count:")
            for i, venue in enumerate(top_venues, 1):
                name = venue['name'] or 'Unknown'
                city = venue['city']
                state = venue['state']
                location = f"{city}, {state}" if city and state else city or state or venue['country']
                print(f"  {i:2d}. {name} ({location}) - {venue['show_count']} shows")
        
        print("="*60)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Process venues from raw setlist data')
    parser.add_argument('--input', '-i', required=True,
                        help='Input raw setlists JSON file')
    parser.add_argument('--output', '-o', required=True,
                        help='Output venues JSON file')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize processor
    processor = VenueProcessor()
    
    try:
        # Load setlist data
        setlists_data = processor.load_setlists(args.input)
        
        # Process venues
        venues_data = processor.process_venues(setlists_data)
        
        # Save results
        processor.save_venues(venues_data, args.output)
        
        # Print summary
        processor.print_venue_summary(venues_data)
        
        logger.info("Venue processing completed successfully")
        
    except KeyboardInterrupt:
        logger.info("Processing interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Processing failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()