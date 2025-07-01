# Dead Archive Android App - Makefile
# Simplifies common development tasks

.PHONY: help build clean test lint install run run-emulator debug release tag-release dry-run-release setup deps check status logs capture-test-data clean-test-data view-test-data

# Default target
help:
	@echo "Dead Archive Android App - Available Commands:"
	@echo ""
	@echo "Setup & Dependencies:"
	@echo "  make setup       - Initial project setup and dependency installation"
	@echo "  make deps        - Download and install dependencies"
	@echo "  make download-icons - Download Material icons and update resources"
	@echo ""
	@echo "Build Commands:"
	@echo "  make build       - Build debug APK"
	@echo "  make release     - Build release APK"
	@echo "  make tag-release - Create release version, changelog, and build APK"
	@echo "  make dry-run-release - Test release process without making changes"
	@echo "  make clean       - Clean build artifacts"
	@echo ""
	@echo "Development:"
	@echo "  make install     - Install debug APK to connected device"
	@echo "  make run         - Install and run app on device"  
	@echo "  make run-emulator - Complete workflow: start emulator + build + install + launch"
	@echo "  make debug       - Build and install debug APK"
	@echo ""
	@echo "Quality Assurance:"
	@echo "  make test        - Run all unit tests"
	@echo "  make lint        - Run lint checks"
	@echo "  make check       - Run tests and lint"
	@echo ""
	@echo "Device & Emulator:"
	@echo "  make devices     - Show connected devices and emulators"
	@echo "  make emulator    - Start Android emulator (first available AVD)"
	@echo "  make emu-list    - List all available Android Virtual Devices"
	@echo "  make emu-stop    - Stop all running emulators"
	@echo "  make emu-cold-boot - Perform a cold boot of the emulator with virtual audio"
	@echo ""
	@echo "Test Data Management:"
	@echo "  make capture-test-data - Pull test data exported by debug screen"
	@echo "  make clean-test-data   - Clear app data and remove test data files"  
	@echo "  make view-test-data    - Show info about captured test data"
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
	gradle assembleDebug --console=plain 2>&1 | tee build-output.log
	@echo "✅ Debug APK built successfully! Output saved to build-output.log"
	@echo "📱 APK location: app/build/outputs/apk/debug/"

release:
	@echo "🔨 Building release APK..."
	gradle assembleRelease --console=plain 2>&1 | tee release-output.log
	@echo "✅ Release APK built successfully! Output saved to release-output.log"
	@echo "📱 APK location: app/build/outputs/apk/release/"

tag-release:
	@echo "🚀 Creating new release version..."
	@chmod +x ./scripts/release.sh
	@./scripts/release.sh
	@echo "✅ Release tagged and pushed! GitHub Actions will build the artifacts."

dry-run-release:
	@echo "🧪 Testing release process (dry run)..."
	@chmod +x ./scripts/release.sh
	@./scripts/release.sh --dry-run
	@echo "💡 To perform an actual release, use 'make tag-release'"

clean:
	@echo "🧹 Cleaning build artifacts..."
	gradle clean
	@echo "✅ Clean complete!"

# Development
install:
	@echo "📱 Installing debug APK to device..."
	gradle installDebug --console=plain 2>&1 | tee install-output.log
	@echo "✅ App installed! Output saved to install-output.log"

run: install
	@echo "🚀 Launching Dead Archive app..."
	adb shell am start -n com.deadarchive.app/.MainActivity
	@echo "✅ App launched!"

run-emulator:
	@echo "🎯 Starting complete emulator workflow..."
	@echo "1️⃣ Starting emulator..."
	@$(MAKE) --no-print-directory emu-start
	@echo ""
	@echo "2️⃣ Waiting for emulator to be ready..."
	@echo "⏳ This may take 30-60 seconds for first boot..."
	@for i in $$(seq 1 60); do \
		if adb devices 2>/dev/null | grep -q "emulator.*device$$"; then \
			echo "✅ Emulator is ready!"; \
			break; \
		fi; \
		if [ $$i -eq 60 ]; then \
			echo "❌ Timeout waiting for emulator"; \
			echo "💡 Try manually: make devices"; \
			exit 1; \
		fi; \
		printf "⏳ Waiting... ($$i/60)\r"; \
		sleep 2; \
	done
	@echo ""
	@echo "3️⃣ Building and installing Dead Archive app..."
	@$(MAKE) --no-print-directory build
	@$(MAKE) --no-print-directory install
	@echo ""
	@echo "4️⃣ Launching app..."
	@adb shell am start -n com.deadarchive.app/.MainActivity
	@echo ""
	@echo "🎉 SUCCESS! Dead Archive is now running on emulator!"
	@echo "📱 Use 'make emu-stop' when done"

