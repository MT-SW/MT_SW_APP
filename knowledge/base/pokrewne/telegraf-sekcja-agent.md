### Do czego służy sekcja [agent] w konfiguracji Telegrafa?
Tabela `[agent]` steruje ogólnym zachowaniem agenta, czyli interwałami zbierania, buforowaniem, zapisem, logowaniem i innymi ustawieniami dla całego procesu. Konfiguracja wpływa na wszystkie wtyczki naraz.

Słowa: telegraf, agent, konfiguracja, toml, interval, buforowanie
Źródło: https://docs.influxdata.com/telegraf/v1/configuration/ (sprawdzono 2026-10)

### Które ustawienia harmonogramu są w sekcji [agent] Telegrafa?
Ustawienia `interval`, `round_interval`, `collection_jitter`, `collection_offset`, `flush_interval` i `flush_jitter` decydują, kiedy Telegraf zbiera i zapisuje metryki. Typy i wartości domyślne trzeba sprawdzić w osobnej stronie referencyjnej ustawień agenta.

Słowa: telegraf, agent, interval, flush_interval, jitter, harmonogram zbierania
Źródło: https://docs.influxdata.com/telegraf/v1/configuration/ (sprawdzono 2026-10)

### Jak włączyć wtyczkę w Telegrafie?
Wtyczkę włącza się, dodając jej tabelę TOML, np. `[[inputs.cpu]]`, `[[processors.*]]` albo `[[outputs.influxdb_v3]]`. Tę samą wtyczkę można zdefiniować wielokrotnie jako osobne instancje. Poprawna konfiguracja potrzebuje co najmniej jednego wejścia i jednego wyjścia.

Słowa: telegraf, toml, inputs, outputs, wtyczka, instancja
Źródło: https://docs.influxdata.com/telegraf/v1/configuration/ (sprawdzono 2026-10)

### Jakie wspólne opcje mają wtyczki Telegrafa?
Wszystkie wtyczki mają wspólne opcje, m.in. `alias`, `interval`, `name_override` i `tags`. Szczegóły działania każdej z nich są na stronach opcji konkretnej wtyczki.

Słowa: telegraf, alias, name_override, tags, interval, opcje wspólne
Źródło: https://docs.influxdata.com/telegraf/v1/configuration/ (sprawdzono 2026-10)
