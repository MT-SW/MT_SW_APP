# Ustawienia MQTT, sieci Wi-Fi/Ethernet, Bluetooth i wiadomości gotowych

### Ustawienia modułu MQTT: domyślne wartości
Moduł jest domyślnie wyłączony. Adres serwera domyślnie mqtt.meshtastic.org, użytkownik meshdev, hasło large4cats (publiczny broker). Encryption Enabled domyślnie wyłączone: wtedy wszystkie wiadomości trafiają do brokera nieszyfrowane, nawet gdy kanały mają klucze. JSON (domyślnie wyłączony) jest zawsze nieszyfrowany i nie działa na nRF52. TLS próbuje bezpiecznego połączenia. Root Topic pozwala oddzielić sieci (z listami ACL na brokerze). Client Proxy używa internetu telefonu; płytki nRF52 i RP2040 nie mają ustawień sieci, więc muszą go używać. Żeby pakiety szły, na kanale musi być włączony uplink lub downlink.
Słowa: MQTT ustawienia, mqtt.meshtastic.org, meshdev, large4cats, encryption_enabled, JSON, TLS, root topic, client proxy, uplink, downlink, nRF52 MQTT
Źródło: https://meshtastic.org/docs/configuration/module/mqtt/ (sprawdzono 2026-10)

### Raport mapy MQTT: co ujawnia i jak często?
Map Reporting (od firmware 2.3.2, domyślnie wyłączony) wysyła nieszyfrowane raporty, które mogą zawierać nazwy węzła, ID, pozycję, wysokość, model sprzętu, rolę, wersję firmware, region, preset i nazwę kanału głównego. Domyślna precyzja pozycji to odchylenie ok. 1459 m. Interwał publikacji to 3600 s (1 godzina) i jest to zarazem minimum. Domyślny kanał LongFast na publicznym serwerze ma bardzo duży ruch, który może przeciążyć urządzenie, więc lepiej użyć innego kanału lub własnego serwera.
Słowa: map reporting, raport mapy, prywatność, pozycja na mapie, 1459 m, 3600 s, publiczny serwer, przeciążenie, LongFast MQTT
Źródło: https://meshtastic.org/docs/configuration/module/mqtt/ (sprawdzono 2026-10)

### Jak podłączyć węzeł do Wi-Fi lub Ethernetu?
Włącz Wi-Fi, wpisz nazwę sieci (do 32 znaków) i hasło (do 64 znaków); wielkość liter ma znaczenie. Działa tylko sieć 2,4 GHz, a ESP32 łączy się jako klient (nie tworzy własnego punktu dostępu). Włączenie Wi-Fi wyłącza Bluetooth. Pierwsze uruchomienie po włączeniu Wi-Fi lub Ethernetu trwa 20–30 s dłużej, bo generowane są klucze SSL. Z jednym urządzeniem w sieci wejdź na http://meshtastic.local, z kilkoma użyj adresów IP. Tryb adresu: DHCP (domyślnie) lub STATIC (wtedy IP, brama, maska i DNS). Serwer NTP domyślnie meshtastic.pool.ntp.org. Można ustawić serwer rsyslog i flagę UDP_BROADCAST (wysyłanie pakietów przez UDP w sieci lokalnej). Konfiguracja przez niezaufaną sieć Wi-Fi naraża dane.
Słowa: Wi-Fi, Ethernet, SSID, hasło Wi-Fi, 2,4 GHz, meshtastic.local, DHCP, static IP, NTP, rsyslog, UDP broadcast, Bluetooth wyłączony, nie łączy z Wi-Fi
Źródło: https://meshtastic.org/docs/configuration/radio/network/ (sprawdzono 2026-10)

### Bluetooth: tryby parowania i PIN
Bluetooth domyślnie włączony. Tryby: RANDOM_PIN (losowy PIN na ekranie; domyślny dla urządzeń z ekranem), FIXED_PIN (stały, 6 cyfr; domyślnie 123456 dla urządzeń bez ekranu), NO_PIN (bez PIN-u). Dokumentacja ostrzega, że pozostawienie domyślnego kodu to poważne ryzyko, więc zmień go. Jeśli zdejmiesz ekran po pierwszym uruchomieniu, urządzenie zostaje w trybie RANDOM_PIN i nie zobaczysz kodu, więc zmień tryb przed zdjęciem ekranu. Na ESP32 włączenie Wi-Fi wyłącza Bluetooth.
Słowa: Bluetooth, parowanie, PIN, 123456, RANDOM_PIN, FIXED_PIN, NO_PIN, nie mogę sparować, kod parowania, bez ekranu
Źródło: https://meshtastic.org/docs/configuration/radio/bluetooth/ (sprawdzono 2026-10)

### Wiadomości gotowe (Canned Messages) i klawiatury
Moduł wysyła przygotowane wiadomości bez telefonu. Źródła wejścia: dowolne (_any, domyślnie), enkoder obrotowy (rotEnc1), enkoder góra/dół (upDownEnc1, także RAK14006), jeden przycisk (scanAndSelect: krótko przewija, długo wysyła na kanał główny), klawiatura M5 CardKB (także RAK14004) i serialkb. Piny enkodera A, B i Press (1–39; 0 kasuje), np. dla TTGO LoRa V1: 22, 23 i 21. Wiadomości oddziela się znakiem |, razem do 200 bajtów. Opcja dzwonka dodaje znak ASCII 0x07 (może wywołać powiadomienie). Na CardKB w trybie wolnego tekstu piszesz i wciskasz ENTER, a TAB i strzałki wybierają adresata wiadomości prywatnej.
Słowa: canned messages, gotowe wiadomości, enkoder, CardKB, RAK14004, scanAndSelect, przycisk, klawiatura, 200 bajtów, bez telefonu
Źródło: https://meshtastic.org/docs/configuration/module/canned-message/ (sprawdzono 2026-10)
