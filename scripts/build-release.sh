#!/bin/bash

# Dead Archive Release Build Script
# This script helps build release versions of the app

set -e  # Exit on any error

echo "🎸 Dead Archive Release Build Script 🎸"
echo "======================================"

# Check if we're in the right directory
if [ ! -f "app/build.gradle.kts" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

# Check if keystore exists
if [ ! -f "release.keystore" ]; then
    echo "⚠️  Release keystore not found. Creating one now..."
    echo "You'll need to provide some information for the keystore:"
    keytool -genkey -v -keystore release.keystore -alias dead-archive-key -keyalg RSA -keysize 2048 -validity 10000
    echo "✅ Keystore created successfully!"
    echo ""
fi

# Check if gradle.properties exists with signing config
if [ ! -f "gradle.properties" ] || ! grep -q "RELEASE_STORE_FILE" gradle.properties; then
    echo "⚠️  Setting up gradle.properties for signing..."
    echo "Please enter your keystore password:"
    read -s KEYSTORE_PASS
    echo "Please enter your key password:"
    read -s KEY_PASS
    
    cat >> gradle.properties << EOF

# Release signing configuration (DO NOT COMMIT TO VERSION CONTROL)
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=$KEYSTORE_PASS
RELEASE_KEY_ALIAS=dead-archive-key
RELEASE_KEY_PASSWORD=$KEY_PASS
EOF
    echo "✅ Signing configuration added to gradle.properties"
    echo ""
fi

# Set up Java environment
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

# Choose Gradle command (use system gradle if wrapper is broken)
if ./gradlew --version >/dev/null 2>&1; then
    GRADLE_CMD="./gradlew"
    echo "✅ Using Gradle wrapper"
else
    echo "⚠️  Gradle wrapper not working, using system gradle"
    GRADLE_CMD="gradle"
fi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
$GRADLE_CMD clean

# Run tests
echo "🧪 Running tests..."
$GRADLE_CMD test

# Build release APK
echo "📦 Building release APK..."
$GRADLE_CMD assembleRelease

# Build release AAB (for Play Store)
echo "📦 Building release AAB..."
$GRADLE_CMD bundleRelease

# Verify the build
APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo "✅ Release APK built successfully!"
    echo "   📍 Location: $APK_PATH"
    echo "   📏 Size: $APK_SIZE"
else
    echo "❌ APK build failed!"
    exit 1
fi

if [ -f "$AAB_PATH" ]; then
    AAB_SIZE=$(ls -lh "$AAB_PATH" | awk '{print $5}')
    echo "✅ Release AAB built successfully!"
    echo "   📍 Location: $AAB_PATH"
    echo "   📏 Size: $AAB_SIZE"
else
    echo "❌ AAB build failed!"
    exit 1
fi

# Verify signing
echo "🔐 Verifying APK signature..."
if jarsigner -verify "$APK_PATH" > /dev/null 2>&1; then
    echo "✅ APK is properly signed!"
else
    echo "❌ APK signature verification failed!"
    exit 1
fi

# Show APK info
echo ""
echo "📋 APK Information:"
echo "=================="
aapt dump badging "$APK_PATH" | grep -E "(package|application-label|uses-permission)"

echo ""
echo "🎉 Release build completed successfully!"
echo ""
echo "Next steps:"
echo "1. Test the APK on different devices"
echo "2. Upload AAB to Google Play Console (for Play Store)"
echo "3. Or distribute APK directly to users"
echo ""
echo "Files generated:"
echo "- APK: $APK_PATH"
echo "- AAB: $AAB_PATH"
echo ""
echo "🎸 Keep on truckin'! 🎸"