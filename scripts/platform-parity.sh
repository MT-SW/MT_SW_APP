#!/usr/bin/env bash
# Porównanie wersji: które „Local…Provider” dostarcza Android, a nie dostarcza iPhone/iPad (i komputer).
# Uruchom z katalogu głównego repozytorium: ./scripts/platform-parity.sh
set -euo pipefail
cd "$(dirname "$0")/.."

providers() { git grep -ho "Local[A-Za-z]* provides" -- "$@" | sed 's/ provides//' | sort -u; }

android=$(providers androidApp/src/main/kotlin/org/meshtastic/app/MainActivity.kt)
desktop=$(providers desktopApp/src/main/kotlin/org/meshtastic/desktop/Main.kt)
ios=$(providers iosShared/src)

echo "== Android ma, iPhone/iPad nie =="
comm -23 <(echo "$android") <(echo "$ios")
echo
echo "== Android ma, komputer nie =="
comm -23 <(echo "$android") <(echo "$desktop")
echo
echo "== iPhone/iPad ma, Android nie =="
comm -13 <(echo "$android") <(echo "$ios")
