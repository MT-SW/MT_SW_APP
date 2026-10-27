# Urządzenia: T-Beam, T-Echo i T1000-E (szczegóły)

### LILYGO T-Beam: wersje, GPS i akumulator 18650
Modele: T-Beam S3-Core, Supreme i T-Beam 1W. S3-Core i Supreme: ESP32-S3, SX1262, pasma 433/868/915 MHz, USB-C, antena U.FL; ekran OLED 1,3" tylko w Supreme; GPS do wyboru: u-blox NEO-M10S (GPS, GLONASS, Galileo, BeiDou, lepsza czułość) albo Quectel L76K (GPS, BeiDou, GLONASS, tańszy). T-Beam 1W: ESP32-S3FN8, 16 MB flash, 8 MB PSRAM, SX1262 ze wzmacniaczem 1 W do 32 dBm, pasma 868/915, GPS L76K, ekran SH1106 128×64. Koszyk 18650 jest na tylnej stronie (poza S3-Core) i przyjmuje tylko ogniwa niezabezpieczone z płaskim biegunem, bo ogniwa z wypukłym biegunem i chronione mają zwykle 65–70 mm. Plik firmware: firmware-tbeam-s3-core-… (S3-Core i Supreme) lub firmware-t-beam-1w-… (1W).
Słowa: T-Beam, T-Beam Supreme, T-Beam 1W, 18650, NEO-M10S, L76K, ogniwo płaskie, AXP2101, firmware-tbeam-s3-core
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### T-Beam: ostrzeżenia i tryb wgrywania
Podłącz antenę przed wgraniem na Supreme i przed nadawaniem na 1W, bo inaczej możesz uszkodzić radio. Na S3-Core i Supreme fabrycznie jest SoftRF, więc Meshtastic trzeba wgrać. Czytnik microSD w Supreme nie jest zaimplementowany w Meshtastic. T-Beam 1W używa zarządzania AXP2101 i wymaga stabilnego zasilania stopnia RF; nie ładuje podłączonego pakietu 7,4 V. Tryb wgrywania: przycisk "1200bps reset" we flasherze albo ręcznie: przytrzymaj BOOT przy podłączaniu USB i puść po 2–3 sekundach.
Słowa: T-Beam antena, SoftRF, microSD, 1200bps, BOOT, tryb wgrywania, pakiet 7,4 V, zasilanie 1W, ostrzeżenie
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### LILYGO T-Echo: dane techniczne
nRF52840 z Bluetooth 5.0 i NFC, SX1262 w pasmach 433/868/915 MHz, złącze U.FL, przyciski Reset, Program i pojemnościowy, opcjonalny czujnik BME280, w zestawie obudowa i akumulator. Ekran e-ink 1,54". GPS L76K (GPS, BeiDou, GLONASS, QZSS). T-Echo Plus ma ekran SPI 200×200 i akumulator 2400 mAh. Plik firmware: firmware-t-echo-….uf2. Źródło nie opisuje kroków wgrywania (podwójny reset i podobne); sprawdź stronę przycisków T-Echo.
Słowa: T-Echo, T-Echo Plus, e-ink, nRF52840, L76K, BME280, UF2, 2400 mAh, firmware-t-echo
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/techo/ (sprawdzono 2026-10)

### Seeed T1000-E: bateria, IP65, przyciski i wgrywanie UF2
Akumulator 700 mAh, ładowanie magnetycznym kablem USB, obudowa IP65, wbudowany buzzer. Wgrywanie: plik firmware-tracker-t1000-e-….uf2, wymagany bootloader Meshtastic OTAFIX w wersji 2.5 lub nowszej. Kroki: podłącz USB, przytrzymaj przycisk 3 s przy włączaniu, poczekaj na pojawienie się dysku USB i skopiuj plik .uf2; wysunięcie dysku bez pliku kończy tryb bootloadera bez wgrywania. Bootloader dostarczany przez Seeed nie reaguje na przycisk: zaktualizuj go wg przewodnika Seeed lub użyj meshtastic --enter-dfu przez USB. Przyciski: długie naciśnięcie 5 s wyłącza, podwójne wysyła pozycję, potrójne przełącza GPS na żądanie, poczwórne wycisza.
Słowa: T1000-E, UF2, OTAFIX, bootloader, enter-dfu, przyciski, IP65, 700 mAh, karta-tracker, wyciszenie
Źródło: https://meshtastic.org/docs/hardware/devices/seeed-studio/sensecap/card-tracker/ (sprawdzono 2026-10)
