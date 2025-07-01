# Material Icon Utility Quickstart

This guide provides a quick overview of how to set up and use the Material Icon Utility in your project.

## Setup

1. **Add Material Icons Extended Dependency**

   In your app's `build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation("androidx.compose.material:material-icons-extended")
   }
   ```

2. **Set Up the Icon Download Script**

   ```bash
   # Install dependencies
   cd scripts
   pip install -r requirements.txt
   
   # Download initial icon set
   python download_material_icons.py --from-json material_icons_config.json
   ```

3. **Configure Gradle Integration** (Optional)

   In your module's `build.gradle.kts`:

   ```kotlin
   tasks.register<Exec>("downloadMaterialIcons") {
       commandLine("python", "${projectDir}/scripts/download_material_icons.py", 
                   "--from-json", "${projectDir}/scripts/material_icons_config.json")
       description = "Downloads and converts required Material Design icons"
   }

   tasks.preBuild {
       dependsOn("downloadMaterialIcons")
   }
   ```

## Quick Examples

### Basic Icon Usage

```kotlin
import com.deadarchive.core.design.component.IconResources

// In your composable
Icon(
    imageVector = IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.MEDIUM)
)
```

### Conditional Icons

```kotlin
Icon(
    imageVector = if (isSelected) {
        IconResources.Navigation.Home
    } else {
        IconResources.Navigation.HomeOutlined
    },
    contentDescription = "Home"
)
```

### Getting Missing Icons

If you need an icon that's not in the standard set:

```bash
# Get a specific icon
python scripts/download_material_icons.py --icon-name "replay_30" --update-registry

# Get multiple icons
python scripts/download_material_icons.py --icon-name "replay_30" --icon-name "replay_5" --update-registry
```

### Creating Icon Config File

Create a `custom_icons.json` file:

```json
{
  "PlayerControls": [
    "replay_5",
    "replay_10",
    "replay_30"
  ],
  "Navigation": [
    "arrow_upward",
    "arrow_downward"
  ]
}
```

Then download all icons at once:

```bash
python scripts/download_material_icons.py --from-json custom_icons.json
```

## Next Steps

- Review the full [Material Icons Utility Documentation](material-icons-utility.md)
- Check out detailed [Icon Usage Examples](icon-usage-examples.md)
- Explore the [Material Design Icon Library](https://fonts.google.com/icons) for available icons