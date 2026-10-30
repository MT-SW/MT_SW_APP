<!-- Baza wiedzy MT_SW: wersje plików w Syncthing (kosz, prosty, stopniowany, zewnętrzny). Tylko informacyjnie. -->
# Syncthing: wersje plików i kosz

### Jak Syncthing zachowuje stare wersje plików: kosz i proste wersje?
Wersjonowanie w Syncthing działa tylko dla zmian przychodzących z innych urządzeń. Jeśli inne urządzenie zmieni albo usunie plik, stara wersja trafia do folderu zapasowego, a na urządzeniu, na którym to włączysz, zachowuje się kopię. Domyślnie wyłączone. Strategia kosza (trash can) przenosi zmieniony lub usunięty plik do kosza, a plik o tej samej nazwie w koszu jest nadpisywany. Opcja czyszczenia usuwa elementy po liczbie dni, a wartość zero wyłącza automatyczne czyszczenie. Strategia prosta (simple) przenosi pliki do folderu wersji z sygnaturą czasową i zachowuje tyle wersji, ile wskażesz w polu Keep Versions. Źródło opisuje to dla każdej strategii osobno, a wartości liczbowe podajesz sam.
Słowa: syncthing, wersjonowanie, kosz, trash can, simple versioning, keep versions, stversions, usunięty plik, przywracanie, kopia pliku, folder wersji, cofnięcie zmiany
Źródło: https://docs.syncthing.net/users/versioning (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)

### Czym różni się wersjonowanie stopniowane (staggered) w Syncthing?
Strategia stopniowana też zapisuje wersje z czasem, ale stare są przerzedzane automatycznie. Według dokumentacji przez pierwszą godzinę zostają wersje co 30 sekund, przez pierwszy dzień co godzinę, a do 30. dnia co dzień, potem co tydzień, aż do maksymalnego wieku. Maksymalny wiek to czas przechowywania wersji: w pliku konfiguracji podaje się go w sekundach, w interfejsie w dniach, a wartość zero oznacza przechowywanie na zawsze. Te przedziały opisuje oficjalna dokumentacja jako zachowanie strategii, więc nie trzeba ich pamiętać na pamięć. Jeśli zależy Ci na bardzo długiej historii, pamiętaj o miejscu na dysku.
Słowa: staggered, stopniowane, wersjonowanie, max age, maksymalny wiek, przerzedzanie, historia pliku, co 30 sekund, godzinowe, dzienne, tygodniowe, miejsce na dysku
Źródło: https://docs.syncthing.net/users/versioning (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)

### Jak zewnętrzna strategia wersjonowania w Syncthing wywołuje skrypt?
Strategia zewnętrzna (external) uruchamia wskazane polecenie tuż przed zastąpieniem lub usunięciem pliku. Przekazuje ścieżkę folderu i ścieżkę pliku, np. jako %FOLDER_PATH% i %FILE_PATH%, a także jako zmienne środowiskowe. Skrypt sam musi usunąć plik z folderu, bo inaczej Syncthing zgłosi błąd. Dokumentacja podaje przykład skryptu w powłoce, który przenosi plik do ~/.trashcan z zachowaniem struktury katalogów, oraz wersję dla Windows. Ta strategia ignoruje ustawienia ścieżki wersji. Uwaga na zapisy w pamięci USB: źródło ostrzega, że jeśli kosz systemowy jest wyłączony, skrypt może kasować pliki na stałe.
Słowa: external versioning, zewnętrzne wersjonowanie, skrypt, %FOLDER_PATH%, %FILE_PATH%, trashcan, kosz systemowy, usuwanie na stałe, pendrive, zmienne środowiskowe, onlylatest
Źródło: https://docs.syncthing.net/users/versioning (sprawdzono 2026-10; oficjalna dokumentacja Syncthing; uwaga o kasowaniu na stałe z wersji dokumentacji 1.19.1)
