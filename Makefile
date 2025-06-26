# Dead Archive Android App - Makefile
# Simplifies common development tasks

.PHONY: help build clean test lint install run debug release setup deps check status logs

# Default target
help:
	@echo "Dead Archive Android App - Available Commands:"
	@echo ""
	@echo "Setup & Dependencies:"
	@echo "  make setup       - Initial project setup and dependency installation"
	@echo "  make deps        - Download and install dependencies"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build       - Build debug APK"
	@echo "  make release     - Build release APK"
	@echo "  make clean       - Clean build artifacts"
	@echo ""
	@echo "Development:"
	@echo "  make install     - Install debug APK to connected device"
	@echo "  make run         - Install and run app on device"
	@echo "  make debug       - Build and install debug APK"
	@echo ""
	@echo "Quality Assurance:"
	@echo "  make test        - Run all unit tests"
	@echo "  make lint        - Run lint checks"
	@echo "  make check       - Run tests and lint"
	@echo ""
	@echo "Utilities:"
	@echo "  make status      - Show project and device status"
	@echo "  make logs        - Show app logs from connected device"
	@echo "  make modules     - List all project modules"
	@echo "  make deps-tree   - Show dependency tree"
	@echo ""
	@echo "Cleanup:"
	@echo "  make deep-clean  - Clean everything including Gradle cache"
	@echo "  make reset       - Reset project to clean state"

# Setup and Dependencies
setup:
	@echo "🚀 Setting up Dead Archive project..."
	gradle --version
	@echo "✅ Project setup complete!"

deps:
	@echo "📦 Installing dependencies..."
	gradle build --refresh-dependencies
	@echo "✅ Dependencies installed!"

# Build Commands
build:
	@echo "🔨 Building debug APK..."
	gradle assembleDebug
	@echo "✅ Debug APK built successfully!"
	@echo "📱 APK location: app/build/outputs/apk/debug/"

release:
	@echo "🔨 Building release APK..."
	gradle assembleRelease
	@echo "✅ Release APK built successfully!"
	@echo "📱 APK location: app/build/outputs/apk/release/"

clean:
	@echo "🧹 Cleaning build artifacts..."
	gradle clean
	@echo "✅ Clean complete!"

# Development
install:
	@echo "📱 Installing debug APK to device..."
	gradle installDebug
	@echo "✅ App installed!"

run: install
	@echo "🚀 Launching Dead Archive app..."
	adb shell am start -n com.deadarchive.app/.MainActivity
	@echo "✅ App launched!"

debug: build install
	@echo "🐛 Debug build installed and ready!"

# Quality Assurance
test:
	@echo "🧪 Running unit tests..."
	gradle test
	@echo "✅ Tests completed!"

lint:
	@echo "🔍 Running lint checks..."
	gradle lint
	@echo "✅ Lint checks completed!"
	@echo "📊 Lint report: app/build/reports/lint-results.html"

check: test lint
	@echo "✅ All quality checks completed!"

# Utilities
status:
	@echo "📊 Dead Archive Project Status:"
	@echo ""
	@echo "Gradle Version:"
	@gradle --version | head -5
	@echo ""
	@echo "Connected Devices:"
	@adb devices || echo "❌ ADB not available"
	@echo ""
	@echo "Project Structure:"
	@find . -name "build.gradle.kts" | head -5
	@echo ""
	@echo "Last Build Artifacts:"
	@find . -name "*.apk" -newer . 2>/dev/null | head -3 || echo "No recent APK builds found"

logs:
	@echo "📱 Showing Dead Archive app logs..."
	@echo "Press Ctrl+C to stop"
	adb logcat | grep -i "deadarchive\|grateful\|dead"

modules:
	@echo "📦 Dead Archive Modules:"
	@echo ""
	@echo "Core Modules:"
	@find core -name "build.gradle.kts" -exec dirname {} \; | sort
	@echo ""
	@echo "Feature Modules:"
	@find feature -name "build.gradle.kts" -exec dirname {} \; | sort
	@echo ""
	@echo "App Module:"
	@echo "app"

deps-tree:
	@echo "🌳 Dependency Tree:"
	gradle app:dependencies --configuration debugRuntimeClasspath

# Advanced Cleanup
deep-clean:
	@echo "🧹 Deep cleaning project..."
	gradle clean
	rm -rf .gradle/
	rm -rf build/
	rm -rf */build/
	rm -rf */*/build/
	@echo "✅ Deep clean complete!"

reset: deep-clean
	@echo "🔄 Resetting project to clean state..."
	gradle build --refresh-dependencies
	@echo "✅ Project reset complete!"


# Build variants
.PHONY: apk bundle
apk: build
	@echo "📱 Debug APK: app/build/outputs/apk/debug/app-debug.apk"

bundle:
	@echo "📦 Building Android App Bundle..."
	gradle bundleRelease
	@echo "✅ Bundle built: app/build/outputs/bundle/release/app-release.aab"

# Device management
.PHONY: devices install-release uninstall
devices:
	@echo "📱 Connected Android devices:"
	adb devices -l

install-release:
	@echo "📱 Installing release APK..."
	gradle installRelease

uninstall:
	@echo "🗑️ Uninstalling Dead Archive..."
	adb uninstall com.deadarchive.app || echo "App not installed"

# Performance and analysis
.PHONY: profile analyze size
profile:
	@echo "📊 Building with profiling enabled..."
	gradle assembleDebug -Pandroid.enableProfileJson=true

analyze:
	@echo "🔍 Running dependency analysis..."
	gradle buildHealth

size:
	@echo "📏 Analyzing APK size..."
	gradle analyzeDebugBundle || gradle assembleDebug
	@find . -name "*.apk" -exec ls -lh {} \;

# Documentation
.PHONY: docs
docs:
	@echo "📚 Generating documentation..."
	gradle dokkaHtml || echo "Dokka not configured"
	@echo "📖 Opening README..."
	@cat README.md | head -20