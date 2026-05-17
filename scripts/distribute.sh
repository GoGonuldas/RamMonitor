#!/usr/bin/env bash
#
# RamMonitor → Firebase App Distribution
# Kullanım:
#   ./scripts/distribute.sh
#   TESTERS="a@x.com,b@x.com" ./scripts/distribute.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APP_ID="1:1048044952881:android:58997e2e39339614e9a410"
TESTERS="${TESTERS:-grkngnlds@gmail.com}"
NOTES_FILE="${NOTES_FILE:-release_notes.txt}"

echo "▶ Building release APK..."
./gradlew assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "❌ APK bulunamadı: $APK"
  exit 1
fi

echo "▶ Uploading to Firebase App Distribution..."
echo "   App ID : $APP_ID"
echo "   Testers: $TESTERS"
echo "   Notes  : $NOTES_FILE"
echo ""

firebase appdistribution:distribute "$APK" \
  --app "$APP_ID" \
  --release-notes-file "$NOTES_FILE" \
  --testers "$TESTERS"

echo ""
echo "✅ Done. Testers e-posta ile davet alacak."

