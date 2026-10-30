### Jak pokazać logi jednej usługi w journalctl?
Opcja `-u` (`--unit=`) pokazuje wpisy dla wskazanej jednostki systemd lub dla pasujących wzorcem jednostek. Opcję można powtórzyć. Z flagą `--user` dopasowuje ona wpisy użytkownika.

Słowa: journalctl, systemd, logi, unit, filtrowanie, usługa
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)

### Jak śledzić logi na żywo w journalctl?
Flaga `-f` (`--follow`) pokazuje najnowsze wpisy i dalej wypisuje nowe, aż do przerwania Ctrl-C. Można ją łączyć z `-u`, żeby obserwować jedną usługę.

Słowa: journalctl, follow, logi na żywo, Ctrl-C, systemd
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)

### Jak filtrować logi journalctl po poziomie ważności?
Opcja `-p` (`--priority=`) przyjmuje poziomy od `emerg` (0) do `debug` (7). Pojedynczy poziom pokazuje ten poziom i ważniejsze. Zakres w formie `FROM..TO` obejmuje oba krańce.

Słowa: journalctl, priorytet, emerg, debug, poziom logów, filtrowanie
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)

### Jak wybrać logi z konkretnego startu systemu w journalctl?
Opcja `-b` (`--boot=`) wybiera start komputera. Pusta wartość oznacza bieżący start, `-1` poprzedni, a dodatnie liczby liczą od początku dziennika (1 to pierwszy start). Opcja `all` anuluje wcześniejsze `-b`.

Słowa: journalctl, boot, -b, start systemu, poprzedni start, dziennik
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)

### Jak ograniczyć zakres czasu w journalctl?
Opcje `--since=` i `--until=` ustawiają początek i koniec zakresu, np. w formacie `2012-10-30 18:17:16`. Brak godziny oznacza 00:00:00, a brak daty oznacza dzisiaj. Działają też słowa `yesterday`, `today`, `now` oraz czasy względne z `-` lub `+`.

Słowa: journalctl, since, until, zakres czasu, logi, data
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)

### Jak działa format wyjścia -o json w journalctl?
Opcja `-o json` wypisuje każdy wpis jako osobny obiekt JSON w jednej linii, a `json-pretty` rozbija każdy obiekt na wiele linii. Domyślny `short` przypomina klasyczny syslog, a `cat` pokazuje tylko treść wiadomości.

Słowa: journalctl, json, output, short, cat, format logów
Źródło: https://www.freedesktop.org/software/systemd/man/latest/journalctl.html (sprawdzono 2026-10)
