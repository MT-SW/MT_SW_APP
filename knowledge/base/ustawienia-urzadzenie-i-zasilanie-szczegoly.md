# Ustawienia urządzenia, pozycji, telemetrii i zasilania: szczegóły

### Tryby retransmisji (rebroadcast mode): co znaczą?
ALL (domyślny) powtarza wszystko z własnej i obcych sieci o tych samych ustawieniach modemu. ALL_SKIP_DECODING robi to bez dekodowania, tylko dla roli Repeater. LOCAL_ONLY ignoruje obce, otwarte lub nierozszyfrowalne wiadomości i powtarza tylko na własnym kanale głównym i dodatkowych. KNOWN_ONLY dodatkowo ignoruje nadawców, których nie ma w bazie węzłów. NONE blokuje powtarzanie (tylko dla SENSOR, TRACKER, TAK_TRACKER). CORE_PORTNUMS_ONLY odrzuca nietypowe typy (TAK, RangeTest, PaxCounter) i powtarza tylko NodeInfo, tekst, pozycję, telemetrię i routing.
Słowa: rebroadcast_mode, ALL, LOCAL_ONLY, KNOWN_ONLY, NONE, CORE_PORTNUMS_ONLY, ALL_SKIP_DECODING, powtarzanie, obce sieci
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Jak często węzeł ogłasza swoje imię (NodeInfo) i co z przyciskiem?
node_info_broadcast_secs ma domyślnie 10800 s (3 godziny); wiersz poleceń przyjmuje 3600 i więcej. Węzeł i tak odpowiada na prośby o NodeInfo oraz sam wysyła swoje, gdy usłyszy nieznany węzeł. button_gpio i buzzer_gpio (domyślnie 0, zakres 0–34) wskazują piny przycisku i buzzera tylko dla płytek bez wbudowanych. double_tap_as_button_press traktuje podwójne stuknięcie jak naciśnięcie, o ile jest akcelerometr. tzdef ustawia strefę czasową zapisem POSIX (np. dla Polski CET-1CEST,M3.5.0,M10.5.0/3) i wpływa na czasy na ekranie i w logach. Wyłączenie LED heartbeat gasi domyślne miganie diody.
Słowa: node_info_broadcast_secs, 10800, NodeInfo, button_gpio, buzzer_gpio, double tap, tzdef, strefa czasowa, POSIX TZ, dioda, heartbeat
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10; przykład tzdef dla Polski to zapis ogólny POSIX, nie ze źródła)

### Uwagi o rolach z dokumentacji: ROUTER, REPEATER, TRACKER, SENSOR
Na ESP32 oszczędzanie energii w roli ROUTER włącza się samo i nie da się go wyłączyć. Aby zaktualizować ROUTER przez Bluetooth, trzeba chwilowo zmienić rolę kanałem administracyjnym, a potem ją przywrócić. TRACKER i SENSOR nie trzymają radia w czuwaniu, więc przychodzące pakiety ich nie budzą; ich sen zależy od interwałów nadawania. Rola REPEATER jest oznaczona jako przestarzała od firmware 2.7.11, a ROUTER_CLIENT od 2.3.15. CLIENT_HIDDEN nie pojawia się na liście węzłów, a CLIENT_BASE zawsze powtarza pakiety od i do ulubionych węzłów.
Słowa: ROUTER, REPEATER, deprecated, przestarzała, TRACKER, SENSOR, CLIENT_BASE, CLIENT_HIDDEN, ROUTER_CLIENT, aktualizacja routera, ulubione
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Domyślne interwały pozycji GPS i inteligentne rozgłaszanie
GPS Update Interval 0 oznacza co 2 minuty (bardzo duża wartość to jednorazowa aktualizacja przy starcie). Smart Broadcast (domyślnie włączony) wysyła pozycję częściej w ruchu i rzadziej w bezruchu; minimalny dystans 0 oznacza 100 m, minimalny odstęp 0 oznacza 30 s. Przy wyłączonym smart broadcast używany jest zwykły interwał, 0 oznacza 15 minut. Dokumentacja radzi zostawić interwał domyślny przy włączonym smart. Stała pozycja (fixed_position) używa ostatnio zapisanych współrzędnych; GPS nadal działa wg interwału. Do obliczeń czasu potrzebny jest co najmniej jeden węzeł z GPS, RTC lub internetem (NTP). Źródło ma niespójność: minimalny odstęp smart jest raz 30 s, raz 15 minut.
Słowa: gps_update_interval, smart broadcast, 100 m, 30 s, 15 minut, fixed_position, stała pozycja, NTP, RTC, flagi pozycji
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10; źródło niespójne co do minimalnego odstępu)

