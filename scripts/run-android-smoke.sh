#!/usr/bin/env bash
set -euo pipefail

version=${1:?Expected engine version is required}
apk_dir=consumer/build/outputs/apk
app=com.dolthub.doltlite.smoke

adb install -r "$apk_dir/debug/doltlite-consumer-debug.apk"
adb install -r -t "$apk_dir/androidTest/debug/doltlite-consumer-debug-androidTest.apk"
adb shell pm clear "$app"

for phase in write reopen; do
  output=$(adb shell am instrument -w -r -e phase "$phase" -e version "$version" \
    "$app.test/$app.SmokeRunner")
  printf '%s\n' "$output"
  grep -Fq "DoltLite smoke $phase OK" <<< "$output"
  grep -Fq 'INSTRUMENTATION_CODE: -1' <<< "$output"
  adb shell am force-stop "$app"
done
