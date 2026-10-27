# Konkretne urządzenia: Heltec, LILYGO, Seeed i inne

### Heltec WiFi LoRa 32 V3, V3.1, V4, V4-R8: dane i różnice
V3/V3.1: ESP32-S3FN8, radio SX1262, USB-C, złącze U.FL/IPEX do anteny LoRa, OLED 0,96", przyciski użytkownika i reset, bez GPS, I2C: SDA GPIO41, SCL GPIO42, firmware firmware-heltec-v3. V3.1 ma usunięty tranzystor FDG6322C z zasilania i zmienione wartości filtra antenowego (firmware ten sam). V4: ESP32-S3R2, SX1262, 863–870 i 902–928 MHz, opcja dużej mocy 28±1 dBm, interfejs solarny 1,25 mm 2-pin, interfejs GNSS 1,25 mm 8-pin, zgodny rozmiarem i pinami z V3. V4-R8: ESP32-S3R8, 16 MB flash, 8 MB PSRAM, OLED lub TFT, pinout niezgodny z V4: nie wgrywaj na niego firmware V4 (potrzebny osobny firmware-heltec-v4-r8-oled/tft, Meshtastic 2.7.25+).
Słowa: Heltec V3, V4, V3.1, V4-R8, WiFi LoRa 32, ESP32-S3, SX1262, OLED, GPIO41, PSRAM
Źródło: https://meshtastic.org/docs/hardware/devices/heltec-automation/lora32/ (sprawdzono 2026-10)

### Znane problemy i ostrzeżenia dla Heltec LoRa 32
Ładowanie podłączonego akumulatora może nie działać przy kablu USB-C na USB-C: użyj kabla USB-A na USB-C. W V4 układ CP2102 (USB-UART) nie ma ochrony ESD, więc uważaj na port USB-C. Do flashowania ESP32-S3 zalecany jest przycisk "1200bps reset" w web flasherze (tryb pobierania Espressif); ręcznie: odłącz urządzenie, przytrzymaj USER, podłącz, po 2–3 sekundach puść. Podłącz antenę przed flashowaniem, żeby nie uszkodzić radia.
Słowa: ładowanie nie działa, kabel USB-C, ESD, CP2102, tryb pobierania, USER, 1200bps, Heltec problemy
Źródło: https://meshtastic.org/docs/hardware/devices/heltec-automation/lora32/ (sprawdzono 2026-10)

### LILYGO T-Beam S3-Core, Supreme i 1W
Wspólne (S3-Core, Supreme): ESP32-S3, Bluetooth 5 LE, SX1262, pasma 433/868/915 MHz, GNSS NEO-M10S lub Quectel L76K (zależnie od SKU), USB-C, U.FL. Supreme ma też ekran OLED 1,3", BME280, IMU QMI8658, magnetometr QMC6310, RTC PCF8563 i czytnik microSD (nieobsługiwany w Meshtastic). Fabrycznie jest tam SoftRF, więc trzeba wgrać Meshtastic (w flasherze wybierz "Tbeam S3 Core"). T-Beam 1W: ESP32-S3FN8 16 MB flash i 8 MB PSRAM, SX1262 ze wzmacniaczem do 32 dBm (868 i 915 MHz), GNSS L76K, OLED SH1106 128×64, zasilanie AXP2101, wentylator chłodzący przy dużej mocy; wymaga stabilnego zasilania toru RF i nie ładuje podłączonego pakietu 7,4 V.
Słowa: T-Beam, S3-Core, Supreme, 1W, ESP32-S3, NEO-M10S, L76K, AXP2101, SoftRF, wentylator, 32 dBm
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### Jakie akumulatory 18650 pasują do T-Beam?
Uchwyt z tyłu (wszystkie modele poza S3-Core) pasuje tylko do niezabezpieczonych ogniw 18650 z płaskim biegunem. Ogniwa zabezpieczone i z wypukłym biegunem są zwykle dłuższe niż 65 mm (często bliżej 70 mm) i się nie zmieszczą. Przy włączaniu Supreme nie kontynuuj bez podłączonej anteny, bo można uszkodzić radio.
Słowa: 18650, uchwyt, ogniwo, zabezpieczone, płaski biegun, 65 mm, bateria T-Beam, wymiary ogniwa
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tbeam/ (sprawdzono 2026-10)

