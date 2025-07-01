# Material Icons Downloader

This directory contains scripts for downloading Material Design icons, converting them to Android vector drawable format, and adding them to your project.

## Prerequisites

Install required dependencies:

```bash
pip install -r requirements.txt
```

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