#!/bin/bash

# Download Monitoring Script for Dead Archive App
# This script helps monitor download activity in real-time

echo "🎵 Dead Archive Download Monitor"
echo "==============================="
echo "Monitoring download activity in real-time..."
echo "Press Ctrl+C to stop"
echo ""

# Monitor download-related logcat output with color highlighting
adb logcat | grep --line-buffered -E "(Download|Queue|Worker|Audio)" | while read line; do
    # Color coding for different types of messages
    if [[ $line == *"ERROR"* ]] || [[ $line == *"FAILED"* ]]; then
        echo -e "\033[31m$line\033[0m"  # Red for errors
    elif [[ $line == *"SUCCESS"* ]] || [[ $line == *"COMPLETED"* ]]; then
        echo -e "\033[32m$line\033[0m"  # Green for success
    elif [[ $line == *"DOWNLOADING"* ]] || [[ $line == *"PROGRESS"* ]]; then
        echo -e "\033[33m$line\033[0m"  # Yellow for progress
    elif [[ $line == *"QUEUED"* ]] || [[ $line == *"STARTED"* ]]; then
        echo -e "\033[36m$line\033[0m"  # Cyan for queue activity
    else
        echo "$line"  # Normal color for other messages
    fi
done