### Jakie są state_class w sensorach Home Assistant?
Dla sensorów o stanie liczbowym integracja ustawia `state_class`: `measurement` to aktualna wartość, `measurement_angle` to kąt w stopniach, `total` to kwota rosnąca i malejąca, a `total_increasing` to kwota tylko rosnąca, z wyjątkiem resetu.

Słowa: home assistant, state_class, measurement, total, total_increasing, sensor, liczba
Źródło: https://www.home-assistant.io/integrations/sensor/#state_class (sprawdzono 2026-10)

### Jak długo Home Assistant trzyma statystyki krótkoterminowe sensorów?
Sensory z `state_class` dostają statystyki. Krótkoterminowe zapisywane są co 5 minut i domyślnie usuwane po 10 dniach. Długoterminowe zapisywane są co godzinę i nigdy nie są usuwane automatycznie. Sensory bez `state_class` nie mają statystyk długoterminowych.

Słowa: home assistant, statystyki, krótkoterminowe, długoterminowe, 10 dni, 5 minut, state_class
Źródło: https://www.home-assistant.io/integrations/sensor/#state_class (sprawdzono 2026-10)

### Do czego służy device_class w sensorze Home Assistant?
Device class określa, co mierzy sensor, np. temperaturę lub energię. Wpływa na ikonę, jednostkę i format wyświetlania oraz na to, gdzie sensor można użyć, np. w niektórych triggerach, warunkach, panelu energii albo jako sensor temperatury obszaru. W szablonach pojawia się jako atrybut `device_class`.

Słowa: home assistant, device_class, temperatura, energia, ikona, jednostka, panel energii
Źródło: https://www.home-assistant.io/integrations/sensor/#state_class (sprawdzono 2026-10)
