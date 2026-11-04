#!/bin/bash
# Buduje silniki AI dla iPhone'a i symulatora w jednym frameworku: llama.cpp (asystent, tłumacz, dyktowanie
# Gemma) oraz whisper.cpp (dyktowanie Whisper), ze wspólnym ggml: native/ios/llama.xcframework
# Builds the AI engines for iPhone and simulator in one framework: llama.cpp (assistant, translator, Gemma
# dictation) and whisper.cpp (Whisper dictation), sharing one ggml: native/ios/llama.xcframework
set -euo pipefail
LLAMA_TAG="b11960"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/build/llama-ios"
rm -rf "$WORK"
mkdir -p "$WORK" "$ROOT/native/ios"
git clone --depth 1 --branch "$LLAMA_TAG" https://github.com/ggml-org/llama.cpp "$WORK/llama.cpp"
git clone --depth 1 https://github.com/ggml-org/whisper.cpp "$WORK/whisper.cpp"
cd "$WORK/llama.cpp"
# whisper.cpp builds on the ggml of llama.cpp (one copy of ggml in the app).
cat >> CMakeLists.txt <<CMAKE

set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory($WORK/whisper.cpp whisper_ext)
CMAKE
sed -i '' 's|^\(.*\)"${base_dir}/${build_dir}/tools/mtmd/${release_dir}/libmtmd.a"|&\
\1"${base_dir}/${build_dir}/whisper_ext/src/${release_dir}/libwhisper.a"|' build-xcframework.sh
sed -i '' "s|^    cp tools/mtmd/mtmd-helper.h    \${header_path}|&\\
    cp $WORK/whisper.cpp/include/whisper.h \${header_path}|" build-xcframework.sh
grep -n "libwhisper\|whisper.h" build-xcframework.sh
./build-xcframework.sh
rm -rf "$ROOT/native/ios/llama.xcframework"
cp -R build-apple/llama.xcframework "$ROOT/native/ios/llama.xcframework"
echo "Gotowe / Done: $ROOT/native/ios/llama.xcframework"
