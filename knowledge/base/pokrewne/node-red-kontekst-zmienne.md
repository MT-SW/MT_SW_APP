<!-- Baza wiedzy MT_SW: zakresy kontekstu (zmiennych) w Node-RED i ich zapis na dysku. Tylko informacyjnie. -->
# Node-RED: kontekst i zmienne między węzłami

### Jakie są zakresy kontekstu w Node-RED: węzeł, przepływ i globalny?
Kontekst w Node-RED to miejsce na dane współdzielone między węzłami bez przesyłania ich w wiadomościach. Ma trzy zakresy. Węzeł (node) widzi tylko węzeł, który ustawił wartość. Przepływ (flow) widzi cały przepływ, czyli zakładka w edytorze. Globalny (global) widzi wszystkie węzły. Wybór zakresu zależy od tego, kto potrzebuje wartości. Jeśli tylko jeden węzeł funkcyjny, wystarcza zakres węzła. W podprzepływach (subflow) węzły współdzielą kontekst podprzepływu, a nie przepływu nadrzędnego. Od wersji 0.20 da się sięgnąć do rodzica przez prefiks $parent. Zakres globalny warto stosować ostrożnie, bo każdy przepływ może go zmienić. Źródło nie opisuje dokładnie ryzyka konfliktów, więc ten wniosek jest z ogólnej wiedzy.
Słowa: node-red kontekst, context, zakres, flow context, global context, node context, $parent, podprzepływ, subflow, zmienne, flow.set, global.get, zmienna między węzłami
Źródło: https://nodered.org/docs/user-guide/context (sprawdzono 2026-10; oficjalna dokumentacja Node-RED)

### Czy zmienne kontekstu w Node-RED przetrwają restart i jak je zapisać na dysku?
Domyślnie kontekst jest tylko w pamięci, więc po restarcie Node-RED wszystko znika. Żeby zapisać go na dysku, dodaje się w pliku settings.js blok contextStorage z modułem localfilesystem jako domyślnym magazynem. Dane trafiają wtedy do plików w katalogu ~/.node-red/context/. Wartości są trzymane w pamięci i zapisywane na dysk co 30 sekund, według opisu dokumentacji. Oprócz tego Node-RED ma wbudowany moduł memory i można dodać własne magazyny. Wybór magazynu dla ważnych danych, np. stanu alarmu, zależy od tego, czy zapis co 30 sekund wystarcza.
Słowa: node-red persistent, zapis kontekstu, contextStorage, localfilesystem, settings.js, ~/.node-red/context, restart, dane po restarcie, memory store, magazyn kontekstu, 30 sekund, trwały kontekst
Źródło: https://nodered.org/docs/user-guide/context (sprawdzono 2026-10; oficjalna dokumentacja Node-RED; interwał 30 sekund w opisie dokumentacji)
