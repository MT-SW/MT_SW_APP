#!/bin/bash
# Buduje plik .ipa bez podpisu (do instalacji przez AltStore / Sideloadly, które podpisują go Apple ID osoby testującej).
# Builds an unsigned .ipa (for AltStore / Sideloadly, which sign it with the tester's own Apple ID).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/iosApp"
xcodegen
# IPA_CONFIG: Release (domyślnie, szybka aplikacja, kompilator iOS potrzebuje dużo pamięci) albo Debug (mało pamięci, np. na GitHubie).
# IPA_NATIVE_HEAP: pamięć kompilatora iOS dla Release.
# IPA_CONFIG: Release (default, fast app, the iOS compiler needs a lot of memory) or Debug (little memory, e.g. on GitHub).
# IPA_NATIVE_HEAP: memory for the iOS compiler in Release.
CONFIG="${IPA_CONFIG:-Release}"
# Kompilator iOS (Kotlin/Native) działa w samym Gradle, więc to on dostaje dużo pamięci (Release).
build() {
  CONFIG="$1"
  if [ "$CONFIG" = "Release" ]; then
    export MTSW_GRADLE_ARGS="-Dorg.gradle.jvmargs=-Xmx${IPA_NATIVE_HEAP:-12g} -Pkotlin.native.jvmArgs=-Xmx${IPA_NATIVE_HEAP:-12g}"
  else
    export MTSW_GRADLE_ARGS="-Dorg.gradle.jvmargs=-Xmx4g"
  fi
  (cd "$ROOT" && ./gradlew --stop) || true
  xcodebuild -project MTSWApp.xcodeproj -scheme MTSWApp -sdk iphoneos -configuration "$CONFIG" \
    -derivedDataPath "$ROOT/build/xcode-device" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" MTSW_APP_STORE_BUILD="${IPA_APP_STORE:-NO}" build
}
CONFIG="${IPA_CONFIG:-Release}"
build "$CONFIG"
APP="$ROOT/build/xcode-device/Build/Products/${CONFIG}-iphoneos/MTSWApp.app"
rm -rf "$ROOT/build/ipa" && mkdir -p "$ROOT/build/ipa/Payload"
cp -R "$APP" "$ROOT/build/ipa/Payload/"
(cd "$ROOT/build/ipa" && zip -qry "$ROOT/build/MT_SW_APP-iOS.ipa" Payload)
echo "Gotowe: $ROOT/build/MT_SW_APP-iOS.ipa"
