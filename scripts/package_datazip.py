#!/usr/bin/env python3
"""
Data Packaging Script for Dead Archive Android App

This script packages processed metadata files into data.zip for app deployment.
The data.zip file contains all metadata required by the Android app including
ratings, setlists, songs, and venues data.

Usage:
    python scripts/package_datazip.py --metadata scripts/metadata --output app/src/main/assets/data.zip
    
    # Via Makefile (recommended)
    make package-datazip
"""

import argparse
import json
import os
import zipfile
from datetime import datetime
from pathlib import Path
import logging


class DataZipPackager:
    """
    Packages processed metadata files into data.zip for Android app deployment.
    """
    
    def __init__(self, metadata_dir: str = "scripts/metadata"):
        """Initialize the packager with metadata directory."""
        self.metadata_dir = Path(metadata_dir)
        self.logger = logging.getLogger(__name__)
        
        # Required files for data.zip
        self.required_files = {
            'ratings.json': self.metadata_dir / 'ratings.json',
            'setlists.json': self.metadata_dir / 'setlists' / 'setlists.json',
            'songs.json': self.metadata_dir / 'songs' / 'songs.json',
            'venues.json': self.metadata_dir / 'venues' / 'venues.json'
        }
    
    def validate_source_files(self) -> bool:
        """Validate that all required source files exist."""
        missing_files = []
        
        for zip_name, file_path in self.required_files.items():
            if not file_path.exists():
                missing_files.append(f"{zip_name} -> {file_path}")
            else:
                size_mb = file_path.stat().st_size / (1024 * 1024)
                modified = datetime.fromtimestamp(file_path.stat().st_mtime)
                self.logger.info(f"✓ {zip_name}: {size_mb:.1f}MB (modified: {modified.strftime('%Y-%m-%d %H:%M')})")
        
        if missing_files:
            self.logger.error("Missing required files:")
            for missing in missing_files:
                self.logger.error(f"  ❌ {missing}")
            return False
        
        return True
    
    def get_file_stats(self) -> dict:
        """Get statistics about the source files."""
        stats = {}
        
        for zip_name, file_path in self.required_files.items():
            if file_path.exists():
                stat = file_path.stat()
                stats[zip_name] = {
                    'size_bytes': stat.st_size,
                    'size_mb': stat.st_size / (1024 * 1024),
                    'modified': datetime.fromtimestamp(stat.st_mtime).isoformat()
                }
        
        return stats
    
    def create_data_zip(self, output_path: str) -> bool:
        """Create data.zip with all metadata files."""
        output_file = Path(output_path)
        
        try:
            # Ensure output directory exists
            output_file.parent.mkdir(parents=True, exist_ok=True)
            
            # Create ZIP file
            with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
                total_uncompressed = 0
                
                for zip_name, file_path in self.required_files.items():
                    if file_path.exists():
                        # Add file to ZIP
                        zf.write(file_path, zip_name)
                        
                        # Track statistics
                        file_size = file_path.stat().st_size
                        total_uncompressed += file_size
                        
                        self.logger.info(f"📁 Added {zip_name} ({file_size / (1024 * 1024):.1f}MB)")
                    else:
                        self.logger.warning(f"⚠️  Skipped missing file: {zip_name}")
                
                # Add metadata about the package
                package_info = {
                    'created_at': datetime.now().isoformat(),
                    'created_by': 'package_datazip.py',
                    'source_directory': str(self.metadata_dir),
                    'files_included': list(self.required_files.keys()),
                    'file_stats': self.get_file_stats()
                }
                
                zf.writestr('package_info.json', json.dumps(package_info, indent=2))
                self.logger.info("📋 Added package metadata")
            
            # Report final statistics
            compressed_size = output_file.stat().st_size
            compression_ratio = compressed_size / total_uncompressed if total_uncompressed > 0 else 0
            
            self.logger.info("")
            self.logger.info("📦 Package Summary:")
            self.logger.info(f"   Output: {output_file}")
            self.logger.info(f"   Uncompressed: {total_uncompressed / (1024 * 1024):.1f}MB")
            self.logger.info(f"   Compressed: {compressed_size / (1024 * 1024):.1f}MB")
            self.logger.info(f"   Compression: {compression_ratio * 100:.1f}% of original")
            self.logger.info(f"   Files: {len(self.required_files)} metadata files")
            
            return True
            
        except Exception as e:
            self.logger.error(f"❌ Failed to create data.zip: {e}")
            return False
    
    def package(self, output_path: str) -> bool:
        """Main packaging method."""
        self.logger.info("📦 Dead Archive Data Packaging")
        self.logger.info(f"Source: {self.metadata_dir}")
        self.logger.info(f"Output: {output_path}")
        self.logger.info("")
        
        # Validate source files
        self.logger.info("🔍 Validating source files...")
        if not self.validate_source_files():
            return False
        
        # Create the package
        self.logger.info("")
        self.logger.info("📁 Creating data.zip...")
        if not self.create_data_zip(output_path):
            return False
        
        self.logger.info("")
        self.logger.info("✅ Data packaging completed successfully!")
        self.logger.info("🚀 Ready for Android app deployment")
        
        return True


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Package metadata files into data.zip for Android app deployment'
    )
    parser.add_argument(
        '--metadata', 
        default='scripts/metadata',
        help='Metadata directory containing processed files (default: scripts/metadata)'
    )
    parser.add_argument(
        '--output',
        default='app/src/main/assets/data.zip',
        help='Output path for data.zip (default: app/src/main/assets/data.zip)'
    )
    parser.add_argument(
        '--verbose', 
        action='store_true',
        help='Enable verbose logging'
    )
    
    args = parser.parse_args()
    
    # Configure logging
    log_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(message)s'
    )
    
    # Create packager and run
    packager = DataZipPackager(metadata_dir=args.metadata)
    success = packager.package(args.output)
    
    if not success:
        exit(1)


if __name__ == '__main__':
    main()