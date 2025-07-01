#!/usr/bin/env python3
"""
Material Icon Downloader for Android

This script downloads Material Design icons from Google's CDN,
converts them to Android vector drawable format, and adds them to your project.

Usage:
  python download_material_icons.py --icon-name "skip_next" --icon-name "volume_up"
  python download_material_icons.py --from-json "material_icons_config.json"
"""

import argparse
import json
import os
import re
import shutil
import sys
import tempfile
from io import BytesIO
from pathlib import Path
from typing import Dict, List, Optional, Set, Union

try:
    import requests
    from lxml import etree
except ImportError:
    print("Required packages missing. Install with: pip install requests lxml")
    sys.exit(1)

# Configuration
MATERIAL_ICONS_CDN = "https://fonts.gstatic.com/s/i/materialicons"
DEFAULT_VERSION = "v4"
DEFAULT_OUTPUT_DIR = "core/design/src/main/res/drawable"
ICON_RESOURCES_PATH = "core/design/src/main/java/com/deadarchive/core/design/component/IconResources.kt"
ICON_REGISTRY_PATTERN = r"object (\w+) \{"

# Icon styles mapping
STYLES = {
    "filled": "",
    "outlined": "outlined",
    "round": "round",
    "sharp": "sharp",
    "twotone": "two_tone"
}

def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Download Material Design icons for Android')
    
    icon_group = parser.add_mutually_exclusive_group(required=True)
    icon_group.add_argument('--icon-name', action='append', help='Name of the icon(s) to download')
    icon_group.add_argument('--from-json', help='Load icon configuration from JSON file')
    
    parser.add_argument('--style', choices=list(STYLES.keys()), default="filled", 
                      help='Icon style (filled, outlined, round, sharp, twotone)')
    parser.add_argument('--version', default=DEFAULT_VERSION, 
                      help='Icon version, e.g., v4')
    parser.add_argument('--output', default=DEFAULT_OUTPUT_DIR, 
                      help='Output directory for vector drawables')
    parser.add_argument('--update-registry', action='store_true',
                      help='Update IconResources.kt with new icons')
    parser.add_argument('--icon-registry-path', default=ICON_RESOURCES_PATH,
                      help='Path to the IconResources.kt file')
    parser.add_argument('--registry-category', default="PlayerControls",
                      help='Category in IconResources to add icons to')
    parser.add_argument('--dry-run', action='store_true', 
                      help='Show what would be downloaded without downloading')
    
    return parser.parse_args()

def snake_to_camel_case(name: str) -> str:
    """Convert snake_case to camelCase."""
    components = name.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def camel_to_snake_case(name: str) -> str:
    """Convert camelCase to snake_case."""
    name = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', name).lower()

def download_icon_svg(icon_name: str, style: str, version: str) -> Optional[bytes]:
    """Download SVG icon from Google's Material Icons CDN."""
    # For non-filled styles, use the style subdirectory
    style_path = f"/{STYLES[style]}" if STYLES[style] else ""
    
    url = f"{MATERIAL_ICONS_CDN}{style_path}/{icon_name}/{version}/24px.svg"
    response = requests.get(url)
    
    if response.status_code != 200:
        # Try alternative version (v7 is also common)
        alt_version = "v7" if version != "v7" else "v5"
        url = f"{MATERIAL_ICONS_CDN}{style_path}/{icon_name}/{alt_version}/24px.svg"
        response = requests.get(url)
        
        if response.status_code != 200:
            print(f"Error downloading {icon_name}: HTTP {response.status_code}")
            return None
    
    return response.content

