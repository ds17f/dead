# Dead Archive Release Build Guide

This guide explains how to build release versions of the Dead Archive Android app.

## Prerequisites

1. **Android SDK** - Ensure you have the Android SDK installed
2. **Java 17+** - Required for building
3. **Signing Key** - You'll need to create a release keystore (see below)

## 1. Create Release Keystore

First, you need to create a keystore for signing your release APK:

```bash
# Navigate to your project root
cd /path/to/deadarchive

# Create a keystore (replace values with your information)
keytool -genkey -v -keystore release.keystore -alias dead-archive-key -keyalg RSA -keysize 2048 -validity 10000

# You'll be prompted for:
# - Keystore password (remember this!)
# - Key password (remember this!)
# - Your name, organization, city, state, country
```

**⚠️ IMPORTANT**: 
- Keep your keystore file safe and backed up
- Remember your passwords - you'll need them for every release
- Never commit the keystore to version control

## 2. Set Up Signing Configuration

Create a `gradle.properties` file in your project root (or use environment variables):

```properties
# Release signing configuration
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=dead-archive-key
RELEASE_KEY_PASSWORD=your_key_password
```

**⚠️ SECURITY**: Never commit `gradle.properties` with real passwords to version control!

### Alternative: Environment Variables

Instead of `gradle.properties`, you can set environment variables:

```bash
export RELEASE_STORE_FILE=release.keystore
export RELEASE_STORE_PASSWORD=your_keystore_password
export RELEASE_KEY_ALIAS=dead-archive-key
export RELEASE_KEY_PASSWORD=your_key_password
```

## 3. Build Release APK

### Option A: Command Line

```bash
# Clean and build release APK
./gradlew clean assembleRelease

# The APK will be generated at:
# app/build/outputs/apk/release/app-release.apk
```

### Option B: Android Studio

1. Open the project in Android Studio
2. Go to **Build > Generate Signed Bundle / APK**
3. Choose **APK**
4. Select your keystore file and enter passwords
5. Choose **release** build variant
6. Click **Finish**

## 4. Build Release AAB (Recommended for Play Store)

```bash
# Build Android App Bundle (AAB) for Play Store
./gradlew clean bundleRelease

# The AAB will be generated at:
# app/build/outputs/bundle/release/app-release.aab
```

## 5. Verify Release Build

Before distributing, verify your release build:

```bash
# Check APK info
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Verify signing
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Check APK size and contents
ls -lh app/build/outputs/apk/release/app-release.apk
```

## 6. Release Checklist

Before releasing, ensure:

- [ ] Version code incremented in `app/build.gradle.kts`
- [ ] Version name updated (e.g., "1.1.0")
- [ ] All tests pass: `./gradlew test`
- [ ] ProGuard rules don't break functionality
- [ ] App functions correctly on different devices/Android versions
- [ ] Archive.org API integration works
- [ ] Network permissions are properly configured
- [ ] No debug code or logs in release build

## 7. Build Variants

The app supports multiple build variants:

- **debug**: Development build with debugging enabled
  - Application ID: `com.deadarchive.app.debug`
  - Debuggable: Yes
  - Obfuscation: No

- **release**: Production build for distribution
  - Application ID: `com.deadarchive.app`
  - Debuggable: No
  - Obfuscation: Yes (ProGuard/R8)
  - Signing: Required

- **benchmark**: Performance testing build
  - Based on release configuration
  - Used for performance profiling

## 8. Release Optimization Features

The release build includes:

- **Code Shrinking**: Removes unused code with R8
- **Resource Shrinking**: Removes unused resources
- **Code Obfuscation**: Makes reverse engineering harder
- **Optimized Dependencies**: Reduces APK size
- **Crash Reporting**: Line numbers preserved for stack traces

## 9. Distribution

### Google Play Store
1. Upload the AAB file to Google Play Console
2. Fill out store listing information
3. Set up pricing and distribution
4. Submit for review

### Direct Distribution
1. Use the APK file for direct installation
2. Users need to enable "Unknown Sources" in Android settings
3. Consider using Firebase App Distribution for beta testing

## 10. Version Management

Update these values in `app/build.gradle.kts` for each release:

```kotlin
defaultConfig {
    versionCode = 2        // Increment for each release
    versionName = "1.1.0"  // Semantic versioning (MAJOR.MINOR.PATCH)
}
```

## 11. Troubleshooting

### Build Fails with ProGuard Errors
- Check `app/build/outputs/mapping/release/usage.txt` for removed code
- Add specific `-keep` rules to `proguard-rules.pro`
- Test thoroughly after adding ProGuard rules

### Keystore Issues
- Ensure keystore file path is correct
- Verify passwords are correct
- Check that keystore is accessible by build system

### API Issues in Release
- Archive.org API models are protected from obfuscation
- Network security config allows HTTP connections to archive.org
- Custom JSON serializers are preserved

## 12. Security Best Practices

- Never commit keystores or passwords to version control
- Use different keystores for debug and release
- Keep backup copies of release keystore in secure location
- Consider using Android App Signing by Google Play
- Regularly update dependencies for security patches

## 13. Automated Builds

For CI/CD, you can automate release builds:

```yaml
# Example GitHub Actions workflow
- name: Build Release APK
  run: ./gradlew assembleRelease
  env:
    RELEASE_STORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    RELEASE_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

Store sensitive values as repository secrets, never in code.