### Czym jest warunek w automatyzacji Home Assistant?
Warunek to sprawdzenie, czy coś jest prawdziwe w danej chwili, np. czy drzwi są zamknięte albo czy ktoś jest w domu. Warunki są opcjonalne. Gdy trigger uruchomi automatyzację, Home Assistant sprawdza warunki, a jeśli są spełnione, wykonuje akcje.

Słowa: home assistant, automatyzacja, warunek, condition, akcje, opcjonalne
Źródło: https://www.home-assistant.io/docs/automation/condition/ (sprawdzono 2026-10)

### Czym różni się warunek od triggera w Home Assistant?
Trigger uruchamia automatyzację, a warunek tego nie robi. Warunki są sprawdzane tylko w momencie uruchomienia automatyzacji, więc późniejsza zmiana stanu nie jest ponownie sprawdzana. Żeby reagować na zmianę, trzeba użyć triggera.

Słowa: home assistant, warunek, trigger, zmiana stanu, sprawdzanie w momencie uruchomienia
Źródło: https://www.home-assistant.io/docs/automation/condition/ (sprawdzono 2026-10)

### Jakie wbudowane warunki są w Home Assistant?
Wbudowane warunki, niezależne od integracji, to state, numeric state, time i template. Są też warunki zależne od integracji, np. "światło jest włączone" albo "słońce jest na niebie", oraz warunki urządzeń w grupie Device.

Słowa: home assistant, warunki, state, numeric state, time, template, integracja
Źródło: https://www.home-assistant.io/docs/automation/condition/ (sprawdzono 2026-10)

### Jak łączyć warunki w automatyzacji Home Assistant?
Domyślnie wszystkie warunki muszą być spełnione. Blok `or` działa, gdy spełniony jest co najmniej jeden warunek w środku. Blok `and` wymaga spełnienia wszystkich, a `not` działa, gdy żaden z warunków wewnątrz nie jest spełniony. Strona zaleca używać `and` wewnątrz `or` lub `not`.

Słowa: home assistant, warunki, and, or, not, łączenie warunków, AND
Źródło: https://www.home-assistant.io/docs/automation/condition/ (sprawdzono 2026-10)

### Jak wyłączyć pojedynczy warunek w Home Assistant bez usuwania go?
Warunek można wyłączyć, co powoduje pominięcie go tak, jakby go nie było. W YAML służy do tego `enabled: false`.

Słowa: home assistant, warunek, enabled, wyłączanie warunku, YAML
Źródło: https://www.home-assistant.io/docs/automation/condition/ (sprawdzono 2026-10)
