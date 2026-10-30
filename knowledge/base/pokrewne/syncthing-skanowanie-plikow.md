<!-- Baza wiedzy MT_SW: jak Syncthing wykrywa zmiany w plikach (pełne skanowanie, watcher) i pliki tymczasowe. Tylko informacyjnie. -->
# Syncthing: skanowanie zmian i obserwator plików

### Jak Syncthing wykrywa zmiany w plikach: pełne skanowanie czy obserwator plików?
Syncthing wykrywa zmiany na dwa sposoby: przez regularne pełne skanowanie i przez powiadomienia z systemu plików, czyli obserwator (watcher). Watcher jest domyślnie włączony, a pełne skanowanie odbywa się co godzinę, o ile nie zmienisz interwału dla folderu. Rzeczywisty czas każdego skanowania jest losowany między 3/4 a 5/4 interwału, żeby foldery nie skanowały się jednocześnie. Pełne skanowania zostają włączone nawet przy watcherze, bo watcher może pominąć niektóre zmiany. Watcher nie uruchamia skanowania od razu. Zbiera zmiany przez 10 sekund (regulowane fsWatcherDelayS), a usunięte pliki czekają dodatkowo minutę. Wykryte zmiany trafiają do innych urządzeń po każdym skanowaniu.
Słowa: syncthing skanowanie, watcher, obserwator plików, fsWatcherDelayS, pełne skanowanie, co godzinę, interwał skanowania, zmiany plików, opóźnienie synchronizacji, usunięte pliki, synchronizacja w czasie rzeczywistym
Źródło: https://docs.syncthing.net/users/syncing.html (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)

### Jak Syncthing zapisuje pliki tymczasowe i co z nimi dzieje się po błędzie?
Syncthing nigdy nie zapisuje zmian bezpośrednio do pliku docelowego. Najpierw zapisuje kopię tymczasową, a potem przenosi ją na miejsce oryginału. Plik tymczasowy jest ukryty i nazywa się .syncthing.nazwa.tmp, a na Windows ~syncthing~nazwa.tmp. Gdy nazwa byłaby za długa, Syncthing używa skrótu SHA-256 nazwy pliku. Jeśli błąd przerwie synchronizację, plik tymczasowy zostaje na około dzień, żeby nie pobierać danych od nowa. Ukryte pliki .syncthing.* nie są błędem, tylko częścią mechanizmu.
Słowa: syncthing plik tymczasowy, .syncthing.tmp, ~syncthing~, skrót sha-256, przerwana synchronizacja, pobieranie od nowa, ukryte pliki, dzień, błąd synchronizacji
Źródło: https://docs.syncthing.net/users/syncing.html (sprawdzono 2026-10; oficjalna dokumentacja Syncthing; okres przechowywania z opisu strony)
