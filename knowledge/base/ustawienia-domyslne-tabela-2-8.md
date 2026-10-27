# Ustawienia radia i ich wartości domyślne (dokumentacja 2.8)

### LoRa: wartości domyślne i ostrzeżenia, pole po polu
Region: UNSET, bez ustawienia regionu węzeł nie nadaje; wybierz zgodnie z lokalnym prawem (pasma krótkofalarskie wymagają licencji i nie wolno w nich szyfrować). Use Preset: true. Modem Preset: LONG_FAST; węzły muszą mieć ten sam preset, a LONG_TURBO z LONG_FAST nie rozmawiają. Bandwidth 250, Spread Factor 11 (zakres 5 do 12, starsze SX127x tylko 7 do 12), Coding Rate 5 (czyli 4/5, zakres 4 do 8); poniżej 62,5 kHz może być potrzebny TCXO. Max Hops: 3, maksimum 7. Transmit Power: 0 oznacza maksymalną dozwoloną moc ciągłą (zakres 0 do 30 dBm). Transmit Enabled: true (wyłączenie przydaje się przy wymianie anteny). Override Duty Cycle: false; ignoruje godzinny limit w Europie i może łamać przepisy. SX126x RX Boosted Gain: false, trochę więcej prądu za lepszą czułość. Frequency Offset: 0, tylko z przyrządami pomiarowymi.
Słowa: lora ustawienia, domyślne, region UNSET, hop limit, tx power, duty cycle, override, boosted gain, frequency offset, spread factor
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Frequency Slot, Ignore MQTT i OK to MQTT
Frequency Slot: 0 (UNSET) oznacza, że slot wynika z hasha nazwy kanału głównego; w innym wypadku 1 do maksimum regionu, a wszystkie węzły sieci muszą używać tego samego slotu. Ignore MQTT (false): odrzuca pakiety LoRa, które po drodze przeszły przez MQTT; wymaga firmware 2.2.19 lub nowszego na obu urządzeniach. OK to MQTT (false): zgoda na wysyłanie twoich pakietów do brokerów MQTT; gdy false, odbiorców prosi się o nieprzekazywanie, ale to prośba, a nie kontrola kryptograficzna, i dotyczy tylko kanałów z domyślnym lub eventowym kluczem.
Słowa: frequency slot, slot częstotliwości, ignore mqtt, ok to mqtt, uplink, 869.44165, kanał, hash nazwy
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Pozycja: domyślne interwały i znane niespójności dokumentacji
GPS mode: ENABLED, DISABLED lub NOT_PRESENT (zależnie od urządzenia). GPS update interval: 0 to co 2 minuty. Fixed position: false. Smart broadcast: true; minimalny dystans 0 oznacza według strony 100 m; minimalny odstęp 0 oznacza 30 sekund w jednym miejscu strony i 15 minut w innym (dokumentacja się tu nie zgadza, więc nie traktuj tej liczby jako pewnej). Broadcast interval: 0 to 15 minut. Flags: UNSET. Osobnego pola ani wartości dla pinu GPS EN strona nie podaje; opisuje pola position.rx_gpio (0 do 39) i position.tx_gpio (0 do 34).
Słowa: pozycja, gps_mode, smart broadcast, interwał pozycji, fixed position, rx_gpio, tx_gpio, position flags, 15 minut
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10)

### Zasilanie: oszczędzanie energii i uśpienie
Power Saving (false) wyłącza Bluetooth, Serial, Wi-Fi i ekran; dobre przy małych źródłach, jak solar. Shutdown after losing power (0, wyłączone): wyłącza urządzenie po czasie bez zasilania zewnętrznego; najpierw sprawdź, czy wykrywanie zasilania działa, bo błędny odczyt może wyłączać węzeł mimo zasilania. ADC Multiplier Override: 0 używa wartości z firmware, strona podaje zakres 2 do 6 w jednym miejscu i 2 do 4 w innym. Wait Bluetooth Interval: 0 to 1 minuta. Light Sleep Interval: 0 to 5 minut, tylko ESP32 (nRF52 i RP2040 bez efektu). Minimum Wake Interval: 0 to 10 sekund. INA2xx Address: adres I²C dziesiętnie czujnika INA jako źródła napięcia baterii, ignorowany tam, gdzie jest PMU (np. T-Beam). O „super deep sleep” strona nie mówi.
Słowa: power saving, oszczędzanie energii, light sleep, ADC multiplier, INA219, INA260, shutdown, solar zasilanie, wait bluetooth
Źródło: https://meshtastic.org/docs/configuration/radio/power/ (sprawdzono 2026-10)

