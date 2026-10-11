#!/usr/bin/env bash
# Lance l'AVD, installe la build debug et démarre l'app. Usage : tools/run-emulator.sh [nom_avd]
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
AVD="${1:-Pixel_10}"

if ! "$SDK/platform-tools/adb" devices | grep -q 'emulator-.*device$'; then
  "$SDK/emulator/emulator" -avd "$AVD" -no-snapshot-save >/dev/null 2>&1 &
  "$SDK/platform-tools/adb" wait-for-device
  until [ "$("$SDK/platform-tools/adb" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
fi

cd "$(dirname "$0")/.."
./gradlew :app:installDebug
"$SDK/platform-tools/adb" shell am start -n org.tally.app/.MainActivity
