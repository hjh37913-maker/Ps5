#!/usr/bin/env bash
# Emulator smoke test: install, launch, tap, capture logcat + screenshot.
# Exits 1 if the APK fails to install, the activity is not resumed, or a crash appears.
set -uo pipefail
exec 2> smoke-stderr.txt
set -x

adb install -r dist/PS5MobileSimulator-debug.apk > install.txt 2>&1
INSTALL_RC=$?
adb logcat -c || true
adb shell am start -n com.kaizer.ps5simulator/.MainActivity > start.txt 2>&1
AM_RC=$?
sleep 15
adb shell input tap 500 300 > tap.txt 2>&1
TAP_RC=$?
sleep 4
(adb shell dumpsys activity activities | grep -iE 'mResumedActivity|topResumedActivity'; \
 adb shell dumpsys window | grep -i mCurrentFocus) | head -8 > resumed.txt 2>&1
adb logcat -d -v threadtime > logcat.txt
LOGCAT_RC=$?
adb exec-out screencap -p > screenshot.png
SCREEN_RC=$?

{
  echo "install=$INSTALL_RC"
  echo "am_start=$AM_RC"
  echo "tap=$TAP_RC"
  echo "logcat=$LOGCAT_RC"
  echo "screencap=$SCREEN_RC"
} > smoke-status.txt

set +x
echo "=== install ==="; cat install.txt
echo "=== am start ==="; cat start.txt
echo "=== resumed ==="; cat resumed.txt
echo "=== status ==="; cat smoke-status.txt

if grep -qE 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt; then
  echo "crash=1" >> smoke-status.txt
  echo "=== CRASH ==="
  grep -E -A16 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt | head -100
  exit 1
fi
echo "crash=0" >> smoke-status.txt
if [ "$INSTALL_RC" -ne 0 ]; then echo "INSTALL FAILED"; exit 1; fi
if ! grep -q 'com.kaizer.ps5simulator' resumed.txt; then echo "ACTIVITY NOT RESUMED"; exit 1; fi
echo "SMOKE OK"
echo "SCRIPT_COMPLETED" >> smoke-status.txt
exit 0
