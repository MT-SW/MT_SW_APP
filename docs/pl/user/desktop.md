---
title: Aplikacja na komputer
parent: Przewodnik użytkownika
nav_order: 14
last_updated: 2026-10-21
description: Instalacja i obsługa aplikacji MT_SW_APP na komputerze z Linuxem, macOS i Windows — połączenia, zgodność funkcji i skróty klawiszowe.
aliases:
  - desktop
  - linux
  - macos
  - windows
  - jvm
---

# Aplikacja na komputer

Ta strona opisuje instalację aplikacji na komputer, podłączanie radia i różnice względem wersji na Androida. Aplikacja na komputer dzieli z Androidem główny kod dzięki Kotlin Multiplatform, więc większość funkcji działa tak samo w Linuxie, macOS i Windows.

## Instalacja

### Linux

- Pobierz pakiet `.deb`, `.rpm` lub `.AppImage` ze [strony wydań](https://github.com/meshtastic/Meshtastic-Android/releases). Każdy jest w wersji x86_64 i arm64 (aarch64).
- AppImage ma własne informacje o aktualizacji, więc AppImageUpdate i inne programy oparte na zsync mogą zaktualizować go na miejscu do najnowszego wydania produkcyjnego.
- Albo zainstaluj z Flathub: `flatpak install flathub org.meshtastic.MeshtasticDesktop`
- Albo zbuduj ze źródeł poleceniem `./gradlew :desktopApp:run`

### macOS

- Pobierz pakiet `.dmg` ze [strony wydań](https://github.com/meshtastic/Meshtastic-Android/releases). Jest zbudowany tylko dla układów Apple silicon.
- Albo zbuduj ze źródeł

### Windows

- Pobierz instalator `.msi` lub `.exe` ze [strony wydań](https://github.com/meshtastic/Meshtastic-Android/releases). Oba są w wersji x64.
- Albo zbuduj ze źródeł

## Podłączanie radia

Wszystkie trzy transporty są w zakładce **Połącz**, tak samo jak na Androidzie. Wybierz jeden selektorem transportu. Pełny opis znajdziesz w [Połączenia](connections).

### USB Serial (podstawowe)

Najpewniejsza metoda połączenia na komputerze:

Podłącz radio kablem USB, wybierz **USB** w selektorze transportu i kliknij port szeregowy radia na liście. Lista odświeża się sama, gdy porty się pojawiają i znikają.

### TCP/IP

Dla radiów podłączonych do sieci wybierz **Sieć** w selektorze transportu, a potem:

1. Włącz **Skanuj w poszukiwaniu urządzeń sieciowych** i kliknij radio, które skanowanie znajdzie, albo
2. Kliknij **Dodaj urządzenie ręcznie…** i wpisz adres IP lub nazwę hosta radia oraz port (domyślnie: `4403`).

### Bluetooth (BLE)

Bluetooth Low Energy jest obsługiwany na komputerze przez bibliotekę [Kable](https://github.com/JuulLabs/kable):

1. Upewnij się, że komputer ma adapter Bluetooth, i wybierz **Bluetooth** w selektorze transportu.
2. Kliknij **Skanuj w poszukiwaniu urządzeń Bluetooth**, a potem wybierz swoje radio z listy.

## Zgodność funkcji

| Funkcja | Android | Komputer | Uwagi |
|---------|---------|----------|-------|
| Wiadomości | ✓ | ✓ | Pełna zgodność |
| Lista węzłów | ✓ | ✓ | Pełna zgodność |
| Mapa | ✓ | ✓ | Interaktywna mapa MapLibre, z wyborem mapy bazowej i nakładek oraz własnymi źródłami kafelków. Można pobrać teren offline (cieniowanie wzniesień i warstwice); pakiety map bazowych offline i lokalne archiwa `.mbtiles` nie są obsługiwane |
| Warstwy mapy (`.kml`/`.kmz`/GeoJSON) | ✓ | ✓ | Ten sam magazyn warstw i ten sam panel co na Androidzie; zaimportowane pliki są rysowane na mapie na komputerze |
| Planer stanowisk (Site Planner) | ✓ | ✓ | Liczy oszacowanie zasięgu w aplikacji i dodaje je do mapy jako warstwę, tak jak na Androidzie |
| Ustawienia | ✓ | ✓ | Pełna zgodność |
| Bluetooth (BLE) | ✓ | ✓ | Na komputerze przez Kable |
| Aktualizacja firmware | ✓ | ✓ | Aktualizacja w aplikacji przez USB, BLE i Wi-Fi (ESP32) działa tak samo jak na Androidzie. Tryb serwisowy USB — kasowanie fabryczne nRF52/RP2040 i aktualizacja bootloadera — jest tylko na Androidzie |
| Powiadomienia | ✓ | ✓ | Natywne powiadomienia systemu |
| Widżety | ✓ | ✗ | Tylko Android |
| Asystent AI (Chirpy) | ✓* | ✗ | Tylko Android w wersji Google |
| Funkcje aplikacji (AI systemowe) | ✓† | ✗ | Tylko Android w wersji Google |

*Chirpy AI wymaga Androida 14+ w wersjach Google na obsługiwanym sprzęcie.

†Funkcje aplikacji udostępniają działania aplikacji systemowemu AI Androida w wersjach Google. Zobacz [Funkcje aplikacji](app-functions).

## Różnice w interfejsie

Aplikacja na komputer używa tego samego interfejsu Compose Multiplatform, dostosowanego do większych ekranów i obsługi na komputerze.

### Skróty klawiszowe

Skróty używają **⌘** (Command) w macOS oraz **Ctrl** w Windows i Linuxie. (Klawisz Super / Windows nie jest używany.)

| Skrót | Działanie |
|----------|--------|
| **⌘/Ctrl+Q** | Zamknij aplikację |
| **⌘/Ctrl+,** | Otwórz Ustawienia |
| **⌘/Ctrl+1** | Przejdź do karty Wiadomości |
| **⌘/Ctrl+2** | Przejdź do karty Węzły |
| **⌘/Ctrl+3** | Przejdź do karty Mapa |
| **⌘/Ctrl+4** | Przejdź do karty Połączenie |
| **⌘/Ctrl+/** | Otwórz informacje o aplikacji |

### Okno i zasobnik systemowy

- **Zmiana rozmiaru okna** — układ dostosowuje się do wymiarów okna
- **Zasobnik systemowy** — zamknięcie okna minimalizuje aplikację do zasobnika systemowego, aby sieć mesh działała w tle, pod warunkiem że ikonę w zasobniku można kliknąć, aby przywrócić okno. W Linuxie wymaga to środowiska graficznego z zasobnikiem StatusNotifierItem. Tam, gdzie takiego zasobnika nie ma, zamknięcie okna zamyka aplikację
- **Menu zasobnika** — kliknij prawym przyciskiem ikonę w zasobniku, aby wybrać **Pokaż** lub **Zakończ**
- **Obsługa myszą** — podświetlanie po najechaniu i standardowa nawigacja jak na komputerze

### Preferencje powiadomień

Aplikacja na komputer ma przełączniki, którymi wybierasz, jakie powiadomienia mają być pokazywane. Znajdziesz je w sekcji **Powiadomienia aplikacji** na ekranie Ustawienia: **Powiadomienia o bezpośredniej wiadomości**, **Powiadomienia o nowych węzłach**, **Powiadomienia o niskim poziomie baterii**, **Powiadomienia o reakcjach** i **Powiadomienia o punktach trasy**.

## Wbudowana przeglądarka dokumentacji

Aplikacja na komputer zawiera wbudowaną przeglądarkę dokumentacji, dzięki której szybko znajdziesz pomoc bez wychodzenia z aplikacji.

![Przeglądarka dokumentacji ze spisem treści](../../assets/screenshots/docs-browser_toc.png)

Przeglądarka wyszukuje w tytułach stron i słowach kluczowych we wszystkich dokumentach:

![Wyszukiwanie w przeglądarce dokumentacji](../../assets/screenshots/docs-browser_search.png)

Poszczególne strony dokumentacji są wyświetlane z pełnym formatowaniem:

![Strona dokumentacji](../../assets/screenshots/docs-browser_page.png)

## Budowanie ze źródeł

```shell
git clone https://github.com/meshtastic/Meshtastic-Android.git
cd Meshtastic-Android
./gradlew :desktopApp:run
```

Wymagania:
- JDK 25 do uruchamiania Gradle (Gradle potrafi sam je pobrać przez foojay)
- JetBrains Runtime (JBR) 25 zainstalowany tam, gdzie Gradle go znajdzie. Aplikacja na komputer jest kompilowana z nim i go dołącza, a foojay go nie oferuje, więc Gradle nie może go pobrać
- Android SDK nie jest potrzebne przy budowaniu samej wersji na komputer

## Znane ograniczenia

- Pobieranie map bazowych offline i lokalne archiwa `.mbtiles` nie są dostępne na komputerze. Teren offline jest dostępny — zobacz [Mapa i punkty trasy](map-and-waypoints).
- Import warstw `.kml`/`.kmz`/GeoJSON działa — zobacz
  [Mapa i punkty trasy](map-and-waypoints#map-layers). Własne sieciowe źródła kafelków
  też działają — zobacz [Mapa i punkty trasy](map-and-waypoints#adding-your-own-tile-source)
- Tryb serwisowy USB — kasowanie fabryczne nRF52/RP2040 i aktualizacja bootloadera — jest tylko na Androidzie, a
  aplikacja na komputer go nie oferuje. Użyj zamiast tego [Web Flasher](https://flasher.meshtastic.org)
- Niektóre funkcje specyficzne dla Androida (widżety, określone kanały powiadomień) są niedostępne
- Wydajność może być słabsza na komputerach o niskiej specyfikacji z Compose Desktop
- Parowanie z zapamiętaniem (bonding) BLE nie jest obsługiwane na komputerze (parowanie działa bez bondingu)

## Powiązane tematy

- [Połączenia](connections) — przegląd metod połączenia
- [Aktualizacje firmware](firmware) — aktualizacja w aplikacji przez USB, BLE i Wi-Fi działa tak samo jak na Androidzie
- [Mapa i punkty trasy](map-and-waypoints) — mapy bazowe, warstwy, własne źródła kafelków i to, czego mapa na komputerze nie potrafi
