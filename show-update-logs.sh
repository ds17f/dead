#!/bin/bash

echo "🔍 Monitoring update check logs..."
echo "📱 Go to Settings → App Updates and press 'Check Now'"
echo "💡 Logs will appear below in real-time:"
echo ""
echo "----------------------------------------"

# Filter for update-related logs
adb logcat -s UpdateServiceImpl SettingsViewModel | grep -E "(UpdateServiceImpl|SettingsViewModel)" --line-buffered