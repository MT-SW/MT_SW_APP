# Algorytm sieci, aplikacja Android w użyciu, Heltec V3 i V4, integracje

### Zarządzane zalewanie (managed flooding): jak węzły decydują o retransmisji
Węzły retransmitują odebrany pakiet, dopóki nie skończy się limit przeskoków, ale najpierw chwilę nasłuchują: jeśli inny węzeł już go przekazał, milczą. Okno rywalizacji zależy od SNR odebranego pakietu: słaby sygnał (dalekie węzły) dostaje małe okno, więc to dalsze węzły zwykle retransmitują pierwsze, a bliższe się powstrzymują. ROUTER i ROUTER_LATE retransmitują nawet wtedy, gdy usłyszą cudzą retransmisję; CLIENT_BASE tak samo, ale tylko dla pakietów od i do ulubionych węzłów. Role routerowe mają domyślnie interwały telemetrii i pozycji co 12 godzin zamiast co godzinę. Powyżej 40 węzłów online firmware wydłuża interwały: Interwał × (1 + (liczba_online − 40) × 2^SF / (BW_kHz × 100)); wyjątki: ROUTER, ROUTER_LATE, SENSOR, TRACKER, TAK_TRACKER. Przykład z tego wzoru: 60 węzłów, SF11 i 250 kHz daje mnożnik około 2,6.
Słowa: managed flooding, rebroadcast, SNR, okno rywalizacji, ROUTER, interwał skalowany, 40 węzłów, algorytm mesh
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Hop limit, potwierdzenia i ponowienia
HopLimit to 3-bitowe pole zmniejszane przy każdej retransmisji; pakiet z zerem nie jest przekazywany dalej. Osobne 3-bitowe pole HopStart pamięta oryginalny limit. Zwykłe ACK dla rozgłoszeń jest wyłączone, żeby nie zalewać kanału; nadawca uznaje za niejawne potwierdzenie usłyszenie, jak jakiś węzeł Meshtastic przekazuje jego pakiet. Pewne rozgłoszenia wysyła się najwyżej 3 razy łącznie, pewne wiadomości bezpośrednie najwyżej 5 razy. Bez ACK lub NAK węzeł lokalny sam generuje wewnętrzny NAK; limit czasu uwzględnia czas antenowy, zajętość kanału i opóźnienie obliczeń. Przy wiadomościach bezpośrednich odbiorca odsyła prawdziwe ACK, a nadawca ponawia tylko, gdy nie dostanie żadnego.
Słowa: hop limit, HopStart, ACK, NAK, retransmisja, implicit ACK, wiadomość bezpośrednia, ponowienia, 3 razy, 5 razy
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Wiadomości bezpośrednie: routing następnego skoku (od v2.6)
Wiadomość bezpośrednia zaczyna od zarządzanego zalewania. Jeśli odpowiedź wraca przez węzeł, który też przekazał pierwotny pakiet, ten węzeł zostaje wyznaczonym następnym skokiem dla tego odcinka. Potem kolejne pakiety przekazuje tylko pasujący następny skok, co ogranicza ruch w eterze. Przy ostatniej próbie retransmisji węzeł wraca do zalewania, jeśli nie usłyszy przekazania przez swój następny skok. Dzięki temu DM obciąża sieć mniej niż zwykły rozgłos.
Słowa: next-hop, routing DM, wiadomość prywatna, v2.6, zalewanie, ruch w sieci, następny skok
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Aplikacja Android: łączenie, kanały, ikony wiadomości
Przycisk „+” (prawy dół) szuka urządzeń Bluetooth; Wi-Fi i Ethernet w tej samej sieci zwykle wykrywają się same albo wpisujesz adres IP; USB OTG wykrywa się automatycznie. Pierwsze połączenie wymaga parowania (część urządzeń bez PIN, inne pokazują PIN na ekranie). Ikona chmury: przekreślona to brak urządzenia, z ptaszkiem połączono, ze strzałką w górę urządzenie śpi lub poza zasięgiem. Domyślny kanał #LongFast-I widzą wszyscy użytkownicy w zasięgu. Zmiana kanału zrywa kontakt z tymi, którzy nie dostali nowych danych; udostępniasz kodem QR lub linkiem. Ikony wysłanej wiadomości: chmura ze strzałką (w kolejce w aplikacji), chmura (wysłano przez LoRa), chmura z ptaszkiem (potwierdził co najmniej jeden węzeł), osoba z ptaszkiem (potwierdził odbiorca DM), przekreślona chmura (brak potwierdzenia w czasie). Wiadomości domyślnie nie są magazynowane, więc niedoręczone przepadają.
Słowa: aplikacja Android, parowanie, ikona chmury, status wiadomości, kod QR kanału, LongFast, quick chat, połączenie
Źródło: https://meshtastic.org/docs/software/android/usage/ (sprawdzono 2026-10)