### Sieć: Wi-Fi, Ethernet, NTP, rsyslog
Wi-Fi jest domyślnie wyłączone; SSID do 32 znaków, hasło do 64, wielkość liter ma znaczenie. Włączenie Wi-Fi wyłącza Bluetooth, więc działa jedna metoda połączenia naraz. ESP32 działają tylko jako klient Wi-Fi, tryb SoftAP nie jest obsługiwany. Ethernet domyślnie wyłączony. Tryb IPv4: domyślnie DHCP, można STATIC (adres, brama, maska, DNS) i dotyczy Ethernetu oraz Wi-Fi. NTP: domyślnie meshtastic.pool.ntp.org. Rsyslog: serwer i port, facility domyślnie user. Flagi: NO_BROADCAST i UDP_BROADCAST (rozgłaszanie UDP w sieci lokalnej), domyślnych wartości strona nie podaje. Każde polecenie CLI restartuje urządzenie, więc łącz kilka --set w jedno polecenie. Wpisywanie danych Wi-Fi przez niezaufaną sieć ujawnia ten ruch.
Słowa: wifi, ethernet, ntp, rsyslog, DHCP, static ip, udp broadcast, softap, sieć ustawienia, ESP32 klient
Źródło: https://meshtastic.org/docs/configuration/radio/network/ (sprawdzono 2026-10)

### Bluetooth: tryby parowania i PIN
Bluetooth jest domyślnie włączony, a na ESP32 wyłącza go włączone Wi-Fi. Tryby: RANDOM_PIN (PIN generowany w czasie działania), FIXED_PIN (twój PIN), NO_PIN (bez uwierzytelniania). Domyślny tryb ustawia się przy pierwszym uruchomieniu zależnie od tego, czy wykryto ekran: z ekranem RANDOM_PIN, bez FIXED_PIN. RANDOM_PIN zostaje, jeśli ekran później zdemontujesz, więc zmień tryb przed jego zdjęciem. Domyślny stały PIN to 123456; źródło ostrzega, że pozostawienie go to poważne ryzyko, więc zmień go na własny sześciocyfrowy. Wartość PIN działa tylko w trybie FIXED_PIN.
Słowa: bluetooth, PIN, 123456, fixed pin, random pin, no pin, parowanie, ekran, bezpieczeństwo bluetooth
Źródło: https://meshtastic.org/docs/configuration/radio/bluetooth/ (sprawdzono 2026-10)

### Ekran: pola i wartości domyślne
Screen On Duration: 0 to 10 minut. GPS format: DEC (domyślnie), DMS, UTM, MGRS, OLC, OSGR, MLS. Auto carousel: 0 wyłączone. Compass north: gdy włączone, północ stoi na górze kółka kompasu. Flip screen obraca obraz o 180 stopni. Units: METRIC lub IMPERIAL, dotyczy tylko ekranu urządzenia (aplikacje używają ustawień klienta). OLED: OLED_AUTO, SSD1306, SH1106, SH1107. Display mode: DEFAULT, TWOCOLOR, INVERTED, COLOR. Heading bold i Wake on tap or motion (wymaga akcelerometru lub dotykowego przycisku), domyślnie false. Jasności strona nie opisuje.
Słowa: ekran, display, screen timeout, GPS format, MGRS, UTM, OLED, flip screen, carousel, kompas, wake on tap
Źródło: https://meshtastic.org/docs/configuration/radio/display/ (sprawdzono 2026-10)

### Bezpieczeństwo: klucze, Managed Mode, konsola
Public Key: udostępniany innym węzłom do wyliczenia wspólnego sekretu i weryfikacji podpisów; numer węzła z niego wynika; para kluczy powstaje po ustawieniu regionu LoRa. Private Key: trzymaj w tajemnicy. Admin Key: do trzech kluczy publicznych uprawnionych do wiadomości administracyjnych, przyjmowanych tylko jako zaszyfrowany DM od wymienionego węzła. Managed Mode: blokuje zapis konfiguracji z aplikacji (odczyt zostaje), zmiany idą przez PKC; wymaga co najmniej jednego klucza admina, inaczej firmware to wyłączy; najpierw sprawdź, czy zdalna administracja działa. Serial Console (security.serial_enabled) i Debug Log (security.debug_log_api_enabled) sterują konsolą i logami przez serial lub Bluetooth. Admin Channel Enabled to stary, mniej bezpieczny sposób sterowania.
Słowa: managed mode, admin key, klucz prywatny, klucz publiczny, serial console, debug log, zdalna administracja, PKC, bezpieczeństwo
Źródło: https://meshtastic.org/docs/configuration/radio/security/ (sprawdzono 2026-10)
