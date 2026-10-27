# Moduły Meshtastic: szczegóły ustawień

### Store & Forward: jak odebrać wiadomości, które przegapiłem?
Serwer S&F przechowuje wiadomości tekstowe i na prośbę odsyła te z zadanego okna czasu. Od firmware 2.4 po połączeniu aplikacji z serwerem historia pobiera się sama. Serwer musi być urządzeniem ESP32 z PSRAM (np. T-Beam, T3S3) i działać cały czas. Ustawienia: włącz moduł i nadaj rolę ROUTER albo is_server = true (od 2.4). Domyślnie zwraca do 25 wiadomości z ostatnich 240 minut; liczba rekordów 0 oznacza ok. 2/3 PSRAM (ok. 11 000 wiadomości). Heartbeat co 15 minut ogłasza obecność serwera; można go wyłączyć, jeśli historię pobiera się tylko przez aplikację. W Androidzie wyślij do serwera wiadomość prywatną "SF". Uwaga: duże żądania na chwilę zapychają sieć, możliwe są duplikaty, a na domyślnym kanale publicznym pobieranie historii przez LoRa nie działa.
Słowa: Store and Forward, S&F, historia wiadomości, przegapione wiadomości, PSRAM, SF, serwer, heartbeat, 25 wiadomości, 240 minut, T-Beam
Źródło: https://meshtastic.org/docs/configuration/module/store-and-forward-module/ (sprawdzono 2026-10)

### Moduł szeregowy (Serial): tryby pracy
SIMPLE (domyślny) przesyła surowe dane przez kanał o nazwie "serial", który musi istnieć. PROTO to API protobuf. TEXTMSG wysyła tekst z portu na kanał domyślny, a odebrane wiadomości wypisuje jako "ShortName: tekst". NMEA co 2 s wysyła $GNGGA z własną pozycją i $GPWPL dla każdego węzła z pozycją. CALTOPO co 10 s wysyła punkty $GPWPL do CalTopo/SARTopo. WS85 czyta wiatr z Ecowitt WS85 (wymaga przeróbki czujnika) i wysyła telemetrię co 5 minut. VE_DIRECT odbiera dane z urządzeń Victron. MS_CONFIG obsługuje MeshSolar. LOG i LOGTEXT zapisują pakiety lub tylko teksty. Domyślna prędkość 38400 (do wyboru 110–921600), timeout w SIMPLE 0 = 250 ms.
Słowa: serial, moduł szeregowy, SIMPLE, PROTO, TEXTMSG, NMEA, CALTOPO, WS85, Victron, VE.Direct, MeshSolar, 38400, LOG
Źródło: https://meshtastic.org/docs/configuration/module/serial/ (sprawdzono 2026-10)

### Moduł szeregowy: podłączenie i ostrzeżenia
TX urządzenia łączy się z RX adaptera, a RX z TX, plus masa z masą. Używaj adaptera USB–UART z logiką 3,3 V (FT232, CP2102, CH340); 5 V może uszkodzić nRF52840, a Arduino Pro Mini w wersji 5 V uszkodzi T1000-E (użyj wersji 3,3 V/8 MHz). Zły GPIO może fizycznie uszkodzić sprzęt, więc najpierw sprawdź schemat. Przykładowe piny: T1000-E TX 16, RX 17 (38400 8N1, nie podłączaj pinu 3V3); RAK4631 na RAK19007 TXD1 16, RXD1 15 (GPS Mode na Not_Present); T-Beam v1.1 zalecane 14 (TX) i 13 (RX). Override Console Serial Port działa tylko w NMEA i CALTOPO. Na Linuksie "Permission denied" na porcie naprawisz, dodając użytkownika do grupy dialout i logując się ponownie.
Słowa: podłączenie UART, TX RX, 3,3 V, CH340, CP2102, FT232, T1000-E piny, RAK4631 piny, T-Beam piny, dialout, permission denied, Linux port szeregowy
Źródło: https://meshtastic.org/docs/configuration/module/serial/ (sprawdzono 2026-10)

### Range Test: jak sprawdzić zasięg krok po kroku
Włącz moduł na obu węzłach. Na stałym nadajniku ustaw interwał (sender) w sekundach, np. 30 dla Long Fast i 15 dla Medium i Short Fast; 0 wyłącza wysyłanie. Z odbiornikiem (najlepiej z GPS) idź lub jedź; dopóki pakiety dochodzą, jesteś w zasięgu. Ustaw wysoką precyzję pozycji na kanale domyślnym, inaczej zapisana pozycja będzie niedokładna. Zapis do pliku CSV działa tylko na ESP32 (rangetest.csv, pobierasz przez Wi-Fi z meshtastic.local/rangetest.csv); w Androidzie eksportujesz go z logu debugowania. Plik zaimportujesz do Google Earth, My Maps lub uMap. Ważne: po teście wyłącz moduł, bo zajmuje dużo eteru i zaśmieca kanał; sam wyłącza się po 8 godzinach.
Słowa: range test, test zasięgu, sender, rangetest.csv, Google Earth, uMap, mapa zasięgu, wyłącz po teście, 8 godzin, precyzja pozycji
Źródło: https://meshtastic.org/docs/configuration/module/range-test/ (sprawdzono 2026-10)

### Powiadomienia zewnętrzne: buzzer, dioda, wibracja
Moduł obsługuje do 3 pinów: ogólny/LED (output), wibracja (output_vibra) i buzzer (output_buzzer). active ustawia polaryzację (true = stan wysoki aktywny). output_ms 0 oznacza 1000 ms. use_pwm włącza buzzer PWM z melodiami RTTTL (wtedy używany jest device.buzzer_gpio). Alerty: na wiadomość (alert_message) i na znak dzwonka ASCII 0x07 (alert_bell), każdy w wariancie LED, wibracja i buzzer. nag_timeout większy od zera powtarza alarm do naciśnięcia przycisku lub upływu sekund. Na ESP32 piny 34–39 są tylko wejściami. Moduł reaguje tylko na wiadomości tekstowe; zły GPIO może uszkodzić sprzęt.
Słowa: external notification, powiadomienie zewnętrzne, buzzer, PWM, RTTTL, dioda, wibracja, alert bell, nag timeout, GPIO 34, dzwonek
Źródło: https://meshtastic.org/docs/configuration/module/external-notification/ (sprawdzono 2026-10)

### Czujnik wykrywania (Detection Sensor): alarm z kontaktronu lub PIR
Moduł obserwuje wybrany pin i wysyła wiadomość przy zmianie stanu. Ustawienia: minimalny odstęp między wiadomościami (przykład 90 s), interwał stanu wysyłanego niezależnie od zmian (przykład 300 s; 0 = tylko przy zmianie), dzwonek ASCII (może wywołać powiadomienie zewnętrzne), nazwa do 20 znaków (np. "Motion" daje "Motion detected"), pin (przykład GPIO 7), wyzwalanie stanem wysokim lub niskim oraz wewnętrzny pull-up (jeśli pin go ma). Wymaga firmware i aplikacji 2.2.2 lub nowszych.
Słowa: detection sensor, czujnik ruchu, PIR, kontaktron, alarm, pin, pull-up, friendly name, motion detected, powiadomienie o ruchu
Źródło: https://meshtastic.org/docs/configuration/module/detection-sensor/ (sprawdzono 2026-10)
