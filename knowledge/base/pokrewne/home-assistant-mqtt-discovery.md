### Czym jest MQTT discovery w Home Assistant?
MQTT discovery pozwala urządzeniu zgłosić się do Home Assistant przez opublikowanie konfiguracji w formie JSON na określonym temacie MQTT. Home Assistant sam tworzy encje, więc ręczna konfiguracja jest zbędna. Funkcja jest domyślnie włączona.

Słowa: home assistant, mqtt, discovery, autodiscovery, encje, konfiguracja
Źródło: https://www.home-assistant.io/integrations/mqtt/#discovery (sprawdzono 2026-10)

### Jaki jest domyślny prefiks i format tematu discovery w MQTT?
Domyślny prefiks to `homeassistant`. Tematy mają postać `<discovery_prefix>/<component>/[<node_id>/]<object_id>/config`. Komponentem jest platforma, np. `sensor` lub `switch`, a `<object_id>` może zawierać tylko litery, cyfry, podkreślenia i myślniki. Część `node_id` jest opcjonalna i Home Assistant jej nie wykorzystuje.

Słowa: home assistant, mqtt, discovery, prefiks, homeassistant, object_id, config
Źródło: https://www.home-assistant.io/integrations/mqtt/#discovery (sprawdzono 2026-10)

### Jak usunąć encję zgłoszoną przez MQTT discovery?
Trzeba opublikować pusty ciąg na temacie `config` tej encji. Komponent zostaje usunięty, a jeśli nic innego nie odwołuje się do urządzenia, znika też wpis urządzenia. Żeby usunięcie utrzymało się po restarcie brokera, pusty komunikat wysyła się z flagą retain.

Słowa: home assistant, mqtt, discovery, usuwanie encji, pusty payload, retain
Źródło: https://www.home-assistant.io/integrations/mqtt/#discovery (sprawdzono 2026-10)

### Czy discovery w MQTT wymaga flagi retain?
Nie jest to wymagane. Konfigurację można wysłać z flagą retain, a broker przechowa ją i odda po połączeniu Home Assistant. Bez retain urządzenie powinno ponownie wysłać konfigurację po starcie integracji, zwykle nasłuchując wiadomości birth na `homeassistant/status`. Dokumentacja ostrzega, że retained wiadomości mogą tworzyć tzw. duchy encji.

Słowa: home assistant, mqtt, discovery, retain, homeassistant/status, duchy encji
Źródło: https://www.home-assistant.io/integrations/mqtt/#discovery (sprawdzono 2026-10)
