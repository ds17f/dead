# 🎸 Dead Archive - Build Instructions 🎸

## Current Issue: Gradle Wrapper Corrupted

The Gradle wrapper (`./gradlew`) is currently corrupted and needs to be fixed. Here are the solutions:

## ⚡ Quick Solution (Recommended)

### Option 1: Fix Gradle Wrapper
```bash
# Delete corrupted wrapper
rm gradle/wrapper/gradle-wrapper.jar

# Use system gradle to regenerate wrapper
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
gradle wrapper --gradle-version=8.7

# Test the wrapper
./gradlew --version
```

### Option 2: Use System Gradle Directly
```bash
# Set up Java environment
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

# Clean and build with system gradle
gradle clean
gradle assembleDebug        # For debug APK
gradle assembleRelease      # For release APK (needs signing setup)
gradle bundleRelease        # For release AAB (needs signing setup)
```

## 🔐 Release Build Setup

### 1. Create Keystore (First Time Only)
```bash
keytool -genkey -v -keystore release.keystore -alias dead-archive-key -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Create gradle.properties (First Time Only)
```bash
# Copy template and fill in your passwords
cp gradle.properties.template gradle.properties

# Edit gradle.properties with your actual keystore passwords:
# RELEASE_STORE_FILE=release.keystore
# RELEASE_STORE_PASSWORD=your_keystore_password
# RELEASE_KEY_ALIAS=dead-archive-key  
# RELEASE_KEY_PASSWORD=your_key_password
```

### 3. Build Release
```bash
# Using system gradle (if wrapper is broken)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
gradle clean
gradle assembleRelease    # Creates APK at app/build/outputs/apk/release/
gradle bundleRelease      # Creates AAB at app/build/outputs/bundle/release/

# Or using wrapper (once fixed)
./gradlew clean
./gradlew assembleRelease
./gradlew bundleRelease
```

## 📱 Build Outputs

**Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- For development and testing
- No signing required
- Larger size, not optimized

**Release APK**: `app/build/outputs/apk/release/app-release.apk`  
- For direct distribution
- Signed and optimized
- Smaller size with ProGuard

**Release AAB**: `app/build/outputs/bundle/release/app-release.aab`
- For Google Play Store
- Even smaller with dynamic delivery

## 🧪 Testing Builds

```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install release APK  
adb install -r app/build/outputs/apk/release/app-release.apk

# Check APK info
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

## ⚠️ Current Gradle Issues

The project has these Gradle-related issues:

1. **Corrupted Wrapper JAR**: `gradle/wrapper/gradle-wrapper.jar` is truncated
2. **Daemon Conflicts**: Multiple Gradle daemons are conflicting  
3. **Java Home**: Environment needs `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64`

## 🔧 Troubleshooting

### Fix Gradle Daemons
```bash
# Stop all gradle daemons
gradle --stop

# Check daemon status  
gradle --status

# Clean gradle cache if needed
rm -rf ~/.gradle/caches
```

### Alternative Build Methods

**Android Studio**: 
- Open project in Android Studio
- Build → Generate Signed Bundle/APK
- Follow the signing wizard

**Manual Assembly**:
- Build modules individually if full build fails
- Check specific error logs in `build/` directories

## 🎯 What's Working

✅ **Project Structure**: All modules properly configured  
✅ **Dependencies**: All libraries properly declared  
✅ **API Integration**: Archive.org networking layer complete  
✅ **Build Configuration**: Release signing and optimization configured  
✅ **ProGuard Rules**: Custom rules for kotlinx.serialization and API models  

## 🚀 Next Steps

1. **Fix Gradle Wrapper**: Use system gradle to regenerate wrapper
2. **Test Build**: Verify debug and release builds work
3. **Sign Release**: Set up keystore and gradle.properties  
4. **CI/CD Setup**: Configure GitHub Actions for automated builds

The app is ready for release builds once the Gradle wrapper issue is resolved! 🎸