### SenseCAP Card Tracker T1000-E: dane, przyciski, ograniczenia
nRF52840, radio Semtech LR1110, pasmo 865–928 MHz, GPS Mediatek AG3335, akumulator 700 mAh, ładowanie magnetycznym kablem na pogo-piny, obudowa IP65, rozmiar karty kredytowej, buzzer. Przyciski: długie naciśnięcie (5 s) wyłącza, podwójne wysyła doraźnie pozycję, potrójne przełącza GPS na żądanie, poczwórne wycisza lub włącza dźwięk; przytrzymanie przy starcie wchodzi w tryb bootloadera. Znany problem: LR1110 nie odbiera pakietów Meshtastic od starszych radii SX127x (naprawa wymagałaby zmiany niezgodnej wstecz); nadawanie do SX127x działa, a przy przekazaniu przez radio SX126x pakiety z SX127x można odbierać.
Słowa: T1000-E, SenseCAP, Card Tracker, LR1110, nRF52840, przyciski, bootloader, SX127x, kompatybilność radia
Źródło: https://meshtastic.org/docs/hardware/devices/seeed-studio/sensecap/card-tracker/ (sprawdzono 2026-10)

### LILYGO T-Echo
nRF52840 z Bluetooth LE 5.0 i NFC, radio SX1262, pasma 433/868/915 MHz, złącze U.FL, ekran e-paper 1,54", przyciski reset, program i dotykowy, GPS L76K (GPS, BeiDou, GLONASS, QZSS), opcjonalnie czujnik BME280, w zestawie obudowa i akumulator. Firmware: firmware-t-echo UF2. Wersja T-Echo Plus ma akumulator 2400 mAh (nie ta sama co T-Echo).
Słowa: T-Echo, e-paper, eInk, nRF52840, L76K, T-Echo Plus, UF2, BME280
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/techo/ (sprawdzono 2026-10)

### Klient WWW Meshtastic: wymagania i połączenia
Działa w przeglądarce: client.meshtastic.org lub własny hosting (kod na GitHubie). Najlepsze są przeglądarki Chromium (Chrome, Edge). Połączenie HTTP tylko z urządzeniami ESP32 (najpierw ustaw połączenie sieciowe węzła), Bluetooth przez Web Bluetooth API (głównie Chromium), USB przez Web Serial API (najmniej wsparcia platform, ale łączy bezpośrednio). Wersja hostowana wymaga HTTPS: węzły generują certyfikaty samopodpisane, więc otwórz najpierw https://ADRES_IP_WĘZŁA/ w przeglądarce i zaufaj certyfikatowi; adres IP znajdziesz na ekranie węzła, w tabeli DHCP routera lub w konsoli szeregowej.
Słowa: web client, client.meshtastic.org, Web Bluetooth, Web Serial, HTTPS, certyfikat samopodpisany, przeglądarka, meshtastic.local
Źródło: https://meshtastic.org/docs/software/web-client/ (sprawdzono 2026-10)

### Jak działa Meshtastic Site Planner (planowanie zasięgu)?
Narzędzie open source site.meshtastic.org przewiduje zasięg i pokrycie. Wejścia: nazwa, współrzędne, wysokość anteny nad ziemią, moc, częstotliwość, zysk anteny nadajnika; czułość odbiornika (-130 dBm dla domyślnego LongFast), wysokość i zysk anteny odbiorczej, maksymalny zasięg symulacji (powyżej 50 km liczy się dłużej), wysokość zabudowy/zieleni (clutter) i próg czułości. Wynik: kolorowa mapa pokrycia z RSSI (silny sygnał powyżej −110 dBm, poniżej −125 dBm przeszkody mogą ograniczać niezawodność). Teren z danych SRTM NASA o dokładności ok. 90 m, bez budynków i drzew; wyniki warto sprawdzić pomiarem w terenie.
Słowa: site planner, planowanie zasięgu, SRTM, RSSI, clutter, mapa pokrycia, symulacja, Mesh Link Planer
Źródło: https://meshtastic.org/docs/software/site-planner/ (sprawdzono 2026-10)
