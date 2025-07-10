#!/usr/bin/env python3
"""
Image Processor for Grateful Dead Archive

This script processes raw image data from GDSets.com to create a normalized images database.
It follows the implementation plan outlined in docs/setlist-implementation-plan.md.

Architecture:
- Loads raw image data from GDSets scraper output
- Matches images with venue IDs and show data
- Creates unique image IDs for consistent referencing
- Generates image metadata with proper categorization
- Produces clean images.json for app integration

Key Features:
- Image type classification (poster, ticket, backstage, program)
- Show and venue ID linking
- Image URL validation and normalization
- Metadata enrichment with descriptions and dates
- Duplicate detection and handling

Usage:
    python scripts/process_images.py --input scripts/metadata/images/gdsets_images.json --venues scripts/metadata/venues/venues.json --output scripts/metadata/images/images.json
    python scripts/process_images.py --input gdsets_images.json --venues venues.json --output images.json --verbose
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
from urllib.parse import urlparse


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('process_images.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class ImageProcessor:
    """Processes and normalizes image data from raw GDSets images"""
    
    def __init__(self):
        """Initialize the image processor"""
        self.images = {}
        self.venues_by_id = {}
        self.venue_lookup = {}
        self.processing_stats = {
            'total_raw_images': 0,
            'images_processed': 0,
            'images_with_venues': 0,
            'images_with_shows': 0,
            'duplicate_urls': 0,
            'invalid_urls': 0,
            'start_time': datetime.now().isoformat()
        }
        
        # Image type classifications
        self.image_type_patterns = {
            'poster': [r'poster', r'ad', r'flyer', r'handbill'],
            'ticket': [r'ticket', r'stub'],
            'backstage': [r'backstage', r'pass', r'credential'],
            'program': [r'program', r'bill', r'lineup'],
            'venue': [r'venue', r'hall', r'theater', r'stage'],
            'memorabilia': [r'setlist', r'note', r'misc', r'other']
        }
    
    def load_venues(self, venues_path: str) -> None:
        """
        Load venues data for venue ID matching
        
        Args:
            venues_path: Path to venues JSON file
        """
        try:
            with open(venues_path, 'r', encoding='utf-8') as f:
                venues_data = json.load(f)
            
            self.venues_by_id = venues_data.get('venues', {})
            
            # Create lookup table for venue matching
            for venue_id, venue_info in self.venues_by_id.items():
                venue_name = venue_info.get('name', '').lower().strip()
                city = venue_info.get('city', '').lower().strip()
                
                # Primary lookup by name
                if venue_name:
                    self.venue_lookup[venue_name] = venue_id
                
                # Secondary lookup by name + city
                if venue_name and city:
                    self.venue_lookup[f"{venue_name} {city}"] = venue_id
                
                # Include aliases
                for alias in venue_info.get('aliases', []):
                    alias_key = alias.lower().strip()
                    if alias_key and alias_key not in self.venue_lookup:
                        self.venue_lookup[alias_key] = venue_id
            
            logger.info(f"Loaded {len(self.venues_by_id)} venues with {len(self.venue_lookup)} lookup entries")
            
        except Exception as e:
            logger.error(f"Failed to load venues: {e}")
            raise
    
    def load_raw_images(self, input_path: str) -> Dict[str, Any]:
        """
        Load raw image data from GDSets scraper output
        
        Args:
            input_path: Path to raw images JSON file
            
        Returns:
            Loaded raw image data
        """
        try:
            with open(input_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            if 'images' not in data:
                raise ValueError("Input file missing 'images' key")
            
            logger.info(f"Loaded {len(data['images'])} raw images from {input_path}")
            return data
            
        except Exception as e:
            logger.error(f"Failed to load raw images: {e}")
            raise
    
    def classify_image_type(self, description: str, filename: str) -> str:
        """
        Classify image type based on description and filename
        
        Args:
            description: Image description from source
            filename: Image filename
            
        Returns:
            Classified image type
        """
        text_to_check = f"{description} {filename}".lower()
        
        for image_type, patterns in self.image_type_patterns.items():
            for pattern in patterns:
                if re.search(pattern, text_to_check):
                    return image_type
        
        return 'memorabilia'  # Default fallback
    
    def generate_image_id(self, url: str, show_id: str, image_type: str) -> str:
        """
        Generate unique image ID
        
        Args:
            url: Image URL
            show_id: Associated show ID
            image_type: Classified image type
            
        Returns:
            Unique image ID
        """
        # Create unique string for hashing
        unique_string = f"{url}|{show_id}|{image_type}"
        image_hash = hashlib.md5(unique_string.encode()).hexdigest()[:8]
        
        # Create readable ID components
        id_parts = []
        if show_id:
            id_parts.append(show_id.replace('-', ''))
        if image_type:
            id_parts.append(image_type[:4])  # First 4 chars of type
        
        # Combine parts with hash
        base_id = '-'.join(id_parts) if id_parts else 'img'
        image_id = f"{base_id}-{image_hash}"
        
        # Clean up the ID
        image_id = re.sub(r'[^a-z0-9\-]', '', image_id.lower())
        image_id = re.sub(r'\-+', '-', image_id).strip('-')
        
        return image_id
    
    def validate_image_url(self, url: str) -> bool:
        """
        Validate image URL format and accessibility
        
        Args:
            url: Image URL to validate
            
        Returns:
            True if URL appears valid
        """
        if not url or not url.strip():
            return False
        
        try:
            parsed = urlparse(url)
            if not parsed.scheme or not parsed.netloc:
                return False
            
            # Check for image file extensions
            path_lower = parsed.path.lower()
            if not any(path_lower.endswith(ext) for ext in ['.jpg', '.jpeg', '.png', '.gif', '.webp']):
                return False
            
            return True
            
        except Exception:
            return False
    
    def find_venue_id(self, show_id: str, description: str) -> Optional[str]:
        """
        Find venue ID for image based on show ID and description
        
        Args:
            show_id: Show identifier
            description: Image description that might contain venue info
            
        Returns:
            Venue ID if found, None otherwise
        """
        # For now, we'll need to implement venue matching logic
        # This could be enhanced to parse venue names from descriptions
        # or cross-reference with show data if available
        
        # Simple approach: look for venue names in description
        description_lower = description.lower()
        for venue_name, venue_id in self.venue_lookup.items():
            if venue_name in description_lower:
                return venue_id
        
        return None
    
    def process_images(self, raw_images_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process all images from raw data
        
        Args:
            raw_images_data: Raw image data from scraper
            
        Returns:
            Processed images data
        """
        logger.info("Starting image processing")
        start_time = datetime.now()
        
        raw_images = raw_images_data.get('images', {})
        self.processing_stats['total_raw_images'] = len(raw_images)
        
        # Track processed images and duplicates
        processed_images = {}
        url_to_id = {}  # Track duplicate URLs
        
        for raw_id, raw_image in raw_images.items():
            try:
                # Extract basic information
                url = raw_image.get('url', '').strip()
                show_id = raw_image.get('show_id', '').strip()
                description = raw_image.get('description', '').strip()
                filename = raw_image.get('filename', '').strip()
                
                # Validate URL
                if not self.validate_image_url(url):
                    self.processing_stats['invalid_urls'] += 1
                    logger.debug(f"Invalid URL skipped: {url}")
                    continue
                
                # Check for duplicate URLs
                if url in url_to_id:
                    self.processing_stats['duplicate_urls'] += 1
                    logger.debug(f"Duplicate URL skipped: {url}")
                    continue
                
                # Classify image type
                image_type = self.classify_image_type(description, filename)
                
                # Generate unique image ID
                image_id = self.generate_image_id(url, show_id, image_type)
                
                # Find associated venue ID
                venue_id = self.find_venue_id(show_id, description)
                if venue_id:
                    self.processing_stats['images_with_venues'] += 1
                
                # Create processed image entry
                processed_image = {
                    'image_id': image_id,
                    'type': image_type,
                    'url': url,
                    'filename': filename,
                    'description': description,
                    'show_id': show_id if show_id else None,
                    'venue_id': venue_id,
                    'source': 'gdsets.com',
                    'source_url': raw_image.get('source_url', ''),
                    'scraped_at': raw_image.get('scraped_at', ''),
                    'processed_at': datetime.now().isoformat()
                }
                
                # Remove None values for cleaner JSON
                processed_image = {k: v for k, v in processed_image.items() if v is not None}
                
                processed_images[image_id] = processed_image
                url_to_id[url] = image_id
                
                if show_id:
                    self.processing_stats['images_with_shows'] += 1
                
                self.processing_stats['images_processed'] += 1
                
            except Exception as e:
                logger.error(f"Failed to process image {raw_id}: {e}")
                continue
        
        duration = (datetime.now() - start_time).total_seconds()
        
        logger.info(f"Image processing completed: {len(processed_images)} images processed in {duration:.1f} seconds")
        
        return {
            'metadata': {
                'source': 'processed from gdsets_images.json',
                'processed_at': datetime.now().isoformat(),
                'processing_duration_seconds': duration,
                'total_images': len(processed_images),
                'processing_stats': self.processing_stats,
                'processor_version': '1.0.0'
            },
            'images': processed_images
        }
    
    def save_images(self, images_data: Dict[str, Any], output_path: str) -> None:
        """
        Save processed images data
        
        Args:
            images_data: Processed images data
            output_path: Output file path
        """
        try:
            output_file = Path(output_path)
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(images_data, f, indent=2, ensure_ascii=False)
            
            logger.info(f"Images data saved to {output_path}")
            
            # Log file size
            file_size = output_file.stat().st_size
            logger.info(f"Output file size: {file_size / 1024:.1f} KB")
            
        except Exception as e:
            logger.error(f"Failed to save images data: {e}")
            raise
    
    def print_image_summary(self, images_data: Dict[str, Any]) -> None:
        """Print summary of image processing"""
        images = images_data.get('images', {})
        metadata = images_data.get('metadata', {})
        stats = metadata.get('processing_stats', {})
        
        print("\n" + "="*60)
        print("IMAGE PROCESSING SUMMARY")
        print("="*60)
        print(f"Total raw images:         {stats.get('total_raw_images', 0):,}")
        print(f"Images processed:         {metadata.get('total_images', 0):,}")
        print(f"Images with show links:   {stats.get('images_with_shows', 0):,}")
        print(f"Images with venue links:  {stats.get('images_with_venues', 0):,}")
        print(f"Duplicate URLs skipped:   {stats.get('duplicate_urls', 0):,}")
        print(f"Invalid URLs skipped:     {stats.get('invalid_urls', 0):,}")
        print(f"Processing time:          {metadata.get('processing_duration_seconds', 0):.1f}s")
        
        if images:
            # Show image type distribution
            type_counts = Counter(img['type'] for img in images.values())
            print(f"\nImage Type Distribution:")
            for img_type, count in type_counts.most_common():
                print(f"  {img_type.capitalize():<12}: {count:,}")
            
            # Show sample images by type
            print(f"\nSample Images by Type:")
            samples_shown = set()
            for img_type in ['poster', 'ticket', 'backstage', 'program']:
                for img in images.values():
                    if img['type'] == img_type and img_type not in samples_shown:
                        show_id = img.get('show_id', 'unknown')
                        desc = img.get('description', 'no description')[:30] + "..."
                        print(f"  {img_type.capitalize():<12}: {show_id} - {desc}")
                        samples_shown.add(img_type)
                        break
        
        print("="*60)


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Process images from raw GDSets image data')
    parser.add_argument('--input', '-i', required=True,
                        help='Input raw images JSON file')
    parser.add_argument('--venues', '-v', required=True,
                        help='Input venues JSON file')
    parser.add_argument('--output', '-o', required=True,
                        help='Output processed images JSON file')
    parser.add_argument('--verbose', action='store_true',
                        help='Enable verbose logging')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Initialize processor
    processor = ImageProcessor()
    
    try:
        # Load venues data
        processor.load_venues(args.venues)
        
        # Load raw images data
        raw_images_data = processor.load_raw_images(args.input)
        
        # Process images
        images_data = processor.process_images(raw_images_data)
        
        # Save results
        processor.save_images(images_data, args.output)
        
        # Print summary
        processor.print_image_summary(images_data)
        
        logger.info("Image processing completed successfully")
        
    except KeyboardInterrupt:
        logger.info("Processing interrupted by user")
        sys.exit(1)
    except Exception as e:
        logger.error(f"Processing failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()