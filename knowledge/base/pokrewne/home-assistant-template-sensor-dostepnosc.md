### Jak zdefiniować template sensor w Home Assistant?
Encje z szablonem definiuje się pod kluczem `template:`. Szablony służą do ustawiania właściwości, np. nazwy albo stanu. Stan ustawia szablon `state`.

Słowa: home assistant, template, template sensor, state, szablon, YAML
Źródło: https://www.home-assistant.io/integrations/template/ (sprawdzono 2026-10)

### Do czego służy unique_id w szablonie Home Assistant?
Opcjonalne `unique_id` pozwala zmieniać nazwę, ikonę i `entity_id` z poziomu interfejsu WWW. Zmiana `entity_id` w interfejsie nadpisuje wartość `default_entity_id`.

Słowa: home assistant, template, unique_id, entity_id, default_entity_id, interfejs
Źródło: https://www.home-assistant.io/integrations/template/ (sprawdzono 2026-10)

### Jak działa availability w szablonie Home Assistant?
Szablon `availability` jest opcjonalny. Gdy go brak, encja jest zawsze dostępna. Jeśli szablon nie wyrenderuje się albo zwróci `True`, `"1"`, `"true"`, `"yes"`, `"on"`, `"enable"` lub niezerową liczbę, encja jest dostępna. Inne wartości oznaczają niedostępność, a porównanie tekstu nie rozróżnia wielkości liter.

Słowa: home assistant, template, availability, unavailable, dostępność, szablon
Źródło: https://www.home-assistant.io/integrations/template/ (sprawdzono 2026-10)

### Czym różni się stan unknown od unavailable w szablonie Home Assistant?
Stan `unavailable` wynika z szablonu `availability`. Stan `unknown` wynika z wyniku szablonu `state`. W czujnikach binarnych wynik `None` daje `unknown`, a każda inna nieprawdziwa wartość daje `off`.

Słowa: home assistant, unknown, unavailable, binary sensor, None, off
Źródło: https://www.home-assistant.io/integrations/template/ (sprawdzono 2026-10)