debug: build install
	@echo "🐛 Debug build installed and ready!"

# Quality Assurance
test:
	@echo "🧪 Running unit tests..."
	gradle test --console=plain 2>&1 | tee test-output.log
	@echo "✅ Tests completed! Output saved to test-output.log"

lint:
	@echo "🔍 Running lint checks..."
	gradle lint --console=plain 2>&1 | tee lint-output.log
	@echo "✅ Lint checks completed! Output saved to lint-output.log"
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
.PHONY: devices install-release uninstall emulator emu-list emu-start emu-stop emu-cold-boot
devices:
	@echo "📱 Connected Android devices:"
	adb devices -l

install-release:
	@echo "📱 Installing release APK..."
	gradle installRelease

uninstall:
	@echo "🗑️ Uninstalling Dead Archive..."
	adb uninstall com.deadarchive.app || echo "App not installed"

# Emulator management
emu-list:
	@echo "📲 Available Android Virtual Devices:"
	@if command -v emulator >/dev/null 2>&1; then \
		emulator -list-avds; \
	elif [ -f "$$ANDROID_HOME/emulator/emulator" ]; then \
		$$ANDROID_HOME/emulator/emulator -list-avds; \
	elif [ -f "$$HOME/Library/Android/sdk/emulator/emulator" ]; then \
		$$HOME/Library/Android/sdk/emulator/emulator -list-avds; \
	elif [ -f "/usr/local/share/android-commandlinetools/emulator/emulator" ]; then \
		/usr/local/share/android-commandlinetools/emulator/emulator -list-avds; \
	else \
		echo "❌ Emulator not found. Searching common locations..."; \
		echo ""; \
		echo "Checking for Android SDK..."; \
		ls -la "$$HOME/Library/Android/sdk/emulator/" 2>/dev/null || echo "Not found: ~/Library/Android/sdk/emulator/"; \
		ls -la "$$ANDROID_HOME/emulator/" 2>/dev/null || echo "Not found: $$ANDROID_HOME/emulator/"; \
		echo ""; \
		echo "💡 To fix this:"; \
		echo "   1. Find your Android SDK path in Android Studio (Preferences > SDK Manager)"; \
		echo "   2. Add to your shell profile: export PATH=\$$PATH:\$$ANDROID_HOME/emulator"; \
		echo "   3. Or set ANDROID_HOME: export ANDROID_HOME=/path/to/your/android/sdk"; \
	fi

emulator: emu-start

emu-start:
	@echo "🚀 Starting Android emulator..."
	@EMULATOR_CMD=""; \
	if command -v emulator >/dev/null 2>&1; then \
		EMULATOR_CMD="emulator"; \
	elif [ -f "$$ANDROID_HOME/emulator/emulator" ]; then \
		EMULATOR_CMD="$$ANDROID_HOME/emulator/emulator"; \
	elif [ -f "$$HOME/Library/Android/sdk/emulator/emulator" ]; then \
		EMULATOR_CMD="$$HOME/Library/Android/sdk/emulator/emulator"; \
	elif [ -f "/usr/local/share/android-commandlinetools/emulator/emulator" ]; then \
		EMULATOR_CMD="/usr/local/share/android-commandlinetools/emulator/emulator"; \
	else \
		echo "❌ Emulator not found. Run 'make emu-list' for help."; \
		exit 1; \
	fi; \
	AVD_NAME=$$($$EMULATOR_CMD -list-avds 2>/dev/null | head -1); \
	if [ -z "$$AVD_NAME" ]; then \
		echo "❌ No AVDs available. Create one in Android Studio first."; \
		echo "💡 Android Studio > Tools > AVD Manager > Create Virtual Device"; \
		exit 1; \
	else \
		echo "📱 Starting emulator: $$AVD_NAME"; \
		$$EMULATOR_CMD -avd $$AVD_NAME -audio virtual -no-boot-anim & \
		echo "⏳ Emulator starting in background..."; \
		echo "🔍 Use 'make devices' to check when it's ready"; \
	fi

emu-stop:
	@echo "🛑 Stopping Android emulators..."
	@adb devices | grep emulator | cut -f1 | while read line; do adb -s $$line emu kill; done
	@echo "✅ All emulators stopped"

