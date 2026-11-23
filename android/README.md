# PS5 Mobile Simulator — CI build

The repository zip contained only Gradle configs, the keystore and the asset
generator — no Kotlin/Compose sources. This directory is a self-contained,
dependency-free rebuild (plain Android SDK + Canvas UI) that can be built
reproducibly in GitHub Actions.

`tools/build-ci.sh` expects these env vars (the workflow provides them):

| var | meaning |
|---|---|
| `AAPT2` | aapt2 binary |
| `ECJ` | Eclipse batch compiler jar |
| `D8JAR` | R8/D8 jar |
| `APKSIGNER_JAR` | apksigner jar |
| `ANDROID_JAR` | android.jar (API 35) |

Output: `dist/PS5MobileSimulator-debug.apk`, signed with a generated debug key,
`android:debuggable="true"`, minSdk 26 / targetSdk 35.
