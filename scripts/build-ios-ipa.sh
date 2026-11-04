#!/bin/bash
# Buduje plik .ipa bez podpisu (do instalacji przez AltStore / Sideloadly, które podpisują go Apple ID osoby testującej).
# Builds an unsigned .ipa (for AltStore / Sideloadly, which sign it with the tester's own Apple ID).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/iosApp"
xcodegen
xcodebuild -project MTSWApp.xcodeproj -scheme MTSWApp -sdk iphoneos -configuration Release \
  -derivedDataPath "$ROOT/build/xcode-device" CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build
APP="$ROOT/build/xcode-device/Build/Products/Release-iphoneos/MTSWApp.app"
rm -rf "$ROOT/build/ipa" && mkdir -p "$ROOT/build/ipa/Payload"
cp -R "$APP" "$ROOT/build/ipa/Payload/"
(cd "$ROOT/build/ipa" && zip -qry "$ROOT/build/MT_SW_APP-iOS.ipa" Payload)
echo "Gotowe: $ROOT/build/MT_SW_APP-iOS.ipa"
