# Moduły firmware: ustawienia i ograniczenia

### Jak działa moduł telemetrii i jakie ma domyślne interwały?
Telemetria urządzenia (bateria, napięcie, czas pracy) i środowiska domyślnie wysyłana co 1800 s (30 minut); aplikacja podłączona do węzła dostaje metryki urządzenia co minutę niezależnie od ustawienia. Czujniki środowiska, jakości powietrza, mocy i zdrowia są domyślnie wyłączone i trzeba je włączyć. Sensory I2C wykrywane są automatycznie przy starcie. Wyświetlanie °F dotyczy tylko ekranu urządzenia (czujniki zawsze mierzą w °C). Dokumentacja jest niespójna (domyślnie 0 = 30 minut w jednym miejscu, 5 minut w przykładzie CLI): sprawdź na swoim firmware.
Słowa: telemetria, telemetry, interwał, czujnik, temperatura, wilgotność, bateria, 1800, environment, air quality
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Jakie czujniki I2C obsługuje telemetria?
Temperatura/ciśnienie/wilgotność: AHT10/AHT20 (0x38), BMP280/BME280/BMP388/BMP390/BME68x/DPS310 (0x76, 0x77), SHTC3 (0x70), SHT31/SHT4X (0x44, 0x45), MCP9808 (0x18), LPS22 (0x5D, 0x5C). Prąd i napięcie: INA219/INA226/INA260 (0x40, 0x41, 0x43), INA3221 (0x42), MAX17048 (0x36, bateria). Światło: OPT3001, VEML7700 (0x10), TSL2591/TSL2561 (0x29), BH1750 (0x23), UV LTR390UV (0x53). CO2: SCD30 (0x61), SCD4X (0x62). Pyły: PMSA003I (0x12), HM330X, SEN5X (0x69), SEN6X (0x6B). Inne: AS3935 (błyskawice), RadSens (promieniowanie), DS18B20 przez DS2482, ADS1X15, NAU7802, MAX30102, MLX90614.
Słowa: BME280, BMP280, SHT31, INA219, INA226, SCD40, PMS5003, VEML7700, adres I2C, czujnik, sensor, obsługiwane czujniki
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Jakie metryki środowiskowe są wysyłane i co z nowymi czujnikami?
Metryki środowiska ograniczone są do temperatury, wilgotności, ciśnienia, rezystancji gazu (AQI), napięcia i prądu. Nowy czujnik wymaga biblioteki zgodnej z GPL i zmieszczenia się w limitach rozmiaru firmware. Telemetria zdrowia i opcje czujników wymagają kompilacji firmware z odpowiednimi makrami. Dla AS3935 podłącz pin IRQ i zdefiniuj AS3935_IRQ w wariancie, by uniknąć odczytu I2C co sekundę.
Słowa: AQI, nowy czujnik, kompilacja, makro, AS3935, SEN55, SEN66
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Jak działa Range Test i czego uważać?
Włącz moduł na nadajniku i odbiorniku (enabled). Nadajnik wysyła pakiety co zadaną liczbę sekund (sender; sugerowane 30 s dla Long Fast, 15 s dla Medium i Short Fast; 0 wyłącza). Moduł wyłącza się sam po 8 godzinach, ale zużywa dużo czasu antenowego i zapycha kanał, więc wyłącz go po teście. Opcja save zapisuje rangetest.csv (tylko ESP32; zapis staje, gdy zostaje mniej niż 50 KB), plik pobierzesz z meshtastic.local/rangetest.csv i wczytasz do Google Earth, My Maps lub uMap. Ustaw wysoką precyzję pozycji na kanale, inaczej zapisane położenie będzie przybliżone.
Słowa: range test, test zasięgu, rangetest.csv, zasięg, sender, save, 8 godzin, uMap
Źródło: https://meshtastic.org/docs/configuration/module/range-test/ (sprawdzono 2026-10)

### Jak skonfigurować External Notification (buzzer, dioda, wibracja)?
Wymaga dołączonego urządzenia (bez niego moduł nic nie robi). Do trzech niezależnych wyjść GPIO: ogólne/LED (output), wibracja (output_vibra), buzzer (output_buzzer). active określa polaryzację, alert_message reaguje na wiadomości tekstowe, alert_bell na znak dzwonka ASCII 0x07 (obecnie wysyła go tylko Python API), output_ms czas aktywności (0 = 1000 ms), nag_timeout powtarza alert do wciśnięcia przycisku, use_pwm użyje buzzera PWM z melodiami RTTTL i pinu device.buzzer_gpio. Na ESP32 piny 34–39 są tylko wejściowe. Błędne ustawienia GPIO mogą uszkodzić sprzęt: poznaj schemat.
Słowa: external notification, powiadomienie zewnętrzne, buzzer, wibracja, dioda, GPIO, RTTTL, dzwonek, output_ms, nag
Źródło: https://meshtastic.org/docs/configuration/module/external-notification/ (sprawdzono 2026-10)

