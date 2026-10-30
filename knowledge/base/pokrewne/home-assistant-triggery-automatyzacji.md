### Czym jest trigger w automatyzacji Home Assistant?
Trigger to część automatyzacji, która ją uruchamia, gdy zajdzie określone zdarzenie. Po zadziałaniu triggera Home Assistant sprawdza warunki i jeśli są spełnione, wykonuje akcje. Trigger reaguje w konkretnym momencie, a nie na to, że stan trwa.

Słowa: home assistant, automatyzacja, trigger, warunki, akcje, zdarzenie
Źródło: https://www.home-assistant.io/docs/automation/trigger/ (sprawdzono 2026-10)

### Kiedy reaguje trigger typu state w Home Assistant?
Trigger typu state reaguje, gdy zmienia się stan encji, np. z `unavailable` na `on`. Przykład z dokumentacji dotyczy zmiany stanu drzwi z zamkniętych na otwarte, a nie stanu trwającego.

Słowa: home assistant, trigger, state, zmiana stanu, encja, unavailable
Źródło: https://www.home-assistant.io/docs/automation/trigger/ (sprawdzono 2026-10)

### Do czego służy trigger numeric_state w Home Assistant?
Trigger numeric state reaguje, gdy wartość liczbowa przekroczy zadany poziom. W przykładzie dokumentacji sprawdzany jest moment, w którym wysokość Słońca spada poniżej -4°.

Słowa: home assistant, numeric state, próg, wartość liczbowa, wysokość słońca
Źródło: https://www.home-assistant.io/docs/automation/trigger/ (sprawdzono 2026-10)

### Jakie triggery czasowe są w Home Assistant?
Trigger time reaguje o określonej godzinie dnia, np. 15:32. Time pattern reaguje w powtarzających się odstępach, np. co 5 minut. Trigger sun reaguje, gdy Słońce osiąga 0° wysokości, czyli wschód lub zachód, i można go przesunąć opcjonalnym offsetem.

Słowa: home assistant, trigger, time, time pattern, sun, wschód, zachód, offset
Źródło: https://www.home-assistant.io/docs/automation/trigger/ (sprawdzono 2026-10)

### Jakie triggery zdarzeń i sieci są w Home Assistant?
Do typów należą event (własny typ zdarzenia), MQTT (powiązany z tematem MQTT) i webhook (reaguje na żądanie HTTP na adres webhooka). Strona podaje też trigger sentence dla Assist, tag dla zeskanowanych tagów i zone dla wejścia lub wyjścia osoby albo śledzonego urządzenia z strefy.

Słowa: home assistant, trigger, event, MQTT, webhook, zone, tag, sentence, Assist
Źródło: https://www.home-assistant.io/docs/automation/trigger/ (sprawdzono 2026-10)
