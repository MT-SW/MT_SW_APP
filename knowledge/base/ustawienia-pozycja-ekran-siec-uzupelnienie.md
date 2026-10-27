# Uzupełnienie: pozycja, ekran, Bluetooth i sieć (wersja 2.8)

### Ustawienia pozycji: interwał, inteligentne nadawanie, stała pozycja
Interwał nadawania pozycji (position_broadcast_secs): 0 oznacza 15 minut i działa, gdy inteligentne nadawanie jest wyłączone. Smart Broadcast (domyślnie włączone) wysyła pozycję częściej w ruchu i rzadziej na postoju; wtedy interwał najlepiej zostawić domyślny. Minimalny dystans inteligentnego nadawania: 0 oznacza 100 m. Minimalny interwał: 0 to według opisu 30 s, ale tabela CLI podaje 15 minut, więc źródło jest niespójne i trzeba sprawdzić na swoim firmware. Pozycja stała: węzeł nadal próbuje GPS, ale zgłasza ostatnio zapisane współrzędne (z GPS węzła, telefonu lub ręcznie). Interwał aktualizacji GPS: 0 to 2 minuty, a bardzo duża wartość oznacza jedno ustalenie przy starcie.
Słowa: pozycja, smart broadcast, position_broadcast_secs, stała pozycja, fixed position, interwał GPS, 15 minut, 100 metrów, gps_update_interval
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10)

### Flagi pozycji: co dołączyć do wiadomości z pozycją
Position Flags to pole bitowe wybierające opcjonalne dane: ALTITUDE, ALTITUDE_MSL, GEOIDAL_SEPARATION, DOP, HVDOP, PDOP, SATINVIEW, SEQ_NO, TIMESTAMP, HEADING, SPEED; domyślnie UNSET. DOP domyślnie oznacza PDOP; DOP razem z HVDOP wysyła HDOP i VDOP osobno. Każda dodatkowa flaga powiększa pakiet, a w naszej sieci pakiety na lokalnym paśmie są kosztowne czasowo, więc dokładaj tylko to, co potrzebne (wniosek ogólny). CLI: --pos-fields.
Słowa: position flags, flagi pozycji, pos-fields, wysokość, HDOP, PDOP, prędkość, kierunek, satelity, rozmiar pakietu
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10; wniosek o koszcie pakietu: ogólny)

### Ustawienia ekranu: czas, karuzela, jednostki, OLED
Czas świecenia ekranu (screen_on_secs): 0 to 10 minut po naciśnięciu przycisku lub wiadomości. Automatyczna karuzela stron (auto_screen_carousel_secs): 0 wyłączona. Jednostki ekranu METRIC lub IMPERIAL dotyczą tylko ekranu urządzenia, a aplikacje używają ustawień lokalnych telefonu. Typ OLED: domyślnie autodetekcja, ręcznie SSD1306, SH1106 lub SH1107 (SH1107 zakłada panel 128×128). Odwrócenie ekranu o 180° dla montażu do góry nogami, pogrubienie nagłówka (przy trybach INVERTED i TWOCOLOR) oraz budzenie dotknięciem lub ruchem (akcelerometr lub przycisk pojemnościowy). Po każdym poleceniu CLI urządzenie się restartuje, więc łącz wiele ustawień w jedno polecenie z kilkoma --set.
Słowa: ekran, display, screen_on_secs, karuzela, OLED, SSD1306, SH1106, flip screen, wake on tap, jednostki metryczne
Źródło: https://meshtastic.org/docs/configuration/radio/display/ (sprawdzono 2026-10)

### Bluetooth: tryby parowania i domyślny PIN
Bluetooth jest domyślnie włączony. Tryby: RANDOM_PIN (losowy PIN w czasie pracy), FIXED_PIN (ustawiony przez Ciebie), NO_PIN (bez PIN). Domyślny tryb zależy od ekranu przy pierwszym uruchomieniu: z ekranem RANDOM_PIN, bez ekranu FIXED_PIN. Domyślny stały PIN to 123456, a dokumentacja ostrzega, że zostawienie go to poważne ryzyko, więc ustaw własny sześciocyfrowy. Jeśli zmienisz tryb przed odłączeniem ekranu, po jego usunięciu urządzenie zostaje przy RANDOM_PIN, dopóki go ręcznie nie zmienisz. Na ESP32 włączenie Wi-Fi wyłącza Bluetooth.
Słowa: Bluetooth, PIN, 123456, RANDOM_PIN, FIXED_PIN, NO_PIN, parowanie, bezpieczeństwo BLE, bluetooth.mode
Źródło: https://meshtastic.org/docs/configuration/radio/bluetooth/ (sprawdzono 2026-10)

### Sieć Wi-Fi i Ethernet w węźle: pola i uwagi
Wi-Fi i Ethernet są domyślnie wyłączone. SSID i hasło (PSK) rozróżniają wielkość liter (maks. 32 i 64 znaki). Tryb adresu: DHCP (domyślnie) lub STATIC z adresem, bramą, maską i DNS. Serwer NTP domyślnie meshtastic.pool.ntp.org. Można ustawić serwer rsyslog i flagi protokołu: NO_BROADCAST wyłącza rozgłaszanie sieciowe, UDP_BROADCAST włącza UDP w sieci lokalnej. Wi-Fi działa jako klient na ESP32; tryb punktu dostępu (SoftAP) nie jest obsługiwany. Wpisanie danych Wi-Fi przez niezaufaną sieć ujawnia ruch. Pierwszy restart po włączeniu Wi-Fi lub Ethernetu trwa o 20–30 s dłużej z powodu generowania kluczy SSL.
Słowa: Wi-Fi węzła, Ethernet, NTP, rsyslog, UDP_BROADCAST, DHCP, adres statyczny, SoftAP, klucze SSL, network.wifi_ssid
Źródło: https://meshtastic.org/docs/configuration/radio/network/ (sprawdzono 2026-10)
