#!/usr/bin/env python3
"""
CMU Setlist Scraper for Grateful Dead Archive

This script scrapes setlist data from the CS.CMU.EDU setlist archive (1972-1995).
It follows the implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Scrapes all year links from the main setlist page (1972-1995)
- For each year, scrapes all individual show links
- Parses each show text file to extract raw data:
  - First line: venue, city, state, date information
  - Set information based on double newline separators
  - Raw song lists for each set
- Preserves the original data structure with minimal processing
- Stores in structured JSON format with original date format

Usage:
    python scripts/scrape_cmu_setlists.py --output scripts/metadata/setlists/cmu_setlists.json
    python scripts/scrape_cmu_setlists.py --output scripts/metadata/setlists/cmu_setlists.json --delay 1.0
    python scripts/scrape_cmu_setlists.py --year-range 1977-1980 --output cmu_subset.json
"""

import argparse
import json
import logging
import re
import requests
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from urllib.parse import urljoin, urlparse
import sys
from bs4 import BeautifulSoup


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('cmu_scraper.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class CMUSetlistScraper:
    """Scraper for CS.CMU.EDU Grateful Dead setlist archive"""
    
    BASE_URL = "https://www.cs.cmu.edu/~mleone/gdead/"
    SETLISTS_URL = urljoin(BASE_URL, "setlists.html")
    
    def __init__(self, delay: float = 0.5, year_range: Optional[Tuple[int, int]] = None):
        """
        Initialize the scraper
        
        Args:
            delay: Delay between requests in seconds
            year_range: Optional tuple of (start_year, end_year) to limit scraping
        """
        self.delay = delay
        self.year_range = year_range
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Dead Archive Setlist Scraper (respectful crawling for archival purposes)'
        })
        
        # Progress tracking
        self.progress = {
            'years_completed': [],
            'current_year': None,
            'shows_scraped': 0,
            'total_shows': 0,
            'start_time': datetime.now().isoformat(),
            'errors': []
        }
        
        # Results storage
        self.setlists = {}
        
    def get_page(self, url: str, retries: int = 3) -> Optional[str]:
        """
        Fetch a page with retries and rate limiting
        
        Args:
            url: URL to fetch
            retries: Number of retry attempts
            
        Returns:
            Page content as string or None if failed
        """
        for attempt in range(retries):
            try:
                time.sleep(self.delay)
                response = self.session.get(url, timeout=30)
                response.raise_for_status()
                return response.text
            except requests.RequestException as e:
                logger.warning(f"Attempt {attempt + 1} failed for {url}: {e}")
                if attempt == retries - 1:
                    logger.error(f"Failed to fetch {url} after {retries} attempts")
                    self.progress['errors'].append({
                        'url': url,
                        'error': str(e),
                        'timestamp': datetime.now().isoformat()
                    })
                    return None
                time.sleep(self.delay * (attempt + 1))  # Exponential backoff
        
        return None
    
    def extract_year_links(self, main_page_content: str) -> List[Tuple[int, str]]:
        """
        Extract year links from the main setlists page
        
        Args:
            main_page_content: HTML content of the main setlists page
            
        Returns:
            List of (year, url) tuples
        """
        year_links = []
        
        # First try using BeautifulSoup for more reliable extraction
        soup = BeautifulSoup(main_page_content, 'html.parser')
        
        # Method 1: Look for links with 2-digit year pattern (e.g., 95.html)
        for a_tag in soup.find_all('a'):
            href = a_tag.get('href', '')
            text = a_tag.text.strip()
            
            # Check for YY.html pattern
            year_match = re.search(r'(\d{2})\.html', href)
            if year_match:
                year_text = year_match.group(1)
                year = int('19' + year_text)
                
                # CMU archive covers 1972-1995
                if 1972 <= year <= 1995:
                    if self.year_range:
                        start_year, end_year = self.year_range
                        if not (start_year <= year <= end_year):
                            continue
                    
                    full_url = urljoin(self.SETLISTS_URL, href)
                    year_links.append((year, full_url))
                    logger.debug(f"Found year link: {year} -> {href}")
        
        # Method 2: Extract year from link text if Method 1 failed
        if not year_links:
            for a_tag in soup.find_all('a'):
                href = a_tag.get('href', '')
                text = a_tag.text.strip()
                
                # Try to extract a year number from text
                year_match = re.search(r'(19\d{2}|\d{2})', text)
                if year_match:
                    year_text = year_match.group(1)
                    # Handle 2-digit years
                    if len(year_text) == 2:
                        year = int('19' + year_text)
                    else:
                        year = int(year_text)
                    
                    # CMU archive covers 1972-1995
                    if 1972 <= year <= 1995:
                        if self.year_range:
                            start_year, end_year = self.year_range
                            if not (start_year <= year <= end_year):
                                continue
                        
                        full_url = urljoin(self.SETLISTS_URL, href)
                        year_links.append((year, full_url))
                        logger.debug(f"Found year link from text: {year} -> {href}")
        
        # Fallback to regex method if BeautifulSoup methods failed
        if not year_links:
            # Look for links to year pages (pattern: YYYY.html or YY.html)
            year_pattern = r'<a\s+href="([^"]*(?:setlists/)?(\d{2}|\d{4})\.html?)"'
            matches = re.findall(year_pattern, main_page_content, re.IGNORECASE)
            
            for link, year_str in matches:
                # Handle 2-digit years
                if len(year_str) == 2:
                    year = int('19' + year_str)
                else:
                    year = int(year_str)
                
                # CMU archive covers 1972-1995
                if 1972 <= year <= 1995:
                    if self.year_range:
                        start_year, end_year = self.year_range
                        if not (start_year <= year <= end_year):
                            continue
                    
                    full_url = urljoin(self.SETLISTS_URL, link)
                    year_links.append((year, full_url))
                    logger.debug(f"Found year link (regex): {year} -> {link}")
        
        year_links.sort()  # Sort by year
        logger.info(f"Found {len(year_links)} year links: {[y for y, _ in year_links]}")
        return year_links
    
    def extract_show_links(self, year_page_content: str, year: int) -> List[Tuple[str, str]]:
        """
        Extract individual show links from a year page
        
        Args:
            year_page_content: HTML content of the year page
            year: Year being processed
            
        Returns:
            List of (show_id, url) tuples
        """
        show_links = []
        year_suffix = str(year)[2:] # Get 2-digit year (e.g., '77' from 1977)
        
        # Try BeautifulSoup first to find MM-DD-YY.txt pattern links
        soup = BeautifulSoup(year_page_content, 'html.parser')
        
        # Look for links ending with .txt
        for a_tag in soup.find_all('a'):
            href = a_tag.get('href', '')
            
            # Check if it's a setlist file
            if href.endswith('.txt'):
                # Try to extract date from href pattern like 'dead-sets/77/2-26-77.txt'
                date_match = re.search(r'/([0-9]{1,2}-[0-9]{1,2})-([0-9]{2})\.txt$', href)
                if date_match:
                    month_day = date_match.group(1)
                    year_part = date_match.group(2)
                    
                    # Verify year suffix matches
                    if year_part == year_suffix:
                        # Format as YYYY-MM-DD
                        month, day = month_day.split('-')
                        date_str = f"{year}-{month.zfill(2)}-{day.zfill(2)}"
                        
                        full_url = urljoin(self.BASE_URL, href)
                        show_links.append((date_str, full_url))
                        logger.debug(f"Found show link: {date_str} -> {href}")
        
        # If BeautifulSoup didn't work, try regex patterns
        if not show_links:
            # Pattern for MM-DD-YY.txt format
            show_pattern = r'<a\s+href="([^"]*/([\d]{1,2}-[\d]{1,2})-' + year_suffix + r'\.txt?)"'
            matches = re.findall(show_pattern, year_page_content, re.IGNORECASE)
            
            for link, month_day in matches:
                # Format as YYYY-MM-DD
                month, day = month_day.split('-')
                date_str = f"{year}-{month.zfill(2)}-{day.zfill(2)}"
                
                full_url = urljoin(self.BASE_URL, link)
                show_links.append((date_str, full_url))
                logger.debug(f"Found show link (regex): {date_str} -> {link}")
        
        # Original patterns as fallback
        if not show_links:
            # Try YYYY-MM-DD pattern
            show_pattern = r'<a\s+href="([^"]*(\d{4}-\d{2}-\d{2})[^"]*\.txt)"'
            matches = re.findall(show_pattern, year_page_content, re.IGNORECASE)
            
            for link, date_str in matches:
                # Verify the date matches the current year
                if date_str.startswith(str(year)):
                    full_url = urljoin(self.SETLISTS_URL, link)
                    show_links.append((date_str, full_url))
            
            # Try broader pattern as last resort
            if not show_links:
                general_pattern = r'<a\s+href="([^"]*\.txt)"[^>]*>([^<]*)'
                matches = re.findall(general_pattern, year_page_content, re.IGNORECASE)
                
                for link, text in matches:
                    # Try to extract date from link or text
                    date_match = re.search(r'(\d{4}-\d{2}-\d{2})', link + ' ' + text)
                    if date_match and date_match.group(1).startswith(str(year)):
                        full_url = urljoin(self.SETLISTS_URL, link)
                        show_links.append((date_match.group(1), full_url))
        
        show_links.sort()  # Sort by date
        logger.info(f"Found {len(show_links)} show links for {year}")
        return show_links
    
    def parse_setlist(self, content: str, show_id: str) -> Optional[Dict[str, Any]]:
        """
        Parse individual setlist from text content
        
        Args:
            content: Raw text content of the setlist file
            show_id: Show identifier (typically date)
            
        Returns:
            Parsed setlist dictionary or None if parsing failed
        """
        try:
            lines = content.strip().split('\n')
            if not lines:
                return None
            
            # First line: venue, city, state, date information
            venue_line = lines[0].strip()
            
            # Parse the remaining content for sets
            full_text = '\n'.join(lines[1:]) if len(lines) > 1 else ''
            
            # Split by double newlines to separate sets
            sections = re.split(r'\n\s*\n', full_text)
            
            sets = {}
            current_set = 1
            
            for section in sections:
                section = section.strip()
                if not section:
                    continue
                
                # Identify set headers
                section_lower = section.lower()
                if any(keyword in section_lower for keyword in ['set 1', 'first set', '1st set']):
                    set_key = 'set1'
                elif any(keyword in section_lower for keyword in ['set 2', 'second set', '2nd set']):
                    set_key = 'set2'
                elif any(keyword in section_lower for keyword in ['set 3', 'third set', '3rd set']):
                    set_key = 'set3'
                elif any(keyword in section_lower for keyword in ['encore']):
                    set_key = 'encore'
                else:
                    # Auto-assign set number
                    set_key = f'set{current_set}'
                    current_set += 1
                
                # Extract songs from the section
                # Remove set headers and extract song lines
                song_lines = []
                for line in section.split('\n'):
                    line = line.strip()
                    if not line:
                        continue
                    # Skip obvious header lines
                    if any(keyword in line.lower() for keyword in 
                           ['set 1', 'set 2', 'set 3', 'first set', 'second set', 'third set', 'encore:']):
                        continue
                    song_lines.append(line)
                
                if song_lines:
                    sets[set_key] = song_lines
            
            # If no sets were identified, treat the whole content as one set
            if not sets and full_text.strip():
                all_songs = []
                for line in full_text.split('\n'):
                    line = line.strip()
                    if line:
                        all_songs.append(line)
                if all_songs:
                    sets['set1'] = all_songs
            
            return {
                'show_id': show_id,
                'venue_line': venue_line,
                'sets': sets,
                'raw_content': content,
                'source': 'cs.cmu.edu',
                'scraped_at': datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Failed to parse setlist for {show_id}: {e}")
            return None
    
    def scrape_year(self, year: int, year_url: str) -> int:
        """
        Scrape all shows for a given year
        
        Args:
            year: Year to scrape
            year_url: URL for the year page
            
        Returns:
            Number of shows successfully scraped
        """
        logger.info(f"Scraping year {year} from {year_url}")
        self.progress['current_year'] = year
        
        # Get the year page
        year_content = self.get_page(year_url)
        if not year_content:
            logger.error(f"Failed to get year page for {year}")
            return 0
        
        # Extract show links
        show_links = self.extract_show_links(year_content, year)
        if not show_links:
            logger.warning(f"No show links found for {year}")
            return 0
        
        scraped_count = 0
        
        for show_id, show_url in show_links:
            logger.info(f"Scraping show {show_id} from {show_url}")
            
            # Get the show content
            show_content = self.get_page(show_url)
            if not show_content:
                logger.error(f"Failed to get show content for {show_id}")
                continue
            
            # Parse the setlist
            setlist = self.parse_setlist(show_content, show_id)
            if setlist:
                self.setlists[show_id] = setlist
                scraped_count += 1
                self.progress['shows_scraped'] += 1
                logger.info(f"Successfully scraped {show_id}")
            else:
                logger.error(f"Failed to parse setlist for {show_id}")
        
        self.progress['years_completed'].append(year)
        logger.info(f"Completed year {year}: {scraped_count}/{len(show_links)} shows scraped")
        return scraped_count
    
    def scrape_all(self) -> Dict[str, Any]:
        """
        Scrape all setlists from the CMU archive
        
        Returns:
            Dictionary containing all scraped setlists and metadata
        """
        logger.info("Starting CMU setlist scraping")
        start_time = datetime.now()
        
        # Get the main setlists page
        main_content = self.get_page(self.SETLISTS_URL)
        if not main_content:
            logger.error("Failed to get main setlists page")
            return {}
        
        # Extract year links
        year_links = self.extract_year_links(main_content)
        if not year_links:
            logger.error("No year links found")
            return {}
        
        # Scrape each year
        total_scraped = 0
        for year, year_url in year_links:
            try:
                scraped = self.scrape_year(year, year_url)
                total_scraped += scraped
            except Exception as e:
                logger.error(f"Error scraping year {year}: {e}")
                self.progress['errors'].append({
                    'year': year,
                    'error': str(e),
                    'timestamp': datetime.now().isoformat()
                })
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        logger.info(f"Scraping completed: {total_scraped} shows scraped in {duration:.1f} seconds")
        
        # Return structured results
        return {
            'metadata': {
                'source': 'cs.cmu.edu/~mleone/gdead/setlists.html',
                'scraped_at': end_time.isoformat(),
                'duration_seconds': duration,
                'total_shows': total_scraped,
                'year_range': f"{year_links[0][0]}-{year_links[-1][0]}" if year_links else "none",
                'scraper_version': '1.0.0'
            },
            'progress': self.progress,
            'setlists': self.setlists
        }
    
    def save_results(self, output_path: str, results: Dict[str, Any]) -> None:
        """
        Save results to JSON file
        
        Args:
            output_path: Path to save the results
            results: Results dictionary to save
        """
        output_file = Path(output_path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Results saved to {output_path}")


def parse_year_range(year_range_str: str) -> Tuple[int, int]:
    """Parse year range string like '1977-1980' into tuple"""
    if '-' in year_range_str:
        start, end = year_range_str.split('-')
        return int(start), int(end)
    else:
        year = int(year_range_str)
        return year, year


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Scrape CMU Grateful Dead setlist archive')
    parser.add_argument('--output', '-o', required=True,
                        help='Output JSON file path')
    parser.add_argument('--delay', '-d', type=float, default=0.5,
                        help='Delay between requests in seconds (default: 0.5)')
    parser.add_argument('--year-range', '-y', type=str,
                        help='Year range to scrape (e.g., "1977-1980" or "1977")')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Parse year range if provided
    year_range = None
    if args.year_range:
        try:
            year_range = parse_year_range(args.year_range)
            logger.info(f"Limiting scraping to years {year_range[0]}-{year_range[1]}")
        except ValueError as e:
            logger.error(f"Invalid year range format: {e}")
            sys.exit(1)
    
    # Initialize scraper
    scraper = CMUSetlistScraper(delay=args.delay, year_range=year_range)
    
    try:
        # Scrape all setlists
        results = scraper.scrape_all()
        
        if not results.get('setlists'):
            logger.error("No setlists were scraped")
            sys.exit(1)
        
        # Save results
        scraper.save_results(args.output, results)
        
        # Print summary
        metadata = results.get('metadata', {})
        print(f"\nScraping Summary:")
        print(f"Total shows scraped: {metadata.get('total_shows', 0)}")
        print(f"Year range: {metadata.get('year_range', 'unknown')}")
        print(f"Duration: {metadata.get('duration_seconds', 0):.1f} seconds")
        print(f"Output saved to: {args.output}")
        
        if scraper.progress.get('errors'):
            print(f"Errors encountered: {len(scraper.progress['errors'])}")
            print("Check cmu_scraper.log for details")
    
    except KeyboardInterrupt:
        logger.info("Scraping interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()