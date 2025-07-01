# Material Icons Utility Documentation

## Overview

The Material Icons Utility is a comprehensive solution for managing Material Design icons in the DeadArchive Android application. It provides a centralized resource for accessing both standard Material Design icons and custom icons, ensuring consistent visual language throughout the application.

## Components

### 1. IconResources Class

A Kotlin object that serves as a central registry for all icons used in the application:

```kotlin
object IconResources {
    object Navigation { /* Navigation icons */ }
    object PlayerControls { /* Media player icons */ }
    object Status { /* Status and notification icons */ }
    
    // Custom icon accessor methods
    @Composable
    fun customIcon(@DrawableRes resId: Int): Painter
}
```

### 2. Material Icon Download Script

A Python script (`download_material_icons.py`) that:
- Downloads Material Design SVG icons from Google's repository
- Converts them to Android vector drawable format
- Places them in the correct resource directories
- Updates the IconResources class with new entries

## Setup Instructions

### Adding the Material Icons Extended Library

In your module's `build.gradle.kts` file:

```kotlin
dependencies {
    // Add Material Icons Extended library
    implementation("androidx.compose.material:material-icons-extended")
}
```

### Running the Icon Download Script

```bash
# Basic usage
python scripts/download_material_icons.py --icon-name "skip_next" --icon-name "volume_up"

# Download an entire category
python scripts/download_material_icons.py --category "av"

# Output directory can be specified
python scripts/download_material_icons.py --icon-name "album" --output "core/design/src/main/res/drawable"

# Automatically update IconResources.kt with new entries
python scripts/download_material_icons.py --icon-name "volume_up" --update-registry
```

### Integration with Build Process

The script can be integrated with your build process using Gradle tasks:

```kotlin
// In build.gradle.kts
tasks.register<Exec>("downloadMaterialIcons") {
    commandLine("python", "scripts/download_material_icons.py", "--from-json", "material_icons_config.json")
    description = "Downloads and converts required Material Design icons"
}

tasks.preBuild {
    dependsOn("downloadMaterialIcons")
}
```

## Usage Examples

### Using Standard Material Icons

```kotlin
import com.deadarchive.core.design.component.IconResources

// In your Composable
Icon(
    imageVector = IconResources.PlayerControls.Play,
    contentDescription = "Play",
    tint = MaterialTheme.colorScheme.onSurface
)
```

### Using Custom Icons

```kotlin
// For custom icons loaded from drawable resources
Icon(
    painter = IconResources.customIcon(R.drawable.custom_icon_name),
    contentDescription = "Custom Icon",
    tint = MaterialTheme.colorScheme.primary
)
```

### Ensuring Consistent Icon Sizes

```kotlin
// Using predefined sizes
Icon(
    imageVector = IconResources.PlayerControls.Play,
    contentDescription = "Play",
    modifier = Modifier.size(IconResources.Size.MEDIUM.dp)
)
```

## Best Practices

1. **Always use the IconResources class** instead of direct references to Icons.Default.*
2. **Keep related icons together** in appropriate nested objects within IconResources
3. **Maintain naming consistency** between icon resources and their references
4. **Add new icons to IconResources** whenever introducing them to the application
5. **Use contentDescription** for all icons to ensure accessibility
6. **Apply consistent sizing** using the IconResources.Size constants

## Troubleshooting

### Common Issues

1. **Icons not appearing**: Ensure the icon name is correct and the resource exists
2. **SVG conversion issues**: Some complex SVG files may not convert properly; simplify them before conversion
3. **Material Icons Extended dependency**: The extended icon set significantly increases APK size; use ProGuard/R8 to remove unused icons

### Adding Missing Icons

If you find a missing icon:

1. Check if it's available in the Material Icons Extended library
2. If not, use the download script to fetch it from Google's repository
3. If the icon is custom, add it to the appropriate drawable resources folder
4. Update the IconResources class with a reference to the new icon

## Maintenance

The Material Icons Utility should be maintained as follows:

1. **Regular updates**: When new Material Design icon sets are released
2. **New categories**: Add new nested objects within IconResources as needed
3. **Unused icons**: Periodically audit and remove unused icons
4. **Code organization**: Keep the IconResources class organized by functional categories