def convert_svg_to_vector_drawable(svg_content: bytes) -> bytes:
    """Convert SVG to Android Vector Drawable XML format."""
    # Parse SVG
    parser = etree.XMLParser(remove_blank_text=True)
    svg_tree = etree.parse(BytesIO(svg_content), parser)
    svg_root = svg_tree.getroot()
    
    # Create Vector Drawable
    ns = {"android": "http://schemas.android.com/apk/res/android"}
    vector = etree.Element("vector", nsmap=ns)
    
    # Extract width/height from viewBox
    viewbox = svg_root.get("viewBox", "0 0 24 24")
    vb_parts = viewbox.split()
    width = svg_root.get("width", vb_parts[2]) if len(vb_parts) >= 3 else "24"
    height = svg_root.get("height", vb_parts[3]) if len(vb_parts) >= 4 else "24"
    
    # Set Vector attributes
    vector.set("{http://schemas.android.com/apk/res/android}width", f"{width}dp")
    vector.set("{http://schemas.android.com/apk/res/android}height", f"{height}dp")
    vector.set("{http://schemas.android.com/apk/res/android}viewportWidth", width)
    vector.set("{http://schemas.android.com/apk/res/android}viewportHeight", height)
    vector.set("{http://schemas.android.com/apk/res/android}tint", "?attr/colorControlNormal")
    
    # Process SVG paths to Vector paths
    paths = svg_root.findall(".//{http://www.w3.org/2000/svg}path")
    for path in paths:
        path_data = path.get("d")
        fill = path.get("fill", "#000000")
        
        # Skip "none" fill paths which are usually just bounding boxes
        if fill == "none":
            continue
            
        path_element = etree.SubElement(vector, "path")
        path_element.set("{http://schemas.android.com/apk/res/android}fillColor", fill)
        path_element.set("{http://schemas.android.com/apk/res/android}pathData", path_data)
    
    # Generate formatted XML - Fixed to avoid double XML declaration
    vector_drawable = b'<?xml version="1.0" encoding="utf-8"?>\n' + etree.tostring(
        vector, 
        pretty_print=True,
        encoding="utf-8",
        xml_declaration=False
    )
    
    return vector_drawable

def save_vector_drawable(vector_content: bytes, icon_name: str, output_dir: str) -> str:
    """Save vector drawable to the appropriate resource directory."""
    # Create directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)
    
    # Save vector drawable
    icon_filename = f"ic_{icon_name}.xml"
    output_path = os.path.join(output_dir, icon_filename)
    
    with open(output_path, 'wb') as f:
        f.write(vector_content)
    
    return output_path

def update_icon_registry(icon_names: List[str], registry_path: str, category: str) -> bool:
    """Update the IconResources.kt file with new icon entries."""
    if not os.path.exists(registry_path):
        print(f"Error: Icon registry file not found at {registry_path}")
        return False
    
    # Read the registry file
    with open(registry_path, 'r') as f:
        content = f.read()
    
    # Check the entire file for existing functions to avoid duplicates across categories
    existing_functions = set()
    for match in re.finditer(r'fun\s+(\w+)\(\)', content):
        existing_functions.add(match.group(1))
    
    # Also check for duplicate function definitions at the top level (outside object blocks)
    # These are often found at the end of the file and can cause build errors
    top_level_functions = set()
    for match in re.finditer(r'@Composable\s+fun\s+(\w+)\(\)', content):
        top_level_functions.add(match.group(1))
    
    # Find the specified category object
    pattern = f"object {category} {{"
    category_pos = content.find(pattern)
    
    if category_pos == -1:
        print(f"Error: Category '{category}' not found in IconResources.kt")
        print("Available categories:")
        for match in re.finditer(ICON_REGISTRY_PATTERN, content):
            print(f"  - {match.group(1)}")
        return False
    
    # Find the end of the category block
    open_braces = 1
    category_end_pos = category_pos + len(pattern)
    for i in range(category_end_pos, len(content)):
        if content[i] == '{':
            open_braces += 1
        elif content[i] == '}':
            open_braces -= 1
            if open_braces == 0:
                category_end_pos = i
                break
    
    # Prepare new entries
    new_entries = []
    for icon_name in icon_names:
        camel_case_name = snake_to_camel_case(icon_name)
        
        # Skip if function already exists anywhere in the file
        if camel_case_name in existing_functions:
            print(f"Icon {camel_case_name} already exists somewhere in the file")
            continue
            
        # Skip if function exists at the top level (typically duplicate functions at file end)
        if camel_case_name in top_level_functions:
            print(f"Icon {camel_case_name} exists as a top-level function and may cause conflicts")
            continue
        
        # Check if the icon already exists in this category (both function and property)
        if re.search(rf"fun {camel_case_name}\(\)", content[category_pos:category_end_pos]) or \
           re.search(rf"val {camel_case_name} =", content[category_pos:category_end_pos]):
            print(f"Icon {camel_case_name} already exists in {category}")
            continue
        
        # For custom icons - use Composable function pattern
        entry = f'        @Composable\n        fun {camel_case_name}() = customIcon(R.drawable.ic_{icon_name})'
        
        new_entries.append(entry)
        # Add to set to prevent duplicates within the same run
        existing_functions.add(camel_case_name)
    
    if not new_entries:
        print("No new entries to add")
        return True
    
    # Insert new entries before the closing brace
    insert_pos = content.rfind("    }", 0, category_end_pos) - 1
    new_content = content[:insert_pos] + "\n" + "\n".join(new_entries) + content[insert_pos:]
    
    # Check for and remove any top-level duplicate functions to ensure clean builds
    # These are often found at the end of the file after the closing brace of the main object
    for entry in new_entries:
        # Extract function name from entry
        match = re.search(r'fun\s+(\w+)\(\)', entry)
        if match:
            function_name = match.group(1)
            # Create pattern to find top-level duplicate functions
            duplicate_pattern = rf'@Composable\s+fun\s+{function_name}\(\)[^\n]+\n'
            # Remove these from the file content
            new_content = re.sub(duplicate_pattern, '', new_content)
    
    # Write the updated content
    with open(registry_path, 'w') as f:
        f.write(new_content)
    
    print(f"Added {len(new_entries)} new icons to {category} in {registry_path}")
    return True

