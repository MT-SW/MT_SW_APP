# Moduły: Range Test, Neighbor Info i Detection Sensor (szczegóły)

### Range Test: jak zmierzyć zasięg i czego uważać?
Włącz moduł na nadajniku i odbiorniku (aplikacje pokazują wiadomości testowe tylko przy włączonym module). Nadajnik jest zwykle stały i wysyła kolejne pakiety co 30–60 s (zalecane 30 dla Long Fast, 15 dla Medium i Short Fast); odbiornik jest zwykle ruchomy, najlepiej z GPS. W CLI: meshtastic --set range_test.enabled true i --set range_test.sender 60 (łącz ustawienia w jedno polecenie, bo każde polecenie restartuje węzeł). Opcja save zapisuje rangetest.csv w pamięci flash (tylko ESP32; przy aplikacjach zostaw wyłączoną); plik pobierzesz przez Wi-Fi z meshtastic.local/rangetest.csv, a zapis zatrzymuje się poniżej 50 KB wolnego miejsca. Do mapowania ustaw wysoką precyzję pozycji. Po teście wyłącz moduł: zużywa dużo czasu antenowego i zapycha kanał, a sam wyłącza się po 8 godzinach.
Słowa: Range Test, test zasięgu, range_test.sender, rangetest.csv, pomiar zasięgu, czas antenowy, nadajnik, odbiornik, mapa zasięgu
Źródło: https://meshtastic.org/docs/configuration/module/range-test/ (sprawdzono 2026-10)

### Neighbor Info: do czego służy i jakie ma ograniczenia?
Moduł rozsyła informacje o sąsiadach węzła, ale jest tylko informacyjny i nie wpływa na routing. Domyślnie jest wyłączony, interwał to 6 godzin (21600 s), a minimum 4 godziny (14400 s). Wysyłanie przez LoRa na kanale głównym (Transmit Over LoRa) jest domyślnie wyłączone, wymaga firmware 2.5.13 lub nowszego i działa tylko, gdy kanał główny nie jest publiczny z domyślnym kluczem i nazwą; inaczej dane idą tylko do MQTT i aplikacji. Od wersji 2.3.2 moduł wykrywa sąsiadów, nawet jeśli oni go nie mają. Włączenie na wszystkich węzłach pozwala zbudować graf sieci, choć strona zaznacza, że żaden klient go jeszcze nie rysuje; dane zbiera MQTT.
Słowa: Neighbor Info, sąsiedzi, graf sieci, 21600, kanał główny, Transmit Over LoRa, topologia, moduł
Źródło: https://meshtastic.org/docs/configuration/module/neighbor-info/ (sprawdzono 2026-10)

### Detection Sensor: czujnik na pinie GPIO
Wymaga firmware 2.2.2 lub nowszego. Ustawienia: Monitor Pin (pin GPIO do obserwacji), Minimum Broadcast Interval (minimalna liczba sekund między wiadomościami po wykryciu zmiany, przykład 90), State Broadcast Interval (jak często wysyłać stan niezależnie od zmian, 0 tylko po zmianie, przykład 300), Detection Triggered High (czy wyzwala stan wysoki, czy niski), Use Pull-up (INPUT_PULLUP, tylko gdy płytka ma rezystory podciągające na tym pinie), Send Bell (dodaje znak dzwonka ASCII, co może uruchomić zewnętrzne powiadomienia) oraz Friendly Name (do 20 znaków, "Motion" daje komunikat "Motion detected"). Ustawienia w aplikacjach: Android Settings > Detection Sensor, Apple Settings > Module Configuration > Detection Sensor. CLI: meshtastic --get detection_sensor.
Słowa: Detection Sensor, czujnik, GPIO, monitor_pin, pull-up, czujnik ruchu, kontaktron, send_bell, alarm, detection_triggered_high
Źródło: https://meshtastic.org/docs/configuration/module/detection-sensor/ (sprawdzono 2026-10)
