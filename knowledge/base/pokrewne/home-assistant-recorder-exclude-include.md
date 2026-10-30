### Co wyklucza opcja exclude w recorderze Home Assistant?
Opcja `exclude` zatrzymuje zapis wybranych encji, domen i typów zdarzeń do bazy recordera. Może obejmować `domains`, `entity_globs`, `entities` i `event_types`.

Słowa: home assistant, recorder, exclude, wykluczanie, domeny, encje, baza danych
Źródło: https://www.home-assistant.io/integrations/recorder/#exclude (sprawdzono 2026-10)

### Czym różni się wykluczenie w recorderze od ukrycia w panelu Activity?
Strona podaje, że wykluczone zdarzenia nie są w ogóle zapisywane w bazie. Ukrycie w panelu Activity zmienia tylko to, co jest wyświetlane.

Słowa: home assistant, recorder, exclude, Activity, zapis w bazie, ukrywanie
Źródło: https://www.home-assistant.io/integrations/recorder/#exclude (sprawdzono 2026-10)

### Jak wzorce entity_globs działają w recorderze Home Assistant?
Symbol `*` pasuje do zera lub więcej znaków, a `?` do zera lub jednego znaku. Przykład to `sensor.weather_*`.

Słowa: home assistant, recorder, entity_globs, wzorzec, gwiazdka, znak zapytania
Źródło: https://www.home-assistant.io/integrations/recorder/#exclude (sprawdzono 2026-10)

### Jak wspólnie działają include i exclude w recorderze Home Assistant?
Gdy podano tylko `exclude`, encja jest wykluczona, jeśli jest na liście albo pasuje do wykluczonej domeny lub wzorca. Encja jawnie wymieniona w `include` jest zawsze zapisywana, chyba że jest też w `exclude`. Gdy podano tylko `include`, encje niepasujące do reguł nie są zapisywane.

Słowa: home assistant, recorder, include, exclude, reguły, zapis, domena
Źródło: https://www.home-assistant.io/integrations/recorder/#exclude (sprawdzono 2026-10)
