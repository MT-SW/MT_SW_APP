### Co robi auto_purge w recorderze Home Assistant?
Opcja `auto_purge` domyślnie ma wartość `true` i czyści bazę automatycznie co noc o 04:12 czasu lokalnego. Strona podaje, że dzięki temu baza nie rośnie w nieskończoność. Po wyłączeniu zaleca się automatyzację, która okresowo wywołuje `recorder.purge`.

Słowa: home assistant, recorder, auto_purge, 04:12, recorder.purge, baza danych
Źródło: https://www.home-assistant.io/integrations/recorder/#purge_interval (sprawdzono 2026-10)

### Co robi auto_repack w recorderze Home Assistant?
Opcja `auto_repack` domyślnie ma wartość `true` i przepakowuje bazę automatycznie w drugą niedzielę każdego miesiąca, po automatycznym czyszczeniu. Strona zaznacza, że bez przepakowania baza może po czyszczeniu nie zmniejszyć się. Opcja nie działa, gdy `auto_purge` jest wyłączone.

Słowa: home assistant, recorder, auto_repack, druga niedziela, przepakowanie, zmniejszanie bazy
Źródło: https://www.home-assistant.io/integrations/recorder/#purge_interval (sprawdzono 2026-10)

### Jak uruchomić węzeł inject w Node-RED ręcznie i co może wysyłać?
Węzeł inject uruchamia przepływ ręcznie przez kliknięcie przycisku w edytorze. Może też działać cyklicznie, z interwałem do 596 godzin albo z opcjami czasu opartymi na cronie. Jako payload może wysyłać wartość właściwości kontekstu, String, liczbę, wartość logiczną, Buffer, obiekt albo znacznik czasu w milisekundach od 1 stycznia 1970.

Słowa: node-red, inject, przycisk, interwał, 596 godzin, cron, payload, timestamp
Źródło: https://nodered.org/docs/user-guide/nodes (sprawdzono 2026-10)
