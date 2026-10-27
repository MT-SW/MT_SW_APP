# Kanały, bezpieczeństwo, administracja zdalna i CLI: szczegóły (dokumentacja 2.8)

### Ile jest kanałów i jak się nazywają ich role?
Jest 8 kanałów (indeksy 0–7), indeksów nie można zmienić. Kanał 0 to PRIMARY: jedyny główny, nie da się go wyłączyć, na nim domyślnie idą okresowe pozycje i telemetria. Kanały 1–7 to SECONDARY (własne klucze). DISABLED to kanał wyłączony, ustawienia wracają do domyślnych; aktywne kanały muszą iść po kolei, bez dziur. Nazwa jest krótsza niż 12 bajtów; pusta nazwa kanału głównego oznacza kanał "default". Węzły muszą mieć tę samą nazwę i ten sam klucz, żeby się porozumieć. Zmiana nazwy kanału głównego może przenieść węzeł na inny slot częstotliwości, jeśli slot nie jest ustawiony jawnie; dlatego prywatne grupy lepiej trzymać na kanałach dodatkowych.
Słowa: kanały, 8 kanałów, primary, secondary, disabled, nazwa kanału, 12 bajtów, slot częstotliwości, indeks kanału, default
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Klucz kanału (PSK): jakie wartości i które są publiczne?
Klucz ma 0 bajtów (brak szyfrowania), 16 (AES-128) lub 32 bajty (AES-256). Domyślny klucz kanału głównego to 0x01, czyli AQ==. W wierszu poleceń: none, default (publicznie znany), random (bezpieczny 256-bitowy, do użytku prywatnego), simple0–simple254 (jednobajtowe, też publiczne), klucz szesnastkowy lub base64:. Klucze default i simple są publicznie znane, więc nadają się tylko do testów i kanałów publicznych; do prywatnych użyj random. Opcja AEAD (od 2.8.1) przełącza na uwierzytelniany AES-CCM z 12-bajtowym znacznikiem; jest eksperymentalna, domyślnie wyłączona i wymaga klucza, a wszystkie węzły kanału muszą mieć tak samo.
Słowa: PSK, klucz kanału, AQ==, random, simple, AES128, AES256, klucz publiczny, prywatny kanał, AEAD, base64
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Precyzja pozycji na kanale: ile to metrów?
Wartość od 0 do 32 bitów: 0 oznacza, że pozycja nigdy nie jest wysyłana, 32 pełną dokładność. Kanały z kluczem publicznym (brak klucza, domyślny lub jednobajtowy) są ograniczone do 15 bitów. Przybliżone odległości: 10 bitów to 23,3 km, 12 bitów 5,8 km, 13 bitów 2,9 km, 14 bitów 1,5 km, 15 bitów 729 m, 16 bitów 364 m, 17 bitów 182 m, 18 bitów 91 m, 19 bitów 45 m. Publiczne serwery MQTT odrzucają dokładne pozycje. Uplink i downlink (domyślnie wyłączone) przekazują wiadomości do i z internetu przez bramkę MQTT; kanał można też wyciszyć (muted), by nie dzwonił.
Słowa: precyzja pozycji, position precision, 15 bitów, 729 m, 364 m, prywatność lokalizacji, wyciszenie kanału, uplink, downlink
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Ustawienia bezpieczeństwa: klucze, tryb zarządzany, podpisy
Klucz publiczny jest udostępniany innym węzłom, a numer węzła z niego pochodzi; para kluczy powstaje po ustawieniu regionu. Klucz prywatny musi pozostać tajny. Admin Key to do trzech kluczy publicznych uprawnionych do poleceń administracyjnych. Managed Mode (is_managed) uniemożliwia aplikacjom zapis konfiguracji (odczyt zostaje); wymaga co najmniej jednego klucza admina i bez działającej zdalnej administracji grozi zablokowaniem się. Konsolę szeregową i log debugowania można wyłączyć. Polityka podpisów: COMPATIBLE (domyślna, przyjmuje niepodpisane), BALANCED, STRICT (tylko zweryfikowane; może sprawiać, że DM wyglądają na niedostarczone i odrzuca starsze węzły oraz STM32WL).
Słowa: klucz prywatny, klucz publiczny, admin key, managed mode, zablokowanie, serial console, debug log, packet signature policy, COMPATIBLE, BALANCED, STRICT
Źródło: https://meshtastic.org/docs/configuration/radio/security/ (sprawdzono 2026-10)

