#!/bin/bash
# Odświeża podpis aplikacji na iPhone/iPad (darmowe konto Apple, profil ważny 7 dni).
# Usuwa stare profile podpisu i generuje projekt od nowa, żeby Xcode utworzył świeży profil.
cd "$(dirname "$0")" || exit 1
rm -f ~/Library/MobileDevice/Provisioning\ Profiles/*.mobileprovision
xcodegen generate
echo "Gotowe. Teraz uruchom aplikację w Xcode na iPhonie, żeby powstał nowy podpis na 7 dni."
