# Windows: sterowniki USB i flashowanie ESP32

### Jakie sterowniki USB są potrzebne do płytki z ESP32?
Zależy od układu USB–UART na płytce: CP210x (Silicon Labs) lub CH9102/CH340 (WCH, pakiet CH343SER). Niektóre nowsze płytki wymagają CH9102 zamiast CP210x albo dodatkowo. Sprawdź, jaki układ ma Twoja płytka, pobierz sterownik ze strony producenta układu, zainstaluj i koniecznie zrestartuj komputer. Dla Windows 7 oficjalna strona wskazuje bezpośredni plik CH9102 w repozytorium Xinyuan-LilyGO na GitHubie. Instrukcji dla macOS w pobranej treści nie było.
Słowa: sterownik, CP210x, CH340, CH9102, CH343SER, Windows, USB-UART, ESP32, port COM, restart komputera
Źródło: https://meshtastic.org/docs/getting-started/serial-drivers/esp32/ (sprawdzono 2026-10)

### Wgrywanie firmware na ESP32: którą metodą?
Zalecany jest flasher WWW, który wymaga Chrome lub Edge (flasher.meshtastic.org). Drugi sposób to skrypt wiersza poleceń, a zewnętrzny adapter szeregowy to ostateczność. Strona przeglądowa nie opisuje osobno aktualizacji i pełnego wymazania. Zaznacza, że klient WWW pod meshtastic.local aktualizuje się tylko przez pełne wymazanie i ponowne wgranie, więc przed tym wyeksportuj konfigurację i wczytaj ją po wgraniu. Szczegóły i rozwiązywanie problemów są na podstronach, których nie pobrano.
Słowa: flasher, web flasher, Chrome, Edge, ESP32, wgrywanie firmware, wymazanie, eksport konfiguracji, meshtastic.local
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/ (sprawdzono 2026-10)

### Płytka nie pojawia się w oknie wyboru portu flashera: co sprawdzić?
Według dokumentacji warto najpierw zainstalować właściwy sterownik (patrz wyżej) i zrestartować komputer. Poza tym ogólnie sprawdzone kroki: inny kabel USB (bywają kable tylko do ładowania), inne gniazdo, wejście w tryb wgrywania przyciskiem (w wielu płytkach przytrzymanie BOOT/USER przy włączaniu lub "1200bps reset" we flasherze), a w Windows podgląd Menedżera urządzeń w sekcji Porty (COM i LPT). Te kroki oprócz sterowników to wiedza ogólna, strona z testem sterowników nie została pobrana.
Słowa: brak portu COM, flasher nie widzi urządzenia, kabel tylko do ładowania, Menedżer urządzeń, BOOT, tryb wgrywania, Windows
Źródło: https://meshtastic.org/docs/getting-started/serial-drivers/ (sprawdzono 2026-10; kroki poza sterownikami: wiedza ogólna)