### Jak zrobić kopię kluczy i konfiguracji węzła?
Wymazanie firmware i ponowna instalacja tworzą nowe klucze i nowy numer węzła, więc inni widzą go jako inny węzeł i tracisz szyfrowane wiadomości prywatne z dotychczasowymi kontaktami. Kopię zrobisz poleceniem meshtastic --export-config > config_backup.yaml, zapisując plik w bezpiecznym miejscu; można też zrobić zrzut ekranu kluczy (oba w całości). Odtworzenie: meshtastic --configure config_backup.yaml lub ręczne wpisanie kluczy w aplikacji.
Słowa: kopia zapasowa, backup konfiguracji, export-config, configure, yaml, nowy numer węzła, utrata kluczy, przywracanie
Źródło: https://meshtastic.org/docs/configuration/radio/security/ i https://meshtastic.org/docs/software/python/cli/ (sprawdzono 2026-10)

### Zdalna administracja: jak skonfigurować węzeł przez sieć?
Domyślnie węzeł słucha poleceń administracyjnych tylko przez USB, Bluetooth i TCP. Od firmware 2.5: wpisz klucz publiczny węzła sterującego w polu Admin Key w konfiguracji bezpieczeństwa węzła zdalnego (do trzech kluczy). W CLI: meshtastic --set security.admin_key "base64:KLUCZ". Polecenie dopisuje klucze; wartość 0 czyści całą listę (pojedynczych nie usuniesz), więc do wymiany listy wyczyść ją i ustaw nową w tym samym poleceniu. Zdalnie: --dest z identyfikatorem !nodeid (na Linuksie i Macu w apostrofach), obsługiwane są tylko --set i --get. Stary kanał "admin" (firmware do 2.4) działa w 2.5+ tylko po jawnym włączeniu i służy do zarządzania starymi węzłami. Dokumentacja zaleca najpierw ćwiczyć na węźle testowym, bo zły ustaw może odciąć węzeł od sieci.
Słowa: zdalna administracja, remote admin, admin key, dest, nodeid, kanał admin, konfiguracja przez sieć, odcięcie węzła, security.admin_key
Źródło: https://meshtastic.org/docs/configuration/remote-admin/ (sprawdzono 2026-10)

### CLI Meshtastic: najczęściej używane polecenia
Instalacja: pip3 install --upgrade "meshtastic[cli]". Połączenie: --port /dev/ttyUSB0 (serial), --host meshtastic.local (sieć), --ble nazwa (Bluetooth; wcześniej --ble-scan). Odczyt: --info, --nodes, --get lora.region, --qr, --qr-all. Zmiana: --set lora.region EU_868, --set-owner "Nazwa", --set-owner-short "AB" (do 4 znaków), --ch-add nazwa, --ch-index 1 --ch-del, --ch-set. Wiadomości i diagnostyka: --sendtext 'tekst' (z --dest '!id' --ack), --traceroute '!id', --request-telemetry, --seriallog. Sterowanie: --reboot, --shutdown, --factory-reset, --reset-nodedb, --remove-node '!id'. Każde polecenie restartuje urządzenie, więc łącz kilka --set w jednym wywołaniu. Uwaga: --seturl zastępuje całą konfigurację i kanały, a --set-ham wyłącza szyfrowanie na kanale głównym. Kodom QR ufaj tylko ze znanego źródła, bo stosują wszystkie ustawienia radia.
Słowa: CLI, python, pip, meshtastic --info, --set, --get, --sendtext, --traceroute, --factory-reset, --reset-nodedb, --export-config, --seturl, ham, polecenia
Źródło: https://meshtastic.org/docs/software/python/cli/ (sprawdzono 2026-10)

