### Gdzie Syncthing przechowuje plik config.xml?
Domyślnie na Linuksie i Unixach katalog to `$XDG_STATE_HOME/syncthing` albo `$HOME/.local/state/syncthing`. Na Macu to `$HOME/Library/Application Support/Syncthing`, a na Windows `%LOCALAPPDATA%\Syncthing`. Lokalizację zmienia się flagami `--config` lub `--home` albo zmiennymi `$STCONFDIR` i `STHOMEDIR`.

Słowa: syncthing, config.xml, katalog konfiguracji, XDG_STATE_HOME, .local/state, STCONFDIR
Źródło: https://docs.syncthing.net/users/config.html (sprawdzono 2026-10)

### Gdzie Syncthing trzyma bazę indeksu?
Baza indeksu domyślnie jest w tym samym katalogu co konfiguracja. Można ją zmienić flagami `--data` lub `--home` albo zmienną `$STDATADIR`.

Słowa: syncthing, indeks, baza danych, --data, STDATADIR, katalog
Źródło: https://docs.syncthing.net/users/config.html (sprawdzono 2026-10)

### Jakie atrybuty folderu w config.xml Syncthing są obowiązkowe?
Atrybuty `id` i `path` są obowiązkowe. `id` to identyfikator folderu, który musi być unikalny. `path` to ścieżka do katalogu na tym urządzeniu, która nie jest wysyłana do innych urządzeń.

Słowa: syncthing, config.xml, folder, id, path, unikalny identyfikator, obowiązkowe
Źródło: https://docs.syncthing.net/users/config.html (sprawdzono 2026-10)

### Jak wyłączyć cykliczne skanowanie folderu w Syncthing?
Opcja `rescanIntervalS` ustawia interwał skanowania w sekundach. Wartość `0` wyłącza cykliczne skanowanie, co strona opisuje jako przydatne, gdy rescan wywołują zewnętrzne wtyczki. Przykłady w dokumentacji pokazują `3600`, czyli godzinę, ale strona zaznacza, że przykłady mogą być nieaktualne.

Słowa: syncthing, rescanIntervalS, skanowanie, 0, 3600, wtyczki, interwał
Źródło: https://docs.syncthing.net/users/config.html (sprawdzono 2026-10)
