#!/usr/bin/env bash
# Emulator harness: install, launch, capture real screencaps, then run the in-app
# instrumentation harness which renders every screen to PNG inside the app sandbox.
# PNGs are pulled to ./shots and published by the workflow.
set -uo pipefail
exec 2> smoke-stderr.txt
set -x

mkdir -p shots
adb install -r dist/PS5InterfaceSimulator-debug.apk > install.txt 2>&1
INSTALL_RC=$?
adb logcat -c || true

adb shell am start -n com.kaizer.ps5simulator/.MainActivity > start.txt 2>&1
sleep 10
adb exec-out screencap -p > shots/00_live_home.png || true
adb shell input tap 340 200 > /dev/null 2>&1 || true   # switcher, second tile
sleep 2
adb exec-out screencap -p > shots/00_live_second.png || true
adb shell dumpsys activity activities > resumed.txt 2>&1 || true
adb shell am force-stop com.kaizer.ps5simulator

# Instrumented capture pass (framework-only runner, see ci/ShotRunner.kt)
rm -rf shots/instrumented
adb shell am instrument -w -e class com.kaizer.ps5simulator.ci.ShotRunner \
  com.kaizer.ps5simulator/.ci.ShotRunner > instrument.txt 2>&1
INSTRUMENT_RC=$?
cat instrument.txt
RUNS=$(adb shell pm list instrumentation 2>/dev/null || true)
echo "instrumentation registry: $RUNS" > instrumentation.txt

# pull rendered PNGs (external files dir mirrors /sdcard/Android/data/<pkg>/files)
for base in \
  /sdcard/Android/data/com.kaizer.ps5simulator/files/shots \
  /sdcard/Android/data/com.kaizer.ps5simulator.debug/files/shots ; do
  adb pull "$base" shots/instrumented > /dev/null 2>&1 && break
done

adb logcat -d -v threadtime > logcat.txt
{
  echo "install=$INSTALL_RC"
  echo "instrument=$INSTRUMENT_RC"
} > smoke-status.txt

set +x
echo "=== install ==="; cat install.txt
echo "=== instrument ==="; cat instrument.txt
echo "=== files ==="; find shots -type f | sort
cat smoke-status.txt

if grep -qE 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt; then
  echo "crash=1" >> smoke-status.txt
  grep -E -A20 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt | head -80
  exit 1
fi
echo "crash=0" >> smoke-status.txt
if [ "$INSTALL_RC" -ne 0 ]; then echo "INSTALL FAILED"; exit 1; fi
if [ ! -s shots/00_live_home.png ]; then echo "NO LIVE SCREENSHOT"; exit 1; fi
if [ ! -d shots/instrumented/shots ]; then echo "NO INSTRUMENTED SHOTS"; exit 1; fi
COUNT=$(find shots/instrumented/shots -name '*.png' | wc -l)
echo "instrumented screenshots: $COUNT"
if [ "$COUNT" -lt 6 ]; then echo "TOO FEW SCREENSHOTS"; exit 1; fi
echo "SMOKE OK"
exit 0