### Klient WWW: Chrome, Bluetooth, USB i HTTPS
Działa na hostowanej stronie client.meshtastic.org lub własnym hostingu; zalecane przeglądarki oparte na Chromium (Chrome, Edge). Połączenia: HTTP (tylko ESP32 po ustawieniu sieci), Bluetooth (Web Bluetooth) i szeregowe USB (Web Serial, najmniej obsługiwane). Wersja hostowana wymaga HTTPS, a węzły mają certyfikat samopodpisany, więc najpierw otwórz https://ADRES_IP_WĘZŁA/ w przeglądarce i zaufaj certyfikatowi. Adres IP znajdziesz na ekranie węzła, w tabeli DHCP routera lub w konsoli szeregowej.
Słowa: klient WWW, web client, Chrome, Edge, Web Bluetooth, Web Serial, HTTPS, certyfikat, samopodpisany, client.meshtastic.org
Źródło: https://meshtastic.org/docs/software/web-client/ (sprawdzono 2026-10)

### Dobre praktyki konfiguracji z oficjalnej dokumentacji
Rola: zostaw CLIENT, CLIENT_MUTE lub CLIENT_BASE. ROUTER i REPEATER zwiększają kolizje, obniżają skuteczność doręczeń i zużywają przeskoki, więc używaj ich tylko po uzgodnieniu z lokalną społecznością; sieć klientów z kilkoma dobrze ustawionymi routerami jest zwykle najstabilniejsza. CLIENT_MUTE nadaje się dla ręcznego węzła obok mocniejszego i dla węzłów wewnętrznych słyszących tylko jeden zewnętrzny. CLIENT_BASE: jeden dobrze położony węzeł na poddaszu lub dachu z ulubionymi własnymi węzłami. Hop limit zostaw na 3; jeśli trzeba więcej, zwiększ tylko na węzłach brzegowych. Lokalizację chroń, zmniejszając precyzję; od 2.7.1 można wyłączyć pozycję na kanale głównym i włączyć na jednym prywatnym kanale dodatkowym. Zmiana presetu, regionu lub slotu nie czyści bazy węzłów, więc dawno słyszane węzły wyglądają na osiągalne; od 2.8.1 węzły niesłyszane na bieżących ustawieniach są oznaczane. Zasięg sprawdzaj wiadomościami prywatnymi, bo tylko potwierdzenie adresata dowodzi dostarczenia. Hałaśliwy węzeł wyciszysz --set-muted-node, a zignorujesz --set-ignored-node.
Słowa: dobre praktyki, rola klienta, CLIENT_BASE, router niepotrzebny, hop limit 3, prywatność pozycji, baza węzłów, test zasięgu, wyciszenie węzła, ignorowanie węzła
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)

### Ekran urządzenia: ustawienia wyświetlacza
Domyślnie: czas świecenia ekranu 0 (czyli 10 minut), automatyczna karuzela ekranów wyłączona, jednostki metryczne, wykrywanie typu OLED automatyczne, tryb wyświetlania domyślny, pogrubiony nagłówek wyłączony, wybudzanie dotknięciem lub ruchem wyłączone, kompas bez ustawienia północy na górze. Można odwrócić ekran (flip_screen).
Słowa: ekran, wyświetlacz, OLED, czas świecenia, karuzela, flip screen, jednostki, kompas, wybudzanie
Źródło: https://meshtastic.org/docs/configuration/radio/display/ (sprawdzono 2026-10)

### Moduły: Neighbor Info, Ambient Lighting, Paxcounter
Neighbor Info (domyślnie wyłączony, informacyjny, nie wpływa na trasowanie): interwał minimum 4 godziny, domyślnie 6 godzin; wysyłanie przez LoRa (firmware 2.5.13+) idzie tylko kanałem głównym, który nie jest publicznym z domyślnym kluczem i nazwą. Ambient Lighting steruje diodą RGB (pierwotnie RAK14001): prąd domyślnie 10, kolory 0–255, firmware 2.2.5+. Paxcounter liczy urządzenia Wi-Fi i Bluetooth w pobliżu (np. ruch w sklepie), działa tylko na ESP32 z firmware 2.2.17+ i wymaga wyłączonego Wi-Fi i Bluetooth węzła.
Słowa: neighbor info, sąsiedzi, ambient lighting, dioda RGB, RAK14001, paxcounter, liczenie osób, ESP32
Źródło: https://meshtastic.org/docs/configuration/module/neighbor-info/, .../ambient-lighting/, .../paxcounter/ (sprawdzono 2026-10)
