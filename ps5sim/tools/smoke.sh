#!/usr/bin/env bash
# Emulator harness.
#
# 1. installs and cold-starts the app, captures the live Home screen;
# 2. for every screen, relaunches the app in a debug-only "scenario" and captures the
#    real framebuffer with screencap. Deterministic: no taps, no timing races.
#
# Screenshots land in ./shots and are published to ci-results/ by the workflow.
set -uo pipefail
exec 2> smoke-stderr.txt
set -x

PKG=com.kaizer.ps5simulator
ACT=$PKG/.MainActivity
mkdir -p shots
adb install -r dist/PS5InterfaceSimulator-debug.apk > install.txt 2>&1
INSTALL_RC=$?
adb logcat -c || true

shot() { # file
  adb exec-out screencap -p > "shots/$1.png"
  local sz
  sz=$(stat -c%s "shots/$1.png" 2>/dev/null || echo 0)
  echo "shot $1 ${sz}B"
}

scenario() { # name game
  adb shell am force-stop $PKG
  sleep 1
  adb shell am start -n $ACT --es scenario "$1" --es game "${2:-gtavi}" > /dev/null 2>&1
  sleep 5
  shot "$1"
}

adb shell am start -n $ACT > start.txt 2>&1
sleep 8
shot 00_live_home
scenario home gtavi
scenario home_second spiderman2
scenario hub ghostyotei
scenario library gtavi
scenario control_center ghostyotei
scenario profile gtavi
scenario settings gtavi
scenario trophies gtavi
scenario store gtavi
scenario launch gtavi
scenario media gtavi

(adb shell dumpsys activity activities | grep -iE 'mResumedActivity' ) > resumed.txt 2>&1
adb logcat -d -v threadtime > logcat.txt

{
  echo "install=$INSTALL_RC"
} > smoke-status.txt

set +x
echo "=== install ==="; cat install.txt
echo "=== files ==="; find shots -type f | sort
cat smoke-status.txt

if grep -qE 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt; then
  echo "crash=1" >> smoke-status.txt
  grep -E -A20 'FATAL EXCEPTION|AndroidRuntime: FATAL' logcat.txt | head -80
  exit 1
fi
echo "crash=0" >> smoke-status.txt
if [ "$INSTALL_RC" -ne 0 ]; then echo "INSTALL FAILED"; exit 1; fi
COUNT=$(find shots -name '*.png' -size +20k | wc -l)
echo "screenshots: $COUNT"
if [ "$COUNT" -lt 8 ]; then echo "TOO FEW SCREENSHOTS"; exit 1; fi
echo "SMOKE OK"
exit 0
