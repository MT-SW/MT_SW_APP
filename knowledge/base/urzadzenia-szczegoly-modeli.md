# Szczegóły wybranych urządzeń: T-Beam, Heltec, RAK, SenseCAP Solar

### LILYGO T-Beam: warianty S3-Core, SUPREME i 1W
Wszystkie warianty poza S3-Core mają z tyłu koszyk na 18650, który według dokumentacji pasuje tylko do ogniw płaskich, niechronionych (ogniwa z wypustką i chronione mają zwykle ponad 65 mm, często blisko 70 mm). S3-Core: ESP32-S3, SX1262 (433, 868 lub 915 MHz), GPS u-blox NEO-M10S lub Quectel L76K, USB-C i U.FL; fabrycznie ma SoftRF, więc trzeba wgrać Meshtastic; plik firmware-tbeam-s3-core. SUPREME: moduł S3-Core plus OLED 1,3", BME280, IMU QMI8658, magnetometr QMC6310, RTC PCF8563 i czytnik microSD (w Meshtastic nieobsługiwany). T-Beam 1W: ESP32-S3FN8 z 16 MB flash i 8 MB PSRAM, SX1262 ze wzmacniaczem 1 W (do 32 dBm), tylko 868 lub 915 MHz, GPS L76K, wentylator, AXP2101; wymaga stabilnego zasilania i nie ładuje zewnętrznego pakietu 7,4 V; przed nadawaniem podłącz antenę.
Słowa: T-Beam, S3-Core, Supreme, 1W, 18650 płaskie, SoftRF, NEO-M10S, L76K, AXP2101, wentylator, pakiet 7,4 V
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### T-Beam: jak wejść w tryb wgrywania?
W przeglądarkowym flasherze użyj przycisku "1200bps reset" (SUPREME, 1W). Ręcznie: odłącz urządzenie, przytrzymaj BOOT, podłącz USB i puść BOOT po 2–3 sekundach; w flasherze wybierz "Tbeam S3 Core". Przed wgraniem koniecznie podłącz antenę, żeby nie uszkodzić układu radiowego.
Słowa: tryb wgrywania, BOOT, 1200bps, flasher, T-Beam S3, download mode, antena przed wgraniem
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### Heltec LoRa32 V4 i V4-R8: czym się różnią?
V4: ESP32-S3R2, SX1262, OLED 0,96", do 28 dBm, USB-C, antena U.FL; firmware firmware-heltec-v4. V4-R8: ESP32-S3R8, 16 MB flash, 8 MB PSRAM, wersja OLED lub TFT, inny układ pinów; używaj firmware heltec-v4-r8-oled lub -tft (2.7.25 lub nowszy). Uwaga: V4-R8 nie wolno wgrywać z firmware zwykłego V4. Mostek USB CP2102 w V4-R8 nie ma zabezpieczenia ESD, więc ostrożnie z portem USB-C.
Słowa: Heltec V4, V4-R8, 28 dBm, PSRAM, OLED, TFT, CP2102, ESD, nie wgrywać V4 na R8, firmware Heltec
Źródło: https://meshtastic.org/docs/hardware/devices/heltec-automation/lora32/ (sprawdzono 2026-10)

### Heltec V3, V3.1, Wireless Stick Lite, Tracker i Paper
V3/V3.1: ESP32-S3FN8, SX1262, OLED 0,96", bez GPS; I²C na GPIO41 (SDA) i GPIO42 (SCL); V3.1 usunął tranzystor FDG6322C z zasilania i zmienił wartości filtra antenowego, firmware ten sam co V3. Wireless Stick Lite V3: bez ekranu i GPS, dwa złącza antenowe, antena LoRa obowiązkowa, Wi-Fi opcjonalna. Wireless Tracker v1.0/v1.1: ESP32-S3FN8, LCD 0,96", GNSS przez U.FL (v1.1: SDA 45, SCL 46); V2: dwuzakresowy GNSS UC6580, kolorowy TFT 160×80 i zarządzanie baterią i solarem. Wireless Paper: E-Ink 2,13", bez GPS; plik wireless-paper-v1_0 (v1.0) lub wireless-paper (v1.1). Ładowanie baterii może nie działać z kablem USB-C–USB-C; zalecany USB-A–USB-C.
Słowa: Heltec V3, V3.1, Wireless Stick Lite, Wireless Tracker, Wireless Paper, UC6580, GPIO41, ładowanie baterii, kabel USB-C, E-Ink
Źródło: https://meshtastic.org/docs/hardware/devices/heltec-automation/lora32/ (sprawdzono 2026-10)

### RAK WisBlock: płyta bazowa RAK19007 i złącza zasilania
RAK19007 (zastępuje wycofaną RAK5005-O): 1 miejsce na rdzeń, 1 moduł IO i 4 moduły czujników. Bateria: złącze JST PHR-2 dla LiPo 3,7 V (maks. prąd ładowania 350 mA wg źródła, wejście 3,3–4,3 V). Solar: JST ZHR-2 dla panelu 5 V (ładowanie przy 4,4–5,5 V). Zasilanie i debug przez USB-C, jeden przycisk reset, a przycisk użytkownika może używać pinu AIN1. Płyta wymaga rdzenia; zwykła RAK5005 nie jest zgodna z RAK5005-O. GPS: RAK12500 (u-blox Zoe-M8Q: GPS, GLONASS, QZSS, BeiDou; preferowany) w slocie A płyty RAK19007; RAK1910 (MAX-7Q) jest wycofany.
Słowa: RAK19007, RAK5005-O, WisBlock, JST PHR-2, JST ZHR-2, solar 5 V, RAK12500, RAK1910, slot A, płyta bazowa
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/wisblock/base-board/ i .../peripherals/ (sprawdzono 2026-10; poprawiono ładowanie: źródło podaje "350 mAh", co jest nieścisłością jednostki — prawdopodobnie 350 mA)

### SenseCAP Solar Node P1: dane i ograniczenia
nRF52840, SX1262, BLE 5.0, 862–930 MHz, USB-C, złącze Grove, antena RP-SMA 2 dBi; w obudowie wbudowany panel 5 W i cztery miejsca na ogniwa 18650; zasięg do 8–9 km na otwartym terenie. Wersja P1 nie zawiera baterii (własne 18650), P1 Pro ma cztery ogniwa 3350 mAh i moduł GPS L76K. Wgrany jest firmware Meshtastic (plik seeed_solar_node .uf2). Strona nie podaje stopnia ochrony IP ani szczegółów regulatora, tylko że urządzenie jest przeznaczone na zewnątrz.
Słowa: SenseCAP Solar Node, P1, P1 Pro, panel 5 W, 18650, RP-SMA, Seeed, solar, zewnątrz
Źródło: https://meshtastic.org/docs/hardware/devices/seeed-studio/sensecap/solar-node/ (sprawdzono 2026-10)
