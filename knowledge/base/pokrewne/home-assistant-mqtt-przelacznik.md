### Które pole jest wymagane w przełączniku MQTT w Home Assistant?
Przełącznik MQTT wymaga tylko `command_topic`, czyli tematu, na który Home Assistant publikuje polecenia zmiany stanu. `state_topic` jest opcjonalny. Bez niego integracja działa w trybie optymistycznym i sama zakłada zmianę stanu po każdym wysłanym poleceniu.

Słowa: home assistant, mqtt, przełącznik, switch, command_topic, state_topic
Źródło: https://www.home-assistant.io/integrations/switch.mqtt/ (sprawdzono 2026-10)

### Kiedy przełącznik MQTT pracuje w trybie optymistycznym?
Tryb optymistyczny jest domyślnie włączony, gdy nie podano `state_topic`, a domyślnie wyłączony, gdy temat stanu istnieje. Można go też wymusić ręcznie nawet przy istniejącym `state_topic`. W trybie wyłączonym przełącznik czeka z pokazaniem zmiany, aż urządzenie potwierdzi nowy stan na temacie stanu.

Słowa: home assistant, mqtt, optimistic, tryb optymistyczny, state_topic
Źródło: https://www.home-assistant.io/integrations/switch.mqtt/ (sprawdzono 2026-10)

### Jakie wartości ON i OFF wysyła przełącznik MQTT domyślnie?
Domyślnie `payload_on` to `ON`, a `payload_off` to `OFF`. Te same wartości służą do porównywania komunikatów z tematu stanu oraz jako treść poleceń wysyłanych na `command_topic`. Jeśli urządzenie używa innych słów, np. `1` i `0`, trzeba je ustawić w konfiguracji.

Słowa: home assistant, mqtt, payload_on, payload_off, ON, OFF
Źródło: https://www.home-assistant.io/integrations/switch.mqtt/ (sprawdzono 2026-10)

### Co daje flaga retain w przełączniku MQTT?
Domyślnie `retain` jest wyłączone i wtedy stan startowy przełącznika to `unknown`. Jeśli urządzenie publikuje stan z flagą RETAIN, Home Assistant dostaje aktualny stan od razu po subskrypcji. Komunikat z tematu stanu z wartością `None` resetuje stan do `unknown`, a pusty komunikat jest ignorowany.

Słowa: home assistant, mqtt, retain, state_topic, unknown
Źródło: https://www.home-assistant.io/integrations/switch.mqtt/ (sprawdzono 2026-10)
