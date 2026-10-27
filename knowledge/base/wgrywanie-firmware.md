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
