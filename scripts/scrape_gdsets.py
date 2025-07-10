#!/usr/bin/env python3
"""
GDSets Scraper for Grateful Dead Archive

This script extracts setlist and image data from locally saved GDSets.com HTML with focus on early years (1965-1971).
It follows the implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Parses setlists primarily for early years (1965-1971) to complement CMU data
- Captures image references across all years:
  - Show posters and advertisements
  - Tickets and backstage passes  
  - Programs and memorabilia
  - Venue photos where available
- Adaptive parsing for less structured early setlist data
- Separate JSON outputs for setlists and images
- Preserves source attribution and metadata

Usage:
    python scripts/scrape_gdsets.py --html-file scripts/metadata/sources/gdsets/index.html --output-setlists scripts/metadata/setlists/gdsets_setlists.json --output-images scripts/metadata/images/gdsets_images.json
    python scripts/scrape_gdsets.py --html-file scripts/metadata/sources/gdsets/index.html --focus-years 1965-1971 --output-setlists gdsets_early.json --output-images gdsets_images.json
    python scripts/scrape_gdsets.py --html-file scripts/metadata/sources/gdsets/index.html --images-only --output-images gdsets_images.json
"""

import argparse
import json
import logging
import re
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
from urllib.parse import urljoin
import sys
from bs4 import BeautifulSoup


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('gdsets_scraper.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class GDSetsScraper:
    """Parser for GDSets.com Grateful Dead section HTML"""
    
    BASE_URL = "https://gdsets.com/"
    
    def __init__(self, html_file: str, focus_years: Optional[Tuple[int, int]] = None, images_only: bool = False):
        """
        Initialize the parser
        
        Args:
            html_file: Path to local HTML file with GDSets content
            focus_years: Optional tuple of (start_year, end_year) to prioritize
            images_only: If True, only extract images, skip setlists
        """
        self.html_file = html_file
        self.focus_years = focus_years or (1965, 1971)  # Default focus on early years
        self.images_only = images_only
        
        # Progress tracking
        self.progress = {
            'setlists_found': 0,
            'images_found': 0,
            'start_time': datetime.now().isoformat(),
            'errors': []
        }
        
        # Results storage
        self.setlists = {}
        self.images = {}
        
    def read_html_file(self) -> Optional[str]:
        """
        Read HTML content from local file
        
        Returns:
            HTML content as string or None if failed
        """
        try:
            with open(self.html_file, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception as e:
            logger.error(f"Failed to read HTML file: {e}")
            self.progress['errors'].append({
                'file': self.html_file,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            })
            return None
    
    def extract_setlists_from_html(self, soup: BeautifulSoup) -> Dict[str, Dict[str, Any]]:
        """
        Extract setlist information from the HTML content
        
        Args:
            soup: BeautifulSoup object with parsed HTML content
            
        Returns:
            Dictionary of setlist data by show_id
        """
        setlists = {}
        
        # Find all event headline divs (show entries)
        event_divs = soup.find_all('div', class_='event-headline-div')
        
        for event_div in event_divs:
            try:
                # Get show date and venue
                if not event_div.span:
                    continue
                    
                headline_text = event_div.span.text.strip()
                date_match = re.search(r'(\d{1,2}/\d{1,2}/\d{2,4})\s*\[\w+\]\s*Grateful\s*Dead\s*-\s*(.+)', headline_text)
                if not date_match:
                    continue
                    
                date_text = date_match.group(1)
                venue_line = date_match.group(2).strip()
                show_id = self.parse_date_hint(date_text)
                
                if not show_id:
                    continue
                
                # Skip if not in focus years
                if self.focus_years:
                    try:
                        year = int(show_id.split('-')[0])
                        if not (self.focus_years[0] <= year <= self.focus_years[1]):
                            continue
                    except (ValueError, IndexError):
                        pass
                
                # HARD CUTOFF: No shows after Jerry's death (7/9/95)
                # The last Grateful Dead show was July 9, 1995
                try:
                    show_date = datetime.strptime(show_id, '%Y-%m-%d')
                    last_gd_show = datetime(1995, 7, 9)
                    if show_date > last_gd_show:
                        logger.warning(f"Skipping post-Jerry show: {show_id} (last GD show was 1995-07-09)")
                        continue
                except (ValueError, TypeError):
                    # If we can't parse the date, log it but don't skip
                    logger.debug(f"Could not parse date for validation: {show_id}")
                    pass
                
                # Find setlist content after this headline
                setlist_div = event_div.find_next('div', class_='setlists-div')
                if not setlist_div:
                    # If no setlist, create minimal entry with just venue info
                    setlists[show_id] = {
                        'show_id': show_id,
                        'venue_line': venue_line,
                        'sets': {},
                        'source_url': self.BASE_URL + 'grateful-dead.htm',
                        'source': 'gdsets.com',
                        'scraped_at': datetime.now().isoformat()
                    }
                    continue
                
                # Parse setlist sets
                sets = {}
                set_containers = setlist_div.find_all('div', class_='setlist-container-div')
                
                for container in set_containers:
                    # Find the header inside the container
                    set_header = container.find('div', class_='setlist-header-div')
                    if not set_header:
                        continue
                    
                    # Determine set name
                    set_text = set_header.text.strip()
                    if 'I:' in set_text and not 'II:' in set_text and not 'III:' in set_text:
                        set_name = 'set1'
                    elif 'II:' in set_text and not 'III:' in set_text:
                        set_name = 'set2'
                    elif 'III:' in set_text:
                        set_name = 'set3'
                    elif 'E:' in set_text:
                        set_name = 'encore'
                    else:
                        continue
                    
                    # Find song list inside the same container
                    songs_ul = container.find('ul', class_='setlist-songs-ul')
                    if not songs_ul:
                        continue
                        
                    # Extract songs
                    song_items = songs_ul.find_all('li')
                    songs = [song.text.strip().rstrip(',') for song in song_items if song.text.strip()]
                    
                    if songs:
                        sets[set_name] = songs
                
                # Save setlist
                if sets:
                    setlists[show_id] = {
                        'show_id': show_id,
                        'venue_line': venue_line,
                        'sets': sets,
                        'source_url': self.BASE_URL + 'grateful-dead.htm',
                        'source': 'gdsets.com',
                        'scraped_at': datetime.now().isoformat()
                    }
                    self.progress['setlists_found'] += 1
                    
            except Exception as e:
                logger.error(f"Failed to parse setlist: {e}")
                self.progress['errors'].append({
                    'show': headline_text if 'headline_text' in locals() else 'unknown',
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                })
        
        return setlists
    
    def parse_date_hint(self, date_hint: str) -> Optional[str]:
        """
        Parse various date formats into YYYY-MM-DD format
        
        Args:
            date_hint: Date string in various formats
            
        Returns:
            Standardized date string or None
        """
        # Common patterns: MM/DD/YY, MM/DD/YYYY, M/D/YY, etc.
        patterns = [
            r'(\d{1,2})/(\d{1,2})/(\d{4})',  # MM/DD/YYYY
            r'(\d{1,2})/(\d{1,2})/(\d{2})',  # MM/DD/YY
            r'(\d{4})-(\d{1,2})-(\d{1,2})',  # YYYY-MM-DD
            r'(\d{1,2})-(\d{1,2})-(\d{4})',  # MM-DD-YYYY
        ]
        
        for pattern in patterns:
            match = re.search(pattern, date_hint)
            if match:
                try:
                    if len(match.group(3)) == 2:  # Two-digit year
                        year = int(match.group(3))
                        year = 1900 + year if year >= 65 else 2000 + year
                        month, day = int(match.group(1)), int(match.group(2))
                    elif pattern.startswith(r'(\d{4})'):  # YYYY first
                        year, month, day = int(match.group(1)), int(match.group(2)), int(match.group(3))
                    else:  # MM/DD/YYYY or MM-DD-YYYY
                        month, day, year = int(match.group(1)), int(match.group(2)), int(match.group(3))
                    
                    return f"{year:04d}-{month:02d}-{day:02d}"
                except ValueError:
                    continue
        
        return None
    
    def extract_images_from_html(self, soup: BeautifulSoup) -> List[Dict[str, Any]]:
        """
        Extract image information from the HTML content
        
        Args:
            soup: BeautifulSoup object with parsed HTML content
            
        Returns:
            List of image metadata dictionaries
        """
        images = []
        found_urls = set()
        
        # Find all image collections in the HTML
        image_collections = soup.find_all('div', class_='image-collection')
        for collection in image_collections:
            # Find show_id from nearby elements
            event_div = collection.find_previous('div', class_='event-headline-div')
            show_id = None
            if event_div and event_div.span:
                date_text = event_div.span.text.strip()
                date_match = re.search(r'(\d{1,2}/\d{1,2}/\d{2,4})', date_text)
                if date_match:
                    show_id = self.parse_date_hint(date_match.group(1))
            
            # Process all thumbnails
            img_tags = collection.find_all('img', class_='thumb')
            for img in img_tags:
                img_url = img.get('src', '')
                if not img_url or img_url in found_urls:
                    continue
                found_urls.add(img_url)
                
                # Get image information
                img_type = self.classify_image(img_url, img.get('alt', '') or img.get('title', ''))
                
                # Extract image information
                image_info = {
                    'url': urljoin(self.BASE_URL, img_url),
                    'filename': img_url.split('/')[-1] if '/' in img_url else img_url,
                    'description': (img.get('alt', '') or img.get('title', '')).strip(),
                    'type': img_type,
                    'show_id': show_id,
                    'source_url': self.BASE_URL + 'grateful-dead.htm',
                    'scraped_at': datetime.now().isoformat()
                }
                
                images.append(image_info)
                self.progress['images_found'] += 1
        
        return images
    
    def classify_image(self, img_url: str, description: str) -> str:
        """
        Classify image type based on URL and description
        
        Args:
            img_url: Image URL
            description: Image description/alt text
            
        Returns:
            Image type classification
        """
        url_lower = img_url.lower()
        desc_lower = description.lower()
        
        # Check for specific types
        if any(term in url_lower or term in desc_lower for term in ['poster', 'flyer', 'handbill']):
            return 'poster'
        elif any(term in url_lower or term in desc_lower for term in ['ticket', 'stub']):
            return 'ticket'
        elif any(term in url_lower or term in desc_lower for term in ['backstage', 'pass']):
            return 'backstage_pass'
        elif any(term in url_lower or term in desc_lower for term in ['program', 'setlist']):
            return 'program'
        elif any(term in url_lower or term in desc_lower for term in ['venue', 'hall', 'theater']):
            return 'venue'
        elif any(term in url_lower or term in desc_lower for term in ['band', 'performance', 'stage']):
            return 'performance'
        else:
            return 'memorabilia'
    
    def parse_setlist_from_page(self, content: str, show_id: str, page_url: str) -> Optional[Dict[str, Any]]:
        """
        Parse setlist information from a show page
        
        Args:
            content: HTML page content
            show_id: Show identifier
            page_url: Source page URL
            
        Returns:
            Parsed setlist dictionary or None
        """
        try:
            # Extract venue information - look for common patterns
            venue_patterns = [
                r'<h[12][^>]*>([^<]*(?:Hall|Theater|Theatre|Center|Arena|Stadium|Club|Ballroom)[^<]*)</h[12]>',
                r'<b>([^<]*(?:Hall|Theater|Theatre|Center|Arena|Stadium|Club|Ballroom)[^<]*)</b>',
                r'<strong>([^<]*(?:Hall|Theater|Theatre|Center|Arena|Stadium|Club|Ballroom)[^<]*)</strong>',
            ]
            
            venue_line = ""
            for pattern in venue_patterns:
                match = re.search(pattern, content, re.IGNORECASE)
                if match:
                    venue_line = match.group(1).strip()
                    break
            
            # Look for setlist content - various patterns used by GDSets
            setlist_patterns = [
                r'(?i)set\s*1[:\s]*\n([^<]*(?:\n[^<]*)*?)(?=set\s*2|encore|$)',
                r'(?i)first\s*set[:\s]*\n([^<]*(?:\n[^<]*)*?)(?=second\s*set|set\s*2|encore|$)',
                r'<p[^>]*>([^<]*(?:<br[^>]*>[^<]*)*)</p>',  # Paragraph-based setlists
            ]
            
            sets = {}
            
            # Try to extract structured sets
            set1_pattern = r'(?i)(?:set\s*1|first\s*set)[:\s]*\n?([^\n]*(?:\n[^\n]*)*?)(?=(?:set\s*2|second\s*set|encore)|$)'
            set2_pattern = r'(?i)(?:set\s*2|second\s*set)[:\s]*\n?([^\n]*(?:\n[^\n]*)*?)(?=(?:set\s*3|third\s*set|encore)|$)'
            encore_pattern = r'(?i)encore[:\s]*\n?([^\n]*(?:\n[^\n]*)*?)(?=$)'
            
            for set_name, pattern in [('set1', set1_pattern), ('set2', set2_pattern), ('encore', encore_pattern)]:
                match = re.search(pattern, content, re.MULTILINE | re.DOTALL)
                if match:
                    set_content = match.group(1).strip()
                    # Parse individual songs
                    songs = self.parse_songs_from_text(set_content)
                    if songs:
                        sets[set_name] = songs
            
            # If no structured sets found, try to extract any song list
            if not sets:
                # Look for any list of songs
                song_list_patterns = [
                    r'<ol[^>]*>(.*?)</ol>',
                    r'<ul[^>]*>(.*?)</ul>',
                    r'<div[^>]*setlist[^>]*>(.*?)</div>',
                ]
                
                for pattern in song_list_patterns:
                    match = re.search(pattern, content, re.IGNORECASE | re.DOTALL)
                    if match:
                        list_content = match.group(1)
                        songs = self.parse_songs_from_html_list(list_content)
                        if songs:
                            sets['set1'] = songs
                            break
            
            # Return setlist if we found any content
            if sets or venue_line:
                return {
                    'show_id': show_id,
                    'venue_line': venue_line,
                    'sets': sets,
                    'source_url': page_url,
                    'source': 'gdsets.com',
                    'scraped_at': datetime.now().isoformat()
                }
            
            return None
            
        except Exception as e:
            logger.error(f"Failed to parse setlist for {show_id}: {e}")
            return None
    
    def parse_songs_from_text(self, text: str) -> List[str]:
        """
        Parse individual songs from text content
        
        Args:
            text: Raw text containing song list
            
        Returns:
            List of song names
        """
        songs = []
        
        # Split by newlines and clean up
        lines = text.split('\n')
        for line in lines:
            line = line.strip()
            if not line:
                continue
            
            # Remove HTML tags
            line = re.sub(r'<[^>]+>', '', line)
            
            # Skip lines that look like headers or separators
            if any(skip in line.lower() for skip in ['set 1', 'set 2', 'encore', '---', '===', 'song', 'title']):
                continue
            
            # Remove leading numbers/bullets
            line = re.sub(r'^\d+\.?\s*', '', line)
            line = re.sub(r'^[•\-\*]\s*', '', line)
            
            if line:
                songs.append(line.strip())
        
        return songs
    
    def parse_songs_from_html_list(self, html_content: str) -> List[str]:
        """
        Parse songs from HTML list elements
        
        Args:
            html_content: HTML containing <li> elements
            
        Returns:
            List of song names
        """
        songs = []
        
        # Extract <li> content
        li_pattern = r'<li[^>]*>([^<]*(?:<[^>]*>[^<]*)*)</li>'
        matches = re.findall(li_pattern, html_content, re.IGNORECASE)
        
        for match in matches:
            # Clean up HTML
            song = re.sub(r'<[^>]+>', '', match)
            song = song.strip()
            if song:
                songs.append(song)
        
        return songs
    
    
    def parse_all(self) -> Dict[str, Any]:
        """
        Parse all content from the HTML file
        
        Returns:
            Dictionary containing parsed data and metadata
        """
        logger.info("Starting GDSets parsing from HTML file")
        start_time = datetime.now()
        
        # Read the HTML file
        html_content = self.read_html_file()
        if not html_content:
            logger.error("Failed to read HTML file")
            return {}
        
        # Parse HTML with BeautifulSoup
        soup = BeautifulSoup(html_content, 'html.parser')
        
        # Extract setlists if not images-only mode
        if not self.images_only:
            self.setlists = self.extract_setlists_from_html(soup)
        
        # Extract images
        image_data = self.extract_images_from_html(soup)
        for img in image_data:
            img_key = f"{img['show_id']}_{img['filename']}" if img['show_id'] else img['filename']
            self.images[img_key] = img
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info(f"Parsing completed: {len(self.setlists)} setlists, {len(self.images)} images in {duration:.1f} seconds")
        
        return {
            'setlists': self.setlists,
            'images': self.images,
            'metadata': {
                'source': 'gdsets.com',
                'source_file': self.html_file,
                'parsed_at': end_time.isoformat(),
                'duration_seconds': duration,
                'total_setlists': len(self.setlists),
                'total_images': len(self.images),
                'focus_years': f"{self.focus_years[0]}-{self.focus_years[1]}" if self.focus_years else "all",
                'parser_version': '1.1.0'
            },
            'progress': self.progress
        }
    
    def save_results(self, setlists_path: Optional[str], images_path: Optional[str], results: Dict[str, Any]) -> None:
        """
        Save results to separate JSON files
        
        Args:
            setlists_path: Path to save setlist data (optional)
            images_path: Path to save image data (optional)
            results: Results dictionary to save
        """
        if setlists_path and results.get('setlists'):
            setlists_output = {
                'metadata': results['metadata'].copy(),
                'progress': results['progress'],
                'setlists': results['setlists']
            }
            setlists_output['metadata']['data_type'] = 'setlists'
            
            setlists_file = Path(setlists_path)
            setlists_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(setlists_file, 'w', encoding='utf-8') as f:
                json.dump(setlists_output, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Setlist data saved to {setlists_path}")
        
        if images_path and results.get('images'):
            images_output = {
                'metadata': results['metadata'].copy(),
                'progress': results['progress'],
                'images': results['images']
            }
            images_output['metadata']['data_type'] = 'images'
            
            images_file = Path(images_path)
            images_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(images_file, 'w', encoding='utf-8') as f:
                json.dump(images_output, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Image data saved to {images_path}")


def parse_year_range(year_range_str: str) -> Tuple[int, int]:
    """Parse year range string like '1965-1971' into tuple"""
    if '-' in year_range_str:
        start, end = year_range_str.split('-')
        return int(start), int(end)
    else:
        year = int(year_range_str)
        return year, year


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Parse GDSets.com Grateful Dead HTML data')
    parser.add_argument('--html-file', '-f', required=True,
                        help='Path to HTML file containing GDSets content')
    parser.add_argument('--output-setlists', '-s', 
                        help='Output JSON file for setlist data')
    parser.add_argument('--output-images', '-i',
                        help='Output JSON file for image data')
    parser.add_argument('--focus-years', '-y', type=str, default='1965-1971',
                        help='Year range to prioritize (e.g., "1965-1971", default: 1965-1971)')
    parser.add_argument('--images-only', action='store_true',
                        help='Only extract images, skip setlist parsing')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    if not args.output_setlists and not args.output_images:
        logger.error("Must specify at least one output file (--output-setlists or --output-images)")
        sys.exit(1)
    
    # Parse focus years
    focus_years = None
    if args.focus_years:
        try:
            focus_years = parse_year_range(args.focus_years)
            logger.info(f"Focusing on years {focus_years[0]}-{focus_years[1]}")
        except ValueError as e:
            logger.error(f"Invalid focus years format: {e}")
            sys.exit(1)
    
    # Initialize parser
    scraper = GDSetsScraper(html_file=args.html_file, focus_years=focus_years, images_only=args.images_only)
    
    try:
        # Parse all content
        results = scraper.parse_all()
        
        if not results.get('setlists') and not results.get('images'):
            logger.error("No data was extracted")
            sys.exit(1)
        
        # Save results
        scraper.save_results(args.output_setlists, args.output_images, results)
        
        # Print summary
        metadata = results.get('metadata', {})
        print(f"\nParsing Summary:")
        print(f"Total setlists: {metadata.get('total_setlists', 0)}")
        print(f"Total images: {metadata.get('total_images', 0)}")
        print(f"Focus years: {metadata.get('focus_years', 'all')}")
        print(f"Duration: {metadata.get('duration_seconds', 0):.1f} seconds")
        
        if args.output_setlists:
            print(f"Setlists saved to: {args.output_setlists}")
        if args.output_images:
            print(f"Images saved to: {args.output_images}")
        
        if scraper.progress.get('errors'):
            print(f"Errors encountered: {len(scraper.progress['errors'])}")
            print("Check gdsets_scraper.log for details")
    
    except KeyboardInterrupt:
        logger.info("Scraping interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()