emu-cold-boot:
	@echo "❄️ Performing cold boot of Android emulator..."
	@EMULATOR_CMD=""; \
	if command -v emulator >/dev/null 2>&1; then \
		EMULATOR_CMD="emulator"; \
	elif [ -f "$$ANDROID_HOME/emulator/emulator" ]; then \
		EMULATOR_CMD="$$ANDROID_HOME/emulator/emulator"; \
	elif [ -f "$$HOME/Library/Android/sdk/emulator/emulator" ]; then \
		EMULATOR_CMD="$$HOME/Library/Android/sdk/emulator/emulator"; \
	elif [ -f "/usr/local/share/android-commandlinetools/emulator/emulator" ]; then \
		EMULATOR_CMD="/usr/local/share/android-commandlinetools/emulator/emulator"; \
	else \
		echo "❌ Emulator not found. Run 'make emu-list' for help."; \
		exit 1; \
	fi; \
	AVD_NAME=$$($$EMULATOR_CMD -list-avds 2>/dev/null | head -1); \
	if [ -z "$$AVD_NAME" ]; then \
		echo "❌ No AVDs available. Create one in Android Studio first."; \
		exit 1; \
	else \
		echo "📱 Stopping any running emulators..."; \
		adb devices | grep emulator | cut -f1 | while read line; do adb -s $$line emu kill; done; \
		sleep 2; \
		echo "📱 Cold booting emulator: $$AVD_NAME"; \
		$$EMULATOR_CMD -avd $$AVD_NAME -no-snapshot -audio virtual -no-boot-anim -verbose & \
		echo "⏳ Cold boot emulator starting in background..."; \
		echo "🔍 Use 'make devices' to check when it's ready"; \
	fi

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

# Material Icons Management
.PHONY: download-icons
download-icons:
	@echo "🔍 Downloading Material Icons..."
	@cd scripts && \
		python3 -m venv .venv || virtualenv .venv && \
		. .venv/bin/activate && \
		pip install --upgrade pip && \
		pip install -r requirements.txt && \
		python download_material_icons.py \
		--from-json "$(PWD)/scripts/material_icons_config.json" \
		--output "$(PWD)/core/design/src/main/res/drawable" \
		--icon-registry-path "$(PWD)/core/design/src/main/java/com/deadarchive/core/design/component/IconResources.kt" \
		--update-registry
	@echo "✅ Icons downloaded and processed!"

# Test Data Management
capture-test-data:
	@echo "📥 Capturing test data from device..."
	@echo "1️⃣ Make sure you've exported data using the Debug screen in the app"
	@echo "2️⃣ Pulling files from device..."
	@mkdir -p testdata
	@echo "   Trying app external files directory..."
	@adb pull /storage/emulated/0/Android/data/com.deadarchive.app.debug/files/testdata/ ./testdata/ 2>/dev/null || \
	 adb pull /storage/emulated/0/Android/data/com.deadarchive.app/files/testdata/ ./testdata/ 2>/dev/null || \
	 adb pull /sdcard/Android/data/com.deadarchive.app.debug/files/testdata/ ./testdata/ 2>/dev/null || \
	 adb pull /sdcard/Android/data/com.deadarchive.app/files/testdata/ ./testdata/ 2>/dev/null || \
	 echo "⚠️  No data found. Make sure to export data first using the Debug screen"
	@if [ -d "testdata/testdata" ]; then mv testdata/testdata/* testdata/ && rmdir testdata/testdata; fi
	@echo "✅ Test data captured to ./testdata/"
	@$(MAKE) --no-print-directory view-test-data

clean-test-data:
	@echo "🧹 Cleaning test data..."
	@adb shell pm clear com.deadarchive.app.debug || adb shell pm clear com.deadarchive.app || echo "⚠️  App not installed or ADB not connected"
	@rm -rf testdata/
	@echo "✅ Test data cleaned"

view-test-data:
	@echo "📊 Test Data Summary:"
	@if [ -d "testdata" ]; then \
		echo "  Directory: testdata/"; \
		if [ -f "testdata/complete_catalog_response.json" ]; then \
			echo "  Complete Catalog: $$(wc -l < testdata/complete_catalog_response.json 2>/dev/null || echo 0) lines, $$(du -h testdata/complete_catalog_response.json 2>/dev/null | cut -f1 || echo '0B') size"; \
		else \
			echo "  Complete Catalog: Not found"; \
		fi; \
		if [ -f "testdata/sample_metadata_responses.json" ]; then \
			echo "  Sample Metadata: $$(wc -l < testdata/sample_metadata_responses.json 2>/dev/null || echo 0) lines"; \
		else \
			echo "  Sample Metadata: Not found"; \
		fi; \
		echo "  All files:"; \
		ls -la testdata/ 2>/dev/null || echo "    (empty)"; \
	else \
		echo "  No test data directory found"; \
		echo "  💡 Use 'make capture-test-data' after exporting from Debug screen"; \
	fi