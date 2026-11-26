#!/usr/bin/env bash
# Emulator smoke test + screenshot harness.
#
# Installs the debug APK, launches it, then drives a scripted navigation pass with
# `input tap` and captures a PNG after every step. Screenshots are committed back to
# ci-results/ by the workflow so the interface can be visually compared against the
# reference captures. Fails on install error, crash, or a missing HOME screenshot.
set -uo pipefail
exec 2> smoke-stderr.txt
set -x

mkdir -p shots
adb install -r dist/PS5InterfaceSimulator-debug.apk > install.txt 2>&1
INSTALL_RC=$?
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c || true

adb shell am start -n com.kaizer.ps5simulator/.MainActivity > start.txt 2>&1
AM_RC=$?
sleep 12

shot() { # name
  adb exec-out screencap -p > "shots/$1.png"
  echo "shot $1: $(stat -c%s "shots/$1.png" 2>/dev/null) bytes"
}

# 1) home, settled
shot 01_home
# 2) tap the PlayStation Store tile
adb shell input tap 690 230 > /dev/null 2>&1; sleep 3; shot 02_store
adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1; sleep 2
# 3) select the second game tile in the switcher
adb shell input tap 900 230 > /dev/null 2>&1; sleep 2; shot 03_home_second_game
# 4) open game hub via the Play pill
adb shell input tap 400 850 > /dev/null 2>&1; sleep 5; shot 04_game_hub
# 5) start the simulated launch
adb shell input tap 400 700 > /dev/null 2>&1; sleep 6; shot 05_launch
adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1; sleep 3
adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1; sleep 3
# 6) game library via the store-tile row end (open from settings-free path: back to home)
adb shell input tap 60 60 > /dev/null 2>&1; sleep 1
# open the Control Center by long-press-free path: tap the profile avatar in the top bar
adb shell input tap 1470 70 > /dev/null 2>&1; sleep 4; shot 06_profile
adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1; sleep 2
# 7) settings via top-right gear
adb shell input tap 1400 70 > /dev/null 2>&1; sleep 4; shot 07_settings
adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1; sleep 2
# 8) media tab
adb shell input tap 330 60 > /dev/null 2>&1; sleep 3; shot 08_media
adb shell input tap 120 60 > /dev/null 2>&1; sleep 2

(adb shell dumpsys activity activities | grep -iE 'mResumedActivity|topResumedActivity';
 adb shell dumpsys window | grep -i mCurrentFocus) | head -8 > resumed.txt 2>&1
adb logcat -d -v threadtime > logcat.txt
LOGCAT_RC=$?

{
  echo "install=$INSTALL_RC"
  echo "am_start=$AM_RC"
  echo "logcat=$LOGCAT_RC"
} > smoke-status.txt

set +x
echo "=== install ==="; cat install.txt
echo "=== am start ==="; cat start.txt
echo "=== resolved activity ==="; cat resumed.txt
echo "=== status ==="; cat smoke-status.txt

if grep -qE 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt; then
  echo "crash=1" >> smoke-status.txt
  echo "=== CRASH ==="
  grep -E -A20 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt | head -120
  exit 1
fi
echo "crash=0" >> smoke-status.txt
if [ "$INSTALL_RC" -ne 0 ]; then echo "INSTALL FAILED"; exit 1; fi
if ! grep -q 'com.kaizer.ps5simulator' resumed.txt; then echo "ACTIVITY NOT RESUMED"; exit 1; fi
if [ ! -s shots/01_home.png ]; then echo "NO HOME SCREENSHOT"; exit 1; fi
echo "SMOKE OK"
echo "SCRIPT_COMPLETED" >> smoke-status.txt
exit 0
