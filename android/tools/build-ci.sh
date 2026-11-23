#!/bin/bash
# Reproducible debug APK build using the exact toolchain verified in the sandbox.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
REPO=$(cd "$ROOT/.." && pwd)
WORK="$ROOT/build"
DIST="$REPO/dist"

: "${AAPT2:?set AAPT2}"; : "${ECJ:?set ECJ}"; : "${D8JAR:?set D8JAR}"
: "${APKSIGNER_JAR:?set APKSIGNER_JAR}"; : "${ANDROID_JAR:?set ANDROID_JAR}"
JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}
JAVA="$JAVA_HOME/bin/java"
KEYTOOL="$JAVA_HOME/bin/keytool"

echo "== tools =="
echo "  java      : $JAVA ($($JAVA -version 2>&1 | head -1))"
echo "  aapt2     : $AAPT2"
echo "  android.jar: $ANDROID_JAR"

rm -rf "$WORK" "$DIST"
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/gen" "$DIST"

echo "== 1/7 generate assets =="
python3 "$ROOT/tools/gen_assets.py" "$ROOT"

echo "== 2/7 aapt2 compile =="
"$AAPT2" compile --dir "$ROOT/res" -o "$WORK/gen/res.zip"

echo "== 3/7 aapt2 link (debuggable) =="
"$AAPT2" link -o "$WORK/base.apk" -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" -R "$WORK/gen/res.zip" \
  -A "$ROOT/assets" --auto-add-overlay --debug-mode \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code 1 --version-name 1.0.0-debug -0 wav

echo "== 4/7 compile Java (ECJ) =="
find "$ROOT/src" -name '*.java' -print > "$WORK/sources.txt"
"$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
  -source 1.8 -target 1.8 -encoding UTF-8 -nowarn \
  -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" \
  -d "$WORK/classes" @"$WORK/sources.txt"

echo "== 5/7 d8 =="
python3 "$ROOT/tools/makejar.py" "$WORK/classes" "$WORK/classes.jar"
"$JAVA" -cp "$D8JAR" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api 26 --output "$WORK/dex" "$WORK/classes.jar"

echo "== 6/7 merge classes.dex =="
python3 "$ROOT/tools/add_dex.py" "$WORK/base.apk" "$WORK/dex/classes.dex" "$WORK/unsigned.apk"

echo "== 7/7 sign with debug key + verify =="
"$KEYTOOL" -genkeypair -alias androiddebugkey -keyalg RSA -keysize 2048 \
  -validity 10000 -storetype PKCS12 -keystore "$WORK/debug.keystore" \
  -storepass android -keypass android \
  -dname "CN=Android Debug, OU=PS5Sim, O=Kaizer, C=UA" 2>/dev/null
"$JAVA" -jar "$APKSIGNER_JAR" sign \
  --ks "$WORK/debug.keystore" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --min-sdk-version 26 \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$DIST/PS5MobileSimulator-debug.apk" "$WORK/unsigned.apk"
"$JAVA" -jar "$APKSIGNER_JAR" verify --verbose "$DIST/PS5MobileSimulator-debug.apk" | head -12
"$AAPT2" dump badging "$DIST/PS5MobileSimulator-debug.apk" | grep -E 'package:|debuggable|launchable-activity|application-label'
ls -lh "$DIST/PS5MobileSimulator-debug.apk"
