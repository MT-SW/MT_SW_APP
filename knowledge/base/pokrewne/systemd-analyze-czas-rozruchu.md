### Co pokazuje systemd-analyze time?
Podkomenda `time` raportuje, ile trwał rozruch w trzech częściach: faza jądra przed przestrzenią użytkownika, faza initrd przed normalną przestrzenią użytkownika oraz czas inicjalizacji normalnej przestrzeni użytkownika. Mierzy do momentu uruchomienia wszystkich usług systemowych, a nie do ich pełnego zainicjowania.

Słowa: systemd-analyze, time, rozruch, initrd, jądro, uruchamianie usług
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd-analyze.html (sprawdzono 2026-10)

### Do czego służy systemd-analyze blame?
Podkomenda `blame` wypisuje działające jednostki posortowane według czasu inicjalizacji. Strona ostrzega, że wynik może wprowadzać w błąd, bo wolna usługa może tylko czekać na inną. Pomija usługi `Type=simple`, a nie liczy czasu w kolejce ani opóźnień sprzętowych.

Słowa: systemd-analyze, blame, jednostki, czas inicjalizacji, Type=simple, opóźnienia
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd-analyze.html (sprawdzono 2026-10)

### Jak czytać systemd-analyze critical-chain?
Polecenie `critical-chain` drukuje drzewo jednostek z łańcucha krytycznego. Po znaku `@` pokazany jest moment aktywacji jednostki, a po `+` czas jej uruchamiania. Opcja `--fuzz=` dodaje jednostki zakończone w zadanym przedziale od najpóźniejszej. Wynik może być mylący przy aktywacji przez gniazda i wykonywaniu równoległym.

Słowa: systemd-analyze, critical-chain, @, +, łańcuch krytyczny, --fuzz
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd-analyze.html (sprawdzono 2026-10)

### Co sprawdza systemd-analyze verify?
Podkomenda `verify` ładuje pliki jednostek i te, do których się odwołują, a potem wypisuje ostrzeżenia. Sprawdza nieznane sekcje i dyrektywy, brakujące zależności, nieinstalowane strony man z `Documentation=` oraz brakujące lub nieuruchamialne polecenia `ExecStart=`. Domyślnie kod wyjścia to zero niezależnie od ostrzeżeń.

Słowa: systemd-analyze, verify, pliki jednostek, ExecStart, zależności, kod wyjścia
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd-analyze.html (sprawdzono 2026-10)
