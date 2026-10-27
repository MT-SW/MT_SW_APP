# Home Assistant i Meshtastic przez MQTT

### Jak połączyć Meshtastic z Home Assistant (oficjalna metoda MQTT)?
Węzeł musi publikować do brokera MQTT z włączonym wyjściem JSON. Integracja nie działa z urządzeniami nRF52 (znany problem). W configuration.yaml dodaj include pliku MQTT i utwórz go w tym samym katalogu; w nim definiujesz encje sensorów z state_topic i value_template filtrującym numer węzła. Numery węzłów znajdziesz w MQTT Explorer lub przez meshtastic --info. Przed restartem zawsze uruchom sprawdzenie konfiguracji w Narzędziach deweloperskich.
Słowa: Home Assistant, HA, MQTT, configuration.yaml, sensor, state_topic, value_template, nRF52, integracja
Źródło: https://meshtastic.org/docs/software/integrations/mqtt/home-assistant/ (sprawdzono 2026-10)

### Jakie encje można zrobić w Home Assistant z węzła?
Domyślna telemetria: napięcie baterii, procent baterii, wykorzystanie kanału, wykorzystanie czasu antenowego TX. Środowisko (z czujnikami): temperatura, wilgotność, ciśnienie, rezystancja gazu; szablony mogą przeliczać jednostki. Wiadomości: encja tekstowa z ostatnią wiadomością. Osobne encje: wysokość, szerokość, długość, czas, prąd, sąsiedzi. Lokalizacja: automatyzacja wywołująca device_tracker.see po pakiecie pozycji; współrzędne są całkowite, pomnóż je przez 1e-7; użyj unikalnego dev_id na węzeł (device_tracker.see jest przestarzałe i zniknie w Home Assistant Core 2027.5).
Słowa: encje, telemetria, device_tracker, pozycja, 1e-7, bateria, temperatura, ostatnia wiadomość, dev_id
Źródło: https://meshtastic.org/docs/software/integrations/mqtt/home-assistant/ (sprawdzono 2026-10)

### Jak wysłać wiadomość z Home Assistant do sieci mesh?
Utwórz pomocnika input text o maksymalnej długości ok. 190 znaków. Węzeł musi mieć kanał o nazwie "mqtt" z włączonym downlinkiem (klucz szyfrowania nie ma znaczenia). Automatyzacja publikuje JSON na temat wysyłania po zmianie pola i czyści pole. Pole channel wybiera kanał inny niż główny, pole to z numerem węzła wysyła wiadomość bezpośrednią. Można też robić powiadomienia na telefon po wiadomości z @wzmianką oraz sterować urządzeniami po zdarzeniach z sieci (np. wentylator po progu temperatury).
Słowa: wysyłanie wiadomości, input_text, automatyzacja, powiadomienie, kanał mqtt, downlink, 190 znaków, wiadomość z HA
Źródło: https://meshtastic.org/docs/software/integrations/mqtt/home-assistant/ (sprawdzono 2026-10)
