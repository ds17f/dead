#!/bin/bash
# Dead Archive logging utility script
# Usage: ./scripts/logs.sh [view_type] [timeout_seconds]

# Default timeout of 10 seconds, or use second parameter
TIMEOUT=${2:-10}

# Set up signal handling for Ctrl+C
cleanup() {
  echo ""
  echo "🛑 Interrupted by user"
  kill $! 2>/dev/null
  exit 0
}
trap cleanup INT

# Function to run command with optional timeout that preserves Ctrl+C
run_with_timeout() {
  if [ "$TIMEOUT" -eq 0 ]; then
    # No timeout - run indefinitely, but allow Ctrl+C
    "$@" &
    wait $!
  else
    # Run with timeout in background, allow Ctrl+C
    "$@" &
    local pid=$!

    # Wait for either timeout or process completion
    (
      sleep ${TIMEOUT}
      kill $pid 2>/dev/null
    ) &
    local timeout_pid=$!

    wait $pid 2>/dev/null
    kill $timeout_pid 2>/dev/null
  fi
}

case "$1" in
"error" | "errors")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing Dead Archive error logs (no timeout)..."
  else
    echo "🔍 Showing Dead Archive error logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s "*:E" | grep -E "(DeadArchive|V2|Dead|DEAD)"
  ;;
"dataimport" | "v2import")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing V2 database import logs (no timeout)..."
  else
    echo "🔍 Showing V2 database import logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s AssetManagerV2 DataImportServiceV2 DatabaseManagerV2
  ;;
"v2" | "v2db")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing all V2 database logs (no timeout)..."
  else
    echo "🔍 Showing all V2 database logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat | grep -E ".*Database.*V2|.*Import.*V2|.*Asset.*V2|.*Manager.*V2"
  ;;
"app" | "application")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing application startup logs (no timeout)..."
  else
    echo "🔍 Showing application startup logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s DeadArchiveApplication
  ;;
"player" | "media")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing media player logs (no timeout)..."
  else
    echo "🔍 Showing media player logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s PlaybackStateSync MediaControllerRepository QueueManager PlaybackCommandProcessor
  ;;
"settings")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing settings logs (no timeout)..."
  else
    echo "🔍 Showing settings logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s SettingsViewModel SettingsConfigurationService
  ;;
"debug")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Showing debug panel logs (no timeout)..."
  else
    echo "🔍 Showing debug panel logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat -s DEAD_DEBUG_PANEL
  ;;
"clear")
  adb logcat -c
  echo "✅ Logs cleared"
  ;;
"tail" | "follow")
  if [ "$TIMEOUT" -eq 0 ]; then
    echo "🔍 Following all Dead Archive logs (no timeout)..."
  else
    echo "🔍 Following all Dead Archive logs (${TIMEOUT}s)..."
  fi
  run_with_timeout adb logcat | grep -E "(DeadArchive|V2|DEAD|Dead)"
  ;;
"import-summary")
  echo "📊 V2 Import Summary (last run):"
  adb logcat -d | grep -E "V2.*Import|shows.*venues" | tail -10
  ;;
*)
  echo "📱 Dead Archive Logging Utility"
  echo ""
  echo "Usage: $0 [view_type] [timeout_seconds]"
  echo ""
  echo "📋 Available log views:"
  echo "  error         - Show all error logs for Dead Archive components"
  echo "  dataimport    - Show V2 database import logs"
  echo "  v2            - Show all V2 database related logs"
  echo "  app           - Show application startup logs"
  echo "  player        - Show media player logs"
  echo "  settings      - Show settings related logs"
  echo "  debug         - Show debug panel logs"
  echo "  tail/follow   - Follow all Dead Archive logs in real-time"
  echo "  import-summary- Show V2 import summary from last run"
  echo "  clear         - Clear log buffer"
  echo ""
  echo "⏱️  Timeout options:"
  echo "  Default: 10 seconds"
  echo "  0       - No timeout (run indefinitely)"
  echo "  N       - Run for N seconds"
  echo ""
  echo "💡 Examples:"
  echo "  ./scripts/logs.sh error         # Run for 10s (default)"
  echo "  ./scripts/logs.sh error 5      # Run for 5 seconds"
  echo "  ./scripts/logs.sh error 0      # Run indefinitely"
  echo "  ./scripts/logs.sh dataimport 30 # Run for 30 seconds"
  ;;
esac