### Jak działa moduł Serial i jego tryby?
Domyślnie wyłączony. Tryby: SIMPLE (surowe dane jako pakiety na kanale o nazwie serial, do 237 bajtów), PROTO (API klienta protobuf), TEXTMSG (tekst rozgłaszany na kanale domyślnym), NMEA (co 2 s $GNGGA i $GPWPL), CALTOPO (co 10 s waypointy dla CalTopo), WS85 (wiatromierz Ecowitt), VE_DIRECT (Victron), MS_CONFIG (MeshSolar), LOG i LOGTEXT. Domyślnie 38400 baud, 8N1; TX i RX łącz na krzyż. Przykładowe piny: T-Beam v1.1 TXD 15/RXD 35 (alternatywnie 14/13), RAK4631 RAK19007 TXD 16/RXD 15 (GPS Mode = Not_Present), SenseCAP T1000-E 16/17 (logika 3,3 V).
Słowa: serial, UART, NMEA, CalTopo, VE.Direct, Victron, baud, 38400, TXD, RXD, textmsg, proto
Źródło: https://meshtastic.org/docs/configuration/module/serial/ (sprawdzono 2026-10)

### Jak działa moduł Canned Message (gotowe wiadomości)?
Lista gotowych wiadomości rozdzielonych znakiem | (łącznie do 200 bajtów). Źródło wejścia (Input Source): _any (domyślnie), rotEnc1 (enkoder), upDownEnc1 (też RAK14006), scanAndSelect (jeden przycisk: krótkie naciśnięcie przewija, długie wysyła na kanał główny), cardkb (M5Stack CardKB, RAK14004), serialkb (Chatter). Piny enkodera A, B i Press ustawia się w zakresie GPIO 1–39. Moduł wymaga dołączonego urządzenia wejściowego; błędny GPIO może uszkodzić sprzęt. RAK 3x4: czwarty rząd nie jest skanowany (przyciski 4, 8, 12 nie działają).
Słowa: canned message, gotowe wiadomości, enkoder, CardKB, klawiatura, przycisk, wiadomości predefiniowane
Źródło: https://meshtastic.org/docs/configuration/module/canned-message/ (sprawdzono 2026-10)

### Do czego służy Neighbor Info i jakie ma limity?
Moduł jest informacyjny i nie wpływa na routing; domyślnie wyłączony. Interwał domyślnie 6 godzin (21600 s) i nie może być niższy niż 4 godziny (14400 s). Transmit Over LoRa (firmware 2.5.13+, domyślnie wyłączone): wysyła informacje przez radio na kanale głównym, ale tylko gdy kanał główny nie jest publicznym kanałem z domyślnym kluczem i nazwą; bez tego dane idą tylko do MQTT i aplikacji.
Słowa: neighbor info, sąsiedzi, topologia, interwał 4 godziny, 21600
Źródło: https://meshtastic.org/docs/configuration/module/neighbor-info/ (sprawdzono 2026-10)

### Jak działa Remote Hardware (zdalne GPIO) i czy jest bezpieczny?
Zdalne ustawianie i odczyt pinów GPIO (obecnie funkcje set/read tylko przez Python CLI). Dostęp jest groźny: błędne opcje mogą fizycznie uszkodzić sprzęt. Piny wybiera się maskami bitowymi (GPIO 1 = 0x2, GPIO 4 = 0x10, każdy kolejny pin podwaja wartość). Aby zablokować niepowołanych, utwórz kanał o nazwie gpio na obu węzłach i włącz moduł na obu. Wyniki lecą przez sieć i, przy włączonym MQTT, także przez MQTT. Szybkie zmiany (przyciski) mogą umknąć: użyj modułu Detection Sensor. W firmware powyżej 2.5.3 trzeba zbudować własny firmware, usuwając flagę -DMESHTASTIC_EXCLUDE_REMOTEHARDWARE=1 z platformio.ini.
Słowa: remote hardware, zdalne GPIO, gpio, maska, kanał gpio, platformio.ini, bezpieczeństwo, przekaźnik
Źródło: https://meshtastic.org/docs/configuration/module/remote-hardware/ (sprawdzono 2026-10)

### Jak działa Detection Sensor (czujnik zdarzeń na GPIO)?
Monitoruje pin GPIO pod kątem stanu wysokiego lub niskiego i wysyła alerty tekstowe; do czujników ruchu, kontaktronów i układów otwarty/zamknięty. Wymaga firmware i aplikacji 2.2.2+. Ustawienia: Minimum Broadcast Interval (odstęp między alertami przy zmianie stanu), State Broadcast Interval (cykliczny raport stanu; 0 = tylko przy zmianie), Send Bell, Friendly Name (do 20 znaków, "Motion" daje "Motion detected"), Monitor Pin, Detection Triggered High, Use Pull-up (INPUT_PULLUP, ma sens, gdy płytka ma rezystory podciągające). Klucze CLI: detection_sensor.enabled, minimum_broadcast_secs, state_broadcast_secs, send_bell, name, monitor_pin, detection_triggered_high, use_pullup.
Słowa: detection sensor, czujnik ruchu, kontaktron, PIR, GPIO, alert, drzwi, monitor_pin, pull-up
Źródło: https://meshtastic.org/docs/configuration/module/detection-sensor/ (sprawdzono 2026-10)

### Do czego służy PAX counter i jakie ma wymagania?
Liczy pobliskie osoby lub urządzenia, skanując sygnały Wi-Fi i BLE (sklepy, muzea, miejsca publiczne). Wymaga firmware 2.2.17+, urządzenia na ESP32 i wyłączonych Wi-Fi oraz Bluetooth w ustawieniach. Update Interval to minimalny odstęp między wiadomościami o zmianie liczby (przykład CLI: 900 s). CLI: meshtastic --set paxcounter.enabled true.
Słowa: paxcounter, pax counter, liczenie ludzi, wifi ble, ESP32, licznik, ruch pieszy
Źródło: https://meshtastic.org/docs/configuration/module/paxcounter/ (sprawdzono 2026-10)
