#!/bin/bash
# Field Medic: build, install, and capture crash logs.
# Usage: ./logcat_capture.sh

set -e

LOG="/tmp/fieldmedic_logcat.txt"
PKG="com.google.aiedge.gallery"
ACTIVITY="com.google.ai.edge.gallery.MainActivity"
GRADLE_DIR="$(cd "$(dirname "$0")/Android/src" && pwd)"

echo "=== Field Medic Crash Capture ==="
echo ""

# --- Step 1: Build ---
echo "[1/4] Building debug APK..."
cd "$GRADLE_DIR"
./gradlew :app:assembleDebug -q 2>&1 | tail -5
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo "!!! Build failed. Fix errors and retry."
    exit 1
fi
echo "      Build OK."

# --- Step 2: Install ---
echo "[2/4] Installing on device..."
adb install -r -t app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1
echo "      Install OK."

# --- Step 3: Launch + capture ---
echo "[3/4] Clearing logcat and launching app..."
adb logcat -c 2>/dev/null
> "$LOG"

adb shell am force-stop "$PKG" 2>/dev/null
sleep 1
adb shell am start -n "$PKG/$ACTIVITY" 2>/dev/null

sleep 1
PID=$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r')
echo "      App PID: ${PID:-unknown}"

# Stream everything to file
adb logcat -v threadtime 2>/dev/null >> "$LOG" &
LOGCAT_PID=$!

# Echo key tags to terminal
adb logcat -v brief -s "TriageLoopVM:*" "ModeRouter:*" "FieldMedicVM:*" "AndroidRuntime:E" "DEBUG:F" "libc:F" 2>/dev/null &
TERM_PID=$!

echo ""
echo "[4/4] Streaming logs to $LOG"
echo ""
echo "=========================================="
echo "  Reproduce the crash now."
echo "  Type 'q' + Enter when done."
echo "=========================================="
echo ""

# --- Step 4: Wait for user ---
while true; do
    read -r input
    if [ "$input" = "q" ]; then
        break
    fi
    echo "(type 'q' to stop)"
done

# Grab latest tombstone
echo ""
echo "[*] Grabbing latest tombstone..."
LATEST_TOMB=$(adb shell ls -t /data/tombstones/ 2>/dev/null | grep -v '\.pb$' | head -1 | tr -d '\r')
if [ -n "$LATEST_TOMB" ]; then
    echo "--- TOMBSTONE: $LATEST_TOMB ---" >> "$LOG"
    adb shell cat "/data/tombstones/$LATEST_TOMB" 2>/dev/null >> "$LOG"
    echo "[*] Appended $LATEST_TOMB"
fi

# Cleanup
kill $LOGCAT_PID $TERM_PID 2>/dev/null
wait $LOGCAT_PID $TERM_PID 2>/dev/null

LINES=$(wc -l < "$LOG")
echo "[*] Done. Captured $LINES lines in $LOG"
