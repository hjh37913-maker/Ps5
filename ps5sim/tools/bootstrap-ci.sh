#!/usr/bin/env bash
# Installs the dependency-light toolchain used by build-ci.sh.
#   - Kotlin 2.x compiler (npm package "kotlin-compiler", official JetBrains artifact)
#   - aapt2 (npm package aaptjs3)
#   - d8 / ecj / apksigner / android.jar (npm package @drxiaozhi/minapk)
# Requires: java on PATH, curl, tar, python3.
set -euo pipefail
TC=${TC:-/tmp/tc}
mkdir -p "$TC"
cd "$TC"

if [ ! -f "$TC/kotlin/lib/kotlin-compiler.jar" ]; then
  curl -fsSL https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-2.4.21.tgz -o kc.tgz
  mkdir -p kotlin && tar xzf kc.tgz -C kotlin --strip-components=1
fi
if [ ! -x "$TC/aapt2" ]; then
  curl -fsSL https://registry.npmjs.org/aaptjs3/-/aaptjs3-2.0.2.tgz -o aapt.tgz
  mkdir -p aapt && tar xzf aapt.tgz -C aapt --strip-components=1
  cp aapt/bin/x64/linux/aapt2 "$TC/aapt2" && chmod +x "$TC/aapt2"
fi
if [ ! -f "$TC/tools/d8.jar" ]; then
  curl -fsSL https://registry.npmjs.org/@drxiaozhi/minapk/-/minapk-0.5.1.tgz -o minapk.tgz
  mkdir -p minapk && tar xzf minapk.tgz -C minapk --strip-components=1
  mkdir -p tools && cp -r minapk/tools/* tools/
fi

cat > "$TC/env.sh" <<EOF
export AAPT2=$TC/aapt2
export KOTLINC=$TC/kotlin/lib/kotlin-compiler.jar
export KOTLIN_LIB=$TC/kotlin/lib
export ECJ=$TC/tools/ecj-3.45.0.jar
export D8JAR=$TC/tools/d8.jar
export APKSIGNER_JAR=$TC/tools/apksigner.jar
export ANDROID_JAR=$TC/tools/android.jar
EOF
echo "toolchain ready: $TC (source $TC/env.sh)"
