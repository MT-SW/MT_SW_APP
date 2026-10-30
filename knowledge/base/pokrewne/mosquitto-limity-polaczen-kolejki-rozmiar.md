### Co robi max_connections w Mosquitto?
Opcja `max_connections` ogranicza łączną liczbę klientów podłączonych do danego listenera. Wartość `-1` oznacza brak limitu w samym Mosquitto, choć inne ograniczenia spoza brokera mogą nadal obowiązywać.

Słowa: mosquitto, max_connections, listener, klienci, limit, -1
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#max_connections (sprawdzono 2026-10)

### Ile wiadomości QoS 1 i 2 może być w locie w Mosquitto?
Opcja `max_inflight_messages` domyślnie wynosi 20. Ogranicza liczbę wychodzących wiadomości QoS 1 lub 2 wysyłanych jednocześnie. Wartość 0 usuwa limit, a wartość 1 gwarantuje kolejność dostarczania.

Słowa: mosquitto, max_inflight_messages, QoS 1, QoS 2, kolejność, 20, w locie
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#max_connections (sprawdzono 2026-10)

### Jaki jest domyślny limit kolejki max_queued_messages w Mosquitto?
Domyślnie `max_queued_messages` wynosi 1000 i ogranicza wiadomości QoS 1 lub 2 w kolejce na klienta, ponad te aktualnie w locie. Wartość 0 usuwa limit, czego strona nie zaleca. Jeśli ustawiono też `max_queued_bytes`, kolejka zatrzymuje się na tym limicie, który zostanie osiągnięty pierwszy.

Słowa: mosquitto, max_queued_messages, kolejka, 1000, QoS, max_queued_bytes
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#max_connections (sprawdzono 2026-10)

### Co zrobi Mosquitto z wiadomością większą niż message_size_limit?
Domyślnie `message_size_limit` to 0, czyli przyjmowane są wszystkie poprawne wiadomości MQTT, a sam MQTT ogranicza ładunek do 268435455 bajtów. Wiadomość przekraczająca limit nie jest przekazywana subskrybentom, ale przepływ QoS 1 lub 2 się kończy. Klienci MQTT v5 dostają błąd implementacyjny w kodzie PUBACK lub PUBREC.

Słowa: mosquitto, message_size_limit, payload, 268435455, PUBACK, MQTT v5, błąd
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#max_connections (sprawdzono 2026-10)