def load_icons_from_json(json_path: str) -> Dict[str, List[str]]:
    """Load icon configuration from JSON file."""
    try:
        with open(json_path, 'r') as f:
            config = json.load(f)
            
        # Expected format: {"category_name": ["icon1", "icon2"], ...}
        # or simpler: ["icon1", "icon2", ...]
        if isinstance(config, list):
            return {"PlayerControls": config}
        elif isinstance(config, dict):
            return config
        else:
            raise ValueError("Invalid JSON format")
    except Exception as e:
        print(f"Error loading JSON: {e}")
        sys.exit(1)

def process_icons(icon_names: List[str], args) -> List[str]:
    """Download, convert and save icons."""
    successful_icons = []
    
    for icon_name in icon_names:
        print(f"Processing icon: {icon_name}")
        
        if args.dry_run:
            print(f"Would download {icon_name}")
            successful_icons.append(icon_name)
            continue
        
        # Download SVG directly
        svg_content = download_icon_svg(icon_name, args.style, args.version)
        if not svg_content:
            continue
        
        # Convert to Vector Drawable
        try:
            vector_content = convert_svg_to_vector_drawable(svg_content)
        
            # Save Vector Drawable
            output_path = save_vector_drawable(vector_content, icon_name, args.output)
            print(f"Saved {icon_name} to {output_path}")
            
            successful_icons.append(icon_name)
        except Exception as e:
            print(f"Error processing {icon_name}: {str(e)}")
    
    return successful_icons

def main():
    args = parse_arguments()
    
    # Determine which icons to download
    icons_to_process = []
    categories = {}
    
    if args.icon_name:
        icons_to_process = args.icon_name
        categories = {args.registry_category: icons_to_process}
    elif args.from_json:
        categories = load_icons_from_json(args.from_json)
        
        # Combine all icons into one list for processing
        for category_icons in categories.values():
            icons_to_process.extend(category_icons)
    
    # Process the icons
    successful_icons = process_icons(icons_to_process, args)
    
    # Update the icon registry if requested
    if args.update_registry and successful_icons and not args.dry_run:
        # First, remove any duplicated icon functions at the top level of the file
        # This helps prevent build errors from duplicate definitions
        if os.path.exists(args.icon_registry_path):
            with open(args.icon_registry_path, 'r') as f:
                content = f.read()
                
            # Look for top-level function definitions outside of object blocks
            # These are typically found at the end of the file and can cause build errors
            object_end_pos = content.rfind("}\n\n}")
            if object_end_pos != -1:
                # Everything after the main object closing brace should be cleaned up
                # Only preserve comments and package declarations
                clean_content = content[:object_end_pos+3]
                
                # Write the cleaned content back
                with open(args.icon_registry_path, 'w') as f:
                    f.write(clean_content)
                print(f"Cleaned up duplicated top-level functions in {args.icon_registry_path}")
            
        # Group successful icons by category
        for category, category_icons in categories.items():
            # Only include icons that were successfully processed
            icons_to_register = [icon for icon in category_icons if icon in successful_icons]
            if icons_to_register:
                update_icon_registry(icons_to_register, args.icon_registry_path, category)
    
    if successful_icons:
        print(f"Successfully processed {len(successful_icons)} icons")
    else:
        print("No icons were processed successfully")

if __name__ == "__main__":
    main()