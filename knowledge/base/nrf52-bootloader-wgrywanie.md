# nRF52 i RP2040: tryb bootloadera, UF2, kasowanie, aktualizacja bootloadera

### Jak wejść w tryb bootloadera (DFU) na nRF52?
Większość urządzeń: dwukrotnie szybko (w ok. pół sekundy) naciśnij przycisk reset; urządzenie zgłosi się jako dysk USB. Aby wyjść bez wgrywania, naciśnij reset raz. Z zainstalowanym firmware Meshtastic: podłącz USB i wydaj polecenie meshtastic --enter-dfu (w aplikacji na Androida: opcja USB File Transfer w aktualizacji przez USB). Urządzenia z jednym przyciskiem (SenseCAP Card Tracker T1000-E, MeshTracker X1) wymagają bootloadera Meshtastic OTAFIX w wersji 2.5 lub nowszej: podłącz USB, przytrzymaj przycisk podczas startu i trzymaj 3 sekundy; aby wyjść bez wgrywania, wysuń dysk (odłączenie kabla nie kończy trybu).
Słowa: DFU, bootloader, double reset, reset, enter-dfu, T1000-E, OTAFIX, tryb bootloadera, dysk USB, nRF52
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/entering-bootloader-mode/ (sprawdzono 2026-10)

### Jak wgrać firmware UF2 na nRF52 (przeciągnij i upuść)?
Pobierz i rozpakuj najnowszy firmware, podłącz USB, wejdź w tryb bootloadera (podwójny reset). Pojawi się dysk z plikami CURRENT.UF2, INDEX.HTM i INFO_UF2.TXT. Skopiuj na niego plik firmware-NAZWA_URZĄDZENIA-X.X.X-xxxxxxx.uf2 pasujący do urządzenia i poczekaj na restart. Sprawdź, czy masz RAK4631, a nie RAK4631-R (sprzęt identyczny, ale RAK4631-R wymaga najpierw zmiany bootloadera). Przy aktualizacji ze starej wersji możliwa jest pętla restartów przez stare dane: wtedy potrzebny jest pełny factory erase.
Słowa: UF2, CURRENT.UF2, INFO_UF2.TXT, drag and drop, RAK4631-R, pętla restartów, crash loop, wgrywanie nRF52
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/drag-n-drop/ (sprawdzono 2026-10)

### Jak wgrać firmware na RP2040?
Pobierz i rozpakuj firmware, przytrzymaj przycisk BOOTSEL podczas podłączania USB, pojawi się dysk z INDEX.HTM i INFO_UF2.TXT; skopiuj na niego odpowiedni plik .uf2 i poczekaj na restart.
Słowa: RP2040, BOOTSEL, RAK11310, Pico, uf2, dysk
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/drag-n-drop/ (sprawdzono 2026-10)

### Jak zrobić Factory Erase na nRF52 i co kasuje?
Kasuje wszystko, co Meshtastic przechowuje: ustawienia, klucze szyfrowania, parowania Bluetooth i bazę węzłów; bootloader i firmware zostają. Sprawdź INFO_UF2.TXT: jeśli jest wiersz Factory-Erase: (OTAFIX 2.4+), skopiuj meshtastic_factory_erase.uf2 z wydań Adafruit_nRF52_Bootloader_OTAFIX (jeden plik dla wszystkich płytek nRF52); dysk zniknie na ok. pół sekundy i wróci po kilku sekundach, co oznacza koniec, potem skopiuj firmware lub odłącz. Bez takiego wiersza użyj pliku zgodnego z wersją SoftDevice: Meshtastic_nRF52_factory_erase_v3_S140_7.3.0.uf2 (SoftDevice 7.x.x) lub ..._6.1.0.uf2 (6.x.x), a potem uruchom meshtastic --noproto i sprawdź komunikat "Formatting... Done". Zły plik może uszkodzić SoftDevice i wymagać programatora. Wysunięcie dysku zamiast czekania na jego powrót od razu resetuje urządzenie.
Słowa: factory erase, wymazanie, reset fabryczny, meshtastic_factory_erase.uf2, SoftDevice, S140, kasowanie, nRF52
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/nrf52-erase/ (sprawdzono 2026-10)

### Jak zaktualizować lub odzyskać bootloader nRF52?
Wersję sprawdzisz w INFO_UF2.TXT. Metody: (1) aplikacja Meshtastic na Androidzie przez USB: Advanced → Firmware Update → Upgrade bootloader, wskaż dysk dwa razy (bootloader, potem firmware); nie jest dostępna na iOS. (2) plik UF2 przeciągnięty w trybie bootloadera, potem wgraj firmware. (3) adafruit-nrfutil (pip install adafruit-nrfutil, weryfikuj SHA256, polecenie z --singlebank --touch 1200 ... dfu serial); ta metoda nie sprawdza, czy pakiet pasuje do płytki, więc zły pakiet może zablokować urządzenie. (4) debugger SWD (DAPLink, J-Link), gdy inne zawiodą. Po metodzie 3 na OTAFIX 2.0+ urządzenie może nie pokazywać dysku ani portu i czekać na aktualizację Bluetooth OTA: to nie błąd.
Słowa: bootloader, aktualizacja bootloadera, adafruit-nrfutil, SWD, J-Link, DAPLink, brick, odzyskiwanie, OTAFIX, cegła
Źródło: https://meshtastic.org/docs/getting-started/flashing-firmware/nrf52/update-nrf52-bootloader/ (sprawdzono 2026-10)
