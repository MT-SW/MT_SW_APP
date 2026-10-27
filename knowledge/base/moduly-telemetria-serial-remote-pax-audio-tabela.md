# Moduły: telemetria, serial, zdalny sprzęt, paxcounter, oświetlenie, audio (wartości domyślne)

### Telemetria: pola, interwały i czujniki
Wszystkie moduły telemetrii są domyślnie wyłączone (device_telemetry, environment, air_quality, power, health). Interwały 0 oznaczają 30 minut (1800 s); przykład CLI na stronie mówi o 5 minutach, co przeczy temu, więc wartość domyślną traktuj ostrożnie. Metryki urządzenia (bateria, napięcie, wykorzystanie kanału, airtime) trafiają do podłączonej aplikacji co minutę niezależnie od interwału w sieci. Fahrenheit zmienia tylko wyświetlanie na ekranie, czujnik zawsze mierzy w Celsjuszach. Pomiaru napięcia baterii nie trzeba włączać w power metrics. Moduły środowiska, jakości powietrza i zdrowia muszą być włączone, by dane szły do sieci; zdrowie wymaga firmware z odpowiednimi makrami. Czujniki I²C wykrywane są przy starcie.
Słowa: telemetria, device_update_interval, environment, air quality, power metrics, Fahrenheit, czujniki, I2C, BME280, INA219
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Jakie czujniki obsługuje telemetria?
Z listy na stronie: temperatura, wilgotność, ciśnienie: AHT10/AHT20, BMP085/180/280/388/390, BME280, BME68x, DPS310, SHTC3, SHT31, SHT4X, SPA06. Jakość powietrza: SCD30, SCD4X, SEN5X, SEN6X, SFA30, PMSA003I, HM330X. Światło i UV: OPT3001, VEML7700, TSL2591, TSL2561, LTR390UV, BH1750. Zasilanie: INA219, INA226, INA260, INA3221, MAX17048. Zdrowie: MAX30102 (tętno, SpO2, temperatura ciała), MLX90614, MLX90632. Inne: AS3935 (wyładowania atmosferyczne), RCWL9620 (odległość ultradźwiękowa), RadSens (promieniowanie), DFROBOT_LARK i RAIN, DS18B20 (1-Wire), NAU7802 i ADS1X15 (przetworniki ADC), PCT2075, LPS22.
Słowa: czujniki, BME280, SCD4X, PMSA003I, INA219, MAX30102, AS3935, DS18B20, VEML7700, I2C czujnik pogody
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Moduł Serial: tryby, prędkość, piny
Tryby (domyślnie SIMPLE): SIMPLE przesyła surowe bajty jako pakiety na kanale o nazwie serial, który musi istnieć; PROTO udostępnia API klienta Protobuf na porcie (bez czytelnego tekstu); TEXTMSG rozgłasza tekst z portu jako wiadomość na kanale domyślnym, a odebrane drukuje jako „Skrót: tekst”; NMEA co 2 s wysyła $GNGGA z pozycją urządzenia i $GPWPL dla węzłów z pozycją; CALTOPO co 10 s wysyła $GPWPL dla CalTopo/SARTopo. Baud domyślny 38400 (od 110 do 921600). Piny rxd i txd: 0 oznacza nieustawione; łącz na krzyż (TX urządzenia do RX adaptera). Przykłady: T1000-E TXD 16 i RXD 17; T-Beam 1.1 TXD 15 i RXD 35, polecana para TXD 14 i RXD 13. Timeout (tylko SIMPLE): 0 to 250 ms. Echo (false) odsyła wysłane pakiety.
Słowa: serial module, SIMPLE, PROTO, TEXTMSG, NMEA, CALTOPO, baud 38400, rxd, txd, UART, port szeregowy
Źródło: https://meshtastic.org/docs/configuration/module/serial/ (sprawdzono 2026-10)

### Remote Hardware: zdalne sterowanie GPIO
Moduł pozwala czytać, zapisywać i monitorować piny GPIO zdalnego węzła; odczyty i zmiany mogą iść też przez MQTT. Strona zastrzega, że zmiany pinów nie są niezawodne dla szybkich zdarzeń; do przycisków użyj Detection Sensor. Jedyna opcja to Enabled, włączona na obu węzłach. W firmware nowszym niż 2.5.3 wymaga to własnej kompilacji z usunięciem flagi -DMESHTASTIC_EXCLUDE_REMOTEHARDWARE=1 w platformio.ini. Ustawianie i odczyt GPIO jest na razie możliwe tylko przez Python CLI. Maski: GPIO n to bit n (GPIO4 = 0x10, GPIO21 = 0x200000). Ostrzeżenie ze strony: dostęp do GPIO jest z natury niebezpieczny, błędne opcje mogą zniszczyć sprzęt; poznaj schemat urządzenia. Aby zablokować obcych, utwórz kanał gpio na obu węzłach.
Słowa: remote hardware, GPIO zdalnie, maska pinów, kanał gpio, platformio, Python CLI, sterowanie przekaźnikiem
Źródło: https://meshtastic.org/docs/configuration/module/remote-hardware/ (sprawdzono 2026-10)

### Paxcounter: zliczanie osób po Wi-Fi i BLE
Moduł liczy osoby przechodzące w pobliżu, skanując adresy MAC Wi-Fi i BLE urządzeń. Wymaga firmware 2.2.17 lub nowszego, tylko ESP32, a Wi-Fi i Bluetooth muszą być wyłączone w ustawieniach sieci i Bluetooth. Ustawienia: Enabled i Update Interval (sekundy między wiadomościami przy zmianie stanu; przykład CLI 900). Android: Settings > Paxcounter. CLI: meshtastic --set paxcounter.enabled true oraz --set paxcounter.paxcounter_update_interval 900. Strona nie zawiera uwagi o prywatności, ale skoro wykrywa adresy MAC, przechodnie są wykrywani przez swoje urządzenia; zanim użyjesz go w miejscu publicznym, sprawdź przepisy o danych (to wniosek ogólny, nie ze źródła).
Słowa: paxcounter, liczenie osób, MAC, Wi-Fi scan, BLE scan, ruch pieszy, RODO
Źródło: https://meshtastic.org/docs/configuration/module/paxcounter/ (sprawdzono 2026-10)

### Ambient Lighting i moduł Audio (Codec2)
Ambient Lighting: LED State (CLI: 1 włączone, 0 wyłączone), Current (domyślnie 10), Red/Green/Blue 0 do 255; zbudowany pod moduł RAK14001 RGB z układem NCP5623 po I²C, wymaga firmware 2.2.5. Audio (Codec2): eksperymentalny, tylko ESP32 z radiem SX128x w paśmie 2,4 GHz (LilyGo TLora 2.1-1.8 i T3S3), z wzmacniaczem MAX98357 i mikrofonem MSM261S4030H0; piny I²S trzeba ustawić samemu, PTT domyślnie GPIO 39. Bitrate: CODEC2_DEFAULT, 3200 do 700; tekst podaje 700B, tabela CLI DEFAULT. Pasma poniżej 1 GHz są za wąskie dla ciągłego dźwięku nawet w Short Fast. Dźwięk jest półdupleksowy i mono (mikrofon na lewym kanale, głośnik na prawym). Nie ma konfiguracji w iOS, iPadOS i macOS.
Słowa: ambient lighting, RAK14001, NCP5623, audio, codec2, PTT, SX128x, 2.4 GHz, TLora T3S3, MAX98357, I2S
Źródło: https://meshtastic.org/docs/configuration/module/audio/ (sprawdzono 2026-10)
