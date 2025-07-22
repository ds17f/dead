# Dead Archive - Development Setup Guide

Complete setup instructions for developing the Dead Archive Android application.

## 🛠️ Prerequisites Installation

### 1. Install Java Development Kit (JDK 17+)
```bash
# Linux (Fedora/RHEL)
sudo dnf install java-17-openjdk-devel

# Linux (Debian/Ubuntu)
sudo apt install openjdk-17-jdk

# macOS with Homebrew
brew install openjdk@17

# Verify installation
java -version
```

### 2. Install Android Studio
```bash
# Linux - Download from https://developer.android.com/studio
# Or use flatpak:
flatpak install flathub com.google.AndroidStudio

# macOS with Homebrew  
brew install --cask android-studio
```

**First Launch Setup:**
1. Open Android Studio
2. Follow setup wizard to install Android SDK
3. Note your SDK path (usually `~/Android/Sdk` on Linux, `~/Library/Android/sdk` on macOS)

### 3. Set Environment Variables
Add to your `~/.bashrc`, `~/.zshrc`, or shell profile:

```bash
# Android SDK (Linux)
export ANDROID_HOME="$HOME/Android/Sdk"

# Android SDK (macOS)  
# export ANDROID_HOME="$HOME/Library/Android/sdk"

export PATH="$PATH:$ANDROID_HOME/emulator"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/tools"

# Java (Linux - usually auto-detected)
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"

# Java (macOS with Homebrew)
# export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

# Reload shell
source ~/.bashrc  # or ~/.zshrc
```

## 📱 Create Android Virtual Device (AVD)

### Via Android Studio (Recommended)
1. Open Android Studio
2. Go to **Tools > AVD Manager**
3. Click **Create Virtual Device**
4. Choose **Phone** category
5. Select **Pixel 7** (or similar modern device)
6. Choose **API 34** system image
7. Click **Next > Finish**

### Verify AVD Creation
```bash
# Should list your created AVD
make emu-list
```

## 🚀 Project Setup

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd dead-archive
```

### 2. Verify Build Environment  
```bash
# Check all tools are available
make status
```

### 3. Accept Android SDK Licenses
```bash
# Required for building
sdkmanager --licenses
# Type 'y' for each license
```

## 🎯 Development Workflow

### One-Command Setup (Recommended)
```bash
# Starts emulator, builds, installs, and launches app
make run-emulator
```

### Manual Step-by-Step
```bash
# 1. Build the project
make build

# 2. Start emulator
make emulator

# 3. Wait for emulator (check status)
make devices

# 4. Install and run app
make run

# 5. Stop emulator when done
make emu-stop
```

## 📋 Available Make Commands

### Essential Commands
- `make run-emulator` - Complete workflow: start emulator + build + install + launch
- `make build` - Build debug APK
- `make run` - Install and run app on connected device
- `make clean` - Clean build artifacts

### Emulator Management
- `make emulator` - Start Android emulator
- `make emu-list` - List available Android Virtual Devices
- `make devices` - Show connected devices and emulators
- `make emu-stop` - Stop all running emulators

### Development Tools
- `make test` - Run unit tests
- `make lint` - Run code quality checks
- `make logs` - Show app logs from device
- `make status` - Show project and environment status

### Build Variants
- `make release` - Build release APK
- `make bundle` - Build Android App Bundle (AAB)
- `make install-release` - Install release build

## 🐛 Troubleshooting

### "No connected devices" Error
```bash
# Check if emulator is running and ready
make devices

# If offline, wait for boot completion
# If not listed, start emulator
make emulator
```

### "SDK location not found" Error
```bash
# Set Android SDK path
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Or create local.properties file
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### "Gradle not found" Error
```bash
# Install Gradle
brew install gradle

# Or add to PATH if installed elsewhere
export PATH="$PATH:/path/to/gradle/bin"
```

### Emulator Won't Start
```bash
# Check available AVDs
make emu-list

# If none found, create one in Android Studio:
# Tools > AVD Manager > Create Virtual Device
```

### Build Fails with Version Conflicts
```bash
# Clean and rebuild
make clean
make build

# Check Java version (should be 17+)
java -version

# Check environment
make status
```

## 🔧 Project Architecture

### Module Structure
```
dead-archive/
├── app/                    # Main Android application
├── core/
│   ├── backup/            # Backup and restore functionality
│   ├── common/            # Shared utilities
│   ├── data/              # Repository implementations
│   ├── data-api/          # Repository interfaces
│   ├── database/          # Room database
│   ├── design/            # UI theme and components
│   ├── media/             # ExoPlayer integration
│   ├── media-api/         # Media player interfaces
│   ├── model/             # Data models
│   ├── network/           # Archive.org API
│   ├── settings/          # Settings implementation
│   └── settings-api/      # Settings interfaces
└── feature/
    ├── browse/            # Concert browsing
    ├── downloads/         # Download management
    ├── library/           # User library management
    ├── player/            # Audio player
    └── playlist/          # Playlist and recording selection
```

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + Kotlin Serialization
- **Media**: ExoPlayer/Media3
- **Build**: Gradle with Kotlin DSL

## 🚦 Development Best Practices

### Before Coding
```bash
# Always start with clean build
make clean build

# Verify tests pass
make test

# Check code quality
make lint
```

### Testing Changes
```bash
# Quick development cycle
make run-emulator   # Full workflow
# Make code changes
make build install  # Quick rebuild and install
```

### Before Committing
```bash
# Ensure everything works
make clean test lint build
```

## 🚀 CI/CD Pipeline

The project includes automated build and release workflows:

```bash
# Push code - triggers automatic build
git push origin main

# Create release - triggers APK distribution
git tag v1.0.0 && git push origin v1.0.0
```

📖 **[Complete CI/CD Guide](docs/development/CI-CD.md)** - Detailed workflow documentation

### Quick CI/CD Setup
1. Push code to GitHub repository
2. Workflows automatically activate
3. APKs built on every commit
4. Tagged versions create releases
5. Download APKs from GitHub Releases page

## 📚 Additional Resources

- **[CI/CD Documentation](docs/development/CI-CD.md)** - Complete automated build guide
- [Android Developer Documentation](https://developer.android.com/)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)
- [Archive.org API Documentation](https://archive.org/help/aboutapi.php)
- [Gradle Build Tool](https://gradle.org/guides/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

## 🆘 Getting Help

1. Check this setup guide
2. Run `make status` to diagnose environment issues
3. Check build logs: `gradle build --info`
4. Verify emulator: `make devices`
5. Review error messages in Android Studio

---

*Generated for Dead Archive v1.0 - Last updated: $(date)*