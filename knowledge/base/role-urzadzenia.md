# Role urządzenia i retransmisja

### Jakie są role urządzenia (CLIENT, ROUTER, REPEATER...) i czym się różnią?
CLIENT: urządzenie z aplikacją lub samodzielne, retransmituje pakiet, gdy nikt inny tego nie zrobił. CLIENT_MUTE: nie przekazuje cudzych pakietów. CLIENT_HIDDEN: nadaje tylko w razie potrzeby (dyskrecja lub oszczędzanie energii). CLIENT_BASE: osobista stacja bazowa, zawsze retransmituje pakiety od i do swoich ulubionych węzłów. TRACKER: priorytetowo wysyła pozycję GPS. SENSOR: priorytetowo wysyła telemetrię. LOST_AND_FOUND: regularnie wysyła pozycję na kanał domyślny, by pomóc znaleźć urządzenie. TAK i TAK_TRACKER: do systemu ATAK, ograniczają rutynowe rozgłoszenia.
Słowa: rola, role, client, client_mute, client_base, tracker, sensor, tak, lost and found, hidden
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Czym jest ROUTER i REPEATER, kiedy ich używać?
ROUTER: węzeł infrastruktury do zwiększania zasięgu sieci, zawsze retransmituje pakiety raz; jest widoczny na liście węzłów. REPEATER: to samo, z minimalnym narzutem, ale nie jest widoczny na liście węzłów (rola oznaczona jako przestarzała od firmware 2.7.11). ROUTER_LATE: retransmituje raz, ale dopiero po wszystkich innych. ROUTER_CLIENT jest przestarzały od 2.3.15. Rola ROUTER nie ma ekranu, nie jest dostępna po BLE, ma wysokie zużycie energii, a oszczędzanie energii włącza się automatycznie (ESP32) i nie da się go wyłączyć.
Słowa: router, repeater, przekaźnik, retransmisja, infrastruktura, router_late, router_client, ESP32, zasięg
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Jak zmienić rolę węzła typu ROUTER, gdy łączę się po Bluetooth?
Dokumentacja zaleca użyć kanału administracyjnego, by tymczasowo zmienić rolę, a potem ją przywrócić, bo ROUTER nie jest dostępny po BLE.
Słowa: admin, kanał administracyjny, zdalna zmiana roli, bluetooth, router
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Jakie są tryby retransmisji (rebroadcast mode)?
ALL (domyślny): retransmituje wszystko z własnej sieci i z innych sieci o tych samych ustawieniach modemu, także gdy szyfrowanie jest inne. ALL_SKIP_DECODING: jak ALL, ale bez dekodowania (tylko rola Repeater). LOCAL_ONLY: ignoruje obce sieci, retransmituje tylko na swoich kanałach głównym i dodatkowych. KNOWN_ONLY: jak LOCAL_ONLY i ignoruje węzły spoza własnej bazy NodeDB. NONE: bez retransmisji (tylko dla SENSOR, TRACKER, TAK_TRACKER). CORE_PORTNUMS_ONLY: tylko standardowe pakiety (NodeInfo, tekst, pozycja, telemetria, routing), pomija np. RangeTest i PaxCounter.
Słowa: rebroadcast, retransmisja, tryb, ALL, LOCAL_ONLY, KNOWN_ONLY, NONE, CORE_PORTNUMS_ONLY, obce sieci
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Jak często węzeł rozgłasza NodeInfo?
Parametr Node Info Broadcast Seconds: domyślnie 10800 s (3 godziny), minimum 3600 s. Węzeł wysyła NodeInfo też od razu, gdy usłyszy pakiet od nieznanego węzła, i odpowiada doraźnie na zapytania o NodeInfo.
Słowa: nodeinfo, nazwa węzła, rozgłaszanie, interwał, 10800, 3600
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)

### Inne ustawienia urządzenia: przycisk, buzzer, strefa czasowa, dioda
GPIO przycisku użytkownika i buzzera PWM: numery pinów dla płytek bez predefiniowanych. Double Tap as Button Press: podwójne stuknięcie (akcelerometr) jak przycisk, domyślnie wyłączone. TZDEF: czas lokalny jako ciąg POSIX TZ, np. PST8PDT,M3.2.0,M11.1.0. LED Heartbeat Disabled: wyłącza domyślne mruganie diody.
Słowa: GPIO, przycisk, buzzer, strefa czasowa, TZDEF, dioda, LED, heartbeat, double tap
Źródło: https://meshtastic.org/docs/configuration/radio/device/ (sprawdzono 2026-10)
