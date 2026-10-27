# MQTT: bramki, tematy, serwery

### Jak działa bramka MQTT (gateway)?
Bramką może być każdy węzeł z bezpośrednim dostępem do internetu (Wi-Fi, Ethernet, 4G, satelita) lub węzeł połączony z aplikacją na telefonie. Do brokera trafiają wszystkie pakiety, zarówno z innych urządzeń mesh, jak i z samej bramki. Kilka bramek w jednej sieci publikuje duplikaty, więc odbiorca powinien deduplikować po identyfikatorze pakietu.
Słowa: MQTT, bramka, gateway, broker, duplikaty, internet, uplink
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Jak wyglądają tematy (topic) MQTT?
Domyślny temat główny to msh/REGION. Temat protobuf: msh/REGION/2/e/NAZWA_KANAŁU/ID_UŻYTKOWNIKA (firmware przed 2.3.0 używał /c/ zamiast /e/), np. msh/US/2/e/LongFast/!abcd1234. Temat JSON: msh/US/2/json/NAZWA_KANAŁU/ID_UŻYTKOWNIKA. Surowy protobuf w mosquitto_sub pokazuje tylko, że ruch płynie, i jest mało czytelny.
Słowa: topic, temat, root topic, msh, protobuf, JSON, mosquitto_sub, ServiceEnvelope
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Jak włączyć uplink i downlink MQTT?
Uplink (publikowanie pakietów do MQTT) i downlink (pakiety z MQTT na sieć radiową) włącza się osobno dla każdego kanału. Przykład CLI dla kanału 0: meshtastic --ch-index 0 --ch-set uplink_enabled true. W węźle ustaw też adres brokera, login i hasło (mqtt.address, mqtt.username, mqtt.password); gdy wszystkie puste, węzeł łączy się z brokerem Meshtastic.
Słowa: uplink_enabled, downlink_enabled, ch-set, mqtt.address, broker, login, hasło
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Jak wysłać wiadomość do sieci przez MQTT (JSON downlink)?
Opublikuj JSON na temat msh/US/2/json/mqtt/ (region zgodny z Twoim). Trzeba mieć kanał o nazwie "mqtt" z włączonym downlinkiem (PSK może być losowy) i zrestartować urządzenie po jego utworzeniu. Wymagane pola: from (dziesiętny ID węzła) i payload; opcjonalnie to (brak = rozgłoszenie), channel (0–7), type: sendtext (payload tekstowy) lub sendposition (latitude_i, longitude_i, opcjonalnie altitude, time). Firmware przed 2.2.20 wymagał jeszcze pola sender.
Słowa: json, sendtext, sendposition, publikowanie, Home Assistant, automatyzacja, wysyłanie wiadomości
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Co zawiera wyjście JSON i czego nie obsługuje?
JSON nie jest obsługiwany na nRF52. Serializowane są m.in. porty: TEXT_MESSAGE_APP, TELEMETRY_APP, NODEINFO_APP, POSITION_APP, WAYPOINT_APP, NEIGHBORINFO_APP, TRACEROUTE_APP, DETECTION_SENSOR_APP, PAXCOUNTER_APP, REMOTE_HARDWARE_APP. Pola: id, channel, from, payload, sender, timestamp, to, type; to = -1 oznacza rozgłoszenie. Przy włączonym encryption_enabled ładunek pozostaje zaszyfrowany kluczem kanału, owinięty w ServiceEnvelope.
Słowa: json, nRF52, pola, timestamp, encryption_enabled, szyfrowanie mqtt
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Jakie zasady ma publiczny serwer MQTT Meshtastic?
Polityka zero-hop: bezpośrednio podłączone węzły dostają ruch z serwera, ale nie rozchodzi się on dalej po lokalnej sieci. Przy domyślnym PSK priorytet mają tylko NodeInfo, TextMessageCompressed, TextMessage, Position, Telemetry, MapReport i Routing, a pozycje tylko mało precyzyjne (10–16 bitów). Ograniczenia działają po stronie serwera, bez aktualizacji firmware.
Słowa: publiczny serwer, mqtt.meshtastic.org, zero hop, filtr, pozycja, domyślny klucz
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)

### Czy na własnym brokerze wolno użyć domyślnego klucza kanału?
Nie. Dokumentacja ostrzega, że domyślny PSK na prywatnym brokerze może stworzyć luki bezpieczeństwa i zalać sieć, bo prywatne brokery nie wymuszają polityki zero-hop. Prywatne brokery są do kanałów prywatnych z własnym PSK.
Słowa: prywatny broker, mosquitto, własny serwer, bezpieczeństwo, zalanie sieci
Źródło: https://meshtastic.org/docs/software/mqtt/ (sprawdzono 2026-10)
