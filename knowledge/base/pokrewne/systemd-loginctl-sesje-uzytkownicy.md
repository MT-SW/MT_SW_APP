### Co pokazuje polecenie loginctl list-sessions?
Polecenie `loginctl list-sessions` wypisuje aktualne sesje systemd-logind. Wynik można wypisać jako JSON flagą `--json=` albo `-j`. Strona nie opisuje dokładnie pól wyjścia.

Słowa: systemd, loginctl, list-sessions, sesje, logind, JSON
Źródło: https://www.freedesktop.org/software/systemd/man/latest/loginctl.html (sprawdzono 2026-10)

### Co pokazuje polecenie loginctl list-users?
Polecenie `loginctl list-users` wypisuje aktualnie zalogowanych użytkowników. Tak jak `list-sessions` obsługuje wyjście JSON flagami `--json=` albo `-j`.

Słowa: systemd, loginctl, list-users, zalogowani użytkownicy, JSON
Źródło: https://www.freedesktop.org/software/systemd/man/latest/loginctl.html (sprawdzono 2026-10)

### Co robi loginctl terminate-session?
Polecenie `terminate-session` kończy wskazane sesje. Zabija wszystkie procesy w sesji i zwalnia związane z nią zasoby. Jeśli podano pusty identyfikator, kończona jest sesja, w której wykonano polecenie. Polecenie pojawiło się w wersji 233.

Słowa: systemd, loginctl, terminate-session, zabijanie procesów, sesja, zasoby, wersja 233
Źródło: https://www.freedesktop.org/software/systemd/man/latest/loginctl.html (sprawdzono 2026-10)
