#!/bin/bash

echo "🔧 Dead Archive - Manual Build Script"
echo "====================================="
echo ""

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ ANDROID_HOME not set"
    echo "ℹ️  Please set ANDROID_HOME to your Android SDK path"
    echo "   Example: export ANDROID_HOME=/path/to/android-sdk"
    exit 1
fi

if [ -z "$JAVA_HOME" ]; then
    echo "⚠️  JAVA_HOME not set, using current Java installation"
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

echo "✅ Java: $($JAVA_CMD -version 2>&1 | head -1)"
echo "✅ Android SDK: $ANDROID_HOME"
echo ""

# Check for gradle installation
GRADLE_CMD=""
if command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
    echo "✅ Using system Gradle: $(gradle --version | head -1)"
elif [ -f "$HOME/.gradle/wrapper/dists/gradle-8.2/bin/gradle" ]; then
    GRADLE_CMD="$HOME/.gradle/wrapper/dists/gradle-8.2/bin/gradle"
    echo "✅ Using Gradle from wrapper cache"
else
    echo "❌ No Gradle installation found"
    echo ""
    echo "🛠️  Manual Setup Required:"
    echo "1. Install Gradle 8.2+ manually"
    echo "2. Or use Android Studio to build the project"
    echo "3. Or download Gradle wrapper manually"
    echo ""
    echo "📥 Download Gradle: https://gradle.org/releases/"
    exit 1
fi

echo ""
echo "🔨 Building Dead Archive APK..."
echo ""

# Build the project
$GRADLE_CMD assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo "📱 APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # Check if ADB is available for installation
    if command -v adb &> /dev/null; then
        echo "🔌 Connected devices:"
        adb devices
        echo ""
        echo "📲 To install: adb install app/build/outputs/apk/debug/app-debug.apk"
    else
        echo "ℹ️  Install ADB to deploy to device automatically"
    fi
else
    echo ""
    echo "❌ Build failed"
    echo "🔍 Check the error messages above"
fi