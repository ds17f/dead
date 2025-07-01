# Material Icons Downloader

This directory contains scripts for downloading Material Design icons, converting them to Android vector drawable format, and adding them to your project.

## Usage

### Using the Makefile (Recommended)

The simplest way to download icons is to use the Makefile target from the project root:

```bash
make download-icons
```

This will:
1. Create a Python virtual environment (.venv) in the scripts directory
2. Install all required dependencies
3. Run the script with the correct arguments
4. Save icons to the correct location in the project

### Manual Setup (Alternative)

If you need to run the script directly:

```bash
cd scripts
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
python download_material_icons.py --from-json material_icons_config.json
```

## Script Details

### Key Parameters:
- `--from-json PATH` - Path to the JSON config file with icon names
- `--output PATH` - Path to save the vector drawables (defaults to core/design/src/main/res/drawable)
- `--update-registry` - Update the IconResources.kt file with new icons
- `--icon-registry-path PATH` - Path to the IconResources.kt file

## Available Scripts

### 1. download_material_icons.py

This script attempts to download individual icons directly from Google's Material Design repository.

```bash
python download_material_icons.py --icon-name "skip_next" --icon-name "play_arrow"
python download_material_icons.py --from-json "material_icons_config.json"
```

### 2. download_material_icons_local.py (Recommended)

This script downloads the entire Material Design Icons repository as a ZIP file, extracts the needed icons, and converts them to Android vector drawables.

```bash
# Download and process icons from the config file
python download_material_icons_local.py --from-json material_icons_config.json

# Download specific icons
python download_material_icons_local.py --icon-name "skip_next" --icon-name "play_arrow"

# Use an existing repository download (faster for repeated use)
python download_material_icons_local.py --from-json material_icons_config.json --skip-download

# Update IconResources.kt with new icons
python download_material_icons_local.py --from-json material_icons_config.json --update-registry
```

## Configuration File Format

The JSON configuration file (`material_icons_config.json`) can have two formats:

1. Simple list of icons (will be added to the PlayerControls category):
```json
["play_arrow", "pause", "skip_next", "skip_previous"]
```

2. Organized by categories:
```json
{
  "PlayerControls": ["play_arrow", "pause", "skip_next"],
  "Navigation": ["arrow_back", "close"]
}
```

## Integration with Gradle

Add to your module's build.gradle.kts:

```kotlin
tasks.register<Exec>("downloadMaterialIcons") {
    commandLine("python", "${projectDir}/scripts/download_material_icons_local.py", 
                "--from-json", "${projectDir}/scripts/material_icons_config.json",
                "--skip-download")
    description = "Downloads and converts required Material Design icons"
}

tasks.preBuild {
    dependsOn("downloadMaterialIcons")
}
```