### Aplikacja Android: węzły, mapa, ustawienia, rozwiązywanie problemów
Lista węzłów pokazuje ostatnią aktywność, pozycję i odległość oraz stan zasilania; kolorowy chip przy węźle pozwala napisać wiadomość, poprosić o pozycję, zrobić traceroute albo dodać do listy ignorowanych (i, jeśli włączony kanał admin, zdalnie konfigurować). Mapa pokazuje węzły ze znaną pozycją; część map działa offline (pobierz region). Region ustaw na liście „Region”, bo bez tego nie ma komunikacji. Menu trzech kropek: panel debugowania, konfiguracja radia (restart, wyłączenie, reset do fabrycznych i reset NodeDB), eksport rangetest.csv, motyw, język. Gdy PIN nie działa: urządzenie wgrane bez ekranu ma domyślnie 123456, a uruchomione z ekranem używa losowego; ustaw PIN ręcznie lub zrób reset fabryczny. Gdy kod QR otwiera stronę zamiast aplikacji: Ustawienia Androida > Aplikacje > Domyślne > Meshtastic > Otwieranie linków i włącz obsługiwane linki.
Słowa: lista węzłów, traceroute, mapa offline, region, PIN 123456, panel debugowania, reset NodeDB, link nie otwiera aplikacji
Źródło: https://meshtastic.org/docs/software/android/usage/ (sprawdzono 2026-10)

### Heltec WiFi LoRa 32 V3, V3.1 i V4: różnice i znane problemy
V3/V3.1: ESP32-S3FN8, radio SX1262 (warianty 433 do 510, 470 do 510, 863 do 870, 902 do 928 MHz), OLED 0,96 cala, przyciski użytkownika i reset, bez GPS, złącze U.FL dla LoRa i osobna antena 2,4 GHz dla Wi-Fi/BT; w Meshtastic I²C to SDA GPIO41 i SCL GPIO42. V3.1 usunięto tranzystor FDG6322C z zasilania i zmieniono wartości filtra antenowego (firmware jak V3). V4: ESP32-S3R2, SX1262 863 do 870 i 902 do 928 MHz, opcja dużej mocy 28±1 dBm, złącza baterii, solara i GNSS, ten sam układ pinów co V3. Pliki firmware: firmware-heltec-v3-… i firmware-heltec-v4-…. Znane kwestie: V3 może mieć kłopot z ładowaniem akumulatora kablem USB-C do USB-C (użyj USB-A do USB-C); V4-R8 to inna płytka (ESP32-S3R8, inny pinout), wymaga własnego firmware i nie wgrywaj na nią firmware V4; port USB-C V4-R8 (CP2102) nie ma ochrony ESD.
Słowa: Heltec V3, Heltec V4, V3.1, SX1262, ESP32-S3, V4-R8, GPIO41, GPIO42, ładowanie USB-C, firmware heltec
Źródło: https://meshtastic.org/docs/hardware/devices/heltec-automation/lora32/ (sprawdzono 2026-10)

### Integracje Meshtastic: MQTT, Home Assistant, Node-RED, ATAK, CalTopo
Według strony integracji: MQTT łączy sieci mesh przez internet i z innymi platformami (Home Assistant, Node-RED, Adafruit IO). Wtyczka ATAK to oficjalny dodatek przesyłający ruch Cursor on Target przez sieć do klienta TAK na Androidzie. CalTopo i SARTopo śledzą węzły Meshtastic na mapach. Wsparcie dla integracji powinno pochodzić od ich autorów lub dostawców. Discorda strona nie opisuje. Wpisy o MQTT i Home Assistant w tej bazie opisują naszą własną konfigurację szczegółowo.
Słowa: integracje, ATAK, CalTopo, SARTopo, Node-RED, Adafruit IO, MQTT, Home Assistant
Źródło: https://meshtastic.org/docs/software/integrations/ (sprawdzono 2026-10)

### FAQ Meshtastic: o co najczęściej pytają
Oficjalne FAQ wymienia pytania: czy można aktualizować firmware przez sieć mesh, którą wersję firmware wybrać i jak często aktualizować, jakie Androidy są wspierane, co znaczy ikona przy wiadomości, jak wyczyścić historię, co robić gdy po świeżym flashowaniu węzeł nie łączy się przez Bluetooth, jak wyłączyć i włączyć T-Beam, co się dzieje z wiadomościami, gdy urządzenie zaśnie, co zrobić gdy w Europie urządzenie po pewnym czasie przestaje nadawać (limit czasu nadawania), po co licencja krótkofalarska i jak ustawić znak, dlaczego tryby licencjonowany i nielicencjonowany nie są kompatybilne, czy Meshtastic używa LoRaWAN i czy da się zlokalizować urządzenie triangulacją. Pobrana strona zawierała same pytania, bez odpowiedzi, poza uwagą o kanałach: kod QR i URL kanału niosą ustawienia kanału i LoRa, a skanowanie stosuje wszystko, więc sprawdź je i skanuj tylko zaufane kody.
Słowa: FAQ, najczęstsze pytania, sen urządzenia, Europa przestaje nadawać, LoRaWAN, triangulacja, kod QR kanału, aktualizacja przez mesh
Źródło: https://meshtastic.org/docs/faq/ (sprawdzono 2026-10)
