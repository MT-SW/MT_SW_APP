### Do czego służy availability_topic w encjach MQTT Home Assistant?
Opcja `availability_topic` to temat, na który encja subskrybuje aktualizacje online i offline. Nie można jej łączyć z listą `availability`.

Słowa: home assistant, mqtt, availability_topic, online, offline, dostępność
Źródło: https://www.home-assistant.io/integrations/mqtt/#availability (sprawdzono 2026-10)

### Jakie domyślne wartości mają payload_available i payload_not_available w MQTT Home Assistant?
Domyślnie `payload_available` to `online`, a `payload_not_available` to `offline`. Są to wartości komunikatów, które oznaczają dostępność lub brak dostępności encji.

Słowa: home assistant, mqtt, payload_available, payload_not_available, online, offline
Źródło: https://www.home-assistant.io/integrations/mqtt/#availability (sprawdzono 2026-10)

### Jak działa availability_mode w MQTT Home Assistant?
Domyślnie tryb to `latest`, czyli liczy się ostatnia wiadomość dostępności z dowolnego tematu. Tryb `all` wymaga komunikatu dostępności na wszystkich tematach, a `any` na co najmniej jednym. Dotyczy to przypadku, gdy skonfigurowano listę `availability`.

Słowa: home assistant, mqtt, availability_mode, latest, all, any, lista availability
Źródło: https://www.home-assistant.io/integrations/mqtt/#availability (sprawdzono 2026-10)

### Czy encje MQTT z unique ID są niedostępne po restarcie Home Assistant?
Tak, według dokumentacji wykryte encje MQTT z unique ID są niedostępne po restarcie, aż do przyjścia nowej wiadomości discovery. Strona nie opisuje, jak wygląda to w interfejsie.

Słowa: home assistant, mqtt, unique ID, restart, discovery, niedostępne po restarcie
Źródło: https://www.home-assistant.io/integrations/mqtt/#availability (sprawdzono 2026-10)
