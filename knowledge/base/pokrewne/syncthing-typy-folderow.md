<!-- Baza wiedzy MT_SW: typy folderów w Syncthing (wysyłanie i odbiór, tylko wysyłanie, tylko odbiór). Tylko informacyjnie. -->
# Syncthing: typy folderów

### Czym różnią się typy folderów w Syncthing: wysyłanie i odbiór, tylko wysyłanie, tylko odbiór?
Domyślny typ to Send & Receive, czyli folder wysyła zmiany i odbiera zmiany z innych urządzeń. Typ Send Only ignoruje zmiany z innych urządzeń, więc folder może się rozjechać, ale nic się nie nadpisuje. Lokalne zmiany są rozsyłane dalej. Typ Receive Only stosuje zmiany z klastra i rozsyła je dalej, ale lokalnych zmian nie wysyła do innych. Lokalne modyfikacje nie oznaczają rozjechania na tym urządzeniu, za to inne urządzenia pokażą je jako niezgodne. Źródło nie opisuje praktycznych zastosowań, ale wskazuje, że Receive Only nadaje się do kopii albo dystrybucji treści, bo nie przenosi lokalnych zmian. To wniosek z ogólnej wiedzy.
Słowa: syncthing typy folderów, send only, receive only, send receive, tryb folderu, wysyłanie, odbiór, kopia, lustro, out of sync, rozjechanie, synchronizacja jednokierunkowa
Źródło: https://docs.syncthing.net/users/foldertypes.html (sprawdzono 2026-10; oficjalna dokumentacja Syncthing; zastosowanie kopii z ogólnej wiedzy)

### Co robią przyciski Override Changes i Revert Local Changes w Syncthing?
Przycisk Override Changes pojawia się przy folderze Send Only, gdy jest rozjechany. Wymusza stan tego urządzenia na klastrze: zmienione pliki są nadpisywane wersjami z tego urządzenia, a pliki, których tu brakuje, są usuwane. Przycisk Revert Local Changes pojawia się przy folderze Receive Only, gdy są lokalne zmiany. Cofa je: dodane pliki są kasowane, a zmienione lub usunięte są ponownie pobierane z klastra. Oba działania są destrukcyjne, więc przed kliknięciem sprawdź, które urządzenie ma właściwą wersję.
Słowa: override changes, revert local changes, cofnij zmiany, nadpisz zmiany, wymuś stan, przywróć z klastra, folder rozjechany, usuwanie plików, syncthing przycisk, ostrzeżenie
Źródło: https://docs.syncthing.net/users/foldertypes.html (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)
