<!-- Baza wiedzy MT_SW: konfiguracja czujnika MQTT w Home Assistant (state_topic, jednostki, klasa, szablon, wiadomości retained). Tylko informacyjnie. -->
# Home Assistant: czujnik MQTT

### Jakie pola są potrzebne w czujniku MQTT w Home Assistant i co dają unit_of_measurement, device_class i state_class?
Jedynym wymaganym polem czujnika MQTT jest state_topic, czyli temat, z którego Home Assistant odbiera wartości. Pole unit_of_measurement podaje jednostkę, np. °C albo %. Pole device_class ustala typ czujnika i wygląd ikony w interfejsie, a może być null. Pole state_class określa klasę stanu. Jeśli użyjesz last_reset_value_template, state_class musi być total. Pole value_template wyciąga wartość z wiadomości, np. {{ value_json.temperature }}. Gdy szablon zwróci błąd, czujnik zachowuje poprzedni stan.
Słowa: czujnik mqtt, sensor mqtt, state_topic, unit_of_measurement, device_class, state_class, value_template, value_json, temperatura, wilgotność, jednostki, ikona, szablon
Źródło: https://www.home-assistant.io/integrations/sensor.mqtt/ (sprawdzono 2026-10; oficjalna dokumentacja Home Assistant)

### Co się dzieje z czujnikiem MQTT po restarcie Home Assistant i kiedy nie zachowywać stanu jako retained?
Jeśli broker zachował ostatnią wiadomość z flagą RETAIN, czujnik dostaje ją od razu po subskrypcji, więc po restarcie pokazuje ostatnią znaną wartość. Bez tej flagi początkowy stan jest nieokreślony. Dokumentacja odradza zachowywanie wiadomości stanu, gdy używasz expire_after, bo odtworzona wartość może sprawiać wrażenie, że czujnik jest dostępny z wygasłym stanem. Home Assistant sam zapisuje i przywraca stan czujnika, a czas przechowywania oblicza przed uznaniem go za niedostępny. Domyślnie payload_available to online, a payload_not_available to offline, a availability_mode ma wartość latest.
Słowa: czujnik mqtt restart, retained, retain flag, expire_after, stan nieokreślony, wygasły stan, payload_available, payload_not_available, online offline, availability_mode, latest, przywracanie stanu
Źródło: https://www.home-assistant.io/integrations/sensor.mqtt/ (sprawdzono 2026-10; oficjalna dokumentacja Home Assistant)