### Jakie flagi pozycji można dodać do wiadomości?
Pole flags to maska bitowa dodatkowych danych: wysokość (ALTITUDE, ALTITUDE_MSL, GEOIDAL_SEPARATION), dokładność (DOP lub HVDOP rozdzielone na poziome i pionowe), a także SATINVIEW (liczba widocznych satelitów), SEQ_NO, TIMESTAMP, HEADING (kierunek) i SPEED (prędkość). Im więcej pól, tym dłuższy pakiet i więcej zajętego eteru.
Słowa: flagi pozycji, position flags, wysokość, DOP, HDOP, prędkość, kierunek, satelity, długość pakietu
Źródło: https://meshtastic.org/docs/configuration/radio/position/ (sprawdzono 2026-10; uwaga o długości pakietu to wniosek ogólny)

### Ustawienia oszczędzania energii: co znaczą wartości 0?
Power Saving (domyślnie wyłączone) wyłącza Bluetooth, port szeregowy, Wi-Fi i ekran; niektóre role włączają je same. Wait Bluetooth: 0 oznacza 1 minutę. Light Sleep (ls_secs): 0 oznacza 5 minut, tylko ESP32. Min Wake: 0 oznacza 10 s. Shutdown after losing power: 0 to wyłączone; przed włączeniem sprawdź w aplikacji, czy płytka widzi zasilanie zewnętrzne (ikona wtyczki), bo inaczej urządzenie może się wyłączyć mimo zasilania. ADC Multiplier Override 0 używa wartości z firmware (źródło podaje zakres 2–4 lub 2–6). Kalibracja baterii mapuje 4,2 V na 100 % i nie uwzględnia nieliniowości przetwornika. Adres INA2xx podaje się dziesiętnie (0x40 to 64) i jest ignorowany tam, gdzie jest PMU, np. w T-Beam. Większość użytkowników powinna zamiast tego ustawić rolę.
Słowa: power saving, ls_secs, min_wake_secs, wait_bluetooth_secs, shutdown po utracie zasilania, ADC multiplier, INA219 adres, kalibracja baterii, light sleep
Źródło: https://meshtastic.org/docs/configuration/radio/power/ (sprawdzono 2026-10)

### Ustawienia telemetrii: domyślne interwały i na co uważać
Interwały metryk urządzenia, środowiska, jakości powietrza i zdrowia domyślnie wynoszą 1800 s (30 minut), a wartość 0 oznacza ten domyślny interwał. Podłączona aplikacja dostaje metryki urządzenia co minutę niezależnie od ustawienia. Metryki zasilania (napięcie i prąd z czujnika) są domyślnie wyłączone i nie są potrzebne do samego podglądu napięcia baterii. Temperatura na wyświetlaczu może być w Fahrenheitach, ale czujnik zawsze mierzy w Celsjuszach. Moduły środowiska, jakości powietrza i zdrowia muszą być włączone, by dane szły do sieci; wsparcie czujników zdrowia wymaga kompilacji firmware z odpowiednimi makrami. Dla czujnika AS3935 (wyładowania) podłącz pin przerwania i zdefiniuj AS3935_IRQ w wariancie, żeby uniknąć odczytu I²C co sekundę. Dokumentacja nie ostrzega o zajętości eteru, ale częste interwały zwiększają ruch.
Słowa: telemetria, interwał 1800, device metrics, environment, air quality, power metrics, health telemetry, Fahrenheit, AS3935, makra, czujniki
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10; uwaga o eterze to wniosek ogólny)
