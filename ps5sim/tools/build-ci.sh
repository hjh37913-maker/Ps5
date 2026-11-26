#!/usr/bin/env bash
# Reproducible debug/release build without Gradle daemons.
# Toolchain (installed by CI or tools/bootstrap-ci.sh):
#   AAPT2, ECJ, D8JAR, APKSIGNER_JAR, ANDROID_JAR, KOTLINC (kotlin-compiler.jar), KOTLIN_LIB (dir)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
WORK="$ROOT/build/ci"
DIST="$ROOT/dist"

: "${AAPT2:?set AAPT2}"
: "${ECJ:?set ECJ}"
: "${D8JAR:?set D8JAR}"
: "${APKSIGNER_JAR:?set APKSIGNER_JAR}"
: "${ANDROID_JAR:?set ANDROID_JAR}"
: "${KOTLINC:?set KOTLINC (kotlin-compiler.jar)}"
: "${KOTLIN_LIB:?set KOTLIN_LIB (dir with kotlin-stdlib.jar)}"
JAVA=${JAVA:-java}
KEYTOOL=${KEYTOOL:-keytool}
MODE=${1:-debug}

echo "== toolchain =="
"$JAVA" -version 2>&1 | head -1
"$AAPT2" version

rm -rf "$WORK"
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/gen" "$DIST"

echo "== 1/8 assets =="
python3 "$ROOT/tools/gen_assets.py"

echo "== 2/8 aapt2 compile =="
"$AAPT2" compile --dir "$ROOT/app/src/main/res" -o "$WORK/gen/res.zip"

echo "== 3/8 aapt2 link =="
DEBUG_FLAG=""
if [ "$MODE" = "debug" ]; then DEBUG_FLAG="--debug-mode"; fi
"$AAPT2" link -o "$WORK/base.apk" -I "$ANDROID_JAR" \
  --manifest "$ROOT/app/src/main/AndroidManifest.xml" -R "$WORK/gen/res.zip" \
  -A "$ROOT/app/src/main/assets" --auto-add-overlay $DEBUG_FLAG \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code 2 --version-name 2.0.0-${MODE} -0 wav

echo "== 4/8 kotlinc =="
find "$ROOT/app/src/main/java" -name '*.kt' -print > "$WORK/sources.txt"
wc -l < "$WORK/sources.txt" | xargs echo "  kotlin files:"
CP="$KOTLIN_LIB/kotlin-stdlib.jar"
"$JAVA" -Xmx1536m -cp "$KOTLINC" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-reflect -jvm-target 17 -language-version 2.0 -nowarn \
  -classpath "$ANDROID_JAR:$CP" -d "$WORK/classes" @"$WORK/sources.txt"

echo "== 5/8 java helpers (ECJ) =="
JAVA_SRC=$(find "$ROOT/app/src/main/java" -name '*.java' 2>/dev/null || true)
if [ -n "$JAVA_SRC" ]; then
  echo "$JAVA_SRC" > "$WORK/jsources.txt"
  "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
    -source 17 -target 17 -encoding UTF-8 -nowarn \
    -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR:$CP:$WORK/classes" \
    -d "$WORK/classes" @"$WORK/jsources.txt"
fi

echo "== 6/8 d8 =="
python3 "$ROOT/tools/makejar.py" "$WORK/classes" "$WORK/classes.jar"
"$JAVA" -Xmx1536m -cp "$D8JAR" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api 26 --release --output "$WORK/dex" \
  "$WORK/classes.jar" $(find "$KOTLIN_LIB" -name 'kotlin-stdlib.jar')

echo "== 7/8 merge dex =="
python3 "$ROOT/tools/add_dex.py" "$WORK/base.apk" "$WORK/unsigned.apk" $(find "$WORK/dex" -name '*.dex' | sort)

echo "== 8/8 sign + verify =="
if [ "$MODE" = "release" ] && [ -f "$ROOT/keystore/ps5sim.keystore" ]; then
  KS="$ROOT/keystore/ps5sim.keystore"; KSPASS=$(grep storePassword "$ROOT/keystore.properties" | cut -d= -f2)
  ALIAS=$(grep keyAlias "$ROOT/keystore.properties" | cut -d= -f2)
  KEYPASS=$(grep keyPassword "$ROOT/keystore.properties" | cut -d= -f2)
  OUT="$DIST/PS5InterfaceSimulator-release.apk"
  ZIPALIGN=false
else
  "$KEYTOOL" -genkeypair -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -storetype PKCS12 -keystore "$WORK/debug.keystore" -storepass android -keypass android \
    -dname "CN=Android Debug, OU=PS5Sim, O=Kaizer, C=UA" 2>/dev/null || true
  KS="$WORK/debug.keystore"; KSPASS=android; ALIAS=androiddebugkey; KEYPASS=android
  OUT="$DIST/PS5InterfaceSimulator-debug.apk"
fi
"$JAVA" -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-pass "pass:$KSPASS" --key-pass "pass:$KEYPASS" \
  --ks-key-alias "$ALIAS" --min-sdk-version 26 --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT" "$WORK/unsigned.apk"
"$JAVA" -jar "$APKSIGNER_JAR" verify --verbose "$OUT" | head -6
"$AAPT2" dump badging "$OUT" | grep -E 'package:|launchable-activity|application-label:|sdkVersion|targetSdkVersion'
ls -lh "$OUT"
echo "BUILD OK: $OUT"
