### Co to jest integracja Home Assistant Core?
Integracja Core dostarcza ogólne implementacje, np. akcję `homeassistant.turn_on`, i obsługuje ogólne ustawienia oraz ustawienia encji. Konfiguruje się ją w sekcji `homeassistant:` w pliku `configuration.yaml`.

Słowa: home assistant, core, homeassistant, configuration.yaml, ustawienia ogólne, turn_on
Źródło: https://www.home-assistant.io/integrations/homeassistant/ (sprawdzono 2026-10)

### Do czego służą latitude i longitude w sekcji homeassistant?
Współrzędne `latitude` i `longitude` służą do wyliczania czasu wschodu i zachodu Słońca. Opcja `name` to nazwa miejsca, w którym działa Home Assistant.

Słowa: home assistant, latitude, longitude, wschód, zachód, współrzędne, name
Źródło: https://www.home-assistant.io/integrations/homeassistant/ (sprawdzono 2026-10)

### Jakie są opcje unit_system w Home Assistant?
Opcja `unit_system` przyjmuje `metric` lub `us_customary`. Ustawia też jednostkę temperatury, ale można ją nadpisać opcją `temperature_unit`.

Słowa: home assistant, unit_system, metric, us_customary, temperature_unit, jednostki
Źródło: https://www.home-assistant.io/integrations/homeassistant/ (sprawdzono 2026-10)

### Jak podać strefę czasową w Home Assistant?
Opcja `time_zone` przyjmuje nazwę ze bazy tz, np. `"America/Los_Angeles"`. Domyślna waluta na stronie to EUR, a ustawienia ogólne można też zmienić w interfejsie w Settings > System > General.

Słowa: home assistant, time_zone, strefa czasowa, tz, currency, EUR, interfejs
Źródło: https://www.home-assistant.io/integrations/homeassistant/ (sprawdzono 2026-10)

### Jak nadpisać nazwę i ikonę encji w Home Assistant przez customize?
Sekcje `customize`, `customize_domain` i `customize_glob` nadpisują atrybuty konkretnych encji, całych domen albo encji pasujących do wzorca. Można zmieniać m.in. `friendly_name`, `icon` i `entity_picture`. Te sekcje muszą być w `homeassistant:`, inaczej zawiodą.

Słowa: home assistant, customize, customize_domain, customize_glob, friendly_name, icon, entity_picture
Źródło: https://www.home-assistant.io/integrations/homeassistant/ (sprawdzono 2026-10)
