#!/bin/bash
# Buduje plik .ipa bez podpisu (do instalacji przez AltStore / Sideloadly, które podpisują go Apple ID osoby testującej).
# Builds an unsigned .ipa (for AltStore / Sideloadly, which sign it with the tester's own Apple ID).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/iosApp"
xcodegen
# Zawsze Release (szybka aplikacja; nie ma wersji Debug). Kompilator iOS potrzebuje dużo pamięci: IPA_NATIVE_HEAP (domyślnie 12g).
# Always Release (a fast app; there is no Debug version). The iOS compiler needs a lot of memory: IPA_NATIVE_HEAP (default 12g).
CONFIG="Release"
# Kompilator iOS (Kotlin/Native) działa w samym Gradle, więc to on dostaje dużo pamięci.
build() {
  export MTSW_GRADLE_ARGS="-Dorg.gradle.jvmargs=-Xmx${IPA_NATIVE_HEAP:-12g} -Pkotlin.native.jvmArgs=-Xmx${IPA_NATIVE_HEAP:-12g}"
  (cd "$ROOT" && ./gradlew --stop) || true
  xcodebuild -project MTSWApp.xcodeproj -scheme MTSWApp -sdk iphoneos -configuration "$CONFIG" \
    -derivedDataPath "$ROOT/build/xcode-device" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" MTSW_APP_STORE_BUILD="${IPA_APP_STORE:-NO}" build
}
build
APP="$ROOT/build/xcode-device/Build/Products/${CONFIG}-iphoneos/MTSWApp.app"
rm -rf "$ROOT/build/ipa" && mkdir -p "$ROOT/build/ipa/Payload"
cp -R "$APP" "$ROOT/build/ipa/Payload/"
# Bez podpisu Xcode nie wpisuje uprawnień (większy limit pamięci, App Group widżetu) do plików: jeśli jest ldid, wpisujemy je tu,
# a narzędzie do wgrywania (AltStore/SideStore) odczyta je i podpisze po swojemu.
if command -v ldid >/dev/null 2>&1; then
  for appex in "$ROOT/build/ipa/Payload/MTSWApp.app/PlugIns/"*.appex; do
    [ -d "$appex" ] && ldid -S"$ROOT/iosApp/MTSWWidget/MTSWWidget.entitlements" "$appex"
  done
  ldid -S"$ROOT/iosApp/MTSWApp/MTSWApp.entitlements" "$ROOT/build/ipa/Payload/MTSWApp.app"
else
  echo "Uwaga: brak ldid (brew install ldid): plik .ipa nie będzie miał uprawnień (większa pamięć, widżet)."
fi
(cd "$ROOT/build/ipa" && zip -qry "$ROOT/build/MT_SW_APP-iOS.ipa" Payload)
echo "Gotowe: $ROOT/build/MT_SW_APP-iOS.ipa"
