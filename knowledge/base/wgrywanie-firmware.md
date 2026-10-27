# Wgrywanie i aktualizacja firmware

### Jak wgrać firmware na ESP32?
Zalecana metoda to Web Flasher (flasher.meshtastic.org), który wymaga Chrome lub Edge i jest najprostszy dla początkujących. Drugi sposób to skrypt wiersza poleceń. Zewnętrzny adapter szeregowy zostaw na koniec, gdy nic innego nie zadziałało. Sterowniki portu szeregowego opisuje osobna strona „Install Serial Drivers”.
Słowa: ESP32, flasher, web flasher, instalacja, wgranie, flashowanie, Chrome, Edge, sterowniki, esptool, T-Beam, Heltec, LILYGO
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/ (sprawdzono 2026-10)

### Jak wgrać firmware na nRF52 (RAK4631, T-Echo) lub RP2040?
Te urządzenia używają metody „przeciągnij i upuść” (plik firmware kopiujesz na dysk, który pojawia się po wejściu w tryb bootloadera). Nie trzeba żadnych sterowników ani programów na żadnym systemie. Firmware na nRF52 można też aktualizować przez Bluetooth z telefonu (OTA); ostatnia możliwość to bezpośrednie połączenie SWDIO.
Słowa: nRF52, RAK4631, T-Echo, RP2040, RP2350, UF2, drag and drop, bootloader, DFU, OTA, Bluetooth, SWDIO
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/ (sprawdzono 2026-10)

### Czy przed aktualizacją robić Factory Erase?
Przy nRF52/RP2040 dokumentacja radzi rozważyć Factory Erase przed instalacją, bo format lub miejsce zapisanych danych może zmienić się między wersjami. Przed czyszczeniem wyeksportuj konfigurację, żeby ją potem wczytać z powrotem.
Słowa: erase, wyczyszczenie, reset fabryczny, factory reset, kopia konfiguracji, eksport, aktualizacja
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/ (sprawdzono 2026-10)

### Jak zaktualizować web client (meshtastic.local) na ESP32?
Tylko przez pełne wyczyszczenie i ponowną instalację. Aby zachować ustawienia, wyeksportuj konfigurację przed instalacją i wczytaj po niej.
Słowa: web client, meshtastic.local, aktualizacja, wipe, Wi-Fi
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/ (sprawdzono 2026-10)

### Jak użyć Web Flashera?
Podłącz urządzenie ESP32, otwórz flasher.meshtastic.org w Chrome lub Edge (inne przeglądarki nie są wymienione jako wspierane) i postępuj według komunikatów na ekranie. Po wgraniu przejdź do konfiguracji początkowej. Web Flasher potrafi też pobrać i skopiować firmware za Ciebie dla urządzeń UF2.
Słowa: web flasher, flasher.meshtastic.org, Chrome, Edge, przeglądarka, WebSerial, konfiguracja początkowa
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/web-flasher/ (sprawdzono 2026-10)

### Jak wgrać lub zaktualizować ESP32 skryptem (esptool, device-install)?
Potrzebne: Python 3 i pip3 (na Windows Python z dodaniem do PATH i Git Bash), pakiet esptool (pip3 install --upgrade esptool), pobrany firmware pasujący do Twojej płytki (z zakładki Assets na GitHubie, np. firmware-esp32s3-X.X.X.xxxxxxx). Sprawdź połączenie: esptool chip-id (Windows: esptool.py chip_id). W folderze firmware: instalacja ./device-install.sh -f firmware-PŁYTKA-WERSJA.bin (Windows device-install.bat), aktualizacja plikiem -update.bin skryptem device-update.sh/bat; opcja --web dołącza wbudowanego klienta WWW. Zrestartuj komputer po instalacji sterowników; na Linux/macOS czasem trzeba wywołać esptool.py jawnie.
Słowa: esptool, device-install.sh, device-update.sh, chip-id, skrypt, bin, update.bin, pip, Python, --web, CLI flashing
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/cli-script/ (sprawdzono 2026-10)

### Na co uważać przy wgrywaniu firmware na ESP32 skryptem?
Podłącz antenę przed włączeniem zasilania: bez niej można uszkodzić układ radiowy. Użyj dokładnie firmware dla swojej płytki: zły wsad może zakończyć się bez błędu, a urządzenie będzie nie odpowiadać (np. pusty ekran), możliwe są też uszkodzenia związane z GPIO. T-Beam 0.7 wymaga firmware-tbeam0.7, zwykły firmware-tbeam jest niekompatybilny. Dokumentacja tej strony nie opisuje polecenia kasowania pamięci.
Słowa: zły firmware, pusty ekran, T-Beam 0.7, cegła, antena, uszkodzenie, nie odpowiada po flashowaniu, dobór pliku
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/esp32/cli-script/ (sprawdzono 2026-10)
