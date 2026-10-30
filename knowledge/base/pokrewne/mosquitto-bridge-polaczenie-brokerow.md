### Jak zacząć mostek (bridge) między dwoma brokerami Mosquitto?
Dyrektywa `connection <name>` rozpoczyna nowy mostek. Podana nazwa jest też używana jako ID klienta na zdalnym brokerze. Adres zdalnego brokera podaje się w `address`, a port domyślnie to 1883.

Słowa: mosquitto, bridge, connection, address, broker, mostek
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10)

### Co oznacza kierunek in, out i both w temacie mostka Mosquitto?
W dyrektywie `topic` kierunek `in` importuje wiadomości ze zdalnego brokera, `out` eksportuje je na zdalny broker, a `both` przekazuje w obie strony. Gdy kierunek nie jest podany, domyślnie jest `out`.

Słowa: mosquitto, bridge, topic, in, out, both, kierunek
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10)

### Jak działają prefiksy local-prefix i remote-prefix w mostku Mosquitto?
Prefiksy w dyrektywie `topic` przemapowują tematy między lokalnym a zdalnym brokerem. Pusty prefiks zapisuje się jako `""`. Poziom QoS tematu mostka domyślnie wynosi 0.

Słowa: mosquitto, bridge, prefix, local-prefix, remote-prefix, QoS, przemapowanie tematów
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10)

### Czym różni się cleansession w mostku Mosquitto?
Domyślnie `cleansession` ma wartość `false`, więc zdalne subskrypcje zostają po zerwaniu połączenia. Wartość `true` powoduje, że przy zerwaniu zdalny broker usuwa subskrypcje i wiadomości.

Słowa: mosquitto, bridge, cleansession, subskrypcje, zerwanie połączenia
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10)
