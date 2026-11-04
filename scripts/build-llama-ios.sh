#!/bin/bash
# Buduje silnik AI (llama.cpp) dla iPhone'a i symulatora: native/ios/llama.xcframework
# Building the AI engine (llama.cpp) for iPhone and simulator: native/ios/llama.xcframework
set -euo pipefail
TAG="b11960"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/build/llama-ios"
rm -rf "$WORK"
mkdir -p "$WORK" "$ROOT/native/ios"
git clone --depth 1 --branch "$TAG" https://github.com/ggml-org/llama.cpp "$WORK/llama.cpp"
cd "$WORK/llama.cpp"
./build-xcframework.sh
rm -rf "$ROOT/native/ios/llama.xcframework"
cp -R build-apple/llama.xcframework "$ROOT/native/ios/llama.xcframework"
echo "Gotowe / Done: $ROOT/native/ios/llama.xcframework"
