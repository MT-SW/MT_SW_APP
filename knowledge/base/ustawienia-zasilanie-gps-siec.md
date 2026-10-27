# Ustawienia radia: zasilanie, GPS, Bluetooth, sieć

### Jak działa oszczędzanie energii (Power Saving)?
Power Saving (domyślnie wyłączone) wyłącza Bluetooth, port szeregowy, Wi-Fi i ekran, co pomaga przy słabych źródłach jak panel słoneczny; zmiany nadal możliwe przyciskiem, resetem lub kanałem admin. Wait Bluetooth Interval: czas do wyłączenia BLE bez połączenia (0 = 1 minuta). Light Sleep Interval (tylko ESP32): CPU śpi, LoRa i GPS działają, BLE wyłączone (0 = 5 minut). Minimum Wake Interval: 0 = 10 sekund.
Słowa: oszczędzanie energii, power saving, sleep, light sleep, bateria, solar, uśpienie, deep sleep
Źródło: https://meshtastic.org/docs/configuration/radio/power/ (sprawdzono 2026-10)

### Co to Shutdown after losing power i ADC Multiplier?
Shutdown after losing power (domyślnie 0 = wyłączone): wyłącza urządzenie po zadanej liczbie sekund bez zasilania zewnętrznego; najpierw sprawdź, czy płytka wykrywa zasilanie zewnętrzne, bo niektóre wymagają korekty ADC Multiplier. ADC Multiplier Override (0 = wartości firmware): przelicznik dzielnika napięcia pinu baterii do obliczenia napięcia; dokumentacja podaje zakres 2–6 (w tabeli CLI 2–4) i kalkulator kalibracji. Device Battery INA2xx Address: adres I2C (dziesiętnie, 0x40 = 64) czujnika INA jako źródła napięcia baterii; ignorowane przy układach PMU (np. T-Beam).
Słowa: napięcie baterii, ADC, INA219, INA226, INA260, pomiar baterii, zasilanie zewnętrzne, shutdown
Źródło: https://meshtastic.org/docs/configuration/radio/power/ (sprawdzono 2026-10)

### Jak ustawić GPS i pozycję stałą?
GPS Mode: DISABLED, ENABLED, NOT_PRESENT. GPS Update Interval w sekundach (0 = 2 minuty; bardzo duża wartość = tylko raz przy starcie). Fixed Position (domyślnie wyłączone): węzeł podaje zapisaną pozycję z GPS, z telefonu lub ręczną; usuwa się ją poleceniem --remove-position, ustawia --setlat/--setlon. Po każdym poleceniu CLI urządzenie się restartuje, więc łącz kilka ustawień w jedno polecenie.
Słowa: GPS, pozycja stała, fixed position, setlat, setlon, interwał GPS, bez GPS
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10)

### Jak często węzeł wysyła pozycję (smart broadcast)?
Smart Broadcast (domyślnie włączony): częściej wysyła pozycję w ruchu, rzadziej w bezruchu. Minimalny dystans 0 = 100 m; minimalny odstęp: dokumentacja podaje 30 s w tekście i 15 minut w tabeli CLI (sprawdź na swoim firmware). Broadcast Interval (gdy smart wyłączony): 0 = 15 minut. Position Flags to pola bitowe (ALTITUDE, DOP, HVDOP, SATINVIEW, HEADING, SPEED i inne).
Słowa: pozycja, smart position, broadcast interval, 15 minut, 100 m, flagi pozycji, HDOP, PDOP
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10)

### Jak działa Bluetooth i PIN?
Enabled: włącza Bluetooth. Tryby parowania: RANDOM_PIN (nowy PIN przy każdym uruchomieniu), FIXED_PIN (własny PIN, domyślnie 123456, 6 cyfr), NO_PIN (bez PIN). Przy pierwszym uruchomieniu urządzenie z ekranem ma RANDOM_PIN (zostaje, nawet gdy ekran później usuniesz, więc zmień tryb przed jego zdjęciem), bez ekranu FIXED_PIN. Dokumentacja zdecydowanie zaleca zmianę domyślnego PIN. Na ESP32 włączenie Wi-Fi wyłącza Bluetooth.
Słowa: bluetooth, BLE, PIN, parowanie, 123456, fixed pin, random pin, no pin, bezpieczeństwo
Źródło: https://meshtastic.org/docs/configuration/radio/bluetooth/ (sprawdzono 2026-10)

### Jak skonfigurować Wi-Fi i Ethernet w węźle?
Wi-Fi Enabled (domyślnie wyłączone), SSID do 32 znaków, PSK do 64 (wielkość liter ma znaczenie). Tylko sieć 2,4 GHz; ESP32 łączy się jako klient, tryb SoftAP nie jest wspierany. Włączenie Wi-Fi wyłącza Bluetooth. Po pierwszym włączeniu Wi-Fi lub Ethernetu start trwa o 20–30 s dłużej (generowanie kluczy SSL). Z jednym urządzeniem w sieci wejdziesz na http://meshtastic.local, przy kilku użyj adresów IP. Serwer NTP domyślnie meshtastic.pool.ntp.org, IPv4 domyślnie DHCP (można STATIC), jest też serwer rsyslog i UDP broadcast.
Słowa: wifi, wi-fi, ethernet, SSID, NTP, DHCP, meshtastic.local, 2.4 GHz, rsyslog, UDP, sieć
Źródło: https://meshtastic.org/docs/configuration/radio/network/ (sprawdzono 2026-10)
